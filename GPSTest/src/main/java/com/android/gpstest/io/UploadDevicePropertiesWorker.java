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
import android.util.Log;

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

    public static final String MODEL = "model";

    public static final String ANDROID_VERSION = "androidVersion";

    private static final String RESULT_OK = "STATUS OK";

    public UploadDevicePropertiesWorker(@NonNull Context context,
                                        @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String model = getInputData().getString(MODEL);
        String androidVersion = getInputData().getString(ANDROID_VERSION);
        uploadDeviceProperties(model, androidVersion);
        return Result.success();
    }

    private void uploadDeviceProperties(String model, String androidVersion) {
        Uri uri = buildUri(model, androidVersion);
        try {
            Log.d(TAG, uri.toString());
            URL url = new URL(uri.toString());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(30 * 1000);

            Reader reader = new InputStreamReader(
                    new BufferedInputStream(connection.getInputStream(), 8 * 1024));
            String result = IOUtils.toString(reader);
            if (RESULT_OK.equals(result)) {
                Log.d(TAG, "Successfully uploaded device properties");
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }
    }

    private Uri buildUri(String model, String androidVersion) {
        return Uri.parse(Application.get().getResources().getString(R.string.
                device_properties_upload_url)).buildUpon()
                .appendQueryParameter("model", model)
                .appendQueryParameter("androidVersion", androidVersion)
                .build();
    }
}
