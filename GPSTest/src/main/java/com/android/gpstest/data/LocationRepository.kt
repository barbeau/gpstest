package com.android.gpstest.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class LocationRepository @Inject constructor(
    private val sharedLocationManager: SharedLocationManager,
    private val sharedGnssStatusManager: SharedGnssStatusManager,
    private val sharedNmeaManager: SharedNmeaManager,
    private val sharedSensorManager: SharedSensorManager
) {
    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: StateFlow<Boolean> =
        sharedLocationManager.receivingLocationUpdates

    /**
     * Observable flow for location updates
     */
    @ExperimentalCoroutinesApi
    fun getLocations() = sharedLocationManager.locationFlow()

    /**
     * Observable flow for GnssStatus updates
     */
    @ExperimentalCoroutinesApi
    fun getGnssStatus() = sharedGnssStatusManager.statusFlow()

    /**
     * GnssStatus fix state
     */
    val fixState: StateFlow<FixState> = sharedGnssStatusManager.fixState

    /**
     * GnssStatus first fix state
     */
    val firstFixState: StateFlow<FirstFixState> = sharedGnssStatusManager.firstFixState

    /**
     * Observable flow for NMEA updates
     */
    @ExperimentalCoroutinesApi
    fun getNmea() = sharedNmeaManager.nmeaFlow()

    /**
     * Observable flow for orientation sensor updates
     */
    @ExperimentalCoroutinesApi
    fun getSensorUpdates() = sharedSensorManager.sensorFlow()
}
