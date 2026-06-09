package com.android.gpstest.ui.share

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.gpstest.Application
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.R
import com.android.gpstest.library.model.CoordinateType
import com.android.gpstest.library.util.IOUtils
import com.android.gpstest.library.util.LibUIUtils
import com.android.gpstest.library.util.PreferenceUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ShareLocationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setRetainInstance(true)
        return inflater.inflate(R.layout.share_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val locationValue = view.findViewById<TextView>(R.id.location_value)
        val includeAltitude = view.findViewById<CheckBox>(R.id.include_altitude)
        val includeTimestamp = view.findViewById<CheckBox>(R.id.include_timestamp)
        val noLocation = view.findViewById<TextView>(R.id.no_location)
        val locationCopy: MaterialButton = view.findViewById(R.id.location_copy)
        val locationGeohack: MaterialButton = view.findViewById(R.id.location_geohack)
        val locationLaunchApp: MaterialButton = view.findViewById(R.id.location_launch_app)
        val locationShare: MaterialButton = view.findViewById(R.id.location_share)
        val chipGroup: ChipGroup = view.findViewById(R.id.coordinate_format_group)
        val chipDecimalDegrees: Chip = view.findViewById(R.id.chip_decimal_degrees)
        val chipDMS: Chip = view.findViewById(R.id.chip_dms)
        val chipDegreesDecimalMin: Chip = view.findViewById(R.id.chip_degrees_decimal_minutes)

        val location = arguments?.getParcelable<Location>(ShareDialogFragment.KEY_LOCATION)

        if (location == null) {
            // No location - Hide the location info
            locationValue.visibility = View.GONE
            includeAltitude.visibility = View.GONE
            includeTimestamp.visibility = View.GONE
            chipGroup.visibility = View.GONE
            locationCopy.visibility = View.GONE
            locationGeohack.visibility = View.GONE
            locationLaunchApp.visibility = View.GONE
            locationShare.visibility = View.GONE
        } else {
            // We have a location - Hide the "no location" message
            noLocation.visibility = View.GONE
        }

        // Set default state of include altitude / include timestamp views
        val includeAltitudePref = Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_share_include_altitude), false)
        includeAltitude.isChecked = includeAltitudePref
        val includeTimestampPref = Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_share_include_timestamp), false)
        includeTimestamp.isChecked = includeTimestampPref

        // Returns the location as a plain coordinate string in the currently selected
        // coordinate format, with altitude appended when the "include altitude" checkbox is on.
        fun coordinatesOnly(): String {
            if (location == null) return ""
            return when {
                chipDMS.isChecked -> IOUtils.createLocationShare(
                    LibUIUtils.getDMSFromLocation(app, location.latitude, CoordinateType.LATITUDE),
                    LibUIUtils.getDMSFromLocation(app, location.longitude, CoordinateType.LONGITUDE),
                    if (location.hasAltitude() && includeAltitude.isChecked) location.altitude.toString() else null
                )
                chipDegreesDecimalMin.isChecked -> IOUtils.createLocationShare(
                    LibUIUtils.getDDMFromLocation(app, location.latitude, CoordinateType.LATITUDE),
                    LibUIUtils.getDDMFromLocation(app, location.longitude, CoordinateType.LONGITUDE),
                    if (location.hasAltitude() && includeAltitude.isChecked) location.altitude.toString() else null
                )
                else -> IOUtils.createLocationShare(location, includeAltitude.isChecked)
            }
        }

        // Single entry point that rewrites the locationValue TextView.
        // Combines the coordinate string from coordinatesOnly() with an optional timestamp
        // line when the "include timestamp" checkbox is selected.
        // Every UI event (altitude/timestamp toggles, chip selection, initial display)
        // funnels through this functtion, so the timestamp suffix logic lives
        // in one place rather than being duplicated across each listener.
        fun refreshLocationText() {
            if (location == null) return
            val base = coordinatesOnly()
            locationValue.text = if (includeTimestamp.isChecked && location.time > 0) {
                "$base\n${IOUtils.formatLocationTimestamp(location.time)}"
            } else {
                base
            }
        }

        // Check selected coordinate format and show in UI.
        // The TextView argument to formatLocationForDisplay() is passed as null:
        // We no longer let that helper write the location string directly,
        // instead refreshLocationText() owns that logic and the timestamp
        // suffix is applied consistently.
        // formatLocationForDisplay() is still called for its side effect of
        // selecting the correct chip based on the saved coordinate format preference.
        val coordinateFormat = Application.prefs.getString(Application.app.getString(R.string.pref_key_coordinate_format), Application.app.getString(R.string.preferences_coordinate_format_dd_key))
        if (location != null) {
            LibUIUtils.formatLocationForDisplay(
                app,
                location,
                null,
                includeAltitude.isChecked,
                chipDecimalDegrees,
                chipDMS,
                chipDegreesDecimalMin,
                coordinateFormat
            )
            refreshLocationText()
        }

        // All five listeners below previously contained their own coordinate-formatting branches.
        // They are now one-liners that delegate to refreshLocationText(), which
        // centralises both the coordinate formatting (via coordinatesOnly()) and the
        // optional timestamp suffix. The altitude / timestamp listeners additionally
        // persist their checked state to SharedPreferences.
        includeAltitude.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            refreshLocationText()
            PreferenceUtils.saveBoolean(Application.app.getString(R.string.pref_key_share_include_altitude), isChecked, prefs)
        }

        includeTimestamp.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            refreshLocationText()
            PreferenceUtils.saveBoolean(Application.app.getString(R.string.pref_key_share_include_timestamp), isChecked, prefs)
        }

        chipDecimalDegrees.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) refreshLocationText()
        }
        chipDMS.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) refreshLocationText()
        }
        chipDegreesDecimalMin.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) refreshLocationText()
        }

        locationCopy.setOnClickListener { _: View? ->
            // Copy to clipboard
            if (location != null) {
                val locationString = locationValue.text.toString()
                IOUtils.copyToClipboard(app, locationString)
                Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show()
            }
        }
        locationGeohack.setOnClickListener { _: View? ->
            // Open the browser to the GeoHack site with lots of coordinate conversions
            if (location != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                val geohackUrl = Application.app.getString(R.string.geohack_url) +
                        location.latitude + ";" +
                        location.longitude
                intent.data = Uri.parse(geohackUrl)
                requireActivity().startActivity(intent)
            }
        }
        locationLaunchApp.setOnClickListener { _: View? ->
            // Open the location in another app
            if (location != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(IOUtils.createGeoUri(app, location, includeAltitude.isChecked))
                if (intent.resolveActivity(requireActivity().packageManager) != null) {
                    requireActivity().startActivity(intent)
                }
            }
        }
        locationShare.setOnClickListener { _: View? ->
            // Send the location as a Geo URI (e.g., in an email) when decimal degrees are selected
            // and no timestamp is requested; otherwise send the plain text version so the timestamp
            // (which has no place in a geo: URI) is preserved.
            if (location != null) {
                val intent = Intent(Intent.ACTION_SEND)
                val text: String = if (chipDecimalDegrees.isChecked && !includeTimestamp.isChecked) {
                    IOUtils.createGeoUri(app, location, includeAltitude.isChecked)
                } else {
                    locationValue.text.toString()
                }
                intent.putExtra(Intent.EXTRA_TEXT, text)
                intent.type = "text/plain"
                requireActivity().startActivity(Intent.createChooser(intent, Application.app.getString(R.string.share)))
            }
        }
    }
}