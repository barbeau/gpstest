package com.android.gpstest.data.db

import androidx.room.*
import com.android.gpstest.model.Location
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Transaction
    suspend fun updateLocation(location: Location) {
        location.let {
            deleteLocations()
            insertLocation(it)
        }
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLocation(location: Location)

    @Query("DELETE FROM location_table")
    suspend fun deleteLocations()

    @Query("SELECT * FROM location_table ORDER BY time")
    fun getLocations(): Flow<List<Location>>
}
