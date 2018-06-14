# GPSTest [![Build Status](https://travis-ci.org/barbeau/gpstest.svg?branch=master)](https://travis-ci.org/barbeau/gpstest)

The #1 open-source global navigation satellite system (GNSS) testing app

[![Google Play logo](http://www.android.com/images/brand/android_app_on_play_logo_large.png)](https://play.google.com/store/apps/details?id=com.android.gpstest)

It supports the following satellite systems:

* GPS (USA Navstar)
* GLONASS (Russia)
* QZSS (Japan)
* BeiDou/COMPASS (China)
* Galileo (European Union)
* Various satellite-based augmentation systems SBAS (e.g., GAGAN, Anik F1, Galaxy 15, Inmarsat 3-F2, Inmarsat 4-F3, SES-5)

Questions?  Check out the [FAQ](FAQ.md), our [Slack group](https://gpstest-android.herokuapp.com/), and [Google Group](https://groups.google.com/forum/#!forum/gpstest_android).

## Beta Testing

Get early access to new GPSTest versions, and help us squash bugs! See our [Testing Guide](BETA_TESTING.md) for details.

## Data Output and Logging

Want to know how to output GNSS data to the system log for further analysis?  Check out our [Data Output and Logging](LOGGING.md) page.

## License

GPSTest is licensed under [Apache v2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Build Setup

The below steps will help you build and run the project.  For a Developer's Guide and more details, see https://github.com/barbeau/gpstest/wiki

### Prerequisites for both Android Studio and Gradle

1. Download and install the [Android SDK](http://developer.android.com/sdk/index.html).  Make sure to install the Google APIs for your API level (e.g., 17), the Android SDK Build-tools version for your `buildToolsVersion` version, and the Android Support Repository and Google Repository.
2. Set the "ANDROID_HOME" environmental variable to your Android SDK location.
3. Set the "JAVA_HOME" environmental variables to point to your JDK folder (e.g., "C:\Program Files\Java\jdk1.6.0_27")

### Building in Android Studio

1. Download and install the latest version of [Android Studio](http://developer.android.com/sdk/installing/studio.html).
2. In Android Studio, choose "Import Project" at the welcome screen.
3. Browse to the location of the project, and double-click on the project directory.
4. If prompted with options, check "Use auto-import", and select "Use default gradle wrapper (recommended)".  Click "Ok".
5. The default build flavor will be the `google` flavor, which is the version of GPSTest on Google Play that uses Google Play Services for maps.  If you want to build the version on Fdroid that uses OSMDroid for maps, use the "Build Variants" toolbar item to select the `osmdroid` flavor.
6. Click the green play button (or 'Shift->F10') to run the project!

### Building from the command line using Gradle

1. To build and push the app to the device, run `gradlew installGoogleDebug` from the command line at the root of the project for the version of GPSTest on Google Play.  If you want to build the Fdroid version with OSM Droid, then use `gradlew installOsmdroidDebug`.
3. To start the app, run `adb shell am start -n com.android.gpstest/.GpsTestActivity` for the Google Play version, or `adb shell am start -n com.android.gpstest.fdroid/.GpsTestActivity` for the Fdroid version (alternately, you can manually start the app)

### Release builds

To build a release build, you first need to create a `gradle.properties` file that points to a `secure.properties` file, and a `secure.properties` file that points to your keystore and alias. 

The `gradle.properties` file is located in the `\GPSTest` directory and has the contents:

```
secure.properties=<full_path_to_secure_properties_file>
```

The "secure.properties" file (in the location specified in gradle.properties) has the contents:

```
key.store=<full_path_to_keystore_file>
```

```
key.alias=<key_alias_name>
```

Note that the paths in these files always use the Unix path separator  `/`, even on Windows. If you use the Windows path separator `\` you will get the error `No value has been specified for property 'signingConfig.keyAlias'.`

To build the release build, run:

`gradlew assembleRelease`

If Gradle is running as a daemon, you'll be prompted for the keystore/key password via a popup dialog box. If you're not running Gradle as a daemon, command will prompt for your passwords (See https://github.com/barbeau/gpstest/issues/87).

If you want to force Gradle to not run as a daemon, use `gradlew assembleRelease -D org.gradle.daemon=false`.

### Contributing

We welcome contributions to the project!  Please see our [Contributing Guide](.github/CONTRIBUTING.md) for details, including Code Style Guidelines and Template.

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
