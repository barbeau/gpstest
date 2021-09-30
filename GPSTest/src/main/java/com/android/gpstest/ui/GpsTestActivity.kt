/*
 * Copyright (C) 2008-2021 The Android Open Source Project,
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
package com.android.gpstest.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.Application
import com.android.gpstest.ForegroundOnlyLocationService
import com.android.gpstest.ForegroundOnlyLocationService.LocalBinder
import com.android.gpstest.GpsMapFragment
import com.android.gpstest.R
import com.android.gpstest.data.FirstFixState
import com.android.gpstest.data.FixState
import com.android.gpstest.data.LocationRepository
import com.android.gpstest.databinding.ActivityMainBinding
import com.android.gpstest.map.MapConstants
import com.android.gpstest.ui.NavigationDrawerFragment.NavigationDrawerCallbacks
import com.android.gpstest.util.*
import com.android.gpstest.util.PreferenceUtils.isTrackingStarted
import com.android.gpstest.util.SharedPreferenceUtil.isFileLoggingEnabled
import com.android.gpstest.util.SharedPreferenceUtil.minDistance
import com.android.gpstest.util.SharedPreferenceUtil.minTimeMillis
import com.android.gpstest.util.SharedPreferenceUtil.runInBackground
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.zxing.integration.android.IntentIntegrator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GpsTestActivity : AppCompatActivity(), NavigationDrawerCallbacks {
    private lateinit var binding: ActivityMainBinding

    private var useDarkTheme = false

    /**
     * Currently selected navigation drawer position (so we don't unnecessarily swap fragments
     * if the same item is selected).  Initialized to -1 so the initial callback from
     * NavigationDrawerFragment always instantiates the fragments
     */
    private var currentNavDrawerPosition = -1

    //
    // Fragments controlled by the nav drawer
    //
    private var statusFragment: GpsStatusFragment? = null
    private var mapFragment: GpsMapFragment? = null
    private var skyFragment: GpsSkyFragment? = null
    private var accuracyFragment: GpsMapFragment? = null
    var gpsResume = false
    private var switch // GPS on/off switch
            : SwitchMaterial? = null
    private var lastLocation: Location? = null
    var lastSavedInstanceState: Bundle? = null
    private var userDeniedPermission = false
    private var benchmarkController: BenchmarkController? = null
    private var initialLanguage: String? = null
    private var shareDialogOpen = false
    private var progressBar: ProgressBar? = null
    var deviceInfoViewModel: DeviceInfoViewModel? = null
    private var isServiceBound = false
    private var service: ForegroundOnlyLocationService? = null
    private var foregroundOnlyServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder = iBinder as LocalBinder
            service = binder.service
            isServiceBound = true
            if (locationFlow?.isActive == true) {
                // Activity started location updates but service wasn't bound yet - tell service to start now
                service?.subscribeToLocationUpdates()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            service = null
            isServiceBound = false
        }
    }

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null

    // Preference listener that will cancel the above flows when the user turns off tracking via service notification
    private val stopTrackingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        SharedPreferenceUtil.newStopTrackingListener { gpsStop() }

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme
        if (Application.prefs.getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            setTheme(R.style.AppTheme_Dark_NoActionBar)
            useDarkTheme = true
        }
        super.onCreate(savedInstanceState)
        // Reset the activity title to make sure dynamic locale changes are shown
        UIUtils.resetActivityTitle(this)
        saveInstanceState(savedInstanceState)

        // Observe stopping location updates from the service
        Application.prefs.registerOnSharedPreferenceChangeListener(stopTrackingListener)

        // Set the default values from the XML file if this is the first execution of the app
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        initialLanguage = PreferenceUtils.getString(getString(R.string.pref_key_language))

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        benchmarkController = BenchmarkControllerImpl(this, findViewById(R.id.mainlayout))

        // Set initial Benchmark view visibility here - we can't do it before setContentView() b/c views aren't inflated yet
        if (accuracyFragment != null && currentNavDrawerPosition == NavigationDrawerFragment.NAVDRAWER_ITEM_ACCURACY) {
            initAccuracy()
        } else {
            (benchmarkController as BenchmarkControllerImpl).hide()
        }
        setSupportActionBar(binding.toolbar)
        progressBar = findViewById(R.id.progress_horizontal)
        setupNavigationDrawer()
        deviceInfoViewModel = ViewModelProviders.of(this).get(
            DeviceInfoViewModel::class.java
        )
        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, BIND_AUTO_CREATE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // If another app is passing in a ground truth location, recreate the activity to initialize an existing instance
        if (IOUtils.isShowRadarIntent(intent) || IOUtils.isGeoIntent(intent)) {
            recreateApp(intent)
        }
    }

    /**
     * Save instance state locally so we can use it after the permission callback
     * @param savedInstanceState instance state to save
     */
    private fun saveInstanceState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            lastSavedInstanceState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                savedInstanceState.deepCopy()
            } else {
                savedInstanceState
            }
        }
    }

    private fun setupNavigationDrawer() {
        // Fragment managing the behaviors, interactions and presentation of the navigation drawer.
        val navDrawerFragment =
            supportFragmentManager.findFragmentById(R.id.navigation_drawer) as NavigationDrawerFragment?

        // Set up the drawer.
        navDrawerFragment!!.setUp(
            R.id.navigation_drawer,
            binding.navDrawerLeftPane
        )
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        // Save GPS resume state
        outState.putBoolean(GPS_RESUME, gpsResume)
        //         if (service.csvFileLogger.isStarted() && !shareDialogOpen) {
//             outState.putSerializable(EXISTING_CSV_LOG_FILE, service.csvFileLogger.getFile());
//         }
//         if (service.jsonFileLogger.isStarted() && !shareDialogOpen) {
//             outState.putSerializable(EXISTING_JSON_LOG_FILE, service.jsonFileLogger.getFile());
//         }
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        shareDialogOpen = false
        if (!userDeniedPermission) {
            requestPermissionAndInit(this)
        } else {
            // Explain permission to user (don't request permission here directly to avoid infinite
            // loop if user selects "Don't ask again") in system permission prompt
            UIUtils.showLocationPermissionDialog(this)
        }
        // If the set language has changed since we created the Activity (e.g., returning from Settings), recreate Activity
        if (Application.prefs.contains(getString(R.string.pref_key_language))) {
            val currentLanguage = PreferenceUtils.getString(getString(R.string.pref_key_language))
            if (currentLanguage != initialLanguage) {
                initialLanguage = currentLanguage
                recreateApp(null)
            }
        }
        benchmarkController!!.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UIUtils.PICKFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            // User picked a file to share from the Share dialog - update the dialog
            if (data != null) {
                val uri = data.data
                Log.i(TAG, "Uri: " + uri.toString())
                val location = lastLocation
                shareDialogOpen = true
                UIUtils.showShareFragmentDialog(
                    this, location, isFileLoggingEnabled(),
                    service!!.csvFileLogger, service!!.jsonFileLogger, uri
                )
            }
        } else {
            // See if this result was a scanned QR Code with a ground truth location
            val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (scanResult != null) {
                val geoUri = scanResult.contents
                val l = IOUtils.getLocationFromGeoUri(geoUri)
                if (l != null) {
                    l.removeAltitude() // TODO - RFC 5870 requires altitude height above geoid, which we can't support yet (see #296 and #530), so remove altitude here
                    // Create a SHOW_RADAR intent out of the Geo URI and pass that to set ground truth
                    val showRadar = IOUtils.createShowRadarIntent(l)
                    recreateApp(showRadar)
                } else {
                    Toast.makeText(
                        this, getString(R.string.qr_code_cannot_read_code),
                        Toast.LENGTH_LONG
                    ).show()
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
    fun recreateApp(currentIntent: Intent?) {
        val i = Intent(this, GpsTestActivity::class.java)
        if (IOUtils.isShowRadarIntent(currentIntent)) {
            // If we're creating the app because we got a SHOW_RADAR intent, copy over the intent action and extras
            i.action = currentIntent!!.action
            i.putExtras(currentIntent.extras!!)
        } else if (IOUtils.isGeoIntent(currentIntent)) {
            // If we're creating the app because we got a geo: intent, turn it into a SHOW_RADAR intent for simplicity (they are used the same way)
            val l = IOUtils.getLocationFromGeoUri(
                currentIntent!!.data.toString()
            )
            if (l != null) {
                val showRadarIntent = IOUtils.createShowRadarIntent(l)
                i.action = showRadarIntent.action
                i.putExtras(showRadarIntent.extras!!)
            }
        }
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        // Restart process to destroy and recreate everything
        System.exit(0)
    }

    override fun attachBaseContext(base: Context) {
        // For dynamically changing the locale
        super.attachBaseContext(Application.localeManager.setLocale(base))
    }

    private fun initAccuracy() {
        accuracyFragment!!.setOnMapClickListener { location: Location? ->
            benchmarkController!!.onMapClick(
                location
            )
        }
        benchmarkController!!.show()
    }

    private fun requestPermissionAndInit(activity: Activity) {
        if (PermissionUtils.hasGrantedPermissions(activity, PermissionUtils.REQUIRED_PERMISSIONS)) {
            init()
        } else {
            // Request permissions from the user
            ActivityCompat.requestPermissions(
                activity,
                PermissionUtils.REQUIRED_PERMISSIONS,
                PermissionUtils.LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.LOCATION_PERMISSION_REQUEST) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                userDeniedPermission = false
                init()
            } else {
                userDeniedPermission = true
            }
        }
    }

    private fun init() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val provider = locationManager.getProvider(LocationManager.GPS_PROVIDER)
        if (provider == null) {
            Log.e(TAG, "Unable to get GPS_PROVIDER")
            Toast.makeText(
                this, getString(R.string.gps_not_supported),
                Toast.LENGTH_SHORT
            ).show()
        }
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            UIUtils.promptEnableGps(this)
        }
        setupStartState(lastSavedInstanceState)

        // If the theme has changed (e.g., from Preferences), destroy and recreate to reflect change
        val useDarkTheme =
            Application.prefs.getBoolean(getString(R.string.pref_key_dark_theme), false)
        if (this.useDarkTheme != useDarkTheme) {
            this.useDarkTheme = useDarkTheme
            recreate()
        }
        val settings = Application.prefs
        checkKeepScreenOn(settings)
        UIUtils.autoShowWhatsNew(this)
    }

    override fun onPause() {
        gpsResume = if (isTrackingStarted()) {
            gpsStop()
            // If GPS was started, we want to resume it after orientation change
            true
        } else {
            false
        }
        // Stop the service if this isn't a configuration change and the user hasn't opted to run in background
        if (!isChangingConfigurations && !runInBackground()) {
            service?.unsubscribeToLocationUpdates()
        }
        super.onPause()
    }

    private fun setupStartState(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // Activity is being restarted and has previous state (e.g., user rotated device)
            val gpsResume = savedInstanceState.getBoolean(GPS_RESUME, true)
            if (gpsResume) {
                gpsStart()
            }
        } else {
            // Activity is starting without previous state - use "Auto-start GNSS" setting, or gpsResume (e.g., if app was backgrounded via Home button)
            if (Application.prefs.getBoolean(
                    getString(R.string.pref_key_auto_start_gps),
                    true
                ) || gpsResume
            ) {
                gpsStart()
            }
        }
    }

    override fun onNavigationDrawerItemSelected(position: Int) {
        goToNavDrawerItem(position)
    }

    private fun goToNavDrawerItem(item: Int) {
        // Update the main content by replacing fragments
        when (item) {
            NavigationDrawerFragment.NAVDRAWER_ITEM_STATUS -> if (currentNavDrawerPosition != NavigationDrawerFragment.NAVDRAWER_ITEM_STATUS) {
                showStatusFragment()
                currentNavDrawerPosition = item
            }
            NavigationDrawerFragment.NAVDRAWER_ITEM_MAP -> if (currentNavDrawerPosition != NavigationDrawerFragment.NAVDRAWER_ITEM_MAP) {
                showMapFragment()
                currentNavDrawerPosition = item
            }
            NavigationDrawerFragment.NAVDRAWER_ITEM_SKY -> if (currentNavDrawerPosition != NavigationDrawerFragment.NAVDRAWER_ITEM_SKY) {
                showSkyFragment()
                currentNavDrawerPosition = item
            }
            NavigationDrawerFragment.NAVDRAWER_ITEM_ACCURACY -> if (currentNavDrawerPosition != NavigationDrawerFragment.NAVDRAWER_ITEM_ACCURACY) {
                showAccuracyFragment()
                currentNavDrawerPosition = item
            }
            NavigationDrawerFragment.NAVDRAWER_ITEM_INJECT_PSDS_DATA -> forcePsdsInjection()
            NavigationDrawerFragment.NAVDRAWER_ITEM_INJECT_TIME_DATA -> forceTimeInjection()
            NavigationDrawerFragment.NAVDRAWER_ITEM_CLEAR_AIDING_DATA -> {
                val prefs = Application.prefs
                if (!prefs.getBoolean(
                        getString(R.string.pref_key_never_show_clear_assist_warning),
                        false
                    )
                ) {
                    showDialog(UIUtils.CLEAR_ASSIST_WARNING_DIALOG)
                } else {
                    deleteAidingData()
                }
            }
            NavigationDrawerFragment.NAVDRAWER_ITEM_SETTINGS -> startActivity(
                Intent(
                    this,
                    Preferences::class.java
                )
            )
            NavigationDrawerFragment.NAVDRAWER_ITEM_HELP -> showDialog(UIUtils.HELP_DIALOG)
            NavigationDrawerFragment.NAVDRAWER_ITEM_OPEN_SOURCE -> {
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(getString(R.string.open_source_github))
                startActivity(i)
            }
            NavigationDrawerFragment.NAVDRAWER_ITEM_SEND_FEEDBACK -> {
                // Send App feedback
                val email = getString(R.string.app_feedback_email)
                var locationString: String? = null
                if (lastLocation != null) {
                    locationString = LocationUtils.printLocationDetails(lastLocation)
                }
                UIUtils.sendEmail(this, email, locationString, deviceInfoViewModel)
            }
        }
        invalidateOptionsMenu()
    }

    private fun showStatusFragment() {
        val fm = supportFragmentManager
        // Hide everything that shouldn't be shown
        hideMapFragment()
        hideSkyFragment()
        hideAccuracyFragment()
        if (benchmarkController != null) {
            benchmarkController!!.hide()
        }

        // Show fragment (we use show instead of replace to keep the map state)
        if (statusFragment == null) {
            // First check to see if an instance of fragment already exists
            statusFragment = fm.findFragmentByTag(TAG) as GpsStatusFragment?
            if (statusFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsStatusFragment")
                statusFragment = GpsStatusFragment()
                fm.beginTransaction()
                    .add(R.id.fragment_container, statusFragment!!, TAG)
                    .commit()
            }
        }
        supportFragmentManager.beginTransaction().show(statusFragment!!).commit()
        title = resources.getString(R.string.gps_status_title)
    }

    private fun hideStatusFragment() {
        val fm = supportFragmentManager
        statusFragment = fm.findFragmentByTag(TAG) as GpsStatusFragment?
        if (statusFragment != null && !statusFragment!!.isHidden) {
            fm.beginTransaction().hide(statusFragment!!).commit()
        }
    }

    private fun showMapFragment() {
        val fm = supportFragmentManager
        // Hide everything that shouldn't be shown
        hideStatusFragment()
        hideSkyFragment()
        hideAccuracyFragment()
        if (benchmarkController != null) {
            benchmarkController!!.hide()
        }

        // Show fragment (we use show instead of replace to keep the map state)
        if (mapFragment == null) {
            // First check to see if an instance of fragment already exists
            mapFragment = fm.findFragmentByTag(MapConstants.MODE_MAP) as GpsMapFragment?
            if (mapFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsMapFragment")
                val bundle = Bundle()
                bundle.putString(MapConstants.MODE, MapConstants.MODE_MAP)
                mapFragment = GpsMapFragment()
                mapFragment!!.arguments = bundle
                fm.beginTransaction()
                    .add(R.id.fragment_container, mapFragment!!, MapConstants.MODE_MAP)
                    .commit()
            }
        }
        supportFragmentManager.beginTransaction().show(mapFragment!!).commit()
        title = resources.getString(R.string.gps_map_title)
    }

    private fun hideMapFragment() {
        val fm = supportFragmentManager
        mapFragment = fm.findFragmentByTag(MapConstants.MODE_MAP) as GpsMapFragment?
        if (mapFragment != null && !mapFragment!!.isHidden) {
            fm.beginTransaction().hide(mapFragment!!).commit()
        }
    }

    private fun showSkyFragment() {
        val fm = supportFragmentManager
        // Hide everything that shouldn't be shown
        hideStatusFragment()
        hideMapFragment()
        hideAccuracyFragment()
        if (benchmarkController != null) {
            benchmarkController!!.hide()
        }
        // Show fragment (we use show instead of replace to keep the map state)
        if (skyFragment == null) {
            // First check to see if an instance of fragment already exists
            skyFragment = fm.findFragmentByTag(GpsSkyFragment.TAG) as GpsSkyFragment?
            if (skyFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsStatusFragment")
                skyFragment = GpsSkyFragment()
                fm.beginTransaction()
                    .add(R.id.fragment_container, skyFragment!!, GpsSkyFragment.TAG)
                    .commit()
            }
        }
        supportFragmentManager.beginTransaction().show(skyFragment!!).commit()
        title = resources.getString(R.string.gps_sky_title)
    }

    private fun hideSkyFragment() {
        val fm = supportFragmentManager
        skyFragment = fm.findFragmentByTag(GpsSkyFragment.TAG) as GpsSkyFragment?
        if (skyFragment != null && !skyFragment!!.isHidden) {
            fm.beginTransaction().hide(skyFragment!!).commit()
        }
    }

    private fun showAccuracyFragment() {
        val fm = supportFragmentManager
        // Hide everything that shouldn't be shown
        hideStatusFragment()
        hideMapFragment()
        hideSkyFragment()
        // Show fragment (we use show instead of replace to keep the map state)
        if (accuracyFragment == null) {
            // First check to see if an instance of fragment already exists
            accuracyFragment = fm.findFragmentByTag(MapConstants.MODE_ACCURACY) as GpsMapFragment?
            if (accuracyFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new GpsMapFragment for Accuracy")
                val bundle = Bundle()
                bundle.putString(MapConstants.MODE, MapConstants.MODE_ACCURACY)
                accuracyFragment = GpsMapFragment()
                accuracyFragment!!.arguments = bundle
                fm.beginTransaction()
                    .add(R.id.fragment_container, accuracyFragment!!, MapConstants.MODE_ACCURACY)
                    .commit()
            }
        }
        supportFragmentManager.beginTransaction().show(accuracyFragment!!).commit()
        title = resources.getString(R.string.gps_accuracy_title)
        if (benchmarkController != null) {
            initAccuracy()
        }
    }

    private fun hideAccuracyFragment() {
        val fm = supportFragmentManager
        accuracyFragment = fm.findFragmentByTag(MapConstants.MODE_ACCURACY) as GpsMapFragment?
        if (accuracyFragment != null && !accuracyFragment!!.isHidden) {
            fm.beginTransaction().hide(accuracyFragment!!).commit()
        }
    }

    private fun forcePsdsInjection() {
        val success =
            IOUtils.forcePsdsInjection(getSystemService(LOCATION_SERVICE) as LocationManager)
        if (success) {
            Toast.makeText(
                this, getString(R.string.force_psds_injection_success),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                Application.app.getString(R.string.capability_key_inject_psds),
                PreferenceUtils.CAPABILITY_SUPPORTED
            )
        } else {
            Toast.makeText(
                this, getString(R.string.force_psds_injection_failure),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                Application.app.getString(R.string.capability_key_inject_psds),
                PreferenceUtils.CAPABILITY_NOT_SUPPORTED
            )
        }
    }

    private fun forceTimeInjection() {
        val success =
            IOUtils.forceTimeInjection(getSystemService(LOCATION_SERVICE) as LocationManager)
        if (success) {
            Toast.makeText(
                this, getString(R.string.force_time_injection_success),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                Application.app.getString(R.string.capability_key_inject_time),
                PreferenceUtils.CAPABILITY_SUPPORTED
            )
        } else {
            Toast.makeText(
                this, getString(R.string.force_time_injection_failure),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                Application.app.getString(R.string.capability_key_inject_time),
                PreferenceUtils.CAPABILITY_NOT_SUPPORTED
            )
        }
    }

    @ExperimentalCoroutinesApi
    private fun deleteAidingData() {
        // If GPS is currently running, stop it
        val lastStartState = isTrackingStarted()
        if (isTrackingStarted()) {
            gpsStop()
        }
        val success =
            IOUtils.deleteAidingData(getSystemService(LOCATION_SERVICE) as LocationManager)
        if (success) {
            Toast.makeText(
                this, getString(R.string.delete_aiding_data_success),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                Application.app.getString(R.string.capability_key_delete_assist),
                PreferenceUtils.CAPABILITY_SUPPORTED
            )
        } else {
            Toast.makeText(
                this, getString(R.string.delete_aiding_data_failure),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                Application.app.getString(R.string.capability_key_delete_assist),
                PreferenceUtils.CAPABILITY_NOT_SUPPORTED
            )
        }
        // Restart the GPS, if it was previously started, with a slight delay,
        // to refresh the assistance data
        if (lastStartState) {
            lifecycleScope.launch {
                delay(500)
                gpsStart()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.navDrawerLeftPane.isDrawerOpen(GravityCompat.START)) {
            // Close navigation drawer
            binding.navDrawerLeftPane.closeDrawer(GravityCompat.START)
            return
        } else if (benchmarkController != null) {
            // Close sliding drawer
            if (benchmarkController!!.onBackPressed()) {
                return
            }
        }
        super.onBackPressed()
    }

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    @Synchronized
    private fun gpsStart() {
        PreferenceUtils.saveTrackingStarted(true)
        service?.subscribeToLocationUpdates()
        showProgressBar()

        // Observe flows
        observeLocationFlow()
        observeGnssStates()

        // Show Toast only if the user has set minTime or minDistance to something other than default values
        if (minTimeMillis() != (getString(R.string.pref_gps_min_time_default_sec).toDouble() * SECONDS_TO_MILLISECONDS).toLong() ||
            minDistance() != getString(R.string.pref_gps_min_distance_default_meters).toFloat()
        ) {
            Toast.makeText(
                this,
                String.format(
                    getString(R.string.gnss_running),
                    (minTimeMillis().toDouble() / SECONDS_TO_MILLISECONDS).toString(),
                    minDistance().toString()
                ),
                Toast.LENGTH_SHORT
            ).show()
        }

        // Reset the options menu to trigger updates to action bar menu items
        invalidateOptionsMenu()
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        if (locationFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        locationFlow = repository.getLocations()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                lastLocation = it
                //Log.d(TAG, "Activity location: ${it.toNotificationTitle()}")

                hideProgressBar()

                // Reset the options menu to trigger updates to action bar menu items
                invalidateOptionsMenu()

                benchmarkController?.onLocationChanged(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun observeGnssStates() {
        repository.firstFixState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    is FirstFixState.Acquired -> hideProgressBar()
                    is FirstFixState.NotAcquired -> if (isTrackingStarted()) showProgressBar()
                }
            }
            .launchIn(lifecycleScope)

        repository.fixState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    is FixState.Acquired -> hideProgressBar()
                    is FixState.NotAcquired -> if (isTrackingStarted()) showProgressBar()
                }
            }
            .launchIn(lifecycleScope)
    }

    @Synchronized
    private fun gpsStop() {
        PreferenceUtils.saveTrackingStarted(false)
        locationFlow?.cancel()

        // Reset the options menu to trigger updates to action bar menu items
        invalidateOptionsMenu()
        progressBar?.visibility = View.GONE
    }

    private fun hideProgressBar() {
        if (progressBar != null) {
            UIUtils.hideViewWithAnimation(progressBar, UIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun showProgressBar() {
        if (progressBar != null) {
            UIUtils.showViewWithAnimation(progressBar, UIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun checkKeepScreenOn(settings: SharedPreferences) {
        binding.toolbar.keepScreenOn =
            settings.getBoolean(getString(R.string.pref_key_keep_screen_on), true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        initGpsSwitch(menu)
        return true
    }

    @ExperimentalCoroutinesApi
    private fun initGpsSwitch(menu: Menu) {
        val item = menu.findItem(R.id.gps_switch_item)
        if (item != null) {
            switch = MenuItemCompat.getActionView(item).findViewById(R.id.gps_switch)
            if (switch != null) {
                // Initialize state of GPS switch before we set the listener, so we don't double-trigger start or stop
                switch!!.isChecked = isTrackingStarted()

                // Set up listener for GPS on/off switch
                switch!!.setOnClickListener {
                    // Turn GPS on or off
                    if (!switch!!.isChecked && isTrackingStarted()) {
                        gpsStop()
                        service?.unsubscribeToLocationUpdates()
                        // Measurements need to be stopped to prevent GnssStatus from updating - Samsung bug?
                        // TODO - removeGnssMeasurementsListener();
                    } else {
                        if (switch!!.isChecked && !isTrackingStarted()) {
                            gpsStart()
                            // Measurements need to be started again
                            // TODO - addGnssMeasurementsListener();
                        }
                    }
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.share)
        if (item != null) {
            item.isVisible = lastLocation != null || isFileLoggingEnabled() == true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle menu item selection
        val itemId = item.itemId
        if (itemId == R.id.gps_switch) {
            return true
        } else if (itemId == R.id.share) {
            share()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun share() {
        val location = lastLocation
        shareDialogOpen = true
        UIUtils.showShareFragmentDialog(
            this, location, isFileLoggingEnabled(),
            service!!.csvFileLogger, service!!.jsonFileLogger, null
        )
    }

    override fun onCreateDialog(id: Int): Dialog {
        when (id) {
            UIUtils.WHATSNEW_DIALOG -> return UIUtils.createWhatsNewDialog(this)
            UIUtils.HELP_DIALOG -> return UIUtils.createHelpDialog(this)
            UIUtils.CLEAR_ASSIST_WARNING_DIALOG -> return createClearAssistWarningDialog()
        }
        return super.onCreateDialog(id)
    }

    private fun createClearAssistWarningDialog(): Dialog {
        val view = layoutInflater.inflate(R.layout.clear_assist_warning, null)
        val neverShowDialog = view.findViewById<CheckBox>(R.id.clear_assist_never_ask_again)
        neverShowDialog.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Save the preference
            PreferenceUtils.saveBoolean(
                getString(R.string.pref_key_never_show_clear_assist_warning),
                isChecked
            )
        }
        val icon = ContextCompat.getDrawable(Application.app, R.drawable.ic_delete)
        if (icon != null) {
            DrawableCompat.setTint(icon, resources.getColor(R.color.colorPrimary))
        }
        val builder = AlertDialog.Builder(this)
            .setTitle(R.string.clear_assist_warning_title)
            .setIcon(icon)
            .setCancelable(false)
            .setView(view)
            .setPositiveButton(
                R.string.yes
            ) { _: DialogInterface?, _: Int -> deleteAidingData() }
            .setNegativeButton(
                R.string.no
            ) { _: DialogInterface?, _: Int -> }
        return builder.create()
    }

    companion object {
        private const val TAG = "GpsTestActivity"
        private const val SECONDS_TO_MILLISECONDS = 1000
        private const val GPS_RESUME = "gps_resume"
        private const val EXISTING_CSV_LOG_FILE = "existing_csv_log_file"
        private const val EXISTING_JSON_LOG_FILE = "existing_json_log_file"
    }
}