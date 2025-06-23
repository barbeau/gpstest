package com.android.gpstest.library.util.rinex

import android.text.format.DateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object RinexWriting {
    val GPS_EPOCH = run {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(1980, 0, 6, 0, 0, 0)
        calendar
    }
    const val S_TO_NS: Long = 1_000_000_000
    const val NS_TO_S: Double = 1e-9
    const val NS_TO_uS: Double = 1e-3
    const val S_TO_uS: Long = 1_000_000
    const val WEEK_TO_S: Long = 7 * 24 * 60 * 60
    const val YEAR_TO_S: Long = 365 * 24 * 60 * 60
    const val SPEED_OF_LIGHT: Double = 299792458.0

    /**
     * Leap seconds difference between BDST and GPST
     */
    const val BDST_TO_GPST = 14

    /**
     * Cursed function that converts the time in GPS to a gregorian representation
     * @param gpstNs the GPS time in nanoseconds
     * @param format the format string, as described in {@link android.text.format.DateFormat}
     * @return a {@link CharSequence} containing the requested text
     */
    @JvmStatic
    fun formatGpst(gpstNs: Long, format: String): CharSequence {
        val utcDate = GPS_EPOCH.clone() as Calendar
        // Horrible resolution issue in the toInt here, that's why we use seconds and not ms
        utcDate.add(Calendar.SECOND, (gpstNs * NS_TO_S).toInt())
        return DateFormat.format(format, utcDate)
    }

    /**
     * Get the microseconds nanoseconds, bounded to 1s
     */
    @JvmStatic
    fun formatSecondsFromGpst(gpstNs: Long): CharSequence {
        val nanos = gpstNs.mod(60 * S_TO_NS) * NS_TO_S
        return "%2.6f".format(nanos)
    }

    @JvmStatic
    fun writeRnx3Header(ver: Double = 3.03, type: String, system: String = "M: MIXED"): String {
        val tail = "RINEX VERSION / TYPE"
        // %9.2f = width 9, 2 decimals; %11s = 11 spaces; etc.
        return String.format(
            Locale.US,
            "%10s%10s%-20s%-20s%s%n",
            ver, "", type, system, tail
        )
    }

    @JvmStatic
    fun writeRnx3HeaderRunBy(runAt: Calendar, pgm: String = "Rokubun", agency: String = "not-available"): String {
        val tail = "PGM / RUN BY / DATE"
        val date = DateFormat.format("yyyyMMdd HHmmss", runAt)
        val dateString = "$date UTC"
        return String.format(
            "%-20s%-20s%-20s%s%n",
            pgm, agency, dateString, tail
        )
    }

    @JvmStatic
    fun writeRnx3HeaderEnd(): String =
        String.format("%-60s%s%n", "", "END OF HEADER")
}