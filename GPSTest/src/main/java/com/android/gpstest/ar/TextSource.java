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
 * This interface corresponds to a text label placed at some fixed location in
 * space.
 *
 * @author Brent Bryan
 */
public interface TextSource extends Colorable, PositionSource {

    /**
     * Returns the text to be displayed at the specified location in the renderer.
     */
    public String getText();

    /**
     * Changes the text in this {@link TextSource}.
     */
    public void setText(String newText);

    /**
     * Returns the size of the font in points (e.g. 10, 12).
     */
    public int getFontSize();

    public float getOffset();
    // TODO(brent): talk to James: can we add font, style info?
    // TODO(brent): can we specify label orientation?
}
