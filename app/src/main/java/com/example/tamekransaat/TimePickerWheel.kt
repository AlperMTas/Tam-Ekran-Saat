package com.example.tamekransaat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelTimePicker(
    initialTime: String,
    onTimeSelected: (String) -> Unit
) {
    // initialTime format: "HH:mm" (e.g. "09:30")
    val initialHour = initialTime.substringBefore(":").toIntOrNull() ?: 9
    val initialMinute = initialTime.substringAfter(":").toIntOrNull() ?: 0

    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = initialHour)
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinute)

    val coroutineScope = rememberCoroutineScope()

    // 0-23 hours, 0-59 minutes
    val hours = (0..23).toList()
    val minutes = (0..59).toList()

    // Observe center items
    val currentHour by remember {
        derivedStateOf {
            val visibleItems = hourState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) initialHour
            else visibleItems[visibleItems.size / 2].index
        }
    }

    val currentMinute by remember {
        derivedStateOf {
            val visibleItems = minuteState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) initialMinute
            else visibleItems[visibleItems.size / 2].index
        }
    }

    // Seçim değiştiğinde callback'i tetikle
    LaunchedEffect(currentHour, currentMinute) {
        val formattedTime = String.format(Locale.getDefault(), "%02d:%02d", currentHour, currentMinute)
        onTimeSelected(formattedTime)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp) // Wheel yüksekliği
            .background(Color(0xFF161618)), // Modern koyu arkaplan
        contentAlignment = Alignment.Center
    ) {
        // Ortadaki Seçili Alanın Vurgusu (Koyu Gri Şerit)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(Color(0xFF222224)) // Ekteki görseldeki orta şerit
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SAAT ÇARKI
            LazyColumn(
                state = hourState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = hourState),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 55.dp) // Ortalamak için padding
            ) {
                items(hours.size) { index ->
                    val isCenter = index == currentHour
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", hours[index]),
                        color = if (isCenter) Color.White else Color.Gray,
                        fontSize = if (isCenter) 24.sp else 18.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .alpha(if (isCenter) 1f else 0.4f)
                    )
                }
            }

            // Araya Nokta
            Text(
                text = ":",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // DAKİKA ÇARKI
            LazyColumn(
                state = minuteState,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = minuteState),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 55.dp) // Ortalamak için padding
            ) {
                items(minutes.size) { index ->
                    val isCenter = index == currentMinute
                    Text(
                        text = String.format(Locale.getDefault(), "%02d", minutes[index]),
                        color = if (isCenter) Color.White else Color.Gray,
                        fontSize = if (isCenter) 24.sp else 18.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .alpha(if (isCenter) 1f else 0.4f)
                    )
                }
            }
        }
    }
}