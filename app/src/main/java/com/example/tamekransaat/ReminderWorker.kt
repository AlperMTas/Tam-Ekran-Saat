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
            sendNotification(applicationContext.getString(R.string.upcoming_event_title), applicationContext.getString(R.string.upcoming_event_desc, upcomingEvent.timeStr, upcomingEvent.text), 101)
            notificationSent = true
        }

        // 2. KONTROL: Bugünkü Görevler (Eğer etkinlik bildirimi atılmadıysa)
        if (!notificationSent) {
            val todayTask = todayNotes.firstOrNull { it.type == NoteType.TASK }
            if (todayTask != null) {
                sendNotification(applicationContext.getString(R.string.todays_task_title), applicationContext.getString(R.string.todays_task_desc, todayTask.text), 102)
                notificationSent = true
            }
        }

        // 3. KONTROL: Teşvik edici dostça mesajlar (Eğer özel bir görev/etkinlik yoksa %50 şansla gönderir)
        if (!notificationSent && Math.random() > 0.5) {
            val motivations = listOf(
                applicationContext.getString(R.string.motivation_1),
                applicationContext.getString(R.string.motivation_2),
                applicationContext.getString(R.string.motivation_3),
                applicationContext.getString(R.string.motivation_4)
            )
            sendNotification(applicationContext.getString(R.string.focus_time_title), motivations.random(), 103)
        }

        return Result.success()
    }

    private fun sendNotification(title: String, desc: String, notificationId: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "tam_ekran_saat_reminders"
        
        // Android 8 (Oreo) ve üzeri için Bildirim Kanalı (Channel) oluşturulması zorunludur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, applicationContext.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = applicationContext.getString(R.string.reminder_channel_desc)
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