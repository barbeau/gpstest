package com.android.gpstest.wear

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import com.android.gpstest.Application
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.library.data.FixState
import com.android.gpstest.library.data.LocationRepository
import com.android.gpstest.library.model.*
import com.android.gpstest.library.ui.SignalInfoViewModel
import com.android.gpstest.library.util.*
import com.android.gpstest.library.util.FormatUtils.formatBearing
import com.android.gpstest.library.util.FormatUtils.formatDOP
import com.android.gpstest.library.util.FormatUtils.formatHVDOP
import com.android.gpstest.library.util.FormatUtils.formatLatOrLon
import com.android.gpstest.wear.theme.GpstestTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var userDeniedPermission = false

    @OptIn(ExperimentalCoroutinesApi::class)
    private val signalInfoViewModel: SignalInfoViewModel by viewModels()

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var locationFlow: Job? = null

    // Preference listener that will cancel the above flows when the user turns off tracking via service notification
    private val stopTrackingListener: SharedPreferences.OnSharedPreferenceChangeListener =
        PreferenceUtil.newStopTrackingListener({ gpsStop() }, prefs)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe stopping location updates from the service
        prefs.registerOnSharedPreferenceChangeListener(stopTrackingListener)

        if (!userDeniedPermission) {
            requestPermissionAndStartGps(this)
        } else {
            LibUIUtils.showLocationPermissionDialog(this)
        }

        setContent {
            WearApp(this, signalInfoViewModel)
        }
    }

    private fun requestPermissionAndStartGps(activity: Activity) {
        if (PermissionUtils.hasGrantedPermissions(
                activity,
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
            )
        ) {
            gpsStart()
        } else {
            // Request permissions from the user
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION),
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
                gpsStart()
            } else {
                userDeniedPermission = true
            }
        }
    }

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    @Synchronized
    private fun gpsStart() {
        PreferenceUtils.saveTrackingStarted(true, prefs)

        // Observe flows
        observeLocationFlow()
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
                //lastLocation = it
                Log.d(TAG, "Activity location: $it")
            }
            .launchIn(lifecycleScope)
    }

    @Synchronized
    private fun gpsStop() {
        PreferenceUtils.saveTrackingStarted(false, prefs)
        locationFlow?.cancel()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun WearApp(context: Context, signalInfoViewModel: SignalInfoViewModel) {
    val gnssStatuses: List<SatelliteStatus> by signalInfoViewModel.filteredGnssStatuses.observeAsState(
        emptyList()
    )
    val location: Location by signalInfoViewModel.location.observeAsState(Location("invalid"))
    val fixState: FixState by signalInfoViewModel.fixState.observeAsState(FixState.NotAcquired)
    val satelliteMetadata: SatelliteMetadata by signalInfoViewModel.filteredSatelliteMetadata.observeAsState(
        SatelliteMetadata()
    )
    val dop: DilutionOfPrecision by signalInfoViewModel.dop.observeAsState(
        DilutionOfPrecision(
            Double.NaN,
            Double.NaN,
            Double.NaN
        )
    )
    GpstestTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = {
                if (!listState.isScrollInProgress) {
                    TimeText(
                        timeSource = object : TimeSource {
                            override val currentTime: String
                                @Composable
                                get() = if (location.time == 0L) "" else SimpleDateFormat("HH:mm:ss").format(
                                    location.time
                                )
                        }
                    )
                }
            },
            positionIndicator = {
                PositionIndicator(
                    scalingLazyListState = listState
                )
            }
        ) {
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)

            ScalingLazyColumn(
                modifier = contentModifier,
                autoCentering = AutoCenteringParams(itemIndex = 0),
                state = listState
            ) {
                item {
                    CustomLinearProgressBar(fixState)
                }

                item {
                    Text(
                        text = String.format("Lat: ") + formatLatOrLon(
                            Application.app, location.latitude, CoordinateType.LATITUDE,
                            prefs
                        )
                    )
                }
                item {
                    Text(
                        text = String.format("Long: ") + formatLatOrLon(
                            Application.app,
                            location.longitude,
                            CoordinateType.LONGITUDE,
                            prefs
                        )
                    )
                }
                item {
                    Text(
                        text = String.format(
                            "# Sats: %s", stringResource(
                                R.string.gps_num_sats_value,
                                satelliteMetadata.numSatsUsed,
                                satelliteMetadata.numSatsInView,
                                satelliteMetadata.numSatsTotal
                            )
                        )
                    )
                }
                item {
                    Text(
                        text = "Bearing: " + formatBearing(
                            Application.app,
                            location
                        )
                    )
                }
                item {
                    Text(text = "PDOP: " + formatDOP(context, dop))
                }
                item {
                    Text(text = "H/V DOP: " + formatHVDOP(context, dop))
                }
                item {
                    Text(
                        text = "Speed: " + FormatUtils.formatSpeed(
                            context, location, prefs
                        )
                    )
                }
                item {
                    StatusRowHeader(isGnss = true)
                }

                for (satelliteStatus in gnssStatuses) {
                    item {
                        StatusRow(satelliteStatus = satelliteStatus)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomLinearProgressBar(fixState: FixState) {
    AnimatedVisibility(visible = (fixState == FixState.NotAcquired)) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp),
            backgroundColor = Color.LightGray,
            color = Color.Gray
        )
    }
}

@Composable
fun StatusRowHeader(isGnss: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 5.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val small = Modifier.defaultMinSize(minWidth = 5.dp)
        val medium = Modifier.defaultMinSize(minWidth = dimensionResource(R.dimen.min_column_width))
        val large = Modifier.defaultMinSize(minWidth = 8.dp)

        StatusLabel(R.string.id_column_label, small)
        if (isGnss) {
            StatusLabel(R.string.gnss_flag_image_label, large)
        } else {
            StatusLabel(R.string.sbas_flag_image_label, large)
        }
        StatusLabel(R.string.cf_column_label, small)
        StatusLabel(R.string.gps_cn0_column_label, medium)
        StatusLabel(R.string.flags_aeu_column_label, medium)
    }
}

@Composable
fun StatusLabel(@StringRes id: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id),
        modifier = modifier.padding(start = 3.dp, end = 3.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        textAlign = TextAlign.Start
    )
}

@Composable
fun StatusRow(satelliteStatus: SatelliteStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val small = Modifier.defaultMinSize(minWidth = 5.dp)
        val medium = Modifier.defaultMinSize(minWidth = dimensionResource(R.dimen.min_column_width))
        val large = Modifier.defaultMinSize(minWidth = 8.dp)

        Svid(satelliteStatus, small)
        Flag(satelliteStatus, large)
        CarrierFrequency(satelliteStatus, small)
        Cn0(satelliteStatus, medium)
        AEU(satelliteStatus, medium)
    }
}

@Composable
fun CarrierFrequency(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    if (satelliteStatus.hasCarrierFrequency) {
        val carrierLabel = CarrierFreqUtils.getCarrierFrequencyLabel(satelliteStatus)
        if (carrierLabel != CarrierFreqUtils.CF_UNKNOWN) {
            StatusValue(carrierLabel, modifier)
        } else {
            // Shrink the size so we can show raw number, convert Hz to MHz
            val carrierMhz = MathUtils.toMhz(satelliteStatus.carrierFrequencyHz)
            Text(
                text = String.format("%.3f", carrierMhz),
                modifier = modifier.padding(start = 3.dp, end = 2.dp),
                fontSize = 9.sp,
                textAlign = TextAlign.Start
            )
        }
    } else {
        Box(
            modifier = modifier
        )
    }
}

@Composable
fun Svid(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    StatusValue(satelliteStatus.svid.toString(), modifier = modifier)
}

@Composable
fun Cn0(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    if (satelliteStatus.cn0DbHz != SatelliteStatus.NO_DATA) {
        StatusValue(String.format("%.1f", satelliteStatus.cn0DbHz), modifier)
    } else {
        StatusValue("", modifier)
    }
}

@Composable
fun StatusValue(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(start = 3.dp, end = 3.dp),
        fontSize = 13.sp,
        textAlign = TextAlign.Start
    )
}

@Composable
fun Flag(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    when (satelliteStatus.gnssType) {
        GnssType.NAVSTAR -> {
            FlagImage(R.drawable.ic_flag_usa, R.string.gps_content_description, modifier)
        }
        GnssType.GLONASS -> {
            FlagImage(R.drawable.ic_flag_russia, R.string.glonass_content_description, modifier)
        }
        GnssType.QZSS -> {
            FlagImage(R.drawable.ic_flag_japan, R.string.qzss_content_description, modifier)
        }
        GnssType.BEIDOU -> {
            FlagImage(R.drawable.ic_flag_china, R.string.beidou_content_description, modifier)
        }
        GnssType.GALILEO -> {
            FlagImage(
                R.drawable.ic_flag_european_union,
                R.string.galileo_content_description,
                modifier
            )
        }
        GnssType.IRNSS -> {
            FlagImage(R.drawable.ic_flag_india, R.string.irnss_content_description, modifier)
        }
        GnssType.SBAS -> SbasFlag(satelliteStatus, modifier)
        GnssType.UNKNOWN -> {
            Box(
                modifier = modifier
            )
        }
    }
}

@Composable
fun SbasFlag(status: SatelliteStatus, modifier: Modifier = Modifier) {
    when (status.sbasType) {
        SbasType.WAAS -> {
            FlagImage(R.drawable.ic_flag_usa, R.string.waas_content_description, modifier)
        }
        SbasType.EGNOS -> {
            FlagImage(
                R.drawable.ic_flag_european_union,
                R.string.egnos_content_description,
                modifier
            )
        }
        SbasType.GAGAN -> {
            FlagImage(R.drawable.ic_flag_india, R.string.gagan_content_description, modifier)
        }
        SbasType.MSAS -> {
            FlagImage(R.drawable.ic_flag_japan, R.string.msas_content_description, modifier)
        }
        SbasType.SDCM -> {
            FlagImage(R.drawable.ic_flag_russia, R.string.sdcm_content_description, modifier)
        }
        SbasType.SNAS -> {
            FlagImage(R.drawable.ic_flag_china, R.string.snas_content_description, modifier)
        }
        SbasType.SACCSA -> {
            FlagImage(R.drawable.ic_flag_icao, R.string.saccsa_content_description, modifier)
        }
        SbasType.UNKNOWN -> {
            Box(
                modifier = modifier
            )
        }
    }
}

@Composable
fun FlagImage(@DrawableRes flagId: Int, @StringRes contentDescriptionId: Int, modifier: Modifier) {
    Box(
        modifier = modifier.padding(start = 3.dp, end = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .border(BorderStroke(1.dp, Color.Black))
        ) {
            Image(
                painter = painterResource(id = flagId),
                contentDescription = stringResource(id = contentDescriptionId),
                Modifier.padding(1.dp)
            )
        }
    }
}

@Composable
fun AEU(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    val flags = CharArray(3)
    flags[0] = if (satelliteStatus.hasAlmanac) 'A' else ' '
    flags[1] = if (satelliteStatus.hasEphemeris) 'E' else ' '
    flags[2] = if (satelliteStatus.usedInFix) 'U' else ' '
    StatusValue(String(flags), modifier)
}
