/*
 * Copyright 2019-2021 Google LLC, Sean Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest.library.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.location.LocationManager
import android.util.Log
import android.view.Display
import android.view.Surface
import com.android.gpstest.library.R
import com.android.gpstest.library.model.Orientation
import com.android.gpstest.library.util.MathUtils
import com.android.gpstest.library.util.SatelliteUtils
import com.android.gpstest.library.util.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch


private const val TAG = "SharedSensorManager"

/**
 * Wraps rotation sensor updates in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedSensorManager constructor(
    private val prefs: SharedPreferences,
    private val context: Context,
    externalScope: CoroutineScope,
) {
    private val ROT_VECTOR_SENSOR_DELAY_MICROS = 10 * 1000 // 100Hz updates

    // Holds sensor data
    private val rotationMatrix = FloatArray(16)
    private val remappedMatrix = FloatArray(16)
    private val values = FloatArray(3)
    private val truncatedRotationVector = FloatArray(4)
    private var truncateVector = false
    private lateinit var geomagneticField: GeomagneticField

    init {
        externalScope.launch {
            while (!::geomagneticField.isInitialized) {
                initMagField()
                if (!::geomagneticField.isInitialized) delay(3000)
            }
        }
    }

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _sensorUpdates = callbackFlow {
        val callback: SensorEventListener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    var orientationX: Double
                    var tiltY = Double.NaN
                    var yawZ = Double.NaN

                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            // Modern rotation vector sensors
                            maybeTruncateVector(event)

                            val display = getDisplay()
                            if (display != null) handleRotation(display.rotation)
                            orientationX = Math.toDegrees(values[0].toDouble()) // azimuth
                            tiltY = Math.toDegrees(values[1].toDouble())
                            yawZ = Math.toDegrees(values[2].toDouble())
                        }
                        Sensor.TYPE_ORIENTATION ->
                            // Legacy orientation sensors
                            orientationX = event.values[0].toDouble()
                        else ->
                            // A sensor we're not using, so return
                            return
                    }

                    // Correct for true north, if preference is set
                    if (::geomagneticField.isInitialized && prefs.getBoolean(
                            context.getString(R.string.pref_key_true_north),
                            true
                        )
                    ) {
                        orientationX += geomagneticField.declination.toDouble()
                        // Make sure value is between 0-360
                        orientationX = MathUtils.mod(orientationX, 360.0)
                    }

                    //Log.d(TAG, "New sensor: $orientationX and $tiltY")
                    // Send the new sensors to the Flow observers
                    trySend(Orientation(event.timestamp, doubleArrayOf(orientationX, tiltY, yawZ)))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // No op
                }
            }

        Log.d(TAG, "Starting sensor updates")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        try {
            if (SatelliteUtils.isRotationVectorSensorSupported(context)) {
                // Use the modern rotation vector sensors
                val vectorSensor: Sensor =
                    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                sensorManager.registerListener(
                    callback,
                    vectorSensor,
                    ROT_VECTOR_SENSOR_DELAY_MICROS
                )
            } else if (SatelliteUtils.isOrientationSensorSupported(context)) {
                // Use the legacy orientation sensors
                val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
                if (sensor != null) {
                    sensorManager.registerListener(
                        callback,
                        sensor,
                        ROT_VECTOR_SENSOR_DELAY_MICROS
                )
            } else {
                // No sensors to observe
                    Log.e(TAG, "Device doesn't support sensor TYPE_ROTATION_VECTOR or TYPE_ORIENTATION")
                close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in sensor flow: $e")
            close(e) // in case of exception, close the Flow
        }

        awaitClose {
            Log.d(TAG, "Stopping sensor updates")
            sensorManager.unregisterListener(callback) // clean up when Flow collection ends
        }
    }.shareIn(
        externalScope,
        replay = 0,
        started = SharingStarted.WhileSubscribed()
    )

    @SuppressLint("MissingPermission")
    private fun initMagField() {
        Log.d(TAG, "Initializing Mag Field...")
        if (::geomagneticField.isInitialized) return

        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
            !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        ) return

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (!::geomagneticField.isInitialized && lastLocation != null) {
            geomagneticField = GeomagneticField(
                lastLocation.latitude.toFloat(),
                lastLocation.longitude.toFloat(), lastLocation.altitude.toFloat(),
                lastLocation.time
            )
            Log.d(TAG, "Mag Field initialized")
        }
    }

    private fun maybeTruncateVector(event: SensorEvent) {
        if (!truncateVector) {
            try {
                SensorManager.getRotationMatrixFromVector(
                    rotationMatrix,
                    event.values
                )
            } catch (e: IllegalArgumentException) {
                // On some Samsung devices, an exception is thrown if this vector > 4 (see #39)
                // Truncate the array, since we can deal with only the first four values
                Log.e(
                    TAG,
                    "Samsung device error? Will truncate vectors - $e"
                )
                truncateVector = true
                // Do the truncation here the first time the exception occurs
                getRotationMatrixFromTruncatedVector(event.values)
            }
        } else {
            // Truncate the array to avoid the exception on some devices (see #39)
            getRotationMatrixFromTruncatedVector(event.values)
        }
    }

    private fun getRotationMatrixFromTruncatedVector(vector: FloatArray) {
        System.arraycopy(vector, 0, truncatedRotationVector, 0, 4)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, truncatedRotationVector)
    }

    private fun getDisplay(): Display? {
        val displayManager =
            context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        return displayManager.getDisplay(0)
    }

    private fun handleRotation(rotation: Int) {
        when (rotation) {
            Surface.ROTATION_0 ->
                // No orientation change, use default coordinate system
                SensorManager.getOrientation(
                    rotationMatrix,
                    values
                )
            Surface.ROTATION_90 -> {
                // Log.d(MODE_MAP, "Rotation-90");
                SensorManager.remapCoordinateSystem(
                    rotationMatrix, SensorManager.AXIS_Y,
                    SensorManager.AXIS_MINUS_X, remappedMatrix
                )
                SensorManager.getOrientation(
                    remappedMatrix,
                    values
                )
            }
            Surface.ROTATION_180 -> {
                // Log.d(MODE_MAP, "Rotation-180");
                SensorManager
                    .remapCoordinateSystem(
                        rotationMatrix, SensorManager.AXIS_MINUS_X,
                        SensorManager.AXIS_MINUS_Y, remappedMatrix
                    )
                SensorManager.getOrientation(
                    remappedMatrix,
                    values
                )
            }
            Surface.ROTATION_270 -> {
                // Log.d(MODE_MAP, "Rotation-270");
                SensorManager
                    .remapCoordinateSystem(
                        rotationMatrix, SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_X, remappedMatrix
                    )
                SensorManager.getOrientation(
                    remappedMatrix,
                    values
                )
            }
            else ->
                // This shouldn't happen - assume default orientation
                SensorManager.getOrientation(
                    rotationMatrix,
                    values
                )
        }
    }

    /**
     * A flow of sensor orientations
     */
    @ExperimentalCoroutinesApi
    fun sensorFlow(): Flow<Orientation> {
        return _sensorUpdates
    }
}