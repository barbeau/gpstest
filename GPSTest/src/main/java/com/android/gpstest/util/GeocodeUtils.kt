/*
 * Copyright (C) 2022 Sean J. Barbeau
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

import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import com.android.gpstest.Application
import com.android.gpstest.model.UserCountry
import com.android.gpstest.ui.share.UploadDeviceInfoFragment
import java.io.IOException

internal object GeocodeUtils {

    /**
     * Returns the UserCountry for the given location.
     */
    fun geocode(location: Location): UserCountry {
        if (Geocoder.isPresent()) {
            val geocoder = Geocoder(Application.app)
            var addresses: List<Address>? = emptyList()
            try {
                addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            } catch (ioe: IOException) {
                Log.e(UploadDeviceInfoFragment.TAG,
                    "Error getting address from location via geocoder: $ioe"
                )
            } catch (iae: IllegalArgumentException) {
                Log.e(UploadDeviceInfoFragment.TAG,
                    "Invalid lat/lon when getting address from location via geocoder: $iae"
                )
            }
            if (!addresses.isNullOrEmpty()) {
                return UserCountry(
                    countryCode = addresses[0].countryCode,
                    countryName = addresses[0].countryName
                )
            }
        }
        return UserCountry()
    }
}