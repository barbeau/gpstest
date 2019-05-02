// Copyright 2008 Google Inc. From StarDroid.
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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.LinearLayout;

import com.android.gpstest.R;

/**
 * Contains the provider buttons.
 */

public class ButtonLayerView extends LinearLayout {
    // TODO(jontayler): clear up the fade code which is no longer used.
    private int fadeTime = 500;

    public ButtonLayerView(Context context) {
        this(context, null);
    }

    public ButtonLayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(false);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ButtonLayerView);
        fadeTime = a.getResourceId(R.styleable.ButtonLayerView_fade_time, 500);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        /* Consume all touch events so they don't get dispatched to the view
         * beneath this view.
         */
        return true;
    }

    public void show() {
        fade(View.VISIBLE, 0.0f, 1.0f);
    }

    public void hide() {
        fade(View.GONE, 1.0f, 0.0f);
    }

    private void fade(int visibility, float startAlpha, float endAlpha) {
        AlphaAnimation anim = new AlphaAnimation(startAlpha, endAlpha);
        anim.setDuration(fadeTime);
        startAnimation(anim);
        setVisibility(visibility);
    }

    @Override
    public boolean hasFocus() {
        int numChildren = getChildCount();
        boolean hasFocus = false;
        for (int i = 0; i < numChildren; ++i) {
            hasFocus = hasFocus || getChildAt(i).hasFocus();
        }
        return hasFocus;
    }
}
