package com.prayeralarm.app

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/** Saves the stack trace of any crash so MainActivity can show it on the next launch. */
class PrayerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter()
                e.printStackTrace(PrintWriter(sw))
                File(filesDir, CRASH_FILE).writeText(
                    "Thread: ${t.name}\n" +
                        "Android: ${android.os.Build.VERSION.SDK_INT}\n" +
                        "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n$sw"
                )
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(t, e)
        }
    }

    companion object {
        const val CRASH_FILE = "last_crash.txt"
    }
}
