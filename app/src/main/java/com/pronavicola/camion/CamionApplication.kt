package com.pronavicola.camion

import android.app.Application

class CamionApplication : Application() {
    lateinit var appContext: CamionAppContext
        private set

    override fun onCreate() {
        super.onCreate()
        appContext = CamionAppContext(platformContext = this).apply {
            startBackgroundServices()
        }
    }
}
