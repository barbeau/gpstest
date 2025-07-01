package com.android.gpstest.library.util.rinex

import android.location.GnssClock
import com.android.gpstest.library.model.GnssType
import com.android.gpstest.library.rinex.navigation.GpsEphemeris
import com.android.gpstest.library.util.rinex.RinexWriting.S_TO_NS
import com.android.gpstest.library.util.rinex.RinexWriting.formatGpst
import com.android.gpstest.library.util.rinex.RinexWriting.writeRnx3Header
import com.android.gpstest.library.util.rinex.RinexWriting.writeRnx3HeaderEnd
import com.android.gpstest.library.util.rinex.RinexWriting.writeRnx3HeaderRunBy
import java.util.Calendar
import java.util.Date
import java.util.Locale

object RinexNavigationWriting {

    fun generateHeader(
        runAt: Calendar,
        ver: Double = 3.03,
        system: String = "M: MIXED",
        pgm: String = "Rokubun",
        agency: String = "unknown"
    ): String {
        return buildString {
            append(writeRnx3Header(ver, "N: GNSS NAV DATA", system))
            append(writeRnx3HeaderRunBy(runAt, pgm, agency))
            // TODO add ionospheric parameters
            append(writeRnx3HeaderEnd())
        }
    }

    fun generateNavigationRinexString(
        ephemeris: GpsEphemeris
    ): String {
        fun format(value: Double): String {
            return String.format(Locale.US, "%+1.12E", value)
        }

        val satelliteSystem = GnssType.NAVSTAR.toRinexChar()
        val prn = ephemeris.prnNumber
        val epoch = formatGpst((ephemeris.toc * S_TO_NS).toLong(), "yyyy MM dd HH mm ss")

        val svClockBias = format(ephemeris.svClockBias)
        val svClockDrift = format(ephemeris.svClockDrift)
        val svClockDriftRate = format(ephemeris.svClockDriftRate)
        val iode = format(ephemeris.iode.toDouble())
        val crs = format(ephemeris.crs)
        val deltaN = format(ephemeris.deltaN)
        val m0 = format(ephemeris.m0)
        val cuc = format(ephemeris.cuc)
        val e = format(ephemeris.e)
        val cus = format(ephemeris.cus)
        val sqrtA = format(ephemeris.rootOfA)
        val toe = format(ephemeris.toe)
        val cic = format(ephemeris.cic)
        val omega0 = format(ephemeris.omega0)
        val cis = format(ephemeris.cis)
        val i0 = format(ephemeris.i0)
        val crc = format(ephemeris.crc)
        val omega = format(ephemeris.omega)
        val omegaDot = format(ephemeris.omega0)
        val idot = format(ephemeris.iDot)
        val codesOnL2 = format(ephemeris.l2Code.toDouble())
        val gpsWeekNumber = format(ephemeris.week.toDouble())
        val l2PFlag = format(ephemeris.l2Flag.toDouble())
        val svAccuracy = format(ephemeris.svAccuracyM)
        val svHealth = format(ephemeris.svHealth.toDouble())
        val tgd = format(ephemeris.tgd)
        val iodc = format(ephemeris.iodc.toDouble())
        val transmissionTime = format(ephemeris.tom)
        val fitInterval = format(ephemeris.fitInterval)

        val sb = StringBuilder()
        sb.appendLine("$satelliteSystem${"%2d".format(prn)} $epoch$svClockBias$svClockDrift$svClockDriftRate")
        sb.appendLine("    $iode$crs$deltaN$m0")
        sb.appendLine("    $cuc$e$cus$sqrtA")
        sb.appendLine("    $toe$cic$omega0$cis")
        sb.appendLine("    $i0$crc$omega$omegaDot")
        sb.appendLine("    $idot$codesOnL2$gpsWeekNumber$l2PFlag")
        sb.appendLine("    $svAccuracy$svHealth$tgd$iodc")
        sb.appendLine("    $transmissionTime$fitInterval")
        return sb.toString()
    }


}