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
import android.location.GnssMeasurementsEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.R
import com.android.gpstest.io.BaseFileLogger
import com.android.gpstest.io.FileLogger
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.util.rinex.RinexObservationWriting
import java.io.BufferedWriter
import java.io.IOException
import java.util.Calendar
import java.util.SimpleTimeZone
import java.util.TimeZone

/**
 * A GNSS logger to store information to a CSV file. Originally from
 * https://github.com/rokubun/android_rinex, modified for GPSTest.
 */
class RinexObservationFileLogger(context: Context) : BaseFileLogger(context), FileLogger {

    /**
     * Cannot write the header until there is a first observation
     */
    private var writeHeaderWhenPossible = false

    private val runAtTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"))

    override fun getFileExtension(): String {
        val weekNumber = runAtTime.get(Calendar.WEEK_OF_YEAR)
        return "${weekNumber}o"
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
        // Cannot write the header until we have the first observation
        writeHeaderWhenPossible = true
    }

    @Synchronized
    fun onGnssMeasurementsReceived(event: GnssMeasurementsEvent) {
        if (fileWriter == null) {
            return
        }

        if (writeHeaderWhenPossible) {
            /* @see {RinexObservationWriting::getRnxAttr} */
            val allPossibleObservations = mapOf(
                GnssType.NAVSTAR to listOf("C1C", "L1C", "D1C", "S1C"),
                GnssType.NAVSTAR to listOf("C2C", "L2C", "D2C", "S2C"),

                GnssType.GLONASS to listOf("C1B", "L1B", "D1B", "S1B"),
                GnssType.GLONASS to listOf("C2C", "L2C", "D2C", "S2C"),

                GnssType.GALILEO to listOf("C1C", "L1C", "D1C", "S1C"),
                GnssType.GALILEO to listOf("C5X", "L5X", "D5X", "S5X"),

                GnssType.QZSS to listOf("C1C", "L1C", "D1C", "S1C"),
                GnssType.QZSS to listOf("C1X", "L1X", "D1X", "S1X"),

                GnssType.BEIDOU to listOf("C2I", "L2I", "D2I", "S2I"),

                //GnssType.IRNSS to listOf("C5X", "L5X", "D5X", "S5X"), //TODO support

                GnssType.SBAS to listOf("C1C", "L1C", "D1C", "S1C"),
                GnssType.SBAS to listOf("C1X", "L1X", "D1X", "S1X"), //TODO validate data
            )
            val header = RinexObservationWriting.generateHeader(allPossibleObservations, event.clock, runAtTime)
            try {
                fileWriter.append(header)
            } catch (e: IOException) {
                logException(app.getString(R.string.error_writing_file), e)
                return
            }
            writeHeaderWhenPossible = false
        }

        val observations = RinexObservationWriting.generateObservationsRinexString(event.clock, event.measurements)

        try {
            fileWriter.append(observations)
        } catch (e: IOException) {
            logException(app.getString(R.string.error_writing_file), e)
        }
    }

}
