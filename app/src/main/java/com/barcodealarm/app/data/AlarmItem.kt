package com.barcodealarm.app.data

import com.google.gson.annotations.SerializedName

data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val hour: Int = 7,
    val minute: Int = 0,
    val label: String = "",
    val barcodeValue: String = "",
    val barcodeFormat: String = "",
    val isEnabled: Boolean = true,
    val isOneTime: Boolean = true,
    val repeatDays: List<Boolean> = listOf(false, false, false, false, false, false, false),
    val snoozeMinutes: Int = 5,
    val soundUri: String = "default",
    val scanTimeoutSeconds: Int = 30,
    val is24Hour: Boolean = true
) {
    fun getRepeatDaysText(): String {
        val days = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
        val activeDays = repeatDays.mapIndexed { index, active -> if (active) days[index] else null }.filterNotNull()
        return if (activeDays.size == 7) "هر روز" else activeDays.joinToString(" - ")
    }

    fun getTimeText(): String {
        val h = if (is24Hour) hour.toString() else {
            if (hour == 0) "12" else if (hour > 12) (hour - 12).toString() else hour.toString()
        }
        val m = minute.toString().padStart(2, '0')
        return "$h:$m"
    }

    fun getAmPm(): String {
        if (is24Hour) return ""
        return if (hour >= 12) " ق.ظ" else " ب.ظ"
    }
}
