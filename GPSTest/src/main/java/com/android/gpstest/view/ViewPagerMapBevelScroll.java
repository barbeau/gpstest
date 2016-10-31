/*
 * Copyright (C) 2013 Sean J. Barbeau, 
 * Martin Hochstrasser (some code derived from http://goo.gl/Hyl2E)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gpstest.view;

import com.android.gpstest.SectionsPagerAdapter;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

/**
 * Extension of ViewPager that allows the user to move the map
 * inside the fragment, but still supports swiping to the
 * next fragment via "bezel swipe" (i.e., swiping from the edge)
 */
public class ViewPagerMapBevelScroll extends ViewPager {

    private static final int DEFAULT_SWIPE_MARGIN_WIDTH_DIP = 20;

    private int swipeMarginWidth;

    public ViewPagerMapBevelScroll(Context context) {
        super(context);
        setDefaultSwipeMargin(context);
    }

    public ViewPagerMapBevelScroll(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDefaultSwipeMargin(context);
    }

    private void setDefaultSwipeMargin(final Context context) {
        swipeMarginWidth = (int) (DEFAULT_SWIPE_MARGIN_WIDTH_DIP * context
                .getResources().getDisplayMetrics().density);
    }

    @Override
    protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
        if (v instanceof ViewPagerMapBevelScroll) {
            switch (getCurrentItem()) {
                case SectionsPagerAdapter.GPS_STATUS_FRAGMENT:
                    break;
                case SectionsPagerAdapter.GPS_MAP_FRAGMENT:
                    //If we should allow the map swipe, then return false
                    return !isAllowedMapSwipe(x, dx);
                case SectionsPagerAdapter.GPS_SKY_FRAGMENT:
                    break;
            }
        }
        return super.canScroll(v, checkV, dx, x, y);
    }

    /**
     * Determines if the pointer movement event at x and moved pixels is
     * considered an allowed swipe movement overriding the inner horizontal
     * scroll content protection.
     *
     * @param x  X coordinate of the active touch point
     * @param dx Delta scrolled in pixels
     * @return true if the movement should start a page swipe
     */
    protected boolean isAllowedMapSwipe(final float x, final float dx) {
        return ((x < swipeMarginWidth) && (dx > 0))
                || ((x > (getWidth() - swipeMarginWidth)) && (dx < 0));
    }
}