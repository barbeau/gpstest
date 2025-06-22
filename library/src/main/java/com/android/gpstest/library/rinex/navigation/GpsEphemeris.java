/*
 * Copyright (C) 2017 The Android Open Source Project
 * With modifications
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
package com.android.gpstest.library.rinex.navigation;

public class GpsEphemeris {
    // Time used for generating this data (typically, the queried time).
    public long timeNs;
    // PRN.
    public int prnNumber;
    // GPS week number.
    public int week;
    // Code on L2.
    public int l2Code;
    // L2 P data flag.
    public int l2Flag;
    // SV accuracy in meters.
    public double svAccuracyM;
    // SV health bits.
    public int svHealth;
    // Issue of data (ephemeris).
    public int iode;
    // Issue of data (clock).
    public int iodc;
    // Time of clock (second).
    public double toc;
    // Time of ephemeris (second).
    public double toe;
    // Transmission time of the message.
    public double tom;
    // Clock info (drift, bias, etc).
    public double svClockBias;
    public double svClockDrift;
    public double svClockDriftRate;
    public double tgd;
    // Orbital parameters.
    // Square root of semi-major axis
    public double rootOfA;
    // Eccentricity.
    public double e;
    // Inclination angle (radian).
    public double i0;
    // Rate of inclination angle (radians/sec).
    public double iDot;
    // Argument of perigee.
    public double omega;
    // Longitude of ascending node of orbit plane at the beginning of week.
    public double omega0;
    // Rate of right ascension.
    public double omegaDot;
    // Mean anomaly at reference time.
    public double m0;
    // Mean motion difference from computed value.
    public double deltaN;
    // Amplitude of second-order harmonic perturbations.
    public double crc;
    public double crs;
    public double cuc;
    public double cus;
    public double cic;
    public double cis;
    // FIT interval.
    public double fitInterval;
}
