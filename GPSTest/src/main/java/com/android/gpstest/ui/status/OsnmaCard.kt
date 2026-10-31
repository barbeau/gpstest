/*
 * Copyright (C) 2026 The GPSTest contributors
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

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.library.data.OsnmaUiState
import com.android.gpstest.library.osnma.Gst
import com.android.gpstest.library.osnma.KeySource
import com.android.gpstest.library.osnma.MacFunction
import com.android.gpstest.library.osnma.NmaStatus
import com.android.gpstest.library.osnma.OsnmaEvent
import com.android.gpstest.library.osnma.OsnmaStatus
import com.android.gpstest.library.osnma.OsnmaStatus.Phase
import com.android.gpstest.library.osnma.SatelliteAuthStatus
import kotlin.math.roundToInt

// Chip backgrounds (white text on top)
private val Green = Color(0xFF2E7D32)
private val Amber = Color(0xFFB26A00)

/** Green for text, readable on light and dark cards */
private val greenText: Color
    @Composable get() = if (MaterialTheme.colors.isLight) Green else Color(0xFF81C784)

/** Amber for text, readable on light and dark cards */
private val amberText: Color
    @Composable get() = if (MaterialTheme.colors.isLight) Amber else Color(0xFFFFB74D)
private val Gray = Color(0xFF757575)

/**
 * Shows the state of Galileo OSNMA (Open Service Navigation Message Authentication): whether
 * the navigation data broadcast by each Galileo satellite has been cryptographically
 * authenticated. Collapsed, it's a single line with the status; tap to expand it with all the
 * details. Hidden on devices that have never delivered Galileo I/NAV pages.
 */
@Composable
fun OsnmaCard(state: OsnmaUiState) {
    // Only show the card on devices that provide (or have provided) Galileo I/NAV messages
    val known = state.inavSeenBefore || state.inavPagesReceived > 0 || state.status != null || state.keysRevoked
    if (!state.enabled || !known) return
    var expanded by remember { mutableStateOf(osnmaCardExpanded) }
    val status = state.status

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp)
            .clickable {
                expanded = !expanded
                osnmaCardExpanded = expanded
            },
        elevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.osnma_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                PhaseChip(state)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = collapsedSummary(state) + if (expanded) "  \u25B4" else "  \u25BE",
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            if (expanded) {
                Text(
                    text = phaseDescription(state),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                if (state.clockOffsetTooLarge) {
                    Text(
                        text = stringResource(R.string.osnma_clock_offset_warning, state.clockOffsetSeconds ?: 0L),
                        fontSize = 13.sp,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (state.poorReception) {
                    Text(
                        text = receptionHint(state),
                        fontSize = 13.sp,
                        color = amberText,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                if (status != null && status.statistics.osnmaPages > 0) {
                    Details(status)
                }
                Expanded(state)
            }
        }
    }
}

/** Remembers whether the card is expanded while the app runs (e.g. when switching screens) */
private var osnmaCardExpanded = false

/** Short status for the collapsed card, e.g. "12 sats" */
@Composable
private fun collapsedSummary(state: OsnmaUiState): String {
    val status = state.status ?: return ""
    return when {
        status.phase == Phase.AUTHENTICATING ->
            stringResource(R.string.osnma_summary_satellites, status.authenticatedSatellites.size)
        state.poorReception -> stringResource(R.string.osnma_summary_poor_reception)
        else -> ""
    }
}

@Composable
private fun PhaseChip(state: OsnmaUiState) {
    val status = state.status
    val phase = if (state.keysRevoked) Phase.ALERT else status?.phase ?: Phase.NO_DATA
    val failures = status != null && recentFailures(status).isNotEmpty()
    val (color, textId) = when (phase) {
        Phase.AUTHENTICATING -> if (state.clockOffsetTooLarge) {
            Pair(MaterialTheme.colors.error, R.string.osnma_chip_time)
        } else {
            Pair(Green, R.string.osnma_chip_authenticated)
        }
        Phase.ALERT -> Pair(MaterialTheme.colors.error, R.string.osnma_chip_alert)
        Phase.DONT_USE -> Pair(MaterialTheme.colors.error, R.string.osnma_chip_dont_use)
        Phase.NO_DATA, Phase.NO_OSNMA -> Pair(Gray, R.string.osnma_chip_no_data)
        else -> Pair(Amber, R.string.osnma_chip_waiting)
    }
    Chip(stringResource(textId), color)
    if (failures) {
        Spacer(modifier = Modifier.width(4.dp))
        Chip(stringResource(R.string.osnma_chip_failures), MaterialTheme.colors.error)
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(start = 6.dp, end = 6.dp, top = 1.dp, bottom = 1.dp)
    )
}

@Composable
private fun phaseDescription(state: OsnmaUiState): String {
    val status = state.status
    if (state.keysRevoked) return stringResource(R.string.osnma_phase_alert)
    if (status == null) {
        return if (state.inavPagesReceived > 0) {
            stringResource(R.string.osnma_phase_syncing)
        } else {
            stringResource(R.string.osnma_phase_no_data)
        }
    }
    return when (status.phase) {
        Phase.NO_DATA -> stringResource(R.string.osnma_phase_no_data)
        Phase.NO_OSNMA -> stringResource(R.string.osnma_phase_no_osnma)
        Phase.WAITING_FOR_PUBLIC_KEY -> stringResource(R.string.osnma_phase_waiting_public_key)
        Phase.WAITING_FOR_KROOT -> stringResource(R.string.osnma_phase_waiting_kroot)
        Phase.WAITING_FOR_TAGS -> stringResource(R.string.osnma_phase_waiting_tags)
        Phase.AUTHENTICATING -> stringResource(
            R.string.osnma_phase_authenticating,
            status.authenticatedSatellites.size
        )
        Phase.DONT_USE -> stringResource(R.string.osnma_phase_dont_use)
        Phase.ALERT -> stringResource(R.string.osnma_phase_alert)
    }
}

@Composable
private fun Details(status: OsnmaStatus) {
    val header = status.nmaHeader
    if (header != null) {
        val nmas = when (header.nmaStatus) {
            NmaStatus.OPERATIONAL -> stringResource(R.string.osnma_nmas_operational)
            NmaStatus.TEST -> stringResource(R.string.osnma_nmas_test)
            NmaStatus.DONT_USE -> stringResource(R.string.osnma_nmas_dont_use)
            NmaStatus.RESERVED -> stringResource(R.string.osnma_nmas_reserved)
        }
        val verified = if (status.nmaHeaderAuthenticated) " ✓" else ""
        DetailRow(R.string.osnma_label_status, nmas + verified)
    }

    val key = status.publicKey
    DetailRow(
        R.string.osnma_label_public_key,
        if (key != null) {
            val source = when (key.source) {
                KeySource.SIGNAL -> stringResource(R.string.osnma_key_source_signal)
                KeySource.MERKLE_FILE -> stringResource(R.string.osnma_key_source_merkle_file)
                KeySource.IMPORTED -> stringResource(R.string.osnma_key_source_imported)
                KeySource.BUILT_IN -> stringResource(R.string.osnma_key_source_built_in)
            }
            stringResource(R.string.osnma_public_key_value, key.pkid, key.curveName, source)
        } else {
            stringResource(R.string.osnma_public_key_none)
        }
    )

    val tesla = status.teslaKey
    if (tesla != null) {
        val mac = if (tesla.chain.macFunction == MacFunction.HMAC_SHA_256) "HMAC-SHA-256" else "CMAC-AES"
        DetailRow(
            R.string.osnma_label_chain,
            stringResource(
                R.string.osnma_chain_value,
                tesla.chain.chainId,
                mac,
                tesla.chain.tagSizeBits,
                formatGst(tesla.gstSubframe)
            )
        )
    }

    val authenticated = status.authenticatedSatellites
    if (authenticated.isNotEmpty()) {
        DetailRow(
            R.string.osnma_label_authenticated,
            authenticated.joinToString(" ") { "E%02d".format(it.svn) },
            color = greenText
        )
    }
    val failed = recentFailures(status)
    if (failed.isNotEmpty()) {
        DetailRow(
            R.string.osnma_label_failed,
            failed.joinToString(" ") { "E%02d (%d)".format(it.svn, it.tagsFailed) },
            color = MaterialTheme.colors.error
        )
    }
}

@Composable
private fun Expanded(state: OsnmaUiState) {
    val status = state.status
    Column(modifier = Modifier.padding(top = 6.dp)) {
        if (status != null) {
            val s = status.statistics
            DetailRow(
                R.string.osnma_label_tags,
                stringResource(
                    R.string.osnma_tags_value,
                    s.tagsOk + s.dummyTagsOk,
                    s.tagsOkAdkd0,
                    s.tagsOkAdkd4,
                    s.tagsOkAdkd12,
                    s.tagsFailed + s.dummyTagsFailed
                )
            )
            DetailRow(
                R.string.osnma_label_pages,
                stringResource(
                    R.string.osnma_pages_value,
                    state.inavPagesReceived,
                    s.pages,
                    s.osnmaPages,
                    state.pagesWithoutTime,
                    s.pagesRejected + state.pagesWrongSize,
                    state.pagesCorrupted + s.crcErrors
                )
            )
            DetailRow(
                R.string.osnma_label_device_parity,
                stringResource(
                    R.string.osnma_device_parity_value,
                    state.pagesParityPassed,
                    state.inavPagesReceived,
                    state.crcFailedButParityPassed
                )
            )
            s.subframeCompleteness?.let {
                DetailRow(
                    R.string.osnma_label_subframes,
                    stringResource(
                        R.string.osnma_subframes_value,
                        s.subframesCompletedCounted,
                        s.subframesStarted,
                        (it * 100).roundToInt()
                    )
                )
            }
            status.gst?.let { DetailRow(R.string.osnma_label_gst, formatGst(it)) }
            DetailRow(
                R.string.osnma_label_timing,
                stringResource(
                    if (state.timingUsesSubframeNumbers) R.string.osnma_timing_subframe_numbers else R.string.osnma_timing_word_types,
                    state.timingCorrections
                )
            )
            state.clockOffsetSeconds?.let {
                DetailRow(R.string.osnma_label_clock_offset, stringResource(R.string.osnma_clock_offset_value, it))
            }
            if (state.trustingPlatformParity) {
                SmallText(stringResource(R.string.osnma_trusting_parity), FontStyle.Italic)
            }
            status.events.takeLast(MAX_EVENTS_SHOWN).reversed().forEach { e ->
                val color = when (e.type) {
                    OsnmaEvent.Type.ERROR -> MaterialTheme.colors.error
                    OsnmaEvent.Type.WARNING -> amberText
                    OsnmaEvent.Type.SUCCESS -> greenText
                    OsnmaEvent.Type.INFO -> MaterialTheme.colors.onSurface
                }
                Text(
                    text = "${formatGst(e.gst)}  ${e.message}",
                    fontSize = 11.sp,
                    color = color,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        SmallText(stringResource(R.string.osnma_explanation), FontStyle.Italic)
    }
}

@Composable
private fun receptionHint(state: OsnmaUiState): String {
    val cause = if (state.pagesStalled) {
        stringResource(R.string.osnma_reception_stalled)
    } else {
        val percent = ((state.status?.statistics?.subframeCompleteness ?: 0.0) * 100).roundToInt()
        stringResource(R.string.osnma_reception_incomplete, percent)
    }
    return cause + " " + stringResource(R.string.osnma_reception_advice)
}

@Composable
private fun DetailRow(@StringRes label: Int, value: String, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.padding(top = 2.dp)) {
        Text(
            text = stringResource(label),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.defaultMinSize(minWidth = 110.dp)
        )
        Text(text = value, fontSize = 13.sp, color = color)
    }
}

@Composable
private fun SmallText(text: String, fontStyle: FontStyle = FontStyle.Normal) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontStyle = fontStyle,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/** Satellites with a failed tag in the last [RECENT_FAILURE_SECONDS] */
private fun recentFailures(status: OsnmaStatus): List<SatelliteAuthStatus> {
    val now = status.gst ?: return emptyList()
    return status.satellites.filter { sat ->
        val last = sat.lastFailureGst
        last != null && now.totalSeconds - last.totalSeconds <= RECENT_FAILURE_SECONDS
    }
}

/** Formats a GST as "WN 1385 Tue 14:30:00" */
private fun formatGst(gst: Gst): String {
    val day = gst.tow / 86400
    val secondOfDay = gst.tow % 86400
    val days = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    return "WN %d %s %02d:%02d:%02d".format(
        gst.wn, days[day], secondOfDay / 3600, (secondOfDay / 60) % 60, secondOfDay % 60
    )
}

private const val MAX_EVENTS_SHOWN = 10
private const val RECENT_FAILURE_SECONDS = 600L
