// Copyright 2010 Google Inc. From StarDroid.
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

import java.util.List;

/**
 * Data object which contains all of the elements for an
 * {@link AstronomicalSource}. These elements describe the lines, text, images,
 * etc sent to renderer to be drawn.
 *
 * @author Brent Bryan
 */
public interface Sources {

    /**
     * Returns the list of points that should be drawn in the renderer.
     */
    List<? extends PointSource> getPoints();

    /**
     * Returns the list of text labels that should be drawn in the renderer.
     */
    List<? extends TextSource> getLabels();

    /**
     * Returns the list of lines that should be drawn in the renderer.
     */
    List<? extends LineSource> getLines();

    /**
     * Returns the list of images that should be drawn in the renderer.
     */
    List<? extends ImageSource> getImages();
}
