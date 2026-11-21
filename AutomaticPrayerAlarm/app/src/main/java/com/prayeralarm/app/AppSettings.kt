package com.prayeralarm.app

import android.content.Context

class AppSettings(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("prayer_alarm", Context.MODE_PRIVATE)

    // Default location: Habiganj, Bangladesh
    var placeName: String
        get() = sp.getString("place", "হবিগঞ্জ") ?: "হবিগঞ্জ"
        set(v) = sp.edit().putString("place", v).apply()

    var lat: Double
        get() = sp.getString("lat", null)?.toDoubleOrNull() ?: 24.3745
        set(v) = sp.edit().putString("lat", v.toString()).apply()

    var lon: Double
        get() = sp.getString("lon", null)?.toDoubleOrNull() ?: 91.4156
        set(v) = sp.edit().putString("lon", v.toString()).apply()

    var asrHanafi: Boolean
        get() = sp.getBoolean("hanafi", true)
        set(v) = sp.edit().putBoolean("hanafi", v).apply()

    /** 0..100 */
    var volume: Int
        get() = sp.getInt("volume", 80)
        set(v) = sp.edit().putInt("volume", v.coerceIn(0, 100)).apply()

    /** Set the alarm stream volume to [volume]% while the adhan plays (works even if media volume is low). */
    var raiseAlarmVolume: Boolean
        get() = sp.getBoolean("raise_alarm_volume", true)
        set(v) = sp.edit().putBoolean("raise_alarm_volume", v).apply()

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit().putBoolean("vibrate", v).apply()

    fun enabled(p: Prayer): Boolean = sp.getBoolean("en_${p.id}", true)
    fun setEnabled(p: Prayer, v: Boolean) = sp.edit().putBoolean("en_${p.id}", v).apply()

    /** minutes, -120..120 */
    fun offset(p: Prayer): Int = sp.getInt("off_${p.id}", 0)
    fun setOffset(p: Prayer, v: Int) = sp.edit().putInt("off_${p.id}", v.coerceIn(-120, 120)).apply()

    /** One adhan file (e.g. Makkah adhan MP3) used for every prayer that has no own custom file. */
    var defaultAdhanPath: String?
        get() = sp.getString("def_adhan_path", null)
        set(v) = sp.edit().putString("def_adhan_path", v).apply()

    var defaultAdhanName: String?
        get() = sp.getString("def_adhan_name", null)
        set(v) = sp.edit().putString("def_adhan_name", v).apply()

    /** Reminder spoken/played after the adhan: "ওয়াক্ত হয়েছে, নামাজের জন্য প্রস্তুত হোন". */
    var reminderEnabled: Boolean
        get() = sp.getBoolean("reminder_enabled", true)
        set(v) = sp.edit().putBoolean("reminder_enabled", v).apply()

    var reminderPath: String?
        get() = sp.getString("reminder_path", null)
        set(v) = sp.edit().putString("reminder_path", v).apply()

    var reminderName: String?
        get() = sp.getString("reminder_name", null)
        set(v) = sp.edit().putString("reminder_name", v).apply()

    /** Fixed clock time for a prayer, minutes since midnight (0..1439). -1 = automatic (calculated + offset). */
    fun customTime(p: Prayer): Int = sp.getInt("ct_${p.id}", -1)
    fun setCustomTime(p: Prayer, minutes: Int) = sp.edit().putInt("ct_${p.id}", minutes).apply()

    /** Reminder shortly BEFORE the adhan. */
    var preEnabled: Boolean
        get() = sp.getBoolean("pre_enabled", true)
        set(v) = sp.edit().putBoolean("pre_enabled", v).apply()

    var preMinutes: Int
        get() = sp.getInt("pre_minutes", 10)
        set(v) = sp.edit().putInt("pre_minutes", v.coerceIn(1, 120)).apply()

    /**
     * All "minutes before adhan" at which a reminder fires (e.g. [10, 15] = two reminders per prayer).
     * Falls back to the old single [preMinutes] value for people who updated from an earlier version.
     */
    var preMinutesList: List<Int>
        get() {
            val raw = sp.getString("pre_list", null) ?: return listOf(preMinutes)
            return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..180 }.distinct().sorted()
        }
        set(v) = sp.edit().putString("pre_list", v.filter { it in 1..180 }.distinct().sorted().joinToString(",")).apply()

    /** How many times the reminder repeats AFTER the adhan (0 = off). */
    var postCount: Int
        get() = sp.getInt("post_count", 1)
        set(v) = sp.edit().putInt("post_count", v.coerceIn(0, 10)).apply()

    /** Minutes between those repeats. */
    var postInterval: Int
        get() = sp.getInt("post_interval", 5)
        set(v) = sp.edit().putInt("post_interval", v.coerceIn(5, 60)).apply()

    fun customPath(p: Prayer): String? = sp.getString("path_${p.id}", null)
    fun customName(p: Prayer): String? = sp.getString("name_${p.id}", null)
    fun setCustom(p: Prayer, path: String?, name: String?) {
        sp.edit().putString("path_${p.id}", path).putString("name_${p.id}", name).apply()
    }
}
