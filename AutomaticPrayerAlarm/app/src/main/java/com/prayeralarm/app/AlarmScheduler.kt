package com.prayeralarm.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

const val EXTRA_PRAYER = "prayer"
const val EXTRA_KIND = "kind"
const val EXTRA_MINUTES = "minutes"

object AlarmScheduler {
    private const val REQ_ALARM = 1001
    private const val REQ_SHOW = 1002

    private fun alarmIntent(ctx: Context, prayer: Prayer?, kind: String?, minutes: Int = 0): PendingIntent {
        val i = Intent(ctx, AlarmReceiver::class.java)
        if (prayer != null) i.putExtra(EXTRA_PRAYER, prayer.id)
        if (kind != null) i.putExtra(EXTRA_KIND, kind)
        i.putExtra(EXTRA_MINUTES, minutes)
        return PendingIntent.getBroadcast(
            ctx, REQ_ALARM, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Schedules ONE exact alarm for the next event (reminder before adhan, the adhan, or a reminder repeat
     * after it). When it fires, AlarmReceiver plays it and schedules the following one, so the chain
     * continues forever (also after reboot via BootReceiver).
     * setAlarmClock is used because it is exempt from Doze/battery restrictions and needs no extra permission.
     */
    fun scheduleNext(ctx: Context) {
        try {
            doSchedule(ctx)
        } catch (e: Exception) {
            e.printStackTrace()
            DebugLog.add(ctx, "অ্যালার্ম সেট করতে ত্রুটি: ${e.javaClass.simpleName} ${e.message}")
        }
    }

    /** Real alarm through AlarmManager -> AlarmReceiver -> AdhanService, firing after [delaySec] seconds. */
    fun scheduleTest(ctx: Context, delaySec: Int = 60) {
        val app = ctx.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val i = Intent(app, AlarmReceiver::class.java)
            .putExtra(EXTRA_PRAYER, Prayer.DHUHR.id)
            .putExtra(EXTRA_KIND, Times.KIND_ADHAN)
            .putExtra(EXTRA_MINUTES, 0)
        val pi = PendingIntent.getBroadcast(
            app, 1003, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            app, REQ_SHOW, Intent(app, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val at = System.currentTimeMillis() + delaySec * 1000L
        am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
        DebugLog.add(app, "টেস্ট অ্যালার্ম সেট হয়েছে, বাজবে ${DebugLog.fmt(at)}")
    }

    private fun doSchedule(ctx: Context) {
        val app = ctx.applicationContext
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val settings = AppSettings(app)
        val next = Times.nextEvent(System.currentTimeMillis() + 1000, settings)
        if (next == null) {
            am.cancel(alarmIntent(app, null, null))
            return
        }
        val show = PendingIntent.getActivity(
            app, REQ_SHOW, Intent(app, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(
            AlarmManager.AlarmClockInfo(next.time, show),
            alarmIntent(app, next.prayer, next.kind, next.minutes)
        )
        DebugLog.add(app, "পরের অ্যালার্ম: ${next.prayer.bn} (${next.kind}) ${DebugLog.fmt(next.time)}")
    }
}
