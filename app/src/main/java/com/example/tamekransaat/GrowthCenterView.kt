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
fun GrowthCenterView(preferencesManager: PreferencesManager) {
    val thisWeekMinutes = preferencesManager.getWeeklyFocusMinutes(0)
    val lastWeekMinutes = preferencesManager.getWeeklyFocusMinutes(1)

    val diff = thisWeekMinutes - lastWeekMinutes
    val isPositive = diff > 0
    val isNeutral = diff == 0

    val message = if (thisWeekMinutes == 0 && lastWeekMinutes == 0) {
        "Gelişim yolculuğun burada başlıyor. İlk adımını atmak için harika bir hafta!"
    } else if (isNeutral) {
        "Harika bir tutarlılık! Geçen haftaki temponu birebir koruyorsun, aynen böyle devam."
    } else if (isPositive) {
        "İnanılmazsın! Geçen haftaya göre ${diff} dakika daha fazla odaklandın. Sınırlarını aşıyorsun!"
    } else {
        "Dinlenmek de sürecin önemli bir parçası. Bu haftayı zihnini şarj etmeye ayırdın. Önümüzdeki hafta daha güçlü döneceksin!"
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

    val daysTr = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cts", "Paz")
    val insightText = if (maxMinutes == 0) {
        "Akıllı Analiz: Henüz yeterli veri yok. Odaklanmaya başla!"
    } else {
        "Akıllı Analiz: En yüksek verimini ${daysTr[bestDay - 1]} sabah ${String.format(Locale.getDefault(), "%02d:00", bestHour)} sularında alıyorsun. Bugün o saatte en zor işini hallet!"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = insightText,
                color = Color(0xFF4CAF50),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Kişisel Gelişim Merkezi",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Geçen Hafta", color = Color.Gray, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(120.dp).background(Color(0xFF1E1E1E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${lastWeekMinutes}dk", color = Color.LightGray, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!isNeutral && (thisWeekMinutes > 0 || lastWeekMinutes > 0)) {
                    val color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    val icon = if (isPositive) "▲" else "▼"
                    Text(
                        text = "$icon %${percentage.toInt()}",
                        color = color,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text("=", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Bu Hafta", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(140.dp).background(Color(0xFF00FF33).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${thisWeekMinutes}dk", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                color = Color.White,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
        }
    }
}
