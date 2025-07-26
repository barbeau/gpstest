package com.android.gpstest.ui.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.BuildConfig
import com.android.gpstest.R
import com.android.gpstest.library.util.IOUtils
import com.android.gpstest.library.util.LibUIUtils
import com.google.android.material.button.MaterialButton
import java.io.File
import java.util.*

class ShareLogFragment : Fragment() {

    private lateinit var listener: Listener

    interface Listener {
        /**
         * Called when the fragment sends the log file
         */
        fun onLogFileSent()

        /**
         * Called when the file browser is opened
         */
        fun onFileBrowse()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setRetainInstance(true)
        return inflater.inflate(R.layout.share_log, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val logLabel = view.findViewById<TextView>(R.id.log_file_label)
        val fileName = view.findViewById<TextView>(R.id.log_file_name)
        val logInstructions = view.findViewById<TextView>(R.id.log_instructions)
        val logBrowse: MaterialButton = view.findViewById(R.id.log_browse)
        val logShare: MaterialButton = view.findViewById(R.id.log_share)

        val loggingEnabled = arguments?.getBoolean(ShareDialogFragment.KEY_LOGGING_ENABLED) ?: false
        val files = arguments?.getSerializable(ShareDialogFragment.KEY_LOG_FILES) as ArrayList<File>?
        val alternateFileUri = arguments?.getParcelable<Uri>(ShareDialogFragment.KEY_ALTERNATE_FILE_URI)

        if (loggingEnabled && files != null) {
            // Hide the logging instructions - logging is enabled and working
            logInstructions.visibility = View.GONE
        } else {
            // Hide the logging and file views so the user can see the instructions
            logLabel.visibility = View.GONE
            fileName.visibility = View.GONE
            logBrowse.visibility = View.GONE
            logShare.visibility = View.GONE
        }

        // Set the log file name
        if (loggingEnabled) {
            if (files != null) {
                if (alternateFileUri == null) {
                    var fileNameText = ""
                    // Set the log file currently being logged to by the FileLogger
                    for (file in files) {
                        fileNameText += file.name + System.getProperty("line.separator")
                    }
                    fileName.text = fileNameText
                } else {
                    // Set the log file selected by the user using the File Browse button
                    val lastPathSegment: String? = alternateFileUri.getLastPathSegment()
                    // Parse file name from string like "primary:gnss_log/gnss_log_2019..."
                    val parts = lastPathSegment?.split("/".toRegex())?.toTypedArray()
                    fileName.text = parts?.get(parts.size - 1) ?: ""
                }
            } else {
                // Something went wrong - did user allow file/storage permissions when prompted when they enabled logging in Settings?
                logInstructions.setText(R.string.log_error)
                return
            }
        }

        logBrowse.setOnClickListener { _: View? ->
            // File browse
            val uri = IOUtils.getUriFromFile(activity, BuildConfig.APPLICATION_ID, files?.get(0))
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.data = uri
            requireActivity().startActivityForResult(intent, LibUIUtils.PICKFILE_REQUEST_CODE)
            // Dismiss the dialog - it will be re-created in the callback to GpsTestActivity
            listener.onFileBrowse()
        }

        logShare.setOnClickListener { _: View? ->
            // Send the log file
            if (alternateFileUri == null && files != null) {
                // Send the log file currently being logged to by the FileLogger
                IOUtils.sendLogFile(app, BuildConfig.APPLICATION_ID, activity, *files.toTypedArray())
                listener.onLogFileSent()
            } else {
                // Send the log file selected by the user using the File Browse button
                IOUtils.sendLogFile(app, activity, ArrayList(Collections.singleton(alternateFileUri)))
                listener.onLogFileSent()
            }
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }
}