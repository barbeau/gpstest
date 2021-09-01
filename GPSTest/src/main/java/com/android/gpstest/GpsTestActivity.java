/*
 * Copyright (C) 2008-2018 The Android Open Source Project,
 * Sean J. Barbeau
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

import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_ACCURACY;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_CLEAR_AIDING_DATA;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_HELP;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_INJECT_PSDS_DATA;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_INJECT_TIME_DATA;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_MAP;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_OPEN_SOURCE;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_SEND_FEEDBACK;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_SETTINGS;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_SKY;
import static com.android.gpstest.NavigationDrawerFragment.NAVDRAWER_ITEM_STATUS;
import static com.android.gpstest.util.PermissionUtils.LOCATION_PERMISSION_REQUEST;
import static com.android.gpstest.util.PermissionUtils.REQUIRED_PERMISSIONS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.android.gpstest.map.MapConstants;
import com.android.gpstest.util.IOUtils;
import com.android.gpstest.util.LocationUtils;
import com.android.gpstest.util.PermissionUtils;
import com.android.gpstest.util.PreferenceUtils;
import com.android.gpstest.util.UIUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class GpsTestActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = "GpsTestActivity";

    private static final int SECONDS_TO_MILLISECONDS = 1000;

    private static final String GPS_RESUME = "gps_resume";
    private static final String EXISTING_CSV_LOG_FILE = "existing_csv_log_file";
    private static final String EXISTING_JSON_LOG_FILE = "existing_json_log_file";

    static boolean mIsLargeScreen = false;

    private static GpsTestActivity mActivity;

    private Toolbar mToolbar;

    private boolean mUseDarkTheme = false;

    /**
     * Currently selected navigation drawer position (so we don't unnecessarily swap fragments
     * if the same item is selected).  Initialized to -1 so the initial callback from
     * NavigationDrawerFragment always instantiates the fragments
     */
    private int mCurrentNavDrawerPosition = -1;

    //
    // Fragments controlled by the nav drawer
    //
    private GpsStatusFragment mStatusFragment;

    private GpsMapFragment mMapFragment;

    private GpsSkyFragment mSkyFragment;

    private GpsMapFragment mAccuracyFragment;

    boolean mStarted;

    boolean gpsResume = false;

    private Switch mSwitch;  // GPS on/off switch

    private LocationManager mLocationManager;

    private LocationProvider mProvider;

    private LocationListener locationListener;

    private Location mLastLocation;

    private long minTime; // Min Time between location updates, in milliseconds

    private float minDistance; // Min Distance between location updates, in meters

    private SensorManager mSensorManager;

    Bundle mLastSavedInstanceState;

    private boolean mUserDeniedPermission = false;

    private BenchmarkController mBenchmarkController;

    private String mInitialLanguage;

    private boolean shareDialogOpen = false;

    private ProgressBar progressBar = null;

    DeviceInfoViewModel deviceInfoViewModel;

    private boolean isServiceBound = false;

    private ForegroundOnlyLocationService service = null;

    ServiceConnection foregroundOnlyServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            ForegroundOnlyLocationService.LocalBinder binder = (ForegroundOnlyLocationService.LocalBinder) iBinder;
            service = binder.getService();
            isServiceBound = true;

            gpsStart();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            service = null;
            isServiceBound = false;
        }
    };

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Set theme
        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.AppTheme_Dark_NoActionBar);
            mUseDarkTheme = true;
        }
        super.onCreate(savedInstanceState);
        mActivity = this;
        // Reset the activity title to make sure dynamic locale changes are shown
        UIUtils.resetActivityTitle(this);

        saveInstanceState(savedInstanceState);

        // Set the default values from the XML file if this is the first
        // execution of the app
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        mInitialLanguage = PreferenceUtils.getString(getString(R.string.pref_key_language));

        // If we have a large screen, show all the fragments in one layout
        // TODO - Fix large screen layouts (see #122)
//        if (SatelliteUtils.isLargeScreen(this)) {
//            setContentView(R.layout.activity_main_large_screen);
//            mIsLargeScreen = true;
//        } else {
            setContentView(R.layout.activity_main);
//        }

        mBenchmarkController = new BenchmarkControllerImpl(this, findViewById(R.id.mainlayout));

        // Set initial Benchmark view visibility here - we can't do it before setContentView() b/c views aren't inflated yet
        if (mAccuracyFragment != null && mCurrentNavDrawerPosition == NAVDRAWER_ITEM_ACCURACY) {
            initAccuracy();
        } else {
            mBenchmarkController.hide();
        }

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        progressBar = findViewById(R.id.progress_horizontal);

        setupNavigationDrawer();

        deviceInfoViewModel = ViewModelProviders.of(this).get(DeviceInfoViewModel.class);

        Intent serviceIntent = new Intent(this, ForegroundOnlyLocationService.class);
        bindService(serviceIntent, foregroundOnlyServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // If another app is passing in a ground truth location, recreate the activity to initialize an existing instance
        if (IOUtils.isShowRadarIntent(intent) || IOUtils.isGeoIntent(intent)) {
            recreateApp(intent);
        }
    }

    /**
     * Save instance state locally so we can use it after the permission callback
     * @param savedInstanceState instance state to save
     */
    private void saveInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                mLastSavedInstanceState = savedInstanceState.deepCopy();
            } else {
                mLastSavedInstanceState = savedInstanceState;
            }
        }
    }

    private void setupNavigationDrawer() {
        // Fragment managing the behaviors, interactions and presentation of the navigation drawer.
        NavigationDrawerFragment navDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        navDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.nav_drawer_left_pane));
    }

     @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save GPS resume state
        outState.putBoolean(GPS_RESUME, gpsResume);
//         if (service.csvFileLogger.isStarted() && !shareDialogOpen) {
//             outState.putSerializable(EXISTING_CSV_LOG_FILE, service.csvFileLogger.getFile());
//         }
//         if (service.jsonFileLogger.isStarted() && !shareDialogOpen) {
//             outState.putSerializable(EXISTING_JSON_LOG_FILE, service.jsonFileLogger.getFile());
//         }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        shareDialogOpen = false;

        if (!mUserDeniedPermission) {
            requestPermissionAndInit(this);
        } else {
            // Explain permission to user (don't request permission here directly to avoid infinite
            // loop if user selects "Don't ask again") in system permission prompt
            UIUtils.showLocationPermissionDialog(this);
        }
        // If the set language has changed since we created the Activity (e.g., returning from Settings), recreate Activity
        if (Application.getPrefs().contains(getString(R.string.pref_key_language))) {
            String currentLanguage = PreferenceUtils.getString(getString(R.string.pref_key_language));
            if (!currentLanguage.equals(mInitialLanguage)) {
                mInitialLanguage = currentLanguage;
                recreateApp(null);
            }
        }
        mBenchmarkController.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UIUtils.PICKFILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // User picked a file to share from the Share dialog - update the dialog
            if (data != null) {
                Uri uri = data.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                final Location location = mLastLocation;
                shareDialogOpen = true;
                UIUtils.showShareFragmentDialog(this, location, service.isFileLoggingEnabled(),
                        service.csvFileLogger, service.jsonFileLogger, uri);
            }
        } else {
            // See if this result was a scanned QR Code with a ground truth location
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null) {
                String geoUri = scanResult.getContents();
                Location l = IOUtils.getLocationFromGeoUri(geoUri);
                if (l != null) {
                    l.removeAltitude(); // TODO - RFC 5870 requires altitude height above geoid, which we can't support yet (see #296 and #530), so remove altitude here
                    // Create a SHOW_RADAR intent out of the Geo URI and pass that to set ground truth
                    Intent showRadar = IOUtils.createShowRadarIntent(l);
                    recreateApp(showRadar);
                } else {
                    Toast.makeText(this, getString(R.string.qr_code_cannot_read_code),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Destroys and recreates the main activity in a new process.  If we don't use a new process,
     * the map state and Accuracy ground truth location TextViews get messed up with mixed locales
     * and partial state retention.
     * @param currentIntent the Intent to pass to the re-created app, or null if there is no intent to pass
     */
    void recreateApp(Intent currentIntent) {
        Intent i = new Intent(this, GpsTestActivity.class);
        if (IOUtils.isShowRadarIntent(currentIntent)) {
            // If we're creating the app because we got a SHOW_RADAR intent, copy over the intent action and extras
            i.setAction(currentIntent.getAction());
            i.putExtras(currentIntent.getExtras());
        } else if (IOUtils.isGeoIntent(currentIntent)) {
            // If we're creating the app because we got a geo: intent, turn it into a SHOW_RADAR intent for simplicity (they are used the same way)
            Location l = IOUtils.getLocationFromGeoUri(currentIntent.getData().toString());
            if (l != null) {
                Intent showRadarIntent = IOUtils.createShowRadarIntent(l);
                i.setAction(showRadarIntent.getAction());
                i.putExtras(showRadarIntent.getExtras());
            }
        }

        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        // Restart process to destroy and recreate everything
        System.exit(0);
    }

    @Override
    protected void attachBaseContext(Context base) {
        // For dynamically changing the locale
        super.attachBaseContext(Application.getLocaleManager().setLocale(base));
    }

    private void initAccuracy() {
        mAccuracyFragment.setOnMapClickListener(location -> mBenchmarkController.onMapClick(location));
        mBenchmarkController.show();
    }

    private void requestPermissionAndInit(final Activity activity) {
        if (PermissionUtils.hasGrantedPermissions(activity, REQUIRED_PERMISSIONS)) {
            init();
        } else {
            // Request permissions from the user
            ActivityCompat.requestPermissions(mActivity, REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mUserDeniedPermission = false;
                init();
            } else {
                mUserDeniedPermission = true;
            }
        }
    }

    private void init() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mProvider = mLocationManager.getProvider(LocationManager.GPS_PROVIDER);
        if (mProvider == null) {
            Log.e(TAG, "Unable to get GPS_PROVIDER");
            Toast.makeText(this, getString(R.string.gps_not_supported),
                    Toast.LENGTH_SHORT).show();
        }

        if (locationListener == null) {
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    mLastLocation = location;
                    Log.d(TAG, "Location from " + this + location);

                    // Reset the options menu to trigger updates to action bar menu items
                    invalidateOptionsMenu();
                }
            };
        }

        setupStartState(mLastSavedInstanceState);

        // If the theme has changed (e.g., from Preferences), destroy and recreate to reflect change
        boolean useDarkTheme = Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false);
        if (mUseDarkTheme != useDarkTheme) {
            mUseDarkTheme = useDarkTheme;
            recreate();
        }
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            UIUtils.promptEnableGps(this);
        }

        SharedPreferences settings = Application.getPrefs();

        checkKeepScreenOn(settings);

        checkTimeAndDistance(settings);

        UIUtils.autoShowWhatsNew(this);
    }

    @Override
    protected void onPause() {
        if (mStarted) {
            gpsStop();
            // If GPS was started, we want to resume it after orientation change
            gpsResume = true;
        } else {
            gpsResume = false;
        }

        super.onPause();
    }

    private void setupStartState(Bundle savedInstanceState) {
        // Apply start state settings from preferences
        SharedPreferences settings = Application.getPrefs();

        double tempMinTime = Double.parseDouble(
                settings.getString(getString(R.string.pref_key_gps_min_time),
                        getString(R.string.pref_gps_min_time_default_sec))
        );
        minTime = (long) (tempMinTime * SECONDS_TO_MILLISECONDS);
        minDistance = Float.parseFloat(
                settings.getString(getString(R.string.pref_key_gps_min_distance),
                        getString(R.string.pref_gps_min_distance_default_meters))
        );

        if (savedInstanceState != null) {
            // Activity is being restarted and has previous state (e.g., user rotated device)
            boolean gpsResume = savedInstanceState.getBoolean(GPS_RESUME, true);
            if (gpsResume) {
                gpsStart();
            }
        } else {
            // Activity is starting without previous state - use "Auto-start GNSS" setting, or gpsResume (e.g., if app was backgrounded via Home button)
            if (settings.getBoolean(getString(R.string.pref_key_auto_start_gps), true) || gpsResume) {
                gpsStart();
            }
        }
    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        goToNavDrawerItem(position);
    }

    private void goToNavDrawerItem(int item) {
        // Update the main content by replacing fragments
        switch (item) {
            case NAVDRAWER_ITEM_STATUS:
                if (mCurrentNavDrawerPosition != NAVDRAWER_ITEM_STATUS) {
                    showStatusFragment();
                    mCurrentNavDrawerPosition = item;
                }
                break;
            case NAVDRAWER_ITEM_MAP:
                if (mCurrentNavDrawerPosition != NAVDRAWER_ITEM_MAP) {
                    showMapFragment();
                    mCurrentNavDrawerPosition = item;
                }
                break;
            case NAVDRAWER_ITEM_SKY:
                if (mCurrentNavDrawerPosition != NAVDRAWER_ITEM_SKY) {
                    showSkyFragment();
                    mCurrentNavDrawerPosition = item;
                }
                break;
            case NAVDRAWER_ITEM_ACCURACY:
                if (mCurrentNavDrawerPosition != NAVDRAWER_ITEM_ACCURACY) {
                    showAccuracyFragment();
                    mCurrentNavDrawerPosition = item;
                }
                break;
            case NAVDRAWER_ITEM_INJECT_PSDS_DATA:
                forcePsdsInjection();
                break;
            case NAVDRAWER_ITEM_INJECT_TIME_DATA:
                forceTimeInjection();
                break;
            case NAVDRAWER_ITEM_CLEAR_AIDING_DATA:
                SharedPreferences prefs = Application.getPrefs();
                if (!prefs.getBoolean(getString(R.string.pref_key_never_show_clear_assist_warning), false)) {
                    showDialog(UIUtils.CLEAR_ASSIST_WARNING_DIALOG);
                } else {
                    deleteAidingData();
                }
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                startActivity(new Intent(this, Preferences.class));
                break;
            case NAVDRAWER_ITEM_HELP:
                showDialog(UIUtils.HELP_DIALOG);
                break;
            case NAVDRAWER_ITEM_OPEN_SOURCE:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(getString(R.string.open_source_github)));
                startActivity(i);
                break;
            case NAVDRAWER_ITEM_SEND_FEEDBACK:
                // Send App feedback
                String email = getString(R.string.app_feedback_email);
                String locationString = null;
                if (mLastLocation != null) {
                    locationString = LocationUtils.printLocationDetails(mLastLocation);
                }

                UIUtils.sendEmail(this, email, locationString, deviceInfoViewModel);
                break;
        }
        invalidateOptionsMenu();
    }

    private void showStatusFragment() {
        FragmentManager fm = getSupportFragmentManager();
        // Hide everything that shouldn't be shown
        hideMapFragment();
        hideSkyFragment();
        hideAccuracyFragment();
        if (mBenchmarkController != null) {
            mBenchmarkController.hide();
        }

        // Show fragment (we use show instead of replace to keep the map state)
        if (mStatusFragment == null) {
            // First check to see if an instance of fragment already exists
            mStatusFragment = (GpsStatusFragment) fm.findFragmentByTag(GpsStatusFragment.TAG);

            if (mStatusFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsStatusFragment");
                mStatusFragment = new GpsStatusFragment();
                fm.beginTransaction()
                        .add(R.id.fragment_container, mStatusFragment, GpsStatusFragment.TAG)
                        .commit();
            }
        }

        getSupportFragmentManager().beginTransaction().show(mStatusFragment).commit();
        setTitle(getResources().getString(R.string.gps_status_title));
    }

    private void hideStatusFragment() {
        FragmentManager fm = getSupportFragmentManager();
        mStatusFragment = (GpsStatusFragment) fm.findFragmentByTag(GpsStatusFragment.TAG);
        if (mStatusFragment != null && !mStatusFragment.isHidden()) {
            fm.beginTransaction().hide(mStatusFragment).commit();
        }
    }

    private void showMapFragment() {
        FragmentManager fm = getSupportFragmentManager();
        // Hide everything that shouldn't be shown
        hideStatusFragment();
        hideSkyFragment();
        hideAccuracyFragment();
        if (mBenchmarkController != null) {
            mBenchmarkController.hide();
        }

        // Show fragment (we use show instead of replace to keep the map state)
        if (mMapFragment == null) {
            // First check to see if an instance of fragment already exists
            mMapFragment = (GpsMapFragment) fm.findFragmentByTag(MapConstants.MODE_MAP);

            if (mMapFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsMapFragment");
                Bundle bundle = new Bundle();
                bundle.putString(MapConstants.MODE, MapConstants.MODE_MAP);
                mMapFragment = new GpsMapFragment();
                mMapFragment.setArguments(bundle);
                fm.beginTransaction()
                        .add(R.id.fragment_container, mMapFragment, MapConstants.MODE_MAP)
                        .commit();
            }
        }

        getSupportFragmentManager().beginTransaction().show(mMapFragment).commit();
        setTitle(getResources().getString(R.string.gps_map_title));
    }

    private void hideMapFragment() {
        FragmentManager fm = getSupportFragmentManager();
        mMapFragment = (GpsMapFragment) fm.findFragmentByTag(MapConstants.MODE_MAP);
        if (mMapFragment != null && !mMapFragment.isHidden()) {
            fm.beginTransaction().hide(mMapFragment).commit();
        }
    }

    private void showSkyFragment() {
        FragmentManager fm = getSupportFragmentManager();
        // Hide everything that shouldn't be shown
        hideStatusFragment();
        hideMapFragment();
        hideAccuracyFragment();
        if (mBenchmarkController != null) {
            mBenchmarkController.hide();
        }
        // Show fragment (we use show instead of replace to keep the map state)
        if (mSkyFragment == null) {
            // First check to see if an instance of fragment already exists
            mSkyFragment = (GpsSkyFragment) fm.findFragmentByTag(GpsSkyFragment.TAG);

            if (mSkyFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsStatusFragment");
                mSkyFragment = new GpsSkyFragment();
                fm.beginTransaction()
                        .add(R.id.fragment_container, mSkyFragment, GpsSkyFragment.TAG)
                        .commit();
            }
        }

        getSupportFragmentManager().beginTransaction().show(mSkyFragment).commit();
        setTitle(getResources().getString(R.string.gps_sky_title));
    }

    private void hideSkyFragment() {
        FragmentManager fm = getSupportFragmentManager();
        mSkyFragment = (GpsSkyFragment) fm.findFragmentByTag(GpsSkyFragment.TAG);
        if (mSkyFragment != null && !mSkyFragment.isHidden()) {
            fm.beginTransaction().hide(mSkyFragment).commit();
        }
    }

    private void showAccuracyFragment() {
        FragmentManager fm = getSupportFragmentManager();
        // Hide everything that shouldn't be shown
        hideStatusFragment();
        hideMapFragment();
        hideSkyFragment();
        // Show fragment (we use show instead of replace to keep the map state)
        if (mAccuracyFragment == null) {
            // First check to see if an instance of fragment already exists
            mAccuracyFragment = (GpsMapFragment) fm.findFragmentByTag(MapConstants.MODE_ACCURACY);

            if (mAccuracyFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsMapFragment for Accuracy");
                Bundle bundle = new Bundle();
                bundle.putString(MapConstants.MODE, MapConstants.MODE_ACCURACY);
                mAccuracyFragment = new GpsMapFragment();
                mAccuracyFragment.setArguments(bundle);
                fm.beginTransaction()
                        .add(R.id.fragment_container, mAccuracyFragment, MapConstants.MODE_ACCURACY)
                        .commit();
            }
        }

        getSupportFragmentManager().beginTransaction().show(mAccuracyFragment).commit();
        setTitle(getResources().getString(R.string.gps_accuracy_title));

        if (mBenchmarkController != null) {
            initAccuracy();
        }
    }

    private void hideAccuracyFragment() {
        FragmentManager fm = getSupportFragmentManager();
        mAccuracyFragment = (GpsMapFragment) fm.findFragmentByTag(MapConstants.MODE_ACCURACY);
        if (mAccuracyFragment != null && !mAccuracyFragment.isHidden()) {
            fm.beginTransaction().hide(mAccuracyFragment).commit();
        }
    }

    private void forcePsdsInjection() {
        boolean success = IOUtils.forcePsdsInjection(mLocationManager);
        if (success) {
            Toast.makeText(this, getString(R.string.force_psds_injection_success),
                    Toast.LENGTH_SHORT).show();
            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_inject_psds), PreferenceUtils.CAPABILITY_SUPPORTED);
        } else {
            Toast.makeText(this, getString(R.string.force_psds_injection_failure),
                    Toast.LENGTH_SHORT).show();
            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_inject_psds), PreferenceUtils.CAPABILITY_NOT_SUPPORTED);
        }
    }

    private void forceTimeInjection() {
        boolean success = IOUtils.forceTimeInjection(mLocationManager);
        if (success) {
            Toast.makeText(this, getString(R.string.force_time_injection_success),
                    Toast.LENGTH_SHORT).show();
            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_inject_time), PreferenceUtils.CAPABILITY_SUPPORTED);
        } else {
            Toast.makeText(this, getString(R.string.force_time_injection_failure),
                    Toast.LENGTH_SHORT).show();
            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_inject_time), PreferenceUtils.CAPABILITY_NOT_SUPPORTED);
        }
    }

    private void deleteAidingData() {
        // If GPS is currently running, stop it
        boolean lastStartState = mStarted;
        if (mStarted) {
            gpsStop();
        }
        boolean success = IOUtils.deleteAidingData(mLocationManager);
        if (success) {
            Toast.makeText(this, getString(R.string.delete_aiding_data_success),
                    Toast.LENGTH_SHORT).show();
            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_delete_assist), PreferenceUtils.CAPABILITY_SUPPORTED);
        } else {
            Toast.makeText(this, getString(R.string.delete_aiding_data_failure),
                    Toast.LENGTH_SHORT).show();
            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_delete_assist), PreferenceUtils.CAPABILITY_NOT_SUPPORTED);
        }

        if (lastStartState) {
            Handler h = new Handler();
            // Restart the GPS, if it was previously started, with a slight delay,
            // to refresh the assistance data
            h.postDelayed(this::gpsStart, 500);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.nav_drawer_left_pane);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            // Close navigation drawer
            drawer.closeDrawer(GravityCompat.START);
            return;
        } else if (mBenchmarkController != null) {
            // Close sliding drawer
            if (mBenchmarkController.onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    static GpsTestActivity getInstance() {
        return mActivity;
    }

    @SuppressLint("MissingPermission")
    private synchronized void gpsStart() {
        if (service != null) {
            service.subscribeToLocationUpdates();
        }
        showProgressBar();

        if (mLocationManager == null || mProvider == null) {
            return;
        }

        // TODO - Observe flows here

        if (!mStarted) {
            mLocationManager
                    .requestLocationUpdates(mProvider.getName(), minTime, minDistance, locationListener);
            mStarted = true;

            // Show Toast only if the user has set minTime or minDistance to something other than default values
            if (minTime != (long) (Double.parseDouble(getString(R.string.pref_gps_min_time_default_sec))
                    * SECONDS_TO_MILLISECONDS) ||
                    minDistance != Float
                            .parseFloat(getString(R.string.pref_gps_min_distance_default_meters))) {
                Toast.makeText(this, String.format(getString(R.string.gps_set_location_listener),
                        String.valueOf((double) minTime / SECONDS_TO_MILLISECONDS),
                        String.valueOf(minDistance)), Toast.LENGTH_SHORT).show();
            }

            // Reset the options menu to trigger updates to action bar menu items
            invalidateOptionsMenu();
        }
    }

    private synchronized void gpsStop() {
        if (mLocationManager == null) {
            return;
        }
        if (mStarted) {
            mLocationManager.removeUpdates(locationListener);
            mStarted = false;

            // Reset the options menu to trigger updates to action bar menu items
            invalidateOptionsMenu();
            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    // TODO - On first fix, on any fix
    private void hideProgressBar() {
        if (progressBar != null) {
            UIUtils.hideViewWithAnimation(progressBar, UIUtils.ANIMATION_DURATION_SHORT_MS);
        }
    }

    // TODO - On lost fix
    private void showProgressBar() {
        if (progressBar != null) {
            UIUtils.showViewWithAnimation(progressBar, UIUtils.ANIMATION_DURATION_SHORT_MS);
        }
    }



    @SuppressLint("MissingPermission")
    private void checkTimeAndDistance(SharedPreferences settings) {
        double minTimeDouble = Double
                .parseDouble(settings.getString(getString(R.string.pref_key_gps_min_time), "1"));
        long minTimeLong = (long) (minTimeDouble * SECONDS_TO_MILLISECONDS);

        if (minTime != minTimeLong ||
                minDistance != Float.parseFloat(
                        settings.getString(getString(R.string.pref_key_gps_min_distance), "0"))) {
            // User changed preference values, get the new ones
            minTime = minTimeLong;
            minDistance = Float.parseFloat(
                    settings.getString(getString(R.string.pref_key_gps_min_distance), "0"));
            // If the GPS is started, reset the location listener with the new values
            if (mStarted && mProvider != null) {
                mLocationManager
                        .requestLocationUpdates(mProvider.getName(), minTime, minDistance, locationListener);
                Toast.makeText(this, String.format(getString(R.string.gps_set_location_listener),
                        String.valueOf(minTimeDouble), String.valueOf(minDistance)),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void checkKeepScreenOn(SharedPreferences settings) {
//        if (!mIsLargeScreen) {
        mToolbar.setKeepScreenOn(settings.getBoolean(getString(R.string.pref_key_keep_screen_on), true));
//        } else {
//            // TODO - After we fix large screen devices in #122, we can delete the below block and
//            // use the above block with mToolbar.setKeepScreenOn() for all screen sizes
//            View v = findViewById(R.id.large_screen_layout);
//            if (v != null) {
//                if (settings.getBoolean(getString(R.string.pref_key_keep_screen_on), true)) {
//                    v.setKeepScreenOn(true);
//                } else {
//                    v.setKeepScreenOn(false);
//                }
//            }
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        initGpsSwitch(menu);
        return true;
    }

    private void initGpsSwitch(Menu menu) {
        MenuItem item = menu.findItem(R.id.gps_switch_item);
        if (item != null) {
            mSwitch = MenuItemCompat.getActionView(item).findViewById(R.id.gps_switch);
            if (mSwitch != null) {
                // Initialize state of GPS switch before we set the listener, so we don't double-trigger start or stop
                mSwitch.setChecked(mStarted);

                // Set up listener for GPS on/off switch
                mSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Turn GPS on or off
                        if (!mSwitch.isChecked() && mStarted) {
                            gpsStop();
                            // Measurements need to be stopped to prevent GnssStatus from updating - Samsung bug?
                            // TODO - removeGnssMeasurementsListener();
                        } else {
                            if (mSwitch.isChecked() && !mStarted) {
                                gpsStart();
                                // Measurements need to be started again
                                // TODO - addGnssMeasurementsListener();
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.share);
        if (item != null) {
            item.setVisible(mLastLocation != null || service.isFileLoggingEnabled());
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle menu item selection
        int itemId = item.getItemId();
        if (itemId == R.id.gps_switch) {
            return true;
        } else if (itemId == R.id.share) {
            share();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void share() {
        final Location location = mLastLocation;
        shareDialogOpen = true;
        UIUtils.showShareFragmentDialog(this, location, service.isFileLoggingEnabled(),
                service.csvFileLogger, service.jsonFileLogger, null);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case UIUtils.WHATSNEW_DIALOG:
                return UIUtils.createWhatsNewDialog(this);
            case UIUtils.HELP_DIALOG:
                return UIUtils.createHelpDialog(this);
            case UIUtils.CLEAR_ASSIST_WARNING_DIALOG:
                return createClearAssistWarningDialog();
        }
        return super.onCreateDialog(id);
    }

    private Dialog createClearAssistWarningDialog() {
        View view = getLayoutInflater().inflate(R.layout.clear_assist_warning, null);
        CheckBox neverShowDialog = view.findViewById(R.id.clear_assist_never_ask_again);

        neverShowDialog.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // Save the preference
            PreferenceUtils.saveBoolean(getString(R.string.pref_key_never_show_clear_assist_warning), isChecked);
        });

        Drawable icon = ContextCompat.getDrawable(Application.get(), R.drawable.ic_delete);
        if (icon != null) {
            DrawableCompat.setTint(icon, getResources().getColor(R.color.colorPrimary));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.clear_assist_warning_title)
                .setIcon(icon)
                .setCancelable(false)
                .setView(view)
                .setPositiveButton(R.string.yes,
                        (dialog, which) -> deleteAidingData()
                )
                .setNegativeButton(R.string.no,
                        (dialog, which) -> {
                            // No-op
                        }
                );
        return builder.create();
    }
}
