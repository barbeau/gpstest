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

import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.TimeUtils;

import java.util.Date;

/**
 * Utilities for working with angles, distances, matrices, and time.
 *
 * @author Kevin Serafini
 * @author Brent Bryan
 * @author Dominic Widdows
 * @author John Taylor
 */

public class Geometry {
    // Convert Degrees to Radians
    public static final float DEGREES_TO_RADIANS = MathUtils.PI / 180.0f;

    // Convert Radians to Degrees
    public static final float RADIANS_TO_DEGREES = 180.0f / MathUtils.PI;

    private Geometry() {
    }

    /**
     * Return the integer part of a number
     */
    public static float abs_floor(float x) {
        float result;
        if (x >= 0.0)
            result = (float) Math.floor(x);
        else
            result = (float) Math.ceil(x);
        return result;
    }

    /**
     * Returns the modulo the given value by 2\pi. Returns an angle in the range 0
     * to 2\pi radians.
     */
    public static float mod2pi(float x) {
        float factor = x / MathUtils.TWO_PI;
        float result = MathUtils.TWO_PI * (factor - abs_floor(factor));
        if (result < 0.0) {
            result = MathUtils.TWO_PI + result;
        }
        return result;
    }

    public static float scalarProduct(Vector3 v1, Vector3 v2) {
        return v1.x * v2.x + v1.y * v2.y + v1.z * v2.z;
    }

    public static Vector3 vectorProduct(Vector3 v1, Vector3 v2) {
        return new Vector3(v1.y * v2.z - v1.z * v2.y,
                -v1.x * v2.z + v1.z * v2.x,
                v1.x * v2.y - v1.y * v2.x);
    }

    /**
     * Scales the vector by the given amount and returns a new vector.
     */
    public static Vector3 scaleVector(Vector3 v, float scale) {
        return new Vector3(scale * v.x, scale * v.y, scale * v.z);
    }

    /**
     * Creates and returns a new Vector3 which is the sum of both arguments.
     *
     * @param first
     * @param second
     * @return vector sum first + second
     */
    public static Vector3 addVectors(Vector3 first, Vector3 second) {
        return new Vector3(first.x + second.x, first.y + second.y, first.z + second.z);
    }

    public static float cosineSimilarity(Vector3 v1, Vector3 v2) {
        // We might want to optimize this implementation at some point.
        return scalarProduct(v1, v2)
                / (float) Math.sqrt(scalarProduct(v1, v1)
                * scalarProduct(v2, v2));
    }

    /**
     * Convert ra and dec to x,y,z where the point is place on the unit sphere.
     */
    public static GeocentricCoordinates getXYZ(RaDec raDec) {
        float raRadians = raDec.ra * DEGREES_TO_RADIANS;
        float decRadians = raDec.dec * DEGREES_TO_RADIANS;
        GeocentricCoordinates result = new GeocentricCoordinates(
                (float) Math.cos(raRadians) * (float) Math.cos(decRadians),
                (float) Math.sin(raRadians) * (float) Math.cos(decRadians),
                (float) Math.sin(decRadians));
        return result;
    }

    /**
     * Compute celestial coordinates of zenith from utc, lat long.
     */
    public static RaDec calculateRADecOfZenith(Date utc, LatLong location) {
        // compute overhead RA in degrees
        float my_ra = TimeUtils.meanSiderealTime(utc, location.getLongitude());
        float my_dec = location.getLatitude();
        return new RaDec(my_ra, my_dec);
    }

    /**
     * Multiply two 3X3 matrices m1 * m2.
     */
    public static Matrix33 matrixMultiply(Matrix33 m1, Matrix33 m2) {
        return new Matrix33(m1.xx * m2.xx + m1.xy * m2.yx + m1.xz * m2.zx,
                m1.xx * m2.xy + m1.xy * m2.yy + m1.xz * m2.zy,
                m1.xx * m2.xz + m1.xy * m2.yz + m1.xz * m2.zz,
                m1.yx * m2.xx + m1.yy * m2.yx + m1.yz * m2.zx,
                m1.yx * m2.xy + m1.yy * m2.yy + m1.yz * m2.zy,
                m1.yx * m2.xz + m1.yy * m2.yz + m1.yz * m2.zz,
                m1.zx * m2.xx + m1.zy * m2.yx + m1.zz * m2.zx,
                m1.zx * m2.xy + m1.zy * m2.yy + m1.zz * m2.zy,
                m1.zx * m2.xz + m1.zy * m2.yz + m1.zz * m2.zz);
    }

    /**
     * Calculate w = m * v where m is a 3X3 matrix and v a column vector.
     */
    public static Vector3 matrixVectorMultiply(Matrix33 m, Vector3 v) {
        return new Vector3(m.xx * v.x + m.xy * v.y + m.xz * v.z,
                m.yx * v.x + m.yy * v.y + m.yz * v.z,
                m.zx * v.x + m.zy * v.y + m.zz * v.z);
    }

    /**
     * Calculate the rotation matrix for a certain number of degrees about the
     * give axis.
     *
     * @param degrees
     * @param axis    - must be a unit vector.
     */
    public static Matrix33 calculateRotationMatrix(float degrees, Vector3 axis) {
        // Construct the rotation matrix about this vector
        float cosD = (float) Math.cos(degrees * Geometry.DEGREES_TO_RADIANS);
        float sinD = (float) Math.sin(degrees * Geometry.DEGREES_TO_RADIANS);
        float oneMinusCosD = 1f - cosD;

        float x = axis.x;
        float y = axis.y;
        float z = axis.z;

        float xs = x * sinD;
        float ys = y * sinD;
        float zs = z * sinD;

        float xm = x * oneMinusCosD;
        float ym = y * oneMinusCosD;
        float zm = z * oneMinusCosD;

        float xym = x * ym;
        float yzm = y * zm;
        float zxm = z * xm;

        Matrix33 rotationMatrix = new Matrix33(x * xm + cosD, xym + zs, zxm - ys,
                xym - zs, y * ym + cosD, yzm + xs,
                zxm + ys, yzm - xs, z * zm + cosD);
        return rotationMatrix;
    }
}
