package com.example.tamekransaat

import android.content.ComponentName
import android.content.Context
import android.appwidget.AppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetManager
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}

// Uygulama içinden Notlar değiştiğinde Widget'ı anında güncellemeye yarayan yardımcı fonksiyon
@OptIn(DelicateCoroutinesApi::class)
fun updateTaskWidget(context: Context) {
    // 1. Android sistemini doğrudan uyar (Native Broadcast)
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val componentName = ComponentName(context, TaskWidgetReceiver::class.java)
    val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
    
    val intent = Intent(context, TaskWidgetReceiver::class.java).apply {
        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
    }
    context.sendBroadcast(intent)

    // 2. Glance'ın kendisini zorla (Fallback)
    GlobalScope.launch {
        TaskWidget().updateAll(context)
    }
}

class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = PreferencesManager(context)
        
        provideContent {
            val today = LocalDate.now()
            val todayNotes = prefs.getNotesForDate(today.toString())
            
            // Eğer bugünkü notlar 3'ten azsa (Widget'ta yer varsa), gelecek 30 güne bakıp en yakın planı bul
            var upcomingDate: LocalDate? = null
            var upcomingNote: DayNote? = null
            
            if (todayNotes.size < 3) {
                for (i in 1..30) {
                    val futureDate = today.plusDays(i.toLong())
                    val futureNotes = prefs.getNotesForDate(futureDate.toString())
                    if (futureNotes.isNotEmpty()) {
                        upcomingDate = futureDate
                        upcomingNote = futureNotes.first()
                        break
                    }
                }
            }
            
            WidgetContent(notes = todayNotes, upcomingDate = upcomingDate, upcomingNote = upcomingNote, context = context)
        }
    }
}

@Composable
fun WidgetContent(notes: List<DayNote>, upcomingDate: LocalDate?, upcomingNote: DayNote?, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1E1E1E))
            .cornerRadius(16.dp)
            .padding(16.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java).apply {
                action = "ACTION_OPEN_CALENDAR"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }))
    ) {
        Text(
            text = "Günün Planı",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
            modifier = GlanceModifier.padding(bottom = 12.dp)
        )
        
        if (notes.isEmpty()) {
            Text(
                text = "Bugün için eklenmiş bir plan bulunmuyor.",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = 14.sp
                ),
                modifier = GlanceModifier.padding(bottom = 8.dp)
            )
        } else {
            notes.forEach { note ->
                Row(
                    modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(10.dp)
                            .background(Color(note.color))
                            .cornerRadius(5.dp)
                    ) {}
                    Spacer(modifier = GlanceModifier.width(8.dp))
                    Text(
                        text = "${note.timeStr} - ${note.text}",
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )
                }
            }
        }
        
        // Gelecek Etkinlik Bölümü (Eğer yer varsa ve bulunduysa)
        if (upcomingNote != null && upcomingDate != null) {
            Spacer(modifier = GlanceModifier.defaultWeight()) // Alta it
            
            val formatter = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
            Text(
                text = "Yaklaşan (${upcomingDate.format(formatter)}):",
                style = TextStyle(
                    color = ColorProvider(Color(0xFFAAAAAA)), // Daha silik bir gri
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(bottom = 4.dp, top = 8.dp)
            )
            
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(8.dp)
                        .background(Color(upcomingNote.color))
                        .cornerRadius(4.dp)
                ) {}
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = "${upcomingNote.timeStr} - ${upcomingNote.text}",
                    style = TextStyle(
                        color = ColorProvider(Color.LightGray),
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
            }
        }
    }
}