# Data Output and Logging (GPSTest v4 and higher)

GPSTest allows you to output raw data about GNSS/GPS to files as well as the Android system log, which can be used to visualize the data using tools like Google's [GPS Measurement Tools project](https://github.com/google/gps-measurement-tools) shown below.  The following sections discuss how to access these logs, and details about the data that is output.

![image](https://user-images.githubusercontent.com/928045/64804800-844fae00-d55d-11e9-8212-b0ef65885dc7.png)

## Logging data to files

You can enable file logging in GPSTest via the Settings. By default file logging is turned off for all data to avoid unnecessarily filling up storage space.

Steps to log and view data:
1. In the GPSTest app, go to "Settings", scroll down, tap on "Logging and Output" and under "File Output" make sure the box is checked for each data output type you'd like to see (see next sections).
1. You can share CSV and JSON log files via the "Share" button in the top action bar of the app, or by copying them off internal memory or SD card over USB.

*Note: The file output from GPSTest is buffered, so if you manually copy the file off the device while the app is running you can end up with a partial file if the buffer hasn't been fully flushed. You can force the flush of the buffer by killing the app (e.g., via the Back button), and then if you refresh your file view you should get the complete file with all your data.*

#### Data output - CSV

Here are the details of file logging:
* CSV format - Same file format as the Google [GPS Measurement Tools (GNSS Logger) project](https://github.com/google/gps-measurement-tools) so you can use MATLAB tools under that project to [analyze the data](https://github.com/google/gps-measurement-tools#to-process-a-log-file-you-collected-from-gnsslogger).
* Files are saved to your Android device storage under the `gnss_log` directory.
* After you've enabled logging via Settings, each time you start the app a new file will be created named with the date and time (e.g., `gnss_log_2019_09_11_13_09_50.txt`). If you end the app (e.g., hit back button) and restart it, another file gets created.
* Each row of the file is prefixed with a string designating the data type:
    * `Raw` - Raw GNSS measurements
    * `Fix` - Location fix information
    * `Nav` - Navigation message
    * `NMEA` - NMEA sentences
    * `GnssAntennaInfo` - [Antenna characteristics](https://developer.android.com/reference/android/location/GnssAntennaInfo) for the device model. Available on supported devices (e.g., Pixel 5) with Android 11 and higher.
    * `Status` - [GnssStatus](https://developer.android.com/reference/android/location/GnssStatus) per signal
    * `OrientationDeg` - [Orientation sensor](https://developer.android.com/reference/android/hardware/SensorEvent#values) data

The header of the CSV file explains the format for each data type:

~~~
# Raw GNSS measurements format:
#   Raw,utcTimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,BasebandCn0DbHz,FullInterSignalBiasNanos,FullInterSignalBiasUncertaintyNanos,SatelliteInterSignalBiasNanos,SatelliteInterSignalBiasUncertaintyNanos,CodeType,ChipsetElapsedRealtimeNanos
#
# Location fix format:
#   Fix,Provider,LatitudeDegrees,LongitudeDegrees,AltitudeMeters,SpeedMps,AccuracyMeters,BearingDegrees,UnixTimeMillis,SpeedAccuracyMps,BearingAccuracyDegrees,elapsedRealtimeNanos,VerticalAccuracyMeters,IsMockLocation
#
# Navigation message format:
#   Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)
#
# NMEA format (for [NMEA sentence] format see https://en.wikipedia.org/wiki/NMEA_0183):
#   NMEA,[NMEA sentence],(UTC)TimeInMs
#
# GnssAntennaInfo format (https://developer.android.com/reference/android/location/GnssAntennaInfo):
#   GnssAntennaInfo,CarrierFrequencyMHz,PhaseCenterOffsetXOffsetMm,PhaseCenterOffsetXOffsetUncertaintyMm,PhaseCenterOffsetYOffsetMm,PhaseCenterOffsetYOffsetUncertaintyMm,PhaseCenterOffsetZOffsetMm,PhaseCenterOffsetZOffsetUncertaintyMm,PhaseCenterVariationCorrectionsArray,PhaseCenterVariationCorrectionUncertaintiesArray,PhaseCenterVariationCorrectionsDeltaPhi,PhaseCenterVariationCorrectionsDeltaTheta,SignalGainCorrectionsArray,SignalGainCorrectionUncertaintiesArray,SignalGainCorrectionsDeltaPhi,SignalGainCorrectionsDeltaTheta
#
# GnssStatus format (https://developer.android.com/reference/android/location/GnssStatus):
#   Status,UnixTimeMillis,SignalCount,SignalIndex,ConstellationType,Svid,CarrierFrequencyHz,Cn0DbHz,AzimuthDegrees,ElevationDegrees,UsedInFix,HasAlmanacData,HasEphemerisData,BasebandCn0DbHz
# Orientation sensor format (https://developer.android.com/reference/android/hardware/SensorEvent#values):
#   OrientationDeg,utcTimeMillis,elapsedRealtimeNanos,yawDeg,rollDeg,pitchDeg
~~~

Sample data looks like:

~~~
NMEA,$GNGSA,A,2,66,81,87,,,,,,,,,,1.3,1.0,0.9,2*3F,1568222348217
NMEA,$GNVTG,,T,,M,0.0,N,0.0,K,A*3D,1568222348218
NMEA,$GNGGA,171859.00,2804.281311,N,08225.605044,W,1,07,1.0,39.1,M,-24.8,M,,*75,1568222348220
NMEA,$GNRMC,171859.00,A,2804.281311,N,08225.605044,W,0.0,,110919,3.1,W,A,V*77,1568222348220
Raw,1308593941,1258975580000000,18,,-1320040982834296519-0.42849159240722656,249.35539113357663,-16.601702122755693,22.49701461923959,1417,1,0.0,16399,416758333755552,489,29.8,255.40264892578125,1.5410001277923584,16,0.0,0.0,1575420030,,,,0,,1,0.26,24.8,0.0,0.0,,,C,1308593941103684
Raw,1308593941,1258975580000000,18,,-1320040982834296519-0.42849159240722656,249.35539113357663,-16.601702122755693,22.49701461923959,1417,16,0.0,16399,416758334210563,425,30.0,-752.9444580078125,1.4825000762939453,16,0.0,0.0,1575420030,,,,0,,1,0.26,25.0,0.0,0.0,,,C,1308593941407851
Raw,1308593941,1258975580000000,18,,-1320040982834296519-0.42849159240722656,249.35539113357663,-16.601702122755693,22.49701461923959,1417,22,0.0,16399,416758339876604,278,33.3,-357.5998840332031,1.2899999618530273,16,0.0,0.0,1575420030,,,,0,,1,0.26,28.299999999999997,0.0,0.0,,,C,1308593941522799
Raw,1308593941,1258975580000000,18,,-1320040982834296519-0.42849159240722656,249.35539113357663,-16.601702122755693,22.49701461923959,1417,25,0.0,16399,416758336060464,368,31.7,577.1130981445312,1.4255000352859497,16,0.0,0.0,1575420030,,,,0,,1,0.26,26.7,0.0,0.0,,,C,1308593941624153
Raw,1308593941,1258975580000000,18,,-1320040982834296519-0.42849159240722656,249.35539113357663,-16.601702122755693,22.49701461923959,1417,32,0.0,16399,416758342536604,12,42.5,167.77867126464844,0.047997571527957916,16,0.0,0.0,1575420030,,,,0,,1,0.26,37.5,0.0,0.0,,,C,1308593941724570
Raw,1308593941,1258975580000000,18,,-1320040982834296519-0.42849159240722656,249.35539113357663,-16.601702122755693,22.49701461923959,1417,9,0.0,49359,81940349009787,564,28.4,-236.4537811279297,1.725000023841858,16,0.0,0.0,1600875010,,,,0,,3,-0.68,24.4,3825.078857421875,17.50518035888672,,,C,1308593941811497
GnssAntennaInfo,1575.42,1.2,0.1,3.4,0.2,5.6,0.3,[11.22 33.44 55.66 77.88; 10.2 30.4 50.6 70.8; 12.2 34.4 56.6 78.8],[0.1 0.2 0.3 0.4; 1.1 1.2 1.3 1.4; 2.1 2.2 2.3 2.4],60.0,120.0,[9.8 8.7 7.6 6.5; 5.4 4.3 3.2 2.1; 1.3 2.4 3.5 4.6],[0.11 0.22 0.33 0.44; 0.55 0.66 0.77 0.88; 0.91 0.92 0.93 0.94],60.0,120.0
GnssAntennaInfo,1227.6,3.4,0.2,5.6,0.3,1.2,0.1,[55.66 77.88; 11.22 33.44; 56.6 78.8; 12.2 34.4],[0.3 0.4; 1.1 1.2; 2.1 2.2; 0.1 0.2],180.0,90.0,[7.6 6.5; 5.4 4.3; 1.3 2.4; 9.8 8.7],[0.91 0.92; 0.55 0.66; 0.11 0.22; 0.93 0.94],180.0,90.0
OrientationDeg,1637264740657,1308593196466709,123.92464891404495,0.3803144045376981,-0.3826964863715929
OrientationDeg,1637264740662,1308593201310095,123.9245396310182,0.38081343523393213,-0.383448687634367
OrientationDeg,1637264740667,1308593206153428,123.92448498950489,0.3812450711729652,-0.3836270729655807
Status,0,0,13,1,1,1575420032,25.5,273.0,14.0,0,1,0,20.5
Status,0,1,13,1,16,1575420032,24.7,193.0,17.0,0,1,0,19.7
Status,0,2,13,1,22,1575420032,27.5,304.0,40.0,0,1,0,22.5
Status,0,3,13,1,25,1575420032,28.3,42.0,19.0,0,1,0,23.3
Status,0,4,13,1,26,1575420032,22.6,174.0,55.0,0,1,0,17.6
Status,0,5,13,1,32,1575420032,38.1,74.0,47.0,0,1,1,33.1
Fix,gps,28.07124449,-82.42663169,-16.32806396484375,0.0,21.196445,0.0,1637264742000,1.9549425,,1308594915408632,13.918213,0
~~~

Google's documentation for the [Smartphone Decimeter Challenge](https://www.kaggle.com/c/google-smartphone-decimeter-challenge/data#) has additional information.

Here are a few more additional pieces of information on the following formats, which is copied from the above page.

`Raw` - The raw GNSS measurements of one GNSS signal (each satellite may have 1-2 signals for L5-enabled smartphones), collected from the Android API GnssMeasurement:
* utcTimeMillis - Milliseconds since UTC epoch (1970/1/1), converted from GnssClock
* TimeNanos - The GNSS receiver internal hardware clock value in nanoseconds.
* LeapSecond - The leap second associated with the clock's time.
* TimeUncertaintyNanos - The clock's time uncertainty (1-sigma) in nanoseconds.
* FullBiasNanos - The difference between hardware clock getTimeNanos() inside GPS receiver and the true GPS time since 0000Z, January 6, 1980, in nanoseconds.
* BiasNanos - The clock's sub-nanosecond bias.
* BiasUncertaintyNanos - The clock's bias uncertainty (1-sigma) in nanoseconds.
* DriftNanosPerSecond - The clock's drift in nanoseconds per second.
* DriftUncertaintyNanosPerSecond - The clock's drift uncertainty (1-sigma) in nanoseconds per second.
* HardwareClockDiscontinuityCount - Count of hardware clock discontinuities.
* Svid - The satellite ID. More info can be found here.
* TimeOffsetNanos - The time offset at which the measurement was taken in nanoseconds.
* State - Integer signifying sync state of the satellite. Each bit in the integer attributes to a particular state information of the measurement. See the metadata/raw_state_bit_map.json file for the mapping between bits and states.
* ReceivedSvTimeNanos - The received GNSS satellite time, at the measurement time, in nanoseconds.
* ReceivedSvTimeUncertaintyNanos - The error estimate (1-sigma) for the received GNSS time, in nanoseconds.
* Cn0DbHz - The carrier-to-noise density in dB-Hz.
* PseudorangeRateMetersPerSecond - The pseudorange rate at the timestamp in m/s.
* PseudorangeRateUncertaintyMetersPerSecond - The pseudorange's rate uncertainty (1-sigma) in m/s.
* AccumulatedDeltaRangeState - This indicates the state of the 'Accumulated Delta Range' measurement. Each bit in the integer attributes to state of the measurement. See the metadata/accumulated_delta_range_state_bit_map.json file for the mapping between bits and states.
* AccumulatedDeltaRangeMeters - The accumulated delta range since the last channel reset, in meters.
* AccumulatedDeltaRangeUncertaintyMeters - The accumulated delta range's uncertainty (1-sigma) in meters.
* CarrierFrequencyHz - The carrier frequency of the tracked signal.
* CarrierCycles - The number of full carrier cycles between the satellite and the receiver. Null in these datasets.
* CarrierPhase - The RF phase detected by the receiver. Null in these datasets.
* CarrierPhaseUncertainty - The carrier-phase's uncertainty (1-sigma). Null in these datasets.
* MultipathIndicator - A value indicating the 'multipath' state of the event.
* SnrInDb - The (post-correlation & integration) Signal-to-Noise ratio (SNR) in dB.
* ConstellationType - GNSS constellation type. It's an integer number, whose mapping to string value is provided in the constellation_type_mapping.csv file.
* AgcDb - The Automatic Gain Control level in dB.
* BasebandCn0DbHz - The baseband carrier-to-noise density in dB-Hz. Only available in Android 11.
* FullInterSignalBiasNanos - The GNSS measurement's inter-signal bias in nanoseconds with sub-nanosecond accuracy. Only available in Android 11.
* FullInterSignalBiasUncertaintyNanos - The GNSS measurement's inter-signal bias uncertainty (1 sigma) in nanoseconds with sub-nanosecond accuracy. Only available in Android 11.
* SatelliteInterSignalBiasNanos - The GNSS measurement's satellite inter-signal bias in nanoseconds with sub-nanosecond accuracy. Only available in Android 11.
* SatelliteInterSignalBiasUncertaintyNanos - The GNSS measurement's satellite inter-signal bias uncertainty (1 sigma) in nanoseconds with sub-nanosecond accuracy. Only available in Android 11.
* CodeType - The GNSS measurement's code type. Only available in recent logs.
* ChipsetElapsedRealtimeNanos - The elapsed real-time of this clock since system boot, in nanoseconds. Only available in recent logs.

`Status` - The status of a GNSS signal, as collected from the Android API GnssStatus.
* UnixTimeMillis - Milliseconds since UTC epoch (1970/1/1), reported from the last location changed by GPS provider.
* SignalCount - The total number of satellites in the satellite list.
* SignalIndex - The index of current signal.
* ConstellationType: The constellation type of the satellite at the specified index.
* Svid: The satellite ID.
* CarrierFrequencyHz: The carrier frequency of the signal tracked.
* Cn0DbHz: The carrier-to-noise density at the antenna of the satellite at the specified index in dB-Hz.
* AzimuthDegrees: The azimuth the satellite at the specified index.
* ElevationDegrees: The elevation of the satellite at the specified index.
* UsedInFix: Whether the satellite at the specified index was used in the calculation of the most recent position fix (`0` or `1`).
* HasAlmanacData: Whether the satellite at the specified index has almanac data (`0` or `1`).
* HasEphemerisData: Whether the satellite at the specified index has ephemeris data (`0` or `1`).
* BasebandCn0DbHz: The baseband carrier-to-noise density of the satellite at the specified index in dB-Hz.

`OrientationDeg` - Each row represents an estimated device orientation, collected from Android API SensorManager#getOrientation:
* utcTimeMillis - The sum of elapsedRealtimeNanos below and the estimated device boot time at UTC, after a recent NTP (Network Time Protocol) sync.
* elapsedRealtimeNanos - The time in nanoseconds at which the event happened.
* yawDeg - If the screen is in portrait mode, this value equals the Azimuth degree (modulus to 0° and 360°). If the screen is in landscape mode, it equals the sum (modulus to 0° and 360°) of the screen rotation angle (either 90° or 270°) and the Azimuth degree. Azimuth, refers to the angle of rotation about the -z axis. This value represents the angle between the device's y axis and the magnetic north pole.
* rollDeg - Roll, angle of rotation about the y axis. This value represents the angle between a plane perpendicular to the device's screen and a plane perpendicular to the ground.
* pitchDeg - Pitch, angle of rotation about the x axis. This value represents the angle between a plane parallel to the device's screen and a plane parallel to the ground.

#### Data output - JSON

[GnssAntennaInfo](https://developer.android.com/reference/android/location/GnssAntennaInfo) logging is available on supported devices (e.g., Pixel 5) with Android 11 and is also logged in the JSON format. GNSS antenna(s) characteristics, such as phase center offset (PCO) coordinates, phase center variation (PCV) corrections, and signal gain corrections can be applied to the raw measurements to improve accuracy.

Logging works similar to the CSV file process, with a file name like `gnss_log_2019_09_11_13_09_50.json`. Here's example data from a Pixel 5:

~~~
[
   {
      "carrierFrequencyMHz":1575.42,
      "phaseCenterOffset":{
         "xoffsetMm":1.2,
         "xoffsetUncertaintyMm":0.1,
         "yoffsetMm":3.4,
         "yoffsetUncertaintyMm":0.2,
         "zoffsetMm":5.6,
         "zoffsetUncertaintyMm":0.3
      },
      "phaseCenterVariationCorrections":{
         "correctionUncertaintiesArray":[
            [ 0.1, 0.2, 0.3, 0.4 ],
            [ 1.1, 1.2, 1.3, 1.4 ],
            [ 2.1, 2.2, 2.3, 2.4 ]
         ],
         "correctionsArray":[
            [ 11.22, 33.44, 55.66, 77.88 ],
            [ 10.2, 30.4, 50.6, 70.8 ],
            [ 12.2, 34.4, 56.6, 78.8 ]
         ],
         "deltaPhi":60.0,
         "deltaTheta":120.0
      },
      "signalGainCorrections":{
         "correctionUncertaintiesArray":[
            [ 0.11, 0.22, 0.33, 0.44 ],
            [ 0.55, 0.66, 0.77, 0.88 ],
            [ 0.91, 0.92, 0.93, 0.94 ]
         ],
         "correctionsArray":[
            [ 9.8, 8.7, 7.6, 6.5 ],
            [ 5.4, 4.3, 3.2, 2.1 ],
            [ 1.3, 2.4, 3.5, 4.6 ]
         ],
         "deltaPhi":60.0,
         "deltaTheta":120.0
      }
   }
   ...
]
~~~

#### Data output - RINEX

Are you interested in data in the RINEX format? GPSTest doesn't natively support RINEX, but you can use the below tool to convert from the above CSV format to RINEX:
https://github.com/rokubun/android_rinex

#### Data Analysis

Use the CSV file output from GPSTest along with the Google [GPS Measurement Tools project](https://github.com/google/gps-measurement-tools) to analyze the data (as of late October 2020 supported for [GnssAntennaInfo](https://developer.android.com/reference/android/location/GnssAntennaInfo) doesn't seem to exist).

For details see the Android documentation:
* ["GNSS Measurements - Analyzing raw measurements"](https://developer.android.com/guide/topics/sensors/gnss#analyze)

Here's a screenshot from the GPS Measurement Tools desktop software:

![image](https://user-images.githubusercontent.com/928045/64804800-844fae00-d55d-11e9-8212-b0ef65885dc7.png)

## Accessing the system log via Android Studio

You can view the data output from GPSTest by using Android Monitor, which is included with Android Studio.

Steps to view data:

1. Install [Android Studio](https://developer.android.com/studio/index.html)
1. [Enable USB debugging](https://developer.android.com/studio/run/device.html#developer-device-options) on your device
1. In [Android Monitor](https://developer.android.com/studio/profile/android-monitor.html), in the drop down box on far right side, select [`No Filters`](https://developer.android.com/studio/debug/am-logcat.html#filtering).
1. In Android Monitor, in the [search box](https://developer.android.com/studio/debug/am-logcat.html#searching) with magnifying glass, enter `GpsOutput` to filter out all other system output.
1. In the GPSTest app, go to "Settings", scroll down, tap on "Logging and Output" and under "Android Monitor Output" make sure the box is checked for each data output type you'd like to see (see next section).

#### Data output

Android 2.3 and higher:

* **NMEA output** - NMEA strings.  Enabled by default for Android Studio.  To show only this output in Android Monitor, use the search box text `GpsOutputNmea`.

Android 7.0 and higher:

* **GNSS Measurements** - Raw GNSS satellite measurements observed by the GNSS subsystem.  Disabled by default for Android Studio.  To show only this output in Android Monitor, use the search box text `GpsOutputMeasure`.
* **GNSS Navigation Message** - Navigation messages observed by the GNSS subsystem.  Disabled by default for Android Studio.  To show only this output in Android Monitor, use the search box text `GpsOutputNav`.

Android 12 and higher:
* `IsMockLocation` for location fixes

## What devices support pseudorange measurements and navigation messages?

Check out the [FAQ](https://github.com/barbeau/gpstest/blob/master/FAQ.md#what-android-70-devices-support-logging-raw-pseudorange-and-navigation-messages) for known device information.

## Where can I find logging documentation for older versions of GPSTest (v3 and lower)?

The documentation on this page is for GPSTest v4 and higher. Check out [this page](/LOGGING-v3.md).