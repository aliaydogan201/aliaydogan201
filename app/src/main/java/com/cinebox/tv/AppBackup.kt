package com.cinebox.tv

import android.content.Context
import android.net.Uri
import android.util.AtomicFile
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

internal object AppBackup {
    private val files = listOf("archive_dynamic.json", "archive_episodes.json")
    private val prefs = listOf("cinebox", "cinebox_web", "archive_sync", "alico_history", "alico_series")
    private const val limit = 20 * 1024 * 1024
    private fun snapshot(c: Context): JSONObject {
        val fs = JSONObject()
        for (name in files) {
            val f = AtomicFile(File(c.filesDir,name))
            fs.put(name, if (f.baseFile.exists()) JSONObject(f.openRead().bufferedReader().use { it.readText() }) else JSONObject())
        }
        val ps = JSONObject()
        for (name in prefs) {
            val entries = JSONObject()
            c.getSharedPreferences(name,Context.MODE_PRIVATE).all.forEach { (key,value) ->
                val type = when(value) {
                    is String -> "string"; is Boolean -> "boolean"; is Int -> "int"
                    is Long -> "long"; is Float -> "float"; is Set<*> -> "set"
                    else -> error("Desteklenmeyen ayar")
                }
                entries.put(key,JSONObject().put("type",type).put("value",if(value is Set<*>) JSONArray(value.toList()) else value))
            }
            ps.put(name,entries)
        }
        return JSONObject().put("format","ALICOTV_BACKUP").put("version",1)
            .put("created",System.currentTimeMillis()).put("files",fs).put("prefs",ps)
    }
    private fun validate(o: JSONObject) {
        require(o.getString("format")=="ALICOTV_BACKUP" && o.getInt("version")==1) { "Geçerli bir ALICOTV yedeği değil." }
        val fs=o.getJSONObject("files")
        files.forEach { fs.getJSONObject(it) }
        val movies=fs.getJSONObject("archive_dynamic.json").optJSONArray("movies") ?: JSONArray()
        for(i in 0 until movies.length()) {
            val row=movies.getJSONObject(i)
            row.getInt("id"); row.getString("title")
            require(Uri.parse(row.getString("url")).scheme=="https")
        }
        val episodes=fs.getJSONObject("archive_episodes.json")
        episodes.keys().forEach { key ->
            val rows=episodes.getJSONArray(key)
            for(i in 0 until rows.length()) {
                val row=rows.getJSONObject(i)
                require(row.getInt("season")>=0 && row.getInt("episode")>=0)
                require(Uri.parse(row.getString("url")).scheme=="https")
            }
        }
        val ps=o.getJSONObject("prefs")
        prefs.forEach { name ->
            val entries=ps.getJSONObject(name)
            entries.keys().forEach { key ->
                val e=entries.getJSONObject(key)
                when(e.getString("type")) {
                    "string" -> e.getString("value")
                    "boolean" -> e.getBoolean("value")
                    "int" -> e.getInt("value")
                    "long" -> e.getLong("value")
                    "float" -> require(e.getDouble("value").toFloat().isFinite())
                    "set" -> { val a=e.getJSONArray("value"); for(i in 0 until a.length()) a.getString(i) }
                    else -> error("Geçersiz ayar türü")
                }
            }
        }
    }
    private fun apply(c:Context,o:JSONObject) {
        files.forEach { name ->
            val f=AtomicFile(File(c.filesDir,name));val out=f.startWrite()
            try { out.write(o.getJSONObject("files").getJSONObject(name).toString().toByteArray(Charsets.UTF_8));f.finishWrite(out) }
            catch(e:Exception) { f.failWrite(out);throw e }
        }
        prefs.forEach { name ->
            val edit=c.getSharedPreferences(name,Context.MODE_PRIVATE).edit().clear()
            val entries=o.getJSONObject("prefs").getJSONObject(name)
            entries.keys().forEach { key ->
                val e=entries.getJSONObject(key)
                when(e.getString("type")) {
                    "string" -> edit.putString(key,e.getString("value"))
                    "boolean" -> edit.putBoolean(key,e.getBoolean("value"))
                    "int" -> edit.putInt(key,e.getInt("value"))
                    "long" -> edit.putLong(key,e.getLong("value"))
                    "float" -> edit.putFloat(key,e.getDouble("value").toFloat())
                    "set" -> {val a=e.getJSONArray("value");edit.putStringSet(key,(0 until a.length()).map { a.getString(it) }.toSet())}
                }
            }
            check(edit.commit()) { "Ayarlar kaydedilemedi." }
        }
    }
    fun export(c:Context,uri:Uri) {
        val bytes=snapshot(c).toString().toByteArray(Charsets.UTF_8)
        require(bytes.size<=limit) { "Yedek 20 MB sınırını aşıyor." }
        val out=c.contentResolver.openOutputStream(uri,"wt") ?: error("Dosya açılamadı.")
        out.use { it.write(bytes) }
    }
    fun restore(c:Context,uri:Uri) {
        val input=c.contentResolver.openInputStream(uri) ?: error("Yedek açılamadı.")
        val bytes=input.use {
            val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
            while(true) {val count=it.read(buffer);if(count<0)break;require(out.size()+count<=limit){"Yedek çok büyük."};out.write(buffer,0,count)}
            out.toByteArray()
        }
        val incoming=JSONObject(String(bytes,Charsets.UTF_8));validate(incoming)
        val previous=snapshot(c)
        // Keep a local recovery copy before replacing any data.
        val recovery=AtomicFile(File(c.filesDir,"before_restore.json"));val out=recovery.startWrite()
        try {out.write(previous.toString().toByteArray(Charsets.UTF_8));recovery.finishWrite(out)}
        catch(e:Exception){recovery.failWrite(out);throw e}
        try {apply(c,incoming)} catch(e:Exception) {apply(c,previous);throw e}
    }
}
