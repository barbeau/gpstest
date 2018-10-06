/*
 * Copyright (C) 2018 Sean J. Barbeau
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
package com.android.gpstest.util

import android.os.Build
import com.android.gpstest.model.SatelliteStatus

/**
 * Utilities for sorting lists.  Java Comparator.comparing() is only available on API 24 and higher,
 * so the functions defined here allow us to do the same in Kotlin for all API versions.
 */
class SortUtil {
    companion object {
        /**
         * Sorts the [list] by the SatelliteStatus gnssType then svid asc and returns the sorted list
         */
        fun sortByGnssThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            return list.sortedWith(compareBy(SatelliteStatus::gnssType, SatelliteStatus::svid)).toMutableList()
        }

        /**
         * Sorts the [list] by the SatelliteStatus sbasType then svid asc and returns the sorted list
         */
        fun sortBySbasThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            return list.sortedWith(compareBy(SatelliteStatus::sbasType, SatelliteStatus::svid)).toMutableList()
        }

        /**
         * Sorts the [list] by the SatelliteStatus usedInFix desc then svid asc and returns the sorted list
         */
        fun sortByUsedThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return list.sortedWith(compareByDescending(SatelliteStatus::usedInFix).thenComparing(SatelliteStatus::svid)).toMutableList()
            } else {
                // We don't explicitly sort by ID on M and lower
                return list.sortedWith(compareByDescending(SatelliteStatus::usedInFix)).toMutableList()
            }
        }

        /**
         * Sorts the [list] by the SatelliteStatus C/N0 desc and returns the sorted list
         */
        fun sortByCn0(list: List<SatelliteStatus>): List<SatelliteStatus> {
            return list.sortedWith(compareByDescending(SatelliteStatus::cn0DbHz)).toMutableList()
        }

        /**
         * Sorts the [list] by the SatelliteStatus carrier frequency then svid and returns the sorted list
         */
        fun sortByCarrierFrequencyThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            return list.sortedWith(compareBy(SatelliteStatus::carrierFrequencyHz, SatelliteStatus::svid)).toMutableList()
        }

        /**
         * Sorts the [list] by the SatelliteStatus gnssType then usedInFix desc then svid asc and returns the sorted list
         */
        fun sortByGnssThenUsedThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return list.sortedWith(compareBy(SatelliteStatus::gnssType).thenByDescending(SatelliteStatus::usedInFix).thenComparing(SatelliteStatus::svid)).toMutableList()
            } else {
                // We sort all by ascending on M and lower
                return list.sortedWith(compareBy(SatelliteStatus::gnssType, SatelliteStatus::usedInFix, SatelliteStatus::svid)).toMutableList()
            }
        }

        /**
         * Sorts the [list] by the SatelliteStatus sbasType then usedInFix desc then svid asc and returns the sorted list
         */
        fun sortBySbasThenUsedThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return list.sortedWith(compareBy(SatelliteStatus::sbasType).thenByDescending(SatelliteStatus::usedInFix).thenComparing(SatelliteStatus::svid)).toMutableList()
            } else {
                // We sort all by ascending on M and lower
                return list.sortedWith(compareBy(SatelliteStatus::sbasType, SatelliteStatus::usedInFix, SatelliteStatus::svid)).toMutableList()
            }
        }

        /**
         * Sorts the [list] by the SatelliteStatus gnssType then C/N0 desc and returns the sorted list
         */
        fun sortByGnssThenCn0ThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return list.sortedWith(compareBy(SatelliteStatus::gnssType).thenByDescending(SatelliteStatus::cn0DbHz)).toMutableList()
            } else {
                // We sort all by ascending on M and lower
                return list.sortedWith(compareBy(SatelliteStatus::gnssType, SatelliteStatus::cn0DbHz)).toMutableList()
            }
        }

        /**
         * Sorts the [list] by the SatelliteStatus sbasType then C/N0 desc and returns the sorted list
         */
        fun sortBySbasThenCn0ThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return list.sortedWith(compareBy(SatelliteStatus::sbasType).thenByDescending(SatelliteStatus::cn0DbHz)).toMutableList()
            } else {
                // We sort all by ascending on M and lower
                return list.sortedWith(compareBy(SatelliteStatus::sbasType, SatelliteStatus::cn0DbHz)).toMutableList()
            }
        }

        /**
         * Sorts the [list] by the SatelliteStatus gnssType then carrier frequency then svid and returns the sorted list
         */
        fun sortByGnssThenCarrierFrequencyThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            return list.sortedWith(compareBy(SatelliteStatus::gnssType, SatelliteStatus::carrierFrequencyHz, SatelliteStatus::svid)).toMutableList()
        }

        /**
         * Sorts the [list] by the SatelliteStatus sbasType then carrier frequency then svid and returns the sorted list
         */
        fun sortBySbasThenCarrierFrequencyThenId(list: List<SatelliteStatus>): List<SatelliteStatus> {
            return list.sortedWith(compareBy(SatelliteStatus::sbasType, SatelliteStatus::carrierFrequencyHz, SatelliteStatus::svid)).toMutableList()
        }
    }
}
