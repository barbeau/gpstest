package com.android.gpstest.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.android.gpstest.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator


class ShareDialogFragment : DialogFragment() {
    private lateinit var shareCollectionAdapter: ShareCollectionAdapter
    private lateinit var viewPager: ViewPager2

    companion object {
        val KEY_LOCATION = "location"
        val KEY_LOGGING_ENABLED = "logging-enabled"
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity!!.layoutInflater.inflate(R.layout.share, null)
        val builder = AlertDialog.Builder(activity!!)
                .setTitle(R.string.share)
                .setView(view)
                .setNeutralButton(R.string.main_help_close) { dialog, which -> }
        shareCollectionAdapter = ShareCollectionAdapter(this)
        viewPager = view.findViewById(R.id.pager)
        viewPager.offscreenPageLimit = 2
        viewPager.adapter = shareCollectionAdapter
        val tabLayout = view.findViewById(R.id.share_tab_layout) as TabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = getString(R.string.location)
                1 -> tab.text = getString(R.string.log)
            }
        }.attach()

        return builder.show()
    }
}

class ShareCollectionAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        when (position) {
            0 -> return ShareLocationFragment()
            1 -> return ShareLogFragment()
        }
        return ShareLocationFragment()
    }
}