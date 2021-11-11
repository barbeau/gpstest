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
import android.location.GnssAntennaInfo;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.android.gpstest.Application;
import com.android.gpstest.BuildConfig;
import com.android.gpstest.R;
import com.android.gpstest.util.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A GNSS logger to store information to a CSV file. Originally from https://github.com/google/gps-measurement-tools/tree/master/GNSSLogger,
 * modified for GPSTest.
 */
public class CsvFileLogger extends BaseFileLogger implements FileLogger {

    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "Version: ";

    public CsvFileLogger(Context context) {
        super(context);
    }

    @Override
    String getFileExtension() {
        return "txt";
    }

    @Override
    boolean postFileInit(BufferedWriter fileWriter, boolean isNewFile) {
        ContextCompat.getMainExecutor(context).execute(() -> Toast.makeText(
                Application.Companion.getApp().getApplicationContext(),
                Application.Companion.getApp().getString(
                        R.string.logging_to_new_file,
                        file.getAbsolutePath()
                ),
                Toast.LENGTH_LONG
        ).show());
        return true;
    }

    /**
     * Initialize file by adding a CSV header
     *
     * @param writer   writer to use when writing file
     * @param filePath path to the current file
     */
    @Override
    void writeFileHeader(BufferedWriter writer, String filePath) {
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

            version.append("GNSS HW Year: " + IOUtils.getGnssHardwareYear() + ", ");

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
            writer.write("NMEA format (for [NMEA sentence] format see https://en.wikipedia.org/wiki/NMEA_0183):");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("  NMEA,[NMEA sentence],(UTC)TimeInMs");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("GnssAntennaInfo format (https://developer.android.com/reference/android/location/GnssAntennaInfo):");
            writer.newLine();
            writer.write(COMMENT_START);
            writer.write("  GnssAntennaInfo,CarrierFrequencyMHz,PhaseCenterOffsetXOffsetMm,PhaseCenterOffsetXOffsetUncertaintyMm,PhaseCenterOffsetYOffsetMm,PhaseCenterOffsetYOffsetUncertaintyMm,PhaseCenterOffsetZOffsetMm,PhaseCenterOffsetZOffsetUncertaintyMm,PhaseCenterVariationCorrectionsArray,PhaseCenterVariationCorrectionUncertaintiesArray,PhaseCenterVariationCorrectionsDeltaPhi,PhaseCenterVariationCorrectionsDeltaTheta,SignalGainCorrectionsArray,SignalGainCorrectionUncertaintiesArray,SignalGainCorrectionsDeltaPhi,SignalGainCorrectionsDeltaTheta");
            writer.newLine();
        } catch (IOException e) {
            logException(Application.Companion.getApp().getString(R.string.could_not_initialize_file, filePath), e);
            return;
        }
    }

    public synchronized void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
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
                logException(Application.Companion.getApp().getString(R.string.error_writing_file), e);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        if (fileWriter == null) {
            return;
        }
        GnssClock gnssClock = event.getClock();
        for (GnssMeasurement measurement : event.getMeasurements()) {
            try {
                writeGnssMeasurementToFile(gnssClock, measurement);
            } catch (IOException e) {
                logException(Application.Companion.getApp().getString(R.string.error_writing_file), e);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public synchronized void onGnssNavigationMessageReceived(GnssNavigationMessage navigationMessage) {
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
            logException(Application.Companion.getApp().getString(R.string.error_writing_file), e);
        }
    }

    public synchronized void onNmeaReceived(long timestamp, String s) {
        if (fileWriter == null) {
            return;
        }
        String nmeaStream = "NMEA," + s.trim() + "," + timestamp;
        try {
            fileWriter.write(nmeaStream);
            fileWriter.newLine();
        } catch (IOException e) {
            logException(Application.Companion.getApp().getString(R.string.error_writing_file), e);
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

    @RequiresApi(api = Build.VERSION_CODES.R)
    public synchronized void onGnssAntennaInfoReceived(@NonNull List<GnssAntennaInfo> list) {
        try {
            for (GnssAntennaInfo info : list) {
                fileWriter.write(IOUtils.serialize(info));
                fileWriter.newLine();
            }
            fileWriter.newLine();
        } catch (IOException e) {
            logException("Unable to write antenna info to CSV", e);
        }
    }
}
