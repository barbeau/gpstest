# GPSTest [![Build Status](https://travis-ci.org/barbeau/gpstest.svg?branch=master)](https://travis-ci.org/barbeau/gpstest) [![Twitter Follow](https://img.shields.io/twitter/follow/sjbarbeau.svg?style=social&label=Follow)](https://twitter.com/sjbarbeau)

The #1 open-source global navigation satellite system (GNSS) testing app

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
      alt="Get it on Google Play"
      height="80">](https://play.google.com/store/apps/details?id=com.android.gpstest)
[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/packages/com.android.gpstest.osmdroid/)

It supports the following satellite systems:

* GPS (USA Navstar)
* GLONASS (Russia)
* QZSS (Japan)
* BeiDou/COMPASS (China)
* Galileo (European Union)
* Various satellite-based augmentation systems (SBAS):
    * Wide Area Augmentation System (WAAS) (USA)
    * European Geostationary Navigation Overlay Service (EGNOS) (European Union)
    * GPS-aided GEO augmented navigation (GAGAN) (India)
    * Multi-functional Satellite Augmentation System (MSAS) (Japan)
    * System for Differential Corrections and Monitoring (SDCM) (Russia)
    * Satellite Navigation Augmentation System (SNAS) (China)
    * Soluciόn de Aumentaciόn para Caribe, Centro y Sudamérica (SACCSA) (ICAO)

Questions?  Check out the [FAQ](FAQ.md), our [Slack group](https://gpstest-android.herokuapp.com/), and [Google Group](https://groups.google.com/forum/#!forum/gpstest_android).

## Beta Testing

Get early access to new GPSTest versions, and help us squash bugs! See our [Testing Guide](BETA_TESTING.md) for details.

## Data Output and Logging

Want to know how to output GNSS data to the system log for further analysis?  Check out our [Data Output and Logging](LOGGING.md) page.

## License

GPSTest is licensed under [Apache v2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Building the project

You can open and build this project using [Android Studio](https://developer.android.com/studio).  For more details, see our [Build documentation](BUILD.MD).

### Contributing

We welcome contributions to the project!  Please see our [Contributing Guide](.github/CONTRIBUTING.md) for details, including Code Style Guidelines and Template.

Don't know where to start?  Take a look at the issues marked with the [your-first-pr](https://github.com/barbeau/gpstest/labels/your-first-pr) label and comment to let me know if you're interested in working on it.

### Translations

Want to improve existing translations, or add a new translation?  See our [Translations](/TRANSLATIONS.md) documentation.

## Troubleshooting

### When importing to Android Studio, I get an error "You are using an old, unsupported version of Gradle..."

If you're using Android Studio v0.4.2 or lower, when importing, please be sure to select the "settings.gradle" file in the root, **NOT** the project directory.
You will get the above error if you select the project directory / name of the project.

### I get build errors for the Android Support libraries or Google APIs

Open Android SDK Manager, and under the "Extras" category make sure you've installed both the "Android Support Repository" (in addition to the "Android Support library") as well as the
 "Google Repository".  Also, make sure you have the Google API installed for the API level that you're working with in the "/build.gradle" file,
 including the "Android SDK Build-tools" version (at the top of the "Tools" category in the Android SDK Manager) that
 matches the compileSdkVersion and buildToolsVersion numbers in /GPSTest/build.gradle.

### I get the import gradle project error - “Cause: unexpected end of block data”

Make sure you have the Google API installed for the API level that you're working with in the `/build.gradle` file,
 including the "Android SDK Build-tools" version (at the top of the "Tools" category in the Android SDK Manager) that
 matches the `compileSdkVersion` and `buildToolsVersion` numbers in `/GPSTest/build.gradle`.

### Android Studio or Gradle can't find my Android SDK, or the API Levels that I have installed

Make sure that you're consistently using the same Android SDK throughout Android Studio and your environmental variables.
Android Studio comes bundled with an Android SDK, and can get confused if you're pointing to this SDK within Android Studio
but have your environmental variables pointed elsewhere.  Click "File->Project Structure", and then under "Android SDK"
make sure you "Android SDK Location" is the correct location of your Android SDK.

Also, make sure you've set the "ANDROID_HOME" environmental variable to your Android SDK location and
the "JAVA_HOME" environmental variables to point to your JDK folder.
