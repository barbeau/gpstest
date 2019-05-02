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

import android.graphics.Bitmap;

/**
 * This source corresponds to an image to be drawn at a specific point on the
 * sky by the renderer.
 *
 * @author Brent Bryan
 */
public interface ImageSource extends PositionSource {

    /**
     * Returns the image to be displayed at the specified point.
     */
    public Bitmap getImage();

    // TODO(brent): talk to James to determine what's really needed here.

    public float[] getVerticalCorner();

    public float[] getHorizontalCorner();

    public boolean requiresBlending();
}
