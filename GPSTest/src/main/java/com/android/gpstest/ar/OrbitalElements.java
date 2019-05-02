// Copyright 2008 Google Inc. From StarDroid.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.ar;

import android.util.Log;

import com.android.gpstest.util.MiscUtils;

/**
 * This class wraps the six parameters which define the path an object takes as
 * it orbits the sun.
 * <p>
 * The equations come from JPL's Solar System Dynamics site:
 * http://ssd.jpl.nasa.gov/?planet_pos
 * <p>
 * The original source for the calculations is based on the approximations described in:
 * Van Flandern T. C., Pulkkinen, K. F. (1979): "Low-Precision Formulae for
 * Planetary Positions", 1979, Astrophysical Journal Supplement Series, Vol. 41,
 * pp. 391-411.
 *
 * @author Kevin Serafini
 * @author Brent Bryan
 */

public class OrbitalElements {
    private static String TAG = MiscUtils.getTag(OrbitalElements.class);
    // calculation error
    private final static float EPSILON = 1.0e-6f;

    public final float distance;       // Mean distance (AU)
    public final float eccentricity;   // Eccentricity of orbit
    public final float inclination;    // Inclination of orbit (AngleUtils.RADIANS)
    public final float ascendingNode;  // Longitude of ascending node (AngleUtils.RADIANS)
    public final float perihelion;     // Longitude of perihelion (AngleUtils.RADIANS)
    public final float meanLongitude;  // Mean longitude (AngleUtils.RADIANS)

    public OrbitalElements(float d, float e, float i, float a, float p, float l) {
        this.distance = d;
        this.eccentricity = e;
        this.inclination = i;
        this.ascendingNode = a;
        this.perihelion = p;
        this.meanLongitude = l;
    }

    public float getAnomaly() {
        return calculateTrueAnomaly(meanLongitude - perihelion, eccentricity);
    }

    // compute the true anomaly from mean anomaly using iteration
    // m - mean anomaly in radians
    // e - orbit eccentricity
    // Return value is in radians.
    private static float calculateTrueAnomaly(float m, float e) {
        // initial approximation of eccentric anomaly
        float e0 = m + e * (float) Math.sin(m) * (1.0f + e * (float) Math.cos(m));
        float e1;

        // iterate to improve accuracy
        int counter = 0;
        do {
            e1 = e0;
            e0 = e1 - (e1 - e * (float) Math.sin(e1) - m) / (1.0f - e * (float) Math.cos(e1));
            if (counter++ > 100) {
                Log.d(TAG, "Failed to converge! Exiting.");
                Log.d(TAG, "e1 = " + e1 + ", e0 = " + e0);
                Log.d(TAG, "diff = " + (float) Math.abs(e0 - e1));
                break;
            }
        } while ((float) Math.abs(e0 - e1) > EPSILON);

        // convert eccentric anomaly to true anomaly
        float v =
                2f * (float) Math.atan((float) Math.sqrt((1 + e) / (1 - e))
                        * (float) Math.tan(0.5f * e0));
        return Geometry.mod2pi(v);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Mean Distance: " + distance + " (AU)\n");
        sb.append("Eccentricity: " + eccentricity + "\n");
        sb.append("Inclination: " + inclination + " (AngleUtils.RADIANS)\n");
        sb.append("Ascending Node: " + ascendingNode + " (AngleUtils.RADIANS)\n");
        sb.append("Perihelion: " + perihelion + " (AngleUtils.RADIANS)\n");
        sb.append("Mean Longitude: " + meanLongitude + " (AngleUtils.RADIANS)\n");

        return sb.toString();
    }
}
