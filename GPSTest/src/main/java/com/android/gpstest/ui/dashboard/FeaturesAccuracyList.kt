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
import android.location.LocationManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.model.ScanStatus
import com.android.gpstest.ui.components.Wave
import com.android.gpstest.ui.theme.Green500
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.PreferenceUtils.*
import com.android.gpstest.util.SatelliteUtils
import kotlinx.coroutines.launch

@Composable
fun FeaturesAccuracyList(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_feature_accuracy),
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
            DualFrequency(
                satelliteMetadata,
                scanStatus
            )
            RawMeasurements(
                satelliteMetadata,
                scanStatus
            )
            CarrierPhase(
                satelliteMetadata,
                scanStatus
            )
            AutoGainControl(
                satelliteMetadata,
                scanStatus
            )
            AntennaInfo(satelliteMetadata)
        }
    }
}

@Composable
fun DualFrequency(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
) {
    FeatureSupport(
        // This drawable isn't used because we use the animated canvas, but provide it as a backup
        imageId = R.drawable.ic_dual_frequency,
        contentDescriptionId = R.string.dashboard_feature_dual_frequency_title,
        featureTitleId = R.string.dashboard_feature_dual_frequency_title,
        featureDescriptionId = R.string.dashboard_feature_dual_frequency_description,
        satelliteMetadata = satelliteMetadata,
        supported = if (satelliteMetadata.isNonPrimaryCarrierFreqInView) Support.YES else Support.NO,
        scanStatus = scanStatus
    )
}

@Composable
fun FrequencyImage(
    modifier: Modifier = Modifier,
    showSecondFrequency: Boolean = false,
    initialDeltaX: Float = -20f,
    animationDurationMs: Int = 25000,
    frequencyMultiplier: Float = 1.2f
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary)
            .border(
                BorderStroke(1.dp, MaterialTheme.colors.primary),
                CircleShape
            )
    ) {
        if (showSecondFrequency) {
            Wave(
                modifier = modifier,
                color = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                frequencyMultiplier = .8f,
                initialDeltaX = 0f,
                animationDurationMs = 10000
            )
        }
        Wave(
            modifier = modifier,
            color = MaterialTheme.colors.onPrimary.copy(alpha = 1.0f),
            frequencyMultiplier = frequencyMultiplier,
            initialDeltaX = initialDeltaX,
            animationDurationMs = animationDurationMs
        )
    }
}

@Composable
fun RawMeasurements(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
) {
    val capabilityMeasurementsInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_raw_measurements),
        CAPABILITY_UNKNOWN
    )

    // On Android S and higher we immediately know if support is available, so don't wait for scan
    val newScanStatus = ScanStatus(
        finishedScanningCfs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) true else scanStatus.finishedScanningCfs,
        timeUntilScanCompleteMs = scanStatus.timeUntilScanCompleteMs,
        scanDurationMs = scanStatus.scanDurationMs
    )

    // On Android S and higher we immediately know if support is available, so don't wait for scan
    FeatureSupport(
        imageId = R.drawable.ic_raw_measurements,
        contentDescriptionId = R.string.dashboard_feature_raw_measurements_title,
        featureTitleId = R.string.dashboard_feature_raw_measurements_title,
        featureDescriptionId = R.string.dashboard_feature_raw_measurements_description,
        satelliteMetadata = satelliteMetadata,
        supported = if (capabilityMeasurementsInt == CAPABILITY_SUPPORTED) Support.YES else Support.NO,
        scanStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) newScanStatus else scanStatus,
        iconSizeDp = 45
    )
}

@Composable
fun CarrierPhase(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
) {
    val capability = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_measurement_delta_range),
        CAPABILITY_UNKNOWN
    )

    FeatureSupport(
        imageId = R.drawable.ic_carrier_phase,
        contentDescriptionId = R.string.dashboard_feature_carrier_phase_title,
        featureTitleId = R.string.dashboard_feature_carrier_phase_title,
        featureDescriptionId = R.string.dashboard_feature_carrier_phase_description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capability),
        scanStatus = scanStatus,
        iconSizeDp = 45
    )
}

@Composable
fun AntennaInfo(satelliteMetadata: SatelliteMetadata) {
    val locationManager =
        Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val gnssAntennaInfo =
        getCapabilityDescription(SatelliteUtils.isGnssAntennaInfoSupported(locationManager))
    val antennaCfs: String
    val supported: Support
    if (gnssAntennaInfo.equals(Application.app.getString(R.string.capability_value_supported))) {
        antennaCfs = getString(Application.app.getString(R.string.capability_key_antenna_cf))
        supported = Support.YES
    } else {
        antennaCfs = ""
        supported = Support.NO
    }

    FeatureSupport(
        imageId = R.drawable.ic_antenna_24,
        contentDescriptionId = R.string.dashboard_feature_antenna_info_title,
        featureTitleId = R.string.dashboard_feature_antenna_info_title,
        featureDescriptionId = R.string.dashboard_feature_antenna_info_description,
        featureDescription = antennaCfs,
        satelliteMetadata = satelliteMetadata,
        supported = supported,
        iconSizeDp = 45
    )
}

@Composable
fun AutoGainControl(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
) {
    val autoGainControl = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_measurement_automatic_gain_control),
        CAPABILITY_UNKNOWN
    )

    FeatureSupport(
        imageId = R.drawable.ic_auto_gain_control_24,
        contentDescriptionId = R.string.dashboard_feature_auto_gain_control_title,
        featureTitleId = R.string.dashboard_feature_auto_gain_control_title,
        featureDescriptionId = R.string.dashboard_feature_auto_gain_control_description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(autoGainControl),
        scanStatus = scanStatus,
        iconSizeDp = 45
    )
}

@Composable
fun FeatureSupport(
    @DrawableRes imageId: Int,
    @StringRes contentDescriptionId: Int,
    @StringRes featureTitleId: Int,
    @StringRes featureDescriptionId: Int,
    featureDescription: String = "",
    satelliteMetadata: SatelliteMetadata,
    supported: Support,
    scanStatus: ScanStatus = ScanStatus(true, 0, 0),
    iconSizeDp: Int = 50,
    onClick: () -> Support = { Support.UNKNOWN }
) {
    val imagePaddingDp = 10

    val scope = rememberCoroutineScope()

    // Allow user to manually tap row to check support, and use this value if populated
    var manualSupported by remember { mutableStateOf(Support.UNKNOWN) }

    Row(modifier = Modifier
        .clickable {
            scope.launch {
                manualSupported = onClick()
            }
        }) {
        Column(
            modifier = Modifier
                .align(CenterVertically)
        ) {
            val customIconModifier = Modifier
                .size(iconSize)
                .clip(CircleShape)
                .padding(imagePaddingDp.dp)
            // TODO - refactor below IF statement to passing in Composables to FeatureSupport()
            if (featureTitleId == R.string.dashboard_feature_dual_frequency_title) {
                FrequencyImage(
                    customIconModifier,
                    showSecondFrequency = true
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(iconSize)
                        .padding(imagePaddingDp.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary),
                ) {
                    if (featureTitleId == R.string.pref_nmea_output_title) {
                        Text(
                            text = stringResource(R.string.nmea_prefix),
                            modifier = Modifier
                                .background(MaterialTheme.colors.primary)
                                .align(Center),
                            style = TextStyle(
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colors.onPrimary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(imageId),
                            contentDescription = stringResource(id = contentDescriptionId),
                            modifier = Modifier
                                .size(iconSizeDp.dp)
                                .padding(5.dp)
                                .background(MaterialTheme.colors.primary)
                                .align(Center),
                            tint = MaterialTheme.colors.onPrimary,
                        )
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .align(CenterVertically)
                .weight(1f)
        ) {
            Text(
                modifier = Modifier.padding(start = 5.dp, top = 10.dp),
                text = stringResource(id = featureTitleId),
                style = titleStyle
            )
            Text(
                modifier = Modifier.padding(start = 5.dp, bottom = 10.dp),
                text = if (featureDescription.isEmpty()) stringResource(id = featureDescriptionId) else featureDescription,
                style = subtitleStyle
            )
        }

        Column(
            modifier = Modifier
                .align(CenterVertically)
                .padding(end = 5.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row {
                if (satelliteMetadata.supportedGnss.isEmpty() && !scanStatus.finishedScanningCfs) {
                    // No signals yet
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(34.dp)
                            .padding(end = 10.dp, top = 4.dp, bottom = 4.dp)
                    )
                } else {
                    if (scanStatus.finishedScanningCfs || supported == Support.YES || manualSupported == Support.YES) {
                        // We've decided if it's supported
                        Check(
                            modifier = Modifier.align(CenterVertically),
                            supported = if (manualSupported != Support.UNKNOWN) manualSupported else supported
                        )
                    } else {
                        // Waiting for scan timeout to complete
                        ChipProgress(
                            Modifier
                                .align(CenterVertically)
                                .padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
                            scanStatus
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Check(
    modifier: Modifier = Modifier,
    supported: Support
) {
    Icon(
        modifier = modifier
            .size(34.dp)
            .padding(end = 5.dp, top = 4.dp, bottom = 4.dp),
        imageVector = ImageVector.vectorResource(
            id = when (supported) {
                Support.YES -> R.drawable.ic_baseline_check_circle_24
                Support.NO -> R.drawable.ic_baseline_cancel_24
                Support.UNKNOWN -> R.drawable.ic_baseline_question_24
            }
        ),
        contentDescription =
        when (supported) {
            Support.YES -> stringResource(R.string.dashboard_supported)
            Support.NO -> stringResource(R.string.dashboard_not_supported)
            Support.UNKNOWN -> stringResource(R.string.unknown)
        },
        tint = when (supported) {
            Support.YES -> Green500
            Support.NO -> MaterialTheme.colors.error
            Support.UNKNOWN -> Color.DarkGray
        }
    )
}

enum class Support {
    YES, NO, UNKNOWN
}

fun fromPref(preference: Int): Support {
    return when (preference) {
        PreferenceUtils.CAPABILITY_UNKNOWN -> Support.UNKNOWN
        PreferenceUtils.CAPABILITY_SUPPORTED -> Support.YES
        PreferenceUtils.CAPABILITY_NOT_SUPPORTED -> Support.NO
        else -> Support.UNKNOWN
    }
}