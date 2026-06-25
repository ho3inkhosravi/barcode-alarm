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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bed
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barcodealarm.app.data.AlarmItem
import com.barcodealarm.app.scanner.BarcodeScannerActivity

// مراحل مختلف آلارم در حال اجرا
enum class AlarmStage { RINGING, AWAKE_CHECK, MUTE_TIMER, DONE }

// تولید مسئله ریاضی ساده (جمع، تفریق، ضرب اعداد ۱ رقمی)
data class MathProblem(val a: Int, val b: Int, val op: Char, val answer: Int) {
    fun toText(): String {
        val opSymbol = when (op) {
            '+' -> "+"
            '-' -> "−"
            '*' -> "×"
            else -> "+"
        }
        return "$a $opSymbol $b"
    }
}

fun generateMathProblem(): MathProblem {
    val ops = listOf('+', '-', '*')
    val op = ops.random()
    return when (op) {
        '+' -> {
            val a = (1..9).random()
            val b = (1..9).random()
            MathProblem(a, b, '+', a + b)
        }
        '-' -> {
            val a = (5..9).random()
            val b = (1..a).random()
            MathProblem(a, b, '-', a - b)
        }
        else -> {
            val a = (1..5).random()
            val b = (1..5).random()
            MathProblem(a, b, '*', a * b)
        }
    }
}

@Composable
fun RingingScreen(
    alarm: AlarmItem,
    onSnooze: () -> Unit,
    onScanResult: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(AlarmStage.RINGING) }

    // مسئله ریاضی
    var mathProblem by remember { mutableStateOf(generateMathProblem()) }
    var answerInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    // تایمر زمان رسیدن به بارکد
    val totalSeconds = alarm.scanTimeoutSeconds.coerceIn(10, 120)
    var remainingSeconds by remember { mutableStateOf(totalSeconds) }
    var timerFinished by remember { mutableStateOf(false) }

    // نوع چک بیداری: "mute" (برای قطع) یا "snooze" (برای چرت)
    var awakeCheckMode by remember { mutableStateOf("mute") }

    // اسکنر بارکد
    val barcodeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val match = result.data?.getBooleanExtra("match", false) ?: false
        onScanResult(match)
    }

    // شروع تایمر وقتی وارد مرحله MUTE_TIMER شدیم
    LaunchedEffect(stage) {
        if (stage == AlarmStage.MUTE_TIMER) {
            // صدا رو قطع کن
            com.barcodealarm.app.alarm.AlarmService.mute()
            remainingSeconds = totalSeconds
            timerFinished = false
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
    }

    val progress = animateFloatAsState(
        targetValue = if (timerFinished) 0f else remainingSeconds.toFloat() / totalSeconds.toFloat(),
        animationSpec = tween(1000),
        label = "progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (stage) {
            AlarmStage.RINGING -> {
                RingingContent(
                    alarm = alarm,
                    onMuteClick = {
                        awakeCheckMode = "mute"
                        mathProblem = generateMathProblem()
                        answerInput = ""
                        mathError = false
                        stage = AlarmStage.AWAKE_CHECK
                    },
                    onSnooze = {
                        awakeCheckMode = "snooze"
                        mathProblem = generateMathProblem()
                        answerInput = ""
                        mathError = false
                        stage = AlarmStage.AWAKE_CHECK
                    }
                )
            }

            AlarmStage.AWAKE_CHECK -> {
                AwakeCheckContent(
                    problem = mathProblem,
                    answerInput = answerInput,
                    mathError = mathError,
                    onAnswerChange = { answerInput = it; mathError = false },
                    onConfirm = {
                        if (answerInput.trim() == mathProblem.answer.toString()) {
                            if (awakeCheckMode == "snooze") {
                                onSnooze()
                            } else {
                                stage = AlarmStage.MUTE_TIMER
                            }
                        } else {
                            mathError = true
                            mathProblem = generateMathProblem()
                            answerInput = ""
                        }
                    },
                    onBack = { stage = AlarmStage.RINGING }
                )
            }

            AlarmStage.MUTE_TIMER -> {
                MuteTimerContent(
                    remainingSeconds = remainingSeconds,
                    timerFinished = timerFinished,
                    progress = progress.value,
                    onScanNow = {
                        val intent = Intent(context, BarcodeScannerActivity::class.java).apply {
                            putExtra("expected_barcode", alarm.barcodeValue)
                            putExtra("mode", "alarm")
                        }
                        barcodeLauncher.launch(intent)
                    }
                )
            }

            AlarmStage.DONE -> {}
        }
    }
}

// === محتوای مرحله زنگ خوردن ===
@Composable
fun RingingContent(
    alarm: AlarmItem,
    onMuteClick: () -> Unit,
    onSnooze: () -> Unit
) {
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
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) { Text("⏰", fontSize = 56.sp) }

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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "برای قطع صدا، مسئله ریاضی حل کنید",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = onMuteClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.VolumeOff, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("بی‌صدا کردن", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

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

// === محتوای بررسی بیداری (مسئله ریاضی) ===
@Composable
fun AwakeCheckContent(
    problem: MathProblem,
    answerInput: String,
    mathError: Boolean,
    onAnswerChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    val keyboard = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🧠 آیا بیدار هستی؟", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "جواب مسئله را وارد کن تا مطمئن شیم بیداری",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = problem.toText() + " = ?",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = answerInput,
                    onValueChange = onAnswerChange,
                    label = { Text("جواب شما") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    isError = mathError,
                    supportingText = if (mathError) {
                        { Text("جواب اشتباه! دوباره امتحان کن", color = MaterialTheme.colorScheme.error) }
                    } else null
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        keyboard?.hide()
                        onConfirm()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = answerInput.isNotBlank()
                ) {
                    Text("تأیید", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        TextButton(onClick = onBack) {
            Text("برگشت به آلارم", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

// === محتوای تایم رسیدن به بارکد ===
@Composable
fun MuteTimerContent(
    remainingSeconds: Int,
    timerFinished: Boolean,
    progress: Float,
    onScanNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🤫 صدا قطع شد", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (!timerFinished) "فرصت رسیدن به بارکد"
                       else "زمان تمام شد! همین حالا اسکن کن",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }

        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${remainingSeconds}",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text("ثانیه", fontSize = 16.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }

        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = Color.White.copy(alpha = 0.2f)
        )

        Button(
            onClick = onScanNow,
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("اسکن بارکد", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}
