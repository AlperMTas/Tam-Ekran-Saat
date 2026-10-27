package com.example.tamekransaat

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import android.os.Build
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import androidx.compose.ui.res.stringResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var notificationManager: NotificationManager
    
    // Uygulama genel durumunu burada tutup Widget'tan gelen komutlara göre güncelleyeceğiz
    private val appState = mutableStateOf(AppState.AMBIENT)
    private val initialCalendarTab = mutableStateOf(CalendarTab.GROWTH)

    // Uygulama herhangi bir şekilde ekrana geldiğinde, arka planda bitmiş bir odak süresi varsa
    // doğrudan odak (konfeti) ekranına geçilmesini garantiler.
    override fun onResume() {
        super.onResume()
        if (::preferencesManager.isInitialized && preferencesManager.focusCompletedInBackground) {
            appState.value = AppState.FOCUS
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferencesManager = PreferencesManager(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Dinamik olarak yatay modda (landscape) çalışmaya zorla (Özellikle büyük tabletler/Android 12+ için)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Ekranı uyanık tut ve tam ekran (Immersive) moduna geç
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Android 10+ (Q ve sonrası) için Sistem Barları (Navigasyon ve Durum Çubuğu) kontrast koruması
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        
        // Android 13+ (TIRAMISU) için Bildirim İzni İste (Odak Süresi Bittiğinde Gerekiyor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Çentikli (Notch) ve delikli ekranlarda üstte siyah/beyaz boşluk kalmasını engelle
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        // Bildirim Servisini (Worker) Başlat - Günde 1 Kez (24 saatte bir) çalışır
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // Eğer uygulama kapalıyken Widget'a basılmışsa Takvimi aç
        handleIntent(intent)

        setContent {
            MaterialTheme {
                MainScreen(
                    preferencesManager = preferencesManager,
                    notificationManager = notificationManager,
                    appState = appState.value,
                    initialCalendarTab = initialCalendarTab.value,
                    onAppStateChange = { appState.value = it },
                    onRequestDndPermission = {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    // Uygulama zaten açıkken Widget'a basılırsa burası tetiklenir
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "ACTION_OPEN_CALENDAR") {
            appState.value = AppState.CALENDAR
            initialCalendarTab.value = CalendarTab.MONTHLY
            intent.action = null // İkinci kez aynı intentin tetiklenmesini engelle
        } else {
            // İster bildirimden (ACTION_OPEN_FROM_NOTIFICATION) gelsin
            // ister normal simgeden (Launcher) gelsin, eğer sayaç arka planda bitmişse
            // her halükarda uygulamanın doğrudan FOCUS (Odak) ekranında açılmasını sağla
            if (preferencesManager.focusCompletedInBackground) {
                // Focus'a geç ve ondan sonra state değişimi compose'da tetiklensin
                appState.value = AppState.FOCUS
            }
            
            // Eğer bildirimden gelmişse Intent'i temizle
            if (intent?.action == "ACTION_OPEN_FROM_NOTIFICATION") {
                intent.action = null
            }
            
            initialCalendarTab.value = CalendarTab.GROWTH
        }
    }
    
    // Uygulama kapandığında veya arka plana atıldığında DND (Rahatsız Etmeyin) modunu kapat
    override fun onDestroy() {
        super.onDestroy()
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        }
    }
}

@Composable
fun MainScreen(
    preferencesManager: PreferencesManager,
    notificationManager: NotificationManager,
    appState: AppState,
    initialCalendarTab: CalendarTab,
    onAppStateChange: (AppState) -> Unit,
    onRequestDndPermission: () -> Unit
) {
    val burnInOffset by rememberBurnInOffset()
    
    // DND izni isteme penceresini göstermek için kontrol değişkeni
    var showDndDialog by remember { mutableStateOf(false) }
    
    // Öğretici ekranını (Tooltip) göstermek için kontrol değişkeni
    var showTutorial by remember { mutableStateOf(!preferencesManager.hasSeenCalendarTutorial) }

    // Ekrana çift tıklama (Double Tap) veya uzun basma (Long Press) algılama
    // Yalnızca Ana Saat Ekranındayken (AMBIENT) takvimi açmaya izin ver
    Box(
        modifier = Modifier
            .fillMaxSize()
            // appState değiştiğinde pointerInput'un yeniden değerlendirilmesini (yeniden yakalanmasını) sağla!
            .pointerInput(appState) {
                detectTapGestures(
                    onDoubleTap = {
                        if (appState == AppState.AMBIENT) {
                            if (!preferencesManager.hasSeenCalendarTutorial) {
                                preferencesManager.hasSeenCalendarTutorial = true
                                showTutorial = false
                            }
                            onAppStateChange(AppState.CALENDAR)
                        }
                    },
                    onLongPress = {
                        if (appState == AppState.AMBIENT) {
                            if (!preferencesManager.hasSeenCalendarTutorial) {
                                preferencesManager.hasSeenCalendarTutorial = true
                                showTutorial = false
                            }
                            onAppStateChange(AppState.CALENDAR)
                        }
                    }
                )
            }
    ) {
        // Durumlar arası geçişte Crossfade animasyonu
        Crossfade(targetState = appState, label = "StateTransition") { state ->
            
            // Burn-in koruması yalnızca Ambient modunda uygulanır (odak ve takvim aktifken UI yerinden oynamasın)
            val currentOffset = if (state == AppState.AMBIENT) burnInOffset else IntOffset.Zero
            
            Box(modifier = Modifier.offset { currentOffset }) {
                when (state) {
                    AppState.AMBIENT -> {
                        AmbientScreen(
                            onStartFocus = {
                                // Odak moduna geçerken DND (Rahatsız Etmeyin) yetkisini kontrol et
                                if (notificationManager.isNotificationPolicyAccessGranted) {
                                    // Yetki varsa sessize al (Sadece alarmlar çalar) ve odak moduna geç
                                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                                    onAppStateChange(AppState.FOCUS)
                                } else if (preferencesManager.dontAskDndAgain) {
                                    // Kullanıcı daha önce "Tekrar sorma / Gerek yok" dediyse sormadan direkt başlat
                                    onAppStateChange(AppState.FOCUS)
                                } else {
                                    // Yetki yoksa ve tekrar sorma demediyse kullanıcıya sor
                                    showDndDialog = true
                                }
                            }
                        )
                    }
                    AppState.FOCUS -> {
                        FocusScreen(
                            preferencesManager = preferencesManager,
                            onExitFocus = {
                                // Odak modundan çıkarken bildirimleri normale döndür
                                if (notificationManager.isNotificationPolicyAccessGranted) {
                                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                                }
                                onAppStateChange(AppState.AMBIENT) 
                            }
                        )
                    }
                    AppState.CALENDAR -> {
                        CalendarScreen(
                            preferencesManager = preferencesManager,
                            initialTab = initialCalendarTab,
                            onClose = { onAppStateChange(AppState.AMBIENT) }
                        )
                    }
                }
            }
        }
        
        // İlk Kullanım Öğreticisi (Tooltip)
        androidx.compose.animation.AnimatedVisibility(
            visible = appState == AppState.AMBIENT && showTutorial,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(androidx.compose.ui.Alignment.TopCenter).padding(top = 48.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .background(Color(0xE61E1E1E), shape = RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tutorial_text),
                    color = Color.White,
                    fontSize = 14.sp
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "✕",
                    color = Color.Gray,
                    modifier = Modifier
                        .clickable { 
                            preferencesManager.hasSeenCalendarTutorial = true
                            showTutorial = false
                        }
                        .padding(8.dp)
                )
            }
        }
        
        // DND (Rahatsız Etmeyin) İzni Sorma Penceresi
        if (showDndDialog) {
            AlertDialog(
                onDismissRequest = { 
                    // Dışarı tıklarsa tekrar sorma olarak kaydet ve direkt odak modunu başlat
                    preferencesManager.dontAskDndAgain = true
                    showDndDialog = false
                    onAppStateChange(AppState.FOCUS)
                },
                containerColor = Color(0xFF161618),
                shape = RoundedCornerShape(24.dp),
                title = { 
                    Text(
                        text = stringResource(R.string.focus_mode_experience), 
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = { 
                    Text(
                        text = stringResource(R.string.focus_dnd_prompt), 
                        color = Color.LightGray,
                        fontSize = 16.sp
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            showDndDialog = false
                            onRequestDndPermission() // Ayarlara yönlendir
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text(stringResource(R.string.allow_settings), color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            // Kullanıcı gerek yok derse bir daha sormaması için kaydet ve başlat
                            preferencesManager.dontAskDndAgain = true
                            showDndDialog = false
                            onAppStateChange(AppState.FOCUS)
                        }
                    ) {
                        Text(stringResource(R.string.dont_ask_start), color = Color.Gray)
                    }
                }
            )
        }
    }
}