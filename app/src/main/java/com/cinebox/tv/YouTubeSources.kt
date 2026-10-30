package com.cinebox.tv

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import org.jsoup.Jsoup
import org.jsoup.Connection

internal object YouTubeSources {
    data class Channel(val id: String, val name: String, val category: String)
    data class Video(val id: String, val title: String)
    data class Result(val videos: List<Video>, val warnings: List<String>)
    val channels = listOf(
        Channel("UC73GUWWHTahOKVo7l9cKHfw", "Nature Empire Türkçe", "Belgesel"),
        Channel("UCR5wZcXtOUka8jTA57flzMg", "netd müzik", "Müzik"),
        Channel("UCVV3OecRPDpx0n-4XO-brnw", "Maç Hikayeleri", "Maç Özeti"),
        Channel("UCdlEXiVLTEvA280oyMvr8Kw", "Güldür Güldür", "Eğlence")
    )
    fun videoId(url: String): String? = Regex("(?:watch\\?v=|youtu\\.be/|shorts/)([A-Za-z0-9_-]{11})").find(url)?.groupValues?.get(1)
    private fun request(url: String) = Jsoup.connect(url).userAgent("Mozilla/5.0")
        .timeout(15000).maxBodySize(6_000_000).ignoreContentType(true)
    internal fun initial(html: String): JSONObject {
        val match = Regex("(?:var\\s+)?ytInitialData\\s*=\\s*").find(html)
            ?: error("Kanal verisi bulunamadı")
        return JSONTokener(html.substring(match.range.last + 1)).nextValue() as JSONObject
    }
    private fun walk(value: Any?, visit: (JSONObject) -> Unit) {
        when (value) {
            is JSONObject -> {
                visit(value)
                val keys = value.keys()
                while (keys.hasNext()) walk(value.opt(keys.next()), visit)
            }
            is JSONArray -> for (i in 0 until value.length()) walk(value.opt(i), visit)
        }
    }
    internal fun videos(data: JSONObject): List<Video> {
        val found = linkedMapOf<String, Video>()
        walk(data) { obj ->
            val v = obj.optJSONObject("lockupViewModel")
            if (v?.optString("contentType") == "LOCKUP_CONTENT_TYPE_VIDEO") {
                val id = v.optString("contentId")
                val title = v.optJSONObject("metadata")?.optJSONObject("lockupMetadataViewModel")
                    ?.optJSONObject("title")?.optString("content").orEmpty()
                if (id.matches(Regex("[A-Za-z0-9_-]{11}")) && title.isNotBlank()) found[id] = Video(id, title)
            }
            val short = obj.optJSONObject("shortsLockupViewModel")
            if (short != null) {
                val id = short.optJSONObject("onTap")?.optJSONObject("innertubeCommand")
                    ?.optJSONObject("reelWatchEndpoint")?.optString("videoId").orEmpty()
                val title = short.optJSONObject("overlayMetadata")?.optJSONObject("primaryText")?.optString("content").orEmpty()
                if (id.matches(Regex("[A-Za-z0-9_-]{11}")) && title.isNotBlank()) found[id] = Video(id, title)
            }
            val legacy = obj.optJSONObject("videoRenderer")
            if (legacy != null) {
                val id = legacy.optString("videoId")
                val runs = legacy.optJSONObject("title")?.optJSONArray("runs")
                val title = if (runs == null) "" else (0 until runs.length()).joinToString("") { runs.getJSONObject(it).optString("text") }
                if (id.matches(Regex("[A-Za-z0-9_-]{11}")) && title.isNotBlank()) found[id] = Video(id, title)
            }
        }
        return found.values.toList()
    }
    internal fun continuation(data: JSONObject): String? {
        var token: String? = null
        walk(data) { obj ->
            obj.optJSONObject("continuationItemRenderer")?.let { item ->
                walk(item) { child -> child.optJSONObject("continuationCommand")?.optString("token")?.takeIf { it.isNotBlank() }?.let { token = it } }
            }
        }
        return token
    }
    fun fetch(channel: Channel, known: Set<String>): Result {
        val found = linkedMapOf<String, Video>()
        val warnings = mutableListOf<String>()
        for (section in listOf("videos", "shorts", "streams")) {
            try {
                val html = request("https://www.youtube.com/channel/${channel.id}/$section?hl=en").get().html()
                val data = initial(html)
                val actualId = data.optJSONObject("metadata")?.optJSONObject("channelMetadataRenderer")?.optString("externalId")
                check(actualId == channel.id) { "Kanal doğrulanamadı" }
                val tabs = data.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer").getJSONArray("tabs")
                val tab = (0 until tabs.length()).mapNotNull { tabs.getJSONObject(it).optJSONObject("tabRenderer") }.firstOrNull { it.optBoolean("selected") }
                    ?: error("Sekme okunamadı")
                val expected = when(section) { "videos" -> "Videos"; "shorts" -> "Shorts"; else -> "Live" }
                if (tab.optString("title") != expected) {
                    if (section == "videos") error("Video listesi okunamadı")
                    continue // Channel does not expose this tab.
                }
                var page = tab.getJSONObject("content")
                val version = Regex("\"INNERTUBE_CLIENT_VERSION\":\"([^\"]+)\"").find(html)?.groupValues?.get(1)
                val seenTokens = mutableSetOf<String>()
                for (index in 0 until 10) {
                    val batch = videos(page)
                    check(batch.isNotEmpty()) { "Liste boş veya okunamadı" }
                    batch.forEach { if (it.id !in known) found[it.id] = it }
                    val token = continuation(page) ?: break
                    if (batch.any { it.id in known }) break
                    if (index == 9) { warnings.add("${channel.name}: tarama sınırına ulaşıldı; eski kayıtlar eksik olabilir"); break }
                    check(seenTokens.add(token)) { "Tekrarlanan liste" }
                    check(version != null) { "Kanal sürümü okunamadı" }
                    val payload = JSONObject().put("context", JSONObject().put("client", JSONObject().put("clientName", "WEB").put("clientVersion", version)))
                        .put("continuation", token)
                    page = JSONObject(request("https://www.youtube.com/youtubei/v1/browse?prettyPrint=false")
                        .method(Connection.Method.POST).header("Content-Type", "application/json").requestBody(payload.toString()).execute().body())
                }
            } catch (e: Exception) { warnings.add("${channel.name} ($section): listeye erişilemedi veya liste okunamadı") }
        }
        return Result(found.values.toList(), warnings)
    }
}
