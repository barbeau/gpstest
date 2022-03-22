/*
 * Copyright (C) 2022 Sean J. Barbeau
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

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.gpstest.R
import com.android.gpstest.model.UserCountry
import com.android.gpstest.ui.components.OkDialog
import com.android.gpstest.ui.components.TitleWithHelp
import com.android.gpstest.util.IOUtils
import com.android.gpstest.util.IOUtils.getAppVersionDescription

@Composable
fun DevicePropertiesList(userCountry: UserCountry) {
    TitleWithHelp(
        titleTextId = R.string.dashboard_device_properties,
        helpTitleId = R.string.dashboard_device_properties,
        helpTextId = R.string.dashboard_device_properties_help
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
        elevation = 2.dp
    ) {
        Column {
            DeviceMakeModel()
            GnssHardwareName()
            GnssHardwareYear()
            AndroidBuildVersionAndCodename()
            AndroidVersionAndApiLevel()
            AppVersion()
            if (userCountry.countryCode.isNotBlank() ||
                userCountry.countryName.isNotBlank()
            ) {
                UserCountry(userCountry)
            }
        }
    }
}

@Composable
fun DeviceMakeModel() {
    val manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercaseChar() }
    val model = Build.MODEL
    val device = Build.DEVICE
    PropertiesRow(
        titleId = R.string.device_make_model,
        description = "$manufacturer $model ($device)",
        helpTextId = R.string.device_make_model_help
    )
}

@Composable
fun GnssHardwareName() {
    PropertiesRow(
        titleId = R.string.gnss_hardware_name,
        description = IOUtils.getGnssHardwareModelName(),
        helpTextId = R.string.gnss_hardware_name_help
    )
}

@Composable
fun GnssHardwareYear() {
    PropertiesRow(
        titleId = R.string.gnss_hardware_year,
        description = IOUtils.getGnssHardwareYear(),
        helpTextId = R.string.gnss_hardware_year_help
    )
}

@Composable
fun AndroidBuildVersionAndCodename() {
    PropertiesRow(
        titleId = R.string.android_software_incremental_build_and_codename,
        description = Build.VERSION.INCREMENTAL + " (" + Build.VERSION.CODENAME + ")",
        helpTextId = R.string.android_software_incremental_build_help
    )
}

@Composable
fun AndroidVersionAndApiLevel() {
    val version = Build.VERSION.RELEASE
    val apiLevel = Build.VERSION.SDK_INT
    PropertiesRow(
        titleId = R.string.android_version_and_api_level,
        description = "Android $version (API Level $apiLevel)",
        helpTextId = R.string.android_version_and_api_level_help
    )
}

@Composable
fun AppVersion() {
    PropertiesRow(
        titleId = R.string.app_version_and_code,
        description = getAppVersionDescription(),
        helpTextId = R.string.app_version_and_code_help
    )
}

@Composable
fun UserCountry(userCountry: UserCountry) {
    PropertiesRow(
        titleId = R.string.user_country,
        description = userCountry.countryName + " (" + userCountry.countryCode + ")",
        helpTextId = R.string.user_country_help
    )
}


/**
 * A row that describes a property of the device
 */
@Composable
fun PropertiesRow(
    @StringRes titleId: Int,
    description: String = "",
    @StringRes helpTextId: Int,
) {
    var openDialog by remember { mutableStateOf(false) }
    Row(modifier = Modifier.clickable {
        openDialog = true
    }) {
        Column(
            modifier = Modifier
                .align(CenterVertically)
                .weight(1f)
                .padding(start = imagePaddingDp, end = 10.dp)
        ) {
            Text(
                modifier = Modifier.padding(start = 5.dp, top = if (description.isNotEmpty()) 5.dp else 0.dp),
                text = stringResource(id = titleId),
                style = smallSubtitleStyle
            )
            if (description.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(
                        start = 5.dp,
                        bottom = 5.dp
                    ),
                    text = description,
                    style = smallTitleStyle
                )
            }
        }
    }
    OkDialog(
        open = openDialog,
        onDismiss = { openDialog = false },
        title = stringResource(titleId),
        text = stringResource(helpTextId)
    )
}