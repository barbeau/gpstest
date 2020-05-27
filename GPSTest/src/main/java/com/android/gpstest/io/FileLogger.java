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

import android.app.Activity;
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
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A GNSS logger to store information to a file. Originally from https://github.com/google/gps-measurement-tools/tree/master/GNSSLogger,
 * modified for GPSTest.
 */
public class FileLogger {

    private static final String TAG = "FileLogger";
    private static final String FILE_PREFIX = "gnss_log";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "GPSTest version: ";

    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    private final Context mContext;

    private final Object mFileLock = new Object();
    private BufferedWriter mFileWriter;
    private File mFile;
    private boolean mIsStarted = false;

    public FileLogger(Context context) {
        mContext = context;
    }

    public File getFile() {
        return mFile;
    }

    /**
     * Start a file logging process
     *
     * @param existingFile The existing file if file logging is to be continued, or null if a
     *                     new file should be created.
     */
    public void startLog(File existingFile) {
        synchronized (mFileLock) {
            File baseDirectory;
            String state = Environment.getExternalStorageState();
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
                if (mFileWriter != null) {
                    try {
                        mFileWriter.close();
                    } catch (IOException e) {
                        logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                        return;
                    }
                }
                mFile = existingFile;
                mFileWriter = writer;
            } else {
                // Create new logging file
                SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
                String fileName = String.format("%s_%s.txt", FILE_PREFIX, formatter.format(new Date()));
                File currentFile = new File(baseDirectory, fileName);
                currentFilePath = currentFile.getAbsolutePath();
                BufferedWriter writer;
                try {
                    writer = new BufferedWriter(new FileWriter(currentFile));
                } catch (IOException e) {
                    logException("Could not open file: " + currentFilePath, e);
                    return;
                }

                writeFileHeader(writer, currentFilePath);

                if (mFileWriter != null) {
                    try {
                        mFileWriter.close();
                    } catch (IOException e) {
                        logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                        return;
                    }
                }

                mFile = currentFile;
                mFileWriter = writer;

                Toast.makeText(mContext, Application.get().getString(R.string.logging_to_new_file, currentFilePath), Toast.LENGTH_LONG).show();
                deleteOldFiles(baseDirectory, mFile);
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

            if (existingFile == null) {
                // Only if file didn't previously exist
                writeFileHeader(writer, currentFilePath);
            }

            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                } catch (IOException e) {
                    logException(Application.get().getString(R.string.unable_to_close_all_file_streams), e);
                    return;
                }
            }

            mFileWriter = writer;
            mFile = file;

            if (existingFile == null) {
                // Only if file didn't previously exist
                Toast.makeText(mContext, Application.get().getString(R.string.logging_to_new_file, currentFilePath), Toast.LENGTH_LONG).show();
                deleteOldFiles(baseDirectory, mFile);
            }

            mIsStarted = true;
        }
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
                PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
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
     * Deletes old files in the given baseDirectory, except the fileNotToDelete
     *
     * @param baseDirectory   base directory in which to delete files
     * @param fileNotToDelete file not to delete
     */
    private void deleteOldFiles(File baseDirectory, File fileNotToDelete) {
        // To make sure that files do not fill up the external storage:
        // - Remove all empty files
        FileFilter filter = new FileToDeleteFilter(fileNotToDelete);
        for (File pastFile : baseDirectory.listFiles(filter)) {
            pastFile.delete();
        }
        // - Trim the number of files with data
        File[] pastFiles = baseDirectory.listFiles();
        int filesToDeleteCount = pastFiles.length - MAX_FILES_STORED;
        if (filesToDeleteCount > 0) {
            Arrays.sort(pastFiles);
            for (int i = 0; i < filesToDeleteCount; ++i) {
                pastFiles[i].delete();
            }
        }
    }

    /**
     * Returns true if the logger is already started, or false if it is not
     *
     * @return
     */
    public synchronized boolean isStarted() {
        return mIsStarted;
    }

    /**
     * Send the current log via email or other options selected from the chooser shown to the user. The
     * current log is closed when calling this method.
     * @param activity Activity used to open the chooser to send the file
     */
    public void send(Activity activity) {
        if (mFile == null) {
            return;
        }
        android.net.Uri uri = IOUtils.getUriFromFile(mContext, mFile);
        IOUtils.sendLogFile(activity, uri);
        close();
    }

    public void close() {
        if (mFileWriter != null) {
            try {
                mFileWriter.flush();
                mFileWriter.close();
                mFileWriter = null;
                mIsStarted = false;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }
    }

    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            synchronized (mFileLock) {
                if (mFileWriter == null) {
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
                    mFileWriter.write(locationStream);
                    mFileWriter.newLine();
                } catch (IOException e) {
                    logException(Application.get().getString(R.string.error_writing_file), e);
                }
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
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
        synchronized (mFileLock) {
            if (mFileWriter == null) {
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
                mFileWriter.write(builder.toString());
                mFileWriter.newLine();
            } catch (IOException e) {
                logException(Application.get().getString(R.string.error_writing_file), e);
            }
        }
    }

    public void onNmeaReceived(long timestamp, String s) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }
            String nmeaStream = "NMEA," + s.trim() + "," + timestamp;
            try {
                mFileWriter.write(nmeaStream);
                mFileWriter.newLine();
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
        mFileWriter.write(clockStream);

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
        mFileWriter.write(measurementStream);
        mFileWriter.newLine();
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Implements a {@link FileFilter} to delete files that are not in the
     * {@link FileToDeleteFilter#mRetainedFiles}.
     */
    private static class FileToDeleteFilter implements FileFilter {
        private final List<File> mRetainedFiles;

        public FileToDeleteFilter(File... retainedFiles) {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }

        /**
         * Returns {@code true} to delete the file, and {@code false} to keep the file.
         *
         * <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname == null || !pathname.exists()) {
                return false;
            }
            if (mRetainedFiles.contains(pathname)) {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    }
}
