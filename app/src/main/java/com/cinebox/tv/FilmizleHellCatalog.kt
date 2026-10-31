package com.cinebox.tv

import android.content.Context
import org.json.JSONArray

internal object FilmizleHellCatalog {
    @Volatile private var cached: List<Movie>? = null
    fun load(context: Context): List<Movie> = cached ?: synchronized(this) {
        cached ?: read(context).also { cached = it }
    }
    private fun read(context: Context): List<Movie> {
        val entries = context.assets.open("filmizlehell.json").bufferedReader(Charsets.UTF_8).use { JSONArray(it.readText()) }
        return (0 until entries.length()).map { index ->
            val v = entries.getJSONObject(index)
            val details = listOf(v.optString("year"), v.optString("genre"), v.optString("language")).filter { it.isNotBlank() }.joinToString(" · ")
            Movie(id=v.getInt("id"), title=v.getString("title"),
                overview=details + ". Film izle Hell film sayfası. Dil ve puan bilgileri kaynak kartından alınmıştır. Oynatma kaynak siteye bağlıdır.",
                poster=v.optString("poster"), backdrop="", year=v.optString("year"), score=v.optDouble("score",0.0),
                genreIds=emptyList(), provider="Film izle Hell", webUrl=v.getString("url"), category="Film")
        }
    }
}
