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

import com.android.gpstest.model.SatelliteStatus

/**
 * Utilities for sorting lists.  Java Comparator.comparing() is only available on API 24 and higher,
 * so the functions defined here allow us to do the same in Kotlin for all API versions.
 */
class SortUtil {
    companion object {
        /**
         * Sorts the [list] by the SatelliteStatus svid asc and returns the sorted list
         */
        fun sortById(list: List<SatelliteStatus>): List<SatelliteStatus> {
            return list.sortedWith(compareBy(SatelliteStatus::svid)).toMutableList()
        }

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
    }
}
