/*
 * Copyright (C) 2018 Sean J. Barbeau
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

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * An adapter for the navigation drawer elements
 */

public class NavDrawerAdapter extends ArrayAdapter<NavDrawerItem> {
    Context mContext;
    private List<NavDrawerItem> mItems;

    public NavDrawerAdapter(@NonNull Context context, List<NavDrawerItem> items) {
        super(context, R.layout.nav_drawer_item);
        mContext = context;
        mItems = items;
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public NavDrawerItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mItems.indexOf(getItem(position));
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater mInflater = (LayoutInflater) mContext
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = mInflater.inflate(R.layout.nav_drawer_item, null);
        }

        ImageView imgIcon = convertView.findViewById(R.id.nav_drawer_icon);
        imgIcon.setColorFilter(mContext.getResources().getColor(R.color.navdrawer_icon_tint_selected));

        TextView txtTitle = convertView.findViewById(R.id.nav_drawer_title);

        NavDrawerItem item = mItems.get(position);
        // Set the image resource and title
        imgIcon.setImageResource(item.getImage());
        txtTitle.setText(item.getTitle());

        return convertView;
    }
}
