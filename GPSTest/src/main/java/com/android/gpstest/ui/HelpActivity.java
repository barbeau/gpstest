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
import android.view.MenuItem;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.gpstest.Application;
import com.android.gpstest.BuildConfig;
import com.android.gpstest.R;
import com.android.gpstest.library.util.IOUtils;
import com.android.gpstest.library.util.LibUIUtils;
import com.android.gpstest.library.util.PreferenceUtil;

/**
 * Activity for displaying application version information and help/documentation text.
 */
public class HelpActivity extends AppCompatActivity {

    public static final String TAG = "HelpActivity";

    /**
     * Initializes the activity, sets up the toolbar and theme, applies system bar and cutout insets,
     * and populates version and help information.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     *                           then this Bundle contains the data it most recently supplied.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set theme
        if (PreferenceUtil.INSTANCE.darkTheme(this, Application.Companion.getPrefs())) {
            setTheme(R.style.AppTheme_Dark_NoActionBar);
        }
        super.onCreate(savedInstanceState);
        LibUIUtils.resetActivityTitle(this);
        setContentView(R.layout.activity_help);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ScrollView scrollView = findViewById(R.id.help_scroll_view);
        int initialPaddingLeft = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int initialPaddingRight = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        int initialPaddingTop = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        int initialPaddingBottom = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);

        int initialToolbarPaddingLeft = toolbar.getPaddingLeft();
        int initialToolbarPaddingRight = toolbar.getPaddingRight();
        int initialToolbarPaddingTop = toolbar.getPaddingTop();
        int initialToolbarPaddingBottom = toolbar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(
                    initialToolbarPaddingLeft + insets.left,
                    initialToolbarPaddingTop,
                    initialToolbarPaddingRight + insets.right,
                    initialToolbarPaddingBottom
            );
            return windowInsets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(scrollView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            // Ensure toolbar horizontal insets are also updated in case AppBarLayout did not dispatch insets to its children
            toolbar.setPadding(
                    initialToolbarPaddingLeft + insets.left,
                    initialToolbarPaddingTop,
                    initialToolbarPaddingRight + insets.right,
                    initialToolbarPaddingBottom
            );
            v.setPadding(
                    initialPaddingLeft + insets.left,
                    initialPaddingTop,
                    initialPaddingRight + insets.right,
                    initialPaddingBottom + insets.bottom
            );
            return windowInsets;
        });

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

        StringBuilder version = new StringBuilder();
        // Version info
        version.append("v")
                .append(versionString)
                .append(" (")
                .append(versionCode)
                .append("-" + BuildConfig.FLAVOR + ")\n");

        version.append("GNSS HW Year: " + IOUtils.getGnssHardwareYear(Application.Companion.getApp()) + "\n");
        version.append("GNSS HW Name: " + IOUtils.getGnssHardwareModelName(Application.Companion.getApp()) + "\n");

        String versionRelease = Build.VERSION.RELEASE;
        version.append("Platform: " + versionRelease + "\n");
        int apiLevel = Build.VERSION.SDK_INT;
        version.append("API Level: " + apiLevel + "\n");

        versionView.setText(version.toString());

        helpView.setText(R.string.help_text);
    }

    /**
     * Handles action bar item selections, such as navigating up via the home button.
     *
     * @param item The menu item that was selected.
     * @return true if the item was handled, false otherwise.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Attaches the base context with the user's selected locale applied.
     *
     * @param base The base context.
     */
    @Override
    protected void attachBaseContext(Context base) {
        // For dynamically changing the locale
        super.attachBaseContext(Application.Companion.getLocaleManager().setLocale(base));
    }
}
