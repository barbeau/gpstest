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
package com.android.gpstest.ui.status

import android.location.Location
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.R
import com.android.gpstest.library.data.FixState
import com.android.gpstest.library.model.DilutionOfPrecision
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.model.SatelliteMetadata
import com.android.gpstest.library.model.SatelliteStatus
import com.android.gpstest.library.model.SbasType
import com.android.gpstest.library.ui.SignalInfoViewModel
import com.android.gpstest.library.util.CarrierFreqUtils
import com.android.gpstest.library.util.MathUtils
import com.android.gpstest.library.util.PreferenceUtils
import com.android.gpstest.library.util.PreferenceUtils.gnssFilter

@Composable
fun StatusScreen(viewModel: SignalInfoViewModel) {
    //
    // Observe LiveData from ViewModel
    //
    val location: Location by viewModel.location.observeAsState(Location("default"))
    val ttff: String by viewModel.ttff.observeAsState("")
    val altitudeMsl: Double by viewModel.altitudeMsl.observeAsState(Double.NaN)
    val dop: DilutionOfPrecision by viewModel.dop.observeAsState(DilutionOfPrecision(Double.NaN,Double.NaN,Double.NaN))
    val satelliteMetadata: SatelliteMetadata by viewModel.filteredSatelliteMetadata.observeAsState(
        SatelliteMetadata()
    )
    val fixState: FixState by viewModel.fixState.observeAsState(FixState.NotAcquired)
    val gnssStatuses: List<SatelliteStatus> by viewModel.filteredGnssStatuses.observeAsState(emptyList())
    val sbasStatuses: List<SatelliteStatus> by viewModel.filteredSbasStatuses.observeAsState(emptyList())
    val allStatuses: List<SatelliteStatus> by viewModel.allStatuses.observeAsState(emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            LocationCard(
                location,
                ttff,
                altitudeMsl,
                dop,
                satelliteMetadata,
                fixState)
            if (gnssFilter(app, prefs).isNotEmpty()) {
                Filter(allStatuses.size, satelliteMetadata) { PreferenceUtils.clearGnssFilter(app, prefs) }
            }
            GnssStatusCard(gnssStatuses)
            SbasStatusCard(sbasStatuses)
        }
    }
}

@Composable
fun Filter(totalNumSignals: Int, satelliteMetadata: SatelliteMetadata, onClick: () -> Unit) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(1.dp)
    ) {
        Text(
            text = stringResource(
                id = R.string.filter_signal_text,
                satelliteMetadata.numSignalsTotal,
                totalNumSignals
            ),
            fontSize = 13.sp,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colors.onBackground
        )
        Text(
            text = buildAnnotatedString {
                val string = stringResource(id = R.string.filter_showall)
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colors.primary,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(string)
                }
            },
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(start = 2.dp)
                .clickable {
                    onClick()
                }
        )
    }
}

@Composable
fun GnssStatusCard(satStatuses: List<SatelliteStatus>) {
    StatusCard(satStatuses, true)
}

@Composable
fun SbasStatusCard(satStatuses: List<SatelliteStatus>) {
    StatusCard(satStatuses, false)
}

@Composable
fun StatusCard(
    satStatuses: List<SatelliteStatus>,
    isGnss: Boolean,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .padding(5.dp)

    if (showList(isGnss, satStatuses)) {
        // Only scroll if we're showing satellites - we don't want "Not available" text to extend offscreen
        modifier.horizontalScroll(rememberScrollState())
    }

    Card(
        modifier = modifier,
        elevation = 2.dp
    ) {
        if (showList(isGnss, satStatuses)) {
            Column {
                StatusRowHeader(isGnss)
                satStatuses.forEach {
                    StatusRow(it)
                }
                StatusRowFooter()
            }
        } else {
            NotAvailable(isGnss)
        }
    }
}

private fun showList(isGnss: Boolean, satStatuses: List<SatelliteStatus>): Boolean {
    return isGnss ||
            (!isGnss && satStatuses.isNotEmpty())
}

@Composable
fun StatusRow(satelliteStatus: SatelliteStatus) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val small = Modifier.defaultMinSize(minWidth = 36.dp)
        val medium = Modifier.defaultMinSize(minWidth = dimensionResource(R.dimen.min_column_width_medium))
        val large = Modifier.defaultMinSize(minWidth = 50.dp)

        Svid(satelliteStatus, small)
        Flag(satelliteStatus, large)
        CarrierFrequency(satelliteStatus, small)
        Cn0(satelliteStatus, medium)
        AEU(satelliteStatus, medium)
        Elevation(satelliteStatus, medium)
        Azimuth(satelliteStatus, medium)
    }
}

@Composable
fun Svid(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    StatusValue(satelliteStatus.svid.toString(), modifier = modifier)
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
        SbasType.SOUTHPAN -> {
            FlagImage(R.drawable.ic_flag_southpan, R.string.southpan_content_description, modifier)
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
fun Cn0(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    if (satelliteStatus.cn0DbHz != SatelliteStatus.NO_DATA) {
        StatusValue(String.format("%.1f", satelliteStatus.cn0DbHz), modifier)
    } else {
        StatusValue("", modifier)
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

@Composable
fun Elevation(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    if (satelliteStatus.elevationDegrees != SatelliteStatus.NO_DATA) {
        StatusValue(
            stringResource(
                R.string.gps_elevation_column_value,
                satelliteStatus.elevationDegrees
            ).trimZeros(),
            modifier
        )
    } else {
        StatusValue("", modifier)
    }
}

private fun String.trimZeros(): String {
    return this.replace(".0", "")
        .replace(",0", "")
}

@Composable
fun Azimuth(satelliteStatus: SatelliteStatus, modifier: Modifier) {
    if (satelliteStatus.azimuthDegrees != SatelliteStatus.NO_DATA) {
        StatusValue(
            stringResource(
                R.string.gps_azimuth_column_value,
                satelliteStatus.azimuthDegrees
            ).trimZeros(),
            modifier
        )
    } else {
        StatusValue("", modifier)
    }
}

@Composable
fun StatusRowHeader(isGnss: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 5.dp, start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val small = Modifier.defaultMinSize(minWidth = dimensionResource(com.android.gpstest.library.R.dimen.min_column_width_small))
        val medium = Modifier.defaultMinSize(minWidth = dimensionResource(com.android.gpstest.library.R.dimen.min_column_width_medium))
        val large = Modifier.defaultMinSize(dimensionResource(com.android.gpstest.library.R.dimen.min_column_width_large))

        StatusLabel(com.android.gpstest.library.R.string.id_column_label, small)
        if (isGnss) {
            StatusLabel(com.android.gpstest.library.R.string.gnss_flag_image_label, large)
        } else {
            StatusLabel(com.android.gpstest.library.R.string.sbas_flag_image_label, large)
        }
        StatusLabel(com.android.gpstest.library.R.string.cf_column_label, small)
        StatusLabel(com.android.gpstest.library.R.string.gps_cn0_column_label, medium)
        StatusLabel(com.android.gpstest.library.R.string.flags_aeu_column_label, medium)
        StatusLabel(com.android.gpstest.library.R.string.elevation_column_label, medium)
        StatusLabel(com.android.gpstest.library.R.string.azimuth_column_label, medium)
    }
}

@Composable
fun StatusLabel(@StringRes id: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(id),
        modifier = modifier.padding(start = 3.dp, end = 3.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        textAlign = TextAlign.Start
    )
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
fun NotAvailable(isGnss: Boolean) {
    val message = if (isGnss) {
        stringResource(R.string.gnss_not_available)
    } else {
        stringResource(R.string.sbas_not_available)
    }
    NotAvailableText(message)
}

@Composable
fun NotAvailableText(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(10.dp),
        fontSize = 13.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun StatusRowFooter() {
    Spacer(modifier = Modifier.padding(bottom = 5.dp))
}
