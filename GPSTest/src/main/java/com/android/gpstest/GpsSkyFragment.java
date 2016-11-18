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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import java.util.Iterator;

public class GpsSkyFragment extends Fragment implements GpsTestListener {

    private final static String TAG = "GpsSkyFragment";

    // View dimensions, to draw the compass with the correct width and height
    public static int mHeight;

    public static int mWidth;

    private GpsSkyView mSkyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mSkyView = new GpsSkyView(getActivity());
        GpsTestActivity.getInstance().addListener(this);

        // Get the proper height and width of this view, to ensure the compass draws onscreen
        mSkyView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {

                    @SuppressWarnings("deprecation")
                    @SuppressLint("NewApi")
                    @Override
                    public void onGlobalLayout() {
                        final View v = getView();
                        mHeight = v.getHeight();
                        mWidth = v.getWidth();

                        if (v.getViewTreeObserver().isAlive()) {
                            // remove this layout listener
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                v.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                v.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    }
                }
        );

        return mSkyView;
    }

    public void onLocationChanged(Location loc) {
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
    }

    public void gpsStart() {
    }

    public void gpsStop() {
    }

    @Override
    public void onGnssFirstFix(int ttffMillis) {

    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onSatelliteStatusChanged(GnssStatus status) {
        mSkyView.setGnssStatus(status);
    }

    @Override
    public void onGnssStarted() {
        mSkyView.setStarted();
    }

    @Override
    public void onGnssStopped() {
        mSkyView.setStopped();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        mSkyView.setGnssMeasurementEvent(event);
    }

    @Deprecated
    public void onGpsStatusChanged(int event, GpsStatus status) {
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                mSkyView.setStarted();
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                mSkyView.setStopped();
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                mSkyView.setSats(status);
                break;
        }
    }

    @Override
    public void onOrientationChanged(double orientation, double tilt) {
        // For performance reasons, only proceed if this fragment is visible
        if (!getUserVisibleHint()) {
            return;
        }

        if (mSkyView != null) {
            mSkyView.onOrientationChanged(orientation, tilt);
        }
    }

    @Override
    public void onNmeaMessage(String message, long timestamp) {
    }

    private static class GpsSkyView extends View implements GpsTestListener {

        private static final float PRN_TEXT_SCALE = 0.7f;

        private static int SAT_RADIUS;

        private final float mSnrThresholds[];

        private final int mSnrColors[];

        private final float mCn0Thresholds[];

        private final int mCn0Colors[];

        private boolean mUseSnr = false;

        Context mContext;

        WindowManager mWindowManager;

        private Paint mHorizonActiveFillPaint, mHorizonInactiveFillPaint, mHorizonStrokePaint,
                mGridStrokePaint,
                mSatelliteFillPaint, mSatelliteStrokePaint, mSatelliteUsedStrokePaint,
                mNorthPaint, mNorthFillPaint, mPrnIdPaint, mNotInViewPaint;

        private double mOrientation = 0.0;

        private boolean mStarted;

        private float mSnrCn0s[], mElevs[], mAzims[];  // Holds either SNR or C/N0 - see #65

        private boolean mHasEphemeris[], mHasAlmanac[], mUsedInFix[];

        private int mPrns[], mConstellationType[];

        private int mSvCount;

        public GpsSkyView(Context context) {
            super(context);

            mContext = context;
            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
            SAT_RADIUS = GpsTestUtil.dpToPixels(context, 5);

            mHorizonActiveFillPaint = new Paint();
            mHorizonActiveFillPaint.setColor(Color.WHITE);
            mHorizonActiveFillPaint.setStyle(Paint.Style.FILL);
            mHorizonActiveFillPaint.setAntiAlias(true);

            mHorizonInactiveFillPaint = new Paint();
            mHorizonInactiveFillPaint.setColor(Color.LTGRAY);
            mHorizonInactiveFillPaint.setStyle(Paint.Style.FILL);
            mHorizonInactiveFillPaint.setAntiAlias(true);

            mHorizonStrokePaint = new Paint();
            mHorizonStrokePaint.setColor(Color.BLACK);
            mHorizonStrokePaint.setStyle(Paint.Style.STROKE);
            mHorizonStrokePaint.setStrokeWidth(2.0f);
            mHorizonStrokePaint.setAntiAlias(true);

            mGridStrokePaint = new Paint();
            mGridStrokePaint.setColor(Color.GRAY);
            mGridStrokePaint.setStyle(Paint.Style.STROKE);
            mGridStrokePaint.setAntiAlias(true);

            mSatelliteFillPaint = new Paint();
            mSatelliteFillPaint.setColor(Color.YELLOW);
            mSatelliteFillPaint.setStyle(Paint.Style.FILL);
            mSatelliteFillPaint.setAntiAlias(true);

            mSatelliteStrokePaint = new Paint();
            mSatelliteStrokePaint.setColor(Color.BLACK);
            mSatelliteStrokePaint.setStyle(Paint.Style.STROKE);
            mSatelliteStrokePaint.setStrokeWidth(2.0f);
            mSatelliteStrokePaint.setAntiAlias(true);

            mSatelliteUsedStrokePaint = new Paint();
            mSatelliteUsedStrokePaint.setColor(Color.BLACK);
            mSatelliteUsedStrokePaint.setStyle(Paint.Style.STROKE);
            mSatelliteUsedStrokePaint.setStrokeWidth(8.0f);
            mSatelliteUsedStrokePaint.setAntiAlias(true);

            mSnrThresholds = new float[]{0.0f, 10.0f, 20.0f, 30.0f};
            mSnrColors = new int[]{Color.GRAY, Color.RED, Color.YELLOW, Color.GREEN};

            mCn0Thresholds = new float[]{0.0f, 16.6f, 33.3f, 50.0f};
            mCn0Colors = new int[]{Color.GRAY, Color.RED, Color.YELLOW, Color.GREEN};

            mNorthPaint = new Paint();
            mNorthPaint.setColor(Color.BLACK);
            mNorthPaint.setStyle(Paint.Style.STROKE);
            mNorthPaint.setStrokeWidth(4.0f);
            mNorthPaint.setAntiAlias(true);

            mNorthFillPaint = new Paint();
            mNorthFillPaint.setColor(Color.GRAY);
            mNorthFillPaint.setStyle(Paint.Style.FILL);
            mNorthFillPaint.setStrokeWidth(4.0f);
            mNorthFillPaint.setAntiAlias(true);

            mPrnIdPaint = new Paint();
            mPrnIdPaint.setColor(Color.BLACK);
            mPrnIdPaint.setStyle(Paint.Style.STROKE);
            mPrnIdPaint
                    .setTextSize(GpsTestUtil.dpToPixels(getContext(), SAT_RADIUS * PRN_TEXT_SCALE));
            mPrnIdPaint.setAntiAlias(true);

            mNotInViewPaint = new Paint();
            mNotInViewPaint.setColor(ContextCompat.getColor(context, R.color.not_in_view_sat));
            mNotInViewPaint.setStyle(Paint.Style.FILL);
            mNotInViewPaint.setStrokeWidth(4.0f);
            mNotInViewPaint.setAntiAlias(true);

            setFocusable(true);
        }

        public void setStarted() {
            mStarted = true;
            invalidate();
        }

        public void setStopped() {
            mStarted = false;
            mSvCount = 0;
            invalidate();
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void setGnssStatus(GnssStatus status) {
            // Use C/N0 instead of SNR - see #65
            mUseSnr = false;

            if (mPrns == null) {
                /**
                 * We need to allocate arrays big enough so we don't overflow them.  Per
                 * https://developer.android.com/reference/android/location/GnssStatus.html#getSvid(int)
                 * 255 should be enough to contain all known satellites world-wide.
                 */
                final int MAX_LENGTH = 255;
                mPrns = new int[MAX_LENGTH];
                mSnrCn0s = new float[MAX_LENGTH];
                mElevs = new float[MAX_LENGTH];
                mAzims = new float[MAX_LENGTH];
                mConstellationType = new int[MAX_LENGTH];
                mHasEphemeris = new boolean[MAX_LENGTH];
                mHasAlmanac = new boolean[MAX_LENGTH];
                mUsedInFix = new boolean[MAX_LENGTH];
            }

            int length = status.getSatelliteCount();
            mSvCount = 0;
            while (mSvCount < length) {
                mSnrCn0s[mSvCount] = status.getCn0DbHz(mSvCount);
                mElevs[mSvCount] = status.getElevationDegrees(mSvCount);
                mAzims[mSvCount] = status.getAzimuthDegrees(mSvCount);
                mPrns[mSvCount] = status.getSvid(mSvCount);
                mConstellationType[mSvCount] = status.getConstellationType(mSvCount);
                mHasEphemeris[mSvCount] = status.hasEphemerisData(mSvCount);
                mHasAlmanac[mSvCount] = status.hasAlmanacData(mSvCount);
                mUsedInFix[mSvCount] = status.usedInFix(mSvCount);
                mSvCount++;
            }

            mStarted = true;
            invalidate();
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        public void setGnssMeasurementEvent(GnssMeasurementsEvent event) {
            // No-op
        }

        @Deprecated
        public void setSats(GpsStatus status) {
            // Use SNR instead of C/N0 - see #65
            mUseSnr = true;

            Iterator<GpsSatellite> satellites = status.getSatellites().iterator();

            if (mSnrCn0s == null) {
                int length = status.getMaxSatellites();
                mSnrCn0s = new float[length];
                mElevs = new float[length];
                mAzims = new float[length];
                mPrns = new int[length];
                mHasEphemeris = new boolean[length];
                mHasAlmanac = new boolean[length];
                mUsedInFix = new boolean[length];
                // Constellation type isn't used, but instantiate it to avoid NPE in legacy devices
                mConstellationType = new int[length];
            }

            mSvCount = 0;
            while (satellites.hasNext()) {
                GpsSatellite satellite = satellites.next();
                mSnrCn0s[mSvCount] = satellite.getSnr();
                mElevs[mSvCount] = satellite.getElevation();
                mAzims[mSvCount] = satellite.getAzimuth();
                mPrns[mSvCount] = satellite.getPrn();
                mHasEphemeris[mSvCount] = satellite.hasEphemeris();
                mHasAlmanac[mSvCount] = satellite.hasAlmanac();
                mUsedInFix[mSvCount] = satellite.usedInFix();
                mSvCount++;
            }

            mStarted = true;
            invalidate();
        }

        private void drawLine(Canvas c, float x1, float y1, float x2, float y2) {
            // rotate the line based on orientation
            double angle = Math.toRadians(-mOrientation);
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            float centerX = (x1 + x2) / 2.0f;
            float centerY = (y1 + y2) / 2.0f;
            x1 -= centerX;
            y1 = centerY - y1;
            x2 -= centerX;
            y2 = centerY - y2;

            float X1 = cos * x1 + sin * y1 + centerX;
            float Y1 = -(-sin * x1 + cos * y1) + centerY;
            float X2 = cos * x2 + sin * y2 + centerX;
            float Y2 = -(-sin * x2 + cos * y2) + centerY;

            c.drawLine(X1, Y1, X2, Y2, mGridStrokePaint);
        }

        private void drawHorizon(Canvas c, int s) {
            float radius = s / 2;

            c.drawCircle(radius, radius, radius,
                    mStarted ? mHorizonActiveFillPaint : mHorizonInactiveFillPaint);
            drawLine(c, 0, radius, 2 * radius, radius);
            drawLine(c, radius, 0, radius, 2 * radius);
            c.drawCircle(radius, radius, elevationToRadius(s, 60.0f), mGridStrokePaint);
            c.drawCircle(radius, radius, elevationToRadius(s, 30.0f), mGridStrokePaint);
            c.drawCircle(radius, radius, elevationToRadius(s, 0.0f), mGridStrokePaint);
            c.drawCircle(radius, radius, radius, mHorizonStrokePaint);
        }

        private void drawNorthIndicator(Canvas c, int s) {
            float radius = s / 2;
            double angle = Math.toRadians(-mOrientation);
            final float ARROW_HEIGHT_SCALE = 0.05f;
            final float ARROW_WIDTH_SCALE = 0.1f;

            float x1, y1;  // Tip of arrow
            x1 = radius;
            y1 = elevationToRadius(s, 90.0f);

            float x2, y2;
            x2 = x1 + radius * ARROW_HEIGHT_SCALE;
            y2 = y1 + radius * ARROW_WIDTH_SCALE;

            float x3, y3;
            x3 = x1 - radius * ARROW_HEIGHT_SCALE;
            y3 = y1 + radius * ARROW_WIDTH_SCALE;

            Path path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            path.lineTo(x3, y3);
            path.lineTo(x1, y1);
            path.close();

            // Rotate arrow around center point
            Matrix matrix = new Matrix();
            matrix.postRotate((float) -mOrientation, radius, radius);
            path.transform(matrix);

            c.drawPath(path, mNorthPaint);
            c.drawPath(path, mNorthFillPaint);
        }

        private void drawSatellite(Canvas c, int s, float elev, float azim, float snrCn0, int prn,
                int constellationType, boolean usedInFix) {
            double radius, angle;
            float x, y;
            // Place PRN text slightly below drawn satellite
            final double PRN_X_SCALE = 1.4;
            final double PRN_Y_SCALE = 3.8;

            Paint fillPaint;
            if (snrCn0 == 0.0f) {
                // Satellite can't be seen
                fillPaint = mNotInViewPaint;
            } else {
                // Calculate fill color based on signal strength
                fillPaint = getSatellitePaint(mSatelliteFillPaint, snrCn0);
            }

            Paint strokePaint;
            if (usedInFix) {
                strokePaint = mSatelliteUsedStrokePaint;
            } else {
                strokePaint = mSatelliteStrokePaint;
            }

            radius = elevationToRadius(s, elev);
            azim -= mOrientation;
            angle = (float) Math.toRadians(azim);

            x = (float) ((s / 2) + (radius * Math.sin(angle)));
            y = (float) ((s / 2) - (radius * Math.cos(angle)));

            // Change shape based on satellite operator
            GnssType operator;
            if (GpsTestUtil.isGnssStatusListenerSupported()) {
                operator = GpsTestUtil.getGnssConstellationType(constellationType);
            } else {
                operator = GpsTestUtil.getGnssType(prn);
            }
            switch (operator) {
                case NAVSTAR:
                    c.drawCircle(x, y, SAT_RADIUS, fillPaint);
                    c.drawCircle(x, y, SAT_RADIUS, strokePaint);
                    break;
                case GLONASS:
                    c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                            fillPaint);
                    c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                            strokePaint);
                    break;
                case QZSS:
                    drawTriangle(c, x, y, fillPaint, strokePaint);
                    break;
                case BEIDOU:
                    drawPentagon(c, x, y, fillPaint, strokePaint);
                    break;
                case GALILEO:
                    // We're running out of shapes - QZSS should be regional to Japan, so re-use triangle
                    drawTriangle(c, x, y, fillPaint, strokePaint);
                    break;
            }

            c.drawText(String.valueOf(prn), x - (int) (SAT_RADIUS * PRN_X_SCALE),
                    y + (int) (SAT_RADIUS * PRN_Y_SCALE), mPrnIdPaint);
        }

        private float elevationToRadius(int s, float elev) {
            return ((s / 2) - SAT_RADIUS) * (1.0f - (elev / 90.0f));
        }

        private void drawTriangle(Canvas c, float x, float y, Paint fillPaint, Paint strokePaint) {
            float x1, y1;  // Top
            x1 = x;
            y1 = y - SAT_RADIUS;

            float x2, y2; // Lower left
            x2 = x - SAT_RADIUS;
            y2 = y + SAT_RADIUS;

            float x3, y3; // Lower right
            x3 = x + SAT_RADIUS;
            y3 = y + SAT_RADIUS;

            Path path = new Path();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.moveTo(x1, y1);
            path.lineTo(x2, y2);
            path.lineTo(x3, y3);
            path.lineTo(x1, y1);
            path.close();

            c.drawPath(path, fillPaint);
            c.drawPath(path, strokePaint);
        }

        private void drawPentagon(Canvas c, float x, float y, Paint fillPaint, Paint strokePaint) {
            Path path = new Path();
            path.moveTo(x, y - SAT_RADIUS);
            path.lineTo(x - SAT_RADIUS, y - (SAT_RADIUS / 3));
            path.lineTo(x - 2 * (SAT_RADIUS / 3), y + SAT_RADIUS);
            path.lineTo(x + 2 * (SAT_RADIUS / 3), y + SAT_RADIUS);
            path.lineTo(x + SAT_RADIUS, y - (SAT_RADIUS / 3));
            path.close();

            c.drawPath(path, fillPaint);
            c.drawPath(path, strokePaint);
        }

        /**
         * Gets the paint color for a satellite based on provided SNR or C/N0
         *
         * @param base   the base paint color to be changed
         * @param snrCn0 the SNR to use (if mUseSnr is true) or the C/N0 to use (if mUseSnr is
         *               false)
         *               to generate the satellite color based on signal quality
         * @return the paint color for a satellite based on provided SNR or C/N0
         */
        private Paint getSatellitePaint(Paint base, float snrCn0) {
            Paint newPaint;
            newPaint = new Paint(base);

            int numSteps;
            final float thresholds[];
            final int colors[];

            if (mUseSnr) {
                // Use SNR
                numSteps = mSnrThresholds.length;
                thresholds = mSnrThresholds;
                colors = mSnrColors;
            } else {
                // Use C/N0
                numSteps = mCn0Thresholds.length;
                thresholds = mCn0Thresholds;
                colors = mCn0Colors;
            }

            if (snrCn0 <= thresholds[0]) {
                newPaint.setColor(colors[0]);
                return newPaint;
            }

            if (snrCn0 >= thresholds[numSteps - 1]) {
                newPaint.setColor(colors[numSteps - 1]);
                return newPaint;
            }

            for (int i = 0; i < numSteps - 1; i++) {
                float threshold = thresholds[i];
                float nextThreshold = thresholds[i + 1];
                if (snrCn0 >= threshold && snrCn0 <= nextThreshold) {
                    int c1, r1, g1, b1, c2, r2, g2, b2, c3, r3, g3, b3;
                    float f;

                    c1 = colors[i];
                    r1 = Color.red(c1);
                    g1 = Color.green(c1);
                    b1 = Color.blue(c1);

                    c2 = colors[i + 1];
                    r2 = Color.red(c2);
                    g2 = Color.green(c2);
                    b2 = Color.blue(c2);

                    f = (snrCn0 - threshold) / (nextThreshold - threshold);

                    r3 = (int) (r2 * f + r1 * (1.0f - f));
                    g3 = (int) (g2 * f + g1 * (1.0f - f));
                    b3 = (int) (b2 * f + b1 * (1.0f - f));
                    c3 = Color.rgb(r3, g3, b3);

                    newPaint.setColor(c3);

                    return newPaint;
                }
            }

            newPaint.setColor(Color.MAGENTA);

            return newPaint;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int minScreenDimen;

            minScreenDimen = (GpsSkyFragment.mWidth < GpsSkyFragment.mHeight)
                    ? GpsSkyFragment.mWidth : GpsSkyFragment.mHeight;

            drawHorizon(canvas, minScreenDimen);

            drawNorthIndicator(canvas, minScreenDimen);

            if (mElevs != null) {
                int numSats = mSvCount;

                for (int i = 0; i < numSats; i++) {
                    if (mElevs[i] != 0.0f || mAzims[i] != 0.0f) {
                        drawSatellite(canvas, minScreenDimen, mElevs[i], mAzims[i], mSnrCn0s[i],
                                mPrns[i], mConstellationType[i], mUsedInFix[i]);
                    }
                }
            }
        }

        @Override
        public void onOrientationChanged(double orientation, double tilt) {
            mOrientation = orientation;
            invalidate();
        }

        @Override
        public void gpsStart() {
        }

        @Override
        public void gpsStop() {
        }

        @Override
        public void onGnssFirstFix(int ttffMillis) {

        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onSatelliteStatusChanged(GnssStatus status) {
        }

        @Override
        public void onGnssStarted() {
        }

        @Override
        public void onGnssStopped() {
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

        }

        @Override
        public void onNmeaMessage(String message, long timestamp) {
        }

        @Deprecated
        @Override
        public void onGpsStatusChanged(int event, GpsStatus status) {
        }

        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    }
}
