package com.android.gpstest.view;

import static com.android.gpstest.model.SatelliteStatus.NO_DATA;
import static java.util.Collections.emptyList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.android.gpstest.Application;
import com.android.gpstest.R;
import com.android.gpstest.model.GnssType;
import com.android.gpstest.model.SatelliteStatus;
import com.android.gpstest.util.UIUtils;

import java.util.List;

/**
* View that shows satellite positions on a circle representing the sky
*/

public class GpsSkyView extends View {

    public static final float MIN_VALUE_CN0 = 10.0f;
    public static final float MAX_VALUE_CN0 = 45.0f;

    // View dimensions, to draw the compass with the correct width and height
    private static int mHeight;

    private static int mWidth;

    private static final float PRN_TEXT_SCALE = 0.7f;

    private static int SAT_RADIUS;

    private float[] mCn0Thresholds;

    private int[] mCn0Colors;

    Context mContext;

    WindowManager mWindowManager;

    private Paint mHorizonActiveFillPaint, mHorizonInactiveFillPaint, mHorizonStrokePaint,
            mGridStrokePaint, mSatelliteFillPaint, mSatelliteStrokePaint, mSatelliteUsedStrokePaint,
            mNorthPaint, mNorthFillPaint, mPrnIdPaint, mNotInViewPaint;

    private double mOrientation = 0.0;

    private boolean mStarted;

    private float mCn0UsedAvg = 0.0f;

    private float mCn0InViewAvg = 0.0f;

    private List<SatelliteStatus> statuses = emptyList();

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

        int textColor;
        int backgroundColor;
        int satStrokeColorUsed;
        if (Application.Companion.getPrefs().getBoolean(mContext.getString(R.string.pref_key_dark_theme), false)) {
            // Dark theme
            textColor = getResources().getColor(android.R.color.secondary_text_dark);
            backgroundColor = ContextCompat.getColor(context, R.color.navdrawer_background_dark);
            satStrokeColorUsed = getResources().getColor(android.R.color.darker_gray);
        } else {
            // Light theme
            textColor = getResources().getColor(R.color.body_text_2_light);
            backgroundColor = Color.WHITE;
            satStrokeColorUsed = Color.BLACK;
        }

        mHorizonActiveFillPaint = new Paint();
        mHorizonActiveFillPaint.setColor(backgroundColor);
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
        mSatelliteUsedStrokePaint.setColor(satStrokeColorUsed);
        mSatelliteUsedStrokePaint.setStyle(Paint.Style.STROKE);
        mSatelliteUsedStrokePaint.setStrokeWidth(8.0f);
        mSatelliteUsedStrokePaint.setAntiAlias(true);

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
        mPrnIdPaint.setColor(textColor);
        mPrnIdPaint.setStyle(Paint.Style.FILL_AND_STROKE);
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
                () -> {
                    mHeight = getHeight();
                    mWidth = getWidth();
                    return true;
                }
        );
    }

    public void setStarted() {
        mStarted = true;
        invalidate();
    }

    public void setStopped() {
        mStarted = false;
        invalidate();
    }

    public synchronized void setStatus(List<SatelliteStatus> statuses) {
        this.statuses = statuses;

        int svInViewCount = 0;
        int svUsedCount = 0;
        float cn0InViewSum = 0.0f;
        float cn0UsedSum = 0.0f;
        mCn0InViewAvg = 0.0f;
        mCn0UsedAvg = 0.0f;
        for (SatelliteStatus s : statuses) {
            // If satellite is in view, add signal to calculate avg
            if (s.getCn0DbHz() != 0.0f) {
                svInViewCount++;
                cn0InViewSum = cn0InViewSum + s.getCn0DbHz();
            }
            if (s.getUsedInFix()) {
                svUsedCount++;
                cn0UsedSum = cn0UsedSum + s.getCn0DbHz();
            }
        }

        if (svInViewCount > 0) {
            mCn0InViewAvg = cn0InViewSum / svInViewCount;
        }
        if (svUsedCount > 0) {
            mCn0UsedAvg = cn0UsedSum / svUsedCount;
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

    private void drawSatellite(Canvas c, int s, float elev, float azim, float cn0, int prn,
            GnssType gnssType, boolean usedInFix) {
        double radius, angle;
        float x, y;
        // Place PRN text slightly below drawn satellite
        final double PRN_X_SCALE = 1.4;
        final double PRN_Y_SCALE = 3.8;

        Paint fillPaint;
        if (cn0 == 0.0f) {
            // Satellite can't be seen
            fillPaint = mNotInViewPaint;
        } else {
            // Calculate fill color based on signal strength
            fillPaint = getSatellitePaint(mSatelliteFillPaint, cn0);
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

        // Change shape based on satellite gnssType
        switch (gnssType) {
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

    private Paint getSatellitePaint(Paint base, float cn0) {
        Paint newPaint = new Paint(base);
        newPaint.setColor(getSatelliteColor(cn0));
        return newPaint;
    }

    /**
     * Gets the paint color for a satellite based on provided C/N0 and the thresholds defined in this class
     *
     * @param cn0 the C/N0 to use to generate the satellite color based on signal quality
     * @return the paint color for a satellite based on provided C/N0
     */
    public synchronized int getSatelliteColor(float cn0) {
        int numSteps;
        final float[] thresholds;
        final int[] colors;

        numSteps = mCn0Thresholds.length;
        thresholds = mCn0Thresholds;
        colors = mCn0Colors;

        if (cn0 <= thresholds[0]) {
            return colors[0];
        }

        if (cn0 >= thresholds[numSteps - 1]) {
            return colors[numSteps - 1];
        }

        for (int i = 0; i < numSteps - 1; i++) {
            float threshold = thresholds[i];
            float nextThreshold = thresholds[i + 1];
            if (cn0 >= threshold && cn0 <= nextThreshold) {
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

                f = (cn0 - threshold) / (nextThreshold - threshold);

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

        minScreenDimen = Math.min(mWidth, mHeight);

        drawHorizon(canvas, minScreenDimen);

        drawNorthIndicator(canvas, minScreenDimen);

        for (SatelliteStatus s : statuses) {
            if (s.getElevationDegrees() != NO_DATA && s.getAzimuthDegrees() != NO_DATA) {
                drawSatellite(canvas, minScreenDimen,
                        s.getElevationDegrees(),
                        s.getAzimuthDegrees(),
                        s.getCn0DbHz(),
                        s.getSvid(),
                        s.getGnssType(),
                        s.getUsedInFix());
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

    public void onOrientationChanged(double orientation, double tilt) {
        mOrientation = orientation;
        invalidate();
    }

    /**
     * Returns the average signal strength (C/N0) for satellites that are in view of the device (i.e., value is not 0), or 0 if the average can't be calculated
     * @return the average signal strength (C/N0) for satellites that are in view of the device (i.e., value is not 0), or 0 if the average can't be calculated
     */
    public synchronized float getCn0InViewAvg() {
        return mCn0InViewAvg;
    }

    /**
     * Returns the average signal strength (C/N0) for satellites that are being used to calculate a location fix, or 0 if the average can't be calculated
     * @return the average signal strength (C/N0) for satellites that are being used to calculate a location fix, or 0 if the average can't be calculated
     */
    public synchronized float getCn0UsedAvg() {
        return mCn0UsedAvg;
    }
}
