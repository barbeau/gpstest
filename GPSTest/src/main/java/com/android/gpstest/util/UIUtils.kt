/*
 * Copyright (C) 2015-2018 University of South  Florida, Sean J. Barbeau
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
package com.android.gpstest.util

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import com.android.gpstest.R
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.android.gpstest.Application
import com.android.gpstest.Application.Companion.app
import com.android.gpstest.Application.Companion.prefs
import com.android.gpstest.io.CsvFileLogger
import com.android.gpstest.io.JsonFileLogger
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.util.IOUtils
import com.android.gpstest.library.util.LocationUtils
import com.android.gpstest.library.util.PreferenceUtils
import com.android.gpstest.library.util.LibUIUtils
import com.android.gpstest.ui.GnssFilterDialog
import com.android.gpstest.ui.HelpActivity
import com.android.gpstest.ui.share.ShareDialogFragment
import com.android.gpstest.ui.share.ShareDialogFragment.Companion.KEY_ALTERNATE_FILE_URI
import com.android.gpstest.ui.share.ShareDialogFragment.Companion.KEY_LOCATION
import com.android.gpstest.ui.share.ShareDialogFragment.Companion.KEY_LOGGING_ENABLED
import com.android.gpstest.ui.share.ShareDialogFragment.Companion.KEY_LOG_FILES
import java.io.File
import java.util.*

/**
 * Utilities for processing user inteface elements
 */
internal object UIUtils {
    /**
     * Tests to see if the provided text latitude, longitude, and altitude values are valid, and if
     * not shows an error dialog and returns false, or if yes then returns true
     * @param activity
     * @param lat latitude to validate
     * @param lon longitude to validate
     * @param alt altitude to validate
     * @return true if the latitude, longitude, and latitude are valid, false if any of them are not
     */
    @JvmStatic
    fun isValidLocationWithErrorDialog(
        activity: AppCompatActivity,
        lat: String?,
        lon: String?,
        alt: String?
    ): Boolean {
        val dialogTitle = app.getString(R.string.ground_truth_invalid_location_title)
        val dialogMessage: String
        if (!LocationUtils.isValidLatitude(lat)) {
            dialogMessage = app.getString(R.string.ground_truth_invalid_lat)
            showLocationErrorDialog(activity, dialogTitle, dialogMessage)
            return false
        }
        if (!LocationUtils.isValidLongitude(lon)) {
            dialogMessage = app.getString(R.string.ground_truth_invalid_long)
            showLocationErrorDialog(activity, dialogTitle, dialogMessage)
            return false
        }
        if (!TextUtils.isEmpty(alt) && !LocationUtils.isValidAltitude(alt)) {
            dialogMessage = app.getString(R.string.ground_truth_invalid_alt)
            showLocationErrorDialog(activity, dialogTitle, dialogMessage)
            return false
        }
        return true
    }

    /**
     * Shows an error dialog for an incorrectly entered latitude, longitude, or altitude
     * @param activity
     * @param title title of the error dialog
     * @param message message body of the error dialog
     */
    private fun showLocationErrorDialog(
        activity: AppCompatActivity,
        title: String,
        message: String
    ) {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog: DialogInterface?, id: Int -> }
            .create()
            .show()
    }

    @JvmStatic
    fun createQrCodeDialog(activity: AppCompatActivity): Dialog {
        val view = activity.layoutInflater.inflate(R.layout.qr_code_instructions, null)
        val neverShowDialog = view.findViewById<CheckBox>(R.id.qr_code_never_show_again)
        neverShowDialog.setOnCheckedChangeListener { compoundButton: CompoundButton?, isChecked: Boolean ->
            // Save the preference
            PreferenceUtils.saveBoolean(
                app.getString(R.string.pref_key_never_show_qr_code_instructions),
                isChecked,
                Application.prefs
            )
        }
        val builder = AlertDialog.Builder(activity)
            .setTitle(R.string.qr_code_instructions_title)
            .setCancelable(false)
            .setView(view)
            .setPositiveButton(
                R.string.ok
            ) { dialog: DialogInterface?, which: Int -> IOUtils.openQrCodeReader(activity) }
            .setNegativeButton(
                R.string.not_now
            ) { dialog: DialogInterface?, which: Int -> }
        return builder.create()
    }

    /**
     * Creates a dialog for sharing location and files
     *
     * @param activity
     * @param location
     * @param loggingEnabled true if logging is enabled, false if it is not
     * @param csvFileLogger the file logger being used to log files
     * @param alternateFileUri The URI for a file if a file other than the one current used by the FileLogger should be used (e.g., one previously picked from the folder browse button), or null if no alternate file is chosen and the file from the file logger should be shared.
     * @return a dialog for sharing location and files
     */
    fun showShareFragmentDialog(
        activity: AppCompatActivity, location: Location?,
        loggingEnabled: Boolean, csvFileLogger: CsvFileLogger?,
        jsonFileLogger: JsonFileLogger?, alternateFileUri: Uri?
    ) {
        val files = ArrayList<File>(2)
        if (csvFileLogger != null && csvFileLogger.file != null) {
            files.add(csvFileLogger.file)
        }
        if (jsonFileLogger != null && jsonFileLogger.file != null) {
            files.add(jsonFileLogger.file)
        }
        val fm = activity.supportFragmentManager
        val dialog = ShareDialogFragment()
        val shareListener: ShareDialogFragment.Listener = object : ShareDialogFragment.Listener {
            override fun onLogFileSent() {
                csvFileLogger?.close()
                jsonFileLogger?.close()
            }

            override fun onFileBrowse() {
                dialog.dismiss()
            }
        }
        dialog.setListener(shareListener)
        dialog.arguments =
            createBundleForShareDialog(location, loggingEnabled, files, alternateFileUri)
        dialog.show(fm, ShareDialogFragment.TAG)
    }

    /**
     * Creates a bundle out of the provided variables for passing between fragments
     * @param location
     * @param loggingEnabled
     * @param files
     * @param alternateFileUri
     * @return a bundle out of the provided variables for passing between fragments
     */
    private fun createBundleForShareDialog(
        location: Location?,
        loggingEnabled: Boolean,
        files: ArrayList<File>,
        alternateFileUri: Uri?
    ): Bundle {
        val bundle = Bundle()
        bundle.putParcelable(KEY_LOCATION, location)
        bundle.putBoolean(KEY_LOGGING_ENABLED, loggingEnabled)
        bundle.putSerializable(KEY_LOG_FILES, files)
        bundle.putParcelable(KEY_ALTERNATE_FILE_URI, alternateFileUri)
        return bundle
    }

    fun createHelpDialog(activity: Activity): Dialog {
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.title_help)
        val options = R.array.main_help_options
        builder.setItems(
            options
        ) { dialog: DialogInterface?, which: Int ->
            when (which) {
                0 -> activity.showDialog(
                    LibUIUtils.WHATSNEW_DIALOG
                )
                1 -> activity.startActivity(Intent(activity, HelpActivity::class.java))
            }
        }
        return builder.create()
    }

    fun createWhatsNewDialog(activity: Activity): Dialog {
        val textView = activity.layoutInflater.inflate(R.layout.whats_new_dialog, null) as TextView
        textView.setText(R.string.main_help_whatsnew)
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.main_help_whatsnew_title)
        builder.setIcon(R.mipmap.ic_launcher)
        builder.setView(textView)
        builder.setNeutralButton(
            R.string.main_help_close
        ) { _: DialogInterface?, _: Int -> activity.dismissDialog(LibUIUtils.WHATSNEW_DIALOG) }
        return builder.create()
    }

    fun showFilterDialog(activity: FragmentActivity) {
        val gnssTypes = GnssType.values()
        val len = gnssTypes.size
        val filter = PreferenceUtils.gnssFilter(app, prefs)
        val items = arrayOfNulls<String>(len)
        val checks = BooleanArray(len)

        // For each GnssType, if it is in the enabled list, mark it as checked.
        for (i in 0 until len) {
            val gnssType = gnssTypes[i]
            items[i] = LibUIUtils.getGnssDisplayName(app, gnssType)
            if (filter.contains(gnssType)) {
                checks[i] = true
            }
        }

        // Arguments
        val args = Bundle()
        args.putStringArray(GnssFilterDialog.ITEMS, items)
        args.putBooleanArray(GnssFilterDialog.CHECKS, checks)
        val frag = GnssFilterDialog()
        frag.arguments = args
        frag.show(activity.supportFragmentManager, ".GnssFilterDialog")
    }

    fun showSortByDialog(activity: FragmentActivity) {
        // TODO - convert all dialogs to MaterialAlertDialog (https://material.io/components/dialogs/android#using-dialogs)
        val builder = AlertDialog.Builder(
            activity
        )
        builder.setTitle(R.string.menu_option_sort_by)
        val currentSatOrder = PreferenceUtils.getSatSortOrderFromPreferences(app, Application.prefs)
        builder.setSingleChoiceItems(
            R.array.sort_sats, currentSatOrder
        ) { dialog: DialogInterface, index: Int ->
            LibUIUtils.setSortByClause(app, index, prefs)
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setOwnerActivity(activity)
        dialog.show()
    }
}