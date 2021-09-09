/*
 * Copyright (C) 2013 Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.preference.PreferenceManager
import androidx.multidex.MultiDexApplication
import com.android.gpstest.lang.LocaleManager
import dagger.hilt.android.HiltAndroidApp

/**
 * Holds application-wide state
 *
 * @author Sean J. Barbeau
 */
@HiltAndroidApp
class Application : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        app = this
        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // Set theme
        if (prefs.getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.AppTheme_Dark)
        }
    }

    override fun attachBaseContext(base: Context) {
        localeManager = LocaleManager(base)
        super.attachBaseContext(localeManager.setLocale(base))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.setLocale(this)
    }

    companion object {
        lateinit var app: Application
            private set

        lateinit var localeManager: LocaleManager
            private set

        lateinit var prefs: SharedPreferences
            private set
    }
}