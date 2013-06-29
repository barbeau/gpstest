package com.android.gpstest;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

import com.android.gpstest.GpsTestActivity.SectionsPagerAdapter;

/**
 * Extension of ViewPager that prevents swiping on the GpsMapFragment,
 * since swiping blocks the user from properly dragging the map view
 */
public class ViewPagerMapNoScroll extends ViewPager {
	
	public ViewPagerMapNoScroll(Context context) {
		super(context);
	}
	
	public ViewPagerMapNoScroll(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (v instanceof ViewPagerMapNoScroll) {	                        
			SectionsPagerAdapter a = (SectionsPagerAdapter) ((ViewPagerMapNoScroll)v).getAdapter();
			if (a.disableSwipe)
				return false;
			else
				return true;
		}			
		return super.canScroll(v, checkV, dx, x, y);
	}		
}