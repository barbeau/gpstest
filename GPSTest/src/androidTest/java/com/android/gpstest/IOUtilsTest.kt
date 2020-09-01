/*
 * Copyright (C) 2019 Sean J. Barbeau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.gpstest

import android.content.Intent
import android.location.Location
import androidx.test.runner.AndroidJUnit4
import com.android.gpstest.util.IOUtils
import junit.framework.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test IO utilities.  This class has to be under the Android test runner because tested code
 * uses the Application class.
 */
@RunWith(AndroidJUnit4::class)
class IOUtilsTest {

    /**
     * Test if an intent has the SHOW_RADAR action
     */
    @Test
    fun testIsShowRadarIntent() {
        // SHOW_RADAR intent
        val intent = Intent("com.google.android.radar.SHOW_RADAR")
        assertTrue(IOUtils.isShowRadarIntent(intent))

        // Not SHOW_RADAR intents
        assertFalse(IOUtils.isShowRadarIntent(Intent("not.show.radar.intent")))
        assertFalse(IOUtils.isShowRadarIntent(null));
    }

    /**
     * Test parsing a location from SHOW_RADAR intents. Latitude, longitude, and altitude oculd
     * be floats or doubles, so we have to test both (including with and without altitude).
     */
    @Test
    fun testGetLocationFromIntent() {
        // Comparison delta to allow rounding tolerance from float to double
        val delta = 0.00001

        // float values for lat and lon
        val intent = Intent("com.google.android.radar.SHOW_RADAR")
        intent.putExtra("latitude", 28.0527222f)
        intent.putExtra("longitude", -82.4331001f)

        val location = IOUtils.getLocationFromIntent(intent)
        assertEquals(28.0527222, location.latitude, delta)
        assertEquals(-82.433100, location.longitude, delta)
        assertFalse(location.hasAltitude())

        // With float altitude
        val intentWithAltitude = Intent("com.google.android.radar.SHOW_RADAR")
        intentWithAltitude.putExtra("latitude", 28.0527222f)
        intentWithAltitude.putExtra("longitude", -82.4331001f)
        intentWithAltitude.putExtra("altitude", 20.3f)

        val locationWithAltitude = IOUtils.getLocationFromIntent(intentWithAltitude)
        assertEquals(28.0527222, locationWithAltitude.latitude, delta)
        assertEquals(-82.433100, locationWithAltitude.longitude, delta)
        assertEquals(20.3, locationWithAltitude.altitude, delta)

        // double values for lat and lon
        val intentDouble = Intent("com.google.android.radar.SHOW_RADAR")
        intentDouble.putExtra("latitude", 28.0527222)
        intentDouble.putExtra("longitude", -82.4331001)

        val locationDouble = IOUtils.getLocationFromIntent(intentDouble)
        assertEquals(28.0527222, locationDouble.latitude, delta)
        assertEquals(-82.433100, locationDouble.longitude, delta)
        assertFalse(locationDouble.hasAltitude())

        // With double altitude
        val intentDoubleWithAltitude = Intent("com.google.android.radar.SHOW_RADAR")
        intentDoubleWithAltitude.putExtra("latitude", 28.0527222)
        intentDoubleWithAltitude.putExtra("longitude", -82.4331001)
        intentDoubleWithAltitude.putExtra("altitude", 20.3)

        val locationDoubleWithAltitude = IOUtils.getLocationFromIntent(intentDoubleWithAltitude)
        assertEquals(28.0527222, locationDoubleWithAltitude.latitude, delta)
        assertEquals(-82.433100, locationDoubleWithAltitude.longitude, delta)
        assertEquals(20.3, locationDoubleWithAltitude.altitude, delta)

        // NaN value for altitude
        val intentNullAltitude = Intent("com.google.android.radar.SHOW_RADAR")
        intentNullAltitude.putExtra("latitude", 28.0527222)
        intentNullAltitude.putExtra("longitude", -82.4331001)
        intentNullAltitude.putExtra("altitude", Double.NaN)

        val locationNullAltitude = IOUtils.getLocationFromIntent(intentNullAltitude)
        assertEquals(28.0527222, locationNullAltitude.latitude, delta)
        assertEquals(-82.433100, locationNullAltitude.longitude, delta)
        assertFalse(locationNullAltitude.hasAltitude())

        // double values for lat and lon, float for altitude (BenchMap config as of July 31, 2019)
        val intentDoubleWithFloatAltitude = Intent("com.google.android.radar.SHOW_RADAR")
        intentDoubleWithFloatAltitude.putExtra("latitude", 28.0527222)
        intentDoubleWithFloatAltitude.putExtra("longitude", -82.4331001)
        intentDoubleWithFloatAltitude.putExtra("altitude", 20.3f)

        val locationDoubleWithFloatAltitude = IOUtils.getLocationFromIntent(intentDoubleWithFloatAltitude)
        assertEquals(28.0527222, locationDoubleWithFloatAltitude.latitude, delta)
        assertEquals(-82.433100, locationDoubleWithFloatAltitude.longitude, delta)
        assertEquals(20.3, locationDoubleWithFloatAltitude.altitude, delta)
    }

    /**
     * Tests creating a SHOW_RADAR intent from latitude, longitude, and altitude
     */
    @Test
    fun testCreateShowRadarIntent() {
        val resultNoAltitude = IOUtils.createShowRadarIntent(24.5253, 87.23434, null)
        assertEquals(24.5253, resultNoAltitude?.extras?.get("latitude"))
        assertEquals(87.23434, resultNoAltitude?.extras?.get("longitude"))
        assertFalse(resultNoAltitude.hasExtra("altitude"))

        val resultWithAltitude = IOUtils.createShowRadarIntent(24.5253, 87.23434, 15.5)
        assertEquals(24.5253, resultWithAltitude.extras?.get("latitude"))
        assertEquals(87.23434, resultWithAltitude.extras?.get("longitude"))
        assertEquals(15.5, resultWithAltitude.extras?.get("altitude"))

        val locationNoAltitude = Location("TestNoAltitude")
        locationNoAltitude.latitude = -20.8373
        locationNoAltitude.longitude = -120.8273

        val resultFromLocationNoAltitude = IOUtils.createShowRadarIntent(locationNoAltitude)
        assertEquals(-20.8373, resultFromLocationNoAltitude?.extras?.get("latitude"))
        assertEquals(-120.8273, resultFromLocationNoAltitude?.extras?.get("longitude"))
        assertFalse(resultNoAltitude.hasExtra("altitude"))

        val locationWithAltitude = Location("TestWithAltitude")
        locationWithAltitude.latitude = -26.8373
        locationWithAltitude.longitude = -126.8273
        locationWithAltitude.altitude = -13.5

        val resultFromLocationWithAltitude = IOUtils.createShowRadarIntent(locationWithAltitude)
        assertEquals(-26.8373, resultFromLocationWithAltitude.extras?.get("latitude"))
        assertEquals(-126.8273, resultFromLocationWithAltitude.extras?.get("longitude"))
        assertEquals(-13.5, resultFromLocationWithAltitude.extras?.get("altitude"))

    }

    /**
     * Tests parsing a location from a Geo URI (RFC 5870)
     */
    @Test
    fun testGetLocationFromGeoUri() {
        val geoUriLatLon = "geo:37.786971,-122.399677"
        val result1 = IOUtils.getLocationFromGeoUri(geoUriLatLon)
        assertEquals(37.786971, result1.latitude)
        assertEquals(-122.399677, result1.longitude)
        assertFalse(result1.hasAltitude())

        val geoUriLatLonAlt = "geo:-28.9876,87.1937,15"
        val result2 = IOUtils.getLocationFromGeoUri(geoUriLatLonAlt)
        assertEquals(-28.9876, result2.latitude)
        assertEquals(87.1937, result2.longitude)
        assertEquals(15.0, result2.altitude)

        val invalidGeoUri = "http://not.a.geo.uri"
        val result3 = IOUtils.getLocationFromGeoUri(invalidGeoUri)
        assertNull(result3)

        val invalidLatLon = "geo:-999.9876,999.1937"
        val result4 = IOUtils.getLocationFromGeoUri(invalidLatLon)
        assertNull(result4)

        val result5 = IOUtils.getLocationFromGeoUri(null)
        assertNull(result5)

        val invalidData2 = ""
        val result6 = IOUtils.getLocationFromGeoUri(invalidData2)
        assertNull(result6)

        val invalidGeoUri2 = "http://not,a,geo,uri"
        val result7 = IOUtils.getLocationFromGeoUri(invalidGeoUri2)
        assertNull(result7)
    }

    /**
     * Tests creating a Geo URI (RFC 5870) from a location
     */
    @Test
    fun testCreateGeoUri() {
        val l = Location("geouri-no-alt")
        l.latitude = 28.12345
        l.longitude = -82.1345
        val geoUri = IOUtils.createGeoUri(l, true)
        assertEquals("geo:28.12345,-82.1345", geoUri)

        val lAlt = Location("geouri-with-alt")
        lAlt.latitude = 28.12345
        lAlt.longitude = -82.1345
        lAlt.altitude = 104.2
        val geoUriWithAlt = IOUtils.createGeoUri(lAlt, true)
        assertEquals("geo:28.12345,-82.1345,104.2", geoUriWithAlt)

        val geoUriAltExcluded = IOUtils.createGeoUri(lAlt, false)
        assertEquals("geo:28.12345,-82.1345", geoUriAltExcluded)
    }

    /**
     * Tests creating a plain text location string to be shared (e.g., via clipboard)
     */
    @Test
    fun testCreateLocationShare() {
        val lat = 28.12345
        val lon = -82.1345
        val alt = 104.2

        val l = Location("share-no-alt")
        l.latitude = lat
        l.longitude = lon
        val shareString = IOUtils.createLocationShare(l, true)
        assertEquals("28.12345,-82.1345", shareString)

        val lAlt = Location("share-with-alt")
        lAlt.latitude = lat
        lAlt.longitude = lon
        lAlt.altitude = alt
        val shareStringWithAlt = IOUtils.createLocationShare(lAlt, true)
        assertEquals("28.12345,-82.1345,104.2", shareStringWithAlt)

        val shareStringAltRemoved = IOUtils.createLocationShare(lAlt, false)
        assertEquals("28.12345,-82.1345", shareStringAltRemoved)
    }
}