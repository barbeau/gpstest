# Measuring Accuracy using GPSTest

GPSTest allows you to measure the GNSS accuracy of your device.  For more details, see the article ["Measuring GNSS Accuracy on Android devices"](https://medium.com/@sjbarbeau/measuring-gnss-accuracy-on-android-devices-6824492a1389).

## Setting the ground truth location

You can set the ground truth location (i.e., your actual position) for tests in GPSTest several ways.

### Manual

* Type in a latitude, longitude, and altitude (optional)
* Tap on the map to set latitude and longitude

GPSTest also supports receiving a ground truth location from another Android app such as [BenchMap](https://play.google.com/store/apps/details?id=com.tsqmadness.bmmaps), an application that allows searching and viewing of National Geodetic Survey / NGS survey stations on an interactive map.

### From BenchMap app

Follow these steps:
1. Download and install [BenchMap](https://play.google.com/store/apps/details?id=com.tsqmadness.bmmaps)
1. Tap on any marker on the map
1. Tap on the popup balloon that appears above the marker
1. Tap on the 3 dots in the upper right corner ("overflow menu") and choose "Track To"

GPSTest will open to a new test with the ground truth location set to the location of the marker from BenchMap.

Currently only latitude and longitude are supported by BenchMap (no altitude data).

### Implementing support in your own app

GPSTest can receive a ground truth location from any app that implements the [`com.google.android.radar.SHOW_RADAR` intent](http://www.openintents.org/action/com-google-android-radar-show-radar/).

For example, if you add this code to your Android app, it will set the ground truth location in GPSTest:

~~~
public void startShowRadar(double lat, double lon, float alt) {
    Intent intent = new Intent("com.google.android.radar.SHOW_RADAR"); 
    intent.putExtra("latitude", lat); // double, in decimal degrees
    intent.putExtra("longitude", lon); // double, in decimal degrees
    intent.putExtra("altitude", alt); // float or double, in meters above the WGS84 ellipsoid
    if (intent.resolveActivity(getPackageManager()) != null) { 
        startActivity(intent);
    }
}
~~~