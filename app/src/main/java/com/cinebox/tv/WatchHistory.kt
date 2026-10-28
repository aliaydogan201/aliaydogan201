package com.cinebox.tv

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class WatchEntry(val id: Int, val title: String, val url: String, val web: Boolean,
    val position: Long = -1L, val duration: Long = 0L, val updated: Long = 0L, val episode: String = "")
internal object WatchHistory {
    fun list(context: Context): List<WatchEntry> = try {
        val array = JSONArray(context.getSharedPreferences("alico_history", Context.MODE_PRIVATE).getString("entries", "[]"))
        (0 until array.length()).map { i -> val o=array.getJSONObject(i)
            WatchEntry(o.optInt("id"),o.getString("title"),o.getString("url"),o.optBoolean("web"),
                o.optLong("position",-1),o.optLong("duration"),o.optLong("updated"),o.optString("episode"))
        }.sortedByDescending { it.updated }
    } catch (_: Exception) { emptyList() }
    fun save(context: Context, entry: WatchEntry) {
        if (entry.url.isBlank()) return
        val rows = list(context).filterNot { it.url == entry.url }.toMutableList()
        rows.add(0,entry.copy(updated=System.currentTimeMillis()))
        write(context, rows.take(60))
    }
    fun remove(context: Context, url: String) { write(context,list(context).filterNot { it.url==url }) }
    private fun write(context: Context, rows: List<WatchEntry>) {
        val array=JSONArray()
        rows.forEach { e -> array.put(JSONObject().put("id",e.id).put("title",e.title).put("url",e.url)
            .put("web",e.web).put("position",e.position).put("duration",e.duration).put("updated",e.updated).put("episode",e.episode)) }
        context.getSharedPreferences("alico_history",Context.MODE_PRIVATE).edit().putString("entries",array.toString()).apply()
    }
    fun time(ms: Long): String { val seconds=(ms/1000).coerceAtLeast(0);return "%d:%02d:%02d".format(seconds/3600,(seconds/60)%60,seconds%60) }
}
