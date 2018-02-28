/*
 * Copyright (C) 2008-2018 The Android Open Source Project,
 * Sean J. Barbeau (sjbarbeau@gmail.com)
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

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Typeface;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.gpstest.util.GnssType;
import com.android.gpstest.util.GpsTestUtil;
import com.android.gpstest.util.MathUtils;

import java.text.SimpleDateFormat;
import java.util.Iterator;

import static android.util.TypedValue.COMPLEX_UNIT_DIP;
import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static com.android.gpstest.util.GpsTestUtil.isFragmentAttached;


public class GpsStatusFragment extends Fragment implements GpsTestListener {

    public final static String TAG = "GpsStatusFragment";

    private static final String EMPTY_LAT_LONG = "             ";

    @SuppressLint("SimpleDateFormat") // See #117
    SimpleDateFormat mDateFormat = new SimpleDateFormat(
            DateFormat.is24HourFormat(Application.get().getApplicationContext())
                    ? "HH:mm:ss" : "hh:mm:ss a");

    private Resources mRes;

    private TextView mLatitudeView, mLongitudeView, mFixTimeView, mTTFFView, mAltitudeView,
            mAltitudeMslView, mAccuracyView, mSpeedView, mBearingView, mNumSats,
            mPdopLabelView, mPdopView, mHvdopLabelView, mHvdopView;

    private RecyclerView mStatusList;

    private GnssStatusAdapter mAdapter;

    private int mSvCount, mPrns[], mConstellationType[], mUsedInFixCount;

    private float mCarrierFreqsHz[], mSnrCn0s[], mSvElevations[], mSvAzimuths[];

    private String mSnrCn0Title;

    private boolean mHasEphemeris[], mHasAlmanac[], mUsedInFix[];

    private long mFixTime;

    private boolean mNavigating;

    private Drawable mFlagUsa, mFlagRussia, mFlagJapan, mFlagChina, mFlagGalileo, mFlagIndia,
            mFlagCanada, mFlagUnitedKingdom, mFlagLuxembourg;

    private boolean mUseLegacyGnssApi = false;

    private String mTtff = "";

    public void onLocationChanged(Location location) {
        if (!isFragmentAttached(this)) {
            // Fragment isn't visible, so return to avoid IllegalStateException (see #85)
            return;
        }

        // Make sure TTFF is shown, if the TTFF is acquired before the mTTFFView is initialized
        mTTFFView.setText(mTtff);

        mLatitudeView.setText(mRes.getString(R.string.gps_latitude_value, location.getLatitude()));
        mLongitudeView.setText(mRes.getString(R.string.gps_longitude_value, location.getLongitude()));
        mFixTime = location.getTime();
        if (location.hasAltitude()) {
            mAltitudeView.setText(mRes.getString(R.string.gps_altitude_value, location.getAltitude()));
        } else {
            mAltitudeView.setText("");
        }
        if (location.hasAccuracy()) {
            mAccuracyView.setText(mRes.getString(R.string.gps_accuracy_value, location.getAccuracy()));
        } else {
            mAccuracyView.setText("");
        }
        if (location.hasSpeed()) {
            mSpeedView.setText(mRes.getString(R.string.gps_speed_value, location.getSpeed()));
        } else {
            mSpeedView.setText("");
        }
        if (location.hasBearing()) {
            mBearingView.setText(mRes.getString(R.string.gps_bearing_value, location.getBearing()));
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
        View v = inflater.inflate(R.layout.gps_status, container,false);

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
        mFlagIndia = getResources().getDrawable(R.drawable.ic_flag_gagan);
        mFlagCanada = getResources().getDrawable(R.drawable.ic_flag_canada);
        mFlagUnitedKingdom = getResources().getDrawable(R.drawable.ic_flag_united_kingdom);
        mFlagLuxembourg = getResources().getDrawable(R.drawable.ic_flag_luxembourg);

        mStatusList = v.findViewById(R.id.status_list);
        mAdapter = new GnssStatusAdapter();
        mStatusList.setAdapter(mAdapter);
        mStatusList.setFocusable(false);
        mStatusList.setFocusableInTouchMode(false);
        LinearLayoutManager llm = new LinearLayoutManager(getContext());
        llm.setAutoMeasureEnabled(true);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mStatusList.setLayoutManager(llm);
        mStatusList.setNestedScrollingEnabled(false);

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
        if (mFixTime == 0 || (GpsTestActivity.getInstance() != null && !GpsTestActivity.getInstance().mStarted)) {
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
                mTtff = GpsTestUtil.getTtffString(status.getTimeToFirstFix());
                if (mTTFFView != null) {
                    mTTFFView.setText(mTtff);
                }
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                updateLegacyStatus(status);
                break;
        }
    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {
        mTtff = GpsTestUtil.getTtffString(ttffMillis);
        if (mTTFFView != null) {
            mTTFFView.setText(mTtff);
        }
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
                mAltitudeMslView.setText(mRes.getString(R.string.gps_altitude_msl_value, altitudeMsl));
            }
        }
        if (message.startsWith("$GNGSA") || message.startsWith("$GPGSA")) {
            DilutionOfPrecision dop = GpsTestUtil.getDop(message);
            if (dop != null && mNavigating) {
                showDopViews();
                mPdopView.setText(String.valueOf(dop.getPositionDop()));
                mHvdopView.setText(
                        mRes.getString(R.string.hvdop_value, dop.getHorizontalDop(),
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
        mUseLegacyGnssApi = false;
        setStarted(true);
        updateFixTime();

        if (!isFragmentAttached(this)) {
            // Fragment isn't visible, so return to avoid IllegalStateException (see #85)
            return;
        }

        mSnrCn0Title = mRes.getString(R.string.gps_cn0_column_label);

        if (mPrns == null) {
            /**
             * We need to allocate arrays big enough so we don't overflow them.  Per
             * https://developer.android.com/reference/android/location/GnssStatus.html#getSvid(int)
             * 255 should be enough to contain all known satellites world-wide.
             */
            final int MAX_LENGTH = 255;
            mPrns = new int[MAX_LENGTH];
            if (GpsTestUtil.isGnssCarrierFrequenciesSupported()) {
                mCarrierFreqsHz = new float[MAX_LENGTH];
            }
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
            if (GpsTestUtil.isGnssCarrierFrequenciesSupported()) {
                mCarrierFreqsHz[mSvCount] = status.getCarrierFrequencyHz(mSvCount);
            }
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

        mNumSats.setText(mRes.getString(R.string.gps_num_sats_value, mUsedInFixCount, mSvCount));

        mAdapter.notifyDataSetChanged();
    }

    @Deprecated
    private void updateLegacyStatus(GpsStatus status) {
        mUseLegacyGnssApi = true;
        setStarted(true);
        updateFixTime();

        if (!isFragmentAttached(this)) {
            // Fragment isn't visible, so return to avoid IllegalStateException (see #85)
            return;
        }

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

        mNumSats.setText(mRes.getString(R.string.gps_num_sats_value, mUsedInFixCount, mSvCount));

        mAdapter.notifyDataSetChanged();
    }

    private class GnssStatusAdapter extends RecyclerView.Adapter<GnssStatusAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView svId;
            private final TextView gnssFlagHeader;
            private final ImageView gnssFlag;
            private final LinearLayout gnssFlagLayout;
            private final TextView carrierFrequency;
            private final TextView signal;
            private final TextView elevation;
            private final TextView azimuth;
            private final TextView statusFlags;

            ViewHolder(View v) {
                super(v);
                svId = v.findViewById(R.id.sv_id);
                gnssFlagHeader = v.findViewById(R.id.gnss_flag_header);
                gnssFlag = v.findViewById(R.id.gnss_flag);
                gnssFlagLayout = v.findViewById(R.id.gnss_flag_layout);
                carrierFrequency = v.findViewById(R.id.carrier_frequency);
                signal = v.findViewById(R.id.signal);
                elevation = v.findViewById(R.id.elevation);
                azimuth = v.findViewById(R.id.azimuth);
                statusFlags = v.findViewById(R.id.status_flags);
            }

            public TextView getSvId() {
                return svId;
            }

            public TextView getGnssFlagHeader() {
                return gnssFlagHeader;
            }

            public ImageView getGnssFlag() {
                return gnssFlag;
            }

            public LinearLayout getGnssFlagLayout() {
                return gnssFlagLayout;
            }

            public TextView getCarrierFrequency() {
                return carrierFrequency;
            }

            public TextView getSignal() {
                return signal;
            }

            public TextView getElevation() {
                return elevation;
            }

            public TextView getAzimuth() {
                return azimuth;
            }

            public TextView getStatusFlags() {
                return statusFlags;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View v = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.status_row_item, viewGroup, false);
            return new ViewHolder(v);
        }

        @Override
        public int getItemCount() {
            // Add 1 for header row
            return mSvCount + 1;
        }

        public void onBindViewHolder(ViewHolder v, final int position) {
            if (position == 0) {
                // Show the header field for the GNSS flag and hide the ImageView
                v.getGnssFlagHeader().setVisibility(View.VISIBLE);
                v.getGnssFlag().setVisibility(View.GONE);
                v.getGnssFlagLayout().setVisibility(View.GONE);

                // Populate the header fields
                v.getSvId().setText(mRes.getString(R.string.gps_prn_column_label));
                v.getSvId().setTypeface(v.getSvId().getTypeface(), Typeface.BOLD);
                v.getGnssFlagHeader().setText(mRes.getString(R.string.gps_flag_image_label));
                if (GpsTestUtil.isGnssCarrierFrequenciesSupported()) {
                    v.getCarrierFrequency().setVisibility(View.VISIBLE);
                    v.getCarrierFrequency().setText(mRes.getString(R.string.gps_carrier_column_label));
                    v.getCarrierFrequency().setTypeface(v.getCarrierFrequency().getTypeface(), Typeface.BOLD);
                } else {
                    v.getCarrierFrequency().setVisibility(View.GONE);
                }
                v.getSignal().setText(mSnrCn0Title);
                v.getSignal().setTypeface(v.getSignal().getTypeface(), Typeface.BOLD);
                v.getElevation().setText(mRes.getString(R.string.gps_elevation_column_label));
                v.getElevation().setTypeface(v.getElevation().getTypeface(), Typeface.BOLD);
                v.getAzimuth().setText(mRes.getString(R.string.gps_azimuth_column_label));
                v.getAzimuth().setTypeface(v.getAzimuth().getTypeface(), Typeface.BOLD);
                v.getStatusFlags().setText(mRes.getString(R.string.gps_flags_column_label));
                v.getStatusFlags().setTypeface(v.getStatusFlags().getTypeface(), Typeface.BOLD);
            } else {
                // There is a header at 0, so the first data row will be at position - 1, etc.
                int dataRow = position - 1;

                // Show the row field for the GNSS flag mImage and hide the header
                v.getGnssFlagHeader().setVisibility(View.GONE);
                v.getGnssFlag().setVisibility(View.VISIBLE);
                v.getGnssFlagLayout().setVisibility(View.VISIBLE);

                // Populate status data for this row
                v.getSvId().setText(Integer.toString(mPrns[dataRow]));
                v.getGnssFlag().setScaleType(ImageView.ScaleType.FIT_START);

                GnssType type;
                if (GpsTestUtil.isGnssStatusListenerSupported() && !mUseLegacyGnssApi) {
                    type = GpsTestUtil.getGnssConstellationType(mConstellationType[dataRow], mPrns[dataRow]);
                } else {
                    type = GpsTestUtil.getGnssType(mPrns[dataRow]);
                }
                switch (type) {
                    case NAVSTAR:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagUsa);
                        break;
                    case GLONASS:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagRussia);
                        break;
                    case QZSS:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagJapan);
                        break;
                    case BEIDOU:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagChina);
                        break;
                    case GALILEO:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagGalileo);
                        break;
                    case GAGAN:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagIndia);
                        break;
                    case ANIK:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagCanada);
                        break;
                    case GALAXY_15:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagUsa);
                        break;
                    case INMARSAT_3F2:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagUnitedKingdom);
                        break;
                    case INMARSAT_4F3:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagUnitedKingdom);
                        break;
                    case SES_5:
                        v.getGnssFlag().setVisibility(View.VISIBLE);
                        v.getGnssFlag().setImageDrawable(mFlagLuxembourg);
                        break;
                    case UNKNOWN:
                        v.getGnssFlag().setVisibility(View.INVISIBLE);
                        break;
                }
                if (GpsTestUtil.isGnssCarrierFrequenciesSupported()) {
                    if (mCarrierFreqsHz[dataRow] != 0.0f) {
                        // Convert Hz to MHz
                        float carrierMhz = MathUtils.toMhz(mCarrierFreqsHz[dataRow]);
                        String carrierLabel = GpsTestUtil.getCarrierFrequencyLabel(mConstellationType[dataRow],
                                mPrns[dataRow],
                                carrierMhz);
                        if (carrierLabel != null) {
                            // Make sure it's the normal text size (in case it's previously been
                            // resized to show raw number).  Use another TextView for default text size.
                            v.getCarrierFrequency().setTextSize(COMPLEX_UNIT_PX, v.getSvId().getTextSize());
                            // Show label such as "L1"
                            v.getCarrierFrequency().setText(carrierLabel);
                        } else {
                            // Shrink the size so we can show raw number
                            v.getCarrierFrequency().setTextSize(COMPLEX_UNIT_DIP, 10);
                            // Show raw number for carrier frequency
                            v.getCarrierFrequency().setText(Float.toString(carrierMhz));
                        }
                    } else {
                        v.getCarrierFrequency().setText("");
                    }
                } else {
                    v.getCarrierFrequency().setVisibility(View.GONE);
                }
                if (mSnrCn0s[dataRow] != 0.0f) {
                    v.getSignal().setText(Float.toString(mSnrCn0s[dataRow]));
                } else {
                    v.getSignal().setText("");
                }

                if (mSvElevations[dataRow] != 0.0f) {
                    v.getElevation().setText(mRes.getString(R.string.gps_elevation_column_value,
                                    Float.toString(mSvElevations[dataRow])));
                } else {
                    v.getElevation().setText("");
                }

                if (mSvAzimuths[dataRow] != 0.0f) {
                    v.getAzimuth().setText(mRes.getString(R.string.gps_azimuth_column_value,
                            Float.toString(mSvAzimuths[dataRow])));
                } else {
                    v.getAzimuth().setText("");
                }

                char[] flags = new char[3];
                flags[0] = !mHasEphemeris[dataRow] ? ' ' : 'E';
                flags[1] = !mHasAlmanac[dataRow] ? ' ' : 'A';
                flags[2] = !mUsedInFix[dataRow] ? ' ' : 'U';
                v.getStatusFlags().setText(new String(flags));
            }
        }
    }
}