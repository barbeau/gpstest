// Copyright 2009 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.android.gpstest.ar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.android.gpstest.R;
import com.android.gpstest.util.MiscUtils;

public class PreferencesButton extends androidx.appcompat.widget.AppCompatImageButton
        implements android.view.View.OnClickListener, OnSharedPreferenceChangeListener {
    private static final String TAG = MiscUtils.getTag(PreferencesButton.class);
    private OnClickListener secondaryOnClickListener;

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.secondaryOnClickListener = l;
    }

    private Drawable imageOn;
    private Drawable imageOff;
    private boolean isOn;
    private String prefKey;
    private SharedPreferences preferences;
    private boolean defaultValue;

    public PreferencesButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setAttrs(context, attrs);
        init();
    }

    public PreferencesButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAttrs(context, attrs);
        init();
    }

    public PreferencesButton(Context context) {
        super(context);
        init();
    }

    public void setAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PreferencesButton);
        imageOn = a.getDrawable(R.styleable.PreferencesButton_image_on);
        imageOff = a.getDrawable(R.styleable.PreferencesButton_image_off);
        prefKey = a.getString(R.styleable.PreferencesButton_pref_key);
        defaultValue = a.getBoolean(R.styleable.PreferencesButton_default_value, true);
        Log.d(TAG, "Preference key is " + prefKey);
    }

    private void init() {
        super.setOnClickListener(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        preferences.registerOnSharedPreferenceChangeListener(this);
        this.isOn = preferences.getBoolean(prefKey, defaultValue);
        Log.d(TAG, "Setting initial value of preference " + prefKey + " to " + isOn);
        setVisuallyOnOrOff();
    }

    private void setVisuallyOnOrOff() {
        setImageDrawable(isOn ? imageOn : imageOff);
    }

    private void setPreference() {
        Log.d(TAG, "Setting preference " + prefKey + " to... " + isOn);
        if (prefKey != null) {
            preferences.edit().putBoolean(prefKey, isOn).apply();
        }
    }

    @Override
    public void onClick(View v) {
        isOn = !isOn;
        setVisuallyOnOrOff();
        setPreference();
        if (secondaryOnClickListener != null) {
            secondaryOnClickListener.onClick(v);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String changedKey) {
        if (changedKey.equals(prefKey)) {
            isOn = sharedPreferences.getBoolean(changedKey, isOn);
            setVisuallyOnOrOff();
        }
    }
}
