/*
 * Copyright (C) 2017-2018 The Android Open Source Project, Sean J. Barbeau (sjbarbeau@gmail.com)
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
package com.android.gpstest.library.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

public class PermissionUtils {
    public static final int LOCATION_PERMISSION_REQUEST = 1;
    public static final int NOTIFICATION_PERMISSION_REQUEST = 1;

    public static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    };

    /**
     * Returns true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     * @param activity
     * @param requiredPermissions
     * @return true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     */
    public static boolean hasGrantedPermissions(Activity activity, String[] requiredPermissions) {
        for (String p : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /** Returns the permission string for notification permission */
    @RequiresApi(api = VERSION_CODES.TIRAMISU)
    public static String getNotificationPermission() {
        return Manifest.permission.POST_NOTIFICATIONS;
    }

    /** Returns true if the context has been granted notification permissions, false if it has not. */
    public static boolean hasGrantedNotificationPermissions(Context context) {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, getNotificationPermission())
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
