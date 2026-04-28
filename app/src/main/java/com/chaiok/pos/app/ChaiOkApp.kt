package com.chaiok.pos.app

import android.app.Application
import com.chaiok.pos.data.di.AppContainer

class ChaiOkApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
