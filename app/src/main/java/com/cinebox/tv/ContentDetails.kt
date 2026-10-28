package com.cinebox.tv
import android.content.Context
import org.json.JSONObject
internal object ContentDetails {
    private var data: JSONObject? = null
    fun get(context: Context, url: String): JSONObject {
        if (data == null) data = try { JSONObject(context.assets.open("content_details.json").bufferedReader().use { it.readText() }) } catch (_: Exception) { JSONObject() }
        return data?.optJSONObject(url) ?: JSONObject()
    }
}
