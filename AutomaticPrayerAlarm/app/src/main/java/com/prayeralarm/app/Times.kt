package com.prayeralarm.app

import java.time.LocalDate
import java.time.ZoneId

object Times {

    data class Day(
        val date: LocalDate,
        val fajr: Long,
        val sunrise: Long,
        val dhuhr: Long,
        val asr: Long,
        val maghrib: Long,
        val isha: Long,
    ) {
        fun of(p: Prayer): Long = when (p) {
            Prayer.FAJR -> fajr
            Prayer.DHUHR -> dhuhr
            Prayer.ASR -> asr
            Prayer.MAGHRIB -> maghrib
            Prayer.ISHA -> isha
        }
    }

    fun forDate(date: LocalDate, s: AppSettings): Day {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone)
        val tz = zone.rules.getOffset(start.toInstant()).totalSeconds / 3600.0
        val r = PrayerCalculator.compute(
            date.year, date.monthValue, date.dayOfMonth,
            s.lat, s.lon, tz,
            asrFactor = if (s.asrHanafi) 2 else 1,
        )
        val base = start.toInstant().toEpochMilli()
        fun ms(h: Double, p: Prayer?): Long {
            val fixed = p?.let { s.customTime(it) } ?: -1
            return if (fixed >= 0) base + fixed * 60_000L
            else base + (h * 3_600_000).toLong() + (p?.let { s.offset(it) } ?: 0) * 60_000L
        }
        return Day(
            date,
            ms(r.fajr, Prayer.FAJR),
            ms(r.sunrise, null),
            ms(r.dhuhr, Prayer.DHUHR),
            ms(r.asr, Prayer.ASR),
            ms(r.maghrib, Prayer.MAGHRIB),
            ms(r.isha, Prayer.ISHA),
        )
    }

    const val KIND_ADHAN = "adhan"
    const val KIND_PRE = "pre"
    const val KIND_POST = "post"

    data class Event(val prayer: Prayer, val kind: String, val time: Long, val minutes: Int = 0)

    /** Next alarm event (pre-reminder, adhan, or post-reminder repeat) strictly after [now]. */
    fun nextEvent(now: Long, s: AppSettings): Event? {
        val today = LocalDate.now()
        var best: Event? = null
        for (d in 0..2) {
            val day = forDate(today.plusDays(d.toLong()), s)
            for (p in Prayer.entries) {
                if (!s.enabled(p)) continue
                val t = day.of(p)
                val events = ArrayList<Event>()
                if (s.preEnabled) for (m in s.preMinutesList) events += Event(p, KIND_PRE, t - m * 60_000L, m)
                events += Event(p, KIND_ADHAN, t)
                for (k in 1..s.postCount) events += Event(p, KIND_POST, t + k * s.postInterval * 60_000L)
                for (e in events) {
                    val b = best
                    if (e.time > now && (b == null || e.time < b.time)) best = e
                }
            }
            if (best != null) break
        }
        return best
    }

    /** Next prayer strictly after [now]. If [onlyEnabled], skips prayers whose alarm is off. */
    fun next(now: Long, s: AppSettings, onlyEnabled: Boolean): Pair<Prayer, Long>? {
        val today = LocalDate.now()
        for (d in 0..2) {
            val day = forDate(today.plusDays(d.toLong()), s)
            for (p in Prayer.entries) {
                if (onlyEnabled && !s.enabled(p)) continue
                val t = day.of(p)
                if (t > now) return p to t
            }
        }
        return null
    }
}
