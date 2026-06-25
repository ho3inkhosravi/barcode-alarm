package com.barcodealarm.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.barcodealarm.app.data.AlarmItem
import com.barcodealarm.app.alarm.AlarmScheduler
import com.barcodealarm.app.alarm.AlarmService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("barcode_alarm_prefs", 0)
    private val gson = Gson()

    private val _alarms = MutableStateFlow<List<AlarmItem>>(emptyList())
    val alarms: StateFlow<List<AlarmItem>> = _alarms

    private val _activeAlarm = MutableStateFlow<AlarmItem?>(null)
    val activeAlarm: StateFlow<AlarmItem?> = _activeAlarm

    init {
        loadAlarms()
    }

    fun loadAlarms() {
        val json = prefs.getString("alarms_list", null) ?: return
        val type = object : TypeToken<List<AlarmItem>>() {}.type
        val list: List<AlarmItem> = gson.fromJson(json, type)
        _alarms.value = list
    }

    fun saveAlarms() {
        val json = gson.toJson(_alarms.value)
        prefs.edit().putString("alarms_list", json).apply()
    }

    fun addAlarm(alarm: AlarmItem) {
        val newList = _alarms.value.toMutableList()
        newList.add(alarm)
        _alarms.value = newList
        saveAlarms()
        AlarmScheduler.scheduleAlarm(getApplication(), alarm)
    }

    fun updateAlarm(alarm: AlarmItem) {
        val newList = _alarms.value.map {
            if (it.id == alarm.id) alarm else it
        }
        _alarms.value = newList
        saveAlarms()
        AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
        if (alarm.isEnabled) {
            AlarmScheduler.scheduleAlarm(getApplication(), alarm)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        val alarm = _alarms.value.find { it.id == alarmId }
        if (alarm != null) {
            AlarmScheduler.cancelAlarm(getApplication(), alarm.id)
        }
        val newList = _alarms.value.filter { it.id != alarmId }
        _alarms.value = newList
        saveAlarms()
    }

    fun toggleAlarm(alarmId: Long) {
        val alarm = _alarms.value.find { it.id == alarmId } ?: return
        val updated = alarm.copy(isEnabled = !alarm.isEnabled)
        updateAlarm(updated)
    }

    fun setActiveAlarm(alarm: AlarmItem?) {
        _activeAlarm.value = alarm
    }

    fun getAlarmById(id: Long): AlarmItem? {
        return _alarms.value.find { it.id == id }
    }

    fun stopActiveAlarm() {
        AlarmService.stop(getApplication())
        _activeAlarm.value = null
    }

    fun rescheduleAllAlarms() {
        _alarms.value.forEach { alarm ->
            if (alarm.isEnabled) {
                AlarmScheduler.scheduleAlarm(getApplication(), alarm)
            }
        }
    }
}
