package com.example.tamekransaat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput


@Composable
fun GoogleCalendarStyleWeeklyView(preferencesManager: PreferencesManager, onEditNoteRequested: (LocalDate, String) -> Unit) {
    var weeksOffset by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val today = LocalDate.now()
    val startOfTargetWeek = today.plusWeeks(weeksOffset.toLong()).with(java.time.DayOfWeek.MONDAY)
    val endOfTargetWeek = startOfTargetWeek.plusDays(6)

    val days = (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) }
    val months = (1..12).map { java.time.Month.of(it).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.width(500.dp).padding(bottom = 16.dp)
        ) {
            IconButton(onClick = { weeksOffset-- }) {
                Text("◀", color = Color.White, fontSize = 20.sp)
            }

            Text(
                text = "${startOfTargetWeek.dayOfMonth} ${months[startOfTargetWeek.monthValue - 1]} - ${endOfTargetWeek.dayOfMonth} ${months[endOfTargetWeek.monthValue - 1]} ${endOfTargetWeek.year}",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(onClick = { weeksOffset++ }) {
                Text("▶", color = Color.White, fontSize = 20.sp)
            }
        }

        val verticalScrollState = rememberScrollState()

        Row(
            modifier = Modifier.fillMaxSize().verticalScroll(verticalScrollState)
        ) {
            Column(
                modifier = Modifier.width(50.dp).padding(top = 40.dp)
            ) {
                for (hour in 0..23) {
                    Box(modifier = Modifier.height(60.dp), contentAlignment = Alignment.TopEnd) {
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:00", hour),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                    for (i in 0..6) {
                        val date = startOfTargetWeek.plusDays(i.toLong())
                        val isToday = date == today
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isToday) Color(0xFF333333) else Color.Transparent, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${days[i]} ${date.dayOfMonth}",
                                color = if (isToday) Color(0xFF4CAF50) else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(modifier = Modifier.fillMaxWidth().height(1440.dp)) {
                    // Izgara (Grid) Çizgileri
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (hour in 0..23) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            ) {
                                for (i in 0..6) {
                                    val date = startOfTargetWeek.plusDays(i.toLong())
                                    val clickedHour = String.format(Locale.getDefault(), "%02d:00", hour)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, Color(0xFF222222))
                                            .clickable { onEditNoteRequested(date, clickedHour) } // Hangi saate tıklandığını gönder
                                    )
                                }
                            }
                        }
                    }

                    // Etkinlik Blokları (Canvas Mantığı)
                    Row(modifier = Modifier.fillMaxSize()) {
                        for (i in 0..6) {
                            val date = startOfTargetWeek.plusDays(i.toLong())
                            val dummy = refreshTrigger
                            val dayNotes = preferencesManager.getNotesForDate(date.toString())

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                dayNotes.forEach { note ->
                                    val startHour = note.timeStr.take(2).toIntOrNull() ?: 9
                                    val startMin = note.timeStr.drop(3).take(2).toIntOrNull() ?: 0
                                    val startOffsetMinutes = (startHour * 60) + startMin

                                    val endHour = note.endTimeStr.take(2).toIntOrNull() ?: (startHour + 1)
                                    val endMin = note.endTimeStr.drop(3).take(2).toIntOrNull() ?: startMin
                                    var durationMinutes = ((endHour * 60) + endMin) - startOffsetMinutes
                                    
                                    // Bitiş saati başlangıçtan önceyse (geçersizse) veya boşsa varsayılan 1 saat yap
                                    if (durationMinutes <= 0) durationMinutes = 60
                                    
                                    // Çakışma algoritmasını basitleştirmek için tam genişlik veriyoruz (Gerçek Google Calendar gibi çoklu sütun için daha kompleks hesap gerekir)
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 2.dp)
                                            .fillMaxWidth()
                                            .offset(y = startOffsetMinutes.dp) // 1 dakika = 1 dp (60dp = 1 saat)
                                            .height(durationMinutes.dp)
                                            .background(Color(note.color).copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                                            .clickable { onEditNoteRequested(date, note.timeStr) }
                                            .padding(4.dp),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        Column {
                                            Text(
                                                text = if (note.endTimeStr.isNotEmpty()) "${note.timeStr} - ${note.endTimeStr}" else note.timeStr,
                                                color = Color.White,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = note.text,
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = if (durationMinutes < 30) 1 else 3
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
