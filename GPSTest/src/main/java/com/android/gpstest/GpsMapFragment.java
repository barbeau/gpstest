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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.GpsStatus;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockMapFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.LocationSource;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

public class GpsMapFragment extends SherlockMapFragment
        implements GpsTestActivity.GpsTestListener, View.OnClickListener, LocationSource {

	private GoogleMap mMap;
	private OnLocationChangedListener mListener; //Used to update the map with new location
	
	// Constants used to control how the camera animates to a position
	public static final float CAMERA_INITIAL_ZOOM = 18.0f;
    public static final float CAMERA_INITIAL_BEARING = 0.0f;
    public static final float CAMERA_INITIAL_TILT = 45.0f;
	
    private boolean mGotFix;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
	           
        View v = super.onCreateView(inflater, container, savedInstanceState);
        mMap = getMap();
        
        if(isGoogleMapsInstalled()) {
        	if (mMap != null) {
        		//Show the location on the map
                mMap.setMyLocationEnabled(true);
                //Set location source
                mMap.setLocationSource(this);

                GpsTestActivity.getInstance().addSubActivity(this);
        	}
        } else {
        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.please_install_google_maps));
            builder.setCancelable(false);
            builder.setPositiveButton(getString(R.string.install), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                    startActivity(intent);

                    getActivity().finish();
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        
        return v;
    }
    
    @Override
    public void onResume() {
    	SharedPreferences settings = Application.getPrefs();
    	if(mMap != null && settings != null){
    		if (mMap.getMapType() != Integer.valueOf(settings.getString(getString(R.string.pref_key_map_type), String.valueOf(GoogleMap.MAP_TYPE_NORMAL)))) {
    			mMap.setMapType(Integer.valueOf(settings.getString(getString(R.string.pref_key_map_type), String.valueOf(GoogleMap.MAP_TYPE_NORMAL))));
    		}    		
    	}
    	super.onResume();
    }

    public void onClick(View v) {}

    public void gpsStart() {        
        mGotFix = false;
    }

    public void gpsStop() {
    }

    public void onLocationChanged(Location loc) {
    	//Update real-time location on map
		if (mListener != null) {
	        mListener.onLocationChanged(loc);
	    }
		
		if (mMap != null) {
			//Get bounds for detection of real-time location within bounds
	        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
	        if (!mGotFix && 
	        		(!bounds.contains(new LatLng(loc.getLatitude(), loc.getLongitude())) || 
	        				mMap.getCameraPosition().zoom < (mMap.getMaxZoomLevel() / 2))) {
	        	CameraPosition cameraPosition = new CameraPosition.Builder()
		        	.target(new LatLng(loc.getLatitude(), loc.getLongitude()))
	             	.zoom(CAMERA_INITIAL_ZOOM)
	             	.bearing(CAMERA_INITIAL_BEARING) 
	             	.tilt(CAMERA_INITIAL_TILT)
	             	.build(); 
	        	
	        	mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));	        	
	        }
	        mGotFix = true;
		}				
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {}

    public void onProviderEnabled(String provider) {}

    public void onProviderDisabled(String provider) {}

    public void onGpsStatusChanged(int event, GpsStatus status) {}

    /**
     * Maps V2 Location updates
     */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;		
	}

	/**
	 * Maps V2 Location updates
	 */
	@Override
	public void deactivate() {
		mListener = null;		
	}
	
	/**
	 * Returns true if Google Maps is installed, false if it is not
	 * @return
	 */
	@SuppressWarnings("unused")
	public boolean isGoogleMapsInstalled() {
	    try {
	        ApplicationInfo info = getActivity().getPackageManager().getApplicationInfo("com.google.android.apps.maps", 0 );
	        return true;
	    } catch (PackageManager.NameNotFoundException e) {
	        return false;
	    }
	}
}
