package com.android.gpstest.ui.status

import android.location.Location
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.ui.DeviceInfoViewModel
import com.android.gpstest.util.SatelliteUtils.isSpeedAndBearingAccuracySupported
import com.android.gpstest.util.SatelliteUtils.isVerticalAccuracySupported
import com.android.gpstest.util.SharedPreferenceUtil.METERS
import com.android.gpstest.util.SharedPreferenceUtil.distanceUnits
import com.android.gpstest.util.UIUtils

@ExperimentalFoundationApi
@Composable
fun StatusScreen(viewModel: DeviceInfoViewModel) {
    val EMPTY_LAT_LONG = "             "

    // SimpleDateFormat can only do 3 digits of fractional seconds (.SSS)
    val SDF_TIME_24_HOUR = "HH:mm:ss.SSS"
    val SDF_TIME_12_HOUR = "hh:mm:ss.SSS a"
    val SDF_DATE_24_HOUR = "HH:mm:ss.SSS MMM d, yyyy z"
    val SDF_DATE_12_HOUR = "hh:mm:ss.SSS a MMM d, yyyy z"

    val location: Location by viewModel.location.observeAsState(Location("default"))

    // Save these states if variables are updated independently
    val savedLocation = rememberSaveable { location }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            LocationCard(savedLocation)
//            Filter()
//            GnssStatusCard()
//            SbasStatusCard()
        }
    }
}

@Preview
@Composable
fun LocationCardPreview(
    @PreviewParameter(LocationPreviewParameterProvider::class) location: Location
) {
    LocationCard(location)
}

class LocationPreviewParameterProvider : PreviewParameterProvider<Location> {
    override val values = sequenceOf(previewLocation())
}

fun previewLocation(): Location {
    val l = Location("preview")
    l.apply {
        latitude = 28.38473847
        longitude = -87.32837456
        time = 1633375741711
        altitude = 13.5
        speed = 21.5f
        bearing = 240f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            bearingAccuracyDegrees = 5f
        }
    }
    return l
}

@Composable
fun LocationCard(
    location: Location
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = 2.dp
    ) {
        Row {
            LabelColumn1()
            ValueColumn1(location)
            LabelColumn2()
            ValueColumn2(location)
        }
    }
}

@Composable
fun ValueColumn1(location: Location) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // TODO - support formatting values
        // TODO - use hasX() method for relevant values
        LocationValue(location.latitude.toString())
        LocationValue(location.longitude.toString())
        LocationValue(location.altitude.toString())
        LocationValue("") // FIXME - Alt MSL
        LocationValue(location.speed.toString())
        if (isSpeedAndBearingAccuracySupported()) {
            LocationValue(if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond.toString() else "")
        }
        LocationValue("") // FIXME - PDOP
    }
}

@Composable
fun ValueColumn2(location: Location) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        // TODO - support formatting values
        // TODO - use hasX() method for relevant values
        LocationValue(location.time.toString())
        LocationValue("") // FIXME - TTFF
        //Accuracy(location) // FIXME - This seems to break the preview - uncomment when we don't need the preview anymore
        LocationValue("") // FIXME - Num sats
        LocationValue(location.bearing.toString())
        if (isSpeedAndBearingAccuracySupported()) {
            LocationValue(if (location.hasBearingAccuracy()) location.bearingAccuracyDegrees.toString() else "")
        }
        LocationValue("") // FIXME - H/V DOP
    }
}

/**
 * Horizontal and vertical location accuracies based on the provided location
 * @param location
 */
@Composable
fun Accuracy(location: Location) {
    if (isVerticalAccuracySupported(location)) {
        if (distanceUnits().equals(METERS, ignoreCase = true)) {
            LocationValue(
                stringResource(
                    R.string.gps_hor_and_vert_accuracy_value_meters,
                    location.accuracy,
                    location.verticalAccuracyMeters
                )
            )
        } else {
            // Feet
            LocationValue(
                stringResource(
                    R.string.gps_hor_and_vert_accuracy_value_feet,
                    UIUtils.toFeet(location.accuracy.toDouble()),
                    UIUtils.toFeet(
                        location.verticalAccuracyMeters.toDouble()
                    )
                )
            )
        }
    } else {
        if (location.hasAccuracy()) {
            if (distanceUnits().equals(METERS, ignoreCase = true)) {
                LocationValue(
                    stringResource(
                        R.string.gps_accuracy_value_meters, location.accuracy
                    )
                )
            } else {
                // Feet
                LocationValue(
                    stringResource(
                        R.string.gps_accuracy_value_feet,
                        UIUtils.toFeet(location.accuracy.toDouble())
                    )
                )
            }
        } else {
            LocationValue("")
        }
    }
}

@Composable
fun LabelColumn1() {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(start = 5.dp, top = 5.dp, bottom = 5.dp, end = 2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        LocationLabel(R.string.latitude_label)
        LocationLabel(R.string.longitude_label)
        LocationLabel(R.string.altitude_label)
        LocationLabel(R.string.altitude_msl_label)
        LocationLabel(R.string.speed_label)
        LocationLabel(R.string.speed_acc_label)
        LocationLabel(R.string.pdop_label)
    }
}

@Composable
fun LabelColumn2() {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(start = 5.dp, top = 5.dp, bottom = 5.dp, end = 2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        LocationLabel(R.string.fix_time_label)
        LocationLabel(R.string.ttff_label)
        LocationLabel(R.string.hor_and_vert_accuracy_label) // FIXME - change to just H if only H is supported
        LocationLabel(R.string.num_sats_label)
        LocationLabel(R.string.bearing_label)
        LocationLabel(R.string.bearing_acc_label)
        LocationLabel(R.string.hvdop_label)
    }
}

@Composable
fun LocationLabel(@StringRes id: Int) {
    Text(
        text = stringResource(id),
        modifier = Modifier.padding(end = 4.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
}

@Composable
fun LocationValue(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(end = 4.dp),
        fontSize = 13.sp,
    )
}