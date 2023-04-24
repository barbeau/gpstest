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
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.core.view.MenuItemCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.Application
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.BuildConfig
import com.android.gpstest.ForegroundOnlyLocationService
import com.android.gpstest.ForegroundOnlyLocationService.LocalBinder
import com.android.gpstest.R
import com.android.gpstest.databinding.ActivityMainBinding
import com.android.gpstest.library.data.FixState
import com.android.gpstest.library.data.LocationRepository
import com.android.gpstest.library.ui.SignalInfoViewModel
import com.android.gpstest.library.util.*
import com.android.gpstest.library.util.PreferenceUtil.darkTheme
import com.android.gpstest.library.util.PreferenceUtil.isFileLoggingEnabled
import com.android.gpstest.library.util.PreferenceUtil.minDistance
import com.android.gpstest.library.util.PreferenceUtil.minTimeMillis
import com.android.gpstest.library.util.PreferenceUtil.runInBackground
import com.android.gpstest.library.util.PreferenceUtils.isTrackingStarted
import com.android.gpstest.map.MapConstants
import com.android.gpstest.ui.NavigationDrawerFragment.NavigationDrawerCallbacks
import com.android.gpstest.ui.sky.SkyFragment
import com.android.gpstest.ui.status.StatusFragment
import com.android.gpstest.util.BuildUtils
import com.android.gpstest.util.UIUtils
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.zxing.integration.android.IntentIntegrator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.system.exitProcess
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), NavigationDrawerCallbacks {
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
    private var statusFragment: StatusFragment? = null
    private var mapFragment: MapFragment? = null
    private var skyFragment: SkyFragment? = null
    private var accuracyFragment: MapFragment? = null

    // Main signal view model
    @OptIn(ExperimentalCoroutinesApi::class)
    private val signalInfoViewModel: SignalInfoViewModel by viewModels()

    private var switch: SwitchMaterial? = null
    private var lastLocation: Location? = null
    var lastSavedInstanceState: Bundle? = null
    private var userDeniedPermission = false
    private var benchmarkController: BenchmarkController? = null

    private var initialLanguage: String? = null
    private var initialMinTimeMillis: Long? = null
    private var initialMinDistance: Float? = null

    private var shareDialogOpen = false
    private var progressBar: ProgressBar? = null
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
        PreferenceUtil.newStopTrackingListener ({ gpsStop() }, prefs)

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme
        if (darkTheme(app, prefs)) {
            setTheme(R.style.AppTheme_Dark_NoActionBar)
            useDarkTheme = true
        }
        super.onCreate(savedInstanceState)
        // Reset the activity title to make sure dynamic locale changes are shown
        LibUIUtils.resetActivityTitle(this)
        saveInstanceState(savedInstanceState)

        // Observe stopping location updates from the service
        prefs.registerOnSharedPreferenceChangeListener(stopTrackingListener)

        // Set the default values from the XML file if this is the first execution of the app
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        initialLanguage = PreferenceUtils.getString(getString(R.string.pref_key_language), prefs)
        initialMinTimeMillis = minTimeMillis(app, prefs)
        initialMinDistance = minDistance(app, prefs)

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
        val serviceIntent = Intent(this, ForegroundOnlyLocationService::class.java)
        bindService(serviceIntent, foregroundOnlyServiceConnection, BIND_AUTO_CREATE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // If another app is passing in a ground truth location, recreate the activity to initialize an existing instance
        if (IOUtils.isShowRadarIntent(app, intent) || IOUtils.isGeoIntent(app, intent)) {
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

    override fun onResume() {
        super.onResume()
        shareDialogOpen = false
        if (!userDeniedPermission) {
            requestPermissionAndInit(this)
        } else {
            // Explain permission to user (don't request permission here directly to avoid infinite
            // loop if user selects "Don't ask again") in system permission prompt
            LibUIUtils.showLocationPermissionDialog(this)
        }
        maybeRecreateApp()
        benchmarkController!!.onResume()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LibUIUtils.PICKFILE_REQUEST_CODE && resultCode == RESULT_OK) {
            // User picked a file to share from the Share dialog - update the dialog
            val uri = data?.data
            if (uri != null) {
                Log.i(TAG, "Uri: $uri")
                val location = lastLocation
                shareDialogOpen = true
                UIUtils.showShareFragmentDialog(
                    this, location, isFileLoggingEnabled(app, prefs),
                    service!!.csvFileLogger, service!!.jsonFileLogger, uri
                )
            }
        } else {
            // See if this result was a scanned QR Code with a ground truth location
            val scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (scanResult != null) {
                val geoUri = scanResult.contents
                val l = IOUtils.getLocationFromGeoUri(app, geoUri)
                if (l != null) {
                    l.removeAltitude() // TODO - RFC 5870 requires altitude height above geoid, which we can't support yet (see #296 and #530), so remove altitude here
                    // Create a SHOW_RADAR intent out of the Geo URI and pass that to set ground truth
                    val showRadar = IOUtils.createShowRadarIntent(app, l)
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

    private fun maybeRecreateApp() {
        // If the set language has changed since we created the Activity (e.g., returning from Settings), recreate App
        if (prefs.contains(getString(R.string.pref_key_language))) {
            val currentLanguage = PreferenceUtils.getString(getString(R.string.pref_key_language), prefs)
            if (currentLanguage != initialLanguage) {
                initialLanguage = currentLanguage
                recreateApp(null)
            }
        }
        // If the user changed the location update settings, recreate the App
        if (minTimeMillis(app, prefs) != initialMinTimeMillis || minDistance(app, prefs) != initialMinDistance) {
            initialMinTimeMillis = minTimeMillis(app, prefs)
            initialMinDistance = minDistance(app, prefs)
            recreateApp(null)
        }
    }

    /**
     * Destroys and recreates the main activity in a new process.  If we don't use a new process,
     * the map state and Accuracy ground truth location TextViews get messed up with mixed locales
     * and partial state retention.
     * @param currentIntent the Intent to pass to the re-created app, or null if there is no intent to pass
     */
    private fun recreateApp(currentIntent: Intent?) {
        val i = Intent(this, MainActivity::class.java)
        if (IOUtils.isShowRadarIntent(app, currentIntent)) {
            // If we're creating the app because we got a SHOW_RADAR intent, copy over the intent action and extras
            i.action = currentIntent!!.action
            i.putExtras(currentIntent.extras!!)
        } else if (IOUtils.isGeoIntent(app,currentIntent)) {
            // If we're creating the app because we got a geo: intent, turn it into a SHOW_RADAR intent for simplicity (they are used the same way)
            val l = IOUtils.getLocationFromGeoUri(
                app,
                currentIntent!!.data.toString()
            )
            if (l != null) {
                val showRadarIntent = IOUtils.createShowRadarIntent(app, l)
                i.action = showRadarIntent.action
                i.putExtras(showRadarIntent.extras!!)
            }
        }
        startActivity(i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
        // Restart process to destroy and recreate everything
        exitProcess(0)
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
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
            LibUIUtils.promptEnableGps(app,this)
        }
        setupStartState(lastSavedInstanceState)

        // If the theme has changed (e.g., from Preferences), destroy and recreate to reflect change
        val useDarkTheme = darkTheme(app, prefs)
        if (this.useDarkTheme != useDarkTheme) {
            this.useDarkTheme = useDarkTheme
            recreate()
        }
        val settings = prefs
        checkKeepScreenOn(settings)
        LibUIUtils.autoShowWhatsNew(prefs, app,this)
    }

    override fun onPause() {
        // Stop GNSS if this isn't a configuration change and the user hasn't opted to run in background
        if (!isChangingConfigurations && !runInBackground(app, prefs)) {
            service?.unsubscribeToLocationUpdates()
        }
        super.onPause()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupStartState(savedInstanceState: Bundle?) {
        // Use "Auto-start GNSS" setting, or existing tracking state (e.g., if service is running)
        if (prefs.getBoolean(
                getString(R.string.pref_key_auto_start_gps),
                true
            ) || isTrackingStarted(prefs)
        ) {
            gpsStart()
        }
    }

    override fun onNavigationDrawerItemSelected(position: Int) {
        goToNavDrawerItem(position)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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
                val prefs = prefs
                if (!prefs.getBoolean(
                        getString(R.string.pref_key_never_show_clear_assist_warning),
                        false
                    )
                ) {
                    showDialog(LibUIUtils.CLEAR_ASSIST_WARNING_DIALOG)
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
            NavigationDrawerFragment.NAVDRAWER_ITEM_HELP -> showDialog(LibUIUtils.HELP_DIALOG)
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
                LibUIUtils.sendEmail(this, email, locationString, signalInfoViewModel, BuildUtils.getPlayServicesVersion(), prefs, BuildConfig.FLAVOR)
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
            statusFragment = fm.findFragmentByTag(TAG) as StatusFragment?
            if (statusFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new StatusFragment")
                statusFragment = StatusFragment()
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
        statusFragment = fm.findFragmentByTag(TAG) as StatusFragment?
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
            mapFragment = fm.findFragmentByTag(MapConstants.MODE_MAP) as MapFragment?
            if (mapFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new MapFragment")
                val bundle = Bundle()
                bundle.putString(MapConstants.MODE, MapConstants.MODE_MAP)
                mapFragment = MapFragment()
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
        mapFragment = fm.findFragmentByTag(MapConstants.MODE_MAP) as MapFragment?
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
            skyFragment = fm.findFragmentByTag(SkyFragment.TAG) as SkyFragment?
            if (skyFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new SkyFragment")
                skyFragment = SkyFragment()
                fm.beginTransaction()
                    .add(R.id.fragment_container, skyFragment!!, SkyFragment.TAG)
                    .commit()
            }
        }
        supportFragmentManager.beginTransaction().show(skyFragment!!).commit()
        title = resources.getString(R.string.gps_sky_title)
    }

    private fun hideSkyFragment() {
        val fm = supportFragmentManager
        skyFragment = fm.findFragmentByTag(SkyFragment.TAG) as SkyFragment?
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
            accuracyFragment = fm.findFragmentByTag(MapConstants.MODE_ACCURACY) as MapFragment?
            if (accuracyFragment == null) {
                // No existing fragment was found, so create a new one
                Log.d(TAG, "Creating new AccuracyFragment for Accuracy")
                val bundle = Bundle()
                bundle.putString(MapConstants.MODE, MapConstants.MODE_ACCURACY)
                accuracyFragment = MapFragment()
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
        accuracyFragment = fm.findFragmentByTag(MapConstants.MODE_ACCURACY) as MapFragment?
        if (accuracyFragment != null && !accuracyFragment!!.isHidden) {
            fm.beginTransaction().hide(accuracyFragment!!).commit()
        }
    }

    private fun forcePsdsInjection() {
        val success =
            IOUtils.forcePsdsInjection(app, getSystemService(LOCATION_SERVICE) as LocationManager)
        if (success) {
            Toast.makeText(
                this, getString(R.string.force_psds_injection_success),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                app.getString(R.string.capability_key_inject_psds),
                PreferenceUtils.CAPABILITY_SUPPORTED,
                prefs
            )
        } else {
            Toast.makeText(
                this, getString(R.string.force_psds_injection_failure),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                app.getString(R.string.capability_key_inject_psds),
                PreferenceUtils.CAPABILITY_NOT_SUPPORTED,
                prefs
            )
        }
    }

    private fun forceTimeInjection() {
        val success =
            IOUtils.forceTimeInjection(app, getSystemService(LOCATION_SERVICE) as LocationManager)
        if (success) {
            Toast.makeText(
                this, getString(R.string.force_time_injection_success),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                app.getString(R.string.capability_key_inject_time),
                PreferenceUtils.CAPABILITY_SUPPORTED,
                prefs
            )
        } else {
            Toast.makeText(
                this, getString(R.string.force_time_injection_failure),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                app.getString(R.string.capability_key_inject_time),
                PreferenceUtils.CAPABILITY_NOT_SUPPORTED,
                prefs
            )
        }
    }

    @ExperimentalCoroutinesApi
    private fun deleteAidingData() {
        // If GPS is currently running, stop it
        val lastStartState = isTrackingStarted(prefs)
        if (isTrackingStarted(prefs)) {
            gpsStop()
        }
        val success =
            IOUtils.deleteAidingData(app, getSystemService(LOCATION_SERVICE) as LocationManager)
        if (success) {
            Toast.makeText(
                this, getString(R.string.delete_aiding_data_success),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                app.getString(R.string.capability_key_delete_assist),
                PreferenceUtils.CAPABILITY_SUPPORTED,
                prefs
            )
        } else {
            Toast.makeText(
                this, getString(R.string.delete_aiding_data_failure),
                Toast.LENGTH_SHORT
            ).show()
            PreferenceUtils.saveInt(
                app.getString(R.string.capability_key_delete_assist),
                PreferenceUtils.CAPABILITY_NOT_SUPPORTED,
                prefs
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
        PreferenceUtils.saveTrackingStarted(true, prefs)
        service?.subscribeToLocationUpdates()
        showProgressBar()

        // Observe flows
        observeLocationFlow()
        observeGnssStates()

        // Show Toast only if the user has set minTime or minDistance to something other than default values
        if (minTimeMillis(app, prefs) != (getString(R.string.pref_gps_min_time_default_sec).toDouble() * SECONDS_TO_MILLISECONDS).toLong() ||
            minDistance(app, prefs) != getString(R.string.pref_gps_min_distance_default_meters).toFloat()
        ) {
            Toast.makeText(
                this,
                String.format(
                    getString(R.string.gnss_running),
                    (minTimeMillis(app, prefs).toDouble() / SECONDS_TO_MILLISECONDS).toString(),
                    minDistance(app, prefs).toString()
                ),
                Toast.LENGTH_SHORT
            ).show()
        }

        // Reset the options menu to trigger updates to action bar menu items
        invalidateOptionsMenu()
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationFlow() {
        // This should be a Flow and not LiveData to ensure that the Flow is active before the Service is bound
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeGnssStates() {
        // Use ViewModel here to ensure that it's populated for fragments as well -
        // otherwise ViewModel is lazily initialized and we don't save TTFF if viewed later in Status (e.g., if started in Accuracy or Map)
        val gnssStateObserver = Observer<FixState> { fixState ->
            when (fixState) {
                is FixState.Acquired -> hideProgressBar()
                is FixState.NotAcquired -> if (isTrackingStarted(prefs)) showProgressBar()
            }
        }
        signalInfoViewModel.fixState.observe(
    this, gnssStateObserver
        )
    }

    @Synchronized
    private fun gpsStop() {
        PreferenceUtils.saveTrackingStarted(false, prefs)
        locationFlow?.cancel()

        // Reset the options menu to trigger updates to action bar menu items
        invalidateOptionsMenu()
        progressBar?.visibility = View.GONE
    }

    private fun hideProgressBar() {
        val p = progressBar
        if (p != null) {
            LibUIUtils.hideViewWithAnimation(p, LibUIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun showProgressBar() {
        val p = progressBar
        if (p != null) {
            LibUIUtils.showViewWithAnimation(p, LibUIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun checkKeepScreenOn(settings: SharedPreferences) {
        binding.toolbar.keepScreenOn =
            settings.getBoolean(getString(R.string.pref_key_keep_screen_on), true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
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
                switch!!.isChecked = isTrackingStarted(prefs)

                // Set up listener for GPS on/off switch
                switch!!.setOnClickListener {
                    // Turn GPS on or off
                    if (!switch!!.isChecked && isTrackingStarted(prefs)) {
                        gpsStop()
                        service?.unsubscribeToLocationUpdates()
                    } else {
                        if (switch!!.isChecked && !isTrackingStarted(prefs)) {
                            gpsStart()
                        }
                    }
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val item = menu.findItem(R.id.share)
        if (item != null) {
            item.isVisible = lastLocation != null || isFileLoggingEnabled(app, prefs) == true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle menu item selection
        when (item.itemId) {
            R.id.gps_switch -> {
                return true
            }
            R.id.share -> {
                share()
                return true
            }
            R.id.filter_sats -> {
                UIUtils.showFilterDialog(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun share() {
        val location = lastLocation
        shareDialogOpen = true
        UIUtils.showShareFragmentDialog(
            this, location, isFileLoggingEnabled(app, prefs),
            service!!.csvFileLogger, service!!.jsonFileLogger, null
        )
    }

    override fun onCreateDialog(id: Int): Dialog {
        when (id) {
            LibUIUtils.WHATSNEW_DIALOG -> return UIUtils.createWhatsNewDialog(this)
            LibUIUtils.HELP_DIALOG -> return UIUtils.createHelpDialog(this)
            LibUIUtils.CLEAR_ASSIST_WARNING_DIALOG -> return createClearAssistWarningDialog()
        }
        return super.onCreateDialog(id)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createClearAssistWarningDialog(): Dialog {
        val view = layoutInflater.inflate(R.layout.clear_assist_warning, null)
        val neverShowDialog = view.findViewById<CheckBox>(R.id.clear_assist_never_ask_again)
        neverShowDialog.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            // Save the preference
            PreferenceUtils.saveBoolean(
                getString(R.string.pref_key_never_show_clear_assist_warning),
                isChecked,
                prefs
            )
        }
        val icon = ContextCompat.getDrawable(app, R.drawable.ic_delete)
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
    }
}