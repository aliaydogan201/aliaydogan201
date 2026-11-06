package com.cinebox.tv

import android.content.Context
import org.json.JSONArray

internal object SuperFilmCatalog {
    @Volatile private var cached: List<Movie>? = null
    fun load(context: Context): List<Movie> = cached ?: synchronized(this) {
        cached ?: read(context).also { cached = it }
    }
    private fun read(context: Context): List<Movie> {
        val entries = context.assets.open("superfilm.json").bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
        return (0 until entries.length()).map { index ->
            val v = entries.getJSONObject(index)
            Movie(id=v.getInt("id"), title=v.getString("title"),
                overview="SüperFilmizle film sayfası. Yıl ve puan kaynak listesinden alınmıştır. Video erişimi kaynak siteye bağlıdır.",
                poster=v.optString("poster"), backdrop="", year=v.optString("year"), score=v.optDouble("score",0.0),
                genreIds=emptyList(), provider="SüperFilmizle", webUrl=v.getString("url"), category="Film")
        }
    }
}
