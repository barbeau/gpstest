/*
 * Copyright (C) 2019-2020 University of South Florida, Sean Barbeau
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
package com.android.gpstest.io

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.android.gpstest.Application
import com.android.gpstest.R
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.net.URL

sealed class Result<out R> {
    data class Success<out T>(val data: T) : Result<Nothing>()
    data class Error(val exception: Exception) : Result<Nothing>()
}

class DevicePropertiesUploader(private val inputData: Bundle) {

    fun upload() : Result<out R> {
        val uri = buildUri()
        try {
            Log.d(TAG, uri.toString())
            val url = URL(uri.toString())
            val connection = url.openConnection() as HttpURLConnection
            connection.readTimeout = 30 * 1000
            val reader: Reader = InputStreamReader(
                    BufferedInputStream(connection.inputStream, 8 * 1024))
            val result = IOUtils.toString(reader)
            if (RESULT_OK == result) {
                Log.d(TAG, "Successfully uploaded device capabilities!")
                val handler = Handler(Looper.getMainLooper())
                handler.post { Toast.makeText(Application.get(), R.string.upload_success, Toast.LENGTH_SHORT).show() }
                return Result.Success(R.string.upload_success)
            } else {
                logFailure(null)
                return Result.Error(Exception(Application.get().getString(R.string.upload_failure)))
            }
        } catch (e: IOException) {
            logFailure(e)
            return Result.Error(Exception(Application.get().getString(R.string.upload_failure)))
        } finally {
            return Result.Error(Exception(Application.get().getString(R.string.upload_failure)))
        }
    }

    private fun buildUri(): Uri {
        return Uri.parse(Application.get().resources.getString(R.string.device_properties_upload_url)).buildUpon()
                .appendQueryParameter(MANUFACTURER, inputData.getString(MANUFACTURER))
                .appendQueryParameter(MODEL, inputData.getString(MODEL))
                .appendQueryParameter(ANDROID_VERSION, inputData.getString(ANDROID_VERSION))
                .appendQueryParameter(API_LEVEL, inputData.getString(API_LEVEL))
                .appendQueryParameter(GNSS_HARDWARE_YEAR, inputData.getString(GNSS_HARDWARE_YEAR))
                .appendQueryParameter(GNSS_HARDWARE_MODEL_NAME, inputData.getString(GNSS_HARDWARE_MODEL_NAME))
                .appendQueryParameter(DUAL_FREQUENCY, inputData.getString(DUAL_FREQUENCY))
                .appendQueryParameter(SUPPORTED_GNSS, inputData.getString(SUPPORTED_GNSS))
                .appendQueryParameter(GNSS_CFS, inputData.getString(GNSS_CFS))
                .appendQueryParameter(SUPPORTED_SBAS, inputData.getString(SUPPORTED_SBAS))
                .appendQueryParameter(SBAS_CFS, inputData.getString(SBAS_CFS))
                .appendQueryParameter(RAW_MEASUREMENTS, inputData.getString(RAW_MEASUREMENTS))
                .appendQueryParameter(NAVIGATION_MESSAGES, inputData.getString(NAVIGATION_MESSAGES))
                .appendQueryParameter(NMEA, inputData.getString(NMEA))
                .appendQueryParameter(INJECT_PSDS, inputData.getString(INJECT_PSDS))
                .appendQueryParameter(INJECT_TIME, inputData.getString(INJECT_TIME))
                .appendQueryParameter(DELETE_ASSIST, inputData.getString(DELETE_ASSIST))
                .appendQueryParameter(ACCUMULATED_DELTA_RANGE, inputData.getString(ACCUMULATED_DELTA_RANGE))
                .appendQueryParameter(HARDWARE_CLOCK, inputData.getString(HARDWARE_CLOCK))
                .appendQueryParameter(HARDWARE_CLOCK_DISCONTINUITY, inputData.getString(HARDWARE_CLOCK_DISCONTINUITY))
                .appendQueryParameter(AUTOMATIC_GAIN_CONTROL, inputData.getString(AUTOMATIC_GAIN_CONTROL))
                .appendQueryParameter(GNSS_ANTENNA_INFO, inputData.getString(GNSS_ANTENNA_INFO))
                .appendQueryParameter(APP_VERSION_NAME, inputData.getString(APP_VERSION_NAME))
                .appendQueryParameter(APP_VERSION_CODE, inputData.getString(APP_VERSION_CODE))
                .appendQueryParameter(APP_BUILD_FLAVOR, inputData.getString(APP_BUILD_FLAVOR))
                .build()
    }

    private fun logFailure(e: IOException?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post { Toast.makeText(Application.get(), R.string.upload_failure, Toast.LENGTH_SHORT).show() }
        if (e != null) {
            Log.e(TAG, e.toString())
        }
    }

    companion object {
        private const val TAG = "DevicePropsUploader"
        const val MANUFACTURER = "manufacturer"
        const val MODEL = "model"
        const val ANDROID_VERSION = "androidVersion"
        const val API_LEVEL = "apiLevel"
        const val GNSS_HARDWARE_YEAR = "gnssHardwareYear"
        const val GNSS_HARDWARE_MODEL_NAME = "gnssHardwareModelName"
        const val DUAL_FREQUENCY = "duelFrequency"
        const val SUPPORTED_GNSS = "supportedGnss"
        const val GNSS_CFS = "gnssCfs"
        const val SUPPORTED_SBAS = "supportedSbas"
        const val SBAS_CFS = "sbasCfs"
        const val RAW_MEASUREMENTS = "rawMeasurements"
        const val NAVIGATION_MESSAGES = "navigationMessages"
        const val NMEA = "nmea"
        const val INJECT_PSDS = "injectPsds"
        const val INJECT_TIME = "injectTime"
        const val DELETE_ASSIST = "deleteAssist"
        const val ACCUMULATED_DELTA_RANGE = "accumulatedDeltaRange"
        const val HARDWARE_CLOCK = "hardwareClock"
        const val HARDWARE_CLOCK_DISCONTINUITY = "hardwareClockDiscontinuity"
        const val AUTOMATIC_GAIN_CONTROL = "automaticGainControl"
        const val GNSS_ANTENNA_INFO = "gnssAntennaInfo"
        const val APP_VERSION_NAME = "appVersionName"
        const val APP_VERSION_CODE = "appVersionCode"
        const val APP_BUILD_FLAVOR = "appBuildFlavor"
        private const val RESULT_OK = "STATUS OK"
    }
}