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

/**
 * An interface for controlling the Benchmark feature
 */
interface BenchmarkController extends GpsTestListener {

    /**
     * Called when there is a map click on a location so the controller can be updated with that information
     * @param location location that was clicked on the map
     */
    void onMapClick(Location location);

    /**
     * Called from the hosting Activity when onBackPressed() is called (i.e., when the user
     * presses the back button)
     * @return true if the controller handled in the click and super.onBackPressed() should not be
     * called by the hosting Activity, or false if the controller didn't handle the click and
     * super.onBackPressed() should be called
     */
    boolean onBackPressed();

    /**
     * Called from the hosting Activity when it is resumed (e.g., to refresh settings)
     */
    void onResume();

    /**
     * Show the Benchmark views
     */
    void show();

    /**
     * Hide the Benchmark views
     */
    void hide();
}
