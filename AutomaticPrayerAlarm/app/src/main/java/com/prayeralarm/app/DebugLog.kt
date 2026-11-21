package com.prayeralarm.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Keeps the last few alarm events so the Settings screen can show what really happened. */
object DebugLog {
    private const val KEY = "debug_log"

    fun fmt(ms: Long): String = SimpleDateFormat("dd/MM HH:mm:ss", Locale.ENGLISH).format(Date(ms))

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("prayer_alarm", Context.MODE_PRIVATE)

    fun add(ctx: Context, msg: String) {
        try {
            val sp = prefs(ctx)
            val old = (sp.getString(KEY, "") ?: "").split("\n").filter { it.isNotBlank() }
            val lines = (listOf(fmt(System.currentTimeMillis()) + "  " + msg) + old).take(14)
            sp.edit().putString(KEY, lines.joinToString("\n")).apply()
        } catch (_: Exception) {
        }
    }

    fun read(ctx: Context): String = prefs(ctx).getString(KEY, "") ?: ""

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY).apply()
    }
}
