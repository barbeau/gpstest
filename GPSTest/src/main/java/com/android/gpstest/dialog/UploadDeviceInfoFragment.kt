package com.android.gpstest.dialog

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.coroutineScope
import com.android.gpstest.Application
import com.android.gpstest.BuildConfig
import com.android.gpstest.DeviceInfoViewModel
import com.android.gpstest.R
import com.android.gpstest.io.DevicePropertiesUploader
import com.android.gpstest.util.IOUtils
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.SatelliteUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class UploadDeviceInfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setRetainInstance(true)
        return inflater.inflate(R.layout.share_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uploadNoLocationTextView: TextView = view.findViewById(R.id.upload_no_location)
        val uploadDetails: TextView = view.findViewById(R.id.upload_details)
        val uploadProgress: ProgressBar = view.findViewById(R.id.upload_progress)
        val upload: MaterialButton = view.findViewById(R.id.upload)

        val location = arguments?.getParcelable<Location>(ShareDialogFragment.KEY_LOCATION)
        var userCountry = ""

        if (location == null) {
            // No location
            uploadDetails.visibility = View.GONE
            upload.visibility = View.GONE
            uploadNoLocationTextView.visibility = View.VISIBLE
        } else {
            // We have a location
            uploadDetails.visibility = View.VISIBLE
            upload.visibility = View.VISIBLE
            uploadNoLocationTextView.visibility = View.GONE

            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context)
                val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                if (!addresses.isEmpty()) {
                    userCountry = addresses.get(0).countryCode
                }
            }
        }

        upload.setOnClickListener { v: View? ->
            var versionName = ""
            var versionCode = ""
            try {
                val info: PackageInfo = Application.get().packageManager.getPackageInfo(Application.get().packageName, 0)
                versionName = info.versionName
                versionCode = info.versionCode.toString()
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            val locationManager = Application.get().getSystemService(Context.LOCATION_SERVICE) as LocationManager

            // Inject PSDS capability
            val capabilityInjectPsdsInt = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_inject_psds), PreferenceUtils.CAPABILITY_UNKNOWN)
            val psdsSuccessBoolean: Boolean
            val psdsSuccessString: String
            if (capabilityInjectPsdsInt == PreferenceUtils.CAPABILITY_UNKNOWN) {
                psdsSuccessBoolean = IOUtils.forcePsdsInjection(locationManager)
                psdsSuccessString = PreferenceUtils.getCapabilityDescription(psdsSuccessBoolean)
            } else {
                psdsSuccessString = PreferenceUtils.getCapabilityDescription(capabilityInjectPsdsInt)
            }

            // Inject time
            val capabilityInjectTimeInt = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_inject_time), PreferenceUtils.CAPABILITY_UNKNOWN)
            val timeSuccessBoolean: Boolean
            val timeSuccessString: String
            if (capabilityInjectTimeInt == PreferenceUtils.CAPABILITY_UNKNOWN) {
                timeSuccessBoolean = IOUtils.forceTimeInjection(locationManager)
                timeSuccessString = PreferenceUtils.getCapabilityDescription(timeSuccessBoolean)
            } else {
                timeSuccessString = PreferenceUtils.getCapabilityDescription(capabilityInjectTimeInt)
            }

            // Delete assist capability
            val capabilityDeleteAssistInt = Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_delete_assist), PreferenceUtils.CAPABILITY_UNKNOWN)
            val deleteAssistSuccessString: String
            if (capabilityDeleteAssistInt != PreferenceUtils.CAPABILITY_UNKNOWN) {
                // Deleting assist data can be destructive, so don't force it - just use existing info
                deleteAssistSuccessString = PreferenceUtils.getCapabilityDescription(capabilityDeleteAssistInt)
            } else {
                deleteAssistSuccessString = ""
            }

            val deviceInfoViewModel = ViewModelProviders.of(activity!!).get(DeviceInfoViewModel::class.java)

            // Upload device info to database
            val bundle = bundleOf(
                    DevicePropertiesUploader.MANUFACTURER to Build.MANUFACTURER,
                    DevicePropertiesUploader.MODEL to Build.MODEL,
                    DevicePropertiesUploader.ANDROID_VERSION to Build.VERSION.RELEASE,
                    DevicePropertiesUploader.API_LEVEL to Build.VERSION.SDK_INT.toString(),
                    DevicePropertiesUploader.GNSS_HARDWARE_YEAR to IOUtils.getGnssHardwareYear(),
                    DevicePropertiesUploader.GNSS_HARDWARE_MODEL_NAME to IOUtils.getGnssHardwareModelName(),
                    DevicePropertiesUploader.DUAL_FREQUENCY to PreferenceUtils.getCapabilityDescription(deviceInfoViewModel.isNonPrimaryCarrierFreqInView),
//                    DevicePropertiesUploader.SUPPORTED_GNSS
//                    DevicePropertiesUploader.GNSS_CFS
//                    DevicePropertiesUploader.SUPPORTED_SBAS
//                    DevicePropertiesUploader.SBAS_CFS
//                    DevicePropertiesUploader.RAW_MEASUREMENTS
//                    DevicePropertiesUploader.NAVIGATION_MESSAGES
                    DevicePropertiesUploader.NMEA to PreferenceUtils.getCapabilityDescription(Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_nmea), PreferenceUtils.CAPABILITY_UNKNOWN)),
                    DevicePropertiesUploader.INJECT_PSDS to psdsSuccessString,
                    DevicePropertiesUploader.INJECT_TIME to timeSuccessString,
                    DevicePropertiesUploader.DELETE_ASSIST to deleteAssistSuccessString,
//                    DevicePropertiesUploader.ACCUMULATED_DELTA_RANGE
//                    DevicePropertiesUploader.HARDWARE_CLOCK
//                    DevicePropertiesUploader.HARDWARE_CLOCK_DISCONTINUITY
//                    DevicePropertiesUploader.AUTOMATIC_GAIN_CONTROL
                    DevicePropertiesUploader.GNSS_ANTENNA_INFO to PreferenceUtils.getCapabilityDescription(SatelliteUtils.isGnssAntennaInfoSupported(locationManager)),
                    DevicePropertiesUploader.APP_VERSION_NAME to versionName,
                    DevicePropertiesUploader.APP_VERSION_CODE to versionCode,
                    DevicePropertiesUploader.APP_BUILD_FLAVOR to BuildConfig.FLAVOR,
                    DevicePropertiesUploader.USER_COUNTRY to userCountry
            )

            // TODO - check hash of previously uploaded data (if previously uploaded) and only enable option if it's changed
            upload.isEnabled = false
            uploadProgress.visibility = View.VISIBLE

            lifecycle.coroutineScope.launch {
                val uploader = DevicePropertiesUploader(bundle)
                if (uploader.upload()) {
                    Toast.makeText(Application.get(), R.string.upload_success, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(Application.get(), R.string.upload_failure, Toast.LENGTH_SHORT).show()
                    upload.isEnabled = true
                }
                uploadProgress.visibility = View.INVISIBLE
            }
        }
    }
}