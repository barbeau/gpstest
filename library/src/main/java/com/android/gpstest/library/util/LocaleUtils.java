/**
 * Based on https://github.com/YarikSOffice/LanguageTest/blob/master/app/src/main/java/com/yariksoffice/languagetest/Utility.java
 * Licensed under MIT - https://github.com/YarikSOffice/LanguageTest/blob/master/LICENSE
 */
package com.android.gpstest.library.util;

import android.os.Build;

import androidx.annotation.ChecksSdkIntAtLeast;

public class LocaleUtils {

    @ChecksSdkIntAtLeast(parameter = 0)
    public static boolean isAtLeastVersion(int version) {
        return Build.VERSION.SDK_INT >= version;
    }
}
