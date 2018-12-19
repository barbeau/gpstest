/*
 * Copyright (C) 2018 Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest;

import android.location.Location;
import android.os.Bundle;

/**
 * An interface for controlling the Benchmark feature
 */
interface BenchmarkController extends GpsTestListener {

    interface Listener {
        /**
         * Called when the ability to edit the ground truth location changes
         * @param allowChange true if the ground truth can now be edited, and false if it now cannot be edited
         */
        void onAllowGroundTruthEditChanged(boolean allowChange);

        /**
         * Called when the ground truth location has been set for a test
         * @param location the new ground truth location
         */
        void onGroundTruthLocationSaved(Location location);
    }

    /**
     * Sets a listener than will be called when Listener events are fired
     * @param listener a listener than will be called when Listener events are fired
     */
    void setListener(Listener listener);

    /**
     * Called when there is a map click on a location so the controller can be updated with that information
     * @param location location that was clicked on the map
     */
    void onMapClick(Location location);

    /**
     * Called from the hosting class to pass in the Bundle when onSaveInstanceState() is being
     * called in the hosting class so that the BenchmarkController implementation can save any
     * necessary state
     * @param outState outState from the onSaveInstanceState() method in the hosting class
     */
    void onSaveInstanceState(Bundle outState);

    /**
     * Show the Benchmark views
     */
    void show();

    /**
     * Hide the Benchmark views
     */
    void hide();
}
