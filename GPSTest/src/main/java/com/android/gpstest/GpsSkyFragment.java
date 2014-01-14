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

import java.util.Iterator;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.gpstest.util.GnssType;
import com.android.gpstest.util.GpsTestUtil;

public class GpsSkyFragment extends SherlockFragment implements GpsTestActivity.GpsTestListener {

    private final static String TAG = "GpsSkyFragment";

    private GpsSkyView mSkyView;
    private SensorManager mSensorManager;

    // Holds sensor data
    private static float[] mRotationMatrix = new float[16];
    private static float[] mRemappedMatrix = new float[16];
    private static float[] mValues = new float[3];
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);   	
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

    	mSkyView = new GpsSkyView(getActivity());        
        GpsTestActivity.getInstance().addSubActivity(this);
        
        return mSkyView;
    }
    
    @Override
    public void onResume() {
    	super.onResume();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            // Use the modern rotation vector sensors
            Sensor vectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(mSkyView, vectorSensor, 16000); // ~60hz
        } else {
            // Use the legacy orientation sensors
            Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (sensor != null) {
                mSensorManager.registerListener(mSkyView, sensor,
                        SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }
    
    @Override
    public void onPause() {
    	mSensorManager.unregisterListener(mSkyView);
    	super.onPause();
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

    private static class GpsSkyView extends View implements SensorEventListener {
        private Paint mHorizonActiveFillPaint, mHorizonInactiveFillPaint, mHorizonStrokePaint,
                      mGridStrokePaint,
                      mSatelliteFillPaint, mSatelliteStrokePaint, mNorthPaint, mNorthFillPaint, mPrnIdPaint;

        Context mContext;
        WindowManager mWindowManager;
        private double mOrientation = 0.0;
        private double mTilt = 0.0;
        private boolean mStarted;
        private float mSnrs[], mElevs[], mAzims[];
        private int mPrns[];
        private int mSvCount;

        private final float mSnrThresholds[];
        private final int mSnrColors[];

        private static final int SAT_RADIUS = 10;
        private static final float PRN_TEXT_SCALE = 2.5f;

        public GpsSkyView(Context context) {
            super(context);

            mContext = context;
            mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

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

            mSnrThresholds = new float[] { 0.0f,       10.0f,     20.0f,        30.0f       };
            mSnrColors     = new int[]   { Color.GRAY, Color.RED, Color.YELLOW, Color.GREEN };

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
            mNorthPaint.setStrokeWidth(4.0f);
            mPrnIdPaint.setTextSize(SAT_RADIUS * PRN_TEXT_SCALE);
            mPrnIdPaint.setAntiAlias(true);

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

        public void setSats(GpsStatus status) {
            Iterator<GpsSatellite> satellites = status.getSatellites().iterator();

            if (mSnrs == null) {
                int length = status.getMaxSatellites();
                mSnrs = new float[length];
                mElevs = new float[length];
                mAzims = new float[length];
                mPrns = new int[length];
            }

            mSvCount = 0;
            while (satellites.hasNext()) {
                GpsSatellite satellite = satellites.next();
                mSnrs[mSvCount] = satellite.getSnr();
                mElevs[mSvCount] = satellite.getElevation();
                mAzims[mSvCount] = satellite.getAzimuth();
                mPrns[mSvCount] = satellite.getPrn();
                mSvCount++;
            }

            mStarted = true;
            invalidate();
        }

        private void drawLine(Canvas c, float x1, float y1, float x2, float y2) {
            // rotate the line based on orientation
            double angle = Math.toRadians(-mOrientation);
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);

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

            c.drawCircle(radius, radius, radius, mStarted ? mHorizonActiveFillPaint : mHorizonInactiveFillPaint);
            drawLine(c, 0, radius, 2 * radius, radius);
            drawLine(c, radius, 0, radius, 2 * radius);
            c.drawCircle(radius, radius, elevationToRadius(s, 60.0f), mGridStrokePaint);
            c.drawCircle(radius, radius, elevationToRadius(s, 30.0f), mGridStrokePaint);
            c.drawCircle(radius, radius, elevationToRadius(s,  0.0f), mGridStrokePaint);
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
            path.lineTo(x2,y2);
            path.lineTo(x3,y3);
            path.lineTo(x1,y1);
            path.close();

            // Rotate arrow around center point
            Matrix matrix = new Matrix();
            matrix.postRotate((float)-mOrientation, radius, radius);
            path.transform(matrix);

            c.drawPath(path, mNorthPaint);
            c.drawPath(path, mNorthFillPaint);
        }

        private void drawSatellite(Canvas c, int s, float elev, float azim, float snr, int prn) {
            double radius, angle;
            float x, y;
            // Place PRN text slightly below drawn satellite
            final double PRN_X_SCALE = 1.4;
            final double PRN_Y_SCALE = 3.5;
            Paint thisPaint;

            thisPaint = getSatellitePaint(mSatelliteFillPaint, snr);

            radius = elevationToRadius(s, elev);
            azim -= mOrientation;
            angle = (float)Math.toRadians(azim);

            x = (float)((s / 2) + (radius * Math.sin(angle)));
            y = (float)((s / 2) - (radius * Math.cos(angle)));

            // Change shape based on satellite operator
            GnssType operator = GpsTestUtil.getGnssType(prn);
            switch (operator) {
                case NAVSTAR:
                    c.drawCircle(x, y, SAT_RADIUS, thisPaint);
                    c.drawCircle(x, y, SAT_RADIUS, mSatelliteStrokePaint);
                    break;
                case GLONASS:
                    c.drawRect(x-SAT_RADIUS, y+SAT_RADIUS, x+SAT_RADIUS, y-SAT_RADIUS, thisPaint);
                    c.drawRect(x-SAT_RADIUS, y+SAT_RADIUS, x+SAT_RADIUS, y-SAT_RADIUS, mSatelliteStrokePaint);
                    break;
            }

            c.drawText(String.valueOf(prn), x - (int) (SAT_RADIUS * PRN_X_SCALE), y + (int) (SAT_RADIUS * PRN_Y_SCALE), mPrnIdPaint);
        }

        private float elevationToRadius(int s, float elev) {
            return ((s / 2) - SAT_RADIUS) * (1.0f - (elev / 90.0f));
        }

        private Paint getSatellitePaint(Paint base, float snr) {
            int numSteps;
            Paint newPaint;

            newPaint = new Paint(base);

            numSteps = mSnrThresholds.length;

            if (snr <= mSnrThresholds[0]) {
                newPaint.setColor(mSnrColors[0]);
                return newPaint;
            }

            if (snr >= mSnrThresholds[numSteps - 1]) {
                newPaint.setColor(mSnrColors[numSteps - 1]);
                return newPaint;
            }

            for (int i = 0; i < numSteps - 1; i++) {
                float threshold = mSnrThresholds[i];
                float nextThreshold = mSnrThresholds[i + 1];
                if (snr >= threshold && snr <= nextThreshold) {
                    int c1, r1, g1, b1, c2, r2, g2, b2, c3, r3, g3, b3;
                    float f;

                    c1 = mSnrColors[i];
                    r1 = Color.red(c1);
                    g1 = Color.green(c1);
                    b1 = Color.blue(c1);

                    c2 = mSnrColors[i + 1];
                    r2 = Color.red(c2);
                    g2 = Color.green(c2);
                    b2 = Color.blue(c2);

                    f = (snr - threshold) / (nextThreshold - threshold);

                    r3 = (int)(r2 * f + r1 * (1.0f - f));
                    g3 = (int)(g2 * f + g1 * (1.0f - f));
                    b3 = (int)(b2 * f + b1 * (1.0f - f));
                    c3 = Color.rgb(r3, g3, b3);

                    newPaint.setColor(c3);

                    return newPaint;
                }
            }

            newPaint.setColor(Color.MAGENTA);

            return newPaint;
        }

        public void onSensorChanged(SensorEvent event) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                // Modern rotation vector sensors
                SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values);
                int orientation = mWindowManager.getDefaultDisplay().getRotation();
                switch (orientation) {
                    case Surface.ROTATION_0:
                        // No orientation change, use default coordinate system
                        SensorManager.getOrientation(mRotationMatrix, mValues);
                        // Log.d(TAG, "Rotation-0");
                        break;
                    case Surface.ROTATION_90:
                        // Log.d(TAG, "Rotation-90");
                        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_Y,
                                SensorManager.AXIS_MINUS_X, mRemappedMatrix);
                        SensorManager.getOrientation(mRemappedMatrix, mValues);
                        break;
                    case Surface.ROTATION_180:
                        // Log.d(TAG, "Rotation-180");
                        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_X,
                                SensorManager.AXIS_MINUS_Y, mRemappedMatrix);
                        SensorManager.getOrientation(mRemappedMatrix, mValues);
                        break;
                    case Surface.ROTATION_270:
                        // Log.d(TAG, "Rotation-270");
                        SensorManager.remapCoordinateSystem(mRotationMatrix, SensorManager.AXIS_MINUS_Y,
                                SensorManager.AXIS_X, mRemappedMatrix);
                        SensorManager.getOrientation(mRemappedMatrix, mValues);
                        break;
                    default:
                        // This shouldn't happen - assume default orientation
                        SensorManager.getOrientation(mRotationMatrix, mValues);
                        // Log.d(TAG, "Rotation-Unknown");
                        break;
                }
                mOrientation = Math.toDegrees(mValues[0]);  // azimuth
                mTilt = Math.toDegrees(mValues[1]);
            } else {
                // Legacy orientation sensors
                mOrientation = event.values[0];
            }
            invalidate();
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int w, h, s;

            w = canvas.getWidth();
            h = canvas.getHeight();
            s = (w < h) ? w : h;

            drawHorizon(canvas, s);

            drawNorthIndicator(canvas, s);

            if (mElevs != null) {
                int numSats = mSvCount;

                for (int i = 0; i < numSats; i++) {
                    if (mSnrs[i] > 0.0f && (mElevs[i] != 0.0f || mAzims[i] != 0.0f))
                        drawSatellite(canvas, s, mElevs[i], mAzims[i], mSnrs[i], mPrns[i]);
                }
            }
        }
    }
}
