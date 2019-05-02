// Copyright 2009 Google Inc. From StarDroid.
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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

import com.android.gpstest.util.MiscUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Allows a group of layers to be controlled together.
 */
public class LayerManager implements OnSharedPreferenceChangeListener {
    private static final String TAG = MiscUtils.getTag(LayerManager.class);
    private final List<Layer> layers = new ArrayList<>();
    private final SharedPreferences sharedPreferences;

    public LayerManager(SharedPreferences sharedPreferences) {
        Log.d(TAG, "Creating LayerManager");
        this.sharedPreferences = sharedPreferences;
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void addLayer(Layer layer) {
        this.layers.add(layer);
    }

    public void initialize() {
        for (Layer layer : layers) {
            layer.initialize();
        }
    }

    public void registerWithRenderer(RendererController renderer) {
        for (Layer layer : layers) {
            layer.registerWithRenderer(renderer);
            String prefId = layer.getPreferenceId();
            boolean visible = sharedPreferences.getBoolean(prefId, true);
            layer.setVisible(visible);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        for (Layer layer : layers) {
            if (layer.getPreferenceId().equals(key)) {
                boolean visible = prefs.getBoolean(key, true);
                layer.setVisible(visible);
            }
        }
    }

    /**
     * Returns the name of this object.
     */
    public String getName() {
        return "Layer Manager";
    }

    /**
     * Search all visible layers for an object with the given name.
     *
     * @param name the name to search for
     * @return a list of all matching objects.
     */
    public List<SearchResult> searchByObjectName(String name) {
        List<SearchResult> all = new ArrayList<>();
        for (Layer layer : layers) {
            if (isLayerVisible(layer)) {
                all.addAll(layer.searchByObjectName(name));
            }
        }
        Log.d(TAG, "Got " + all.size() + " results in total for " + name);
        return all;
    }

    /**
     * Given a string prefix, find all possible queries for which we have a
     * result in the visible layers.
     *
     * @param prefix the prefix to search for.
     * @return a set of matching queries.
     */
    public Set<SearchTermsProvider.SearchTerm> getObjectNamesMatchingPrefix(String prefix) {
        Set<SearchTermsProvider.SearchTerm> all = new HashSet<>();
        for (Layer layer : layers) {
            if (isLayerVisible(layer)) {
                for (String query : layer.getObjectNamesMatchingPrefix(prefix)) {
                    SearchTermsProvider.SearchTerm result = new SearchTermsProvider.SearchTerm(query, layer.getLayerName());
                    all.add(result);
                }
            }
        }
        Log.d(TAG, "Got " + all.size() + " results in total for " + prefix);
        return all;
    }

    private boolean isLayerVisible(Layer layer) {
        return sharedPreferences.getBoolean(layer.getPreferenceId(), true);
    }
}
