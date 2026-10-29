package com.cinebox.tv
import android.content.Context
internal object SeriesTracking {
    private fun prefs(c: Context) = c.getSharedPreferences("alico_series", Context.MODE_PRIVATE)
    fun following(c: Context): Set<Int> = prefs(c).getStringSet("following", emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    fun follow(c: Context, id: Int, enabled: Boolean) {
        val ids = following(c).toMutableSet()
        if (enabled) ids.add(id) else ids.remove(id)
        prefs(c).edit().putStringSet("following", ids.map { it.toString() }.toSet()).apply()
    }
    private fun key(url: String) = url.substringBefore('#').trimEnd('/')
    fun watched(c: Context, url: String) = prefs(c).getBoolean("seen_" + key(url), false)
    fun mark(c: Context, url: String, value: Boolean) {
        prefs(c).edit().putBoolean("seen_" + key(url), value).apply()
    }
    fun last(c: Context, id: Int): String = prefs(c).getString("last_" + id, "").orEmpty()
    fun opened(c: Context, id: Int, url: String) { prefs(c).edit().putString("last_" + id, url).apply() }
    fun label(c: Context, id: Int, url: String): String =
        (if (watched(c,url)) "✓ " else "") + (if (key(last(c,id)) == key(url)) "Kaldığın bölüm · " else "")
    fun episodes(c: Context, movie: Movie): List<SeriesEpisode> =
        if (movie.id == 980001) (youngSheldonEpisodes + OfflineEpisodes.get(c,movie.webUrl))
            .distinctBy { it.season to it.episode }.sortedWith(compareBy({it.season},{it.episode}))
        else OfflineEpisodes.get(c,movie.webUrl)
}
