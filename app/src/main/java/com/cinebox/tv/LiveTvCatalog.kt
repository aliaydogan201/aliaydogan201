package com.cinebox.tv

import android.content.Context
import org.json.JSONArray

internal object LiveTvCatalog {
    data class Channel(val status: String, val youtubeUrl: String)
    @Volatile private var cached: List<Movie>? = null
    @Volatile private var channels: Map<Int, Channel> = emptyMap()
    fun entry(id: Int): Channel? = channels[id]
    fun label(id: Int): String {
        val item = channels[id] ?: return "Canlı TV"
        return when {
            item.youtubeUrl.isNotBlank() -> "Canlı TV · YouTube seçeneği"
            item.status == "live_page" -> "Canlı yayın sayfası"
            item.status == "website" -> "Yayıncı sitesi · Yayın doğrulanmadı"
            else -> "Yayın bağlantısı bulunamadı"
        }
    }
    fun load(context: Context): List<Movie> = cached ?: synchronized(this) {
        cached ?: read(context).also { cached = it }
    }
    private fun read(context: Context): List<Movie> {
        val data = context.assets.open("live_tv.json").bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
        val details = mutableMapOf<Int, Channel>()
        val result = (0 until data.length()).map { index ->
            val v = data.getJSONObject(index)
            val id = v.getInt("id")
            val status = v.getString("status")
            val youtube = v.optString("youtubeUrl")
            details[id] = Channel(status, youtube)
            val note = when (status) {
                "live_page" -> "Kanalın canlı yayın sayfası açılır. Oynatmayı yayıncı ekranından başlatabilirsiniz."
                "website" -> "Yayıncı sitesine bağlantı. Bu adresin canlı video sunduğu henüz doğrulanmadı."
                else -> "Kanal kaydı eklendi; yayın bağlantısı henüz bulunamadı."
            }
            Movie(id=id, title=v.getString("title"), overview=note +
                (if (youtube.isNotBlank()) " Resmî YouTube canlı yayın seçeneği de bulunur." else "") +
                " Yayın erişimi, bölge ve cihaz desteği yayıncıya bağlıdır.",
                poster="", backdrop="", year="", score=0.0, genreIds=emptyList(),
                provider="Canlı TV · " + v.getString("group") + " · " + v.getString("title"),
                webUrl=v.optString("url"), category="Canlı TV")
        }
        channels = details.toMap()
        return result
    }
}
