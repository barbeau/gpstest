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

package com.android.gpstest;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Holds application-wide state
 *
 * @author Sean J. Barbeau
 */
public class Application extends android.app.Application {

    private static Application mApp;

    private SharedPreferences mPrefs;

    public static Application get() {
        return mApp;
    }

    public static SharedPreferences getPrefs() {
        return get().mPrefs;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mApp = this;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Set theme
        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.AppTheme_Dark);
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        mApp = null;
    }
}
