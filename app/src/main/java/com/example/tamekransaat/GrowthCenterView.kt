package com.example.tamekransaat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.ui.res.stringResource

@Composable
fun GrowthCenterView(preferencesManager: PreferencesManager) {
    val thisWeekMinutes = preferencesManager.getWeeklyFocusMinutes(0)
    val lastWeekMinutes = preferencesManager.getWeeklyFocusMinutes(1)

    val diff = thisWeekMinutes - lastWeekMinutes
    val isPositive = diff > 0
    val isNeutral = diff == 0

    val message = if (thisWeekMinutes == 0 && lastWeekMinutes == 0) {
        stringResource(R.string.growth_start)
    } else if (isNeutral) {
        stringResource(R.string.growth_neutral)
    } else if (isPositive) {
        stringResource(R.string.growth_positive, diff)
    } else {
        stringResource(R.string.growth_negative)
    }

    val percentage = if (lastWeekMinutes > 0) {
        (Math.abs(diff).toFloat() / lastWeekMinutes.toFloat()) * 100f
    } else {
        100f
    }

    var maxMinutes = 0
    var bestDay = 1
    var bestHour = 9

    for (d in 1..7) {
        for (h in 0..23) {
            val mins = preferencesManager.getHeatmapMinute(d, h)
            if (mins > maxMinutes) {
                maxMinutes = mins
                bestDay = d
                bestHour = h
            }
        }
    }

    val insightText = if (maxMinutes == 0) {
        stringResource(R.string.smart_analysis_no_data)
    } else {
        val bestDayName = java.time.DayOfWeek.of(bestDay).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
        val formattedHour = String.format(Locale.getDefault(), "%02d:00", bestHour)
        stringResource(R.string.smart_analysis_data, bestDayName, formattedHour)
    }

    // YENİ MODERN ARAYÜZ TASARIMI (İki Panelli Dashboard)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SOL PANEL: Performans Özeti (Dashboard Card)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Başlık
                Text(
                    text = "📈 " + stringResource(R.string.personal_growth_center),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                // Ana Metrik (Bu Hafta)
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = stringResource(R.string.this_week), 
                        color = Color.Gray, 
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.min_format, thisWeekMinutes), 
                        color = Color.White, 
                        fontSize = 56.sp, 
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Alt Kısım (Geçen Hafta ve Trend)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.last_week), 
                            color = Color.DarkGray, 
                            fontSize = 14.sp
                        )
                        Text(
                            text = stringResource(R.string.min_format, lastWeekMinutes), 
                            color = Color.LightGray, 
                            fontSize = 20.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Trend Rozeti (Artış / Azalış)
                    if (!isNeutral && (thisWeekMinutes > 0 || lastWeekMinutes > 0)) {
                        val trendColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        val icon = if (isPositive) "▲" else "▼"
                        Surface(
                            color = trendColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "$icon %${percentage.toInt()}",
                                color = trendColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = Color.Gray.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "=",
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // SAĞ PANEL: Yapay Zeka Analizi ve Motivasyon
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Akıllı Analiz (AI Insight) Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B231B)), // Hafif yeşilimsi siyah
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✨", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = insightText,
                        color = Color(0xFFE8F5E9),
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Motivasyon Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)), // Hafif mavimsi siyah
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF2196F3).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("💡", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 16.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}
