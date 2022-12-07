package com.android.gpstest

import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.multidex.MultiDexApplication
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class Application : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        app = this
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
    }
    companion object {
        lateinit var app: Application
            private set

        lateinit var prefs: SharedPreferences
            private set
    }
}