package com.barcodealarm.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.barcodealarm.app.data.AlarmItem
import com.barcodealarm.app.ui.*
import com.barcodealarm.app.ui.theme.BarcodeAlarmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions()

        setContent {
            BarcodeAlarmTheme {
                val viewModel: AlarmViewModel = viewModel()
                val alarms by viewModel.alarms.collectAsState()
                val activeAlarm by viewModel.activeAlarm.collectAsState()

                var screen by remember { mutableStateOf<Screen>(Screen.AlarmList) }
                var editingAlarm by remember { mutableStateOf<AlarmItem?>(null) }

                // Check if launched from alarm
                val alarmId = intent?.getLongExtra(BarcodeAlarmApp.EXTRA_ALARM_ID, -1) ?: -1
                LaunchedOnce(alarmId) {
                    if (alarmId != -1L) {
                        val alarm = viewModel.getAlarmById(alarmId)
                        if (alarm != null) {
                            viewModel.setActiveAlarm(alarm)
                        }
                    }
                }

                when {
                    activeAlarm != null -> {
                        RingingScreen(
                            alarm = activeAlarm!!,
                            onSnooze = {
                                viewModel.stopActiveAlarm()
                            },
                            onScanResult = { match ->
                                if (match) {
                                    Toast.makeText(this, "بارکد درست بود! آلارم قطع شد", Toast.LENGTH_LONG).show()
                                    viewModel.stopActiveAlarm()
                                } else {
                                    Toast.makeText(this, "بارکد اشتباه است! دوباره امتحان کنید", Toast.LENGTH_LONG).show()
                                }
                            }
                        )
                    }
                    screen is Screen.AddEdit -> {
                        AddEditAlarmScreen(
                            initialAlarm = editingAlarm,
                            onSave = { alarm ->
                                if (editingAlarm == null) {
                                    viewModel.addAlarm(alarm)
                                } else {
                                    viewModel.updateAlarm(alarm)
                                }
                                screen = Screen.AlarmList
                                editingAlarm = null
                            },
                            onBack = {
                                screen = Screen.AlarmList
                                editingAlarm = null
                            }
                        )
                    }
                    else -> {
                        AlarmListScreen(
                            alarms = alarms,
                            onAddAlarm = {
                                editingAlarm = null
                                screen = Screen.AddEdit
                            },
                            onToggleAlarm = { id ->
                                viewModel.toggleAlarm(id)
                            },
                            onDeleteAlarm = { id ->
                                viewModel.deleteAlarm(id)
                            },
                            onEditAlarm = { alarm ->
                                editingAlarm = alarm
                                screen = Screen.AddEdit
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.CAMERA)

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                val cameraGranted = results[Manifest.permission.CAMERA] ?: true
                if (!cameraGranted) {
                    Toast.makeText(this, "برای اسکن بارکد به دسترسی دوربین نیاز است", Toast.LENGTH_LONG).show()
                }
                // Check exact alarm permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val alarmManager = getSystemService(AlarmManager::class.java)
                    if (!alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(this, "برای آلارم دقیق به دسترسی نیاز است", Toast.LENGTH_LONG).show()
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        })
                    }
                }
            }.launch(toRequest.toTypedArray())
        }
    }
}

sealed class Screen {
    object AlarmList : Screen()
    object AddEdit : Screen()
}
