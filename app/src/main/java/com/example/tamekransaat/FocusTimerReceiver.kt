package com.example.tamekransaat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime

object FocusNotificationHelper {
    const val CHANNEL_ID = "focus_timer_channel_v2"

    fun showFocusCompletedNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // 1. DND (Rahatsız Etmeyin) modunu kapat (Bildirimin ve sesin engellenmemesi için ilk iş olarak açıyoruz)
        if (notificationManager.isNotificationPolicyAccessGranted) {
            try {
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Bildirim Kanalı Oluştur
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.focus_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.focus_channel_desc)
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        // 3. Tıklandığında MainActivity'yi açacak Intent
        val intent = Intent(context, MainActivity::class.java).apply {
            // Android'in mevcut Activity'i yok etmeden (yeniden başlatmadan) çağırması için
            // Single Top ve Reorder to Front kullanıyoruz.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            action = "ACTION_OPEN_FROM_NOTIFICATION"
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 4. Yüksek öncelikli bildirim oluştur
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(context.getString(R.string.focus_completed_title))
            .setContentText(context.getString(R.string.focus_completed_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
    }
}

class FocusTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = PreferencesManager(context)
        
        // Sadece sayaç aktif görünüyorsa çalıştır
        if (!prefs.isFocusRunning) return
        
        // Durumu güncelle
        prefs.isFocusRunning = false
        prefs.focusCompletedInBackground = true
        
        // Bugünün odaklanma süresini ve ısı haritasını kaydet
        val duration = prefs.focusOriginalDuration
        val now = LocalDateTime.now()
        val dateStr = now.toLocalDate().toString()
        
        repeat(duration) {
            prefs.addFocusMinute(dateStr)
            prefs.addFocusMinuteToHeatmap(now.dayOfWeek.value, now.hour)
        }
        
        // DND'yi kapat ve Bildirim göster
        FocusNotificationHelper.showFocusCompletedNotification(context)
        
        // Titreşim
        vibrate(context)
    }
    
    private fun vibrate(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
