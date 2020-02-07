/*
 * Copyright (C) 2020 Sean J. Barbeau
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
 * A container class that holds metadata and statistics information about a group of satellites.
 * Summary statistics on the constellation family such as the number of signals in view
 * ([numSignalsInView]), number of signals used in the fix ([numSignalsUsed], and the number
 * of satellites used in the fix ([numSatsUsed]), and the number of satellites in view ([numSatsInView])
 */
data class SatelliteMetadata(
        val numSignalsInView: Int,
        val numSignalsUsed: Int,
        val numSignalsTotal: Int,
        val numSatsInView: Int,
        val numSatsUsed: Int,
        val numSatsTotal: Int)