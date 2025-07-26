package com.android.gpstest.ui.share

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.gpstest.R
import com.android.gpstest.ui.share.ShareLogFragment.Listener
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class ShareDialogFragment : DialogFragment() {
    private lateinit var shareCollectionAdapter: ShareCollectionAdapter
    private lateinit var viewPager: ViewPager2
    private lateinit var listener: Listener

    companion object {
        val TAG = "ShareDialogFragment"
        val KEY_LOCATION = "location"
        val KEY_LOGGING_ENABLED = "logging-enabled"
        val KEY_ALTERNATE_FILE_URI = "alternate-file-uri"
        val KEY_LOG_FILES = "log-file"
    }

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

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = requireActivity().layoutInflater.inflate(R.layout.share, null)
        setRetainInstance(true)
        val builder = AlertDialog.Builder(requireActivity())
                .setTitle(R.string.share)
                .setView(view)
                .setNeutralButton(R.string.main_help_close) { dialog, _ -> }
        shareCollectionAdapter = ShareCollectionAdapter(this)
        shareCollectionAdapter.setArguments(arguments)
        if (this::listener.isInitialized) {
            shareCollectionAdapter.setListener(listener)
        }
        viewPager = view.findViewById(R.id.pager)
        viewPager.offscreenPageLimit = 2
        viewPager.adapter = shareCollectionAdapter
        val tabLayout = view.findViewById(R.id.share_tab_layout) as TabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = getString(R.string.location)
                    tab.setIcon(R.drawable.ic_baseline_location_on_24)
                }
                1 -> {
                    tab.text = getString(R.string.log)
                    tab.setIcon(R.drawable.ic_baseline_list_alt_24)
                }
                2 -> {
                    tab.text = getString(R.string.device)
                    tab.setIcon(R.drawable.ic_baseline_perm_device_information_24)
                }
            }
        }.attach()
        val alternateFileUri = arguments?.getParcelable<Uri>(KEY_ALTERNATE_FILE_URI)
        if (alternateFileUri != null) {
            // If the user picked a file from the browser, go back to the file logging tab
            viewPager.setCurrentItem(1, false)
        }

        return builder.show()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
        if (this::shareCollectionAdapter.isInitialized) {
            shareCollectionAdapter.setListener(listener)
        }
    }
}

class ShareCollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private var arguments: Bundle? = null
    private var listener: ShareDialogFragment.Listener? = null

    fun setArguments(arguments: Bundle?) {
        this.arguments = arguments
    }

    fun setListener(listener: ShareDialogFragment.Listener) {
        this.listener = listener
    }

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> {
                val fragment = ShareLocationFragment()
                fragment.arguments = arguments
                return fragment
            }
            1 -> {
                val fragment = ShareLogFragment()
                fragment.arguments = arguments
                fragment.setListener(object : Listener {
                    override fun onLogFileSent() {
                        listener?.onLogFileSent()
                    }

                    override fun onFileBrowse() {
                        listener?.onFileBrowse()
                    }
                })
                return fragment
            }
            2 -> {
                val fragment = UploadDeviceInfoFragment()
                fragment.arguments = arguments
                return fragment
            }
        }
        val fragment =  ShareLocationFragment()
        fragment.arguments = arguments
        return fragment
    }
}