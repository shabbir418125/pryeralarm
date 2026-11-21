package com.prayeralarm.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.prayeralarm.app.ui.Actions
import com.prayeralarm.app.ui.PrayerApp
import java.io.File

class MainActivity : ComponentActivity() {

    private val rev = mutableIntStateOf(0)
    /** "default", "reminder" or "prayer:<id>" */
    private var pendingTarget: String? = null

    private val pickAudio =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val t = pendingTarget
            if (uri != null && t != null) importAudio(t, uri)
        }

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) fetchLocation()
            else Toast.makeText(this, "লোকেশন অনুমতি দেওয়া হয়নি", Toast.LENGTH_SHORT).show()
        }

    private fun changed() {
        AlarmScheduler.scheduleNext(this)
        rev.intValue++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the app crashed last time, show the error text first (plain views, no Compose).
        val crashFile = File(filesDir, PrayerApplication.CRASH_FILE)
        if (crashFile.exists()) {
            showCrashScreen(crashFile)
            return
        }

        AlarmScheduler.scheduleNext(this)
        requestNotificationPermission()

        val actions = Actions(
            pickMp3 = { p ->
                pendingTarget = "prayer:${p.id}"
                pickAudio.launch(arrayOf("audio/*"))
            },
            pickDefaultAdhan = {
                pendingTarget = "default"
                pickAudio.launch(arrayOf("audio/*"))
            },
            pickReminder = {
                pendingTarget = "reminder"
                pickAudio.launch(arrayOf("audio/*"))
            },
            useGps = {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED
                ) fetchLocation()
                else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            },
            batterySettings = {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            },
            notificationPermission = { requestNotificationPermission() },
            testPlay = { p ->
                ContextCompat.startForegroundService(
                    this, Intent(this, AdhanService::class.java).putExtra(EXTRA_PRAYER, p.id)
                )
            },
            stopPlay = {
                startService(Intent(this, AdhanService::class.java).setAction(AdhanService.ACTION_STOP))
            },
        )
        setContent { PrayerApp(rev.intValue, ::changed, actions) }
    }

    override fun onResume() {
        super.onResume()
        AlarmScheduler.scheduleNext(this)
        rev.intValue++
    }

    private fun showCrashScreen(file: File) {
        val report = try { file.readText() } catch (_: Exception) { "(রিপোর্ট পড়া যায়নি)" }
        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "অ্যাপটি আগে ক্র্যাশ করেছিল। নিচের লেখার স্ক্রিনশট (বা কপি করে) আমাকে পাঠান।"
            textSize = 16f
            setPadding(pad, pad * 3, pad, pad)
        }
        val body = TextView(this).apply {
            text = report.take(3500)
            textSize = 11f
            setTextIsSelectable(true)
            setPadding(pad, 0, pad, pad)
        }
        val copy = Button(this).apply {
            text = "কপি করুন"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", report))
                Toast.makeText(this@MainActivity, "কপি হয়েছে", Toast.LENGTH_SHORT).show()
            }
        }
        val go = Button(this).apply {
            text = "রিপোর্ট মুছে অ্যাপ চালু করুন"
            setOnClickListener {
                file.delete()
                recreate()
            }
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(copy)
            addView(go)
            addView(body)
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun displayName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i) else null
        }

    private fun importAudio(target: String, uri: Uri) {
        try {
            val name = displayName(uri) ?: "audio.mp3"
            val out = File(filesDir, "audio_${target.replace(':', '_')}.bin")
            contentResolver.openInputStream(uri)!!.use { input ->
                out.outputStream().use { input.copyTo(it) }
            }
            val s = AppSettings(this)
            val label: String
            when {
                target == "default" -> {
                    s.defaultAdhanPath = out.absolutePath
                    s.defaultAdhanName = name
                    label = "ডিফল্ট আজান"
                }
                target == "reminder" -> {
                    s.reminderPath = out.absolutePath
                    s.reminderName = name
                    label = "রিমাইন্ডার"
                }
                else -> {
                    val p = Prayer.fromId(target.removePrefix("prayer:")) ?: return
                    s.setCustom(p, out.absolutePath, name)
                    label = p.bn
                }
            }
            changed()
            Toast.makeText(this, "$label: $name সেট হয়েছে", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "ফাইলটি যোগ করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocation() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: Location? = null
        for (provider in lm.getProviders(true)) {
            val l = try { lm.getLastKnownLocation(provider) } catch (_: Exception) { null } ?: continue
            if (best == null || l.accuracy < best.accuracy) best = l
        }
        val loc = best
        if (loc == null) {
            Toast.makeText(this, "অবস্থান পাওয়া যায়নি। লোকেশন চালু করে আবার চেষ্টা করুন", Toast.LENGTH_LONG).show()
            return
        }
        val s = AppSettings(this)
        s.lat = loc.latitude
        s.lon = loc.longitude
        s.placeName = "বর্তমান অবস্থান"
        changed()
    }
}
