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

import android.content.res.Resources;
import android.util.Log;

import com.android.gpstest.ar.util.AbstractUpdateClosure;
import com.android.gpstest.ar.util.UpdateClosure;
import com.android.gpstest.util.MiscUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Layer for objects which are {@link AstronomicalSource}s.
 *
 * @author Brent Bryan
 */
// TODO(brent): merge with AbstractLayer?
public abstract class AbstractSourceLayer extends AbstractLayer {
    private static final String TAG = MiscUtils.getTag(AbstractSourceLayer.class);

    private final ArrayList<TextSource> textSources = new ArrayList<TextSource>();
    private final ArrayList<ImageSource> imageSources = new ArrayList<ImageSource>();
    private final ArrayList<PointSource> pointSources = new ArrayList<PointSource>();
    private final ArrayList<LineSource> lineSources = new ArrayList<LineSource>();
    private final ArrayList<AstronomicalSource> astroSources = new ArrayList<AstronomicalSource>();

    private HashMap<String, SearchResult> searchIndex = new HashMap<String, SearchResult>();
    private PrefixStore prefixStore = new PrefixStore();
    private final boolean shouldUpdate;
    private SourceUpdateClosure closure;

    public AbstractSourceLayer(Resources resources, boolean shouldUpdate) {
        super(resources);
        this.shouldUpdate = shouldUpdate;
    }

    @Override
    public synchronized void initialize() {
        astroSources.clear();

        initializeAstroSources(astroSources);

        for (AstronomicalSource astroSource : astroSources) {
            Sources sources = astroSource.initialize();

            textSources.addAll(sources.getLabels());
            imageSources.addAll(sources.getImages());
            pointSources.addAll(sources.getPoints());
            lineSources.addAll(sources.getLines());

            List<String> names = astroSource.getNames();
            if (!names.isEmpty()) {
                GeocentricCoordinates searchLoc = astroSource.getSearchLocation();
                for (String name : names) {
                    searchIndex.put(name.toLowerCase(), new SearchResult(name, searchLoc));
                    prefixStore.add(name.toLowerCase());
                }
            }
        }

        // update the renderer
        updateLayerForControllerChange();
    }

    @Override
    protected void updateLayerForControllerChange() {
        refreshSources(EnumSet.of(RendererObjectManager.UpdateType.Reset));
        if (shouldUpdate) {
            if (closure == null) {
                closure = new SourceUpdateClosure(this);
            }
            addUpdateClosure(closure);
        }
    }

    /**
     * Subclasses should override this method and add all their
     * {@link AstronomicalSource} to the given {@link ArrayList}.
     */
    protected abstract void initializeAstroSources(ArrayList<AstronomicalSource> sources);

    /**
     * Redraws the sources on this layer, after first refreshing them based on
     * the current state of the
     * {@link AstronomerModel}.
     */
    protected void refreshSources() {
        refreshSources(EnumSet.noneOf(RendererObjectManager.UpdateType.class));
    }

    /**
     * Redraws the sources on this layer, after first refreshing them based on
     * the current state of the
     * {@link AstronomerModel}.
     */
    protected synchronized void refreshSources(EnumSet<RendererObjectManager.UpdateType> updateTypes) {
        for (AstronomicalSource astroSource : astroSources) {
            updateTypes.addAll(astroSource.update());
        }

        if (!updateTypes.isEmpty()) {
            redraw(updateTypes);
        }
    }

    /**
     * Forcefully resets and redraws all sources on this layer everything on
     * this layer.
     */
    @Override
    protected void redraw() {
        refreshSources(EnumSet.of(RendererObjectManager.UpdateType.Reset));
    }

    private final void redraw(EnumSet<RendererObjectManager.UpdateType> updateTypes) {
        super.redraw(textSources, pointSources, lineSources, imageSources, updateTypes);
    }

    @Override
    public List<SearchResult> searchByObjectName(String name) {
        Log.d(TAG, "Search planets layer for " + name);
        List<SearchResult> matches = new ArrayList<SearchResult>();
        SearchResult searchResult = searchIndex.get(name.toLowerCase());
        if (searchResult != null) {
            matches.add(searchResult);
        }
        Log.d(TAG, getLayerName() + " provided " + matches.size() + "results for " + name);
        return matches;
    }

    @Override
    public Set<String> getObjectNamesMatchingPrefix(String prefix) {
        Log.d(TAG, "Searching planets layer for prefix " + prefix);
        Set<String> results = prefixStore.queryByPrefix(prefix);
        Log.d(TAG, "Got " + results.size() + " results for prefix " + prefix + " in " + getLayerName());
        return results;
    }

    /**
     * Implementation of the {@link UpdateClosure} interface used to update a layer
     */
    public static class SourceUpdateClosure extends AbstractUpdateClosure {
        private final AbstractSourceLayer layer;

        public SourceUpdateClosure(AbstractSourceLayer layer) {
            this.layer = layer;
        }

        @Override
        public void run() {
            layer.refreshSources();
        }
    }
}
