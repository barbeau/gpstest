/*
 * Copyright 2014-2018 Google Inc.,
 * University of South Florida (sjbarbeau@gmail.com),
 * Microsoft Corporation,
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
 *
 * Portions of code taken from the Google I/0 2014 (https://github.com/google/iosched)
 * and a generated NavigationDrawer app from Android Studio, modified for OneBusAway by USF,
 * modified for GPSTest by Sean J. Barbeau
 */
package com.android.gpstest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.android.gpstest.util.IOUtils;
import com.android.gpstest.util.UIUtils;
import com.android.gpstest.view.ScrimInsetsScrollView;

import java.util.ArrayList;

/**
 * Fragment used for managing interactions for and presentation of a navigation drawer.
 * See the <a href="https://developer.android.com/design/patterns/navigation-drawer.html#Interaction">
 * design guidelines</a> for a complete explanation of the behaviors implemented here.
 */
public class NavigationDrawerFragment extends Fragment {

    public static final String TAG = "NavDrawerFragment";

    /**
     * Remember the position of the selected item.
     */
    private static final String STATE_SELECTED_POSITION = "selected_navigation_drawer_position";

    // symbols for navdrawer items (indices must correspond to array below). This is
    // not a list of items that are necessarily *present* in the Nav Drawer; rather,
    // it's a list of all possible items.
    protected static final int NAVDRAWER_ITEM_STATUS = 0;

    protected static final int NAVDRAWER_ITEM_MAP = 1;

    protected static final int NAVDRAWER_ITEM_SKY = 2;

    protected static final int NAVDRAWER_ITEM_SETTINGS = 3;

    protected static final int NAVDRAWER_ITEM_HELP = 4;

    protected static final int NAVDRAWER_ITEM_OPEN_SOURCE = 5;

    protected static final int NAVDRAWER_ITEM_INJECT_PSDS_DATA = 6;

    protected static final int NAVDRAWER_ITEM_INJECT_TIME_DATA = 7;

    protected static final int NAVDRAWER_ITEM_CLEAR_AIDING_DATA = 8;

    protected static final int NAVDRAWER_ITEM_SEND_FEEDBACK = 9;

    protected static final int NAVDRAWER_ITEM_ACCURACY = 10;

    protected static final int NAVDRAWER_ITEM_INVALID = -1;

    protected static final int NAVDRAWER_ITEM_SEPARATOR = -2;

    protected static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -3;

    // Currently selected navigation drawer item (must be value of one of the constants above)
    private int mCurrentSelectedPosition = NAVDRAWER_ITEM_STATUS;

    // titles for navdrawer items (indices must correspond to the above)
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.gps_status_title,
            R.string.gps_map_title,
            R.string.gps_sky_title,
            R.string.navdrawer_item_settings,
            R.string.navdrawer_item_help,
            R.string.navdrawer_item_open_source,
            R.string.force_psds_injection,
            R.string.force_time_injection,
            R.string.delete_aiding_data,
            R.string.navdrawer_item_send_feedback,
            R.string.gps_accuracy_title
    };

    // icons for navdrawer items (indices must correspond to above array)
    private static final int[] NAVDRAWER_ICON_RES_ID = new int[]{
            R.drawable.ic_wireless,  // Status
            R.drawable.ic_map, // Map
            R.drawable.ic_sky, // Sky
            0, // Settings
            0, // Help
            R.drawable.ic_drawer_github, // Open-source
            R.drawable.ic_inject_psds, // Inject PSDS data
            R.drawable.ic_inject_time, // Inject time data
            R.drawable.ic_delete, // Clear assist data
            0, // Send feedback
            R.drawable.ic_accuracy // Accuracy
    };

    // Secondary navdrawer item icons that appear align to right of list item layout
    private static final int[] NAVDRAWER_ICON_SECONDARY_RES_ID = new int[]{
            0,  // Status
            0, // Map
            0, // Sky
            0, // Settings
            0, // Help
            R.drawable.ic_drawer_link, // Open-source
            0, // Inject PSDS data
            0, // Inject time data
            0, // Clear assist data
            0,  // Send feedback
            0  // Accuracy
    };

    // list of navdrawer items that were actually added to the navdrawer, in order
    private ArrayList<Integer> mNavDrawerItems = new ArrayList<Integer>();

    // views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationDrawerCallbacks mCallbacks;

    /**
     * Helper component that ties the action bar to the navigation drawer.
     */
    private ActionBarDrawerToggle mDrawerToggle;

    // Navigation drawer:
    private DrawerLayout mDrawerLayout;

    private View mDrawerItemsListContainer;

    private View mFragmentContainerView;

    private boolean isSignedIn;

    public NavigationDrawerFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sp = Application.getPrefs();

        if (IOUtils.isShowRadarIntent(getActivity().getIntent())) {
            // If another app (e.g., BenchMap) passed in a ground truth location, show the Accuracy view
            mCurrentSelectedPosition = NAVDRAWER_ITEM_ACCURACY;
            Log.d(TAG, "Using Accuracy position due to RADAR intent = " + mCurrentSelectedPosition);
        } else if (savedInstanceState != null) {
            mCurrentSelectedPosition = savedInstanceState.getInt(STATE_SELECTED_POSITION);
            Log.d(TAG, "Using position from savedInstanceState = " + mCurrentSelectedPosition);
        } else {
            // Try to get the saved position from preferences
            mCurrentSelectedPosition = sp.getInt(STATE_SELECTED_POSITION, NAVDRAWER_ITEM_STATUS);
            Log.d(TAG, "Using position from preferences = " + mCurrentSelectedPosition);
        }

        // Select either the default item (0) or the last selected item.
        selectItem(mCurrentSelectedPosition);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Indicate that this fragment would like to influence the set of actions in the action bar.
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mDrawerItemsListContainer = inflater
                .inflate(R.layout.navdrawer_list, container, false);

        return mDrawerItemsListContainer;
    }

    /**
     * Users of this fragment must call this method to set up the navigation drawer interactions.
     *
     * @param fragmentId   The android:id of this fragment in its activity's layout.
     * @param drawerLayout The DrawerLayout containing this fragment's UI.
     */
    public void setUp(int fragmentId, DrawerLayout drawerLayout) {
        int selfItem = mCurrentSelectedPosition;
        mFragmentContainerView = getActivity().findViewById(fragmentId);
        mDrawerLayout = drawerLayout;

        if (mDrawerLayout == null) {
            return;
        }

        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        ScrimInsetsScrollView navDrawer = mDrawerLayout.findViewById(R.id.navdrawer);
        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            // do not show a nav drawer
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }

        // populate the nav drawer with the correct items
        populateNavDrawer();

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the navigation drawer and the action bar app icon.
        mDrawerToggle = new androidx.appcompat.app.ActionBarDrawerToggle(
                getActivity(),                    /* host Activity */
                mDrawerLayout,                    /* DrawerLayout object */
                R.string.navigation_drawer_open,  /* "open drawer" description for accessibility */
                R.string.navigation_drawer_close  /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                if (!isAdded()) {
                    return;
                }

                getActivity().supportInvalidateOptionsMenu(); // calls onPrepareOptionsMenu()
            }
        };

        // Defer code dependent on restoration of previous instance state.
        mDrawerLayout.post(new Runnable() {
            @Override
            public void run() {
                mDrawerToggle.syncState();
            }
        });

        mDrawerLayout.setDrawerListener(mDrawerToggle);
    }

    /**
     * Sets the currently selected navigation drawer item, based on the provided position
     * parameter,
     * which must be one of the NAVDRAWER_ITEM_* contants in this class.
     *
     * @param position the item to select in the navigation drawer - must be one of the
     *                 NAVDRAWER_ITEM_* contants in this class
     */
    public void selectItem(int position) {
        setSelectedNavDrawerItem(position);
        if (mDrawerLayout != null && mFragmentContainerView != null) {
            mDrawerLayout.closeDrawer(mFragmentContainerView);
        }
        if (mCallbacks != null) {
            mCallbacks.onNavigationDrawerItemSelected(position);
        }
    }

    /**
     * Set the selected position as a preference
     */
    public void setSavedPosition(int position) {
        SharedPreferences sp = Application.getPrefs();
        sp.edit().putInt(STATE_SELECTED_POSITION, position).apply();
    }

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could
     * also be accomplished (perhaps more cleanly) with state-based layouts.
     */
    private void setSelectedNavDrawerItem(int itemId) {
        if (!isNewActivityItem(itemId)) {
            // We only change the selected item if it doesn't launch a new activity
            mCurrentSelectedPosition = itemId;
            setSavedPosition(mCurrentSelectedPosition);
        }

        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                if (i < mNavDrawerItems.size()) {
                    int thisItemId = mNavDrawerItems.get(i);
                    formatNavDrawerItem(mNavDrawerItemViews[i], thisItemId, itemId == thisItemId);
                }
            }
        }
    }

    @Override
    public void onAttach(final Context context) {
        super.onAttach(context);
        try {
            mCallbacks = (NavigationDrawerCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationDrawerCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "Saving position = " + mCurrentSelectedPosition);
        outState.putInt(STATE_SELECTED_POSITION, mCurrentSelectedPosition);
    }

    @Override
    public void onResume() {
        super.onResume();
        populateNavDrawer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Forward the new configuration the drawer toggle component.
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle != null && mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private ActionBar getActionBar() {
        return ((AppCompatActivity) getActivity()).getSupportActionBar();
    }

    /**
     * Callbacks interface that all activities using this fragment must implement.
     */
    public interface NavigationDrawerCallbacks {

        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationDrawerItemSelected(int position);
    }


    /**
     * Populates the navigation drawer with the appropriate items.  This method defines the visual
     * order of the entries in the nav drawer.
     */
    public void populateNavDrawer() {
        mNavDrawerItems.clear();

        mNavDrawerItems.add(NAVDRAWER_ITEM_STATUS);
        mNavDrawerItems.add(NAVDRAWER_ITEM_MAP);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SKY);
        mNavDrawerItems.add(NAVDRAWER_ITEM_ACCURACY);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR_SPECIAL);

        mNavDrawerItems.add(NAVDRAWER_ITEM_INJECT_PSDS_DATA);
        mNavDrawerItems.add(NAVDRAWER_ITEM_INJECT_TIME_DATA);
        mNavDrawerItems.add(NAVDRAWER_ITEM_CLEAR_AIDING_DATA);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR);

        mNavDrawerItems.add(NAVDRAWER_ITEM_OPEN_SOURCE);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR);

        mNavDrawerItems.add(NAVDRAWER_ITEM_SETTINGS);
        mNavDrawerItems.add(NAVDRAWER_ITEM_HELP);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SEND_FEEDBACK);

        createNavDrawerItems();
    }

    private void createNavDrawerItems() {
        if (mDrawerItemsListContainer == null || getActivity() == null) {
            return;
        }

        // Set background color of nav drawer
        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            mDrawerItemsListContainer.setBackgroundColor(getContext().getResources().getColor(R.color.navdrawer_background_dark));
        }

        // Populate views
        mNavDrawerItemViews = new View[mNavDrawerItems.size()];
        int i = 0;

        LinearLayout listLayout = mDrawerItemsListContainer.findViewById(R.id.navdrawer_items_list);
        listLayout.removeAllViews();

        for (int itemId : mNavDrawerItems) {
            mNavDrawerItemViews[i] = makeNavDrawerItem(itemId, listLayout);
            listLayout.addView(mNavDrawerItemViews[i]);
            ++i;
        }
    }

    private View makeNavDrawerItem(final int itemId, ViewGroup container) {
        boolean selected = mCurrentSelectedPosition == itemId;
        int layoutToInflate;
        if (itemId == NAVDRAWER_ITEM_SEPARATOR) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else if (itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL) {
            layoutToInflate = R.layout.navdrawer_separator_special;
        } else {
            layoutToInflate = R.layout.navdrawer_item;
        }
        View view = getActivity().getLayoutInflater().inflate(layoutToInflate, container, false);

        if (isSeparator(itemId)) {
            // we are done
            UIUtils.setAccessibilityIgnore(view);
            return view;
        }

        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)
                && layoutToInflate == R.layout.navdrawer_item) {
            // Dark theme
            view.setBackgroundResource(R.drawable.navdrawer_item_selectable_dark);
        }

        ImageView iconView = view.findViewById(R.id.icon);
        TextView titleView = view.findViewById(R.id.title);
        ImageView secondaryIconView = view.findViewById(R.id.secondary_icon);
        int iconId = itemId >= 0 && itemId < NAVDRAWER_ICON_RES_ID.length ?
                NAVDRAWER_ICON_RES_ID[itemId] : 0;
        int titleId = itemId >= 0 && itemId < NAVDRAWER_TITLE_RES_ID.length ?
                NAVDRAWER_TITLE_RES_ID[itemId] : 0;
        int secondaryIconId = itemId >= 0 && itemId < NAVDRAWER_ICON_SECONDARY_RES_ID.length ?
                NAVDRAWER_ICON_SECONDARY_RES_ID[itemId] : 0;

        // set icon and text
        iconView.setVisibility(iconId > 0 ? View.VISIBLE : View.GONE);
        if (iconId > 0) {
            iconView.setImageResource(iconId);
        }
        titleView.setText(getString(titleId));

        // Secondary icon
        secondaryIconView.setVisibility(secondaryIconId > 0 ? View.VISIBLE : View.GONE);
        if (secondaryIconId > 0) {
            secondaryIconView.setImageResource(secondaryIconId);
        }

        formatNavDrawerItem(view, itemId, selected);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectItem(itemId);
            }
        });

        return view;
    }

    private void formatNavDrawerItem(View view, int itemId, boolean selected) {
        if (isSeparator(itemId)) {
            // Don't do any formatting
            return;
        }

        ImageView iconView = view.findViewById(R.id.icon);
        TextView titleView = view.findViewById(R.id.title);
        ImageView secondaryIconView = view.findViewById(R.id.secondary_icon);

        /**
         * Configure its appearance according to whether or not it's selected.  Certain items
         * (e.g., Settings) don't get formatted upon selection, since they open a new activity.
         */
        if (selected) {
            if (isNewActivityItem(itemId)) {
                // Don't change any formatting, since this is a category that launches a new activity
                return;
            } else {
                // Show the category as highlighted by changing background, text, and icon color
                view.setSelected(true);
                titleView.setTextColor(
                        getResources().getColor(R.color.navdrawer_text_color_selected));
                iconView.setColorFilter(
                        getResources().getColor(R.color.navdrawer_icon_tint_selected));
                secondaryIconView.setColorFilter(
                        getResources().getColor(R.color.navdrawer_icon_tint_selected));
            }
        } else {
            // Show the category as not highlighted, if its not currently selected
            if (itemId != mCurrentSelectedPosition) {
                view.setSelected(false);
                if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
                    // Dark theme
                    titleView.setTextColor(getResources().getColor(R.color.navdrawer_text_color_dark));
                    iconView.setColorFilter(getResources().getColor(R.color.navdrawer_icon_tint_dark));
                    secondaryIconView.setColorFilter(getResources().getColor(R.color.navdrawer_icon_tint_dark));
                } else {
                    // Light theme
                    titleView.setTextColor(getResources().getColor(R.color.navdrawer_text_color_light));
                    iconView.setColorFilter(getResources().getColor(R.color.navdrawer_icon_tint_light));
                    secondaryIconView.setColorFilter(getResources().getColor(R.color.navdrawer_icon_tint_light));
                }
            }
        }
    }

    private boolean isSeparator(int itemId) {
        return itemId == NAVDRAWER_ITEM_SEPARATOR || itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL;
    }

    /**
     * Returns true if this is an item that should not allow selection (e.g., Settings),
     * because they launch a new Activity and aren't part of this screen, false if its selectable
     * and changes the current UI via a new fragment
     *
     * @return true if this is an item that should not allow selection (e.g., Settings),
     * because they launch a new Activity and aren't part of this screen, false if its selectable
     * and changes the current UI via a new fragment
     */
    private boolean isNewActivityItem(int itemId) {
        return itemId == NAVDRAWER_ITEM_SETTINGS ||
                itemId == NAVDRAWER_ITEM_HELP ||
                itemId == NAVDRAWER_ITEM_OPEN_SOURCE ||
                itemId == NAVDRAWER_ITEM_INJECT_PSDS_DATA ||
                itemId == NAVDRAWER_ITEM_INJECT_TIME_DATA ||
                itemId == NAVDRAWER_ITEM_CLEAR_AIDING_DATA ||
                itemId == NAVDRAWER_ITEM_SEND_FEEDBACK;
    }
}
