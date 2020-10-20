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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.android.gpstest.Application;
import com.android.gpstest.R;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * A GNSS logger to store information to a JSON file. Originally from https://github.com/google/gps-measurement-tools/tree/master/GNSSLogger,
 * modified for GPSTest.
 */
public class JsonFileLogger extends BaseFileLogger implements FileLogger {

    ObjectMapper mapper;
    JsonGenerator jsonGenerator;

    public JsonFileLogger(Context context) {
        super(context);
    }

    @Override
    String getFileExtension() {
        return "json";
    }

    @Override
    void writeFileHeader(BufferedWriter writer, String filePath) {
        // No-op for JSON files
    }

    @Override
    boolean postFileInit(BufferedWriter fileWriter, boolean isNewFile) {
        if (jsonGenerator == null) {
            try {
                jsonGenerator = mapper.getFactory().createGenerator(fileWriter);
                if (isNewFile) {
                    jsonGenerator.writeStartArray();
                }
            } catch (IOException e) {
                logException(Application.get().getString(R.string.unable_to_open_json_generator), e);
                return false;
            }
        }
        return true;
    }

    /**
     * Start a file logging process
     *
     * @param existingFile The existing file if file logging is to be continued, or null if a
     *                     new file should be created.
     * @param date The date and time to use for the file name
     * @return true if a new file was created, false if an existing file was used
     */
    @Override
    public boolean startLog(File existingFile, Date date) {
        if (mapper == null) {
            mapper = new ObjectMapper();
        }
        return super.startLog(existingFile, date);
    }

    @Override
    public void close() {
        if (fileWriter != null) {
            try {
                if (jsonGenerator != null) {
                    jsonGenerator.writeEndArray();
                    jsonGenerator.flush();
                    jsonGenerator.close();
                }
                mapper = null;
                jsonGenerator = null;
            } catch (IOException e) {
                logException("Unable to close jsonGenerator and mapper file streams.", e);
            }
        }
        super.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void onGnssAntennaInfoReceived(@NonNull List<GnssAntennaInfo> list) {
        try {
            if (mapper != null && jsonGenerator != null) {
                for (GnssAntennaInfo info : list) {
                    mapper.writeValue(jsonGenerator, info);
                }
            }
        } catch (IOException e) {
            logException("Unable to write antenna info to JSON", e);
        }
    }
}
