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

/**
 * Updates some aspect of the {@link AstronomerModel}.
 *
 * <p>Examples are: modifying the model's time, location or direction of
 * pointing.
 *
 * @author John Taylor
 */
public interface Controller {

    /**
     * Enables or disables this controller. When disabled the controller might
     * still be calculating updates, but won't pass them on to the model.
     */
    void setEnabled(boolean enabled);

    /**
     * Sets the {@link AstronomerModel} to be controlled by this controller.
     */
    void setModel(AstronomerModel model);

    /**
     * Starts this controller.
     *
     * <p>Called when the application is active.  Controllers that require
     * expensive resources such as sensor readings should obtain them when this is
     * called.
     */
    void start();

    /**
     * Stops this controller.
     *
     * <p>Called when the application or activity is inactive.  Controllers that
     * require expensive resources such as sensor readings should release them
     * when this is called.
     */
    void stop();
}
