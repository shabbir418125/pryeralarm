package com.prayeralarm.app

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Fully offline prayer time calculator (astronomical algorithm, same maths as PrayTimes.org).
 * All results are local clock hours (e.g. 5.25 = 05:15) for the given time zone offset.
 * Default: University of Islamic Sciences, Karachi (Fajr 18°, Isha 18°) – the standard in Bangladesh.
 */
object PrayerCalculator {

    data class Result(
        val fajr: Double,
        val sunrise: Double,
        val dhuhr: Double,
        val asr: Double,
        val maghrib: Double,
        val isha: Double,
    )

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)

    private fun fixAngle(a: Double): Double {
        var x = a % 360.0
        if (x < 0) x += 360.0
        return x
    }

    private fun fixHour(h: Double): Double {
        var x = h % 24.0
        if (x < 0) x += 24.0
        return x
    }

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** returns (declination°, equation of time in hours) */
    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)))
        val e = 23.439 - 0.00000036 * d
        val ra = fixHour(deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0)
        val eqt = q / 15.0 - ra
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        return decl to eqt
    }

    private fun midDay(jd: Double, t: Double): Double = fixHour(12 - sunPosition(jd + t).second)

    private fun angleTime(jd: Double, angle: Double, t: Double, lat: Double, ccw: Boolean): Double {
        val decl = sunPosition(jd + t).first
        val noon = midDay(jd, t)
        val v = deg(
            acos((-sin(rad(angle)) - sin(rad(decl)) * sin(rad(lat))) / (cos(rad(decl)) * cos(rad(lat))))
        ) / 15.0
        return noon + if (ccw) -v else v
    }

    private fun asrTime(jd: Double, factor: Int, t: Double, lat: Double): Double {
        val decl = sunPosition(jd + t).first
        val angle = -deg(atan(1.0 / (factor + tan(rad(abs(lat - decl))))))
        return angleTime(jd, angle, t, lat, false)
    }

    fun compute(
        year: Int,
        month: Int,
        day: Int,
        lat: Double,
        lon: Double,
        tzHours: Double,
        fajrAngle: Double = 18.0,
        ishaAngle: Double = 18.0,
        asrFactor: Int = 2, // 2 = Hanafi, 1 = Shafi'i/Maliki/Hanbali
    ): Result {
        val jd = julian(year, month, day) - lon / (15.0 * 24.0)
        val adj = tzHours - lon / 15.0
        return Result(
            fajr = angleTime(jd, fajrAngle, 5 / 24.0, lat, true) + adj,
            sunrise = angleTime(jd, 0.833, 6 / 24.0, lat, true) + adj,
            dhuhr = midDay(jd, 12 / 24.0) + adj,
            asr = asrTime(jd, asrFactor, 13 / 24.0, lat) + adj,
            maghrib = angleTime(jd, 0.833, 18 / 24.0, lat, false) + adj,
            isha = angleTime(jd, ishaAngle, 18 / 24.0, lat, false) + adj,
        )
    }
}
