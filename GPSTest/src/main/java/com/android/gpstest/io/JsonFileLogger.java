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

package com.android.gpstest.io;

import android.content.Context;
import android.location.GnssAntennaInfo;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.gpstest.Application;
import com.android.gpstest.R;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * A GNSS logger to store information to a file. Originally from https://github.com/google/gps-measurement-tools/tree/master/GNSSLogger,
 * modified for GPSTest.
 */
public class JsonFileLogger implements FileLogger {

    private static final String TAG = "FileLogger";
    private static final String FILE_PREFIX = "gnss_log";

    private final Context context;

    private final Object fileLock = new Object();
    private BufferedWriter fileWriter;
    private File file;
    private boolean isStarted = false;
    File baseDirectory;
    ObjectMapper mapper;
    JsonGenerator jsonGenerator;

    public JsonFileLogger(Context context) {
        this.context = context;
    }

    public File getFile() {
        return file;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Start a file logging process
     *
     * @param existingFile The existing file if file logging is to be continued, or null if a
     *                     new file should be created.
     */
    public void startLog(File existingFile, Date date) {
        synchronized (fileLock) {
            String state = Environment.getExternalStorageState();
            if (mapper == null) {
                mapper = new ObjectMapper();
            }
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);
                baseDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
                logError("Cannot read external storage.");
                return;
            }

            String currentFilePath;

            if (existingFile != null) {
                // Use existing file
                currentFilePath = existingFile.getAbsolutePath();
                BufferedWriter writer;
                try {
                    writer = new BufferedWriter(new FileWriter(existingFile));
                } catch (IOException e) {
                    logException("Could not open file: " + currentFilePath, e);
                    return;
                }
                if (fileWriter != null) {
                    try {
                        fileWriter.close();
                    } catch (IOException e) {
                        logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                        return;
                    }
                }
                file = existingFile;
                fileWriter = writer;
            } else {
                // Create new logging file
                SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
                String fileName = String.format("%s_%s.json", FILE_PREFIX, formatter.format(date));
                File currentFile = new File(baseDirectory, fileName);
                currentFilePath = currentFile.getAbsolutePath();
                BufferedWriter writer;
                try {
                    writer = new BufferedWriter(new FileWriter(currentFile));
                } catch (IOException e) {
                    logException("Could not open file: " + currentFilePath, e);
                    return;
                }

                if (fileWriter != null) {
                    try {
                        fileWriter.close();
                    } catch (IOException e) {
                        logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                        return;
                    }
                }

                file = currentFile;
                fileWriter = writer;

                Toast.makeText(context, Application.get().getString(R.string.logging_to_new_file, currentFilePath), Toast.LENGTH_LONG).show();
            }


            File file;
            if (existingFile != null) {
                // Use existing file
                currentFilePath = existingFile.getAbsolutePath();
                file = existingFile;
            } else {
                // Create new logging file
                SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
                String fileName = String.format("%s_%s.txt", FILE_PREFIX, formatter.format(new Date()));
                file = new File(baseDirectory, fileName);
                currentFilePath = file.getAbsolutePath();
            }

            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(file));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return;
            }

            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                    return;
                }
            }

            fileWriter = writer;
            this.file = file;

            if (jsonGenerator == null) {
                try {
                    jsonGenerator = mapper.getFactory().createGenerator(fileWriter);
                } catch (IOException e) {
                    logException(Application.get().getString(R.string.unable_to_open_json_generator), e);
                    return;
                }
            }

            if (existingFile == null) {
                // Only if file didn't previously exist
                Toast.makeText(context, Application.get().getString(R.string.logging_to_new_file, currentFilePath), Toast.LENGTH_LONG).show();
            }

            isStarted = true;
        }
    }

    /**
     * Returns true if the logger is already started, or false if it is not
     *
     * @return
     */
    public synchronized boolean isStarted() {
        return isStarted;
    }

    public void close() {
        if (fileWriter != null) {
            try {
                jsonGenerator.close();
                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
                mapper = null;
                isStarted = false;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void onGnssAntennaInfoReceived(@NonNull List<GnssAntennaInfo> list) {
        try {
            jsonGenerator.writeStartArray();
            for (GnssAntennaInfo info : list) {
                mapper.writeValue(jsonGenerator, info);
            }
            jsonGenerator.writeEndArray();
        } catch (IOException e) {
            logException("Unable to write antenna info to JSON", e);
        }
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(TAG, errorMessage, e);
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(TAG, errorMessage);
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show();
    }
}
