package com.cinebox.tv

import android.content.Context
import org.json.JSONArray

/** Bundled channel listing; video playback remains on YouTube. */
internal object NetdCatalog {
    @Volatile private var cached: List<Movie>? = null

    fun load(context: Context): List<Movie> = cached ?: synchronized(this) {
        cached ?: read(context).also { cached = it }
    }

    private fun read(context: Context): List<Movie> {
        val entries = context.assets.open("netd_music.json").bufferedReader(Charsets.UTF_8).use {
            JSONArray(it.readText())
        }
        return (0 until entries.length()).map { index ->
            val entry = entries.getJSONObject(index)
            val videoId = entry.getString("videoId")
            val duration = entry.optString("duration")
            Movie(
                id = entry.getInt("id"),
                title = entry.getString("title"),
                overview = "netd müzik kanalındaki müzik videosu. " +
                    (if (duration.isNotBlank()) "Süre: $duration. " else "") +
                    "YouTube üzerinden açılır.",
                poster = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                backdrop = "", year = "", score = 0.0, genreIds = emptyList(),
                provider = "netd müzik · YouTube",
                webUrl = "https://www.youtube.com/watch?v=$videoId",
                category = "Müzik"
            )
        }
    }
}
