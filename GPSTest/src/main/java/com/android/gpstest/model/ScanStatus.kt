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
package com.android.gpstest.model

/**
 * Contains information about the status of the current scan of GNSS signals. Once a scan (which
 * lasts [scanDurationMs]) is complete (indicated by [finishedScanningCfs]), judgements about the
 * device capabilities are made. Time left until the scan is complete is defined by
 * [timeUntilScanCompleteMs].
 *
 */
data class ScanStatus(
    val finishedScanningCfs: Boolean,
    val timeUntilScanCompleteMs: Long,
    val scanDurationMs: Long
)
