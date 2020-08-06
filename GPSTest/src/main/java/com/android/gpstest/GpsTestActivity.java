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

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.OnNmeaMessageListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.MenuItemCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;

import com.android.gpstest.io.FileLogger;
import com.android.gpstest.map.MapConstants;
import com.android.gpstest.util.IOUtils;
import com.android.gpstest.util.LocationUtils;
import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.PermissionUtils;
import com.android.gpstest.util.PreferenceUtils;
import com.android.gpstest.util.SatelliteUtils;
import com.android.gpstest.util.UIUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.File;
import java.util.ArrayList;

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
import static com.android.gpstest.util.IOUtils.writeGnssMeasurementToAndroidStudio;
import static com.android.gpstest.util.IOUtils.writeNavMessageToAndroidStudio;
import static com.android.gpstest.util.IOUtils.writeNmeaToAndroidStudio;

public class GpsTestActivity extends AppCompatActivity
        implements LocationListener, SensorEventListener, NavigationDrawerFragment.NavigationDrawerCallbacks {

    private static final String TAG = "GpsTestActivity";

    private static final int WHATSNEW_DIALOG = 1;

    private static final int HELP_DIALOG = 2;

    private static final int CLEAR_ASSIST_WARNING_DIALOG = 3;

    private static final String WHATS_NEW_VER = "whatsNewVer";

    private static final int SECONDS_TO_MILLISECONDS = 1000;

    private static final String GPS_STARTED = "gps_started";
    private static final String EXISTING_LOG_FILE = "existing_log_file";

    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    static boolean mIsLargeScreen = false;

    private static GpsTestActivity mActivity;

    private Toolbar mToolbar;

    private boolean mUseDarkTheme = false;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

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

    // Holds sensor data
    private static float[] mRotationMatrix = new float[16];

    private static float[] mRemappedMatrix = new float[16];

    private static float[] mValues = new float[3];

    private static float[] mTruncatedRotationVector = new float[4];

    private static boolean mTruncateVector = false;

    boolean mStarted;

    boolean mFaceTrueNorth;

    boolean mWriteNmeaToAndroidMonitor;

    boolean mWriteNmeaTimestampToAndroidMonitor;

    boolean mWriteNavMessageToAndroidMonitor;

    boolean mWriteRawMeasurementToAndroidMonitor;

    boolean mWriteNmeaToFile;

    boolean mWriteNavMessageToFile;

    boolean mWriteRawMeasurementsToFile;

    boolean mWriteLocationToFile;

    private Switch mSwitch;  // GPS on/off switch

    private LocationManager mLocationManager;

    private LocationProvider mProvider;

    /**
     * Android M (6.0.1) and below status and listener
     */
    private GpsStatus mLegacyStatus;

    private GpsStatus.Listener mLegacyStatusListener;

    private GpsStatus.NmeaListener mLegacyNmeaListener;

    /**
     * Android N (7.0) and above status and listeners
     */
    private GnssStatus mGnssStatus;

    private GnssStatus.Callback mGnssStatusListener;

    private GnssMeasurementsEvent.Callback mGnssMeasurementsListener;

    private OnNmeaMessageListener mOnNmeaMessageListener;

    private GnssNavigationMessage.Callback mGnssNavMessageListener;

    // Listeners for Fragments
    private ArrayList<GpsTestListener> mGpsTestListeners = new ArrayList<GpsTestListener>();

    private Location mLastLocation;

    private GeomagneticField mGeomagneticField;

    private long minTime; // Min Time between location updates, in milliseconds

    private float minDistance; // Min Distance between location updates, in meters

    private SensorManager mSensorManager;

    Bundle mLastSavedInstanceState;

    private boolean mUserDeniedPermission = false;

    private BenchmarkController mBenchmarkController;

    private String mInitialLanguage;

    private FileLogger mFileLogger;

    private boolean shareDialogOpen = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Set theme
        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.AppTheme_Dark_NoActionBar);
            mUseDarkTheme = true;
        }
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
        mGpsTestListeners.add(mBenchmarkController);

        // Set initial Benchmark view visibility here - we can't do it before setContentView() b/c views aren't inflated yet
        if (mAccuracyFragment != null && mCurrentNavDrawerPosition == NAVDRAWER_ITEM_ACCURACY) {
            initAccuracy();
        } else {
            mBenchmarkController.hide();
        }

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        setupNavigationDrawer();

        mFileLogger = new FileLogger(getApplicationContext());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // If another app is passing in a ground truth location, recreate the activity to initialize an existing instance
        if (IOUtils.isShowRadarIntent(intent)) {
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
        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.nav_drawer_left_pane));
    }

     @Override
    public void onSaveInstanceState(Bundle outState) {
        // Save current GPS started state
        outState.putBoolean(GPS_STARTED, mStarted);
         if (mFileLogger.isStarted() && !shareDialogOpen) {
             outState.putSerializable(EXISTING_LOG_FILE, mFileLogger.getFile());
         }
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
            showLocationPermissionDialog();
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
                UIUtils.createShareDialog(this, location, isFileLoggingEnabled(), mFileLogger, uri).show();
            }
        } else {
            // See if this result was a scanned QR Code with a ground truth location
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (scanResult != null) {
                String geoUri = scanResult.getContents();
                Location l = IOUtils.getLocationFromGeoUri(geoUri);
                if (l != null) {
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
            int requestCode, String[] permissions, int[] grantResults) {
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

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        setupStartState(mLastSavedInstanceState);

        // If the theme has changed (e.g., from Preferences), destroy and recreate to reflect change
        boolean useDarkTheme = Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false);
        if (mUseDarkTheme != useDarkTheme) {
            mUseDarkTheme = useDarkTheme;
            recreate();
        }

        addStatusListener();

        addOrientationSensorListener();

        addNmeaListener();

        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            promptEnableGps();
        }

        /**
         * Check preferences to see how these componenets should be initialized
         */
        SharedPreferences settings = Application.getPrefs();

        checkKeepScreenOn(settings);

        checkTimeAndDistance(settings);

        checkTrueNorth(settings);

        checkNmeaOutput(settings);
        checkLocationOutput(settings);

        if (SatelliteUtils.isGnssStatusListenerSupported()) {
            checkGnssMeasurementOutput(settings);
        }

        if (SatelliteUtils.isGnssStatusListenerSupported()) {
            checkNavMessageOutput(settings);
        }

        if (PermissionUtils.hasGrantedFileWritePermission(this)
                && !mFileLogger.isStarted()
                && isFileLoggingEnabled()) {
            // User has granted permissions and has chosen to log at least one data type
            File existingFile = null;
            if (mLastSavedInstanceState != null) {
                // See if this was an orientation change and we should continue logging to
                // an existing file
                existingFile = (File) mLastSavedInstanceState.getSerializable(EXISTING_LOG_FILE);
            }
            mFileLogger.startLog(existingFile);
        }

        autoShowWhatsNew();
    }

    @Override
    protected void onPause() {
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }

        // Remove status listeners
        removeStatusListener();
        removeNmeaListener();
        if (SatelliteUtils.isGnssStatusListenerSupported()) {
            removeNavMessageListener();
        }
        if (SatelliteUtils.isGnssStatusListenerSupported()) {
            removeGnssMeasurementsListener();
        }
        // Check if the user has chosen to stop GNSS whenever app is in background
        if (!isChangingConfigurations() && Application.getPrefs().getBoolean(getString(R.string.pref_key_stop_gnss_in_background), false)) {
            gpsStop();
        }

        super.onPause();
    }

    private boolean isFileLoggingEnabled() {
        return mWriteNmeaToFile || mWriteRawMeasurementsToFile || mWriteNavMessageToFile || mWriteLocationToFile;
    }

    private void setupStartState(Bundle savedInstanceState) {
        // Apply start state settings from preferences
        SharedPreferences settings = Application.getPrefs();

        double tempMinTime = Double.valueOf(
                settings.getString(getString(R.string.pref_key_gps_min_time),
                        getString(R.string.pref_gps_min_time_default_sec))
        );
        minTime = (long) (tempMinTime * SECONDS_TO_MILLISECONDS);
        minDistance = Float.valueOf(
                settings.getString(getString(R.string.pref_key_gps_min_distance),
                        getString(R.string.pref_gps_min_distance_default_meters))
        );

        if (savedInstanceState != null) {
            // Activity is being restarted and has previous state (e.g., user rotated device)
            boolean gpsWasStarted = savedInstanceState.getBoolean(GPS_STARTED, true);
            if (gpsWasStarted) {
                gpsStart();
            }
        } else {
            // Activity is starting without previous state - use "Auto-start GNSS" setting
            if (settings.getBoolean(getString(R.string.pref_key_auto_start_gps), true)) {
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
                    showDialog(CLEAR_ASSIST_WARNING_DIALOG);
                } else {
                    deleteAidingData();
                }
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                startActivity(new Intent(this, Preferences.class));
                break;
            case NAVDRAWER_ITEM_HELP:
                showDialog(HELP_DIALOG);
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

                UIUtils.sendEmail(this, email, locationString);
                break;
        }
        invalidateOptionsMenu();
    }

    // Return true if this HomeActivity has no active content fragments
    private boolean noActiveFragments() {
        return mStatusFragment == null && mMapFragment == null && mSkyFragment == null;
    }

    private void showStatusFragment() {
        FragmentManager fm = getSupportFragmentManager();
        /**
         * Hide everything that shouldn't be shown
         */
        hideMapFragment();
        hideSkyFragment();
        hideAccuracyFragment();
        if (mBenchmarkController != null) {
            mBenchmarkController.hide();
        }

        /**
         * Show fragment (we use show instead of replace to keep the map state)
         */
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
        /**
         * Hide everything that shouldn't be shown
         */
        hideStatusFragment();
        hideSkyFragment();
        hideAccuracyFragment();
        if (mBenchmarkController != null) {
            mBenchmarkController.hide();
        }
        /**
         * Show fragment (we use show instead of replace to keep the map state)
         */
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
        /**
         * Hide everything that shouldn't be shown
         */
        hideStatusFragment();
        hideMapFragment();
        hideAccuracyFragment();
        if (mBenchmarkController != null) {
            mBenchmarkController.hide();
        }

        /**
         * Show fragment (we use show instead of replace to keep the map state)
         */
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
        /**
         * Hide everything that shouldn't be shown
         */
        hideStatusFragment();
        hideMapFragment();
        hideSkyFragment();
        /**
         * Show fragment (we use show instead of replace to keep the map state)
         */
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
        boolean success;
        // FIXME - We should use the platform constant from Build.VERSION_CODES below, but we can't use compileSdkVersion 29 due to NMEA interface issue (https://github.com/barbeau/gpstest/issues/340)
        if (Build.VERSION.SDK_INT >= 29) {
            success = sendExtraCommand(getString(R.string.force_psds_injection_command));
        } else {
            success = sendExtraCommand(getString(R.string.force_xtra_injection_command));
        }

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
        boolean success = sendExtraCommand(getString(R.string.force_time_injection_command));
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
        boolean success = sendExtraCommand(getString(R.string.delete_aiding_data_command));
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
            h.postDelayed(new Runnable() {
                public void run() {
                    gpsStart();
                }
            }, 500);
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

    void addListener(GpsTestListener listener) {
        mGpsTestListeners.add(listener);
    }

    @SuppressLint("MissingPermission")
    private synchronized void gpsStart() {
        if (mLocationManager == null || mProvider == null) {
            return;
        }

        if (!mStarted) {
            mLocationManager
                    .requestLocationUpdates(mProvider.getName(), minTime, minDistance, this);
            mStarted = true;

            // Show Toast only if the user has set minTime or minDistance to something other than default values
            if (minTime != (long) (Double.valueOf(getString(R.string.pref_gps_min_time_default_sec))
                    * SECONDS_TO_MILLISECONDS) ||
                    minDistance != Float
                            .valueOf(getString(R.string.pref_gps_min_distance_default_meters))) {
                Toast.makeText(this, String.format(getString(R.string.gps_set_location_listener),
                        String.valueOf((double) minTime / SECONDS_TO_MILLISECONDS),
                        String.valueOf(minDistance)), Toast.LENGTH_SHORT).show();
            }

            // Show the indeterminate progress bar on the action bar until first GPS status is shown
            setSupportProgressBarIndeterminateVisibility(Boolean.TRUE);

            // Reset the options menu to trigger updates to action bar menu items
            invalidateOptionsMenu();
        }
        for (GpsTestListener listener : mGpsTestListeners) {
            listener.gpsStart();
        }
    }

    private synchronized void gpsStop() {
        if (mLocationManager == null) {
            return;
        }
        if (mStarted) {
            mLocationManager.removeUpdates(this);
            mStarted = false;
            // Stop progress bar
            setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

            // Reset the options menu to trigger updates to action bar menu items
            invalidateOptionsMenu();
        }
        for (GpsTestListener listener : mGpsTestListeners) {
            listener.gpsStop();
        }
    }

    private boolean sendExtraCommand(String command) {
        return mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, command, null);
    }

    private void addOrientationSensorListener() {
        if (SatelliteUtils.isRotationVectorSensorSupported(this)) {
            // Use the modern rotation vector sensors
            Sensor vectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, vectorSensor, 16000); // ~60hz
        } else {
            // Use the legacy orientation sensors
            Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (sensor != null) {
                mSensorManager.registerListener(this, sensor,
                        SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    private void addStatusListener() {
        SharedPreferences settings = Application.getPrefs();
        boolean useGnssApis = settings.getBoolean(getString(R.string.pref_key_use_gnss_apis), true);

        if (SatelliteUtils.isGnssStatusListenerSupported() && useGnssApis) {
            addGnssStatusListener();
        } else {
            addLegacyStatusListener();
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.N)
    private void addGnssStatusListener() {
        mGnssStatusListener = new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onGnssStarted();
                }
            }

            @Override
            public void onStopped() {
                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onGnssStopped();
                }
            }

            @Override
            public void onFirstFix(int ttffMillis) {
                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onGnssFirstFix(ttffMillis);
                }
            }

            @Override
            public void onSatelliteStatusChanged(GnssStatus status) {
                mGnssStatus = status;

                // Stop progress bar after the first status information is obtained
                setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);

                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onSatelliteStatusChanged(mGnssStatus);
                }
            }
        };
        mLocationManager.registerGnssStatusCallback(mGnssStatusListener);
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addGnssMeasurementsListener() {
        mGnssMeasurementsListener = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onGnssMeasurementsReceived(event);
                }
                if (mWriteRawMeasurementToAndroidMonitor) {
                    for (GnssMeasurement m : event.getMeasurements()) {
                        writeGnssMeasurementToAndroidStudio(m);
                    }
                }
                if (mWriteRawMeasurementsToFile &&
                        PermissionUtils.hasGrantedFileWritePermission(GpsTestActivity.this)) {
                    mFileLogger.onGnssMeasurementsReceived(event);
                }
            }

            @Override
            public void onStatusChanged(int status) {
                final String statusMessage;
                switch (status) {
                    case STATUS_LOCATION_DISABLED:
                        statusMessage = getString(R.string.gnss_measurement_status_loc_disabled);
                        PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_raw_measurements), PreferenceUtils.CAPABILITY_LOCATION_DISABLED);
                        break;
                    case STATUS_NOT_SUPPORTED:
                        statusMessage = getString(R.string.gnss_measurement_status_not_supported);
                        PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_raw_measurements), PreferenceUtils.CAPABILITY_NOT_SUPPORTED);
                        break;
                    case STATUS_READY:
                        statusMessage = getString(R.string.gnss_measurement_status_ready);
                        PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_raw_measurements), PreferenceUtils.CAPABILITY_SUPPORTED);
                        break;
                    default:
                        statusMessage = getString(R.string.gnss_status_unknown);
                        PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_raw_measurements), PreferenceUtils.CAPABILITY_UNKNOWN);
                }
                Log.d(TAG, "GnssMeasurementsEvent.Callback.onStatusChanged() - " + statusMessage);
                if (UIUtils.canManageDialog(GpsTestActivity.this)) {
                    new Handler(Looper.getMainLooper()).postDelayed(
                            () -> runOnUiThread(() ->
                                    Toast.makeText(GpsTestActivity.this, statusMessage, Toast.LENGTH_SHORT).show()), 3000);
                }
            }
        };
        mLocationManager.registerGnssMeasurementsCallback(mGnssMeasurementsListener);
    }

    @SuppressLint("MissingPermission")
    private void addLegacyStatusListener() {
        mLegacyStatusListener = new GpsStatus.Listener() {
            @SuppressLint("MissingPermission")
            @Override
            public void onGpsStatusChanged(int event) {
                mLegacyStatus = mLocationManager.getGpsStatus(mLegacyStatus);

                switch (event) {
                    case GpsStatus.GPS_EVENT_STARTED:
                        break;
                    case GpsStatus.GPS_EVENT_STOPPED:
                        break;
                    case GpsStatus.GPS_EVENT_FIRST_FIX:
                        break;
                    case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                        // Stop progress bar after the first status information is obtained
                        setSupportProgressBarIndeterminateVisibility(Boolean.FALSE);
                        break;
                }

                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onGpsStatusChanged(event, mLegacyStatus);
                }
            }
        };
        mLocationManager.addGpsStatusListener(mLegacyStatusListener);
    }

    private void removeStatusListener() {
        SharedPreferences settings = Application.getPrefs();
        boolean useGnssApis = settings.getBoolean(getString(R.string.pref_key_use_gnss_apis), true);

        if (SatelliteUtils.isGnssStatusListenerSupported() && useGnssApis) {
            removeGnssStatusListener();
        } else {
            removeLegacyStatusListener();
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private void removeGnssStatusListener() {
        if (mLocationManager != null) {
            mLocationManager.unregisterGnssStatusCallback(mGnssStatusListener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void removeGnssMeasurementsListener() {
        if (mLocationManager != null && mGnssMeasurementsListener != null) {
            mLocationManager.unregisterGnssMeasurementsCallback(mGnssMeasurementsListener);
        }
    }

    private void removeLegacyStatusListener() {
        if (mLocationManager != null && mLegacyStatusListener != null) {
            mLocationManager.removeGpsStatusListener(mLegacyStatusListener);
        }
    }

    private void addNmeaListener() {
        if (SatelliteUtils.isGnssStatusListenerSupported()) {
            addNmeaListenerAndroidN();
        } else {
            addLegacyNmeaListener();
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addNmeaListenerAndroidN() {
        if (mOnNmeaMessageListener == null) {
            mOnNmeaMessageListener = (message, timestamp) -> {
                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onNmeaMessage(message, timestamp);
                }
                if (mWriteNmeaToAndroidMonitor) {
                    writeNmeaToAndroidStudio(message,
                            mWriteNmeaTimestampToAndroidMonitor ? timestamp : Long.MIN_VALUE);
                }
                if (mWriteNmeaToFile &&
                        PermissionUtils.hasGrantedFileWritePermission(GpsTestActivity.this)) {
                    mFileLogger.onNmeaReceived(timestamp, message);
                }
                PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_nmea), PreferenceUtils.CAPABILITY_SUPPORTED);
            };
        }
        mLocationManager.addNmeaListener(mOnNmeaMessageListener);
    }

    @SuppressLint("MissingPermission")
    private void addLegacyNmeaListener() {
        if (mLegacyNmeaListener == null) {
            mLegacyNmeaListener = (timestamp, nmea) -> {
                for (GpsTestListener listener : mGpsTestListeners) {
                    listener.onNmeaMessage(nmea, timestamp);
                }
                if (mWriteNmeaToAndroidMonitor) {
                    writeNmeaToAndroidStudio(nmea, mWriteNmeaTimestampToAndroidMonitor ? timestamp : Long.MIN_VALUE);
                }
                if (mWriteNmeaToFile &&
                        PermissionUtils.hasGrantedFileWritePermission(GpsTestActivity.this)) {
                    mFileLogger.onNmeaReceived(timestamp, nmea);
                }
                PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_nmea), PreferenceUtils.CAPABILITY_SUPPORTED);
            };
        }
        mLocationManager.addNmeaListener(mLegacyNmeaListener);
    }

    private void removeNmeaListener() {
        if (SatelliteUtils.isGnssStatusListenerSupported()) {
            if (mLocationManager != null && mOnNmeaMessageListener != null) {
                mLocationManager.removeNmeaListener(mOnNmeaMessageListener);
            }
        } else {
            if (mLocationManager != null && mLegacyNmeaListener != null) {
                mLocationManager.removeNmeaListener(mLegacyNmeaListener);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void addNavMessageListener() {
        if (mGnssNavMessageListener == null) {
            mGnssNavMessageListener = new GnssNavigationMessage.Callback() {
                @Override
                public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                    if (mWriteNavMessageToAndroidMonitor) {
                        writeNavMessageToAndroidStudio(event);
                    }
                    if (mWriteNavMessageToFile &&
                            PermissionUtils.hasGrantedFileWritePermission(GpsTestActivity.this)) {
                        mFileLogger.onGnssNavigationMessageReceived(event);
                    }
                }

                @Override
                public void onStatusChanged(int status) {
                    final String statusMessage;
                    switch (status) {
                        case STATUS_LOCATION_DISABLED:
                            statusMessage = getString(R.string.gnss_nav_msg_status_loc_disabled);
                            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_nav_messages), PreferenceUtils.CAPABILITY_LOCATION_DISABLED);
                            break;
                        case STATUS_NOT_SUPPORTED:
                            statusMessage = getString(R.string.gnss_nav_msg_status_not_supported);
                            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_nav_messages), PreferenceUtils.CAPABILITY_NOT_SUPPORTED);
                            break;
                        case STATUS_READY:
                            statusMessage = getString(R.string.gnss_nav_msg_status_ready);
                            PreferenceUtils.saveInt(Application.get().getString(R.string.capability_key_nav_messages), PreferenceUtils.CAPABILITY_SUPPORTED);
                            break;
                        default:
                            statusMessage = getString(R.string.gnss_status_unknown);
                    }
                    Log.d(TAG, "GnssNavigationMessage.Callback.onStatusChanged() - " + statusMessage);
                    if (UIUtils.canManageDialog(GpsTestActivity.this)) {
                        // Delay this toast so it's not overwritten by other toasts
                        new Handler(Looper.getMainLooper()).postDelayed(
                                () -> runOnUiThread(() ->
                                        Toast.makeText(GpsTestActivity.this, statusMessage, Toast.LENGTH_SHORT).show()), 2000);
                    }
                }
            };
        }
        mLocationManager.registerGnssNavigationMessageCallback(mGnssNavMessageListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void removeNavMessageListener() {
        if (mLocationManager != null && mGnssNavMessageListener != null) {
            mLocationManager.unregisterGnssNavigationMessageCallback(mGnssNavMessageListener);
        }
    }

    /**
     * Ask the user if they want to enable GPS
     */
    private void promptEnableGps() {
        new AlertDialog.Builder(this)
                .setMessage(getString(R.string.enable_gps_message))
                .setPositiveButton(getString(R.string.enable_gps_positive_button),
                        (dialog, which) -> {
                            Intent intent = new Intent(
                                    Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                )
                .setNegativeButton(getString(R.string.enable_gps_negative_button),
                        (dialog, which) -> {
                        }
                )
                .show();
    }

    @SuppressLint("MissingPermission")
    private void checkTimeAndDistance(SharedPreferences settings) {
        double tempMinTimeDouble = Double
                .valueOf(settings.getString(getString(R.string.pref_key_gps_min_time), "1"));
        long minTimeLong = (long) (tempMinTimeDouble * SECONDS_TO_MILLISECONDS);

        if (minTime != minTimeLong ||
                minDistance != Float.valueOf(
                        settings.getString(getString(R.string.pref_key_gps_min_distance), "0"))) {
            // User changed preference values, get the new ones
            minTime = minTimeLong;
            minDistance = Float.valueOf(
                    settings.getString(getString(R.string.pref_key_gps_min_distance), "0"));
            // If the GPS is started, reset the location listener with the new values
            if (mStarted && mProvider != null) {
                mLocationManager
                        .requestLocationUpdates(mProvider.getName(), minTime, minDistance, this);
                Toast.makeText(this, String.format(getString(R.string.gps_set_location_listener),
                        String.valueOf(tempMinTimeDouble), String.valueOf(minDistance)),
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void checkKeepScreenOn(SharedPreferences settings) {
//        if (!mIsLargeScreen) {
            if (settings.getBoolean(getString(R.string.pref_key_keep_screen_on), true)) {
                mToolbar.setKeepScreenOn(true);
            } else {
                mToolbar.setKeepScreenOn(false);
            }
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

    private void checkTrueNorth(SharedPreferences settings) {
        mFaceTrueNorth = settings.getBoolean(getString(R.string.pref_key_true_north), true);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkGnssMeasurementOutput(SharedPreferences settings) {
        mWriteRawMeasurementToAndroidMonitor = settings
                .getBoolean(getString(R.string.pref_key_as_measurement_output), false);
        mWriteRawMeasurementsToFile = settings.getBoolean(getString(R.string.pref_key_file_measurement_output), false);

        if (mWriteRawMeasurementToAndroidMonitor || mWriteRawMeasurementsToFile) {
            addGnssMeasurementsListener();
        }
    }

    private void checkNmeaOutput(SharedPreferences settings) {
        mWriteNmeaToAndroidMonitor = settings.getBoolean(getString(R.string.pref_key_as_nmea_output), true);
        mWriteNmeaTimestampToAndroidMonitor = settings
                .getBoolean(getString(R.string.pref_key_as_nmea_timestamp_output), true);
        mWriteNmeaToFile = settings
                .getBoolean(getString(R.string.pref_key_file_nmea_output), false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void checkNavMessageOutput(SharedPreferences settings) {
        mWriteNavMessageToAndroidMonitor = settings
                .getBoolean(getString(R.string.pref_key_as_navigation_message_output), false);
        mWriteNavMessageToFile = settings
                .getBoolean(getString(R.string.pref_key_file_navigation_message_output), false);

        if (mWriteNavMessageToAndroidMonitor || mWriteNavMessageToFile) {
            addNavMessageListener();
        } else {
            removeNavMessageListener();
        }
    }

    private void checkLocationOutput(SharedPreferences settings) {
        mWriteLocationToFile = settings
                .getBoolean(getString(R.string.pref_key_file_location_output), false);
    }

    @Override
    protected void onDestroy() {
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(this);
        }
        mFileLogger.close();
        super.onDestroy();
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
                        } else {
                            if (mSwitch.isChecked() && !mStarted) {
                                gpsStart();
                            }
                        }
                    }
                });
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item;

        item = menu.findItem(R.id.share);
        if (item != null) {
            item.setVisible(mLastLocation != null || isFileLoggingEnabled());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean success;
        // Handle menu item selection
        switch (item.getItemId()) {
            case R.id.gps_switch:
                return true;
            case R.id.share:
                share();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onLocationChanged(Location location) {
        mLastLocation = location;

        updateGeomagneticField();

        // Reset the options menu to trigger updates to action bar menu items
        invalidateOptionsMenu();

        for (GpsTestListener listener : mGpsTestListeners) {
            listener.onLocationChanged(location);
        }
        if (mWriteLocationToFile &&
                PermissionUtils.hasGrantedFileWritePermission(GpsTestActivity.this)) {
            mFileLogger.onLocationChanged(location);
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        for (GpsTestListener listener : mGpsTestListeners) {
            listener.onStatusChanged(provider, status, extras);
        }
    }

    public void onProviderEnabled(String provider) {
        for (GpsTestListener listener : mGpsTestListeners) {
            listener.onProviderEnabled(provider);
        }
    }

    public void onProviderDisabled(String provider) {
        for (GpsTestListener listener : mGpsTestListeners) {
            listener.onProviderDisabled(provider);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @Override
    public void onSensorChanged(SensorEvent event) {

        double orientation = Double.NaN;
        double tilt = Double.NaN;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ROTATION_VECTOR:
                // Modern rotation vector sensors
                if (!mTruncateVector) {
                    try {
                        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                    } catch (IllegalArgumentException e) {
                        // On some Samsung devices, an exception is thrown if this vector > 4 (see #39)
                        // Truncate the array, since we can deal with only the first four values
                        Log.e(TAG, "Samsung device error? Will truncate vectors - " + e);
                        mTruncateVector = true;
                        // Do the truncation here the first time the exception occurs
                        getRotationMatrixFromTruncatedVector(event.values);
                    }
                } else {
                    // Truncate the array to avoid the exception on some devices (see #39)
                    getRotationMatrixFromTruncatedVector(event.values);
                }

                int rot = getWindowManager().getDefaultDisplay().getRotation();
                switch (rot) {
                    case Surface.ROTATION_0:
                        // No orientation change, use default coordinate system
                        SensorManager.getOrientation(mRotationMatrix, mValues);
                        // Log.d(MODE_MAP, "Rotation-0");
                        break;
                    case Surface.ROTATION_90:
                        // Log.d(MODE_MAP, "Rotation-90");
                        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_Y,
                                SensorManager.AXIS_MINUS_X, mRemappedMatrix);
                        SensorManager.getOrientation(mRemappedMatrix, mValues);
                        break;
                    case Surface.ROTATION_180:
                        // Log.d(MODE_MAP, "Rotation-180");
                        SensorManager
                                .remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_X,
                                        SensorManager.AXIS_MINUS_Y, mRemappedMatrix);
                        SensorManager.getOrientation(mRemappedMatrix, mValues);
                        break;
                    case Surface.ROTATION_270:
                        // Log.d(MODE_MAP, "Rotation-270");
                        SensorManager
                                .remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_Y,
                                        SensorManager.AXIS_X, mRemappedMatrix);
                        SensorManager.getOrientation(mRemappedMatrix, mValues);
                        break;
                    default:
                        // This shouldn't happen - assume default orientation
                        SensorManager.getOrientation(mRotationMatrix, mValues);
                        // Log.d(MODE_MAP, "Rotation-Unknown");
                        break;
                }
                orientation = Math.toDegrees(mValues[0]);  // azimuth
                tilt = Math.toDegrees(mValues[1]);
                break;
            case Sensor.TYPE_ORIENTATION:
                // Legacy orientation sensors
                orientation = event.values[0];
                break;
            default:
                // A sensor we're not using, so return
                return;
        }

        // Correct for true north, if preference is set
        if (mFaceTrueNorth && mGeomagneticField != null) {
            orientation += mGeomagneticField.getDeclination();
            // Make sure value is between 0-360
            orientation = MathUtils.mod((float) orientation, 360.0f);
        }

        for (GpsTestListener listener : mGpsTestListeners) {
            listener.onOrientationChanged(orientation, tilt);
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private void getRotationMatrixFromTruncatedVector(float[] vector) {
        System.arraycopy(vector, 0, mTruncatedRotationVector, 0, 4);
        SensorManager.getRotationMatrixFromVector(mRotationMatrix, mTruncatedRotationVector);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void updateGeomagneticField() {
        mGeomagneticField = new GeomagneticField((float) mLastLocation.getLatitude(),
                (float) mLastLocation.getLongitude(), (float) mLastLocation.getAltitude(),
                mLastLocation.getTime());
    }

    private void share() {
        final Location location = mLastLocation;
        shareDialogOpen = true;
        UIUtils.createShareDialog(this, location, isFileLoggingEnabled(), mFileLogger, null).show();
    }

    /**
     * Show the "What's New" message if a new version was just installed
     */
    @SuppressWarnings("deprecation")
    private void autoShowWhatsNew() {
        SharedPreferences settings = Application.getPrefs();

        // Get the current app version.
        PackageManager pm = getPackageManager();
        PackageInfo appInfo = null;
        try {
            appInfo = pm.getPackageInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            // Do nothing
            return;
        }

        final int oldVer = settings.getInt(WHATS_NEW_VER, 0);
        final int newVer = appInfo.versionCode;

        if (oldVer < newVer) {
            showDialog(WHATSNEW_DIALOG);
            PreferenceUtils.saveInt(WHATS_NEW_VER, appInfo.versionCode);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case WHATSNEW_DIALOG:
                return createWhatsNewDialog();
            case HELP_DIALOG:
                return createHelpDialog();
            case CLEAR_ASSIST_WARNING_DIALOG:
                return createClearAssistWarningDialog();
        }
        return super.onCreateDialog(id);
    }

    @SuppressWarnings("deprecation")
    private Dialog createWhatsNewDialog() {
        TextView textView = (TextView) getLayoutInflater().inflate(R.layout.whats_new_dialog, null);
        textView.setText(R.string.main_help_whatsnew);

        AlertDialog.Builder builder
                = new AlertDialog.Builder(this);
        builder.setTitle(R.string.main_help_whatsnew_title);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setView(textView);
        builder.setNeutralButton(R.string.main_help_close,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dismissDialog(WHATSNEW_DIALOG);
                    }
                }
        );
        return builder.create();
    }

    @SuppressWarnings("deprecation")
    private Dialog createHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_help);
        int options = R.array.main_help_options;
        builder.setItems(options,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                showDialog(WHATSNEW_DIALOG);
                                break;
                            case 1:
                                startActivity(new Intent(GpsTestActivity.getInstance(), HelpActivity.class));
                                break;
                        }
                    }
                }
        );
        return builder.create();
    }

    @SuppressWarnings("deprecation")
    private Dialog createClearAssistWarningDialog() {
        View view = getLayoutInflater().inflate(R.layout.clear_assist_warning, null);
        CheckBox neverShowDialog = view.findViewById(R.id.clear_assist_never_ask_again);

        neverShowDialog.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // Save the preference
                PreferenceUtils.saveBoolean(getString(R.string.pref_key_never_show_clear_assist_warning), isChecked);
            }
        });

        Drawable icon = getResources().getDrawable(R.drawable.ic_delete);
        DrawableCompat.setTint(icon, getResources().getColor(R.color.colorPrimary));

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.clear_assist_warning_title)
                .setIcon(icon)
                .setCancelable(false)
                .setView(view)
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteAidingData();
                            }
                        }
                )
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // No-op
                            }
                        }
                );
        return builder.create();
    }

    /**
     * Shows the dialog to explain why location permissions are needed
     *
     * NOTE - this dialog can't be managed under the old dialog framework as the method
     * ActivityCompat.shouldShowRequestPermissionRationale() always returns false.
     */
    private void showLocationPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.title_location_permission)
                .setMessage(R.string.text_location_permission)
                .setCancelable(false)
                .setPositiveButton(R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Request permissions from the user
                                ActivityCompat.requestPermissions(mActivity, REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST);
                            }
                        }
                )
            .setNegativeButton(R.string.exit,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Exit app
                        finish();
                    }
                }
        );
        builder.create().show();
    }
}
