# <img src="/icons/ic_launcher-playstore.png" width="50"/> GPSTest 
[![Build Status](https://github.com/barbeau/gpstest/actions/workflows/android.yml/badge.svg)](https://github.com/barbeau/gpstest/actions/workflows/android.yml) 
[![GitHub issues](https://img.shields.io/github/issues/barbeau/gpstest?color=red)](https://github.com/barbeau/gpstest/issues)
[![Twitter Follow](https://img.shields.io/twitter/follow/sjbarbeau.svg?style=social&label=Follow)](https://twitter.com/sjbarbeau)
[![GitHub](https://img.shields.io/github/license/barbeau/gpstest)](/LICENSE)

The #1 open-source global navigation satellite system (GNSS) testing app.

## Install

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.android.gpstest)
[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.android.gpstest.osmdroid/)

## Features

It supports dual-frequency¹ GNSS for:

* GPS (USA Navstar)
* GLONASS (Russia)
* QZSS (Japan)
* BeiDou/COMPASS (China)
* Galileo (European Union)
* IRNSS/NavIC (India)
* Various satellite-based augmentation systems (SBAS):
    * Wide Area Augmentation System (WAAS) (USA)
    * European Geostationary Navigation Overlay Service (EGNOS) (European Union)
    * GPS-aided GEO augmented navigation (GAGAN) (India)
    * Multi-functional Satellite Augmentation System (MSAS) (Japan)
    * System for Differential Corrections and Monitoring (SDCM) (Russia)
    * BeiDou Satellite-Based Augmentation System (BDSBAS) (China)
    * Soluciόn de Aumentaciόn para Caribe, Centro y Sudamérica (SACCSA) (ICAO)
    * Southern Positioning Augmentation Network (SouthPAN) (Australia / New Zealand)
    
¹*Dual-frequency GNSS requires device hardware support and Android 8.0 Oreo or higher. See [Dual-frequency GNSS on Android](https://medium.com/@sjbarbeau/dual-frequency-gnss-on-android-devices-152b8826e1c) for more details.*

</details>

## Screenshots
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" height="500">

<details>
  <summary>More screenshots</summary>
  
<img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/4.jpg" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/5.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/6.jpg" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/7.png" height="500"> <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/8.jpg" height="500">
  
</details>

## Contributing

I welcome contributions to the project!  Please see the [Contributing Guide](.github/CONTRIBUTING.md) for details, including Code Style Guidelines and Template.

- Don't know where to start?  Take a look at the issues marked with the [your-first-pr](https://github.com/barbeau/gpstest/labels/your-first-pr) label and comment to let me know if you're interested in working on it.

- Beta testing: Get early access to new GPSTest versions, and help us squash bugs! See the [Testing Guide](BETA_TESTING.md) for details.

- Translating: Want to improve existing translations, or add a new translation?  Translate on [Transifex](https://www.transifex.com/sean-barbeau/gpstest-android/dashboard/) or see the [Translations](/TRANSLATIONS.md) documentation.

## FAQ

Questions?  Check out the [FAQ](FAQ.md), the [Slack group](https://gpstest-android.herokuapp.com/), and [Google Group](https://groups.google.com/forum/#!forum/gpstest_android).

- Crowdsourcing GNSS data: Interested in better understanding the state of GNSS feature support on Android devices? See the article [*Crowdsourcing GNSS features of Android devices*](https://barbeau.medium.com/crowdsourcing-gnss-capabilities-of-android-devices-d4228645cf25).

- Accuracy: Measuring your device GNSS accuracy? Check out the [Measuring Accuracy using GPSTest](ACCURACY.md) page, as well as the corresponding article [*Measuring GNSS accuracy on Android devices*](https://medium.com/@sjbarbeau/measuring-gnss-accuracy-on-android-devices-6824492a1389).

- Data Output and Logging: Want to know how to output GNSS data to the system log for further analysis?  Check out the [Data Output and Logging](LOGGING.md) page.

- Building the project: You can open and build this project using [Android Studio](https://developer.android.com/studio).  For more details, see the [Build documentation](BUILD.MD).

## Trusted by industry experts

Notable appearances of GPSTest:

* Xiaomi - [*Xiaomi Redmi Note 9 Pro Max launch*](https://youtu.be/Y_5cfCZBOV4?t=3035), March 12, 2020.
* European Union Global Navigation Satellite Systems Agency (GSA) - [*Test your Android device’s satellite navigation performance*](https://www.gsa.europa.eu/newsroom/news/test-your-android-device-s-satellite-navigation-performance), August 21, 2018.
