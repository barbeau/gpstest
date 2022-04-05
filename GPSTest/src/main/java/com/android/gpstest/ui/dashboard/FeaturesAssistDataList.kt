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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.SatelliteMetadata
import com.android.gpstest.ui.components.OkDialog
import com.android.gpstest.util.IOUtils
import com.android.gpstest.util.PreferenceUtils.CAPABILITY_UNKNOWN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesAssistDataList(
    satelliteMetadata: SatelliteMetadata,
) {
    Text(
        modifier = Modifier.padding(5.dp),
        text = stringResource(id = R.string.dashboard_feature_assist_data),
        style = headingStyle,
        color = MaterialTheme.colorScheme.onBackground
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
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
    FeatureRow(
        featureTitleId = R.string.force_psds_injection,
        featureDescriptionId = description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capabilityInjectPsdsInt),
        onClick = {
            if (IOUtils.forcePsdsInjection(Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
                Support.YES
            } else {
                Support.NO
            }
        }
    ) {
        FeatureIcon(
            imageId = R.drawable.ic_inject_psds_24,
            contentDescriptionId = R.string.force_psds_injection,
            iconSizeDp = 40
        )
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
    FeatureRow(
        featureTitleId = R.string.force_time_injection,
        featureDescriptionId = description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capabilityInjectTimeInt),
        onClick = {
            if (IOUtils.forceTimeInjection(Application.app.getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
                Support.YES
            } else {
                Support.NO
            }
        }
    ) {
        FeatureIcon(
            imageId = R.drawable.ic_inject_time_24,
            contentDescriptionId = R.string.force_time_injection,
            iconSizeDp = 40
        )
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

    FeatureRow(
        featureTitleId = R.string.delete_aiding_data,
        featureDescriptionId = description,
        satelliteMetadata = satelliteMetadata,
        supported = fromPref(capabilityDeleteAssistInt),
        onClick = {
            openDialog = true
            Support.UNKNOWN
        }
    ) {
        FeatureIcon(
            imageId = R.drawable.ic_delete_black_24dp,
            contentDescriptionId = R.string.delete_aiding_data,
            iconSizeDp = 40
        )
    }
    OkDialog(
        open = openDialog,
        onDismiss = { openDialog = false },
        title = stringResource(R.string.delete_aiding_data),
        text = stringResource(R.string.dashboard_feature_tap_nav_drawer_assist)
    )
}