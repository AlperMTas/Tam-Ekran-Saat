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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import androidx.compose.ui.res.stringResource

@Composable
fun GrowthCenterView(preferencesManager: PreferencesManager) {
    // Performans iyileştirmesi: 180 döngü sadece sayfa açıldığında bir kez hesaplanır
    val stats = remember {
        val tw = preferencesManager.getWeeklyFocusMinutes(0)
        val lw = preferencesManager.getWeeklyFocusMinutes(1)
        var maxM = 0
        var bDay = 1
        var bHour = 9

        for (d in 1..7) {
            for (h in 0..23) {
                val mins = preferencesManager.getHeatmapMinute(d, h)
                if (mins > maxM) {
                    maxM = mins
                    bDay = d
                    bHour = h
                }
            }
        }
        listOf(tw, lw, maxM, bDay, bHour)
    }

    val thisWeekMinutes = stats[0]
    val lastWeekMinutes = stats[1]
    val maxMinutes = stats[2]
    val bestDay = stats[3]
    val bestHour = stats[4]

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

    val insightText = if (maxMinutes == 0) {
        stringResource(R.string.smart_analysis_no_data)
    } else {
        val bestDayName = java.time.DayOfWeek.of(bestDay).getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault())
        val formattedHour = String.format(Locale.getDefault(), "%02d:00", bestHour)
        stringResource(R.string.smart_analysis_data, bestDayName, formattedHour)
    }

    // ESKİ DİZİLİM, YENİ GÖRSELLER (Yuvarlak Çemberler Geri Döndü!)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ÜST BİLGİ: Yapay Zeka Analizi (Eski üst kutunun daha şık hali)
        Card(
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161916)), // Şık koyu yeşil arka plan
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✨", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = insightText,
                    color = Color(0xFFE8F5E9),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // BAŞLIK
        Text(
            text = "📈 " + stringResource(R.string.personal_growth_center),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(28.dp))

        // ESKİ İKONİK ÇEMBERLİ İSTATİSTİKLER (Tamamen orjinal yapısına sadık kalarak)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Geçen Hafta
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.last_week), color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Color(0xFF1E1E1E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.min_format, lastWeekMinutes), color = Color.LightGray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Trend
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                            fontSize = 20.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    Text("=", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Bu Hafta
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.this_week), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(Color(0xFF00FF33).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.min_format, thisWeekMinutes), color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // ALT BİLGİ: Motivasyon Mesajı
        Card(
            modifier = Modifier.fillMaxWidth(0.85f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16161A)), // Şık koyu lacivert
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
