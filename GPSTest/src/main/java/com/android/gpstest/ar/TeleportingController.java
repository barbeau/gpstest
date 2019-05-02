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

import android.util.Log;

import com.android.gpstest.util.MiscUtils;
import com.android.gpstest.util.VectorUtil;

/**
 * Flies the user to the search target in manual mode.
 *
 * @author John Taylor
 */
public class TeleportingController extends AbstractController {
    private static final String TAG = MiscUtils.getTag(TeleportingController.class);

    /**
     * Teleport the astronomer instantaneously from his current pointing to a new
     * one.
     *
     * @param targetXyz The destination pointing.
     */
    public void teleport(final GeocentricCoordinates targetXyz) {
        Log.d(TAG, "Teleporting to target " + targetXyz);
        AstronomerModel.Pointing pointing = model.getPointing();
        final GeocentricCoordinates hereXyz = pointing.getLineOfSight();
        if (targetXyz.equals(hereXyz)) {
            return;
        }

        // Here we calculate the new direction of 'up' along the screen in
        // celestial coordinates.  This is not uniquely defined - it just needs
        // to be perpendicular to the target (which is effectively the normal into
        // the screen in celestial coordinates.)
        Vector3 hereTopXyz = pointing.getPerpendicular();
        hereTopXyz.normalize();
        final Vector3 normal = VectorUtil.crossProduct(hereXyz, hereTopXyz);
        Vector3 newUpXyz = VectorUtil.crossProduct(normal, targetXyz);

        model.setPointing(targetXyz, newUpXyz);
    }

    @Override
    public void start() {
        // Nothing to do.
    }

    @Override
    public void stop() {
        // Nothing to do.
        // We could consider aborting the teleport, but it's OK for now.
    }
}
