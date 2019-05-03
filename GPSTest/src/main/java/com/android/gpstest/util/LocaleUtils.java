/**
 * Based on https://github.com/YarikSOffice/LanguageTest/blob/master/app/src/main/java/com/yariksoffice/languagetest/Utility.java
 * Licensed under MIT - https://github.com/YarikSOffice/LanguageTest/blob/master/LICENSE
 */
package com.android.gpstest.util;

import android.os.Build;

public class LocaleUtils {

    public static boolean isAtLeastVersion(int version) {
        return Build.VERSION.SDK_INT >= version;
    }
}
