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
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import java.util.Iterator;

public class GpsSkyFragment extends Fragment implements GpsTestActivity.GpsTestListener {

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

    private static class GpsSkyView extends View implements GpsTestActivity.GpsTestListener {

        private static final int SAT_RADIUS = 10;

        private static final float PRN_TEXT_SCALE = 2.5f;

        private final float mSnrThresholds[];

        private final int mSnrColors[];

        private Paint mHorizonActiveFillPaint, mHorizonInactiveFillPaint, mHorizonStrokePaint,
                mGridStrokePaint,
                mSatelliteFillPaint, mSatelliteStrokePaint, mNorthPaint, mNorthFillPaint,
                mPrnIdPaint, mSnrFillPaint, mSnrValuePaint;

        private double mOrientation = Double.MIN_VALUE;

        private boolean mStarted;

        private float mSnrs[], mElevs[], mAzims[];

        private int mPrns[];

        private int mSvCount;

        public GpsSkyView(Context context) {
            super(context);

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

            mSnrThresholds = new float[]{0.0f, 10.0f, 20.0f, 30.0f};
            mSnrColors = new int[]{Color.GRAY, Color.RED, Color.YELLOW, Color.GREEN};

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

            mSnrFillPaint = new Paint();
            mSnrFillPaint.setStyle(Paint.Style.FILL);
            mSnrFillPaint.setAntiAlias(true);

            mSnrValuePaint = new Paint(mHorizonActiveFillPaint);

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

        private void drawHorizon(Canvas c, int width, int height) {
            float centerX = width / 2;
            float centerY = height / 2;
            float radius = width > height ? centerY : centerX;

            c.drawCircle(centerX, centerY, radius,
                    mStarted ? mHorizonActiveFillPaint : mHorizonInactiveFillPaint);
            if (mOrientation != Double.MIN_VALUE) {
                    drawLine(c, centerX - radius, centerY, centerX + radius, centerY);
                    drawLine(c, centerX, centerY - radius, centerX, centerY + radius);
            }
            c.drawCircle(centerX, centerY, elevationToRadius((int) (radius * 2), 60.0f), mGridStrokePaint);
            c.drawCircle(centerX, centerY, elevationToRadius((int) (radius * 2), 30.0f), mGridStrokePaint);
            c.drawCircle(centerX, centerY, elevationToRadius((int) (radius * 2), 0.0f), mGridStrokePaint);
            c.drawCircle(centerX, centerY, radius - mHorizonStrokePaint.getStrokeWidth(), mHorizonStrokePaint);
        }

        private void drawNorthIndicator(Canvas c, int width, int height) {
            if (mOrientation != Double.MIN_VALUE) {
                float centerX = width / 2;
                float centerY = height / 2;
                float radius = width > height ? centerY : centerX;
                int minScreenDimen = width > height ? height : width;
                // double angle = Math.toRadians(-mOrientation);
                final float ARROW_HEIGHT_SCALE = 0.05f;
                final float ARROW_WIDTH_SCALE = 0.1f;

                float x1, y1;  // Tip of arrow
                x1 = centerX;
                y1 = elevationToRadius(minScreenDimen, 90.0f) + (centerY - radius) + mNorthPaint.getStrokeWidth();

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
                matrix.postRotate((float) -mOrientation, centerX, centerY);
                path.transform(matrix);

                c.drawPath(path, mNorthPaint);
                c.drawPath(path, mNorthFillPaint);
            }
        }

        private void drawSatellite(Canvas c, int width, int height, float elev, float azim, float snr, int prn) {
            if (mOrientation != Double.MIN_VALUE) {
                float centerX = width / 2;
                float centerY = height / 2;
                int minScreenDimen = width > height ? height : width;

                double radius, angle;
                float x, y;
                // Place PRN text slightly below drawn satellite
                final double PRN_X_SCALE = 1.4;
                final double PRN_Y_SCALE = 3.5;
                Paint thisPaint;

                thisPaint = getSatellitePaint(mSatelliteFillPaint, snr);

                radius = elevationToRadius(minScreenDimen, elev);
                azim -= mOrientation;
                angle = (float) Math.toRadians(azim);

                x = (float) (centerX + (radius * Math.sin(angle)));
                y = (float) (centerY - (radius * Math.cos(angle)));

                // Change shape based on satellite operator
                GnssType operator = GpsTestUtil.getGnssType(prn);
                switch (operator) {
                    case NAVSTAR:
                        c.drawCircle(x, y, SAT_RADIUS, thisPaint);
                        c.drawCircle(x, y, SAT_RADIUS, mSatelliteStrokePaint);
                        break;
                    case GLONASS:
                        c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                                thisPaint);
                        c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                                mSatelliteStrokePaint);
                        break;
                    case QZSS:
                        drawTriangle(c, x, y, thisPaint);
                        break;
                    case BEIDOU:
                        drawPentagon(c, x, y, thisPaint);
                        break;
                }

                c.drawText(String.valueOf(prn), x - (int) (SAT_RADIUS * PRN_X_SCALE),
                        y + (int) (SAT_RADIUS * PRN_Y_SCALE), mPrnIdPaint);
            }
        }

    private int drawDisplayHelp(Canvas c, int width, int height) {
        int result;
        float centerX = width / 2;
        float centerY = height / 2;
        float radius = width > height ? centerY : centerX;
        float x, y;
        if (width > height) {
            x = (centerX - radius) / 2;
            y = 4 * SAT_RADIUS;
            c.drawCircle(x, y, SAT_RADIUS, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_usa);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            x = (centerX - radius) / 2;
            y += 4 * SAT_RADIUS + drawable.getIntrinsicHeight();
            c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                    mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_russia);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            x = (centerX - radius) / 2;
            y += 4 * SAT_RADIUS + drawable.getIntrinsicHeight();
            drawTriangle(c, x, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_japan);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            x = (centerX - radius) / 2;
            y += 4 * SAT_RADIUS + drawable.getIntrinsicHeight();
            drawPentagon(c, x, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_china);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            x = centerX + (centerX - (x + drawable.getIntrinsicWidth()));
            y += drawable.getIntrinsicHeight() / 2;
            int length = (int) ((y + drawable.getIntrinsicHeight()) - 4 * SAT_RADIUS * 2);
            if (mSnrFillPaint.getShader() == null) {
                mSnrFillPaint.setShader(new LinearGradient(x, y, x + SAT_RADIUS, y - length,
                        mSnrColors, null, Shader.TileMode.REPEAT));
            }
            c.drawRect(x, y - length, x + SAT_RADIUS, y,
                    mSnrFillPaint);

            x += 2 * SAT_RADIUS;
            String text = getContext().getString(R.string.gps_snr_column_label) + " = " + mSnrThresholds[0] + " " + getContext().getString(R.string.unit_db);
            c.drawText(text, x, y, mSnrValuePaint);

            text = getContext().getString(R.string.gps_snr_column_label) + " = " + mSnrThresholds[mSnrThresholds.length - 1] + " " + getContext().getString(R.string.unit_db);
            c.drawText(String.valueOf(text), x, y - length + mSnrValuePaint.getTextSize(), mSnrValuePaint);

            result = 0;
        } else {
            x = 2 * SAT_RADIUS;
            y = centerY + radius + 3 * SAT_RADIUS;
            c.drawCircle(x, y, SAT_RADIUS, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_usa);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            x += 5 * SAT_RADIUS + drawable.getIntrinsicWidth();
            c.drawRect(x - SAT_RADIUS, y - SAT_RADIUS, x + SAT_RADIUS, y + SAT_RADIUS,
                    mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_russia);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            x += 5 * SAT_RADIUS + drawable.getIntrinsicWidth();
            drawTriangle(c, x, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_japan);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            x += 5 * SAT_RADIUS + drawable.getIntrinsicWidth();
            drawPentagon(c, x, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            c.drawLine(x, y, x + SAT_RADIUS, y, mSnrValuePaint);

            x += 2 * SAT_RADIUS;
            drawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_flag_china);
            drawable.setBounds((int) x, (int) (y - drawable.getIntrinsicHeight() / 2), (int) (x + drawable.getIntrinsicWidth()), (int) (y + drawable.getIntrinsicHeight() / 2));
            drawable.draw(c);

            int length = (int) ((x + drawable.getIntrinsicWidth()) - SAT_RADIUS);
            x = SAT_RADIUS;
            y += 3 * SAT_RADIUS;
            if (mSnrFillPaint.getShader() == null) {
                mSnrFillPaint.setShader(new LinearGradient(x, y, x + length, y + SAT_RADIUS,
                        mSnrColors, null, Shader.TileMode.REPEAT));
            }
            c.drawRect(x, y, x + length, y + SAT_RADIUS,
                    mSnrFillPaint);

            y += 3 * SAT_RADIUS;
            String text = getContext().getString(R.string.gps_snr_column_label) + " = " + mSnrThresholds[0] + " " + getContext().getString(R.string.unit_db);
            c.drawText(text, x, y, mSnrValuePaint);

            text = getContext().getString(R.string.gps_snr_column_label) + " = " + mSnrThresholds[mSnrThresholds.length - 1] + " " + getContext().getString(R.string.unit_db);
            c.drawText(String.valueOf(text), x + length - mSnrValuePaint.measureText(text), y, mSnrValuePaint);

            result = (int) ((y + mSnrValuePaint.getTextSize()) - (centerY + radius + 4 * SAT_RADIUS));
        }
        return result;
    }

        private float elevationToRadius(int s, float elev) {
            return ((s / 2) - SAT_RADIUS) * (1.0f - (elev / 90.0f));
        }

        private void drawTriangle(Canvas c, float x, float y, Paint fillPaint) {
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
            c.drawPath(path, mSatelliteStrokePaint);
        }

        private void drawPentagon(Canvas c, float x, float y, Paint fillPaint) {
            Path path = new Path();
            path.moveTo(x, y - SAT_RADIUS);
            path.lineTo(x - SAT_RADIUS, y - (SAT_RADIUS / 3));
            path.lineTo(x - 2 * (SAT_RADIUS / 3), y + SAT_RADIUS);
            path.lineTo(x + 2 * (SAT_RADIUS / 3), y + SAT_RADIUS);
            path.lineTo(x + SAT_RADIUS, y - (SAT_RADIUS / 3));
            path.close();

            c.drawPath(path, fillPaint);
            c.drawPath(path, mSatelliteStrokePaint);
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
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) {
        } else {
            int dy = drawDisplayHelp(canvas, width, height);
            canvas.save();
            canvas.translate(0, -dy / 2);

            drawHorizon(canvas, width, height);

            drawNorthIndicator(canvas, width, height);

            if (mSnrs != null && mPrns != null && mElevs != null && mAzims != null) {
                int numSats = mSvCount;

                for (int i = 0; i < numSats; i++) {
                    if (/*mSnrs[i] > 0.0f && */((mElevs[i] > 0.0f && mElevs[i] < 90.0f) && (mAzims[i] > 0.0f && mAzims[i] < 360.0f))) {
                        drawSatellite(canvas, width, height, mElevs[i], mAzims[i], mSnrs[i],
                                mPrns[i]);
                    }
                }
            }

            canvas.restore();
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
