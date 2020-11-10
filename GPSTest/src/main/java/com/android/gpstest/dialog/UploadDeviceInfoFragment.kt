package com.android.gpstest.dialog

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.coroutineScope
import com.android.gpstest.Application
import com.android.gpstest.BuildConfig
import com.android.gpstest.R
import com.android.gpstest.io.DevicePropertiesUploader
import com.android.gpstest.util.IOUtils
import com.android.gpstest.util.PreferenceUtils
import com.android.gpstest.util.SatelliteUtils
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
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

            // Upload device info to database
            val bundle = bundleOf(
                    DevicePropertiesUploader.MANUFACTURER to Build.MANUFACTURER,
                    DevicePropertiesUploader.MODEL to Build.MODEL,
                    DevicePropertiesUploader.ANDROID_VERSION to Build.VERSION.RELEASE,
                    DevicePropertiesUploader.API_LEVEL to Build.VERSION.SDK_INT.toString(),
                    DevicePropertiesUploader.GNSS_HARDWARE_YEAR to IOUtils.getGnssHardwareYear(),
                    DevicePropertiesUploader.GNSS_HARDWARE_MODEL_NAME to IOUtils.getGnssHardwareModelName(),
//                    UploadDevicePropertiesWorker.DUAL_FREQUENCY
//                    UploadDevicePropertiesWorker.SUPPORTED_GNSS
//                    UploadDevicePropertiesWorker.GNSS_CFS
//                    UploadDevicePropertiesWorker.SUPPORTED_SBAS
//                    UploadDevicePropertiesWorker.SBAS_CFS
//                    UploadDevicePropertiesWorker.RAW_MEASUREMENTS
//                    UploadDevicePropertiesWorker.NAVIGATION_MESSAGES
                    DevicePropertiesUploader.NMEA to PreferenceUtils.getCapabilityDescription(Application.getPrefs().getInt(Application.get().getString(R.string.capability_key_nmea), PreferenceUtils.CAPABILITY_UNKNOWN)),
                    DevicePropertiesUploader.INJECT_PSDS to psdsSuccessString,
                    DevicePropertiesUploader.INJECT_TIME to timeSuccessString,
//                    UploadDevicePropertiesWorker.DELETE_ASSIST
//                    UploadDevicePropertiesWorker.ACCUMULATED_DELTA_RANGE
//                    UploadDevicePropertiesWorker.HARDWARE_CLOCK
//                    UploadDevicePropertiesWorker.HARDWARE_CLOCK_DISCONTINUITY
//                    UploadDevicePropertiesWorker.AUTOMATIC_GAIN_CONTROL
                    DevicePropertiesUploader.GNSS_ANTENNA_INFO to PreferenceUtils.getCapabilityDescription(SatelliteUtils.isGnssAntennaInfoSupported(locationManager)),
                    DevicePropertiesUploader.APP_VERSION_NAME to versionName,
                    DevicePropertiesUploader.APP_VERSION_CODE to versionCode,
                    DevicePropertiesUploader.APP_BUILD_FLAVOR to BuildConfig.FLAVOR
            )

            upload.isEnabled = false
            uploadProgress.visibility = View.VISIBLE

            lifecycle.coroutineScope.launch(Dispatchers.IO) {
                val uploader = DevicePropertiesUploader(bundle)
                val result = uploader.upload()
                // TODO - re-enable button and stop progress bar after completion
            }

//            workManager.getWorkInfosByTag(TAG)

//            val listenableFuture = workManager.getWorkInfoById(workRequest.id)
//            Futures.addCallback(listenableFuture, object : FutureCallback<WorkInfo?>() {
//                fun onSuccess(@NullableDecl result: WorkInfo?) {
//                    activity?.runOnUiThread {
////                        Toast.makeText(Application.get(), R.string.travel_behavior_enroll_success,
////                                Toast.LENGTH_LONG).show()
//                    }
//                }
//
//                fun onFailure(t: Throwable?) {
//                    activity?.runOnUiThread {
////                        Toast.makeText(Application.get(), R.string.travel_behavior_enroll_fail,
////                                Toast.LENGTH_LONG).show()
//                    }
//                }
//            }, TravelBehaviorFileSaverExecutorManager.getInstance().getThreadPoolExecutor())



//            val workInfo = workManager.getWorkInfoById(workRequest.id)
//            workManager.getWorkInfoByIdLiveData(workRequest.id)
//                .observe(viewLifecycleOwner) { t: WorkInfo? ->
//                    if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
////                        Snackbar.make(requireView(),
////                                R.string.work_completed, Snackbar.LENGTH_SHORT)
////                                .show()
//                    }
//                }
        }
    }
}