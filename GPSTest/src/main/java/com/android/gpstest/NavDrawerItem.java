package com.android.gpstest;

import android.support.annotation.DrawableRes;

/**
 * Holds the string and mImage resource ID for the navigation drawer items
 */

public class NavDrawerItem {
    @DrawableRes int mImage;
    String mTitle;

    public NavDrawerItem(@DrawableRes int image, String title) {
        mImage = image;
        mTitle = title;
    }

    /**
     * Returns the image resource ID for the nav drawer item
     * @return the image resource ID for the nav drawer item
     */
    @DrawableRes
    public int getImage() {
        return mImage;
    }

    /**
     * Sets the image resource ID for the nav drawer item
     * @param image the image resource ID for the nav drawer item
     */
    public void setImage(@DrawableRes int image) {
        mImage = image;
    }

    /**
     * Returns the title of the nav drawer item
     * @return the title of the nav drawer item
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Sets the title of the nav drawer item
     * @param title the title of the nav drawer item
     */
    public void setTitle(String title) {
        mTitle = title;
    }
}
