package com.android.gpstest.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.location.GnssMeasurementsEvent;
import android.location.GnssStatus;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.android.gpstest.GpsTestListener;
import com.android.gpstest.R;
import com.android.gpstest.model.GnssType;
import com.android.gpstest.util.MathUtils;
import com.android.gpstest.util.SatelliteUtils;
import com.android.gpstest.util.UIUtils;

import java.util.Iterator;

/**
* View that shows satellite positions on a circle representing the sky
*/

public class GpsSkyView extends View implements GpsTestListener {

    public static final float MIN_VALUE_CN0 = 10.0f;
    public static final float MAX_VALUE_CN0 = 45.0f;
    public static final float MIN_VALUE_SNR = 0.0f;
    public static final float MAX_VALUE_SNR = 30.0f;

    // View dimensions, to draw the compass with the correct width and height
    private static int mHeight;

    private static int mWidth;

    private static final float PRN_TEXT_SCALE = 0.7f;

    private static int SAT_RADIUS;

    private float mSnrThresholds[];

    private int mSnrColors[];

    private float mCn0Thresholds[];

    private int mCn0Colors[];

    Context mContext;

    WindowManager mWindowManager;

    private Paint mHorizonActiveFillPaint, mHorizonInactiveFillPaint, mHorizonStrokePaint,
            mGridStrokePaint, mSatelliteFillPaint, mSatelliteStrokePaint, mSatelliteUsedStrokePaint,
            mNorthPaint, mNorthFillPaint, mPrnIdPaint, mNotInViewPaint;

    private double mOrientation = 0.0;

    private boolean mStarted;

    private float mSnrCn0s[], mElevs[], mAzims[];  // Holds either SNR or C/N0 - see #65

    private float mSnrCn0UsedAvg = 0.0f;

    private float mSnrCn0InViewAvg = 0.0f;

    private boolean mHasEphemeris[], mHasAlmanac[], mUsedInFix[];

    private int mPrns[], mConstellationType[];

    private int mSvCount;

    private boolean mUseLegacyGnssApi = false;

    private boolean mIsSnrBad = false;

    public GpsSkyView(Context context) {
        super(context);
        init(context);
    }

    public GpsSkyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        SAT_RADIUS = UIUtils.dpToPixels(context, 5);

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
        mGridStrokePaint.setColor(ContextCompat.getColor(mContext, R.color.gray));
        mGridStrokePaint.setStyle(Paint.Style.STROKE);
        mGridStrokePaint.setAntiAlias(true);

        mSatelliteFillPaint = new Paint();
        mSatelliteFillPaint.setColor(ContextCompat.getColor(mContext, R.color.yellow));
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

        mSnrThresholds = new float[]{MIN_VALUE_SNR, 10.0f, 20.0f, MAX_VALUE_SNR};
        mSnrColors = new int[]{ContextCompat.getColor(mContext, R.color.gray),
                ContextCompat.getColor(mContext, R.color.red),
                ContextCompat.getColor(mContext, R.color.yellow),
                ContextCompat.getColor(mContext, R.color.green)};

        mCn0Thresholds = new float[]{MIN_VALUE_CN0, 21.67f, 33.3f, MAX_VALUE_CN0};
        mCn0Colors = new int[]{ContextCompat.getColor(mContext, R.color.gray),
                ContextCompat.getColor(mContext, R.color.red),
                ContextCompat.getColor(mContext, R.color.yellow),
                ContextCompat.getColor(mContext, R.color.green)};

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
                .setTextSize(UIUtils.dpToPixels(getContext(), SAT_RADIUS * PRN_TEXT_SCALE));
        mPrnIdPaint.setAntiAlias(true);

        mNotInViewPaint = new Paint();
        mNotInViewPaint.setColor(ContextCompat.getColor(context, R.color.not_in_view_sat));
        mNotInViewPaint.setStyle(Paint.Style.FILL);
        mNotInViewPaint.setStrokeWidth(4.0f);
        mNotInViewPaint.setAntiAlias(true);

        setFocusable(true);

        // Get the proper height and width of view before drawing
        getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        mHeight = getHeight();
                        mWidth = getWidth();
                        return true;
                    }
                }
        );
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
    public synchronized void setGnssStatus(GnssStatus status) {
        mUseLegacyGnssApi = false;
        mIsSnrBad = false;
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
        int svInViewCount = 0;
        int svUsedCount = 0;
        float cn0InViewSum = 0.0f;
        float cn0UsedSum = 0.0f;
        mSnrCn0InViewAvg = 0.0f;
        mSnrCn0UsedAvg = 0.0f;
        while (mSvCount < length) {
            mSnrCn0s[mSvCount] = status.getCn0DbHz(mSvCount);  // Store C/N0 values (see #65)
            mElevs[mSvCount] = status.getElevationDegrees(mSvCount);
            mAzims[mSvCount] = status.getAzimuthDegrees(mSvCount);
            mPrns[mSvCount] = status.getSvid(mSvCount);
            mConstellationType[mSvCount] = status.getConstellationType(mSvCount);
            mHasEphemeris[mSvCount] = status.hasEphemerisData(mSvCount);
            mHasAlmanac[mSvCount] = status.hasAlmanacData(mSvCount);
            mUsedInFix[mSvCount] = status.usedInFix(mSvCount);
            // If satellite is in view, add signal to calculate avg
            if (status.getCn0DbHz(mSvCount) != 0.0f) {
                svInViewCount++;
                cn0InViewSum = cn0InViewSum + status.getCn0DbHz(mSvCount);
            }
            if (status.usedInFix(mSvCount)) {
                svUsedCount++;
                cn0UsedSum = cn0UsedSum + status.getCn0DbHz(mSvCount);
            }
            mSvCount++;
        }

        if (svInViewCount > 0) {
            mSnrCn0InViewAvg = cn0InViewSum / svInViewCount;
        }
        if (svUsedCount > 0) {
            mSnrCn0UsedAvg = cn0UsedSum / svUsedCount;
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
        mUseLegacyGnssApi = true;
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
        int svInViewCount = 0;
        int svUsedCount = 0;
        float snrInViewSum = 0.0f;
        float snrUsedSum = 0.0f;
        mSnrCn0InViewAvg = 0.0f;
        mSnrCn0UsedAvg = 0.0f;
        while (satellites.hasNext()) {
            GpsSatellite satellite = satellites.next();
            mSnrCn0s[mSvCount] = satellite.getSnr(); // Store SNR values (see #65)
            mElevs[mSvCount] = satellite.getElevation();
            mAzims[mSvCount] = satellite.getAzimuth();
            mPrns[mSvCount] = satellite.getPrn();
            mHasEphemeris[mSvCount] = satellite.hasEphemeris();
            mHasAlmanac[mSvCount] = satellite.hasAlmanac();
            mUsedInFix[mSvCount] = satellite.usedInFix();
            // If satellite is in view, add signal to calculate avg
            if (satellite.getSnr() != 0.0f) {
                svInViewCount++;
                snrInViewSum = snrInViewSum + satellite.getSnr();
            }
            if (satellite.usedInFix()) {
                svUsedCount++;
                snrUsedSum = snrUsedSum + satellite.getSnr();
            }
            mSvCount++;
        }

        if (svInViewCount > 0) {
            mSnrCn0InViewAvg = snrInViewSum / svInViewCount;
        }
        if (svUsedCount > 0) {
            mSnrCn0UsedAvg = snrUsedSum / svUsedCount;
        }

        checkBadSnr();

        mStarted = true;
        invalidate();
    }

    /**
     * Check if the SNR values are bad (see #153)
     */
    private void checkBadSnr() {
        if (mUseLegacyGnssApi) {
            // If either of the avg SNR values are greater than the max SNR value, mark the data as suspect
            if ((MathUtils.isValidFloat(mSnrCn0InViewAvg) && mSnrCn0InViewAvg > GpsSkyView.MAX_VALUE_SNR) ||
                    (MathUtils.isValidFloat(mSnrCn0UsedAvg) && mSnrCn0UsedAvg > GpsSkyView.MAX_VALUE_SNR)) {
                mIsSnrBad = true;
            }
        }
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
        if (SatelliteUtils.isGnssStatusListenerSupported() && !mUseLegacyGnssApi) {
            operator = SatelliteUtils.getGnssConstellationType(constellationType);
        } else {
            operator = SatelliteUtils.getGnssType(prn);
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
                drawHexagon(c, x, y, fillPaint, strokePaint);
                break;
            case BEIDOU:
                drawPentagon(c, x, y, fillPaint, strokePaint);
                break;
            case GALILEO:
                drawTriangle(c, x, y, fillPaint, strokePaint);
                break;
            case IRNSS:
                drawOval(c, x, y, fillPaint, strokePaint);
                break;
            case SBAS:
                drawDiamond(c, x, y, fillPaint, strokePaint);
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

    private void drawDiamond(Canvas c, float x, float y, Paint fillPaint, Paint strokePaint) {
        Path path = new Path();
        path.moveTo(x, y - SAT_RADIUS);
        path.lineTo(x - SAT_RADIUS * 1.5f, y);
        path.lineTo(x, y + SAT_RADIUS);
        path.lineTo(x + SAT_RADIUS * 1.5f, y);
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

    private void drawHexagon(Canvas c, float x, float y, Paint fillPaint, Paint strokePaint) {
        final float MULTIPLIER = 0.6f;
        final float SIDE_MULTIPLIER = 1.4f;
        Path path = new Path();
        // Top-left
        path.moveTo(x - SAT_RADIUS * MULTIPLIER, y - SAT_RADIUS);
        // Left
        path.lineTo(x - SAT_RADIUS * SIDE_MULTIPLIER, y);
        // Bottom
        path.lineTo(x - SAT_RADIUS * MULTIPLIER, y + SAT_RADIUS);
        path.lineTo(x + SAT_RADIUS * MULTIPLIER, y + SAT_RADIUS);
        // Right
        path.lineTo(x + SAT_RADIUS * SIDE_MULTIPLIER, y);
        // Top-right
        path.lineTo(x + SAT_RADIUS * MULTIPLIER, y - SAT_RADIUS);
        path.close();

        c.drawPath(path, fillPaint);
        c.drawPath(path, strokePaint);
    }

    private void drawOval(Canvas c, float x, float y, Paint fillPaint, Paint strokePaint) {
        RectF rect = new RectF(x - SAT_RADIUS * 1.5f, y - SAT_RADIUS, x + SAT_RADIUS * 1.5f, y + SAT_RADIUS);

        c.drawOval(rect, fillPaint);
        c.drawOval(rect, strokePaint);
    }

    private Paint getSatellitePaint(Paint base, float snrCn0) {
        Paint newPaint;
        newPaint = new Paint(base);
        newPaint.setColor(getSatelliteColor(snrCn0));
        return newPaint;
    }

    /**
     * Gets the paint color for a satellite based on provided SNR or C/N0 and the thresholds defined in this class
     *
     * @param snrCn0 the SNR to use (if using legacy GpsStatus) or the C/N0 to use (if using is
     *               GnssStatus) to generate the satellite color based on signal quality
     * @return the paint color for a satellite based on provided SNR or C/N0
     */
    public synchronized int getSatelliteColor(float snrCn0) {
        int numSteps;
        final float thresholds[];
        final int colors[];

        if (!mUseLegacyGnssApi || mIsSnrBad) {
            // Use C/N0 ranges/colors for both C/N0 and SNR on Android 7.0 and higher (see #76)
            numSteps = mCn0Thresholds.length;
            thresholds = mCn0Thresholds;
            colors = mCn0Colors;
        } else {
            // Use legacy SNR ranges/colors for Android versions less than Android 7.0 or if user selects legacy API (see #76)
            numSteps = mSnrThresholds.length;
            thresholds = mSnrThresholds;
            colors = mSnrColors;
        }

        if (snrCn0 <= thresholds[0]) {
            return colors[0];
        }

        if (snrCn0 >= thresholds[numSteps - 1]) {
            return colors[numSteps - 1];
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

                return c3;
            }
        }
        return Color.MAGENTA;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int minScreenDimen;

        minScreenDimen = (mWidth < mHeight) ? mWidth : mHeight;

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
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Use the width of the screen as the measured dimension for width and height of view
        // This allows other views in the same layout to be visible on the screen (#124)
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(specSize, specSize);
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

    /**
     * Returns the average signal strength (C/N0 if isUsingLegacyGpsApi is false, SNR if isUsingLegacyGpsApi is true) for satellites that are in view of the device (i.e., value is not 0), or 0 if the average can't be calculated
     * @return the average signal strength (C/N0 if isUsingLegacyGpsApi is false, SNR if isUsingLegacyGpsApi is true) for satellites that are in view of the device (i.e., value is not 0), or 0 if the average can't be calculated
     */
    public synchronized float getSnrCn0InViewAvg() {
        return mSnrCn0InViewAvg;
    }

    /**
     * Returns the average signal strength (C/N0 if isUsingLegacyGpsApi is false, SNR if isUsingLegacyGpsApi is true) for satellites that are being used to calculate a location fix, or 0 if the average can't be calculated
     * @return the average signal strength (C/N0 if isUsingLegacyGpsApi is false, SNR if isUsingLegacyGpsApi is true) for satellites that are being used to calculate a location fix, or 0 if the average can't be calculated
     */
    public synchronized float getSnrCn0UsedAvg() {
        return mSnrCn0UsedAvg;
    }

    /**
     * Returns true if the app is monitoring the legacy GpsStatus.Listener, or false if the app is monitoring the GnssStatus.Callback
     * @return true if the app is monitoring the legacy GpsStatus.Listener, or false if the app is monitoring the GnssStatus.Callback
     */
    public synchronized boolean isUsingLegacyGpsApi() {
        return mUseLegacyGnssApi;
    }

    /**
     * Returns true if bad SNR data has been detected (avgs exceeded max SNR threshold), or false if no SNR is observed (i.e., C/N0 data is observed) or SNR data seems ok
     * @return true if bad SNR data has been detected (avgs exceeded max SNR threshold), or false if no SNR is observed (i.e., C/N0 data is observed) or SNR data seems ok
     */
    public synchronized boolean isSnrBad() {
        return mIsSnrBad;
    }
}