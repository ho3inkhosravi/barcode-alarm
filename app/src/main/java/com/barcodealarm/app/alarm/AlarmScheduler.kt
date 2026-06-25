package com.barcodealarm.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.barcodealarm.app.BarcodeAlarmApp
import com.barcodealarm.app.data.AlarmItem
import java.util.Calendar
import java.util.Date

object AlarmScheduler {

    fun scheduleAlarm(context: Context, alarm: AlarmItem) {
        if (!alarm.isEnabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = calculateTriggerTime(alarm)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = BarcodeAlarmApp.ALARM_ACTION_TRIGGER
            putExtra(BarcodeAlarmApp.EXTRA_ALARM_ID, alarm.id)
            putExtra(BarcodeAlarmApp.EXTRA_ALARM_JSON, com.google.gson.Gson().toJson(alarm))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context, alarmId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = BarcodeAlarmApp.ALARM_ACTION_TRIGGER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
    }

    private fun calculateTriggerTime(alarm: AlarmItem): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = Calendar.getInstance()

        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        if (!alarm.isOneTime) {
            // Find next matching day
            var daysAdded = 0
            while (daysAdded < 7) {
                val dayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) - 1) % 7
                // Calendar.SUNDAY = 1, so we map to our 0-6 (Saturday first)
                val ourDayIndex = (dayOfWeek + 1) % 7
                if (alarm.repeatDays[ourDayIndex] && calendar.after(now)) {
                    break
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                daysAdded++
            }
        }

        return calendar.timeInMillis
    }

    fun getTimeUntilAlarm(alarm: AlarmItem): String {
        val triggerTime = calculateTriggerTime(alarm)
        val remaining = triggerTime - System.currentTimeMillis()

        if (remaining <= 0) return "اکنون"

        val hours = remaining / (1000 * 60 * 60)
        val minutes = (remaining % (1000 * 60 * 60)) / (1000 * 60)

        return when {
            hours > 0 -> "${hours} ساعت و ${minutes} دقیقه"
            minutes > 0 -> "${minutes} دقیقه"
            else -> "کمتر از یک دقیقه"
        }
    }
}
