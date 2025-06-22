/*
 * Copyright (C) 2017 The Android Open Source Project
 * With substantial modifications
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

package com.android.gpstest.library.rinex.navigation

import android.location.GnssNavigationMessage

/**
 * Decodes navigations messages
 * Depdends extensively on routines developed for GNSSLogger at
 * https://github.com/google/gps-measurement-tools
 */
class GnssNavigationMessageDecoder(
        gnssEphemerisListener: GnssEphemerisListener,
    ) {

    private val gpsDecoder = GpsNavigationMessageStore(gnssEphemerisListener)

    /**
     * Parses a string array containing an updates to the navigation message and return the most
     * recent [GpsNavMessageProto].
     */
    fun parseHwNavigationMessageUpdates(navigationMessage: GnssNavigationMessage) {
        val messagePrn = navigationMessage.svid.toByte()
        val messageType = (navigationMessage.type shr 8).toByte()
        val subMessageId = navigationMessage.submessageId

        val messageRawData = navigationMessage.data
        // parse only GPS navigation messages for now
        if (messageType.toInt() == 1) {
            gpsDecoder.onNavMessageReported(
                messagePrn, messageType, subMessageId.toShort(), messageRawData
            )
        }

        //TODO parse other navigation messages
    }
}
