// Copyright 2009 Google Inc. From StarDroid.
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

/**
 * This interface corresponds to an object which can be represented by a single
 * point in space, such as a star.
 *
 * @author Brent Bryan
 */
public interface PointSource extends Colorable, PositionSource {
    public enum Shape {
        CIRCLE(0),
        STAR(1),
        ELLIPTICAL_GALAXY(2),
        SPIRAL_GALAXY(3),
        IRREGULAR_GALAXY(4),
        LENTICULAR_GALAXY(3),
        GLOBULAR_CLUSTER(5),
        OPEN_CLUSTER(6),
        NEBULA(7),
        HUBBLE_DEEP_FIELD(8);

        private final int imageIndex;

        private Shape(int imageIndex) {
            this.imageIndex = imageIndex;
        }

        public int getImageIndex() {
            // return imageIndex;
            return 0;
        }
    }

    /**
     * Returns the size of the dot which should be drawn to represent this point
     * in the renderer.
     */
    public int getSize();

    /**
     * Returns the Shape of the image used to render the point in the texture file.
     */
    public Shape getPointShape();
}
