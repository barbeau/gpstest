// Copyright 2008 Google Inc. From StarDroid.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.ar;

public class HeliocentricCoordinates extends Vector3 {
    public float radius;  // Radius. (AU)

    // Value of the obliquity of the ecliptic for J2000
    private static final float OBLIQUITY = 23.439281f * Geometry.DEGREES_TO_RADIANS;

    public HeliocentricCoordinates(float radius, float xh, float yh, float zh) {
        super(xh, yh, zh);
        this.radius = radius;
    }

    /**
     * Subtracts the values of the given heliocentric coordinates from this
     * object.
     */
    public void Subtract(HeliocentricCoordinates other) {
        this.x -= other.x;
        this.y -= other.y;
        this.z -= other.z;
    }

    public HeliocentricCoordinates CalculateEquatorialCoordinates() {
        return new HeliocentricCoordinates(this.radius,
                this.x,
                this.y * (float) Math.cos(OBLIQUITY) - this.z * (float) Math.sin(OBLIQUITY),
                this.y * (float) Math.sin(OBLIQUITY) + this.z * (float) Math.cos(OBLIQUITY));
    }

    public float DistanceFrom(HeliocentricCoordinates other) {
        float dx = this.x - other.x;
        float dy = this.y - other.y;
        float dz = this.z - other.z;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static HeliocentricCoordinates getInstance(OrbitalElements elem) {
        float anomaly = elem.getAnomaly();
        float ecc = elem.eccentricity;
        float radius = elem.distance * (1 - ecc * ecc) / (1 + ecc * (float) Math.cos(anomaly));

        // heliocentric rectangular coordinates of planet
        float per = elem.perihelion;
        float asc = elem.ascendingNode;
        float inc = elem.inclination;
        float xh = radius *
                ((float) Math.cos(asc) * (float) Math.cos(anomaly + per - asc) -
                        (float) Math.sin(asc) * (float) Math.sin(anomaly + per - asc) *
                                (float) Math.cos(inc));
        float yh = radius *
                ((float) Math.sin(asc) * (float) Math.cos(anomaly + per - asc) +
                        (float) Math.cos(asc) * (float) Math.sin(anomaly + per - asc) *
                                (float) Math.cos(inc));
        float zh = radius * ((float) Math.sin(anomaly + per - asc) * (float) Math.sin(inc));

        return new HeliocentricCoordinates(radius, xh, yh, zh);
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f, %f)", x, y, z, radius);
    }
}
