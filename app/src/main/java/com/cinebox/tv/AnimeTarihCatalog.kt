package com.cinebox.tv

import android.content.Context
import org.json.JSONArray

internal object AnimeTarihCatalog {
    @Volatile private var cached: List<Movie>? = null
    fun load(context: Context): List<Movie> = cached ?: synchronized(this) {
        cached ?: read(context).also { cached = it }
    }
    private fun read(context: Context): List<Movie> {
        val entries = context.assets.open("anime_tarih.json").bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
        return (0 until entries.length()).map { index ->
            val v = entries.getJSONObject(index)
            val videoId = v.getString("videoId")
            Movie(id=v.getInt("id"), title=v.getString("title"),
                overview="AnimeTarih (Anima History) kanalından tarih anlatımı. YouTube üzerinden açılır.",
                poster="https://i.ytimg.com/vi/$videoId/hqdefault.jpg", backdrop="", year="", score=0.0,
                genreIds=emptyList(), provider="AnimeTarih · Anima History · YouTube",
                webUrl="https://www.youtube.com/watch?v=$videoId", category="Belgesel")
        }
    }
}
