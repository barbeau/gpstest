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

/**
 * Class for representing a 3x3 matrix explicitly, avoiding heap
 * allocation as far as possible.
 *
 * @author Dominic Widdows
 */
public class Matrix33 implements Cloneable {

    public float xx;
    public float xy;
    public float xz;
    public float yx;
    public float yy;
    public float yz;
    public float zx;
    public float zy;
    public float zz;

    /**
     * Construct a new matrix.
     *
     * @param xx row 1, col 1
     * @param xy row 1, col 2
     * @param xz row 1, col 3
     * @param yx row 2, col 1
     * @param yy row 2, col 2
     * @param yz row 2, col 3
     * @param zx row 3, col 1
     * @param zy row 3, col 2
     * @param zz row 3, col 3
     */
    public Matrix33(float xx, float xy, float xz,
                    float yx, float yy, float yz,
                    float zx, float zy, float zz) {
        this.xx = xx;
        this.xy = xy;
        this.xz = xz;
        this.yx = yx;
        this.yy = yy;
        this.yz = yz;
        this.zx = zx;
        this.zy = zy;
        this.zz = zz;
    }

    /**
     * Construct a matrix from three column vectors.
     */
    public Matrix33(Vector3 v1, Vector3 v2, Vector3 v3) {
        this(v1, v2, v3, true);
    }

    /**
     * Construct a matrix from three vectors.
     *
     * @param columnVectors true if the vectors are column vectors, otherwise
     *                      they're row vectors.
     */
    public Matrix33(Vector3 v1, Vector3 v2, Vector3 v3, boolean columnVectors) {
        if (columnVectors) {
            this.xx = v1.x;
            this.yx = v1.y;
            this.zx = v1.z;
            this.xy = v2.x;
            this.yy = v2.y;
            this.zy = v2.z;
            this.xz = v3.x;
            this.yz = v3.y;
            this.zz = v3.z;
        } else {
            this.xx = v1.x;
            this.xy = v1.y;
            this.xz = v1.z;
            this.yx = v2.x;
            this.yy = v2.y;
            this.yz = v2.z;
            this.zx = v3.x;
            this.zy = v3.y;
            this.zz = v3.z;
        }
    }

    // TODO(widdows): rename this to something like copyOf().
    @Override
    public Matrix33 clone() {
        return new Matrix33(xx, xy, xz,
                yx, yy, yz,
                zx, zy, zz);
    }

    public static Matrix33 getIdMatrix() {
        return new Matrix33(1, 0, 0, 0, 1, 0, 0, 0, 1);
    }

    /**
     * Create a zero matrix.
     */
    public Matrix33() {
        new Matrix33(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public float getDeterminant() {
        return xx * yy * zz + xy * yz * zx + xz * yx * zy - xx * yz * zy - yy * zx * xz - zz * xy * yx;
    }

    public Matrix33 getInverse() {
        float det = getDeterminant();
        if (det == 0.0) return null;
        return new Matrix33(
                (yy * zz - yz * zy) / det, (xz * zy - xy * zz) / det, (xy * yz - xz * yy) / det,
                (yz * zx - yx * zz) / det, (xx * zz - xz * zx) / det, (xz * yx - xx * yz) / det,
                (yx * zy - yy * zx) / det, (xy * zx - xx * zy) / det, (xx * yy - xy * yx) / det);
    }

    /**
     * Transpose the matrix, in place.
     */
    public void transpose() {
        float tmp;
        tmp = xy;
        xy = yx;
        yx = tmp;

        tmp = xz;
        xz = zx;
        zx = tmp;

        tmp = yz;
        yz = zy;
        zy = tmp;
    }
}
