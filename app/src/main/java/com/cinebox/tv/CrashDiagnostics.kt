package com.cinebox.tv

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

/** Local only. Does not upload logs or change Android's crash handling. */
internal object CrashDiagnostics {
    private const val FILE = "last_crash.txt"
    fun install(context: Context) {
        val file = File(context.filesDir, FILE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                file.bufferedWriter().use { out ->
                    out.appendLine("ALICOTV ${BuildConfig.VERSION_NAME} · ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}")
                    out.appendLine("İş parçacığı: ${thread.name}")
                    // No URL, search text or exception message is stored.
                    var cause: Throwable? = error
                    repeat(5) {
                        val current = cause ?: return@repeat
                        out.appendLine(current.javaClass.name)
                        current.stackTrace.take(18).forEach { frame -> out.appendLine("  $frame") }
                        cause = current.cause?.takeIf { it !== current }
                    }
                }
            } catch (_: Throwable) { /* A low-memory/process-kill event may not be recordable. */ }
            if (previous != null) previous.uncaughtException(thread, error)
            else { android.os.Process.killProcess(android.os.Process.myPid()); kotlin.system.exitProcess(10) }
        }
    }
    fun showPrevious(activity: Activity) {
        val file = File(activity.filesDir, FILE)
        if (!file.exists()) return
        val report = try { file.bufferedReader().use { it.readText().take(14000) } } catch (_: Exception) { return }
        val text = TextView(activity).apply {
            this.text = "Son kapanmanın teknik bilgisi. Bu ekranın fotoğrafını paylaşabilirsiniz.\n\n$report"
            setTextIsSelectable(true)
            setPadding(24, 16, 24, 16)
            textSize = 14f
        }
        AlertDialog.Builder(activity).setTitle("ALICOTV kapanma kaydı")
            .setView(ScrollView(activity).apply { addView(text) })
            .setPositiveButton("Kapat") { _, _ -> file.delete() }
            .setCancelable(false).show()
    }
}
