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
package com.android.gpstest;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.lang.reflect.InvocationTargetException;

public class HelpActivity extends AppCompatActivity {

    public static final String TAG = "HelpActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView versionView = (TextView) findViewById(R.id.app_version);
        TextView helpView = (TextView) findViewById(R.id.help_text);

        String versionString = "";
        int versionCode = 0;
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionString = info.versionName;
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        StringBuilder version = new StringBuilder();
        // Version info
        version.append("v")
                .append(versionString)
                .append(" (")
                .append(versionCode)
                .append(")\n");

        java.lang.reflect.Method method;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            method = locationManager.getClass().getMethod("getGnssYearOfHardware");
            int hwYear = (int) method.invoke(locationManager);
            if (hwYear == 0) {
                version.append("HW Year: " + "2015 or older \n");
            } else {
                version.append("HW Year: " + hwYear + "\n");
            }
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "No such method exception: ", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Illegal Access exception: ", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "Invocation Target Exception: ", e);
        }

        String versionRelease = Build.VERSION.RELEASE;
        version.append("Platform: " + versionRelease + "\n");
        int apiLevel = Build.VERSION.SDK_INT;
        version.append("API Level: " + apiLevel + "\n");

        versionView.setText(version.toString());

        helpView.setText(R.string.help_text);
    }
}
