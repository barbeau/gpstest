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
package com.android.gpstest

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.location.GnssStatus
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.android.gpstest.data.FirstFixState
import com.android.gpstest.data.FixState
import com.android.gpstest.data.LocationRepository
import com.android.gpstest.util.MathUtils
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.UIUtils
import com.android.gpstest.view.GpsSkyView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class GpsSkyFragment : Fragment() {
    private var skyView: GpsSkyView? = null
    private var legendLines: MutableList<View>? = null
    private var legendShapes: MutableList<ImageView>? = null
    private var legendCn0Title: TextView? = null
    private var legendCn0Units: TextView? = null
    private var legendCn0LeftText: TextView? = null
    private var legendCn0LeftCenterText: TextView? = null
    private var legendCn0CenterText: TextView? = null
    private var legendCn0RightCenterText: TextView? = null
    private var legendCn0RightText: TextView? = null
    private var cn0InViewAvgText: TextView? = null
    private var cn0UsedAvgText: TextView? = null
    private var cn0InViewAvg: ImageView? = null
    private var cn0UsedAvg: ImageView? = null
    private var lock: ImageView? = null
    private var circleUsedInFix: ImageView? = null
    var cn0InViewAvgAnimation: Animation? = null
    var cn0UsedAvgAnimation: Animation? = null
    var cn0InViewAvgAnimationTextView: Animation? = null
    var cn0UsedAvgAnimationTextView: Animation? = null

    // Default light theme values
    var usedCn0Background = R.drawable.cn0_round_corner_background_used
    var usedCn0IndicatorColor = Color.BLACK

    // Repository of location data that the service will observe, injected via Hilt
    @Inject
    lateinit var repository: LocationRepository

    // Get a reference to the Job from the Flow so we can stop it from UI events
    private var gnssFlow: Job? = null
    private var sensorFlow: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.gps_sky, container, false)
        skyView = v.findViewById(R.id.sky_view)
        initLegendViews(v)
        cn0InViewAvg = v.findViewById(R.id.cn0_indicator_in_view)
        cn0UsedAvg = v.findViewById(R.id.cn0_indicator_used)
        lock = v.findViewById(R.id.sky_lock)
        circleUsedInFix = v.findViewById(R.id.sky_legend_used_in_fix)

        observeLocationUpdateStates()

        return v
    }

    override fun onResume() {
        super.onResume()
        val color: Int
        if (Application.getPrefs().getBoolean(getString(R.string.pref_key_dark_theme), false)) {
            // Dark theme
            color = resources.getColor(android.R.color.secondary_text_dark)
            circleUsedInFix!!.setImageResource(R.drawable.circle_used_in_fix_dark)
            usedCn0Background = R.drawable.cn0_round_corner_background_used_dark
            usedCn0IndicatorColor = resources.getColor(android.R.color.darker_gray)
        } else {
            // Light theme
            color = resources.getColor(R.color.body_text_2_light)
            circleUsedInFix!!.setImageResource(R.drawable.circle_used_in_fix)
            usedCn0Background = R.drawable.cn0_round_corner_background_used
            usedCn0IndicatorColor = Color.BLACK
        }
        for (v in legendLines!!) {
            v.setBackgroundColor(color)
        }
        for (v in legendShapes!!) {
            v.setColorFilter(color)
        }
    }

    @ExperimentalCoroutinesApi
    private fun observeLocationUpdateStates() {
        repository.receivingLocationUpdates
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    true -> onGnssStarted()
                    false -> onGnssStopped()
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeGnssFlow() {
        if (gnssFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        gnssFlow = repository.getGnssStatus()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                Log.d(TAG, "Sky gnssStatus: $it")
                updateGnssStatus(it)
            }
            .launchIn(lifecycleScope)
    }

    private fun observeGnssStates() {
        repository.firstFixState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    is FirstFixState.Acquired -> {
                        onGnssFixAcquired()
                    }
                    is FirstFixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted()) onGnssFixLost()
                }
            }
            .launchIn(lifecycleScope)
        repository.fixState
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                when (it) {
                    is FixState.Acquired -> onGnssFixAcquired()
                    is FixState.NotAcquired -> if (PreferenceUtils.isTrackingStarted()) onGnssFixLost()
                }
            }
            .launchIn(lifecycleScope)
    }

    @ExperimentalCoroutinesApi
    private fun observeSensorFlow() {
        if (sensorFlow?.isActive == true) {
            // If we're already observing updates, don't register again
            return
        }
        // Observe locations via Flow as they are generated by the repository
        sensorFlow = repository.getSensorUpdates()
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                Log.d(TAG, "Sky sensor: orientation ${it.orientation}, tilt ${it.tilt}")
                onOrientationChanged(it.orientation, it.tilt)
            }
            .launchIn(lifecycleScope)
    }

    private fun onGnssFixAcquired() {
        showHaveFix()
    }

    private fun onGnssFixLost() {
        showLostFix()
    }

    private fun updateGnssStatus(status: GnssStatus) {
        skyView!!.setGnssStatus(status)
        updatecn0AvgMeterText()
        updateCn0Avgs()
    }

    @ExperimentalCoroutinesApi
    private fun onGnssStarted() {
        skyView!!.setStarted()
        // Activity or service is observing updates, so observe here too
        observeGnssFlow()
        observeGnssStates()
        observeSensorFlow()
    }

    private fun onGnssStopped() {
        // Cancel updates (Note that these are canceled via GlobalScope in main Activity too,
        // otherwise updates won't stop because this Fragment doesn't get the switch UI event.
        // But cancel() here too for good practice)
        sensorFlow?.cancel()
        gnssFlow?.cancel()

        skyView!!.setStopped()
        if (lock != null) {
            lock!!.visibility = View.GONE
        }
    }

    private fun onOrientationChanged(orientation: Double, tilt: Double) {
        // For performance reasons, only proceed if this fragment is visible
        if (!userVisibleHint) {
            return
        }
        if (skyView != null) {
            skyView!!.onOrientationChanged(orientation, tilt)
        }
    }

    /**
     * Initialize the views in the C/N0 and Shape legends
     * @param v view in which the legend view IDs can be found via view.findViewById()
     */
    private fun initLegendViews(v: View) {
        if (legendLines == null) {
            legendLines = LinkedList()
        } else {
            legendLines!!.clear()
        }
        if (legendShapes == null) {
            legendShapes = LinkedList()
        } else {
            legendShapes!!.clear()
        }

        // Avg C/N0 indicator lines
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_left_line4))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_left_line3))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_left_line2))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_left_line1))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_center_line))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_right_line1))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_right_line2))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_right_line3))
        legendLines!!.add(v.findViewById(R.id.sky_legend_cn0_right_line4))

        // Shape Legend lines
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line1a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line1b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line2a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line2b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line3a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line3b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line4a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line4b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line5a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line5b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line6a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line6b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line7a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line7b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line8a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line8b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line9a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line9b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line10a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line10b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line11a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line12a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line13a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line14a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line14b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line15a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line15b))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line16a))
        legendLines!!.add(v.findViewById(R.id.sky_legend_shape_line16b))

        // Shape Legend shapes
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_circle) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_square) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_pentagon) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_triangle) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_hexagon1) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_oval) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_diamond1) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_diamond2) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_diamond3) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_diamond4) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_diamond5) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_diamond6) as ImageView)
        legendShapes!!.add(v.findViewById<View>(R.id.sky_legend_diamond7) as ImageView)

        // C/N0 Legend text
        legendCn0Title = v.findViewById(R.id.sky_legend_cn0_title)
        legendCn0Units = v.findViewById(R.id.sky_legend_cn0_units)
        legendCn0LeftText = v.findViewById(R.id.sky_legend_cn0_left_text)
        legendCn0LeftCenterText = v.findViewById(R.id.sky_legend_cn0_left_center_text)
        legendCn0CenterText = v.findViewById(R.id.sky_legend_cn0_center_text)
        legendCn0RightCenterText = v.findViewById(R.id.sky_legend_cn0_right_center_text)
        legendCn0RightText = v.findViewById(R.id.sky_legend_cn0_right_text)
        cn0InViewAvgText = v.findViewById(R.id.cn0_text_in_view)
        cn0UsedAvgText = v.findViewById(R.id.cn0_text_used)
    }

    private fun updatecn0AvgMeterText() {
        legendCn0Title!!.setText(R.string.gps_cn0_column_label)
        legendCn0Units!!.setText(R.string.sky_legend_cn0_units)
        legendCn0LeftText!!.setText(R.string.sky_legend_cn0_low)
        legendCn0LeftCenterText!!.setText(R.string.sky_legend_cn0_low_middle)
        legendCn0CenterText!!.setText(R.string.sky_legend_cn0_middle)
        legendCn0RightCenterText!!.setText(R.string.sky_legend_cn0_middle_high)
        legendCn0RightText!!.setText(R.string.sky_legend_cn0_high)
    }

    private fun updateCn0Avgs() {
        if (skyView == null) {
            return
        }
        // Based on the avg C/N0 for "in view" and "used" satellites the left margins need to be adjusted accordingly
        val meterWidthPx = (Application.get().resources.getDimension(R.dimen.cn0_meter_width)
            .toInt()
                - UIUtils.dpToPixels(Application.get(), 7.0f)) // Reduce width for padding
        val minIndicatorMarginPx = Application.get().resources
            .getDimension(R.dimen.cn0_indicator_min_left_margin).toInt()
        val maxIndicatorMarginPx = meterWidthPx + minIndicatorMarginPx
        val minTextViewMarginPx = Application.get().resources
            .getDimension(R.dimen.cn0_textview_min_left_margin).toInt()
        val maxTextViewMarginPx = meterWidthPx + minTextViewMarginPx

        // When both "in view" and "used" indicators and TextViews are shown, slide the "in view" TextView by this amount to the left to avoid overlap
        val TEXTVIEW_NON_OVERLAP_OFFSET_DP = -16.0f

        // Calculate normal offsets for avg in view satellite C/N0 value TextViews
        var leftInViewTextViewMarginPx: Int? = null
        if (MathUtils.isValidFloat(skyView!!.cn0InViewAvg)) {
            leftInViewTextViewMarginPx = UIUtils.cn0ToTextViewLeftMarginPx(
                skyView!!.cn0InViewAvg,
                minTextViewMarginPx, maxTextViewMarginPx
            )
        }

        // Calculate normal offsets for avg used satellite C/N0 value TextViews
        var leftUsedTextViewMarginPx: Int? = null
        if (MathUtils.isValidFloat(skyView!!.cn0UsedAvg)) {
            leftUsedTextViewMarginPx = UIUtils.cn0ToTextViewLeftMarginPx(
                skyView!!.cn0UsedAvg,
                minTextViewMarginPx, maxTextViewMarginPx
            )
        }

        // See if we need to apply the offset margin to try and keep the two TextViews from overlapping by shifting one of the two left
        if (leftInViewTextViewMarginPx != null && leftUsedTextViewMarginPx != null) {
            val offset = UIUtils.dpToPixels(Application.get(), TEXTVIEW_NON_OVERLAP_OFFSET_DP)
            if (leftInViewTextViewMarginPx <= leftUsedTextViewMarginPx) {
                leftInViewTextViewMarginPx += offset
            } else {
                leftUsedTextViewMarginPx += offset
            }
        }

        // Define paddings used for TextViews
        val pSides = UIUtils.dpToPixels(Application.get(), 7f)
        val pTopBottom = UIUtils.dpToPixels(Application.get(), 4f)

        // Set avg C/N0 of satellites in view of device
        if (MathUtils.isValidFloat(skyView!!.cn0InViewAvg)) {
            cn0InViewAvgText!!.text = String.format("%.1f", skyView!!.cn0InViewAvg)

            // Set color of TextView
            val color = skyView!!.getSatelliteColor(skyView!!.cn0InViewAvg)
            val background = ContextCompat.getDrawable(
                Application.get(),
                R.drawable.cn0_round_corner_background_in_view
            ) as LayerDrawable?

            // Fill
            val backgroundGradient =
                background!!.findDrawableByLayerId(R.id.cn0_avg_in_view_fill) as GradientDrawable
            backgroundGradient.setColor(color)

            // Stroke
            val borderGradient =
                background.findDrawableByLayerId(R.id.cn0_avg_in_view_border) as GradientDrawable
            borderGradient.setColor(color)
            cn0InViewAvgText!!.background = background

            // Set padding
            cn0InViewAvgText!!.setPadding(pSides, pTopBottom, pSides, pTopBottom)

            // Set color of indicator
            cn0InViewAvg!!.setColorFilter(color)

            // Set position and visibility of TextView
            if (cn0InViewAvgText!!.visibility == View.VISIBLE) {
                animateCn0Indicator(
                    cn0InViewAvgText,
                    leftInViewTextViewMarginPx!!,
                    cn0InViewAvgAnimationTextView
                )
            } else {
                val lp = cn0InViewAvgText!!.layoutParams as RelativeLayout.LayoutParams
                lp.setMargins(
                    leftInViewTextViewMarginPx!!,
                    lp.topMargin,
                    lp.rightMargin,
                    lp.bottomMargin
                )
                cn0InViewAvgText!!.layoutParams = lp
                cn0InViewAvgText!!.visibility = View.VISIBLE
            }

            // Set position and visibility of indicator
            val leftIndicatorMarginPx = UIUtils.cn0ToIndicatorLeftMarginPx(
                skyView!!.cn0InViewAvg,
                minIndicatorMarginPx, maxIndicatorMarginPx
            )

            // If the view is already visible, animate to the new position.  Otherwise just set the position and make it visible
            if (cn0InViewAvg!!.visibility == View.VISIBLE) {
                animateCn0Indicator(cn0InViewAvg, leftIndicatorMarginPx, cn0InViewAvgAnimation)
            } else {
                val lp = cn0InViewAvg!!.layoutParams as RelativeLayout.LayoutParams
                lp.setMargins(leftIndicatorMarginPx, lp.topMargin, lp.rightMargin, lp.bottomMargin)
                cn0InViewAvg!!.layoutParams = lp
                cn0InViewAvg!!.visibility = View.VISIBLE
            }
        } else {
            cn0InViewAvgText!!.text = ""
            cn0InViewAvgText!!.visibility = View.INVISIBLE
            cn0InViewAvg!!.visibility = View.INVISIBLE
        }

        // Set avg C/N0 of satellites used in fix
        if (MathUtils.isValidFloat(skyView!!.cn0UsedAvg)) {
            cn0UsedAvgText!!.text = String.format("%.1f", skyView!!.cn0UsedAvg)
            // Set color of TextView
            val color = skyView!!.getSatelliteColor(skyView!!.cn0UsedAvg)
            val background =
                ContextCompat.getDrawable(Application.get(), usedCn0Background) as LayerDrawable?

            // Fill
            val backgroundGradient =
                background!!.findDrawableByLayerId(R.id.cn0_avg_used_fill) as GradientDrawable
            backgroundGradient.setColor(color)
            cn0UsedAvgText!!.background = background

            // Set padding
            cn0UsedAvgText!!.setPadding(pSides, pTopBottom, pSides, pTopBottom)

            // Set color of indicator
            cn0UsedAvg!!.setColorFilter(usedCn0IndicatorColor)

            // Set position and visibility of TextView
            if (cn0UsedAvgText!!.visibility == View.VISIBLE) {
                animateCn0Indicator(
                    cn0UsedAvgText,
                    leftUsedTextViewMarginPx!!,
                    cn0UsedAvgAnimationTextView
                )
            } else {
                val lp = cn0UsedAvgText!!.layoutParams as RelativeLayout.LayoutParams
                lp.setMargins(
                    leftUsedTextViewMarginPx!!,
                    lp.topMargin,
                    lp.rightMargin,
                    lp.bottomMargin
                )
                cn0UsedAvgText!!.layoutParams = lp
                cn0UsedAvgText!!.visibility = View.VISIBLE
            }

            // Set position and visibility of indicator
            val leftMarginPx = UIUtils.cn0ToIndicatorLeftMarginPx(
                skyView!!.cn0UsedAvg,
                minIndicatorMarginPx, maxIndicatorMarginPx
            )

            // If the view is already visible, animate to the new position.  Otherwise just set the position and make it visible
            if (cn0UsedAvg!!.visibility == View.VISIBLE) {
                animateCn0Indicator(cn0UsedAvg, leftMarginPx, cn0UsedAvgAnimation)
            } else {
                val lp = cn0UsedAvg!!.layoutParams as RelativeLayout.LayoutParams
                lp.setMargins(leftMarginPx, lp.topMargin, lp.rightMargin, lp.bottomMargin)
                cn0UsedAvg!!.layoutParams = lp
                cn0UsedAvg!!.visibility = View.VISIBLE
            }
        } else {
            cn0UsedAvgText!!.text = ""
            cn0UsedAvgText!!.visibility = View.INVISIBLE
            cn0UsedAvg!!.visibility = View.INVISIBLE
        }
    }

    /**
     * Animates a C/N0 indicator view from it's current location to the provided left margin location (in pixels)
     * @param v view to animate
     * @param goalLeftMarginPx the new left margin for the view that the view should animate to in pixels
     * @param animation Animation to use for the animation
     */
    private fun animateCn0Indicator(v: View?, goalLeftMarginPx: Int, animation: Animation?) {
        if (v == null) {
            return
        }
        var mutableAnimation = animation
        mutableAnimation?.reset()
        val p = v.layoutParams as MarginLayoutParams
        val currentMargin = p.leftMargin
        mutableAnimation = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                val newLeft: Int = if (goalLeftMarginPx > currentMargin) {
                    currentMargin + (abs(currentMargin - goalLeftMarginPx)
                            * interpolatedTime).toInt()
                } else {
                    currentMargin - (abs(currentMargin - goalLeftMarginPx)
                            * interpolatedTime).toInt()
                }
                UIUtils.setMargins(
                    v,
                    newLeft,
                    p.topMargin,
                    p.rightMargin,
                    p.bottomMargin
                )
            }
        }
        // C/N0 updates every second, so animation of 300ms (https://material.io/guidelines/motion/duration-easing.html#duration-easing-common-durations)
        // wit FastOutSlowInInterpolator recommended by Material Design spec easily finishes in time for next C/N0 update
        mutableAnimation.setDuration(300)
        mutableAnimation.setInterpolator(FastOutSlowInInterpolator())
        v.startAnimation(mutableAnimation)
    }

    private fun showHaveFix() {
        if (lock != null) {
            UIUtils.showViewWithAnimation(lock, UIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    private fun showLostFix() {
        if (lock != null) {
            UIUtils.hideViewWithAnimation(lock, UIUtils.ANIMATION_DURATION_SHORT_MS)
        }
    }

    companion object {
        const val TAG = "GpsSkyFragment"
    }
}