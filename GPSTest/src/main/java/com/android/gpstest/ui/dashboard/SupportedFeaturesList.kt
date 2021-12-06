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

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.ui.components.Wave
import com.android.gpstest.ui.theme.Green500
import com.android.gpstest.util.PreferenceUtils

@Composable
fun SupportedFeaturesList(
    satelliteMetadata: SatelliteMetadata,
    finishedScanningCfs: Boolean,
    timeUntilScanCompleteMs: Long,
    scanDurationMs: Long
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
                finishedScanningCfs,
                timeUntilScanCompleteMs,
                scanDurationMs
            )
            RawMeasurements(
                satelliteMetadata,
                finishedScanningCfs,
                timeUntilScanCompleteMs,
                scanDurationMs
            )
            CarrierPhase(
                satelliteMetadata,
                finishedScanningCfs,
                timeUntilScanCompleteMs,
                scanDurationMs
            )
        }
    }
}

@Composable
fun DualFrequency(
    satelliteMetadata: SatelliteMetadata,
    finishedScanningCfs: Boolean,
    timeUntilScanCompleteMs: Long,
    scanDurationMs: Long
) {
    FeatureSupport(
        imageId = R.drawable.ic_dual_frequency,
        contentDescriptionId = R.string.dashboard_feature_dual_frequency_title,
        featureTitleId = R.string.dashboard_feature_dual_frequency_title,
        featureDescriptionId = R.string.dashboard_feature_dual_frequency_description,
        satelliteMetadata = satelliteMetadata,
        supported = satelliteMetadata.isNonPrimaryCarrierFreqInView,
        finishedScanningCfs = finishedScanningCfs,
        timeUntilScanCompleteMs = timeUntilScanCompleteMs,
        scanDurationMs = scanDurationMs
    )
}

@Composable
fun DualFrequencyImage(modifier: Modifier = Modifier) {
    Box (modifier = modifier
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
    finishedScanningCfs: Boolean,
    timeUntilScanCompleteMs: Long,
    scanDurationMs: Long
) {
    val capabilityMeasurementsInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_raw_measurements),
        PreferenceUtils.CAPABILITY_UNKNOWN
    )

    FeatureSupport(
        imageId = R.drawable.ic_raw_measurements,
        contentDescriptionId = R.string.dashboard_feature_raw_measurements_title,
        featureTitleId = R.string.dashboard_feature_raw_measurements_title,
        featureDescriptionId = R.string.dashboard_feature_raw_measurements_description,
        satelliteMetadata = satelliteMetadata,
        supported = capabilityMeasurementsInt == PreferenceUtils.CAPABILITY_SUPPORTED,
        finishedScanningCfs = finishedScanningCfs,
        timeUntilScanCompleteMs = timeUntilScanCompleteMs,
        scanDurationMs = scanDurationMs,
    )
}

@Composable
fun CarrierPhase(
    satelliteMetadata: SatelliteMetadata,
    finishedScanningCfs: Boolean,
    timeUntilScanCompleteMs: Long,
    scanDurationMs: Long
) {
    val capabilityCarrierPhaseInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_measurement_delta_range),
        PreferenceUtils.CAPABILITY_UNKNOWN
    )

    FeatureSupport(
        imageId = R.drawable.ic_raw_measurements, // TODO - better ruler??
        contentDescriptionId = R.string.dashboard_feature_carrier_phase_title,
        featureTitleId = R.string.dashboard_feature_carrier_phase_title,
        featureDescriptionId = R.string.dashboard_feature_carrier_phase_description,
        satelliteMetadata = satelliteMetadata,
        supported = capabilityCarrierPhaseInt == PreferenceUtils.CAPABILITY_SUPPORTED,
        finishedScanningCfs = finishedScanningCfs,
        timeUntilScanCompleteMs = timeUntilScanCompleteMs,
        scanDurationMs = scanDurationMs,
    )
}

@Composable
fun FeatureSupport(
    @DrawableRes imageId: Int,
    @StringRes contentDescriptionId: Int,
    @StringRes featureTitleId: Int,
    @StringRes featureDescriptionId: Int,
    satelliteMetadata: SatelliteMetadata,
    supported: Boolean,
    finishedScanningCfs: Boolean,
    timeUntilScanCompleteMs: Long,
    scanDurationMs: Long,
    imageSizeDp: Int = 75,
    imagePaddingDp: Int = 10
) {
    Row {
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
                Box(modifier = Modifier
                    .size(imageSizeDp.dp)
                    .padding(imagePaddingDp.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.primary)
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(imageId),
                        contentDescription = stringResource(id = contentDescriptionId),
                        modifier = Modifier
                            .size(70.dp)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colors.primary),
                        tint = MaterialTheme.colors.onPrimary,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.align(CenterVertically)
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
                .fillMaxSize()
                .padding(end = 5.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row {
                if (satelliteMetadata.gnssToCf.isEmpty()) {
                    // No signals yet
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(34.dp)
                            .padding(end = 10.dp, top = 4.dp, bottom = 4.dp)
                    )
                } else {
                    if (finishedScanningCfs || supported) {
                        // We've decided if it's supported
                        Check(
                            modifier = Modifier.align(CenterVertically),
                            supported = supported
                        )
                    } else {
                        // Waiting for scan timeout to complete
                        ChipProgress(
                            Modifier
                                .align(CenterVertically)
                                .padding(end = 10.dp, top = 4.dp, bottom = 4.dp),
                            finishedScanningCfs = finishedScanningCfs,
                            timeUntilScanCompleteMs = timeUntilScanCompleteMs,
                            scanDurationMs = scanDurationMs
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
    supported: Boolean
) {
    Icon(
        modifier = modifier
            .size(34.dp)
            .padding(end = 5.dp, top = 4.dp, bottom = 4.dp),
        imageVector = ImageVector.vectorResource(
            id = if (supported) R.drawable.ic_baseline_check_circle_24 else R.drawable.ic_baseline_cancel_24
        ),
        contentDescription =
        if (supported) stringResource(R.string.dashboard_supported) else stringResource(
            R.string.dashboard_not_supported
        ),
        tint = if (supported) Green500 else MaterialTheme.colors.error
    )
}