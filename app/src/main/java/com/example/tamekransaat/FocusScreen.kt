package com.example.tamekransaat

import androidx.compose.ui.text.style.TextAlign
import kotlin.random.Random
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import java.util.Locale
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent

fun scheduleFocusAlarm(context: Context, endTime: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, FocusTimerReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    val showIntent = Intent(context, MainActivity::class.java)
    val showPendingIntent = PendingIntent.getActivity(
        context, 0, showIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    
    try {
        // AlarmManager.setAlarmClock, Android'de pil tasarrufundan etkilenmeyen en kesin alarm yöntemidir.
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(endTime, showPendingIntent),
            pendingIntent
        )
    } catch (e: Exception) {
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
        } catch (_: Exception) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, endTime, pendingIntent)
        }
    }
}

fun cancelFocusAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, FocusTimerReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

@Composable
fun FocusScreen(
    preferencesManager: PreferencesManager,
    onExitFocus: () -> Unit
) {
    val context = LocalContext.current
    val maxMinutes = 60
    val maxSeconds = maxMinutes * 60
    var isRunning by remember { mutableStateOf(preferencesManager.isFocusRunning) } // Sayacın çalışma durumu
    var secondsLeft by remember { 
        mutableIntStateOf(
            if (preferencesManager.isFocusRunning) {
                val now = System.currentTimeMillis()
                val end = preferencesManager.focusEndTime
                if (now >= end) {
                    0
                } else {
                    ((end - now) / 1000).toInt()
                }
            } else {
                preferencesManager.getFocusDuration() * 60
            }
        ) 
    }
    var isDragging by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) } // Elle süre girme penceresi
    var manualInputText by remember { mutableStateOf("") } // Elle girilen süre metni
    var showCongratulation by remember { mutableStateOf(false) } // Kutlama yazısı durumu
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) } // Son dokunma zamanı
    var isSleepMode by remember { mutableStateOf(false) } // Uyku (Dim) modu

    // --- MİMARİ YENİDEN YAPILANDIRMA: TEK DOĞRU KAYNAK (SINGLE SOURCE OF TRUTH) ---
    // Artık FocusScreen kendi başına süre bitti kararı vermiyor.
    // Sürenin bitirilmesi, istatistiklerin kaydedilmesi ve bayrağın tetiklenmesi 
    // YALNIZCA AlarmManager (FocusTimerReceiver) tarafından yapılıyor.
    
    // 1. Yaşam Döngüsü (Lifecycle) Gözlemcisi
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var isResumed by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isResumed = true
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                isResumed = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 2. Uygulama Ön Plana Geldiğinde (Resume) veya Baştan Açıldığında Konfeti Kontrolü
    LaunchedEffect(isResumed) {
        if (isResumed && preferencesManager.focusCompletedInBackground) {
            preferencesManager.focusCompletedInBackground = false
            isRunning = false
            isDragging = false
            isSleepMode = false
            secondsLeft = preferencesManager.getFocusDuration() * 60
            showCongratulation = true
        }
    }

    // 3. Ön Planda Çalışan Sayaç Döngüsü (Yalnızca isRunning == true ise çalışır)
    LaunchedEffect(isRunning, isDragging) {
        if (isRunning && !isDragging) {
            while (true) {
                // AlarmManager arka planda süreyi bitirip bayrağı kaldırdıysa:
                if (!preferencesManager.isFocusRunning && preferencesManager.focusCompletedInBackground) {
                    preferencesManager.focusCompletedInBackground = false
                    isRunning = false
                    isSleepMode = false
                    secondsLeft = preferencesManager.getFocusDuration() * 60
                    showCongratulation = true
                    break // Döngüden çık
                }

                // Normal zaman hesabı
                val now = System.currentTimeMillis()
                val end = preferencesManager.focusEndTime
                if (now >= end) {
                    secondsLeft = 0
                    // DİKKAT: Buradan alarmı iptal etmiyoruz veya istatistik kaydetmiyoruz!
                    // Receiver'ın (AlarmManager) tetiklenmesini ve yukarıdaki 'if' bloğuna düşmesini bekliyoruz.
                } else {
                    secondsLeft = ((end - now) / 1000).toInt()
                }

                delay(250) // Çeyrek saniyede bir kontrol et (Hızlı tepkime için)
            }
        }
    }

    // 4. Konfeti Zamanlayıcısı
    LaunchedEffect(showCongratulation) {
        if (showCongratulation) {
            delay(4000) // Konfetiler ekranda 4 saniye kalsın
            showCongratulation = false
        }
    }

    // 5. Hareketsizlik (Uyku) Algılayıcı
    LaunchedEffect(lastInteractionTime) {
        delay(30000) // 30 saniye bekle
        isSleepMode = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Ekrana dokunulduğunda uyku modundan çık ve sayacı sıfırla
                        isSleepMode = false
                        lastInteractionTime = System.currentTimeMillis()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        
        AnimatedContent(
            targetState = showCongratulation,
            transitionSpec = {
                (fadeIn(animationSpec = tween(1000)) + scaleIn(initialScale = 0.8f, animationSpec = tween(1000))) togetherWith 
                (fadeOut(animationSpec = tween(1000)) + scaleOut(targetScale = 1.2f, animationSpec = tween(1000)))
            },
            label = "CongratulationAnimation"
        ) { isCelebrating ->
            if (isCelebrating) {
                // KUTLAMA EKRANI
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ConfettiExplosion()
                    Text(
                        text = stringResource(R.string.focus_congratulations),
                        color = Color.White,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 56.sp
                    )
                }
            } else {
                // NORMAL ODAK SAYACI ARAYÜZÜ
                // Ekran yüksekliğine göre dinamik boyutlandırma
                val configuration = LocalConfiguration.current
                val screenHeightDp = configuration.screenHeightDp
                
                val canvasSize = (screenHeightDp * 0.6f).coerceAtMost(320f).dp
                val fontSizeTime = (screenHeightDp * 0.15f).coerceAtMost(64f).sp

                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Çevrilebilir Yuvarlak Ayar Tuşu (Circular Slider Canvas)
                    Canvas(
                        modifier = Modifier
                            .size(canvasSize)
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { 
                                        isDragging = true
                                        isRunning = false // Sürüklemeye başlayınca sayacı otomatik duraklat
                                        preferencesManager.isFocusRunning = false
                                        cancelFocusAlarm(context)
                                        
                                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        if (notificationManager.isNotificationPolicyAccessGranted) {
                                            notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                                        }
                                        
                                        isSleepMode = false
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    onDragEnd = {
                                        isDragging = false
                                        // Sürükleme bitince yeni süreyi hafızaya kaydet
                                        val newMinutes = (secondsLeft / 60).coerceAtLeast(1)
                                        preferencesManager.saveFocusDuration(newMinutes)
                                        lastInteractionTime = System.currentTimeMillis()
                                    }
                                ) { change, _ ->
                                    lastInteractionTime = System.currentTimeMillis()
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val touchVector = change.position - center
                                    val angleRad = atan2(touchVector.y, touchVector.x)
                                    var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                                    
                                    angleDeg += 90f 
                                    if (angleDeg < 0) angleDeg += 360f
                                    
                                    val newMinutes = ((angleDeg / 360f) * maxMinutes).roundToInt()
                                    secondsLeft = newMinutes.coerceIn(1, maxMinutes) * 60
                                }
                            }
                    ) {
                        val progressAngle = (secondsLeft.toFloat() / maxSeconds) * 360f
                        
                        val trackColor = if (isSleepMode) Color(0xFF0A0A0A) else Color(0xFF1A1A1A)
                        val progressColor = if (isSleepMode) Color(0xFF444444) else Color.White
                        val knobInnerColor = if (isSleepMode) Color(0xFF222222) else Color.White
            
                        drawArc(
                            color = trackColor,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = -90f,
                            sweepAngle = progressAngle,
                            useCenter = false,
                            style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                        )
                        
                        val currentAngleRad = (progressAngle - 90f) * (PI / 180f)
                        val radius = size.width / 2f
                        val knobX = center.x + radius * cos(currentAngleRad).toFloat()
                        val knobY = center.y + radius * sin(currentAngleRad).toFloat()
                        
                        drawCircle(
                            color = knobInnerColor,
                            radius = 14.dp.toPx(),
                            center = Offset(knobX, knobY)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = 6.dp.toPx(),
                            center = Offset(knobX, knobY)
                        )
                    }
            
                    // Merkez Geri Sayım Sayacı
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val minutes = secondsLeft / 60
                        val seconds = secondsLeft % 60
                        
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .clickable {
                                    if (!isRunning && !isSleepMode) {
                                        manualInputText = minutes.toString()
                                        showManualInputDialog = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds),
                                color = if (isSleepMode) Color(0xFF666666) else Color.White,
                                fontSize = fontSizeTime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (!isSleepMode) {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Başla / Duraklat Butonu
                            if (isDragging) {
                                Text(
                                    text = stringResource(R.string.adjust_duration), 
                                    color = Color.White, 
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Button(
                                    onClick = { 
                                        if (secondsLeft > 0) {
                                            isRunning = !isRunning 
                                            if (isRunning) {
                                                preferencesManager.isFocusRunning = true
                                                val endTime = System.currentTimeMillis() + secondsLeft * 1000L
                                                preferencesManager.focusEndTime = endTime
                                                preferencesManager.focusOriginalDuration = (secondsLeft / 60).coerceAtLeast(1)
                                                scheduleFocusAlarm(context, endTime)
                                                
                                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                                if (notificationManager.isNotificationPolicyAccessGranted) {
                                                    notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS)
                                                }
                                            } else {
                                                preferencesManager.isFocusRunning = false
                                                cancelFocusAlarm(context)
                                                
                                                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                                if (notificationManager.isNotificationPolicyAccessGranted) {
                                                    notificationManager.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
                                                }
                                            }
                                        }
                                        lastInteractionTime = System.currentTimeMillis()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isRunning) Color(0xFF1E1E1E) else Color.White,
                                        contentColor = if (isRunning) Color.LightGray else Color.Black
                                    ),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(
                                        text = if (isRunning) stringResource(R.string.pause) else stringResource(R.string.start), 
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
            
                    // Hızlı Süre Önerileri (Telefon ekranları için dikey/sola dayalı, Tabletler için yatay/alta dayalı)
                    if (!isRunning && !isDragging && !isSleepMode) {
                        val isPhoneLandscape = screenHeightDp < 500
                        if (isPhoneLandscape) {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                QuickTimeButton(minutes = 15) { 
                                    secondsLeft = 15 * 60
                                    preferencesManager.saveFocusDuration(15)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                QuickTimeButton(minutes = 30) { 
                                    secondsLeft = 30 * 60
                                    preferencesManager.saveFocusDuration(30)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                QuickTimeButton(minutes = 45) { 
                                    secondsLeft = 45 * 60
                                    preferencesManager.saveFocusDuration(45)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                QuickTimeButton(minutes = 60) { 
                                    secondsLeft = 60 * 60
                                    preferencesManager.saveFocusDuration(60)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 48.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuickTimeButton(minutes = 15) { 
                                    secondsLeft = 15 * 60
                                    preferencesManager.saveFocusDuration(15)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                QuickTimeButton(minutes = 30) { 
                                    secondsLeft = 30 * 60
                                    preferencesManager.saveFocusDuration(30)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                QuickTimeButton(minutes = 45) { 
                                    secondsLeft = 45 * 60
                                    preferencesManager.saveFocusDuration(45)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                                QuickTimeButton(minutes = 60) { 
                                    secondsLeft = 60 * 60
                                    preferencesManager.saveFocusDuration(60)
                                    lastInteractionTime = System.currentTimeMillis()
                                }
                            }
                        }
                    }
            
                    // Odağı manuel bitirme (X İkonu)
                    if (!isSleepMode) {
                        IconButton(
                            onClick = {
                                lastInteractionTime = System.currentTimeMillis()
                                preferencesManager.isFocusRunning = false
                                cancelFocusAlarm(context)
                                onExitFocus()
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .background(Color(0xFF121212), CircleShape)
                        ) {
                            Text("✕", color = Color.Gray, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
        
        // Elle Süre Girme Penceresi (Dialog)
        if (showManualInputDialog) {
            AlertDialog(
                onDismissRequest = { showManualInputDialog = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text(stringResource(R.string.focus_duration_minutes), color = Color.White) },
                text = {
                    OutlinedTextField(
                        value = manualInputText,
                        onValueChange = { 
                            if (it.length <= 3 && it.all { char -> char.isDigit() }) manualInputText = it 
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(color = Color.White, fontSize = 24.sp),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Gray,
                            unfocusedIndicatorColor = Color.DarkGray
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newMins = manualInputText.toIntOrNull()?.coerceIn(1, 120) ?: 25
                            secondsLeft = newMins * 60
                            preferencesManager.saveFocusDuration(newMins)
                            showManualInputDialog = false
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF444444))
                    ) {
                        Text(stringResource(R.string.save), color = Color.White)
                    }
                }
            )
        }
    }
}

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val radius: Float
)

@Composable
fun ConfettiExplosion() {
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    val colors = listOf(Color(0xFFE91E63), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFFFC107), Color(0xFF9C27B0), Color(0xFF00BCD4), Color.White)
    
    LaunchedEffect(Unit) {
        val newParticles = List(100) {
            val angle = Random.nextDouble(0.0, 2 * PI)
            val speed = Random.nextDouble(15.0, 40.0)
            Particle(
                x = 0f, 
                y = 0f,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                color = colors.random(),
                radius = Random.nextFloat() * 12f + 6f
            )
        }
        particles = newParticles

        val startTime = withFrameNanos { it }
        while (true) {
            withFrameNanos { frameTime ->
                val dt = (frameTime - startTime) / 1_000_000_000f 
                particles = particles.map { p ->
                    p.copy(
                        x = p.x + p.vx,
                        y = p.y + p.vy,
                        vy = p.vy + 0.6f
                    )
                }
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        particles.forEach { p ->
            drawCircle(
                color = p.color,
                radius = p.radius,
                center = Offset(center.x + p.x, center.y + p.y)
            )
        }
    }
}

@Composable
fun QuickTimeButton(minutes: Int, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
        modifier = Modifier.size(56.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = minutes.toString(),
            color = Color.Gray,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
    }
}