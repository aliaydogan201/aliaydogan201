package com.cinebox.tv

import android.app.Application

class AlicoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashDiagnostics.install(this)
    }
}
