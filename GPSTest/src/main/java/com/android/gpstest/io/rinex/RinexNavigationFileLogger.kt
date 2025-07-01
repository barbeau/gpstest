/*
 * Copyright (C) 2017-2019 The Android Open Source Project, Sean J. Barbeau
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
package com.android.gpstest.io.rinex

import android.content.Context
import android.location.GnssNavigationMessage
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.R
import com.android.gpstest.io.BaseFileLogger
import com.android.gpstest.io.FileLogger
import com.android.gpstest.library.rinex.navigation.GnssEphemerisListener
import com.android.gpstest.library.rinex.navigation.GnssNavigationMessageDecoder
import com.android.gpstest.library.rinex.navigation.GpsEphemeris
import com.android.gpstest.library.util.rinex.RinexNavigationWriting
import com.android.gpstest.library.util.rinex.RinexObservationWriting
import java.io.BufferedWriter
import java.io.IOException
import java.util.Calendar
import java.util.Date
import java.util.SimpleTimeZone
import java.util.TimeZone

/**
 * A GNSS logger to store information to a CSV file. Originally from
 * https://github.com/rokubun/android_rinex, modified for GPSTest.
 */
class RinexNavigationFileLogger(context: Context) : BaseFileLogger(context), FileLogger,
    GnssEphemerisListener {

    private val gnssNavigationMessageDecoder = GnssNavigationMessageDecoder(this)
    private val runAtDate = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    override fun getFileExtension(): String {
        val weekNumber = runAtDate.get(Calendar.WEEK_OF_YEAR)
        return "${weekNumber}n"
    }

    override fun postFileInit(fileWriter: BufferedWriter, isNewFile: Boolean): Boolean {
        ContextCompat.getMainExecutor(context).execute {
            Toast.makeText(
                app.applicationContext,
                app.getString(
                    R.string.logging_to_new_file,
                    file.absolutePath
                ),
                Toast.LENGTH_LONG
            ).show()
        }
        return true
    }

    /**
     * Initialize file by adding a RINEX header
     *
     * @param writer   writer to use when writing file
     * @param filePath path to the current file
     */
    override fun writeFileHeader(writer: BufferedWriter, filePath: String) {
        val header = RinexNavigationWriting.generateHeader(runAtDate)
        try {
            writer.append(header)
        } catch (e: IOException) {
            logException(app.getString(R.string.error_writing_file), e)
            return
        }
    }

    @Synchronized
    fun onGnssNavigationMessageReceived(navigationMessage: GnssNavigationMessage) {
        gnssNavigationMessageDecoder.parseHwNavigationMessageUpdates(navigationMessage)
    }

    @Synchronized
    override fun onGpsEphemerisDecoded(gpsEphemeris: GpsEphemeris) {
        if (fileWriter == null) {
            return
        }

        val ephemeris = RinexNavigationWriting.generateNavigationRinexString(gpsEphemeris)

        try {
            fileWriter.append(ephemeris)
        } catch (e: IOException) {
            logException(app.getString(R.string.error_writing_file), e)
        }
    }


}
