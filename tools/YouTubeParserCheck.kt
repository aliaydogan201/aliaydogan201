package com.cinebox.tv
import java.io.File
import org.json.JSONObject
fun main(args: Array<String>) {
    val videoData = YouTubeSources.initial(File(args[0]).readText())
    val shortData = YouTubeSources.initial(File(args[1]).readText())
    fun selected(data:JSONObject):JSONObject {
        val tabs=data.getJSONObject("contents").getJSONObject("twoColumnBrowseResultsRenderer").getJSONArray("tabs")
        return (0 until tabs.length()).mapNotNull { tabs.getJSONObject(it).optJSONObject("tabRenderer") }
            .first { it.optBoolean("selected") }.getJSONObject("content")
    }
    val videoPage=selected(videoData);val shorts=selected(shortData)
    check(YouTubeSources.videos(videoPage).size==30)
    check(YouTubeSources.videos(shorts).size==4)
    check(YouTubeSources.continuation(videoPage)!=null)
    check(YouTubeSources.continuation(shorts)==null)
    check(YouTubeSources.videoId("https://www.youtube.com/watch?v=abcdefghijk") == "abcdefghijk")
    check(YouTubeSources.videoId("https://youtu.be/abcdefghijk") == "abcdefghijk")
    check(YouTubeSources.videoId("https://www.youtube.com/shorts/abcdefghijk") == "abcdefghijk")
    val duplicated=JSONObject().put("a",videoPage).put("b",videoPage)
    check(YouTubeSources.videos(duplicated).size==30)
    val chips=JSONObject("""{"chip":{"continuationCommand":{"token":"not-a-page"}}}""")
    check(YouTubeSources.continuation(chips)==null)
    check(runCatching { YouTubeSources.initial("no data") }.isFailure)
    println("YouTube parser: fixture, Shorts, continuation, duplicates, invalid data checks passed")
}
