package com.android.gpstest.ui.status

import android.annotation.SuppressLint
import android.location.Location
import android.text.format.DateFormat
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.CoordinateType
import com.android.gpstest.model.DilutionOfPrecision
import com.android.gpstest.ui.SignalInfoViewModel
import com.android.gpstest.util.DateTimeUtils
import com.android.gpstest.util.FormatUtils.formatAltitude
import com.android.gpstest.util.FormatUtils.formatAltitudeMsl
import com.android.gpstest.util.FormatUtils.formatLatOrLon
import com.android.gpstest.util.FormatUtils.formatSpeed
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.SatelliteUtils.isSpeedAndBearingAccuracySupported
import com.android.gpstest.util.SatelliteUtils.isVerticalAccuracySupported
import com.android.gpstest.util.SharedPreferenceUtil.METERS
import com.android.gpstest.util.SharedPreferenceUtil.distanceUnits
import com.android.gpstest.util.UIUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.text.SimpleDateFormat

@ExperimentalCoroutinesApi
@ExperimentalFoundationApi
@Composable
fun StatusScreen(viewModel: SignalInfoViewModel) {
    //
    // Observe LiveData from ViewModel
    //
    val location: Location by viewModel.location.observeAsState(Location("default"))
    val ttff: String by viewModel.ttff.observeAsState("")
    val altitudeMsl: Double by viewModel.altitudeMsl.observeAsState(Double.NaN)
    val dop: DilutionOfPrecision by viewModel.dop.observeAsState(DilutionOfPrecision(Double.NaN,Double.NaN,Double.NaN))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            LocationCard(location, ttff, altitudeMsl, dop)
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
    LocationCard(location, "5 sec", 1.4, DilutionOfPrecision(1.0,2.0,3.0))
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
        if (isSpeedAndBearingAccuracySupported()) {
            bearingAccuracyDegrees = 5.6f
            speedAccuracyMetersPerSecond = 6.1f
        }
    }
    return l
}

@Composable
fun LocationCard(
    location: Location,
    ttff: String,
    altitudeMsl: Double,
    dop: DilutionOfPrecision,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Row {
            LabelColumn1()
            ValueColumn1(location, altitudeMsl, dop)
            LabelColumn2()
            ValueColumn2(location, ttff, dop)
        }
    }
}

@Composable
fun ValueColumn1(
    location: Location,
    altitudeMsl: Double,
    dop: DilutionOfPrecision,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Latitude(location)
        Longitude(location)
        Altitude(location)
        AltitudeMsl(altitudeMsl)
        Speed(location)
        SpeedAccuracy(location)
        Pdop(dop)
    }
}

@Composable
fun Latitude(location: Location) {
    Value(formatLatOrLon(location.latitude, CoordinateType.LATITUDE))
}

@Composable
fun Longitude(location: Location) {
    Value(formatLatOrLon(location.longitude, CoordinateType.LONGITUDE))
}

@Composable
fun Altitude(location: Location) {
    Value(formatAltitude(location))
}

@Composable
fun AltitudeMsl(altitudeMsl: Double) {
    Value(formatAltitudeMsl(altitudeMsl))
}

@Composable
fun Speed(location: Location) {
    Value(formatSpeed(location))
}

@Composable
fun SpeedAccuracy(location: Location) {
    // FIX ME - format from StatusFragment
    if (isSpeedAndBearingAccuracySupported()) {
        Value(if (location.hasSpeedAccuracy()) location.speedAccuracyMetersPerSecond.toString() else "")
    }
}

@Composable
fun Pdop(dop: DilutionOfPrecision) {
    if (dop.positionDop.isNaN()) {
        Value("")
    } else {
        Value(stringResource(R.string.pdop_value, dop.positionDop))
    }
}

@Composable
fun ValueColumn2(
    location: Location,
    ttff: String,
    dop: DilutionOfPrecision,
) {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Time(location)
        TTFF(ttff)
        Accuracy(location)
        NumSats(location)
        Bearing(location)
        BearingAccuracy(location)
        HVDOP(dop)
    }
}

@Composable
fun Time(location: Location) {
    if (location.time == 0L || !PreferenceUtils.isTrackingStarted()) {
        Value("")
    } else {
        if (DateTimeUtils.isTimeValid(location.time)) {
            formatTime(location.time)
        } else {
            // Error in fix time - FIXME - show error view instead
//            binding.fixTimeError.visibility = View.VISIBLE
//            binding.fixTimeError.text = formatFixTimeDate(location.time)
//            binding.fixTime.visibility = View.GONE
            Value("")
        }
    }
}

@Composable
private fun formatTime(fixTime: Long) {
    // SimpleDateFormat can only do 3 digits of fractional seconds (.SSS)
    val SDF_TIME_24_HOUR = "HH:mm:ss.SSS"
    val SDF_TIME_12_HOUR = "hh:mm:ss.SSS a"
    val SDF_DATE_24_HOUR = "HH:mm:ss.SSS MMM d, yyyy z"
    val SDF_DATE_12_HOUR = "hh:mm:ss.SSS a MMM d, yyyy z"

    // See #117
    @SuppressLint("SimpleDateFormat")
    val timeFormat = remember {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(Application.app.applicationContext)) SDF_TIME_24_HOUR else SDF_TIME_12_HOUR
        )
    }
    @SuppressLint("SimpleDateFormat")
    val timeAndDateFormat = remember {
        SimpleDateFormat(
            if (DateFormat.is24HourFormat(Application.app.applicationContext)) SDF_DATE_24_HOUR else SDF_DATE_12_HOUR
        )
    }

    if (LocalConfiguration.current.screenWidthDp > 450) { // 450dp is a little larger than the width of a Samsung Galaxy S8+
        // Time and date
        Value(timeAndDateFormat.format(fixTime))
    } else {
        // Just time
        Value(timeFormat.format(fixTime))
    }
}

@Composable
fun TTFF(ttff: String) {
    Value(ttff)
}

/**
 * Horizontal and vertical location accuracies based on the provided location
 * @param location
 */
@Composable
fun Accuracy(location: Location) {
    if (isVerticalAccuracySupported(location)) {
        if (distanceUnits().equals(METERS, ignoreCase = true)) {
            Value(
                stringResource(
                    R.string.gps_hor_and_vert_accuracy_value_meters,
                    location.accuracy,
                    location.verticalAccuracyMeters
                )
            )
        } else {
            // Feet
            Value(
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
                Value(
                    stringResource(
                        R.string.gps_accuracy_value_meters, location.accuracy
                    )
                )
            } else {
                // Feet
                Value(
                    stringResource(
                        R.string.gps_accuracy_value_feet,
                        UIUtils.toFeet(location.accuracy.toDouble())
                    )
                )
            }
        } else {
            Value("")
        }
    }
}

@Composable
fun NumSats(location: Location) {
    Value("") // FIXME - Num sats
}

@Composable
fun Bearing(location: Location) {
    // FIXME - format from StatusFragment
    if (location.hasBearing()) {
        Value(stringResource(R.string.gps_bearing_value, location.bearing))
    } else {
        Value("")
    }
}

@Composable
fun BearingAccuracy(location: Location) {
    // FIX ME - format from StatusFragment
    if (isSpeedAndBearingAccuracySupported()) {
        Value(if (location.hasBearingAccuracy()) location.bearingAccuracyDegrees.toString() else "")
    }
}

@Composable
fun HVDOP(dop: DilutionOfPrecision) {
    if (dop.horizontalDop.isNaN() || dop.verticalDop.isNaN()) {
        Value("")
    } else {
        Value(
            stringResource(
                R.string.hvdop_value, dop.horizontalDop,
                dop.verticalDop
            )
        )
    }
}

@Composable
fun LabelColumn1() {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp, start = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        Label(R.string.latitude_label)
        Label(R.string.longitude_label)
        Label(R.string.altitude_label)
        Label(R.string.altitude_msl_label)
        Label(R.string.speed_label)
        Label(R.string.speed_acc_label)
        Label(R.string.pdop_label)
    }
}

@Composable
fun LabelColumn2() {
    Column(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth()
            .padding(top = 5.dp, bottom = 5.dp, end = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.End
    ) {
        Label(R.string.fix_time_label)
        Label(R.string.ttff_label)
        Label(R.string.hor_and_vert_accuracy_label) // FIXME - change to just H if only H is supported
        Label(R.string.num_sats_label)
        Label(R.string.bearing_label)
        Label(R.string.bearing_acc_label)
        Label(R.string.hvdop_label)
    }
}

@Composable
fun Label(@StringRes id: Int) {
    Text(
        text = stringResource(id),
        modifier = Modifier.padding(start = 4.dp, end = 4.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
    )
}

@Composable
fun Value(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(end = 4.dp),
        fontSize = 13.sp,
    )
}