package com.android.gpstest.chart;

import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;

import java.text.DecimalFormat;

public class DistanceValueFormatter implements IValueFormatter, IAxisValueFormatter {
    private final DecimalFormat mFormat;
    private String mSuffix;

    public DistanceValueFormatter(String suffix) {
        mFormat = new DecimalFormat();
        mFormat.setMaximumFractionDigits(0);
        mSuffix = suffix;
    }

    @Override
    public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
        return mFormat.format(value) + " " + mSuffix;
    }

    @Override
    public String getFormattedValue(float value, AxisBase axis) {
        if (axis instanceof XAxis) {
            return mFormat.format(value);
        } else if (value > 0) {
            return mFormat.format(value) +  " " + mSuffix;
        } else {
            return mFormat.format(value);
        }
    }
}
