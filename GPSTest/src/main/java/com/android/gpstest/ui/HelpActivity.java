/*
 * Copyright (C) 2016 Sean J. Barbeau
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
package com.android.gpstest.ui;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.gpstest.Application;
import com.android.gpstest.BuildConfig;
import com.android.gpstest.R;
import com.android.gpstest.library.util.IOUtils;

public class HelpActivity extends AppCompatActivity {

    public static final String TAG = "HelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView versionView = findViewById(R.id.app_version);
        TextView helpView = findViewById(R.id.help_text);

        String versionString = "";
        int versionCode = 0;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionString = info.versionName;
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        // Version info

        String versionRelease = Build.VERSION.RELEASE;
        int apiLevel = Build.VERSION.SDK_INT;
        String version = "v" +
                versionString +
                " (" +
                versionCode +
                "-" + BuildConfig.FLAVOR + ")\n" +
                "GNSS HW Year: " + IOUtils.getGnssHardwareYear(Application.Companion.getApp()) + "\n" +
                "GNSS HW Name: " + IOUtils.getGnssHardwareModelName(Application.Companion.getApp()) + "\n" +
                "Platform: " + versionRelease + "\n" +
                "API Level: " + apiLevel + "\n";

        versionView.setText(version);

        helpView.setText(com.android.gpstest.library.R.string.help_text);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // For dynamically changing the locale
        super.attachBaseContext(Application.Companion.getLocaleManager().setLocale(base));
    }
}
