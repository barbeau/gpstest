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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.android.gpstest.Application;
import com.android.gpstest.BuildConfig;
import com.android.gpstest.R;
import com.android.gpstest.util.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A GNSS logger to store information to a file. Originally from https://github.com/google/gps-measurement-tools/tree/master/GNSSLogger,
 * modified for GPSTest.
 */
public class CsvFileLogger implements FileLogger {

    private static final String TAG = "FileLogger";
    private static final String FILE_PREFIX = "gnss_log";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "GPSTest version: ";

    private final Context context;

    private final Object fileLock = new Object();
    private BufferedWriter fileWriter;
    private File file;
    private boolean isStarted = false;
    private File baseDirectory;

    public CsvFileLogger(Context context) {
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
     * @param date The date and time to use for the file name
     * @return true if a new file was created, false if an existing file was used
     */
    public boolean startLog(File existingFile, Date date) {
        boolean isNewFile = false;
        synchronized (fileLock) {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);
                baseDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return false;
            } else {
                logError("Cannot read external storage.");
                return false;
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
                    return false;
                }
                if (fileWriter != null) {
                    try {
                        fileWriter.close();
                    } catch (IOException e) {
                        logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                        return false;
                    }
                }
                file = existingFile;
                fileWriter = writer;
            } else {
                // Create new logging file
                SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
                String fileName = String.format("%s_%s.txt", FILE_PREFIX, formatter.format(date));
                File currentFile = new File(baseDirectory, fileName);
                currentFilePath = currentFile.getAbsolutePath();
                BufferedWriter writer;
                try {
                    writer = new BufferedWriter(new FileWriter(currentFile));
                } catch (IOException e) {
                    logException("Could not open file: " + currentFilePath, e);
                    return false;
                }

                writeFileHeader(writer, currentFilePath);

                if (fileWriter != null) {
                    try {
                        fileWriter.close();
                    } catch (IOException e) {
                        logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                        return false;
                    }
                }

                file = currentFile;
                fileWriter = writer;

                Log.d(TAG, Application.get().getString(R.string.logging_to_new_file, currentFilePath));
                isNewFile = true;
            }


            File file;
            if (existingFile != null) {
                // Use existing file
                currentFilePath = existingFile.getAbsolutePath();
                file = existingFile;
            } else {
                // Create new logging file
                SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
                String fileName = String.format("%s_%s.txt", FILE_PREFIX, formatter.format(date));
                file = new File(baseDirectory, fileName);
                currentFilePath = file.getAbsolutePath();
            }

            BufferedWriter writer;
            try {
                writer = new BufferedWriter(new FileWriter(file));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return false;
            }

            if (existingFile == null) {
                // Only if file didn't previously exist
                writeFileHeader(writer, currentFilePath);
            }

            if (fileWriter != null) {
                try {
                    fileWriter.close();
                } catch (IOException e) {
                    logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                    return false;
                }
            }

            fileWriter = writer;
            this.file = file;

            if (existingFile == null) {
                // Only if file didn't previously exist
                Log.d(TAG, Application.get().getString(R.string.logging_to_new_file, currentFilePath));
                isNewFile = true;
            }

            isStarted = true;
        }
        return isNewFile;
    }

    /**
     * Initialize file by adding a header
     *
     * @param writer   writer to use when writing file
     * @param filePath path to the current file
     */
    private void writeFileHeader(BufferedWriter writer, String filePath) {
        try {
            writer.write(COMMENT_START);
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("Header Description:");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write(VERSION_TAG);
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;

            String versionString = "";
            int versionCode = 0;
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                versionString = info.versionName;
                versionCode = info.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            StringBuilder version = new StringBuilder();
            // Version info
            version.append("v")
                    .append(versionString)
                    .append(" (")
                    .append(versionCode)
                    .append("-" + BuildConfig.FLAVOR + "), ");

            version.append("Manufacturer: " + manufacturer + ", ");
            version.append("Model: " + model + ", ");

            version.append(IOUtils.getGnssHardwareYear() + ", ");

            String versionRelease = Build.VERSION.RELEASE;
            version.append("Platform: " + versionRelease + ", ");
            int apiLevel = Build.VERSION.SDK_INT;
            version.append("API Level: " + apiLevel + " ");

            writer.write(version.toString());
            writer.newLine();
            writer.write(COMMENT_START);
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("Raw GNSS measurements format:");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write(
                    "  Raw,ElapsedRealtimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,"
                            + "BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,"
                            + "HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,"
                            + "ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,"
                            + "PseudorangeRateUncertaintyMetersPerSecond,"
                            + "AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,"
                            + "AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,"
                            + "CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,"
                            + "ConstellationType,AgcDb,CarrierFrequencyHz");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("Location fix format:");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write(
                    "  Fix,Provider,Latitude,Longitude,Altitude,Speed,Accuracy,(UTC)TimeInMs");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("Navigation message format:");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("  Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("NMEA format (for [NMEA sentence] format see https://www.gpsinformation.org/dale/nmea.htm):");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("  NMEA,[NMEA sentence],(UTC)TimeInMs");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.newLine();
        } catch (IOException e) {
            logException(Application.get().getString(R.string.could_not_initialize_file, filePath), e);
            return;
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
                fileWriter.flush();
                fileWriter.close();
                fileWriter = null;
                isStarted = false;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }
    }

    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            synchronized (fileLock) {
                if (fileWriter == null) {
                    return;
                }
                String locationStream =
                        String.format(
                                Locale.US,
                                "Fix,%s,%f,%f,%f,%f,%f,%d",
                                location.getProvider(),
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAltitude(),
                                location.getSpeed(),
                                location.getAccuracy(),
                                location.getTime());
                try {
                    fileWriter.write(locationStream);
                    fileWriter.newLine();
                } catch (IOException e) {
                    logException(Application.get().getString(R.string.error_writing_file), e);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        synchronized (fileLock) {
            if (fileWriter == null) {
                return;
            }
            GnssClock gnssClock = event.getClock();
            for (GnssMeasurement measurement : event.getMeasurements()) {
                try {
                    writeGnssMeasurementToFile(gnssClock, measurement);
                } catch (IOException e) {
                    logException(Application.get().getString(R.string.error_writing_file), e);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onGnssNavigationMessageReceived(GnssNavigationMessage navigationMessage) {
        synchronized (fileLock) {
            if (fileWriter == null) {
                return;
            }
            StringBuilder builder = new StringBuilder("Nav");
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getSvid());
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getType());
            builder.append(RECORD_DELIMITER);

            int status = navigationMessage.getStatus();
            builder.append(status);
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getMessageId());
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getSubmessageId());
            byte[] data = navigationMessage.getData();
            for (byte word : data) {
                builder.append(RECORD_DELIMITER);
                builder.append(word);
            }
            try {
                fileWriter.write(builder.toString());
                fileWriter.newLine();
            } catch (IOException e) {
                logException(Application.get().getString(R.string.error_writing_file), e);
            }
        }
    }

    public void onNmeaReceived(long timestamp, String s) {
        synchronized (fileLock) {
            if (fileWriter == null) {
                return;
            }
            String nmeaStream = "NMEA," + s.trim() + "," + timestamp;
            try {
                fileWriter.write(nmeaStream);
                fileWriter.newLine();
            } catch (IOException e) {
                logException(Application.get().getString(R.string.error_writing_file), e);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurement measurement)
            throws IOException {
        String clockStream =
                String.format(
                        "Raw,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        SystemClock.elapsedRealtime(),
                        clock.getTimeNanos(),
                        clock.hasLeapSecond() ? clock.getLeapSecond() : "",
                        clock.hasTimeUncertaintyNanos() ? clock.getTimeUncertaintyNanos() : "",
                        clock.getFullBiasNanos(),
                        clock.hasBiasNanos() ? clock.getBiasNanos() : "",
                        clock.hasBiasUncertaintyNanos() ? clock.getBiasUncertaintyNanos() : "",
                        clock.hasDriftNanosPerSecond() ? clock.getDriftNanosPerSecond() : "",
                        clock.hasDriftUncertaintyNanosPerSecond()
                                ? clock.getDriftUncertaintyNanosPerSecond()
                                : "",
                        clock.getHardwareClockDiscontinuityCount() + ",");
        fileWriter.write(clockStream);

        String measurementStream =
                String.format(
                        "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        measurement.getSvid(),
                        measurement.getTimeOffsetNanos(),
                        measurement.getState(),
                        measurement.getReceivedSvTimeNanos(),
                        measurement.getReceivedSvTimeUncertaintyNanos(),
                        measurement.getCn0DbHz(),
                        measurement.getPseudorangeRateMetersPerSecond(),
                        measurement.getPseudorangeRateUncertaintyMetersPerSecond(),
                        measurement.getAccumulatedDeltaRangeState(),
                        measurement.getAccumulatedDeltaRangeMeters(),
                        measurement.getAccumulatedDeltaRangeUncertaintyMeters(),
                        measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "",
                        measurement.hasCarrierCycles() ? measurement.getCarrierCycles() : "",
                        measurement.hasCarrierPhase() ? measurement.getCarrierPhase() : "",
                        measurement.hasCarrierPhaseUncertainty()
                                ? measurement.getCarrierPhaseUncertainty()
                                : "",
                        measurement.getMultipathIndicator(),
                        measurement.hasSnrInDb() ? measurement.getSnrInDb() : "",
                        measurement.getConstellationType(),
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                                && measurement.hasAutomaticGainControlLevelDb()
                                ? measurement.getAutomaticGainControlLevelDb()
                                : "",
                        measurement.hasCarrierFrequencyHz() ? measurement.getCarrierFrequencyHz() : "");
        fileWriter.write(measurementStream);
        fileWriter.newLine();
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
