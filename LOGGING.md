# Data Output and Logging

GPSTest allows you to output raw data about GNSS/GPS to the Android system logs.  The following sections discuss how to access these logs, and details about the data that is output.

## Accessing the system log

You can view the data output from GPSTest by using Android Monitor, which is included with Android Studio.

Steps to view data:

1. Install [Android Studio](https://developer.android.com/studio/index.html)
1. [Enable USB debugging](https://developer.android.com/studio/run/device.html#developer-device-options) on your device
1. In [Android Monitor](https://developer.android.com/studio/profile/android-monitor.html), in the drop down box on far right side, select [`No Filters`](https://developer.android.com/studio/debug/am-logcat.html#filtering).
1. In Android Monitor, in the [search box](https://developer.android.com/studio/debug/am-logcat.html#searching) with magnifying glass, enter `GpsOutput` to filter out all other system output.
1. In the GPSTest app, go to "Settings", scroll down, and make sure the box is checked for each data output type you'd like to see (see next section).

## Data output

Android 2.3 and higher:

* **NMEA output** - NMEA strings.  Enabled by default.  To show only this output in Android Monitor, use the search box text `GpsOutputNmea`.

Android 7.0 and higher:

* **GNSS Measurements** - Raw GNSS satellite measurements observed by the GNSS subsystem.  Disabled by default.  To show only this output in Android Monitor, use the search box text `GpsOutputMeasure`.
* **GNSS Navigation Message** - Navigation messages observed by the GNSS subsystem.  Disabled by default.  To show only this output in Android Monitor, use the search box text `GpsOutputNav`.