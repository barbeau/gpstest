/*
 * Copyright (C) 2019 Sean J. Barbeau
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
package com.android.gpstest.map;

/**
 * Constant values used in the implementation of the maps
 */
public class MapConstants {
    public static final float CAMERA_INITIAL_ZOOM = 18.0f;

    public static final float CAMERA_INITIAL_BEARING = 0.0f;

    public static final float CAMERA_INITIAL_TILT_MAP = 45.0f;

    public static final float CAMERA_INITIAL_TILT_ACCURACY = 0.0f;

    public static final float CAMERA_ANCHOR_ZOOM = 19.0f;

    public static final float CAMERA_MIN_TILT = 0.0f;

    public static final float CAMERA_MAX_TILT = 90.0f;

    public static final double TARGET_OFFSET_METERS = 150;

    public static final float DRAW_LINE_THRESHOLD_METERS = 0.01f;

    // Amount of time the user must not touch the map for the automatic camera movements to kick in
    public static final long MOVE_MAP_INTERACTION_THRESHOLD = 5 * 1000; // milliseconds

    public static final String PREFERENCE_SHOWED_DIALOG = "showed_google_map_install_dialog";

    public static final String MODE = "mode";

    public static final String MODE_MAP = "mode_map";

    public static final String MODE_ACCURACY = "mode_accuracy";

    public static final String GROUND_TRUTH = "ground_truth";

    public static final String ALLOW_GROUND_TRUTH_CHANGE = "allow_ground_truth_change";
}
