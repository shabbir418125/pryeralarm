package com.prayeralarm.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_PRAYER)
        DebugLog.add(context, "অ্যালার্ম এসেছে: $id / ${intent.getStringExtra(EXTRA_KIND)}")
        if (Prayer.fromId(id) != null) {
            val svc = Intent(context, AdhanService::class.java)
                .putExtra(EXTRA_PRAYER, id)
                .putExtra(EXTRA_KIND, intent.getStringExtra(EXTRA_KIND) ?: Times.KIND_ADHAN)
                .putExtra(EXTRA_MINUTES, intent.getIntExtra(EXTRA_MINUTES, 0))
            try {
                ContextCompat.startForegroundService(context, svc)
            } catch (e: Exception) {
                e.printStackTrace()
                DebugLog.add(context, "সার্ভিস চালু করা যায়নি: ${e.javaClass.simpleName} ${e.message}")
            }
        }
        // chain: schedule the following prayer
        AlarmScheduler.scheduleNext(context)
    }
}

/** Re-arms the alarm after reboot, app update, clock or timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.scheduleNext(context)
    }
}
