package com.barcodealarm.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.barcodealarm.app.MainActivity
import com.barcodealarm.app.BarcodeAlarmApp

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getLongExtra(BarcodeAlarmApp.EXTRA_ALARM_ID, -1) ?: -1
        startAlarmSound(alarmId)
        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, BarcodeAlarmApp.CHANNEL_SERVICE)
            .setContentTitle("آلارم بارکد فعال است")
            .setContentText("برای قطع آلارم، بارکد را اسکن کنید")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        startForeground(1, notification)
    }

    private fun startAlarmSound(alarmId: Long) {
        startVibration()

        try {
            // گرفتن صدای سفارشی از تنظیمات یا صدای پیش‌فرض آلارم
            val prefs = getSharedPreferences("barcode_alarm_prefs", 0)
            val customSoundUri = prefs.getString("alarm_sound_uri_$alarmId", null)
            val alarmUri = if (customSoundUri != null) {
                Uri.parse(customSoundUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                setVolume(1.0f, 1.0f)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    // بی‌صدا کردن موقت (توقف پخش بدون از بین بردن سرویس)
    fun muteAlarmSound() {
        mediaPlayer?.let {
            if (it.isPlaying) it.pause()
        }
        vibrator?.cancel()
    }

    fun stopAlarmSound() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
            mediaPlayer = null
        }
        vibrator?.cancel()
        vibrator = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopAlarmSound()
        instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        private var instance: AlarmService? = null

        fun stop(context: Context) {
            context.stopService(Intent(context, AlarmService::class.java))
        }

        // بی‌صدا کردن از هر جای اپ
        fun mute() {
            instance?.muteAlarmSound()
        }
    }
}
