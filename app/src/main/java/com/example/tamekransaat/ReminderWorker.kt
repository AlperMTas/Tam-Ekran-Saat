package com.example.tamekransaat

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = PreferencesManager(applicationContext)
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        
        val todayNotes = prefs.getNotesForDate(today.toString())
        val tomorrowNotes = prefs.getNotesForDate(tomorrow.toString())
        
        var notificationSent = false

        // 1. KONTROL: Yarınki Etkinlikler
        val upcomingEvent = tomorrowNotes.firstOrNull { it.type == NoteType.EVENT }
        if (upcomingEvent != null) {
            sendNotification("Yaklaşan Etkinlik \uD83D\uDCC5", "Yarın ${upcomingEvent.timeStr} sularında bir etkinliğin var: ${upcomingEvent.text}", 101)
            notificationSent = true
        }

        // 2. KONTROL: Bugünkü Görevler (Eğer etkinlik bildirimi atılmadıysa)
        if (!notificationSent) {
            val todayTask = todayNotes.firstOrNull { it.type == NoteType.TASK }
            if (todayTask != null) {
                sendNotification("Günün Görevi \uD83D\uDCCC", "Bugün halletmen gereken bir iş var: ${todayTask.text}", 102)
                notificationSent = true
            }
        }

        // 3. KONTROL: Teşvik edici dostça mesajlar (Eğer özel bir görev/etkinlik yoksa %50 şansla gönderir)
        if (!notificationSent && Math.random() > 0.5) {
            val motivations = listOf(
                "Harika bir gün! Odaklanmak için mükemmel bir zaman. ✨",
                "Küçük adımlar büyük başarılar getirir. 25 dakika odaklanmaya ne dersin?",
                "Zihnin hazır mı? Masana geç ve gününü fethet! ⏳",
                "Bugün de harika işler başaracağına eminim! \uD83D\uDE80"
            )
            sendNotification("Odaklanma Vakti \uD83D\uDE80", motivations.random(), 103)
        }

        return Result.success()
    }

    private fun sendNotification(title: String, desc: String, notificationId: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tam_ekran_saat_reminders"
        
        // Android 8 (Oreo) ve üzeri için Bildirim Kanalı (Channel) oluşturulması zorunludur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Günlük Hatırlatmalar", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Görevler ve teşvik mesajları için bildirimler"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Varsayılan basit bir info ikonu
            .setContentTitle(title)
            .setContentText(desc)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        notificationManager.notify(notificationId, builder.build())
    }
}