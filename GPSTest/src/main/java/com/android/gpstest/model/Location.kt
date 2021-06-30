package com.android.gpstest.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_table")
data class Location(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val time: Long,
        val latitude: Double,
        val longitude: Double,
)