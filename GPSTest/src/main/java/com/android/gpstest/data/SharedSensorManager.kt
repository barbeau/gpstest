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
package com.android.gpstest.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.*
import android.location.LocationManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import com.android.gpstest.Application
import com.android.gpstest.R
import com.android.gpstest.model.OrientationAndTilt
import com.android.gpstest.util.MathUtils
import com.android.gpstest.util.SatelliteUtils
import com.android.gpstest.util.hasPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.shareIn


private const val TAG = "SharedLocationManager"

/**
 * Wraps rotation sensor updates in callbackFlow
 *
 * Derived in part from https://github.com/android/location-samples/blob/main/LocationUpdatesBackgroundKotlin/app/src/main/java/com/google/android/gms/location/sample/locationupdatesbackgroundkotlin/data/MyLocationManager.kt
 * and https://github.com/googlecodelabs/kotlin-coroutines/blob/master/ktx-library-codelab/step-06/myktxlibrary/src/main/java/com/example/android/myktxlibrary/LocationUtils.kt
 */
class SharedSensorManager constructor(
    private val context: Context,
    externalScope: CoroutineScope
) {
    // Holds sensor data
    private val rotationMatrix = FloatArray(16)
    private val remappedMatrix = FloatArray(16)
    private val values = FloatArray(3)
    private val truncatedRotationVector = FloatArray(4)
    private var truncateVector = false
    private lateinit var geomagneticField: GeomagneticField

    @ExperimentalCoroutinesApi
    @SuppressLint("MissingPermission")
    private val _sensorUpdates = callbackFlow {
        initMagField()
        val callback: SensorEventListener =
            object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    var orientation: Double
                    var tilt = Double.NaN

                    when (event.sensor.type) {
                        Sensor.TYPE_ROTATION_VECTOR -> {
                            // Modern rotation vector sensors
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

                            val display: Display? =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    context.display
                                } else {
                                    val windowManager = context
                                        .getSystemService(Context.WINDOW_SERVICE) as WindowManager
                                    windowManager.defaultDisplay
                                }

                            when (display?.rotation) {
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
                            orientation =
                                Math.toDegrees(values[0].toDouble()) // azimuth
                            tilt = Math.toDegrees(values[1].toDouble())
                        }
                        Sensor.TYPE_ORIENTATION ->
                            // Legacy orientation sensors
                            orientation = event.values[0].toDouble()
                        else ->
                            // A sensor we're not using, so return
                            return
                    }

                    // Correct for true north, if preference is set
                    // TODO - move this to a retrying coroutine on class init for efficiency
                    if (!::geomagneticField.isInitialized) {
                        initMagField()
                    }
                    if (::geomagneticField.isInitialized && Application.getPrefs().getBoolean(
                            Application.get().getString(R.string.pref_key_true_north),
                            true
                        )
                    ) {
                        orientation += geomagneticField.declination.toDouble()
                        // Make sure value is between 0-360
                        orientation = MathUtils.mod(orientation.toFloat(), 360.0f).toDouble()
                    }

                    Log.d(TAG, "New sensor: $orientation and $tilt")
                    // Send the new sensors to the Flow observers
                    trySend(OrientationAndTilt(orientation, tilt))
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    // No op
                }
            }

        Log.d(TAG, "Starting sensor updates")

        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        try {
            if (SatelliteUtils.isRotationVectorSensorSupported(Application.get())) {
                // Use the modern rotation vector sensors
                val vectorSensor: Sensor =
                    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                sensorManager.registerListener(
                    callback,
                    vectorSensor,
                    SensorManager.SENSOR_DELAY_FASTEST
                )
            } else {
                // Use the legacy orientation sensors
                val sensor: Sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
                sensorManager.registerListener(
                    callback,
                    sensor,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }
        } catch (e: Exception) {
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
        }
    }

    private fun getRotationMatrixFromTruncatedVector(vector: FloatArray) {
        System.arraycopy(vector, 0, truncatedRotationVector, 0, 4)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, truncatedRotationVector)
    }

    @ExperimentalCoroutinesApi
    fun sensorFlow(): Flow<OrientationAndTilt> {
        return _sensorUpdates
    }
}