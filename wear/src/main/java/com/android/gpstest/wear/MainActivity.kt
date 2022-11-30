package com.android.gpstest.wear

import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.android.gpstest.library.LocationLabelAndData
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.model.SbasType
import com.android.gpstest.library.ui.SignalInfoViewModel
import com.android.gpstest.library.util.CarrierFreqUtils
import com.android.gpstest.library.util.MathUtils
import com.android.gpstest.wear.theme.GpstestTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.compose.runtime.livedata.observeAsState
import dagger.hilt.android.AndroidEntryPoint
import java.util.*

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalCoroutinesApi::class)
     private val signalInfoViewModel: SignalInfoViewModel by viewModels()
     @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearApp(LocationLabelAndData.locationLabelAndDataSample, signalInfoViewModel.filteredGnssStatuses.observeAsState(emptyList()).value)
        }
    }
}

@Composable
fun WearApp(satStatues: List<String>, satelliteStatuses: List<SatelliteStatus>) {
    GpstestTheme {
        val listState = rememberScalingLazyListState()
        Scaffold(
            timeText = {
                if (!listState.isScrollInProgress) {
                    TimeText(
                        timeSource = TimeTextDefaults.timeSource(
                            DateFormat.getBestDateTimePattern(Locale.getDefault(), "hh:mm:ss")
                        )
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

                for (satStatue in satStatues) {
                    item {
                        Text(text = satStatue)
                    }
                }
//                for(satelliteStatus in satelliteStatuses) {
//                    item {
//                        val small = Modifier.defaultMinSize(minWidth = 36.dp)
//                        val medium = Modifier.defaultMinSize(minWidth = dimensionResource(R.dimen.min_column_width))
//                        val large = Modifier.defaultMinSize(minWidth = 50.dp)
//                        Svid(satelliteStatus, small)
//                        Flag(satelliteStatus, large)
//                        CarrierFrequency(satelliteStatus, small)
//                        Cn0(satelliteStatus, medium)
//                        AEU(satelliteStatus, medium)
//                    }
//                }
            }
        }
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
            FlagImage(R.drawable.ic_flag_european_union, R.string.galileo_content_description, modifier)
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
            FlagImage(R.drawable.ic_flag_european_union, R.string.egnos_content_description, modifier)
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
