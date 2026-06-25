package com.barcodealarm.app.ui

import android.content.Intent
import android.os.CountDownTimer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarcodeReader
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barcodealarm.app.data.AlarmItem
import com.barcodealarm.app.scanner.BarcodeScannerActivity

@Composable
fun RingingScreen(
    alarm: AlarmItem,
    onSnooze: () -> Unit,
    onScanResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val totalSeconds = alarm.scanTimeoutSeconds.coerceIn(10, 120)
    var remainingSeconds by remember { mutableStateOf(totalSeconds) }
    var timerFinished by remember { mutableStateOf(false) }

    val barcodeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val match = result.data?.getBooleanExtra("match", false) ?: false
        onScanResult(match)
    }

    LaunchedEffect(alarm.id) {
        object : CountDownTimer((totalSeconds * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
            }

            override fun onFinish() {
                timerFinished = true
                remainingSeconds = 0
            }
        }.start()
    }

    val progress = animateFloatAsState(
        targetValue = if (timerFinished) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(1000),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.error,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Alarm icon
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text("⏰", fontSize = 56.sp)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (alarm.label.isNotEmpty()) {
                Text(
                    text = alarm.label,
                    fontSize = 20.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = alarm.getTimeText(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Timer
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!timerFinished) {
                Text(
                    text = "زمان باقی‌مانده برای اسکن",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${remainingSeconds}",
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "ثانیه",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { progress.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
            } else {
                Text(
                    text = "⚠️ زمان تمام شد!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Scan button
        Button(
            onClick = {
                val intent = Intent(context, BarcodeScannerActivity::class.java).apply {
                    putExtra("expected_barcode", alarm.barcodeValue)
                    putExtra("mode", "alarm")
                }
                barcodeLauncher.launch(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.BarcodeReader, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("اسکن بارکد", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        // Snooze button
        OutlinedButton(
            onClick = onSnooze,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Icon(Icons.Default.Bed, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("چرت ${alarm.snoozeMinutes} دقیقه‌ای", fontSize = 16.sp)
        }
    }
}
