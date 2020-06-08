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
    * Satellite Navigation Augmentation System (SNAS) (China)
    * Soluciόn de Aumentaciόn para Caribe, Centro y Sudamérica (SACCSA) (ICAO)

For a list of upcoming satellite launches, see http://gpsworld.com/resources/upcoming-gnss-satellite-launches/.

Real-time satellites status:
* Galileo - https://www.gsc-europa.eu/system-service-status/constellation-information
* WAAS - http://www.nstb.tc.faa.gov/RT_WaasSatelliteStatus.htm

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

Note that the U.S. [FCC currently restricts use of Galileo](https://groups.google.com/forum/#!topic/gpstest_android/KrBe_csYFfg) when in the United States, so even if your device supports Galileo you won't see any Galileo satellites in GPSTest when in the United States.

## Does GPSTest support dual-frequency GNSS?

Yes!  Look for the "CF" column on the "Status" screen.  For more detailed information on GPSTest support see my article ["Dual-frequency GNSS on Android devices"](https://medium.com/@sjbarbeau/dual-frequency-gnss-on-android-devices-152b8826e1c). 

Here's a nice table (Source: [Rohde & Schwarz](https://www.rohde-schwarz-usa.com/rs/324-UVH-477/images/Wireless_po_en_A1_0758-1029-82_v1600.pdf)) that shows all of the frequency labels you can see in GPSTest, along with their correponding frequencies:

![image](https://user-images.githubusercontent.com/928045/42654926-53581aa0-85e8-11e8-91ab-cd8ab6553bb7.png)

Another reference chart is [here](https://www.novatel.com/assets/Documents/Downloads/NovAtelChartH.pdf) from NovAtel.

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

## Can I record any information with some type of logging tool using GPSTest?

You can view the data output from GPSTest, including NMEA (Android 2.3 and higher) and pseudorange measurements and navigation messages (Android 7.0 and higher) in log format by using Android Monitor, which is included with [Android Studio](https://developer.android.com/studio/index.html).  Check out our [Data Output and Logging](LOGGING.md) page for more info.

## What Android 7.0 and higher devices support logging raw pseudorange and navigation messages?

Google is keeping a list on the [Raw GNSS Measurements page](https://developer.android.com/guide/topics/sensors/gnss.html#supported_devices).

## What devices support the various functionality shown in GPSTest?

The below table is compiled from users of GPSTest, based on sending feedback via the "Send feedback" feature.  An "x" indicates that the feature is supported for a device, a blank value means the feature is not supported for a device, and a "?" means that support is unknown (no one has submitted data for this feature yet).

Manufacturer | Model | Android version | GNSS HW Year | Raw measurements | Navigation messages | NMEA | Inject XTRA | Inject time | Delete assist
-- | -- | -- | -- | -- | -- | -- | -- | -- | --
Google | Pixel 2 | 9 / 28 | 2017 | ? | ? | x |   | x | ?
HTC | One M8 | 6.0 / 23 | N/A | N/A | N/A | x | x | ? | x
HTC | One M9 | 7.1.2 / 25 | 2015 | ? | ? | x | ? | ? | ?
LG | LG-AS993 (G6) | 7.0 / 24 | 2015 | ? | ? | x |   | x | ?
Motorola | XT1028 (Moto G 2018) | 4.4.4 / 19 | N/A | N/A | N/A | x | x | x | x
Nokia | 7 plus | 9 / 28 | 2017 | ? | ? | x |   | x | x
Samsung | SM-A520F (Galaxy A5 2017) | 8.0.0 / 26 | 2015 or older | x | x | x | x | x |  
Samsung | SM-J120G (Galaxy J1 4G) | 5.1.1 / 22 | N/A | N/A | N/A | x | x | x | x
Samsung | SM-G955U (Galaxy S8+) | 8.0.0 / 26 | 2015 |   |   | x |   | x |  
Samsung | SM-G965F (Galaxy S9+) | 8.0.0 / 26 | 2016 | ? | ? | x | ? | ? | ?
Sony | G3121 (Xperia XA1) | 8.0.0 / 26 | 2015 | ? | ? | x |   | x | ?
Sony | G8342 (Xperia XZ1) | 8.0.0 / 26 | 2016 | x |   | x |   | x | ?
Sony | H8266 (Xperia XZ2) | 8.0.0 / 26 | 2017 | x |   | x |   | x | x
Wileyfox | Spark | 7.1.2 / 25 | 2015 or older |   |   | x | x | x | x
Xiaomi | MI 3W | 6.0.1 / 23 | N/A | N/A | N/A | x | x | ? | ?
Xiaomi | MI 8 | 8.1.0 / 27 | 2018 | ? | ? | x | x | x | x
Xiaomi | Redmi 4A | 7.1.2 / 25 | 2015 |   |   | x |   | ? | ?

Is your device not above, or does it still have some question marks?

By default the "Send feedback" email only contains the support information for features that the user has enabled/tried. 

If you want to provide a full record for your device, in the navigation drawer first tap on:
                 
* Inject xtra
* Inject time
* Clear assist (Caution - some devices take a while to recover from this. I'd confirm you can inject the above before trying to clear the data)

...and in Settings, you can check logging for the following features:
                 
* GNSS measurement data
* Navigation messages
* NMEA data

Then tap on "Send feedback", and GPSTest it will capture success/failure for all the above.

## Does GPSTest collect any personal information about me?

No.  See our [Privacy Policy](https://github.com/barbeau/gpstest/wiki/Privacy-Policy) for more details.

## Why is my GPS time wrong?

As of April 6, 2019, some older devices have been impacted by the [GPS Week Number Rollover](https://www.cisa.gov/gps-week-number-roll-over), which can result in unpredictable behavior. Typically, the time computed by GPS will be wrong. You will need to contact your device manufacturer or cellular carrier to determine if you device can be updated to fix this issue. For example, here is [Verizon's GPS Week Rollover help page](https://www.verizonwireless.com/legal/notices/global-positioning-system/).

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

## My question wasn't answered here.  What's the next step?

You can ask questions by:

* Posting a messsage on the [gpstest_android Google Group](https://groups.google.com/forum/#!forum/gpstest_android)
* Filing a issue on the [GPSTest Github issue tracker](https://github.com/barbeau/gpstest/blob/master/.github/CONTRIBUTING.md#issue-tracker)
* Posting a message to the [GPSTest Slack group](https://gpstest.slack.com).  Join via [this link](https://gpstest-android.herokuapp.com/).

