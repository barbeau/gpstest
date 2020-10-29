package com.android.gpstest.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.gpstest.R

class ShareLocationFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.share_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

    }
}