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
package com.android.gpstest.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    private static final String[] FILE_WRITE_REQUIRED_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int FILE_WRITE_PERMISSION_REQUEST = 2;

    /**
     * Returns true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     * @param activity
     * @param requiredPermissions
     * @return true if all of the provided permissions in requiredPermissions have been granted, or false if they have not
     */
    public static boolean hasGrantedPermissions(Activity activity, String[] requiredPermissions) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Permissions granted at install time
            return true;
        }
        for (String p : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * Requestes the permissions required for writing files.  This is a no-op if the permission is already granted,
     * otherwise it prompts the user to grant the file writing permissions.
     *
     * @param activity
     */
    public static void requestFileWritePermission(Activity activity) {
        if (hasGrantedFileWritePermission(activity)) {
            // No-op
        } else {
            // Request permissions from the user
            ActivityCompat.requestPermissions(activity, FILE_WRITE_REQUIRED_PERMISSIONS, FILE_WRITE_PERMISSION_REQUEST);
        }
    }

    /**
     * Returns true if the user has granted file write permissions, and false if they have not
     *
     * @param activity
     * @return true if the user has granted file write permissions, and false if they have not
     */
    public static boolean hasGrantedFileWritePermission(Activity activity) {
        return PermissionUtils.hasGrantedPermissions(activity, FILE_WRITE_REQUIRED_PERMISSIONS);
    }
}
