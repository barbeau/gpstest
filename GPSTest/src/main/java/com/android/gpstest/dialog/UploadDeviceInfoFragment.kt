package com.android.gpstest.dialog

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.io.UploadDevicePropertiesWorker
import com.google.android.material.button.MaterialButton

class UploadDeviceInfoFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setRetainInstance(true)
        return inflater.inflate(R.layout.share_upload, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val uploadNoLocationTextView: TextView = view.findViewById(R.id.upload_no_location)
        val uploadDetails: TextView = view.findViewById(R.id.upload_details)
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
            // Upload device info to database
            val myData = Data.Builder()
                    .putString(UploadDevicePropertiesWorker.MODEL, Build.MODEL)
                    .putString(UploadDevicePropertiesWorker.ANDROID_VERSION, Build.VERSION.RELEASE + " / " + Build.VERSION.SDK_INT)
                    .build()
            val workRequest = OneTimeWorkRequest.Builder(
                    UploadDevicePropertiesWorker::class.java)
                    .setInputData(myData)
                    .build()
            WorkManager.getInstance(Application.get()).enqueue(workRequest)
        }
    }
}