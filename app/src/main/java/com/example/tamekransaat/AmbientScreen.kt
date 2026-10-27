package com.example.tamekransaat

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientScreen(
    onStartFocus: () -> Unit
) {
    // Saniye bilgisini DOM'dan çıkararak gereksiz re-render'ı engelliyoruz
    var time by remember { mutableStateOf(LocalTime.now()) }
    val clockFontFamily = FontFamily(Font(R.font.arcens_display))
    val formatter = DateTimeFormatter.ofPattern("HH:mm") // Saniyeleri kaldırıp sadece Saat:Dakika bırakıyoruz

    // Yalnızca dakika değiştiğinde state güncellenir (Dikkat dağınıklığını azaltıp performansı artırmak için)
    LaunchedEffect(Unit) {
        while (true) {
            val now = LocalTime.now()
            if (now.minute != time.minute) {
                time = now
            }
            delay(1000) // Her saniye kontrol et, ama yalnızca dakika atladığında state tetiklenir
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black), // Saf Siyah (#000000) OLED dostu arka plan
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            
            // Ekran genişliğine (DP cinsinden) göre dinamik boyut hesaplama
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp
            
            // Oransal büyüklükler (Ekranın genişliğine göre belirleniyor)
            // Örnek: Telefonlarda (genişlik ~700-900dp arası yan modda), Tabletlerde (genişlik 1000-1300dp arası)
            val baseScale = screenWidthDp / 800f 
            
            val digitFontSize = (160 * baseScale).coerceIn(120f, 300f).sp
            val colonFontSize = (130 * baseScale).coerceIn(90f, 250f).sp
            val cardWidth = (110 * baseScale).coerceIn(80f, 220f).dp
            
            // Animasyonlu Flip Clock Tasarımı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val timeString = time.format(formatter)
                for (char in timeString) {
                    if (char == ':') {
                        Text(
                            text = ":",
                            color = Color.White,
                            fontSize = colonFontSize, 
                            fontFamily = clockFontFamily,
                            modifier = Modifier.padding(horizontal = (8 * baseScale).dp).padding(bottom = (16 * baseScale).dp)
                        )
                    } else {
                        FlipDigitCard(
                            digit = char, 
                            fontFamily = clockFontFamily,
                            fontSize = digitFontSize,
                            cardWidth = cardWidth
                        )
                    }
                }
            }
        }

        // Alt barda sabit ve dikkat dağıtmayan Odak Modu başlatıcısı
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .background(Color(0xFF0F0F0F), shape = MaterialTheme.shapes.extraLarge)
                .clickable { onStartFocus() }
                .padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.start_focus_mode), 
                color = Color(0xFF555555), 
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun FlipDigitCard(digit: Char, fontFamily: FontFamily, fontSize: TextUnit, cardWidth: Dp) {
    // Rakamların arka planını bir kart gibi gösteren Box
    Box(
        modifier = Modifier
            .padding(horizontal = cardWidth / 18) // Orantılı padding
            .width(cardWidth) // Dinamik Genişlik
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E1E)) // Koyu gri flip kart rengi
            .padding(vertical = cardWidth / 12),
        contentAlignment = Alignment.Center
    ) {
        // Compose'un animasyonlu içerik değiştiricisi (AnimatedContent)
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                // Rakam değişirken eski rakam aşağı kayar/kapanır, yeni rakam yukarıdan gelir
                (slideInVertically { height -> -height } + fadeIn()) togetherWith 
                (slideOutVertically { height -> height } + fadeOut())
            },
            label = "FlipAnimation"
        ) { char ->
            Text(
                text = char.toString(),
                color = Color.White,
                fontSize = fontSize, // Dinamik Font
                fontFamily = fontFamily,
                textAlign = TextAlign.Center
            )
        }

        // Flip Clock (yaprak saat) görünümü için rakamın tam ortasından geçen siyah çizgi
        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp) // Çizgi kalınlığı biraz artırıldı
                    .align(Alignment.Center)
                    .background(Color.Black)
            )
        }
    }
}