package com.prayeralarm.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Plays the adhan / voice announcement for one prayer.
 * Sound source priority:
 *   1. custom MP3 chosen by the user for that prayer
 *   2. bundled raw resource  res/raw/adhan_<prayer>.mp3  (e.g. adhan_fajr.mp3), else res/raw/adhan_makkah.mp3, else res/raw/adhan_default.mp3
 *   3. Bengali text-to-speech (offline, needs Bengali voice data installed on the phone)
 *   4. phone's default alarm sound
 * Audio uses USAGE_ALARM so it follows the phone's normal audio routing (speaker, AUX, Bluetooth).
 */
class AdhanService : Service() {

    companion object {
        const val ACTION_STOP = "com.prayeralarm.app.STOP"
        private const val CHANNEL_ID = "adhan"
        private const val NOTIF_ID = 77
    }

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var audioManager: AudioManager? = null
    private var focusRequest: AudioFocusRequest? = null
    private var savedAlarmVolume = -1
    private var wakeLock: PowerManager.WakeLock? = null
    private var finished = false

    private val alarmAttrs: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finish()
            return START_NOT_STICKY
        }
        val prayer = Prayer.fromId(intent?.getStringExtra(EXTRA_PRAYER))
        if (prayer == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val kind = intent?.getStringExtra(EXTRA_KIND) ?: Times.KIND_ADHAN
        val minutes = intent?.getIntExtra(EXTRA_MINUTES, 0) ?: 0
        releaseAudio() // a new prayer replaces anything still playing
        finished = false
        try {
            startForegroundCompat(prayer, kind, minutes)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            play(prayer, kind, minutes)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
        return START_NOT_STICKY
    }

    private fun preMinutesFor(minutes: Int): Int =
        if (minutes > 0) minutes else (AppSettings(this).preMinutesList.firstOrNull() ?: 10)

    private fun startForegroundCompat(prayer: Prayer, kind: String, minutes: Int) {
        val preMin = preMinutesFor(minutes)
        val title = if (kind == Times.KIND_PRE) "${prayer.genitive} ওয়াক্ত হতে আর ${toBnDigits(preMin.toString())} মিনিট বাকি"
        else "${prayer.genitive} ওয়াক্ত হয়েছে"
        val body = if (kind == Times.KIND_PRE) "নামাজের প্রস্তুতি নিন" else "নামাজের জন্য প্রস্তুত হোন"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL_ID, "নামাজের আজান", NotificationManager.IMPORTANCE_HIGH)
            ch.setSound(null, null)
            nm.createNotificationChannel(ch)
        }
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, AdhanService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE
        )
        val open = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val n: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .setContentIntent(open)
            .addAction(0, "বন্ধ করুন", stop)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIF_ID, n)
        }
    }

    private fun play(prayer: Prayer, kind: String, minutes: Int) {
        val s = AppSettings(this)
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager = am

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "prayeralarm:adhan")
            .also { it.acquire(10 * 60 * 1000L) }

        if (s.raiseAlarmVolume) {
            try {
                val old = am.getStreamVolume(AudioManager.STREAM_ALARM)
                val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    (max * s.volume / 100f).roundToInt().coerceIn(1, max), 0
                )
                savedAlarmVolume = old
            } catch (_: Exception) {
                // e.g. SecurityException when Do Not Disturb blocks volume changes
            }
        }

        focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(alarmAttrs).build()
        am.requestAudioFocus(focusRequest!!)

        if (s.vibrate && kind == Times.KIND_ADHAN) {
            try {
                val v = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 300, 500), -1))
            } catch (_: Exception) {
            }
        }

        val gain = s.volume / 100f
        when (kind) {
            Times.KIND_PRE -> {
                // short spoken heads-up; never falls back to the long alarm tone
                playedAdhan = true
                speak(
                    "${prayer.genitive} ওয়াক্ত হতে আর ${toBnDigits(preMinutesFor(minutes).toString())} মিনিট বাকি। নামাজের জন্য প্রস্তুতি নিন।",
                    gain
                )
            }
            Times.KIND_POST -> {
                // repeat reminder only, no adhan
                playedAdhan = true
                stageReminder(prayer, s, gain, force = true)
            }
            else -> {
                playedAdhan = false
                stageAdhan(prayer, s, gain)
            }
        }
    }

    private var playedAdhan = false

    /** Adhan file: per-prayer custom → default adhan (e.g. Makkah) → bundled raw resource. */
    private fun adhanSource(prayer: Prayer, s: AppSettings): (MediaPlayer.() -> Unit)? {
        s.customPath(prayer)?.takeIf { File(it).exists() }?.let { path ->
            return { setDataSource(path) }
        }
        s.defaultAdhanPath?.takeIf { File(it).exists() }?.let { path ->
            return { setDataSource(path) }
        }
        val resId = rawResource(prayer)
        if (resId != 0) {
            return {
                resources.openRawResourceFd(resId).use {
                    setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            }
        }
        return null
    }

    /** Stage 1: the adhan. When it ends, stage 2 (reminder) follows after a short pause. */
    private fun stageAdhan(prayer: Prayer, s: AppSettings, gain: Float) {
        val src = adhanSource(prayer, s)
        if (src != null) {
            try {
                startPlayer(
                    gain,
                    onDone = {
                        playedAdhan = true
                        Handler(Looper.getMainLooper()).postDelayed({ stageReminder(prayer, s, gain) }, 800)
                    },
                    source = src,
                )
                return
            } catch (_: Exception) {
                releaseAudio()
            }
        }
        stageReminder(prayer, s, gain)
    }

    /** Stage 2: "ওয়াক্ত হয়েছে, নামাজের জন্য প্রস্তুত হোন" – own recording, else Bengali TTS. */
    private fun stageReminder(prayer: Prayer, s: AppSettings, gain: Float, force: Boolean = false) {
        if (finished) return
        if (!s.reminderEnabled && !force) {
            if (playedAdhan) finish() else playDefaultAlarm(gain)
            return
        }
        val rp = s.reminderPath?.takeIf { File(it).exists() }
        if (rp != null) {
            try {
                startPlayer(gain) { setDataSource(rp) }
                return
            } catch (_: Exception) {
                releaseAudio()
            }
        }
        speak(prayer.speech, gain)
    }

    private fun rawResource(prayer: Prayer): Int {
        var id = resources.getIdentifier("adhan_${prayer.id}", "raw", packageName)
        if (id == 0) id = resources.getIdentifier("adhan_makkah", "raw", packageName)
        if (id == 0) id = resources.getIdentifier("adhan_default", "raw", packageName)
        return id
    }

    private fun startPlayer(
        gain: Float,
        onDone: () -> Unit = { finish() },
        source: MediaPlayer.() -> Unit,
    ) {
        try { player?.release() } catch (_: Exception) {}
        player = null
        val mp = MediaPlayer()
        player = mp
        mp.setAudioAttributes(alarmAttrs)
        mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
        mp.source()
        mp.setVolume(gain, gain)
        mp.setOnCompletionListener { onDone() }
        mp.setOnErrorListener { _, _, _ -> onDone(); true }
        mp.prepare()
        mp.start()
    }

    /** Bengali TTS with a lowered pitch to sound more masculine; falls back to the default alarm tone. */
    private fun speak(text: String, gain: Float) {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(this) { status ->
            val e = engine
            val ok = status == TextToSpeech.SUCCESS && e != null &&
                e.setLanguage(Locale("bn", "BD")).let {
                    it != TextToSpeech.LANG_MISSING_DATA && it != TextToSpeech.LANG_NOT_SUPPORTED
                }
            if (!ok) {
                if (playedAdhan) finish() else playDefaultAlarm(gain)
                return@TextToSpeech
            }
            e!!.setAudioAttributes(alarmAttrs)
            e.setPitch(0.7f)
            e.setSpeechRate(0.85f)
            e.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { finish() }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { finish() }
            })
            val params = android.os.Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, gain)
            }
            e.speak(text, TextToSpeech.QUEUE_FLUSH, params, "adhan")
        }
        tts = engine
    }

    private fun playDefaultAlarm(gain: Float) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            startPlayer(gain) { setDataSource(this@AdhanService, uri) }
        } catch (_: Exception) {
            finish()
        }
    }

    private fun releaseAudio() {
        try { player?.stop() } catch (_: Exception) {}
        player?.release()
        player = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun finish() {
        if (finished) return
        finished = true
        releaseAudio()
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE) else stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        releaseAudio()
        val am = audioManager
        if (am != null) {
            try {
                if (savedAlarmVolume >= 0) am.setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0)
                focusRequest?.let { am.abandonAudioFocusRequest(it) }
            } catch (_: Exception) {
            }
        }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
