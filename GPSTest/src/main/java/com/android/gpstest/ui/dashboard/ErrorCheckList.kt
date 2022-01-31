/*
 * Copyright (C) 2021 Sean J. Barbeau
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
package com.android.gpstest.ui.dashboard

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.data.FixState
import com.android.gpstest.model.*
import com.android.gpstest.model.SatelliteStatus.Companion.NO_DATA
import com.android.gpstest.util.*
import com.android.gpstest.util.SatelliteUtil.altitudeComparedTo
import com.android.gpstest.util.SatelliteUtil.constellationName
import com.android.gpstest.util.SatelliteUtil.isTimeApproxEqualTo
import com.android.gpstest.util.SatelliteUtil.timeDiffMs
import com.android.gpstest.util.SortUtil.Companion.sortByGnssThenId
import com.android.gpstest.util.UIUtils.trimZeros
import java.text.SimpleDateFormat

@Composable
fun ErrorCheckList(
    satelliteMetadata: SatelliteMetadata,
    location: Location,
    fixState: FixState,
    geoidAltitude: GeoidAltitude,
    datum: Datum,
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_error_check),
        style = headingStyle,
        color = MaterialTheme.colors.onBackground
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Column {
            val locationManager =
                Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            ValidCfs(satelliteMetadata)
            DuplicateCfs(satelliteMetadata)
            MissingAlmanacEphemeris(satelliteMetadata)
            MismatchAzimuthElevationSameSatellite(satelliteMetadata)
            MismatchAlmanacEphemerisSameSatellite(satelliteMetadata)
            GpsWeekRollover(location, fixState)
            GeoidAltitude(location, fixState, geoidAltitude)
            Datum(datum)
            SignalsWithoutData(satelliteMetadata)
            if (SatelliteUtils.isGnssAntennaInfoSupported(locationManager)) {
                AntennaInfo()
            }
        }
    }
}

@Composable
fun ValidCfs(satelliteMetadata: SatelliteMetadata) {
    val isValid = satelliteMetadata.unknownCarrierStatuses.isEmpty()
    val unknown = satelliteMetadata.numSignalsTotal == 0
    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_for_signals)
    } else {
        if (isValid) "" else stringResource(
            R.string.dashboard_valid_cfs_description_fail
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_valid_cfs_title,
        featureDescription = description,
        badSatelliteStatus = sortByGnssThenId(satelliteMetadata.unknownCarrierStatuses.values.toList()),
        pass = pass
    ) {
        FrequencyImage(
            Modifier
                .size(iconSize)
                .clip(CircleShape)
                .padding(10.dp),
            showStrikeThrough = true
        )
    }
}

@Composable
fun DuplicateCfs(satelliteMetadata: SatelliteMetadata) {
    val isValid = satelliteMetadata.duplicateCarrierStatuses.isEmpty()
    val unknown = satelliteMetadata.numSignalsTotal == 0
    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_for_signals)
    } else {
        if (isValid) "" else stringResource(
            R.string.dashboard_duplicate_cfs_description_fail
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_duplicate_cfs_title,
        featureDescription = description,
        badSatelliteStatus = sortByGnssThenId(satelliteMetadata.duplicateCarrierStatuses.values.toList()),
        pass = pass
    ) {
        FrequencyImage(
            Modifier
                .size(iconSize)
                .clip(CircleShape)
                .padding(10.dp),
            showSecondFrequency = true,
            frequencyMultiplier = .8f,
            initialDeltaX = -10f,
            animationDurationMs = 10000
        )
    }
}

@Composable
fun MismatchAzimuthElevationSameSatellite(satelliteMetadata: SatelliteMetadata) {
    val isValid = satelliteMetadata.mismatchAzimuthElevationSameSatStatuses.isEmpty()
    val unknown = satelliteMetadata.numSignalsTotal == 0
    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_for_signals)
    } else {
        if (isValid) "" else stringResource(
            R.string.dashboard_mismatch_azimuth_elevation_fail
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_mismatch_azimuth_elevation_title,
        featureDescription = description,
        badSatelliteStatus = sortByGnssThenId(satelliteMetadata.mismatchAzimuthElevationSameSatStatuses.values.toList()),
        includeAzimuthAndElevation = true,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_navigation_message,
            contentDescriptionId = R.string.dashboard_feature_navigation_messages_title
        )
    }
}

@Composable
fun MismatchAlmanacEphemerisSameSatellite(satelliteMetadata: SatelliteMetadata) {
    val isValid = satelliteMetadata.mismatchAlmanacEphemerisSameSatStatuses.isEmpty()
    val unknown = satelliteMetadata.numSignalsTotal == 0
    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_for_signals)
    } else {
        if (isValid) "" else stringResource(
            R.string.dashboard_mismatch_almanac_ephemeris_fail
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_mismatch_almanac_ephemeris_title,
        featureDescription = description,
        badSatelliteStatus = sortByGnssThenId(satelliteMetadata.mismatchAlmanacEphemerisSameSatStatuses.values.toList()),
        includeAlmanacAndEphemeris = true,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_navigation_message,
            contentDescriptionId = R.string.dashboard_feature_navigation_messages_title
        )
    }
}

@Composable
fun MissingAlmanacEphemeris(satelliteMetadata: SatelliteMetadata) {
    val isValid = satelliteMetadata.missingAlmanacEphemerisButHaveAzimuthElevation.isEmpty()
    val unknown = satelliteMetadata.numSignalsTotal == 0
    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_for_signals)
    } else {
        if (isValid) "" else stringResource(
            R.string.dashboard_missing_almanac_ephemeris_fail
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_missing_almanac_ephemeris_title,
        featureDescription = description,
        badSatelliteStatus = sortByGnssThenId(satelliteMetadata.missingAlmanacEphemerisButHaveAzimuthElevation.values.toList()),
        includeAlmanacAndEphemeris = true,
        includeAzimuthAndElevation = true,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_navigation_message,
            contentDescriptionId = R.string.dashboard_feature_navigation_messages_title
        )
    }
}

@Composable
fun GpsWeekRollover(location: Location, fixState: FixState) {
    val isValid = DateTimeUtils.isTimeValid(location.time)
    val unknown = fixState == FixState.NotAcquired
    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val format = remember {
        SimpleDateFormat.getDateTimeInstance(
            java.text.DateFormat.LONG,
            java.text.DateFormat.LONG
        )
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_on_fix)
    } else {
        if (isValid) "" else stringResource(
            R.string.dashboard_gps_week_rollover_fail, format.format(location.time),
            DateTimeUtils.NUM_DAYS_TIME_VALID
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_gps_week_rollover_title,
        featureDescription = description,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_baseline_access_time_24,
            contentDescriptionId = R.string.dashboard_gps_week_rollover_title,
            iconSizeDp = 40
        )
    }
}

@Composable
fun GeoidAltitude(
    location: Location,
    fixState: FixState,
    geoidAltitude: GeoidAltitude,
) {
    var lastLocation by remember { mutableStateOf(location) }
    var lastGeoidAltitude by remember { mutableStateOf(geoidAltitude) }

    val hMinusH: hMinusH
    // Make sure we're comparing the values from the same location calculation by checking timestamps
    if (location.isTimeApproxEqualTo(geoidAltitude)) {
        hMinusH = location.altitudeComparedTo(geoidAltitude)
        lastLocation = location
        lastGeoidAltitude = geoidAltitude
    } else {
        // Use previous location and geoid pairing
        hMinusH = lastLocation.altitudeComparedTo(lastGeoidAltitude)
    }

    val unknown = (fixState == FixState.NotAcquired) || hMinusH.hMinusH.isNaN()
    val isValid =  hMinusH.isSame

    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        if (fixState == FixState.NotAcquired) {
            stringResource(R.string.dashboard_waiting_on_fix)
        } else {
            val timeDiff = location.timeDiffMs(geoidAltitude)
            stringResource(R.string.dashboard_waiting_on_nmea_dtm, timeDiff)
        }
    } else {
        if (isValid) stringResource(
            R.string.dashboard_geoid_pass,
            geoidAltitude.heightOfGeoid,
            location.altitude,
            geoidAltitude.altitudeMsl
        )
        else stringResource(
            R.string.dashboard_geoid_fail,
            geoidAltitude.heightOfGeoid,
            location.altitude,
            geoidAltitude.altitudeMsl,
            hMinusH.hMinusH
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_geoid_title,
        featureDescription = description,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_baseline_planet,
            contentDescriptionId = R.string.dashboard_planet_image,
            iconSizeDp = 40
        )
    }
}

@Composable
fun Datum(
    datum: Datum,
) {
    val unknown = datum.timestamp == 0L
    val isValid =
        NmeaUtils.isValidDatum(datum.localDatumCode) && NmeaUtils.isValidDatum(datum.datum)

    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_for_signals)
    } else {
        if (isValid) stringResource(
            R.string.dashboard_datum_pass,
            datum.localDatumCode,
            datum.datum
        ) else stringResource(R.string.dashboard_datum_fail, datum.localDatumCode, datum.datum)
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_datum_title,
        featureDescription = description,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_baseline_planet,
            contentDescriptionId = R.string.dashboard_planet_image,
            iconSizeDp = 40
        )
    }
}

@Composable
fun SignalsWithoutData(satelliteMetadata: SatelliteMetadata) {
    val isValid = satelliteMetadata.signalsWithoutData.isEmpty()
    val unknown = satelliteMetadata.numSignalsTotal == 0
    val pass = if (unknown) Pass.UNKNOWN else {
        if (isValid) Pass.YES else Pass.NO
    }
    val description = if (unknown) {
        stringResource(R.string.dashboard_waiting_for_signals)
    } else {
        if (isValid) "" else stringResource(
            R.string.dashboard_signals_without_data_fail
        )
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_signals_without_data_title,
        featureDescription = description,
        badSatelliteStatus = sortByGnssThenId(satelliteMetadata.signalsWithoutData.values.toList()),
        includeAzimuthAndElevation = true,
        includeAlmanacAndEphemeris = true,
        includeCn0 = true,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_navigation_message,
            contentDescriptionId = R.string.dashboard_feature_navigation_messages_title
        )
    }
}

@Composable
fun AntennaInfo() {
    val locationManager =
        Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gnssAntennaInfo =
        PreferenceUtils.getCapabilityDescription(
            SatelliteUtils.isGnssAntennaInfoSupported(
                locationManager
            )
        )
    val antennaCfs =
        if (gnssAntennaInfo.equals(Application.app.getString(R.string.capability_value_supported))) {
            PreferenceUtils.getString(Application.app.getString(R.string.capability_key_antenna_cf))
        } else {
            ""
        }

    // See https://issuetracker.google.com/issues/190197760
    val pass = if (antennaCfs == "L1, L2") Pass.NO else Pass.YES
    val description = if (pass == Pass.YES) {
        stringResource(
            R.string.dashboard_bad_antenna_info_pass,
            antennaCfs
        )
    } else {
        stringResource(R.string.dashboard_bad_antenna_info_fail, antennaCfs)
    }
    ErrorCheck(
        featureTitleId = R.string.dashboard_bad_antenna_info_title,
        featureDescription = description,
        pass = pass
    ) {
        ErrorIcon(
            imageId = R.drawable.ic_antenna_24,
            contentDescriptionId = R.string.dashboard_feature_antenna_info_title,
            iconSizeDp = 40
        )
    }
}

/**
 * A row that describes an error, with [content] being the @Composable shown as the icon.
 */
@Composable
fun ErrorCheck(
    @StringRes featureTitleId: Int,
    featureDescription: String = "",
    badSatelliteStatus: List<SatelliteStatus> = emptyList(),
    pass: Pass,
    includeAzimuthAndElevation: Boolean = false,
    includeAlmanacAndEphemeris: Boolean = false,
    includeCn0: Boolean = false,
    includeUsedInFix: Boolean = false,
    content: @Composable () -> Unit
) {
    Row {
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            // Icon
            content()
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .weight(1f)
                .padding(end = 10.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 5.dp, top = if (featureDescription.isNotEmpty()) 10.dp else 0.dp),
                text = stringResource(id = featureTitleId),
                style = titleStyle
            )
            if (featureDescription.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(
                        start = 5.dp,
                        bottom = if (pass == Pass.NO) 5.dp else 10.dp
                    ),
                    text = featureDescription,
                    style = subtitleStyle
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .padding(end = 5.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row {
                when (pass) {
                    Pass.YES -> PassChip()
                    Pass.NO -> FailChip()
                    Pass.UNKNOWN -> CircularProgressIndicator(
                        modifier = Modifier
                            .size(34.dp)
                            .padding(end = 10.dp, top = 4.dp, bottom = 4.dp)
                    )
                }
            }
        }
    }
    badSatelliteStatus.forEachIndexed { index, status ->
        val bottomPadding = if (index == badSatelliteStatus.size - 1) 10.dp else 0.dp
        Row(modifier = Modifier.padding(start = 75.dp, bottom = bottomPadding)) {
            val carrierMhz = MathUtils.toMhz(status.carrierFrequencyHz)
            val cf = String.format("%.3f MHz", carrierMhz)
            val elevation = if (status.elevationDegrees != NO_DATA) {
                stringResource(R.string.elevation_column_label) + " " + String.format(
                    stringResource(R.string.gps_elevation_column_value), status.elevationDegrees
                ).trimZeros()
            } else {
                stringResource(R.string.dashboard_elevation_no)
            }
            val azimuth = if (status.azimuthDegrees != NO_DATA) {
                stringResource(R.string.azimuth_column_label) + " " + String.format(
                    stringResource(R.string.gps_azimuth_column_value),
                    status.azimuthDegrees
                ).trimZeros()
            } else {
                stringResource(R.string.dashboard_azimuth_no)
            }
            val almanac =
                if (status.hasAlmanac) stringResource(R.string.dashboard_almanac_yes) else stringResource(
                    R.string.dashboard_almanac_no
                )
            val ephemeris =
                if (status.hasEphemeris) stringResource(R.string.dashboard_ephemeris_yes) else stringResource(
                    R.string.dashboard_ephemeris_no
                )
            val usedInFix =
                if (status.usedInFix) stringResource(R.string.dashboard_used_in_fix_yes) else stringResource(
                    R.string.dashboard_used_in_fix_no
                )
            val cn0 = stringResource(R.string.dashboard_cn0, status.cn0DbHz)

            Text(
                text = "\u2022 ${status.constellationName()}, ID ${status.svid}, $cf"
                        + (if (includeAzimuthAndElevation) ", $elevation, $azimuth" else "")
                        + (if (includeAlmanacAndEphemeris) ", $almanac, $ephemeris" else "")
                        + (if (includeUsedInFix) ", $usedInFix" else "")
                        + (if (includeCn0) ", $cn0" else ""),
                modifier = Modifier.padding(start = 3.dp, end = 2.dp),
                fontSize = 10.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
fun ErrorIcon(
    @DrawableRes imageId: Int,
    @StringRes contentDescriptionId: Int,
    iconSizeDp: Int = 50,
) {
    val imagePaddingDp = 10
    Box(
        modifier = Modifier
            .size(iconSize)
            .padding(imagePaddingDp.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(imageId),
            contentDescription = stringResource(id = contentDescriptionId),
            modifier = Modifier
                .size(iconSizeDp.dp)
                .padding(5.dp)
                .background(MaterialTheme.colors.primary)
                .align(Alignment.Center),
            tint = MaterialTheme.colors.onPrimary,
        )
    }
}

enum class Pass {
    YES, NO, UNKNOWN
}