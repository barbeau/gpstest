package com.android.gpstest.wear

import android.icu.text.SimpleDateFormat
import android.location.Location
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
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

@OptIn(ExperimentalCoroutinesApi::class)
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
                        text = String.format("Lat: ") + FormatUtils.formatLatOrLon(
                            Application.app, location.latitude, CoordinateType.LATITUDE,
                            Application.prefs
                        )
                    )
                }
                item {
                    Text(
                        text = String.format("Long: ") + FormatUtils.formatLatOrLon(
                            Application.app,
                            location.longitude,
                            CoordinateType.LONGITUDE,
                            Application.prefs
                        )
                    )
                }
                item {
                    Text(
                        text = "# Sats: " + FormatUtils.formatNumSats(
                            Application.app,
                            satelliteMetadata
                        )
                    )
                }
                item {
                    Text(
                        text = "Bearing: " + FormatUtils.formatBearing(
                            Application.app,
                            location
                        )
                    )
                }
                item {
                    Text(text = "PDOP: " + FormatUtils.formatDoP(Application.app, dop))
                }
                item {
                    Text(text = "H/V DOP: " + FormatUtils.formatHvDOP(Application.app, dop))
                }
                item {
                    Text(
                        text = "Speed: " + FormatUtils.formatSpeed(
                            Application.app, location, Application.prefs
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
