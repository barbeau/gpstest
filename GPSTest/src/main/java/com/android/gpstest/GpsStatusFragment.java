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

import com.android.gpstest.util.GnssType;
import com.android.gpstest.util.GpsTestUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Iterator;


public class GpsStatusFragment extends Fragment implements GpsTestListener {

    private final static String TAG = "GpsStatusFragment";

    private static final int PRN_COLUMN = 0;

    private static final int FLAG_IMAGE_COLUMN = 1;

    private static final int SNR_COLUMN = 2;

    private static final int ELEVATION_COLUMN = 3;

    private static final int AZIMUTH_COLUMN = 4;

    private static final int FLAGS_COLUMN = 5;

    private static final int COLUMN_COUNT = 6;

    private static final String EMPTY_LAT_LONG = "             ";

    SimpleDateFormat mDateFormat = new SimpleDateFormat("hh:mm:ss.SS a");

    private Resources mRes;

    private TextView mLatitudeView, mLongitudeView, mFixTimeView, mTTFFView, mAltitudeView,
            mAltitudeMslView, mAccuracyView, mSpeedView, mBearingView, mNumSats,
            mPdopLabelView, mPdopView, mHvdopLabelView, mHvdopView;

    private SvGridAdapter mAdapter;

    private int mSvCount, mPrns[], mConstellationType[], mUsedInFixCount;

    private float mSnrCn0s[], mSvElevations[], mSvAzimuths[];

    private String mSnrCn0Title;

    private boolean mHasEphemeris[], mHasAlmanac[], mUsedInFix[];

    private long mFixTime;

    private boolean mNavigating, mGotFix;

    private Drawable mFlagUsa, mFlagRussia, mFlagJapan, mFlagChina, mFlagGalileo;

    public void onLocationChanged(Location location) {
        if (!mGotFix) {
            mTTFFView.setText(GpsTestActivity.getInstance().mTtff);
            mGotFix = true;
        }
        mLatitudeView.setText(getString(R.string.gps_latitude_value, location.getLatitude()));
        mLongitudeView.setText(getString(R.string.gps_longitude_value, location.getLongitude()));
        mFixTime = location.getTime();
        if (location.hasAltitude()) {
            mAltitudeView.setText(getString(R.string.gps_altitude_value, location.getAltitude()));
        } else {
            mAltitudeView.setText("");
        }
        if (location.hasAccuracy()) {
            mAccuracyView.setText(getString(R.string.gps_accuracy_value, location.getAccuracy()));
        } else {
            mAccuracyView.setText("");
        }
        if (location.hasSpeed()) {
            mSpeedView.setText(getString(R.string.gps_speed_value, location.getSpeed()));
        } else {
            mSpeedView.setText("");
        }
        if (location.hasBearing()) {
            mBearingView.setText(getString(R.string.gps_bearing_value, location.getBearing()));
        } else {
            mBearingView.setText("");
        }
        updateFixTime();
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRes = getResources();
        View v = inflater.inflate(R.layout.gps_status, container,
                false);

        mLatitudeView = (TextView) v.findViewById(R.id.latitude);
        mLongitudeView = (TextView) v.findViewById(R.id.longitude);
        mFixTimeView = (TextView) v.findViewById(R.id.fix_time);
        mTTFFView = (TextView) v.findViewById(R.id.ttff);
        mAltitudeView = (TextView) v.findViewById(R.id.altitude);
        mAltitudeMslView = (TextView) v.findViewById(R.id.altitude_msl);
        mAccuracyView = (TextView) v.findViewById(R.id.accuracy);
        mSpeedView = (TextView) v.findViewById(R.id.speed);
        mBearingView = (TextView) v.findViewById(R.id.bearing);
        mNumSats = (TextView) v.findViewById(R.id.num_sats);
        mPdopLabelView = (TextView) v.findViewById(R.id.pdop_label);
        mPdopView = (TextView) v.findViewById(R.id.pdop);
        mHvdopLabelView = (TextView) v.findViewById(R.id.hvdop_label);
        mHvdopView = (TextView) v.findViewById(R.id.hvdop);

        mLatitudeView.setText(EMPTY_LAT_LONG);
        mLongitudeView.setText(EMPTY_LAT_LONG);

        mFlagUsa = getResources().getDrawable(R.drawable.ic_flag_usa);
        mFlagRussia = getResources().getDrawable(R.drawable.ic_flag_russia);
        mFlagJapan = getResources().getDrawable(R.drawable.ic_flag_japan);
        mFlagChina = getResources().getDrawable(R.drawable.ic_flag_china);
        mFlagGalileo = getResources().getDrawable(R.drawable.ic_flag_galileo);

        GridView gridView = (GridView) v.findViewById(R.id.sv_grid);
        mAdapter = new SvGridAdapter(getActivity());
        gridView.setAdapter(mAdapter);
        gridView.setFocusable(false);
        gridView.setFocusableInTouchMode(false);

        GpsTestActivity.getInstance().addListener(this);

        return v;
    }

    private void setStarted(boolean navigating) {
        if (navigating != mNavigating) {
            if (navigating) {

            } else {
                mLatitudeView.setText(EMPTY_LAT_LONG);
                mLongitudeView.setText(EMPTY_LAT_LONG);
                mFixTime = 0;
                updateFixTime();
                mTTFFView.setText("");
                mAltitudeView.setText("");
                mAltitudeMslView.setText("");
                mAccuracyView.setText("");
                mSpeedView.setText("");
                mBearingView.setText("");
                mNumSats.setText("");
                mPdopView.setText("");
                mHvdopView.setText("");

                mSvCount = 0;
                mAdapter.notifyDataSetChanged();
            }
            mNavigating = navigating;
        }
    }

    private void updateFixTime() {
        if (mFixTime == 0 || !GpsTestActivity.getInstance().mStarted) {
            mFixTimeView.setText("");
        } else {
            mFixTimeView.setText(mDateFormat.format(mFixTime));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        GpsTestActivity gta = GpsTestActivity.getInstance();
        setStarted(gta.mStarted);
    }

    public void onGpsStarted() {
        setStarted(true);
    }

    public void onGpsStopped() {
        setStarted(false);
    }

    @SuppressLint("NewApi")
    public void gpsStart() {
        //Reset flag for detecting first fix
        mGotFix = false;
    }

    public void gpsStop() {
    }

    @Deprecated
    public void onGpsStatusChanged(int event, GpsStatus status) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                setStarted(true);
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                setStarted(false);
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                updateLegacyStatus(status);
                break;
        }
    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
        updateGnssStatus(status);
    }

    @Override
    public void onGnssStarted() {
        setStarted(true);
    }

    @Override
    public void onGnssStopped() {
        setStarted(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        // No-op
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
        if (!isAdded()) {
            // Do nothing if the Fragment isn't added
            return;
        }
        if (message.startsWith("$GPGGA") || message.startsWith("$GNGNS")) {
            Double altitudeMsl = GpsTestUtil.getAltitudeMeanSeaLevel(message);
            if (altitudeMsl != null && mNavigating) {
                mAltitudeMslView.setText(getString(R.string.gps_altitude_msl_value, altitudeMsl));
            }
        }
        if (message.startsWith("$GNGSA") || message.startsWith("$GPGSA")) {
            DilutionOfPrecision dop = GpsTestUtil.getDop(message);
            if (dop != null && mNavigating) {
                showDopViews();
                mPdopView.setText(String.valueOf(dop.getPositionDop()));
                mHvdopView.setText(
                        getString(R.string.hvdop_value, dop.getHorizontalDop(),
                                dop.getVerticalDop()));
            }
        }
    }

    private void showDopViews() {
        mPdopLabelView.setVisibility(View.VISIBLE);
        mPdopView.setVisibility(View.VISIBLE);
        mHvdopLabelView.setVisibility(View.VISIBLE);
        mHvdopView.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void updateGnssStatus(GnssStatus status) {
        setStarted(true);
        updateFixTime();

        mSnrCn0Title = mRes.getString(R.string.gps_cn0_column_label);

        if (mPrns == null) {
            /**
             * We need to allocate arrays big enough so we don't overflow them.  Per
             * https://developer.android.com/reference/android/location/GnssStatus.html#getSvid(int)
             * 255 should be enough to contain all known satellites world-wide.
             */
            final int MAX_LENGTH = 255;
            mPrns = new int[MAX_LENGTH];
            mSnrCn0s = new float[MAX_LENGTH];
            mSvElevations = new float[MAX_LENGTH];
            mSvAzimuths = new float[MAX_LENGTH];
            mConstellationType = new int[MAX_LENGTH];
            mHasEphemeris = new boolean[MAX_LENGTH];
            mHasAlmanac = new boolean[MAX_LENGTH];
            mUsedInFix = new boolean[MAX_LENGTH];
        }

        final int length = status.getSatelliteCount();
        mSvCount = 0;
        mUsedInFixCount = 0;
        while (mSvCount < length) {
            int prn = status.getSvid(mSvCount);
            mPrns[mSvCount] = prn;
            mConstellationType[mSvCount] = status.getConstellationType(mSvCount);
            mSnrCn0s[mSvCount] = status.getCn0DbHz(mSvCount);
            mSvElevations[mSvCount] = status.getElevationDegrees(mSvCount);
            mSvAzimuths[mSvCount] = status.getAzimuthDegrees(mSvCount);
            mHasEphemeris[mSvCount] = status.hasEphemerisData(mSvCount);
            mHasAlmanac[mSvCount] = status.hasAlmanacData(mSvCount);
            mUsedInFix[mSvCount] = status.usedInFix(mSvCount);
            if (status.usedInFix(mSvCount)) {
                mUsedInFixCount++;
            }

            mSvCount++;
        }

        mNumSats.setText(getString(R.string.gps_num_sats_value, mUsedInFixCount, mSvCount));

        mAdapter.notifyDataSetChanged();
    }

    @Deprecated
    private void updateLegacyStatus(GpsStatus status) {
        setStarted(true);
        updateFixTime();

        mSnrCn0Title = mRes.getString(R.string.gps_snr_column_label);

        Iterator<GpsSatellite> satellites = status.getSatellites().iterator();

        if (mPrns == null) {
            int length = status.getMaxSatellites();
            mPrns = new int[length];
            mSnrCn0s = new float[length];
            mSvElevations = new float[length];
            mSvAzimuths = new float[length];
            // Constellation type isn't used, but instantiate it to avoid NPE in legacy devices
            mConstellationType = new int[length];
            mHasEphemeris = new boolean[length];
            mHasAlmanac = new boolean[length];
            mUsedInFix = new boolean[length];
        }

        mSvCount = 0;
        mUsedInFixCount = 0;
        while (satellites.hasNext()) {
            GpsSatellite satellite = satellites.next();
            int prn = satellite.getPrn();
            mPrns[mSvCount] = prn;
            mSnrCn0s[mSvCount] = satellite.getSnr();
            mSvElevations[mSvCount] = satellite.getElevation();
            mSvAzimuths[mSvCount] = satellite.getAzimuth();
            mHasEphemeris[mSvCount] = satellite.hasEphemeris();
            mHasAlmanac[mSvCount] = satellite.hasAlmanac();
            mUsedInFix[mSvCount] = satellite.usedInFix();
            if (satellite.usedInFix()) {
                mUsedInFixCount++;
            }
            mSvCount++;
        }

        mNumSats.setText(getString(R.string.gps_num_sats_value, mUsedInFixCount, mSvCount));

        mAdapter.notifyDataSetChanged();
    }

    private class SvGridAdapter extends BaseAdapter {

        private Context mContext;

        public SvGridAdapter(Context c) {
            mContext = c;
        }

        public int getCount() {
            // add 1 for header row
            return (mSvCount + 1) * COLUMN_COUNT;
        }

        public Object getItem(int position) {
            Log.d(TAG, "getItem(" + position + ")");
            return "foo";
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView = null;
            ImageView imageView = null;

            int row = position / COLUMN_COUNT;
            int column = position % COLUMN_COUNT;

            if (convertView != null) {
                if (convertView instanceof ImageView) {
                    imageView = (ImageView) convertView;
                } else if (convertView instanceof TextView) {
                    textView = (TextView) convertView;
                }
            }

            CharSequence text = null;

            if (row == 0) {
                switch (column) {
                    case PRN_COLUMN:
                        text = mRes.getString(R.string.gps_prn_column_label);
                        break;
                    case FLAG_IMAGE_COLUMN:
                        text = mRes.getString(R.string.gps_flag_image_label);
                        break;
                    case SNR_COLUMN:
                        text = mSnrCn0Title;
                        break;
                    case ELEVATION_COLUMN:
                        text = mRes.getString(R.string.gps_elevation_column_label);
                        break;
                    case AZIMUTH_COLUMN:
                        text = mRes.getString(R.string.gps_azimuth_column_label);
                        break;
                    case FLAGS_COLUMN:
                        text = mRes.getString(R.string.gps_flags_column_label);
                        break;
                }
            } else {
                row--;
                switch (column) {
                    case PRN_COLUMN:
                        text = Integer.toString(mPrns[row]);
                        break;
                    case FLAG_IMAGE_COLUMN:
                        if (imageView == null) {
                            imageView = new ImageView(mContext);
                            imageView.setScaleType(ImageView.ScaleType.FIT_START);
                        }
                        GnssType type;
                        if (GpsTestUtil.isGnssStatusListenerSupported()) {
                            type = GpsTestUtil.getGnssConstellationType(mConstellationType[row]);
                        } else {
                            type = GpsTestUtil.getGnssType(mPrns[row]);
                        }
                        switch (type) {
                            case NAVSTAR:
                                imageView.setImageDrawable(mFlagUsa);
                                break;
                            case GLONASS:
                                imageView.setImageDrawable(mFlagRussia);
                                break;
                            case QZSS:
                                imageView.setImageDrawable(mFlagJapan);
                                break;
                            case BEIDOU:
                                imageView.setImageDrawable(mFlagChina);
                                break;
                            case GALILEO:
                                imageView.setImageDrawable(mFlagGalileo);
                                break;
                        }
                        return imageView;
                    case SNR_COLUMN:
                        if (mSnrCn0s[row] != 0.0f) {
                            text = Float.toString(mSnrCn0s[row]);
                        } else {
                            text = "";
                        }
                        break;
                    case ELEVATION_COLUMN:
                        if (mSvElevations[row] != 0.0f) {
                            text = getString(R.string.gps_elevation_column_value,
                                    Float.toString(mSvElevations[row]));
                        } else {
                            text = "";
                        }
                        break;
                    case AZIMUTH_COLUMN:
                        if (mSvAzimuths[row] != 0.0f) {
                            text = getString(R.string.gps_azimuth_column_value,
                                    Float.toString(mSvAzimuths[row]));
                        } else {
                            text = "";
                        }
                        break;
                    case FLAGS_COLUMN:
                        char[] flags = new char[3];
                        flags[0] = !mHasEphemeris[row] ? ' ' : 'E';
                        flags[1] = !mHasAlmanac[row] ? ' ' : 'A';
                        flags[2] = !mUsedInFix[row] ? ' ' : 'U';
                        text = new String(flags);
                        break;
                }
            }

            if (textView == null) {
                textView = new TextView(mContext);
            }

            textView.setText(text);
            return textView;
        }
    }
}