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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.gpstest.R
import com.android.gpstest.library.data.OsnmaUiState
import com.android.gpstest.library.data.ReceiverClockStatus
import com.android.gpstest.library.data.TimeSource
import com.android.gpstest.library.osnma.Gst
import com.android.gpstest.library.osnma.KeySource
import com.android.gpstest.library.osnma.MacFunction
import com.android.gpstest.library.osnma.NmaStatus
import com.android.gpstest.library.osnma.OsnmaCardState
import com.android.gpstest.library.osnma.OsnmaEvent
import com.android.gpstest.library.osnma.OsnmaMainState
import com.android.gpstest.library.osnma.OsnmaMark
import com.android.gpstest.library.osnma.OsnmaStatistics
import com.android.gpstest.library.osnma.OsnmaStatus
import com.android.gpstest.library.osnma.OsnmaStatus.Phase
import com.android.gpstest.library.osnma.OsnmaWarning
import com.android.gpstest.library.osnma.TimeSync
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
    val known = state.inavSeenBefore || state.inavPagesReceived > 0 || state.status != null ||
            state.keysRevoked
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
            val cardState = state.cardState
            Header(state, cardState, expanded)
            if (expanded) {
                val description = phaseDescription(state)
                Text(
                    text = if (cardState.main == OsnmaMainState.IN_PROGRESS) {
                        description + " " + stringResource(R.string.osnma_waiting_time)
                    } else {
                        description
                    },
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                if (state.clockOffsetTooLarge) {
                    val slowMac = state.timeSync == TimeSync.SLOW_MAC_ONLY
                    Text(
                        text = stringResource(
                            if (slowMac) {
                                R.string.osnma_clock_offset_slow_mac
                            } else {
                                R.string.osnma_clock_offset_warning
                            },
                            state.clockOffsetSeconds ?: 0L
                        ),
                        fontSize = 13.sp,
                        color = if (slowMac) amberText else MaterialTheme.colors.error,
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
                Details(status)
                Expanded(state)
            }
        }
    }
}

/** Remembers whether the card is expanded while the app runs (e.g. when switching screens) */
private var osnmaCardExpanded = false

/**
 * The header row: title, main state chip, the number of authenticated (green seal) and failing
 * (red seal) satellites, a progress indicator while authentication is in progress, and the
 * expand/collapse chevron
 */
@Composable
private fun Header(state: OsnmaUiState, cardState: OsnmaCardState, expanded: Boolean) {
    val status = state.status
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Title and chips on one line; chips that don't fit continue on the next line
        ChipFlow(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.osnma_title),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1
            )
            MainStateChip(cardState.main)
            cardState.warnings.forEach { WarningChip(it) }
        }
        val authenticated = status?.authenticatedSatellites?.size ?: 0
        val failed = status?.recentFailures()?.size ?: 0
        if (authenticated > 0) SatelliteCount(OsnmaMark.AUTHENTICATED, authenticated)
        if (failed > 0) SatelliteCount(OsnmaMark.FAILED, failed)
        if (cardState.main == OsnmaMainState.IN_PROGRESS && !state.pagesStalled) {
            val progressDescription = stringResource(R.string.osnma_in_progress)
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(16.dp)
                    .semantics { contentDescription = progressDescription },
                strokeWidth = 2.dp
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_osnma_expand_more),
            contentDescription = stringResource(
                if (expanded) R.string.osnma_collapse else R.string.osnma_expand
            ),
            modifier = Modifier
                .padding(start = 4.dp)
                .size(24.dp)
                .rotate(if (expanded) 180f else 0f)
        )
    }
}

/**
 * A seal followed by a number of satellites, e.g. "✔ 7". Only the number is shown, so that the
 * header fits on narrow screens next to the chip; screen readers get the full sentence.
 */
@Composable
private fun SatelliteCount(mark: OsnmaMark, count: Int) {
    val description = stringResource(
        if (mark == OsnmaMark.AUTHENTICATED) {
            R.string.osnma_count_authenticated
        } else {
            R.string.osnma_count_failed
        },
        count
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(start = 8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
    ) {
        OsnmaMarkIcon(mark)
        Text(
            text = count.toString(),
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}

/** The main state chip (see [OsnmaMainState]) */
@Composable
private fun MainStateChip(main: OsnmaMainState) {
    val (color, textId) = when (main) {
        OsnmaMainState.NO_DATA -> Pair(Gray, R.string.osnma_chip_no_data)
        OsnmaMainState.IN_PROGRESS -> Pair(Amber, R.string.osnma_chip_waiting)
        OsnmaMainState.AUTHENTICATED -> Pair(Green, R.string.osnma_chip_authenticated)
        OsnmaMainState.DONT_USE -> Pair(MaterialTheme.colors.error, R.string.osnma_chip_dont_use)
        OsnmaMainState.ALERT -> Pair(MaterialTheme.colors.error, R.string.osnma_chip_alert)
        OsnmaMainState.TIME_NOT_VERIFIED ->
            Pair(MaterialTheme.colors.error, R.string.osnma_chip_time)
    }
    Chip(stringResource(textId), color)
}

/** The chip of a warning (see [OsnmaWarning]), shown next to the main state chip */
@Composable
private fun WarningChip(warning: OsnmaWarning) {
    val (color, textId) = when (warning) {
        OsnmaWarning.FAILED_TAGS -> Pair(MaterialTheme.colors.error, R.string.osnma_chip_failures)
        OsnmaWarning.POOR_RECEPTION -> Pair(Amber, R.string.osnma_chip_poor_reception)
        OsnmaWarning.SLOW_MAC_ONLY -> Pair(Amber, R.string.osnma_chip_slow_mac)
    }
    Chip(stringResource(textId), color)
}

/**
 * Places its children side by side, vertically centered, and continues on the next line when
 * the width runs out (like FlowRow, which this Compose version doesn't have)
 */
@Composable
private fun ChipFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val gapPx = with(LocalDensity.current) { 6.dp.roundToPx() }
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
        // Split into lines that fit the available width
        val lines = mutableListOf<MutableList<Placeable>>()
        var lineWidth = 0
        placeables.forEach { p ->
            val line = lines.lastOrNull()
            if (line == null || lineWidth + gapPx + p.width > constraints.maxWidth) {
                lines.add(mutableListOf(p))
                lineWidth = p.width
            } else {
                line.add(p)
                lineWidth += gapPx + p.width
            }
        }
        val lineHeights = lines.map { line -> line.maxOf { it.height } }
        val height = lineHeights.sum() + gapPx / 2 * (lines.size - 1).coerceAtLeast(0)
        val contentWidth = lines.maxOfOrNull { line ->
            line.sumOf { it.width } + gapPx * (line.size - 1)
        } ?: 0
        layout(
            contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth),
            height.coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            var y = 0
            lines.forEachIndexed { i, line ->
                var x = 0
                line.forEach { p ->
                    p.placeRelative(x, y + (lineHeights[i] - p.height) / 2)
                    x += p.width + gapPx
                }
                y += lineHeights[i] + gapPx / 2
            }
        }
    }
}

/** A small rounded label with white bold text on [color] */
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

/** One sentence explaining the current state, shown at the top of the expanded card */
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
        Phase.WAITING_FOR_TAGS -> if (state.clockOffsetSeconds == null) {
            stringResource(R.string.osnma_phase_waiting_time)
        } else {
            stringResource(R.string.osnma_phase_waiting_tags)
        }
        Phase.AUTHENTICATING -> stringResource(
            R.string.osnma_phase_authenticating,
            status.authenticatedSatellites.size
        )
        Phase.DONT_USE -> stringResource(R.string.osnma_phase_dont_use)
        Phase.ALERT -> stringResource(R.string.osnma_phase_alert)
    }
}

/**
 * The main fields. They're always shown, with a placeholder until they're known, so that the card
 * keeps its layout while authentication progresses.
 */
@Composable
private fun Details(status: OsnmaStatus?) {
    val header = status?.nmaHeader
    DetailRow(
        R.string.osnma_label_status,
        if (header != null) {
            val nmas = when (header.nmaStatus) {
                NmaStatus.OPERATIONAL -> stringResource(R.string.osnma_nmas_operational)
                NmaStatus.TEST -> stringResource(R.string.osnma_nmas_test)
                NmaStatus.DONT_USE -> stringResource(R.string.osnma_nmas_dont_use)
                NmaStatus.RESERVED -> stringResource(R.string.osnma_nmas_reserved)
            }
            nmas + if (status.nmaHeaderAuthenticated) " ✓" else ""
        } else {
            stringResource(R.string.osnma_value_unknown)
        }
    )

    val key = status?.publicKey
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

    val tesla = status?.teslaKey
    DetailRow(
        R.string.osnma_label_chain,
        if (tesla != null) {
            val mac = if (tesla.chain.macFunction == MacFunction.HMAC_SHA_256) {
                "HMAC-SHA-256"
            } else {
                "CMAC-AES"
            }
            val chain = stringResource(
                R.string.osnma_chain_value,
                tesla.chain.chainId,
                mac,
                tesla.chain.tagSizeBits,
                formatGst(tesla.gstSubframe)
            )
            when {
                status.warmStartBuiltIn ->
                    chain + " " + stringResource(R.string.osnma_chain_built_in)
                status.warmStart -> chain + " " + stringResource(R.string.osnma_chain_warm_start)
                else -> chain
            }
        } else {
            val progress = status?.krootProgress
            when {
                progress == null -> stringResource(R.string.osnma_kroot_waiting)
                progress.second > 0 ->
                    stringResource(R.string.osnma_kroot_progress, progress.first, progress.second)
                else -> stringResource(R.string.osnma_kroot_progress_unknown, progress.first)
            }
        }
    )

    val authenticated = status?.authenticatedSatellites.orEmpty()
    if (authenticated.isNotEmpty()) {
        DetailRow(
            R.string.osnma_label_authenticated,
            authenticated.joinToString(" ") { "E%02d".format(it.svn) },
            color = greenText
        )
    } else {
        DetailRow(R.string.osnma_label_authenticated, stringResource(R.string.osnma_value_none_yet))
    }
    val failed = status?.recentFailures().orEmpty()
    if (failed.isNotEmpty()) {
        DetailRow(
            R.string.osnma_label_failed,
            failed.joinToString(" ") { "E%02d (%d)".format(it.svn, it.tagsFailed) },
            color = MaterialTheme.colors.error
        )
    } else {
        DetailRow(R.string.osnma_label_failed, stringResource(R.string.osnma_value_none))
    }
}

/**
 * The diagnostic part of the expanded card, below [Details]: tag and page counters, data layout,
 * device parity, subframe completeness, Galileo time, page timing, clock offset, the latest
 * events and a short explanation of OSNMA
 */
@Composable
private fun Expanded(state: OsnmaUiState) {
    val status = state.status
    val s = status?.statistics ?: OsnmaStatistics()
    val unknown = stringResource(R.string.osnma_value_unknown)
    Column(modifier = Modifier.padding(top = 6.dp)) {
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
                state.pagesCorrupted + s.crcErrors,
                state.pagesRepaired
            )
        )
        DetailRow(
            R.string.osnma_label_layout,
            when (state.layoutOffsetBits) {
                null -> unknown
                0 -> stringResource(R.string.osnma_layout_standard)
                else -> stringResource(R.string.osnma_layout_shifted)
            }
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
        DetailRow(
            R.string.osnma_label_subframes,
            s.subframeCompleteness?.let {
                stringResource(
                    R.string.osnma_subframes_value,
                    s.subframesCompletedCounted,
                    s.subframesStarted,
                    (it * 100).roundToInt()
                )
            } ?: unknown
        )
        DetailRow(R.string.osnma_label_gst, status?.gst?.let { formatGst(it) } ?: unknown)
        val timing = stringResource(
            when {
                state.timingUsesReceiverClock -> R.string.osnma_timing_receiver_clock
                state.timingUsesSubframeNumbers -> R.string.osnma_timing_subframe_numbers
                else -> R.string.osnma_timing_word_types
            },
            state.timingCorrections
        )
        val receiverClock = receiverClockReason(state.receiverClockStatus)
        DetailRow(
            R.string.osnma_label_timing,
            if (state.timingUsesReceiverClock || receiverClock == null) {
                timing
            } else {
                timing + " " + stringResource(R.string.osnma_receiver_clock_unused, receiverClock)
            }
        )
        DetailRow(
            R.string.osnma_label_clock_offset,
            state.clockOffsetSeconds?.let {
                stringResource(
                    when (state.timeSource) {
                        TimeSource.TRUSTED_TIME -> R.string.osnma_clock_offset_trusted
                        TimeSource.NETWORK_TIME -> R.string.osnma_clock_offset_network
                        TimeSource.DEVICE_CLOCK -> R.string.osnma_clock_offset_device
                    },
                    it
                )
            } ?: unknown
        )
        if (state.trustingPlatformParity) {
            SmallText(stringResource(R.string.osnma_trusting_parity), FontStyle.Italic)
        }
        status?.events?.takeLast(MAX_EVENTS_SHOWN)?.reversed()?.forEach { e ->
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
        SmallText(stringResource(R.string.osnma_explanation), FontStyle.Italic)
    }
}

/** Why the receiver clock isn't used for page timing, or null if it is (or nearly) */
@Composable
private fun receiverClockReason(status: ReceiverClockStatus): String? = when (status) {
    ReceiverClockStatus.OK -> null
    ReceiverClockStatus.NO_MEASUREMENTS ->
        stringResource(R.string.osnma_receiver_clock_no_measurements)
    ReceiverClockStatus.ANDROID_TOO_OLD -> stringResource(R.string.osnma_receiver_clock_android)
    ReceiverClockStatus.NO_FULL_BIAS -> stringResource(R.string.osnma_receiver_clock_no_full_bias)
    ReceiverClockStatus.UNCERTAIN -> stringResource(R.string.osnma_receiver_clock_uncertain)
    ReceiverClockStatus.NO_ELAPSED_TIME -> stringResource(R.string.osnma_receiver_clock_no_elapsed)
    ReceiverClockStatus.INVALID -> stringResource(R.string.osnma_receiver_clock_invalid)
}

/** Hint shown when too many Galileo pages are missing or they stopped arriving */
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

/** A row of the card: bold [label] on the left, [value] on the right */
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

/** Small text, e.g. for the explanation at the bottom of the card */
@Composable
private fun SmallText(text: String, fontStyle: FontStyle = FontStyle.Normal) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontStyle = fontStyle,
        modifier = Modifier.padding(top = 6.dp)
    )
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
