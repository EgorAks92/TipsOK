package com.chaiok.pos.app

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.chaiok.pos.BuildConfig
import com.chaiok.pos.data.di.AppContainer

class ChaiOkApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        Log.i("StartupTrace", "Application.onCreate start")
        super.onCreate()
        enableStrictModeForDebug()
        container = AppContainer(this)
        Log.i("StartupTrace", "Application.onCreate end")
    }

    private fun enableStrictModeForDebug() {
        if (!BuildConfig.DEBUG) return

        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )

        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .detectLeakedSqlLiteObjects()
                .penaltyLog()
                .build()
        )
    }
}
