package com.barcodealarm.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.barcodealarm.app.BarcodeAlarmApp

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == BarcodeAlarmApp.ALARM_ACTION_TRIGGER) {
            val alarmId = intent.getLongExtra(BarcodeAlarmApp.EXTRA_ALARM_ID, -1)
            val alarmJson = intent.getStringExtra(BarcodeAlarmApp.EXTRA_ALARM_JSON) ?: ""

            // Start alarm sound service
            val serviceIntent = Intent(context, AlarmService::class.java).apply {
                putExtra(BarcodeAlarmApp.EXTRA_ALARM_ID, alarmId)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            // Open alarm activity
            val activityIntent = Intent(context, com.barcodealarm.app.MainActivity::class.java).apply {
                putExtra(BarcodeAlarmApp.EXTRA_ALARM_ID, alarmId)
                putExtra(BarcodeAlarmApp.EXTRA_ALARM_JSON, alarmJson)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(activityIntent)
        }
    }
}
