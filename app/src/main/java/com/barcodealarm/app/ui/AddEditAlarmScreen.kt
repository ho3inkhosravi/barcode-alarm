package com.barcodealarm.app.ui

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarcodeReader
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barcodealarm.app.data.AlarmItem
import com.barcodealarm.app.scanner.BarcodeScannerActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAlarmScreen(
    initialAlarm: AlarmItem?,
    onSave: (AlarmItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var hour by remember { mutableStateOf(initialAlarm?.hour ?: 7) }
    var minute by remember { mutableStateOf(initialAlarm?.minute ?: 0) }
    var label by remember { mutableStateOf(initialAlarm?.label ?: "") }
    var barcodeValue by remember { mutableStateOf(initialAlarm?.barcodeValue ?: "") }
    var isOneTime by remember { mutableStateOf(initialAlarm?.isOneTime ?: true) }
    var repeatDays by remember {
        mutableStateOf(initialAlarm?.repeatDays ?: listOf(false, false, false, false, false, false, false))
    }
    var snoozeMinutes by remember { mutableStateOf(initialAlarm?.snoozeMinutes ?: 5) }
    var scanTimeoutSeconds by remember { mutableStateOf(initialAlarm?.scanTimeoutSeconds ?: 30) }

    val dayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")

    val barcodeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            barcodeValue = result.data?.getStringExtra("barcode_value") ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialAlarm == null) "افزودن آلارم" else "ویرایش آلارم",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val alarm = AlarmItem(
                            id = initialAlarm?.id ?: System.currentTimeMillis(),
                            hour = hour,
                            minute = minute,
                            label = label,
                            barcodeValue = barcodeValue,
                            isEnabled = true,
                            isOneTime = isOneTime,
                            repeatDays = repeatDays,
                            snoozeMinutes = snoozeMinutes,
                            scanTimeoutSeconds = scanTimeoutSeconds
                        )
                        onSave(alarm)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "ذخیره")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Time Picker Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ساعت آلارم",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%02d:%02d", hour, minute),
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    hour = h
                                    minute = m
                                },
                                hour, minute, true
                            ).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "برای تغییر ضربه بزنید",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barcode Selection
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = Intent(context, BarcodeScannerActivity::class.java).apply {
                            putExtra("mode", "select")
                        }
                        barcodeLauncher.launch(intent)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (barcodeValue.isNotEmpty())
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BarcodeReader,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "بارکد محصول",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (barcodeValue.isEmpty()) "برای اسکن ضربه بزنید"
                                   else "بارکد: $barcodeValue",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Label
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("عنوان آلارم") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Repeat options
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تکرار", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = isOneTime,
                                onClick = { isOneTime = true },
                                label = { Text("یک‌بار", fontSize = 12.sp) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = !isOneTime,
                                onClick = { isOneTime = false },
                                label = { Text("روزانه", fontSize = 12.sp) }
                            )
                        }
                    }

                    if (!isOneTime) {
                        Spacer(modifier = Modifier.height(16.dp))
                        // Day selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeatDays.forEachIndexed { index, selected ->
                                val dayChar = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")[index]
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            repeatDays = repeatDays.toMutableList().also {
                                                it[index] = !it[index]
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayChar,
                                        color = if (selected) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Settings Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("تنظیمات", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Scan timeout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("زمان رسیدن به بارکد", fontSize = 14.sp)
                        Text("$scanTimeoutSeconds ثانیه", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = scanTimeoutSeconds.toFloat(),
                        onValueChange = { scanTimeoutSeconds = it.toInt() },
                        valueRange = 10f..120f,
                        steps = 10,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Snooze
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مدت چرت (Snooze)", fontSize = 14.sp)
                        Text("$snoozeMinutes دقیقه", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = snoozeMinutes.toFloat(),
                        onValueChange = { snoozeMinutes = it.toInt() },
                        valueRange = 1f..30f,
                        steps = 28,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save button
            Button(
                onClick = {
                    val alarm = AlarmItem(
                        id = initialAlarm?.id ?: System.currentTimeMillis(),
                        hour = hour,
                        minute = minute,
                        label = label,
                        barcodeValue = barcodeValue,
                        isEnabled = true,
                        isOneTime = isOneTime,
                        repeatDays = repeatDays,
                        snoozeMinutes = snoozeMinutes,
                        scanTimeoutSeconds = scanTimeoutSeconds
                    )
                    onSave(alarm)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = barcodeValue.isNotEmpty()
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ذخیره آلارم", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            if (barcodeValue.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠️ برای فعال‌سازی آلارم باید یک بارکد اسکن کنید",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
