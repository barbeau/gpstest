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
import androidx.compose.ui.unit.dp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.model.ScanStatus
import com.android.gpstest.ui.components.LinkifyText
import com.android.gpstest.ui.components.Wave
import com.android.gpstest.ui.theme.Green500
import com.android.gpstest.util.DateTimeUtils
import com.android.gpstest.util.IOUtils
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.PreferenceUtils.*
import com.android.gpstest.util.SatelliteUtils
import kotlinx.coroutines.launch

@Composable
fun SupportedFeaturesList(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_feature_support),
        style = MaterialTheme.typography.h6,
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
            NavigationMessages(
                satelliteMetadata,
                scanStatus
            )
            AntennaInfo(satelliteMetadata)
            InjectPsds(satelliteMetadata)
            InjectTime(satelliteMetadata)
            DeleteAssist(satelliteMetadata)
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
fun DualFrequencyImage(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colors.primary)
            .border(
                BorderStroke(1.dp, MaterialTheme.colors.primary),
                CircleShape
            )
    ) {
        Wave(
            modifier = modifier,
            color = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
            frequencyMultiplier = .8f,
            initialDeltaX = 0f,
            animationDurationMs = 10000
        )
        Wave(
            modifier = modifier,
            color = MaterialTheme.colors.onPrimary.copy(alpha = 1.0f),
            frequencyMultiplier = 1.2f,
            initialDeltaX = -20f,
            animationDurationMs = 25000
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
        supported = if (capabilityMeasurementsInt == PreferenceUtils.CAPABILITY_SUPPORTED) Support.YES else Support.NO,
        scanStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) newScanStatus else scanStatus
    )
}

@Composable
fun CarrierPhase(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus,
) {
    val capabilityCarrierPhaseInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_measurement_delta_range),
        CAPABILITY_UNKNOWN
    )

    FeatureSupport(
        imageId = R.drawable.ic_carrier_phase,
        contentDescriptionId = R.string.dashboard_feature_carrier_phase_title,
        featureTitleId = R.string.dashboard_feature_carrier_phase_title,
        featureDescriptionId = R.string.dashboard_feature_carrier_phase_description,
        satelliteMetadata = satelliteMetadata,
        supported = if (capabilityCarrierPhaseInt == PreferenceUtils.CAPABILITY_SUPPORTED) Support.YES else Support.NO,
        scanStatus = scanStatus,
        iconSizeDp = 50
    )
}

@Composable
fun NavigationMessages(
    satelliteMetadata: SatelliteMetadata,
    scanStatus: ScanStatus
) {
    val capabilityNavMessagesInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_nav_messages),
        CAPABILITY_UNKNOWN
    )
    // On Android S and higher we immediately know if support is available, so don't wait for scan
    val newScanStatus = ScanStatus(
        finishedScanningCfs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) true else scanStatus.finishedScanningCfs,
        timeUntilScanCompleteMs = scanStatus.timeUntilScanCompleteMs,
        scanDurationMs = scanStatus.scanDurationMs
    )
    FeatureSupport(
        imageId = R.drawable.ic_navigation_message,
        contentDescriptionId = R.string.dashboard_feature_navigation_messages_title,
        featureTitleId = R.string.dashboard_feature_navigation_messages_title,
        featureDescriptionId = R.string.dashboard_feature_navigation_messages_description,
        satelliteMetadata = satelliteMetadata,
        supported = if (capabilityNavMessagesInt == CAPABILITY_SUPPORTED) Support.YES else Support.NO,
        scanStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) newScanStatus else scanStatus,
        iconSizeDp = 50
    )
}

@Composable
fun AntennaInfo(satelliteMetadata: SatelliteMetadata) {
    val locationManager =
        Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val supported = SatelliteUtils.isGnssAntennaInfoSupported(locationManager)

    FeatureSupport(
        imageId = R.drawable.ic_antenna_24,
        contentDescriptionId = R.string.dashboard_feature_antenna_info_title,
        featureTitleId = R.string.dashboard_feature_antenna_info_title,
        featureDescriptionId = R.string.dashboard_feature_antenna_info_description,
        satelliteMetadata = satelliteMetadata,
        supported = if (supported) Support.YES else Support.NO,
        iconSizeDp = 50
    )
}

@Composable
fun InjectPsds(satelliteMetadata: SatelliteMetadata) {
    val capabilityInjectPsdsInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_inject_psds),
        CAPABILITY_UNKNOWN
    )
    val description = if (capabilityInjectPsdsInt == CAPABILITY_UNKNOWN) {
        R.string.dashboard_feature_tap_to_try
    } else {
        R.string.dashboard_feature_inject_psds_description
    }

    // We immediately know if support is available, so don't wait for scan
    FeatureSupport(
        imageId = R.drawable.ic_inject_psds_24,
        contentDescriptionId = R.string.force_psds_injection,
        featureTitleId = R.string.force_psds_injection,
        featureDescriptionId = description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capabilityInjectPsdsInt),
        iconSizeDp = 45
    ) {
        if (IOUtils.forcePsdsInjection(Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
            Support.YES
        } else {
            Support.NO
        }
    }
}

@Composable
fun InjectTime(satelliteMetadata: SatelliteMetadata) {
    // Inject time
    val capabilityInjectTimeInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_inject_time),
        CAPABILITY_UNKNOWN
    )
    val description = if (capabilityInjectTimeInt == CAPABILITY_UNKNOWN) {
        R.string.dashboard_feature_tap_to_try
    } else {
        R.string.dashboard_feature_inject_time_description
    }

    // We immediately know if support is available, so don't wait for scan
    FeatureSupport(
        imageId = R.drawable.ic_inject_time_24,
        contentDescriptionId = R.string.force_time_injection,
        featureTitleId = R.string.force_time_injection,
        featureDescriptionId = description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capabilityInjectTimeInt),
        iconSizeDp = 45
    ) {
        if (IOUtils.forceTimeInjection(Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
            Support.YES
        } else {
            Support.NO
        }
    }
}

@Composable
fun DeleteAssist(satelliteMetadata: SatelliteMetadata) {
    // Delete assist data
    val capabilityDeleteAssistInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_delete_assist),
        CAPABILITY_UNKNOWN
    )
    val description = if (capabilityDeleteAssistInt == CAPABILITY_UNKNOWN) {
        R.string.dashboard_feature_empty_string
    } else {
        R.string.dashboard_feature_delete_assist_description
    }

    FeatureSupport(
        imageId = R.drawable.ic_delete_black_24dp,
        contentDescriptionId = R.string.delete_aiding_data,
        featureTitleId = R.string.delete_aiding_data,
        featureDescriptionId = description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capabilityDeleteAssistInt),
        iconSizeDp = 45
    )
}

@Composable
fun FeatureSupport(
    @DrawableRes imageId: Int,
    @StringRes contentDescriptionId: Int,
    @StringRes featureTitleId: Int,
    @StringRes featureDescriptionId: Int,
    satelliteMetadata: SatelliteMetadata,
    supported: Support,
    scanStatus: ScanStatus = ScanStatus(true, 0, 0),
    iconSizeDp: Int = 70,
    onClick: () -> Support = { Support.UNKNOWN }
) {
    val imageSizeDp = 75
    val imagePaddingDp = 10

    val scope = rememberCoroutineScope()

    // Allow user to manually tap row to check support, and use this value if populated
    var manualSupported by remember { mutableStateOf(Support.UNKNOWN) }

    Row(modifier = Modifier.clickable {
        scope.launch {
            manualSupported = onClick()
        }
    }) {
        Column {
            val customIconModifier = Modifier
                .size(imageSizeDp.dp)
                .clip(CircleShape)
                .padding(imagePaddingDp.dp)
            if (featureTitleId == R.string.dashboard_feature_dual_frequency_title) {
                DualFrequencyImage(
                    customIconModifier
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(imageSizeDp.dp)
                        .padding(imagePaddingDp.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary)
                ) {
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
        Column(
            modifier = Modifier
                .align(CenterVertically)
                .weight(1f)
        ) {
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = stringResource(id = featureTitleId),
                style = MaterialTheme.typography.h6
            )
            Text(
                modifier = Modifier.padding(start = 5.dp),
                text = stringResource(id = featureDescriptionId),
                style = MaterialTheme.typography.body2
            )
        }

        Column(
            modifier = Modifier
                .align(CenterVertically)
                .padding(end = 5.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row {
                if (satelliteMetadata.gnssToCf.isEmpty() && !scanStatus.finishedScanningCfs) {
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