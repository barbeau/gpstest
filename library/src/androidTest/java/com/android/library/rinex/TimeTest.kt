package com.android.library.rinex

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.gpstest.library.util.rinex.RinexWriting

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TimeTest {
    @Test
    fun gpsTimeFormattingEpoch() {
        val result = RinexWriting.formatGpst(0, "yyyy MM dd HH mm ss")

        assertEquals("1980 01 06 00 00 00", result)
    }

    @Test
    fun gpsTimeFormattingTime() {
        // See https://gnsscalc.com/ for data
        val result = RinexWriting.formatGpst(
            (1434488539.658 * RinexWriting.S_TO_NS).toLong(),
            "yyyy MM dd HH mm ss"
        )

        assertEquals("2025 06 20 21 02 19", result)
    }

    @Test
    fun gpsTimeToMicroseconds() {
        // See https://gnsscalc.com/ for data
        val result = RinexWriting.formatSecondsFromGpst(
            (1434488539.658 * RinexWriting.S_TO_NS).toLong()
        )

        assertEquals("19.658000", result)
    }
}