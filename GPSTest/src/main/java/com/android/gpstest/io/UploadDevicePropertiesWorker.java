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
package com.android.gpstest.io;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.gpstest.Application;
import com.android.gpstest.R;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UploadDevicePropertiesWorker extends Worker {

    private static final String TAG = "UploadDevicePropsWorker";

    public static final String MANUFACTURER = "manufacturer";
    public static final String MODEL = "model";
    public static final String ANDROID_VERSION = "androidVersion";
    public static final String API_LEVEL = "apiLevel";
    public static final String GNSS_HARDWARE_YEAR = "gnssHardwareYear";
    public static final String GNSS_HARDWARE_MODEL_NAME = "gnssHardwareModelName";
    public static final String DUAL_FREQUENCY = "duelFrequency";
    public static final String SUPPORTED_GNSS = "supportedGnss";
    public static final String GNSS_CFS = "gnssCfs";
    public static final String SUPPORTED_SBAS = "supportedSbas";
    public static final String SBAS_CFS = "sbasCfs";
    public static final String RAW_MEASUREMENTS = "rawMeasurements";
    public static final String NAVIGATION_MESSAGES = "navigationMessages";
    public static final String NMEA = "nmea";
    public static final String INJECT_PSDS = "injectPsds";
    public static final String INJECT_TIME = "injectTime";
    public static final String DELETE_ASSIST = "deleteAssist";
    public static final String ACCUMULATED_DELTA_RANGE = "accumulatedDeltaRange";
    public static final String HARDWARE_CLOCK = "hardwareClock";
    public static final String HARDWARE_CLOCK_DISCONTINUITY = "hardwareClockDiscontinuity";
    public static final String AUTOMATIC_GAIN_CONTROL = "automaticGainControl";
    public static final String GNSS_ANTENNA_INFO = "gnssAntennaInfo";
    public static final String APP_VERSION_NAME = "appVersionName";
    public static final String APP_VERSION_CODE = "appVersionCode";
    public static final String APP_BUILD_FLAVOR = "appBuildFlavor";

    private static final String RESULT_OK = "STATUS OK";

    public UploadDevicePropertiesWorker(@NonNull Context context,
                                        @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        uploadDeviceProperties(buildUri());
        return Result.success();
    }

    private Uri buildUri() {
        return Uri.parse(Application.get().getResources().getString(R.string.
                device_properties_upload_url)).buildUpon()
                .appendQueryParameter(MANUFACTURER, getInputData().getString(MANUFACTURER))
                .appendQueryParameter(MODEL, getInputData().getString(MODEL))
                .appendQueryParameter(ANDROID_VERSION, getInputData().getString(ANDROID_VERSION))
                .appendQueryParameter(API_LEVEL, getInputData().getString(API_LEVEL))
                .appendQueryParameter(GNSS_HARDWARE_YEAR, getInputData().getString(GNSS_HARDWARE_YEAR))
                .appendQueryParameter(GNSS_HARDWARE_MODEL_NAME, getInputData().getString(GNSS_HARDWARE_MODEL_NAME))
                .appendQueryParameter(DUAL_FREQUENCY, getInputData().getString(DUAL_FREQUENCY))
                .appendQueryParameter(SUPPORTED_GNSS, getInputData().getString(SUPPORTED_GNSS))
                .appendQueryParameter(GNSS_CFS, getInputData().getString(GNSS_CFS))
                .appendQueryParameter(SUPPORTED_SBAS, getInputData().getString(SUPPORTED_SBAS))
                .appendQueryParameter(SBAS_CFS, getInputData().getString(SBAS_CFS))
                .appendQueryParameter(RAW_MEASUREMENTS, getInputData().getString(RAW_MEASUREMENTS))
                .appendQueryParameter(NAVIGATION_MESSAGES, getInputData().getString(NAVIGATION_MESSAGES))
                .appendQueryParameter(NMEA, getInputData().getString(NMEA))
                .appendQueryParameter(INJECT_PSDS, getInputData().getString(INJECT_PSDS))
                .appendQueryParameter(INJECT_TIME, getInputData().getString(INJECT_TIME))
                .appendQueryParameter(DELETE_ASSIST, getInputData().getString(DELETE_ASSIST))
                .appendQueryParameter(ACCUMULATED_DELTA_RANGE, getInputData().getString(ACCUMULATED_DELTA_RANGE))
                .appendQueryParameter(HARDWARE_CLOCK, getInputData().getString(HARDWARE_CLOCK))
                .appendQueryParameter(HARDWARE_CLOCK_DISCONTINUITY, getInputData().getString(HARDWARE_CLOCK_DISCONTINUITY))
                .appendQueryParameter(AUTOMATIC_GAIN_CONTROL, getInputData().getString(AUTOMATIC_GAIN_CONTROL))
                .appendQueryParameter(GNSS_ANTENNA_INFO, getInputData().getString(GNSS_ANTENNA_INFO))
                .appendQueryParameter(APP_VERSION_NAME, getInputData().getString(APP_VERSION_NAME))
                .appendQueryParameter(APP_VERSION_CODE, getInputData().getString(APP_VERSION_CODE))
                .appendQueryParameter(APP_BUILD_FLAVOR, getInputData().getString(APP_BUILD_FLAVOR))
                .build();
    }

    private void uploadDeviceProperties(Uri uri) {
        try {
            Log.d(TAG, uri.toString());
            URL url = new URL(uri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(30 * 1000);

            Reader reader = new InputStreamReader(
                    new BufferedInputStream(connection.getInputStream(), 8 * 1024));
            String result = IOUtils.toString(reader);
            if (RESULT_OK.equals(result)) {
                Log.d(TAG, "Successfully uploaded device capabilities!");
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(() -> Toast.makeText(Application.get(), R.string.upload_success, Toast.LENGTH_SHORT).show());
            } else {
                logFailure(null);
            }
        } catch (IOException e) {
            logFailure(e);
        }
    }

    private void logFailure(IOException e) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(Application.get(), R.string.upload_failure, Toast.LENGTH_SHORT).show());
        if (e != null) {
            Log.e(TAG, e.toString());
        }
    }
}
