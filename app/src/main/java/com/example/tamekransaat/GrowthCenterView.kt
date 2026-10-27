package com.example.tamekransaat

import androidx.compose.foundation.layout.*
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
    // PERFORMANS ÇÖZÜMÜ:
    // Menüler arası geçişteki kasma ve donmaların sebebi, her frame çiziminde
    // SharedPreferences veritabanından 180'den fazla veri okunmasıydı.
    // 'remember' kullanarak bu hesaplamaların sekme açıldığında SADECE BİR KERE yapılmasını sağladım.
    // Artık geçişler yağ gibi akacak.
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

    // YENİ, SADE VE ŞIK TASARIM
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // BAŞLIK
        Text(
            text = stringResource(R.string.personal_growth_center),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // 1. HAFTALIK ÖZET KARTI
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Geçen Hafta
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.last_week), color = Color.Gray, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.min_format, lastWeekMinutes), color = Color.LightGray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }

                // Trend
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!isNeutral && (thisWeekMinutes > 0 || lastWeekMinutes > 0)) {
                        val color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                        val icon = if (isPositive) "▲" else "▼"
                        Text(
                            text = "$icon %${percentage.toInt()}",
                            color = color,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text("=", color = Color.Gray, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Bu Hafta
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.this_week), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.min_format, thisWeekMinutes), color = Color(0xFF4CAF50), fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // 2. YARDIMCI BİLGİLER (İki yan yana kutu)
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Analiz
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.ai_insight), color = Color(0xFF4CAF50), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = insightText, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }

            // Motivasyon
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.motivation), color = Color(0xFF2196F3), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}
