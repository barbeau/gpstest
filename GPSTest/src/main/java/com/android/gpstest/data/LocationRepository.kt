package com.android.gpstest.data

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val sharedLocationManager: SharedLocationManager
) {
    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: StateFlow<Boolean> = sharedLocationManager.receivingLocationUpdates

    /**
     * Observable flow for location updates
     */
    fun getLocations() = sharedLocationManager.locationFlow()
}
