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
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.model.ScanStatus
import com.android.gpstest.util.IOUtils
import com.android.gpstest.util.PreferenceUtils.*

@Composable
fun FeaturesAssistDataList(
    satelliteMetadata: SatelliteMetadata,
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_feature_assist_data),
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
            InjectPsds(satelliteMetadata)
            InjectTime(satelliteMetadata)
            DeleteAssist(satelliteMetadata)
        }
    }
}

@Composable
fun InjectPsds(satelliteMetadata: SatelliteMetadata) {
    val capabilityInjectPsdsInt = Application.prefs.getInt(
        Application.app.getString(R.string.capability_key_inject_psds),
        CAPABILITY_UNKNOWN
    )
    val description = if (capabilityInjectPsdsInt == CAPABILITY_UNKNOWN) {
        R.string.dashboard_feature_tap_to_check
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
        R.string.dashboard_feature_tap_to_check
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
        R.string.dashboard_feature_tap_to_check
    } else {
        R.string.dashboard_feature_delete_assist_description
    }

    var openDialog by remember { mutableStateOf(false) }

    FeatureSupport(
        imageId = R.drawable.ic_delete_black_24dp,
        contentDescriptionId = R.string.delete_aiding_data,
        featureTitleId = R.string.delete_aiding_data,
        featureDescriptionId = description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capabilityDeleteAssistInt),
        iconSizeDp = 45
    ) {
        openDialog = true
        Support.UNKNOWN
    }
    if (openDialog) {
        AlertDialog(
            onDismissRequest = {
                openDialog = false
            },
            title = {
                Text(stringResource(R.string.delete_aiding_data))
            },
            text = {
                Column {
                    Text(
                        text = Application.app.getString(
                            R.string.dashboard_feature_tap_nav_drawer
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
fun DeleteAssistDialog(open: Boolean) {
    var openDialog by remember { mutableStateOf(open) }
    if (openDialog) {
        AlertDialog(
            onDismissRequest = {
                openDialog = false
            },
            title = {
                Text(stringResource(R.string.delete_aiding_data))
            },
            text = {
                Column {
                    Text(
                        text = Application.app.getString(
                            R.string.dashboard_feature_tap_nav_drawer
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openDialog = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}