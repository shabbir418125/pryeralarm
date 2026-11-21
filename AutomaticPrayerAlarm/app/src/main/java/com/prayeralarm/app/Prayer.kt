package com.prayeralarm.app

fun toBnDigits(s: String): String =
    s.map { if (it in '0'..'9') '০' + (it - '0') else it }.joinToString("")

enum class Prayer(val id: String, val bn: String, val en: String, val speech: String) {
    FAJR("fajr", "ফজর", "Fajr", "ফজরের ওয়াক্ত হয়েছে। নামাজের জন্য প্রস্তুত হোন।"),
    DHUHR("dhuhr", "যোহর", "Dhuhr", "যোহরের ওয়াক্ত হয়েছে। নামাজের জন্য প্রস্তুত হোন।"),
    ASR("asr", "আসর", "Asr", "আসরের ওয়াক্ত হয়েছে। নামাজের জন্য প্রস্তুত হোন।"),
    MAGHRIB("maghrib", "মাগরিব", "Maghrib", "মাগরিবের ওয়াক্ত হয়েছে। নামাজের জন্য প্রস্তুত হোন।"),
    ISHA("isha", "এশা", "Isha", "এশার ওয়াক্ত হয়েছে। নামাজের জন্য প্রস্তুত হোন।");

    /** "ফজরের", "যোহরের", ... "এশার" */
    val genitive: String get() = if (this == ISHA) "এশার" else bn + "ের"

    companion object {
        fun fromId(id: String?): Prayer? = entries.firstOrNull { it.id == id }
    }
}
