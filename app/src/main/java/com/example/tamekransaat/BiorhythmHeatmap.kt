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
fun BiorhythmHeatmap(preferencesManager: PreferencesManager) {
    val days = (1..7).map { java.time.DayOfWeek.of(it).getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.weekly_biorhythm),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Üst sağ köşe bilgilendirme (ampul) butonu
            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(
                    text = "💡", 
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 12.dp, top = 24.dp)
            ) {
                days.forEach { dayName ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            text = dayName,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                    for (hour in 0..23) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (hour % 4 == 0) {
                                Text(
                                    text = hour.toString(),
                                    color = Color.DarkGray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    for (day in 1..7) {
                        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            for (hour in 0..23) {
                                val mins = preferencesManager.getHeatmapMinute(day, hour)
                                val fraction = (mins / 60f).coerceIn(0f, 1f)

                                val boxColor = if (mins == 0) {
                                    Color.White.copy(alpha = 0.05f)
                                } else {
                                    androidx.compose.ui.graphics.lerp(
                                        start = Color(0xFFFF9800),
                                        stop = Color(0xFF00FF33),
                                        fraction = fraction
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(boxColor, RoundedCornerShape(4.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Bilgilendirme Diyaloğu
        if (showInfoDialog) {
            AlertDialog(
                onDismissRequest = { showInfoDialog = false },
                containerColor = Color(0xFF161618),
                shape = RoundedCornerShape(24.dp),
                title = { 
                    Text(
                        text = stringResource(R.string.biorhythm_info_title), 
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                text = { 
                    Text(
                        text = stringResource(R.string.biorhythm_info_desc), 
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    ) 
                },
                confirmButton = {
                    Button(
                        onClick = { showInfoDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text(stringResource(R.string.got_it), color = Color.White)
                    }
                }
            )
        }
    }
}
