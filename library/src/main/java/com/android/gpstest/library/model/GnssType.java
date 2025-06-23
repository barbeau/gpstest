/*
 * Copyright (C) 2013 Sean J. Barbeau
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
package com.android.gpstest.library.model;

import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Types of Global Navigation Satellite Systems
 */
public enum GnssType {
    NAVSTAR, GLONASS, GALILEO, QZSS, BEIDOU, IRNSS, SBAS, UNKNOWN;

    /**
     * RINEX representation as defined in the The Receiver Independent Exchange Format document
     * @return character representing the satellite constelation or null if unknown
     */
    @Nullable
    public Character toRinexChar() {
        return switch (this) {
            case NAVSTAR -> 'G';
            case GLONASS -> 'R';
            case GALILEO -> 'E';
            case QZSS -> 'J';
            case BEIDOU -> 'C';
            case IRNSS -> 'I';
            case SBAS -> 'S';
            default -> null;
        };
    }

    /**
     * Converts from the string representation of GnssType to the enum, or null if the input is
     * unknown
     * @param gnssType string representation of GnssType
     * @return the GnssType enum, or null if the input is unknown
     */
    public static GnssType fromString(String gnssType) {
        switch (gnssType) {
            case "NAVSTAR":
                return NAVSTAR;
            case "GLONASS":
                return GLONASS;
            case "GALILEO":
                return GALILEO;
            case "QZSS":
                return QZSS;
            case "BEIDOU":
                return BEIDOU;
            case "IRNSS":
                return IRNSS;
            case "SBAS":
                return SBAS;
            case "UNKNOWN":
                return UNKNOWN;
            default:
                return null;
        }
    }
}
