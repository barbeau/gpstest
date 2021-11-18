# Data Output and Logging - v3 and lower

**NOTE** This page contains logging information for older versions of GPSTest (v3 and lower).

***Please see [the main Logging documentation](/LOGGING.md) for the current version of GPSTest.***

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

The header of the CSV file explains the format for each data type:

~~~
# Raw GNSS measurements format (Android 7.0 and higher on supported devices):
#   Raw,ElapsedRealtimeMillis,TimeNanos,LeapSecond,TimeUncertaintyNanos,FullBiasNanos,BiasNanos,BiasUncertaintyNanos,DriftNanosPerSecond,DriftUncertaintyNanosPerSecond,HardwareClockDiscontinuityCount,Svid,TimeOffsetNanos,State,ReceivedSvTimeNanos,ReceivedSvTimeUncertaintyNanos,Cn0DbHz,PseudorangeRateMetersPerSecond,PseudorangeRateUncertaintyMetersPerSecond,AccumulatedDeltaRangeState,AccumulatedDeltaRangeMeters,AccumulatedDeltaRangeUncertaintyMeters,CarrierFrequencyHz,CarrierCycles,CarrierPhase,CarrierPhaseUncertainty,MultipathIndicator,SnrInDb,ConstellationType,AgcDb,CarrierFrequencyHz
# 
# Location fix format (all devices):
#   Fix,Provider,Latitude,Longitude,Altitude,Speed,Accuracy,(UTC)TimeInMs
# 
# Navigation message format (Android 7.0 and higher on supported devices):
#   Nav,Svid,Type,Status,MessageId,Sub-messageId,Data(Bytes)
# 
# NMEA format (Android 2.3 and higher) (for [NMEA sentence] format see https://www.gpsinformation.org/dale/nmea.htm):
#   NMEA,[NMEA sentence],timestamp
#
# GnssAntennaInfo format (https://developer.android.com/reference/android/location/GnssAntennaInfo):
#   GnssAntennaInfo,CarrierFrequencyMHz,PhaseCenterOffsetXOffsetMm,PhaseCenterOffsetXOffsetUncertaintyMm,PhaseCenterOffsetYOffsetMm,PhaseCenterOffsetYOffsetUncertaintyMm,PhaseCenterOffsetZOffsetMm,PhaseCenterOffsetZOffsetUncertaintyMm,PhaseCenterVariationCorrectionsArray,PhaseCenterVariationCorrectionUncertaintiesArray,PhaseCenterVariationCorrectionsDeltaPhi,PhaseCenterVariationCorrectionsDeltaTheta,SignalGainCorrectionsArray,SignalGainCorrectionUncertaintiesArray,SignalGainCorrectionsDeltaPhi,SignalGainCorrectionsDeltaTheta
~~~

Sample data looks like:

~~~
NMEA,$GNGSA,A,2,66,81,87,,,,,,,,,,1.3,1.0,0.9,2*3F,1568222348217
NMEA,$GNVTG,,T,,M,0.0,N,0.0,K,A*3D,1568222348218
NMEA,$GNGGA,171859.00,2804.281311,N,08225.605044,W,1,07,1.0,39.1,M,-24.8,M,,*75,1568222348220
NMEA,$GNRMC,171859.00,A,2804.281311,N,08225.605044,W,0.0,,110919,3.1,W,A,V*77,1568222348220
Raw,1257164406,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,2,0.0,207,73139369397323,271,22.8,-556.3143920898438,10.200000762939453,0,0.0,0.0,,,,,0,,3,,
Raw,1257164408,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,23,0.0,207,73139369842860,1523,19.3,182.0135955810547,10.510000228881836,0,0.0,0.0,,,,,0,,3,,
Raw,1257164411,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,17,0.0,207,73139361386059,1192,21.2,-592.7639770507812,10.278000831604004,0,0.0,0.0,,,,,0,,3,,
Raw,1257164412,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,5,0.0,15,321557370216944,1041,20.3,208.6379852294922,9.641500473022461,0,0.0,0.0,,,,,0,,1,,
Raw,1257164413,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,6,0.0,15,321557356784894,926,21.9,394.1838073730469,9.560500144958496,0,0.0,0.0,,,,,0,,1,,
Raw,1257164413,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,12,0.0,15,321557361694004,825,22.2,557.084716796875,9.458000183105469,0,0.0,0.0,,,,,0,,1,,
Raw,1257164414,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,13,0.0,15,321557363597446,1504,17.2,-508.64544677734375,9.940999984741211,0,0.0,0.0,,,,,0,,1,,
Raw,1257164415,126692640000000,,,-1252130864797923510,0.9759163856506348,571.2438141927123,74.13293543922987,35.96258578603258,729,15,0.0,15,321557358793297,826,22.0,-689.0819702148438,9.483500480651855,0,0.0,0.0,,,,,0,,1,,
Fix,gps,28.071355,-82.426751,14.320496,0.000000,38.592003,1568222340000
GnssAntennaInfo,1575.42,1.2,0.1,3.4,0.2,5.6,0.3,[11.22 33.44 55.66 77.88; 10.2 30.4 50.6 70.8; 12.2 34.4 56.6 78.8],[0.1 0.2 0.3 0.4; 1.1 1.2 1.3 1.4; 2.1 2.2 2.3 2.4],60.0,120.0,[9.8 8.7 7.6 6.5; 5.4 4.3 3.2 2.1; 1.3 2.4 3.5 4.6],[0.11 0.22 0.33 0.44; 0.55 0.66 0.77 0.88; 0.91 0.92 0.93 0.94],60.0,120.0
GnssAntennaInfo,1227.6,3.4,0.2,5.6,0.3,1.2,0.1,[55.66 77.88; 11.22 33.44; 56.6 78.8; 12.2 34.4],[0.3 0.4; 1.1 1.2; 2.1 2.2; 0.1 0.2],180.0,90.0,[7.6 6.5; 5.4 4.3; 1.3 2.4; 9.8 8.7],[0.91 0.92; 0.55 0.66; 0.11 0.22; 0.93 0.94],180.0,90.0
~~~

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

## What devices support pseudorange measurements and navigation messages?

Check out the [FAQ](https://github.com/barbeau/gpstest/blob/master/FAQ.md#what-android-70-devices-support-logging-raw-pseudorange-and-navigation-messages) for known device information.