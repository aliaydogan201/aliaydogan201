package com.cinebox.tv

import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** One sorted index per catalog selection. Query work never runs on the UI thread. */
internal class CatalogSearch private constructor(private val entries: List<Pair<Movie, String>>) {
    suspend fun find(category: String, query: String): List<Movie> {
        val context = currentCoroutineContext()
        val needle = query.trim().lowercase(TURKISH)
        val result = ArrayList<Movie>()
        entries.forEachIndexed { index, (movie, title) ->
            if (index % 128 == 0) context.ensureActive()
            val matchesCategory = when (category) {
                "Tümü" -> true
                "Filmler" -> movie.category == "Film"
                "Diziler" -> movie.category == "Dizi" || movie.category == "Yerli Diziler"
                else -> movie.category == category
            }
            if (matchesCategory && title.contains(needle)) result.add(movie)
        }
        return result
    }
    companion object {
        private val TURKISH = Locale("tr", "TR")
        suspend fun build(movies: List<Movie>): CatalogSearch {
            val context = currentCoroutineContext()
            val collator = Collator.getInstance(TURKISH)
            val sorted = movies.distinctBy { it.id }.sortedWith { a, b ->
                context.ensureActive()
                collator.compare(a.title, b.title)
            }
            return CatalogSearch(sorted.map {
                context.ensureActive()
                it to it.title.lowercase(TURKISH)
            })
        }
    }
}
