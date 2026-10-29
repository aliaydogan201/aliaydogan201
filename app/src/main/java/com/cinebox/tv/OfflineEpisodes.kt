package com.cinebox.tv
import android.content.Context
import org.json.JSONObject
internal object OfflineEpisodes {
    private var data: JSONObject? = null
    fun get(context: Context,url: String): List<SeriesEpisode> {
        if(data==null)data=try{JSONObject(context.assets.open("series_episodes.json").bufferedReader().use { it.readText() })}catch(_:Exception){JSONObject()}
        val array=data?.optJSONArray(url)?:org.json.JSONArray()
        return ((0 until array.length()).map { i -> val row=array.getJSONObject(i);SeriesEpisode(row.getInt("season"),row.getInt("episode"),row.getString("url")) } + ArchiveSync.episodes(context,url)).distinctBy { it.season to it.episode }.sortedWith(compareBy({it.season},{it.episode}))
    }
}
