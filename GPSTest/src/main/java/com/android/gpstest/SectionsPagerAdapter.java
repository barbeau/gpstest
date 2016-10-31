/*
 * Copyright (C) 2008-2013 The Android Open Source Project,
 * Sean J. Barbeau
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

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * A {@link FragmentStatePagerAdapter} that returns a fragment corresponding to
 * one of the primary sections of the app.
 */
public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

    public static final int NUMBER_OF_TABS = 3; // Used to set up TabListener

    // Constants for the different fragments that will be displayed in tabs, in numeric order
    public static final int GPS_STATUS_FRAGMENT = 0;

    public static final int GPS_MAP_FRAGMENT = 1;

    public static final int GPS_SKY_FRAGMENT = 2;

    // Maintain handle to Fragments to avoid recreating them if one already
    // exists
    Fragment gpsStatus, gpsMap, gpsSky;

    public SectionsPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case GPS_STATUS_FRAGMENT:
                if (gpsStatus == null) {
                    gpsStatus = new GpsStatusFragment();
                }
                return gpsStatus;
            case GPS_MAP_FRAGMENT:
                if (gpsMap == null) {
                    gpsMap = new GpsMapFragment();
                }
                return gpsMap;
            case GPS_SKY_FRAGMENT:
                if (gpsSky == null) {
                    gpsSky = new GpsSkyFragment();
                }
                return gpsSky;
        }
        return null; // This should never happen
    }

    @Override
    public int getCount() {
        return NUMBER_OF_TABS;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case GPS_STATUS_FRAGMENT:
                return Application.get().getString(R.string.gps_status_tab);
            case GPS_MAP_FRAGMENT:
                return Application.get().getString(R.string.gps_map_tab);
            case GPS_SKY_FRAGMENT:
                return Application.get().getString(R.string.gps_sky_tab);
        }
        return null; // This should never happen
    }
}
