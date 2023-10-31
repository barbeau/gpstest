# Frequently Asked Questions (FAQ)

Want to know more about the data that GPSTest collects?  See some common questions below.

## What's the purpose of GPSTest?

The purpose of GPSTest is to give application developers and platform implementers an open-source reference application that demonstrates how GNSS works on Android devices.  Its goal is to advance the quality and smart use of GNSS technology in mobile devices and mobile apps.  It also allows curious users to better understand GPS on their Android device.

More details are in the European Global Navigation Satellite System Agency (GSA)'s publication [Test your satellite navigation performance on your Android device](https://www.gsa.europa.eu/sites/default/files/gps-test-app-definition.pdf). 

## What satellite constellations does GPSTest support?

* GPS (USA Navstar)
* GLONASS (Russia)
* QZSS (Japan)
* BeiDou/COMPASS (China)
* Galileo (European Union)
* IRNSS (India)
* Various satellite-based augmentation systems (SBAS):
    * Wide Area Augmentation System (WAAS) (USA)
    * European Geostationary Navigation Overlay Service (EGNOS) (European Union)
    * GPS-aided GEO augmented navigation (GAGAN) (India)
    * Multi-functional Satellite Augmentation System (MSAS) (Japan)
    * System for Differential Corrections and Monitoring (SDCM) (Russia)
    * BeiDou Satellite-Based Augmentation System (BDSBAS) (China)
    * Soluciόn de Aumentaciόn para Caribe, Centro y Sudamérica (SACCSA) (ICAO)

For lists of upcoming satellite launches and real-time satellite status on-line see https://github.com/barbeau/awesome-gnss#lists.

If you see one of these satellites in GPSTest without a flag label, open an issue and let us know!

## How do I know if my device supports Galileo?

An official list of devices that support Galileo can be found at:
https://www.usegalileo.eu

Galileo support has also been confirmed using GPSTest on the following devices:

* BQ Aquaris X5 Plus (See [this post](https://groups.google.com/d/msg/gpstest_android/SbvcUyGU67U/cL8T9GX3BwAJ))
* Huawei Mate 9 (See [this post](http://www.startlr.com/we-tried-galileo-huawei-mate-9-already-sees-the-european-satellites/))
* Samsung Galaxy S8 and S8+ (See [this post](https://github.com/barbeau/gpstest/issues/58#issuecomment-319781535) and [official specs](http://www.samsung.com/global/galaxy/galaxy-s8/specs/) which say "Location (GPS, Galileo, Glonass, BeiDou) *Galileo and BeiDou coverage may be limited.")
* OnePlus 5 (Android 7.1.1 / OxygenOS 4.5.8) (See [this post](https://github.com/barbeau/gpstest/issues/58#issuecomment-322124679))
* Huawei P10 (See [this post](https://groups.google.com/forum/#!topic/gpstest_android/Kc_YOQLp35I) - Android 7.0, firmware version L29C432B171)
* OnePlus 5T (See [this post](https://groups.google.com/forum/m/#!topic/gpstest_android/MCowNjKpVyo)) 
* Xiaomi Mi 9 (See [official specsheet](https://www.mi.com/global/mi9/specs/)) (also supports GLONASS, Beidou and dual-frequency receiving)

Note that in the U.S. the [FCC approved use of Galileo](https://barbeau.medium.com/where-is-the-world-is-galileo-6bb7bfa29e) on November 15th, 2018, although some devices released before or just after this time frame were never updated to account for that approval support. So, even if your device hardware supports Galileo you may not see any Galileo satellites in GPSTest when in the United States.

## Does GPSTest support dual-frequency GNSS?

Yes!  Look for the "CF" column on the "Status" screen.  For more detailed information on GPSTest support see my article ["Dual-frequency GNSS on Android devices"](https://medium.com/@sjbarbeau/dual-frequency-gnss-on-android-devices-152b8826e1c). 

Here's a nice table (Source: [Rohde & Schwarz](https://www.rohde-schwarz-usa.com/rs/324-UVH-477/images/Wireless_po_en_A1_0758-1029-82_v1600.pdf)) that shows all of the frequency labels you can see in GPSTest, along with their correponding frequencies:

![image](https://user-images.githubusercontent.com/928045/42654926-53581aa0-85e8-11e8-91ab-cd8ab6553bb7.png)

Another reference chart is [here](https://gssc.esa.int/navipedia/images/c/cf/GNSS_All_Signals.png) from Navipedia.

For more information on dual-frequency GNSS in general, see the EU GSA article ["Dual-frequency Q&As"](https://www.gsa.europa.eu/system/files/documents/dual_frequencies_qa.pdf).

## What does the "U" flag mean for SBAS satellites?

For SBAS, the Android spec does not specify if the "U" ("Used") flag refers to pseudoranges from the satellite being used in the position solution or to correction information originating from that satellite being used.

[Here's](https://developer.android.com/reference/android/location/GnssStatus.html#usedInFix(int)) the documentation for the boolean value for "used" that's passed from the chipset to the Android OS to GPSTest, which is shown as a "U" letter in the "Flags" column on the GPSTest Status screen:

>Reports whether the satellite at the specified index was used in the calculation of the most recent position fix.

As a result, it's unclear how this is being implemented by OEMs.

## What Android devices does GPSTest run on?

Android 1.5 and up, in its simplest form.  More advanced versions with an updated user interface and better maps interface (based on Android Maps API v2) is available on Android 2.2 and up.  NMEA logging is available on Android 2.3 and up.  GNSS measurements and navigation message logging is available on Android 7.0 and up.  Carrier frequencies are available on supported devices Android 8.0 and higher - all devices with GNSS hardware year 2018 and higher should include them.

## What are the menu buttons for?

* **Start/Stop** - Start/stop the GPS hardware
* **Send Location** - After a latitude and longitude has been acquired, you can share this info
* **Inject Time** - Injects Time assistance data for GPS into the platform, using information from a [Network Time Protocol (NTP)](http://support.ntp.org/bin/view/Main/WebHome) server.  Note that some devices don't use an NTP server for time data - if this is your device, you'll see a message saying "Platform does not support injecting time data".
* **Inject PSDS Data** - Injects Predicted Satellite Data Service (PSDS) assistance data for GNSS into the platform, using information from a PSDS server.  Note that some devices don't use PSDS for assistance data - if this is your device, you'll see a message saying "Platform does not support injecting PSDS data". PSDS is the generic term for products like [XTRA assistance data](http://goo.gl/3RjWX).
* **Clear Aiding Data** - Clears all assistance data used for GPS, including NTP and PSDS/XTRA data (Note: if you select this option to fix broken GPS on your device, for GPS to work again you may need to ‘Inject Time’ and ‘Inject PSDS’ data).  Note that some devices don't support clearing assistance data - if this is your device, you'll see a message saying "Platform does not support deleting aiding data". You may also see a large delay until your device acquires a fix again, so please use this feature with caution.
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

See the Android [`GpsSatellite` documentation](http://developer.android.com/reference/android/location/GpsSatellite.html) for more information on each field shown for each satellite for Android 6.0 and lower, and [`GnssStatus`](https://developer.android.com/reference/android/location/GnssStatus.html) for Android 7.0 and higher.

## What's the difference between SNR and C/N0?

See http://www.insidegnss.com/auto/novdec10-Solutions.pdf for details.

## What's the difference between altitude and altitude (MSL) (mean sea level)?

See https://www.esri.com/news/arcuser/0703/geoid1of3.html for details.

## Can I record any information with some type of logging tool using GPSTest?

You can view the data output from GPSTest, including NMEA (Android 2.3 and higher) and pseudorange measurements and navigation messages (Android 7.0 and higher) in log format by using Android Monitor, which is included with [Android Studio](https://developer.android.com/studio/index.html).  Check out our [Data Output and Logging](LOGGING.md) page for more info.

## What GNSS features does my device support?

Check out the [GPSTest Database](https://bit.ly/gpstest-device-database).

This data has been crowd-sourced from users of the GPSTest app via the "Share->Device" feature.

Don't see your device, or do you have a newer Android version than what's listed? Upload your own device info!

## Does GPSTest collect any personal information about me?

No.  See our [Privacy Policy](https://github.com/barbeau/gpstest/wiki/Privacy-Policy) for more details.

## Why is my GPS time wrong?

As of April 6, 2019, some older devices have been impacted by the [GPS Week Number Rollover](https://www.cisa.gov/gps-week-number-roll-over), which can result in unpredictable behavior. Typically, the time computed by GPS will be wrong. You will need to contact your device manufacturer or cellular carrier to determine if you device can be updated to fix this issue. For example, here is [Verizon's GPS Week Rollover help page](https://www.verizonwireless.com/legal/notices/global-positioning-system/).

See more information in [this article I wrote](https://barbeau.medium.com/how-to-detect-gps-week-rollover-problems-on-android-5cc739f2fa9c) for the hidden feature in GPSTest that can tell you if your device is impacted.

## I'm getting a weird value for Time-to-First-Fix.  What's up?

On Android 4.1 and below, your system clock must be accurate to calculate an accurate Time-To-First-Fix (TTFF) value

## When I swipe from the Map tab to another tab, I see a brief black flash.  Why is this there?

This is apparently a limitation of the implementation of the Android Maps API v2 on certain devices.  On the devices we've seen it on (HTC EVO 4G LTE, Nexus S 4G), it only appears briefly and doesn't interfere with the operation of the app.  If this issue prevents you from using GPSTest, please contact us on the [support forum](https://groups.google.com/forum/#!forum/gpstest_android).

## I'd like to publish my own app based on GPSTest source code.  Is this allowed?

The GPSTest source code is licensed under [Apache v2.0](LICENSE.md) so there are no legal restrictions to using the code in other apps.  Ideally, if you're missing features in GPSTest that you think would generally be useful, please consider contributing those features to GPSTest rather than launching your own app.

If you do launch your own app, I would ask that you do the following when publishing your own app using source code from GPSTest:
* Use different theme colors for your app - These can easily be changed via the top three values in colors.xml.  Looking at any screenshot of the app, it should be easy to distinguish between the two apps based on color.
* Use a different name for your app - Something not close to "GPSTest"
* Use a different logo for your app - Again, something not close to the GPSTest logo
* If your app collects data from users, clearly explain to users within your app on first startup that the app is collecting data, who you are, and how the data will be used
* Clearly explain to users that your app is not endorsed by me or GPSTest
* Clearly explain to users that your app uses source code from GPSTest
* Let me know when you publish the app, and a link to the app on Google Play or another app store
* If you're using GPSTest for research, if possible I'd be interested in seeing the results of your research
* If possible, publish your own code as open-source

## I recorded raw measurements data but when visualizing the data it looks all choppy and interrupted. What's up?

You'll probably need to enable the hidden "Force full GNSS measurements" setting in the Android "Developer options" menu. See [this article](https://barbeau.medium.com/gnss-interrupted-the-hidden-android-setting-you-need-to-know-d812d28a3821) for details.

## On Android 12, there is an option in the GPSTest Settings for "Force full GNSS measurements". How does this interact with the hidden system setting mentioned above?

On Android 12, the Android system Developer Options setting and setting within GPSTest are logically OR'd together at the Android platform level. So if at least one of them is true (set to active), then "Force full GNSS measurements" is true (i.e., duty cycling is disabled). If both of them are false, then "Force full GNSS measurements" is false (i.e., duty cycling is active, which is the default device setting).

## Does GPSTest support RINEX output?

No, but you can use the below tool to convert from the CSV log format that GPSTest supports to RINEX:
https://github.com/rokubun/android_rinex

## When logging data, I only see partial information in the file (like only the header). Where's my data?

The file output from GPSTest is buffered, so if you manually copy the file off the device while the app is running you can end up with a partial file if the buffer hasn't been fully flushed. You can force the flush of the buffer by killing the app (e.g., via the Back button), and then if you refresh your file view you should get the complete file with all your data.

## My question wasn't answered here.  What's the next step?

You can ask questions by:

* Posting a messsage on the [gpstest_android Google Group](https://groups.google.com/forum/#!forum/gpstest_android)
* Filing a issue on the [GPSTest Github issue tracker](https://github.com/barbeau/gpstest/blob/master/.github/CONTRIBUTING.md#issue-tracker)
* Posting a message to the [GPSTest Slack group](https://gpstest.slack.com).  Join via [this link](https://gpstest-android.herokuapp.com/).

