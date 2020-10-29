package com.android.gpstest.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.gpstest.R
import com.google.android.material.button.MaterialButton

class ShareLogFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.share_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fileName = view.findViewById<TextView>(R.id.log_file_name)
        val logInstructions = view.findViewById<TextView>(R.id.log_instructions)
        val logBrowse: MaterialButton = view.findViewById(R.id.log_browse)
        val logShare: MaterialButton = view.findViewById(R.id.log_share)

//        val file: File = fileLogger.getFile()
//
//        if (loggingEnabled && file != null) {
//            // Hide the logging instructions - logging is enabled and working
//            logInstructions.visibility = View.GONE
//        } else {
//            // Hide the logging and file views so the user can see the instructions
//            fileName.visibility = View.GONE
//            logBrowse.visibility = View.GONE
//            logShare.visibility = View.GONE
//        }
//
//        // Set the log file name
//
//        // Set the log file name
//        if (loggingEnabled) {
//            if (file != null) {
//                if (alternateFileUri == null) {
//                    // Set the log file currently being logged to by the FileLogger
//                    fileName.text = file.name
//                } else {
//                    // Set the log file selected by the user using the File Browse button
//                    val lastPathSegment: String = alternateFileUri.getLastPathSegment()
//                    // Parse file name from string like "primary:gnss_log/gnss_log_2019..."
//                    val parts = lastPathSegment.split("/".toRegex()).toTypedArray()
//                    fileName.text = parts[parts.size - 1]
//                }
//            } else {
//                // Something went wrong - did user allow file/storage permissions when prompted when they enabled logging in Settings?
//                logInstructions.setText(R.string.log_error)
//            }
//        }
//
//        logBrowse.setOnClickListener { v: View? ->
//            // File browse
//            val uri = IOUtils.getUriFromFile(activity, fileLogger.getFile())
//            val intent = Intent(Intent.ACTION_GET_CONTENT)
//            intent.data = uri
//            activity!!.startActivityForResult(intent, UIUtils.PICKFILE_REQUEST_CODE)
//            // Dismiss the dialog - it will be re-created in the callback to GpsTestActivity
//            dialog.dismiss()
//        }
//
//        logShare.setOnClickListener { v: View? ->
//            // Send the log file
//            if (alternateFileUri == null) {
//                // Send the log file currently being logged to by the FileLogger
//                fileLogger.send(activity)
//            } else {
//                // Send the log file selected by the user using the File Browse button
//                IOUtils.sendLogFile(activity, alternateFileUri)
//            }
//        }
    }
}