// Copyright 2008 Google Inc. From StarDroid.
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

/**
 * Implements some of the boilerplate of a {@link Controller}.
 *
 * @author John Taylor
 */
public abstract class AbstractController implements Controller {
    private static final String TAG = MiscUtils.getTag(AbstractController.class);
    public AstronomerModel model;
    protected boolean enabled = true;

    // TODO(jontayler): remove this
    @Override
    public void setModel(AstronomerModel model) {
        this.model = model;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            Log.d(TAG, "Enabling controller " + this);
        } else {
            Log.d(TAG, "Disabling controller " + this);
        }
        this.enabled = enabled;
    }
}
