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

/**
 * Utility class that abstracts calls specific to a build flavor - for example, for Google Play
 * Services information so they can be substituted with no-op on build flavors that don't have Play Services
 */
public class BuildUtils {

    /**
     * Returns null, as Google Play Services isn't available on the osmdroid build flavor
     * @return null, as Google Play Services isn't available on the osmdroid build flavor
     */
    public static String getPlayServicesVersion() {
        return null;
    }
}
