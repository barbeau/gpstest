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

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * For representing constellations, constellation boundaries etc.
 */
public class LineSourceImpl extends AbstractSource implements LineSource {

    public final List<GeocentricCoordinates> vertices;
    public final List<RaDec> raDecs;
    public final float lineWidth;

    public LineSourceImpl() {
        this(Color.WHITE, new ArrayList<GeocentricCoordinates>(), 1.5f);
    }

    public LineSourceImpl(int color) {
        this(color, new ArrayList<GeocentricCoordinates>(), 1.5f);
    }

    public LineSourceImpl(int color, List<GeocentricCoordinates> vertices, float lineWidth) {
        super(color);

        this.vertices = vertices;
        this.raDecs = new ArrayList<RaDec>();
        this.lineWidth = lineWidth;
    }

    public float getLineWidth() {
        return lineWidth;
    }

    public List<GeocentricCoordinates> getVertices() {
        List<GeocentricCoordinates> result;
        if (vertices != null) {
            result = vertices;
        } else {
            result = new ArrayList<GeocentricCoordinates>();
        }
        return Collections.unmodifiableList(result);
    }
}
