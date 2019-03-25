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
package com.android.gpstest;

import android.app.Application;

import com.android.gpstest.model.MapPadding;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

/**
 * View model that holds map padding
 */
public class MapPaddingViewModel extends AndroidViewModel {

    private MutableLiveData<MapPadding> mMapPadding = new MutableLiveData<>();

    // Keep track of current map padding
    private int mLeft = 0;

    private int mTop = 0;

    private int mRight = 0;

    private int mBottom = 0;

    public static final int DEFAULT_MAP_PADDING = 0;

    public MapPaddingViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * Define a visible region on the map, to signal to the map that portions of the map around
     * the edges may be obscured, by setting padding on each of the four edges of the map.
     *
     * @param left   the number of pixels of padding to be added on the left of the map, or null
     *               if the existing padding should be used
     * @param top    the number of pixels of padding to be added on the top of the map, or null
     *               if the existing padding should be used
     * @param right  the number of pixels of padding to be added on the right of the map, or null
     *               if the existing padding should be used
     * @param bottom the number of pixels of padding to be added on the bottom of the map, or null
     *               if the existing padding should be used
     */
    public void setPadding(Integer left, Integer top, Integer right, Integer bottom) {

        if (left != null) {
            mLeft = left;
        }
        if (top != null) {
            mTop = top;
        }
        if (right != null) {
            mRight = right;
        }
        if (bottom != null) {
            mBottom = bottom;
        }

        mMapPadding.setValue(new MapPadding(mLeft, mTop, mRight, mBottom));
    }

    public LiveData<MapPadding> getPadding() {
        return mMapPadding;
    }
}
