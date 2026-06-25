package com.barcodealarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class BarcodeAlarmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM,
                "آلارم بارکد",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان آلارم بارکد"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "سرویس آلارم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "سرویس پخش صدای آلارم"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(alarmChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_ALARM = "barcode_alarm_channel"
        const val CHANNEL_SERVICE = "barcode_alarm_service_channel"
        const val ALARM_ACTION_TRIGGER = "com.barcodealarm.app.ALARM_TRIGGER"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_JSON = "alarm_json"
        const val SCAN_TIMEOUT_DEFAULT = 30
    }
}
