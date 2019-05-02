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

package com.android.gpstest.util;

import com.android.gpstest.ar.Vector3;

public class VectorUtil {
    public static Vector3 zero() {
        return new Vector3(0, 0, 0);
    }

    public static float dotProduct(Vector3 p1, Vector3 p2) {
        return p1.x * p2.x + p1.y * p2.y + p1.z * p2.z;
    }

    public static Vector3 crossProduct(Vector3 p1, Vector3 p2) {
        return new Vector3(p1.y * p2.z - p1.z * p2.y,
                -p1.x * p2.z + p1.z * p2.x,
                p1.x * p2.y - p1.y * p2.x);
    }

    public static float angleBetween(Vector3 p1, Vector3 p2) {
        return (float) Math.acos(dotProduct(p1, p2) / (length(p1) * length(p2)));
    }

    public static float length(Vector3 v) {
        return (float) Math.sqrt(lengthSqr(v));
    }

    public static float lengthSqr(Vector3 v) {
        return dotProduct(v, v);
    }

    public static Vector3 normalized(Vector3 v) {
        float len = length(v);
        if (len < 0.000001f) {
            return zero();
        }
        return scale(v, 1.0f / len);
    }

    public static Vector3 project(Vector3 v, Vector3 onto) {
        return scale(dotProduct(v, onto) / length(onto), onto);
    }

    public static Vector3 projectOntoUnit(Vector3 v, Vector3 onto) {
        return scale(dotProduct(v, onto), onto);
    }

    public static Vector3 projectOntoPlane(Vector3 v, Vector3 unitNormal) {
        return difference(v, projectOntoUnit(v, unitNormal));
    }

    public static Vector3 negate(Vector3 v) {
        return new Vector3(-v.x, -v.y, -v.z);
    }

    public static Vector3 sum(Vector3 v1, Vector3 v2) {
        return new Vector3(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z);
    }

    public static Vector3 difference(Vector3 v1, Vector3 v2) {
        return sum(v1, negate(v2));
    }

    public static Vector3 scale(float factor, Vector3 v) {
        return new Vector3(v.x * factor, v.y * factor, v.z * factor);
    }

    public static Vector3 scale(Vector3 v, float factor) {
        return scale(factor, v);
    }
}
