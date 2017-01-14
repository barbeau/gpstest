# Frequently Asked Questions (FAQ)

Want to know more about the data that GPSTest collects?  See some common questions below.

## What's the purpose of GPSTest?

The purpose of GPSTest is to give application developers and platform implementers an open-source reference application that demonstrates how GPS works on Android devices.  Its goal is to advance the quality and smart use of GPS technology in mobile devices and mobile apps.  It also allows curious users to better understand GPS on their Android device.

## What satellite constellations does GPSTest support?

* GPS (USA Navstar)
* GLONASS (Russia)
* QZSS (Japan)
* BeiDou/COMPASS (China)
* Galileo (European Union)

Galileo support has been confirmed on the following devices:

* [BQ Aquaris X5 Plus](https://groups.google.com/forum/m/#!topic/gpstest_android/SbvcUyGU67U)
* [Huawei Mate 9](http://www.startlr.com/we-tried-galileo-huawei-mate-9-already-sees-the-european-satellites/)

## What Android devices does GPSTest run on?

<img src="https://dl.dropboxusercontent.com/u/46443835/GPSTest/GPSTestDeviceMenu.png" width="287" height="498" align=right />

Android 1.5 and up, in its simplest form.  More advanced versions with an updated user interface and better maps interface (based on Android Maps API v2) is available on Android 2.2 and up.  NMEA logging is available on Android 2.3 and up.  GNSS measurements and navigation message logging is available on Android 7.0 and up.

## What are the menu buttons for?

* **Start/Stop** - Start/stop the GPS hardware
* **Send Location** - After a latitude and longitude has been acquired, you can share this info
* **Inject Time** - Injects Time assistance data for GPS into the platform, using information from a [Network Time Protocol (NTP)](http://support.ntp.org/bin/view/Main/WebHome) server
* **Inject XTRA Data** - Injects [XTRA assistance data](http://goo.gl/3RjWX) for GPS into the platform, using information from a XTRA server
* **Clear Aiding Data** - Clears all assistance data used for GPS, including NTP and XTRA data (Note: if you select this option to fix broken GPS on your device, for GPS to work again you may need to ‘Inject Time’ and ‘Inject XTRA’ data)
* **Settings** - Set map tile type

## What information is shown for each location fix?

* Latitude and Longitude (in decimal degrees), and Altitude (in meters)
* Timestamp (Relative to the current time)
* Speed (meters/sec)
* Bearing (i.e., Heading) in 0-359 degrees
* Estimated Horizontal Accuracy (i.e., how accurate the positioning technology thinks the position is)
* For each satellite observed by the device:
  * Pseudorandom Noise  (PRN) code, or satellite ID
  * Signal-to-Noise Ratio (SNR) on Android 6.0 and lower, and carrier-to-noise density (C/N0) on Android 7.0 and up
  * Elevation (in degrees)
  * Azimuth (in degrees)
  * Flags - "E"-flag is shown if the GNSS engine has the ephemeris data for the satellite, the "A"-flag is shown if the GPS engine has almanac data for the satellite, and the "U"-flag is shown if the satellite was used by the GPS engine when calculating the most recent GPS fix

See the Android [GpsSatellite documentation](http://developer.android.com/reference/android/location/GpsSatellite.html) for more information on each field shown for each satellite.

## What's the difference between SNR and C/N0?

See http://www.insidegnss.com/auto/novdec10-Solutions.pdf for details.

## Does GPSTest collect any personal information about me?

No.  See our [Privacy Policy](https://github.com/barbeau/gpstest/wiki/Privacy-Policy) for more details.

## I'm getting a weird value for Time-to-First-Fix.  What's up?

On Android 4.1 and below, your system clock must be accurate to calculate an accurate Time-To-First-Fix (TTFF) value

## When I swipe from the Map tab to another tab, I see a brief black flash.  Why is this there?

This is apparently a limitation of the implementation of the Android Maps API v2 on certain devices.  On the devices we've seen it on (HTC EVO 4G LTE, Nexus S 4G), it only appears briefly and doesn't interfere with the operation of the app.  If this issue prevents you from using GPSTest, please contact us on the [support forum](https://groups.google.com/forum/#!forum/gpstest_android).

## My question wasn't answered here.  What's the next step?

You can ask questions by:

* Posting a messsage on the [gpstest_android Google Group](https://groups.google.com/forum/#!forum/gpstest_android)
* Filing a issue on the [GPSTest Github issue tracker](https://github.com/barbeau/gpstest/blob/master/CONTRIBUTING.md#issue-tracker)
* Posting a message to the [GPSTest Slack group](https://gpstest.slack.com).  Join via [this link](https://gpstest-android.herokuapp.com/).

