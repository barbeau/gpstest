package com.android.gpstest.wear

import android.icu.text.SimpleDateFormat
import android.location.Location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AutoCenteringParams
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeSource
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import com.android.gpstest.Application
import com.android.gpstest.library.data.FixState
import com.android.gpstest.library.model.CoordinateType
import com.android.gpstest.library.model.DilutionOfPrecision
import com.android.gpstest.library.model.SatelliteMetadata
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.ui.SignalInfoViewModel
import com.android.gpstest.library.util.FormatUtils
import com.android.gpstest.wear.theme.GpstestTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

/**
 * The main user interface for Wear OS that displays the basic information of GNSS
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalComposeUiApi::class)
@Composable
fun StatusScreen(signalInfoViewModel: SignalInfoViewModel) {
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
        val coroutineScope = rememberCoroutineScope()
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
            val focusRequester = remember { FocusRequester() }
            val contentModifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .onRotaryScrollEvent {
                    coroutineScope.launch {
                        listState.scrollBy(it.verticalScrollPixels)
                    }
                    true
                }
                .focusRequester(focusRequester)
                .focusable()

            ScalingLazyColumn(
                modifier = contentModifier,
                autoCentering = AutoCenteringParams(itemIndex = 3),
                state = listState
            ) {
                item {
                    CustomLinearProgressBar(fixState)
                }
                item {
                    Latitude(location)
                }
                item {
                    Longitude(location)
                }
                item {
                    NumSats(satelliteMetadata)
                }
                item {
                    Bearing(location)
                }
                item {
                    DoP(dop)
                }
                item {
                    HvDOP(dop)
                }
                item {
                    Speed(location)
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
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}

@Composable
fun Latitude(location: Location) {
    Text(
        text = stringResource(R.string.latitude_label) + " " + FormatUtils.formatLatOrLon(
            Application.app, location.latitude, CoordinateType.LATITUDE,
            Application.prefs
        )
    )
}

@Composable
fun Longitude(location: Location) {
    Text(
        text = stringResource(R.string.longitude_label) + " " + FormatUtils.formatLatOrLon(
            Application.app,
            location.longitude,
            CoordinateType.LONGITUDE,
            Application.prefs
        )
    )
}

@Composable
fun NumSats(satelliteMetadata: SatelliteMetadata) {
    Text(
        text = stringResource(R.string.num_sats_label) + " " + FormatUtils.formatNumSats(
            Application.app,
            satelliteMetadata
        )
    )
}

@Composable
fun Bearing(location: Location) {
    Text(
        text = stringResource(R.string.bearing_label) + " " + FormatUtils.formatBearing(
            Application.app,
            location
        )
    )
}

@Composable
fun DoP(dop: DilutionOfPrecision) {
    Text(text = stringResource(R.string.pdop_label) + " " + FormatUtils.formatDoP(Application.app, dop))
}

@Composable
fun HvDOP(dop: DilutionOfPrecision) {
    Text(text = stringResource(R.string.hvdop_label) + " " + FormatUtils.formatHvDOP(Application.app,
                                                                               dop))
}

@Composable
fun Speed(location: Location) {
    Text(
        text = stringResource(R.string.speed_label) + " " + FormatUtils.formatSpeed(
            Application.app, location, Application.prefs
        )
    )
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