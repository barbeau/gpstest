package com.android.gpstest.data

import androidx.annotation.WorkerThread
import com.android.gpstest.data.db.LocationDao
import com.android.gpstest.model.Location
import javax.inject.Inject

class LocationRepository @Inject constructor(
        private val locationDao: LocationDao
) {

    fun getLocations() = locationDao.getLocations()

    // By default Room runs suspend queries off the main thread, therefore, we don't need to
    // implement anything else to ensure we're not doing long running database work
    // off the main thread.
    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun updateLocation(location: Location) {
        locationDao.updateLocation(location)
    }
}
