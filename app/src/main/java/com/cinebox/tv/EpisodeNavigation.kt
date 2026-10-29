package com.cinebox.tv

internal object EpisodeNavigation {
    private fun key(url: String) = url.substringBefore('#').trimEnd('/')
    fun next(episodes: List<SeriesEpisode>, currentUrl: String): SeriesEpisode? {
        val ordered = episodes.distinctBy { it.season to it.episode }
            .sortedWith(compareBy({ it.season }, { it.episode }))
        val index = ordered.indexOfFirst { key(it.url) == key(currentUrl) }
        return if (index >= 0) ordered.getOrNull(index + 1) else null
    }
}
