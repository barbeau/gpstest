/*
 * Copyright (C) 2018 Sean J. Barbeau
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
package com.android.gpstest.util;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.android.gpstest.Application;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Utility class that abstracts calls specific to a build flavor - for example, for Google Play
 * Services information so they can be substituted with no-op on build flavors that don't have Play Services
 */
public class BuildUtils {

    /**
     * Returns the Google Play Services app version as well as the Google Play Services library version
     * @return the Google Play Services app version as well as the Google Play Services library version
     */
    public static String getPlayServicesVersion() {
        PackageManager pm = Application.get().getPackageManager();
        StringBuilder builder = new StringBuilder();

        PackageInfo appInfoPlayServices;
        try {
            appInfoPlayServices = pm.getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, 0);
            builder.append("Google Play Services App: ");
            builder.append(appInfoPlayServices.versionName + "\n");
        } catch (PackageManager.NameNotFoundException e) {
            // Leave version as empty string
        }

        builder.append("Google Play Services Library: ");
        builder.append(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_VERSION_CODE + "\n");
        return builder.toString();
    }
}
