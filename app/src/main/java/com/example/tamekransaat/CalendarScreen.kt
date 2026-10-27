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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun CalendarScreen(preferencesManager: PreferencesManager, initialTab: CalendarTab = CalendarTab.GROWTH, onClose: () -> Unit) {
    var currentTab by remember { mutableStateOf(initialTab) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Üst Bar & Gezinme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TabButton(stringResource(R.string.growth_center), currentTab == CalendarTab.GROWTH) { currentTab = CalendarTab.GROWTH }
                TabButton(stringResource(R.string.biorhythm_heatmap), currentTab == CalendarTab.BIORHYTHM) { currentTab = CalendarTab.BIORHYTHM }
                TabButton(stringResource(R.string.weekly_schedule), currentTab == CalendarTab.WEEKLY_SCHEDULE) { currentTab = CalendarTab.WEEKLY_SCHEDULE }
                TabButton(stringResource(R.string.monthly_calendar), currentTab == CalendarTab.MONTHLY) { currentTab = CalendarTab.MONTHLY }
            }
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                Text(stringResource(R.string.close), color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        when (currentTab) {
            CalendarTab.GROWTH -> GrowthCenterView(preferencesManager)
            CalendarTab.BIORHYTHM -> BiorhythmHeatmap(preferencesManager)
            CalendarTab.WEEKLY_SCHEDULE -> {
                var selectedDateForEdit by remember { mutableStateOf<LocalDate?>(null) }
                var initialHourForEdit by remember { mutableStateOf<String?>(null) }
                var triggerState by remember { mutableIntStateOf(0) }
                
                // Haftalık görünümde bir boşluğa veya etkinliğe tıklanırsa
                Box {
                    GoogleCalendarStyleWeeklyView(
                        preferencesManager = preferencesManager,
                        onEditNoteRequested = { date, hourStr -> 
                            selectedDateForEdit = date 
                            initialHourForEdit = hourStr
                        }
                    )
                    
                    if (selectedDateForEdit != null) {
                        NoteEditDialog(
                            date = selectedDateForEdit!!,
                            preferencesManager = preferencesManager,
                            initialTimeStr = initialHourForEdit,
                            onDismiss = { 
                                selectedDateForEdit = null 
                                initialHourForEdit = null
                                triggerState++
                            }
                        )
                    }
                }
            }
            CalendarTab.MONTHLY -> ClassicMonthView(preferencesManager)
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color.White else Color(0xFF1E1E1E),
            contentColor = if (isSelected) Color.Black else Color.Gray
        )
    ) {
        Text(text)
    }
}
