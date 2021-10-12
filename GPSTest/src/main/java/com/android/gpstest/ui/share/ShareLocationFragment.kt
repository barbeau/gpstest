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
import com.android.gpstest.R
import com.android.gpstest.model.CoordinateType
import com.android.gpstest.util.IOUtils
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.UIUtils
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
            chipGroup.visibility = View.GONE
            locationCopy.visibility = View.GONE
            locationGeohack.visibility = View.GONE
            locationLaunchApp.visibility = View.GONE
            locationShare.visibility = View.GONE
        } else {
            // We have a location - Hide the "no location" message
            noLocation.visibility = View.GONE
        }

        // Set default state of include altitude view

        // Set default state of include altitude view
        val includeAltitudePref = Application.prefs.getBoolean(Application.app.getString(R.string.pref_key_share_include_altitude), false)
        includeAltitude.isChecked = includeAltitudePref

        // Check selected coordinate format and show in UI

        // Check selected coordinate format and show in UI
        val coordinateFormat = Application.prefs.getString(Application.app.getString(R.string.pref_key_coordinate_format), Application.app.getString(R.string.preferences_coordinate_format_dd_key))
        UIUtils.formatLocationForDisplay(location, locationValue, includeAltitude.isChecked, chipDecimalDegrees, chipDMS, chipDegreesDecimalMin, coordinateFormat)

        // Change the location text when the user toggles the altitude checkbox

        // Change the location text when the user toggles the altitude checkbox
        includeAltitude.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            var format = "dd"
            if (chipDecimalDegrees.isChecked) {
                format = "dd"
            } else if (chipDMS.isChecked) {
                format = "dms"
            } else if (chipDegreesDecimalMin.isChecked) {
                format = "ddm"
            }
            UIUtils.formatLocationForDisplay(location, locationValue, isChecked, chipDecimalDegrees, chipDMS, chipDegreesDecimalMin, format)
            PreferenceUtils.saveBoolean(Application.app.getString(R.string.pref_key_share_include_altitude), isChecked)
        }

        chipDecimalDegrees.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                locationValue.text = IOUtils.createLocationShare(location, includeAltitude.isChecked)
            }
        }
        chipDMS.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                if (location != null) {
                    locationValue.text = IOUtils.createLocationShare(UIUtils.getDMSFromLocation(Application.app, location.latitude, CoordinateType.LATITUDE),
                            UIUtils.getDMSFromLocation(Application.app, location.longitude, CoordinateType.LONGITUDE),
                            if (location.hasAltitude() && includeAltitude.isChecked) location.altitude.toString() else null)
                }
            }
        }
        chipDegreesDecimalMin.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                if (location != null) {
                    locationValue.text = IOUtils.createLocationShare(UIUtils.getDDMFromLocation(Application.app, location.latitude, CoordinateType.LATITUDE),
                            UIUtils.getDDMFromLocation(Application.app, location.longitude, CoordinateType.LONGITUDE),
                            if (location.hasAltitude() && includeAltitude.isChecked) location.altitude.toString() else null)
                }
            }
        }

        locationCopy.setOnClickListener { _: View? ->
            // Copy to clipboard
            if (location != null) {
                val locationString = locationValue.text.toString()
                IOUtils.copyToClipboard(locationString)
                Toast.makeText(activity, R.string.copied_to_clipboard, Toast.LENGTH_LONG).show()
            }
        }
        locationGeohack.setOnClickListener { _: View? ->
            // Open the browser to the GeoHack site with lots of coordinate conversions
            if (location != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                val geohackUrl = Application.app.getString(R.string.geohack_url) +
                        location.getLatitude() + ";" +
                        location.getLongitude()
                intent.data = Uri.parse(geohackUrl)
                activity!!.startActivity(intent)
            }
        }
        locationLaunchApp.setOnClickListener { _: View? ->
            // Open the location in another app
            if (location != null) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(IOUtils.createGeoUri(location, includeAltitude.isChecked))
                if (intent.resolveActivity(activity!!.packageManager) != null) {
                    activity!!.startActivity(intent)
                }
            }
        }
        locationShare.setOnClickListener { _: View? ->
            // Send the location as a Geo URI (e.g., in an email) if the user has decimal degrees
            // selected, otherwise send plain text version
            if (location != null) {
                val intent = Intent(Intent.ACTION_SEND)
                val text: String
                text = if (chipDecimalDegrees.isChecked) {
                    IOUtils.createGeoUri(location, includeAltitude.isChecked)
                } else {
                    locationValue.text.toString()
                }
                intent.putExtra(Intent.EXTRA_TEXT, text)
                intent.type = "text/plain"
                activity!!.startActivity(Intent.createChooser(intent, Application.app.getString(R.string.share)))
            }
        }
    }
}