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
fun ClassicMonthView(preferencesManager: PreferencesManager) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now()
    val daysInMonth = currentYearMonth.lengthOfMonth()

    var selectedDateForNote by remember { mutableStateOf<LocalDate?>(null) }

    var showMonthPicker by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableIntStateOf(currentYearMonth.year) }

    val monthsTr = listOf("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran", "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık")
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.width(450.dp).padding(bottom = 24.dp)
            ) {
                IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                    Text("◀", color = Color.White, fontSize = 24.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${monthsTr[currentYearMonth.monthValue - 1]} ${currentYearMonth.year}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Transparent)
                            .clickable {
                                pickerYear = currentYearMonth.year
                                showMonthPicker = true
                            }
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    if (currentYearMonth != YearMonth.now()) {
                        Text(
                            text = "Bugüne Dön",
                            color = Color(0xFF4CAF50),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { currentYearMonth = YearMonth.now() }
                        )
                    } else {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                    Text("▶", color = Color.White, fontSize = 24.sp)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.width(600.dp)
            ) {
                val daysOfWeek = listOf("PZT", "SAL", "ÇAR", "PER", "CUM", "CTS", "PAZ")
                items(7) { index ->
                    Text(
                        text = daysOfWeek[index],
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                val prefixDays = firstDayOfWeek - 1
                val trailingDays = 42 - (prefixDays + daysInMonth)

                items(prefixDays) {
                    Box(modifier = Modifier.padding(16.dp))
                }

                items(daysInMonth) { day ->
                    val date = currentYearMonth.atDay(day + 1)
                    val isToday = date == today

                    val dayNotes = preferencesManager.getNotesForDate(date.toString())

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .background(
                                color = if (isToday) Color.White else Color(0xFF1E1E1E),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedDateForNote = date },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (day + 1).toString(),
                            color = if (isToday) Color.Black else Color.White,
                            fontSize = 20.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )

                        if (dayNotes.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dayNotes.forEach { note ->
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(note.color), RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }

                if (trailingDays > 0) {
                    items(trailingDays) {
                        Box(modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }

        if (selectedDateForNote != null) {
            NoteEditDialog(
                date = selectedDateForNote!!,
                preferencesManager = preferencesManager,
                onDismiss = { selectedDateForNote = null }
            )
        }

        if (showMonthPicker) {
            AlertDialog(
                onDismissRequest = { showMonthPicker = false },
                containerColor = Color(0xFF1A1A1A),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { pickerYear-- }) {
                            Text("◀", color = Color.White)
                        }
                        Text(pickerYear.toString(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { pickerYear++ }) {
                            Text("▶", color = Color.White)
                        }
                    }
                },
                text = {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(12) { index ->
                            val isSelected = currentYearMonth.year == pickerYear && currentYearMonth.monthValue == index + 1
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isSelected) Color(0xFF4CAF50) else Color(0xFF333333),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        currentYearMonth = YearMonth.of(pickerYear, index + 1)
                                        showMonthPicker = false
                                    }
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = monthsTr[index],
                                    color = if (isSelected) Color.Black else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMonthPicker = false }) {
                        Text("İptal", color = Color.Gray)
                    }
                }
            )
        }
    }
}
