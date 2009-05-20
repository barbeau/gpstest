/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.app.TabActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.TabHost;

import java.util.ArrayList;

public class GpsTestActivity extends TabActivity
        implements LocationListener, GpsStatus.Listener {
    private static final String TAG = "GpsTestActivity";

    private LocationManager mService;
    private LocationProvider mProvider;
    private GpsStatus mStatus;
    private ArrayList<SubActivity> mSubActivities = new ArrayList<SubActivity>();
    boolean mNavigating;
    boolean mStarted;
    private Location mLastLocation;
    private int mFixFrequency = 1;

    private static GpsTestActivity sInstance;

    interface SubActivity extends LocationListener {
        public void gpsStart();
        public void gpsStop();
        public void onGpsStatusChanged(int event, GpsStatus status);
    }

    static GpsTestActivity getInstance() {
        return sInstance;
    }

    void addSubActivity(SubActivity activity) {
        mSubActivities.add(activity);
    }

    private void gpsStart() {
        if (!mNavigating) {
            mService.requestLocationUpdates(mProvider.getName(), mFixFrequency * 1000, 0.0f, this);
            mStarted = true;
        }
        for (SubActivity activity : mSubActivities) {
            activity.gpsStart();
        }
    }

    private void gpsStop() {
        if (mNavigating) {
            mService.removeUpdates(this);
            mStarted = false;
        }
        for (SubActivity activity : mSubActivities) {
            activity.gpsStop();
        }
    }

    private void sendExtraCommand(String command) {
        mService.sendExtraCommand(LocationManager.GPS_PROVIDER, command, null);
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        sInstance = this;

        mService = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        mProvider = mService.getProvider(LocationManager.GPS_PROVIDER);
        if (mProvider == null) {
            // FIXME - fail gracefully here
            Log.e(TAG, "Unable to get GPS_PROVIDER");
        }
        mService.addGpsStatusListener(this);

        final TabHost tabHost = getTabHost();
        final Resources res = getResources();

        tabHost.addTab(tabHost.newTabSpec("tab1")
                .setIndicator(res.getString(R.string.gps_status_tab))
                .setContent(new Intent(this, GpsStatusActivity.class)));

        tabHost.addTab(tabHost.newTabSpec("tab2")
                .setIndicator(res.getString(R.string.gps_map_tab))
                .setContent(new Intent(this, GpsMapActivity.class)));

        tabHost.addTab(tabHost.newTabSpec("tab3")
                .setIndicator(res.getString(R.string.gps_sky_tab))
                .setContent(new Intent(this, GpsSkyActivity.class)));
    }

    @Override
    protected void onDestroy() {
        mService.removeGpsStatusListener(this);
        mService.removeUpdates(this);
        super.onDestroy();
    }

   boolean createOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gps_menu, menu);
        SubMenu subMenu = menu.addSubMenu(R.string.set_fix_frequency);
        inflater.inflate(R.menu.fix_frequency_menu, subMenu);
        return true;
    }

    boolean prepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.gps_start);
        if (item != null) {
            if (mNavigating) {
                item.setTitle(R.string.gps_stop);
            } else {
                item.setTitle(R.string.gps_start);
            }
        }

        item = menu.findItem(R.id.delete_aiding_data);
        if (item != null) {
            item.setEnabled(!mNavigating);
        }

        item = menu.findItem(R.id.send_location);
        if (item != null) {
            item.setEnabled(mLastLocation != null);
        }

        item = menu.findItem(R.id.fix_freq_1_second);
        if (item != null) {
            item.setChecked(mFixFrequency == 1);
        }

        item = menu.findItem(R.id.fix_freq_10_seconds);
        if (item != null) {
            item.setChecked(mFixFrequency == 10);
        }

        item = menu.findItem(R.id.fix_freq_1_minute);
        if (item != null) {
            item.setChecked(mFixFrequency == 60);
        }

        item = menu.findItem(R.id.fix_freq_5_minutes);
        if (item != null) {
            item.setChecked(mFixFrequency == 5 * 60);
        }

        item = menu.findItem(R.id.fix_freq_15_minutes);
        if (item != null) {
            item.setChecked(mFixFrequency == 15 * 60);
        }

        return true;
    }

    boolean optionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.gps_start:
                if (mNavigating) {
                    gpsStop();
                } else {
                    gpsStart();
                }
                return true;

            case R.id.delete_aiding_data:
                sendExtraCommand("delete_aiding_data");
                return true;

            case R.id.send_location:
                sendLocation();
                return true;

            case R.id.force_time_injection:
                sendExtraCommand("force_time_injection");
                return true;

            case R.id.force_xtra_injection:
                sendExtraCommand("force_xtra_injection");
                return true;

            case R.id.fix_freq_1_second:
                setFixFrequency(1);
                return true;

            case R.id.fix_freq_10_seconds:
                setFixFrequency(10);
                return true;

            case R.id.fix_freq_1_minute:
                setFixFrequency(60);
            	return true;

            case R.id.fix_freq_5_minutes:
            	setFixFrequency(5 * 60);
            	return true;

            case R.id.fix_freq_15_minutes:
            	setFixFrequency(15 * 60);
            	return true;
        }

        return false;
    }

    public void onLocationChanged(Location location) {
        mLastLocation = location;

        for (SubActivity activity : mSubActivities) {
            activity.onLocationChanged(location);
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
        for (SubActivity activity : mSubActivities) {
            activity.onStatusChanged(provider, status, extras);
        }
    }

    public void onProviderEnabled(String provider) {
        for (SubActivity activity : mSubActivities) {
            activity.onProviderEnabled(provider);
        }
    }

    public void onProviderDisabled(String provider) {
        for (SubActivity activity : mSubActivities) {
            activity.onProviderDisabled(provider);
        }
    }

    public void onGpsStatusChanged(int event) {
        mStatus = mService.getGpsStatus(mStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
            case GpsStatus.GPS_EVENT_FIRST_FIX:
            case GpsStatus.GPS_EVENT_STARTED:
                mNavigating = true;
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                mNavigating = false;
                break;
        }

        for (SubActivity activity : mSubActivities) {
            activity.onGpsStatusChanged(event, mStatus);
        }
    }

    private void sendLocation() {
        if (mLastLocation != null) {
            Intent intent = new Intent(Intent.ACTION_SENDTO,
                                Uri.fromParts("mailto", "", null));
            String location = "http://maps.google.com/maps?geocode=&q=" +
                    Double.toString(mLastLocation.getLatitude()) + "," +
                    Double.toString(mLastLocation.getLongitude());
            intent.putExtra(Intent.EXTRA_TEXT, location);
            startActivity(intent);
        }
    }

    private void setFixFrequency(int frequency) {
    	if (frequency != mFixFrequency) {
	    	mFixFrequency = frequency;
	        if (mStarted) {
	        	gpsStop();
	        	gpsStart();
	        }
    	}
    }
}
