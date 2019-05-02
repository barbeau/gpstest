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

import java.util.List;
import java.util.Set;


/**
 * A logical collection of objects which should be displayed in SkyMap. For
 * instance, the set of objects which should be turned off / on simultaneously.
 *
 * @author Brent Bryan
 */
public interface Layer {
    /**
     * Initializes the layer; reading data and computing locations as necessary.
     * This method should return quickly - use a background thread if necessary.
     * This method is typically called before the {@link #registerWithRenderer}
     * method, but may not be.
     */
    void initialize();

    /**
     * Registers this layer with the given {@link RendererController}.  None of
     * the objects in this layer can be displayed until this method is called.
     */
    void registerWithRenderer(RendererController controller);

    /**
     * Returns the z-ordering of the layers.  Lower numbers are rendered first and
     * are therefore 'behind' higher numbered layers.
     */
    int getLayerDepthOrder();

    /**
     * Returns the preference label associated with this layer.
     */
    String getPreferenceId();

    /**
     * Returns the name associated with this layer.
     */
    String getLayerName();

    /**
     * Sets whether the {@link AstronomicalSource}s in this layer should be shown
     * by the renderer.
     */
    void setVisible(boolean visible);

    /**
     * Search the layer for an object with the given name.  The search is
     * case-insensitive.
     *
     * @param name the name to search for
     * @return a list of all matching objects.
     */
    List<SearchResult> searchByObjectName(String name);

    /**
     * Given a string prefix, find all possible queries for which we have a
     * search result.  The search is case-insensitive.
     *
     * @param prefix the prefix to search for.
     * @return a set of matching queries.
     */
    Set<String> getObjectNamesMatchingPrefix(String prefix);
}
