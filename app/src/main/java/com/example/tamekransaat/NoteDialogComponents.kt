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


class MutableNote(
    initialText: String = "",
    initialColor: Long = 0xFFE91E63,
    initialTime: String = "09:00",
    initialEndTime: String = "", // Yeni: Bitiş zamanı
    initialType: NoteType = NoteType.NOTE,
    initialRecurring: Boolean = false
) {
    var text by mutableStateOf(initialText)
    var color by mutableLongStateOf(initialColor)
    var timeStr by mutableStateOf(initialTime)
    var endTimeStr by mutableStateOf(initialEndTime)
    var type by mutableStateOf(initialType)
    var isRecurring by mutableStateOf(initialRecurring)
    
    fun toDayNote() = DayNote(text, color, timeStr, type, isRecurring, endTimeStr)
}

@Composable
fun NoteEditDialog(
    date: LocalDate,
    preferencesManager: PreferencesManager,
    initialTimeStr: String? = null, // Dışarıdan istenen bir başlama saati varsa al (Örn: Tıklanan kutucuğun saati)
    onDismiss: () -> Unit
) {
    val dateStr = date.toString()
    val existingNotes = preferencesManager.getNotesForDate(dateStr)
    val months = (1..12).map { java.time.Month.of(it).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()) }
    
    val colors = listOf(
        0xFFFF6B6B, 
        0xFF4ECDC4, 
        0xFFFFD93D, 
        0xFF95E1D3, 
        0xFFA28089  
    )
    
    val mutableNotes = remember {
        mutableStateListOf<MutableNote>().apply {
            if (existingNotes.isEmpty()) {
                add(MutableNote(initialColor = colors[0], initialTime = initialTimeStr ?: "09:00"))
            } else {
                existingNotes.forEach {
                    add(MutableNote(it.text, it.color, it.timeStr, it.endTimeStr, it.type, it.isRecurring))
                }
            }
        }
    }

    val focusManager = LocalFocusManager.current
    var isAnyTextFieldFocused by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { 
            if (isAnyTextFieldFocused) {
                focusManager.clearFocus()
            } else {
                onDismiss() 
            }
        },
        containerColor = Color(0xFF161618),
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
        shape = RoundedCornerShape(24.dp),
        title = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${date.dayOfMonth} ${months[date.monthValue - 1]}", 
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.plan_of_the_day), 
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { 
                            focusManager.clearFocus()
                        })
                    }
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    mutableNotes.forEachIndexed { index, mNote ->
                        ModernNoteSlot(
                            label = stringResource(R.string.note_label_format, index + 1),
                            text = mNote.text,
                            onTextChanged = { mNote.text = it },
                            timeStr = mNote.timeStr,
                            onTimeChanged = { mNote.timeStr = it },
                            endTimeStr = mNote.endTimeStr,
                            onEndTimeChanged = { mNote.endTimeStr = it },
                            selectedColor = mNote.color,
                            onColorChanged = { mNote.color = it },
                            selectedType = mNote.type,
                            onTypeChanged = { mNote.type = it },
                            isRecurring = mNote.isRecurring,
                            onRecurringChanged = { mNote.isRecurring = it },
                            colors = colors,
                            onFocusChanged = { isAnyTextFieldFocused = it },
                            onDelete = { mutableNotes.removeAt(index) }
                        )
                    }
                    
                    // Yeni Not Ekleme Butonu
                    TextButton(
                        onClick = { 
                            mutableNotes.add(MutableNote(initialColor = colors[mutableNotes.size % colors.size])) 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.new_plan_add), color = Color(0xFF4ECDC4), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            val context = LocalContext.current
            Button(
                onClick = { 
                    val newNotes = mutableNotes
                        .filter { it.text.isNotBlank() }
                        .map { it.toDayNote() }
                    
                    preferencesManager.saveNotesForDate(dateStr, newNotes)
                    updateTaskWidget(context)
                    onDismiss() 
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(stringResource(R.string.save_plan), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            val context = LocalContext.current
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { onDismiss() }) {
                    Text(stringResource(R.string.cancel_it), color = Color.Gray)
                }
                TextButton(
                    onClick = { 
                        preferencesManager.saveNotesForDate(dateStr, emptyList())
                        updateTaskWidget(context)
                        onDismiss() 
                    }
                ) {
                    Text(stringResource(R.string.clear_all), color = Color(0xFFFF6B6B))
                }
            }
        }
    )
}

@Composable
fun ModernNoteSlot(
    label: String,
    text: String,
    onTextChanged: (String) -> Unit,
    timeStr: String,
    onTimeChanged: (String) -> Unit,
    endTimeStr: String,
    onEndTimeChanged: (String) -> Unit,
    selectedColor: Long,
    onColorChanged: (Long) -> Unit,
    selectedType: NoteType,
    onTypeChanged: (NoteType) -> Unit,
    isRecurring: Boolean,
    onRecurringChanged: (Boolean) -> Unit,
    colors: List<Long>,
    onFocusChanged: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF222224), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // TİP SEÇİCİ (Görev / Etkinlik / Not) ve SİL BUTONU
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f)) {
                NoteType.values().forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .background(
                                color = if (isSelected) Color(0xFF4CAF50) else Color(0xFF333333),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onTypeChanged(type) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.getDisplayName(LocalContext.current),
                            color = if (isSelected) Color.Black else Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Silme (Çarpı) Butonu
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFF6B6B).copy(alpha = 0.2f), CircleShape)
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = Color(0xFFFF6B6B), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SAAT SEÇİMİ BÖLÜMÜ (Başlangıç ve Bitiş)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChanged,
                placeholder = { Text(label, color = Color.Gray, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF1A1A1C), RoundedCornerShape(12.dp))
                    .onFocusChanged { focusState ->
                        onFocusChanged(focusState.isFocused)
                    }
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Saatleri tutacak sağdaki grup
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Başlangıç Saati
                var expandedStart by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = timeStr,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledContainerColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .width(65.dp)
                            .background(Color(0xFF1A1A1C), RoundedCornerShape(12.dp))
                            .clickable { expandedStart = true }
                    )

                    if (expandedStart) {
                        AlertDialog(
                            onDismissRequest = { expandedStart = false },
                            containerColor = Color(0xFF161618),
                            title = { Text(stringResource(R.string.start_time), color = Color.White) },
                            text = {
                                WheelTimePicker(
                                    initialTime = timeStr,
                                    onTimeSelected = { onTimeChanged(it) }
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = { expandedStart = false }) {
                                    Text(stringResource(R.string.ok), color = Color(0xFF4CAF50))
                                }
                            }
                        )
                    }
                }
                
                Text(" - ", color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp))
                
                // Bitiş Saati (Opsiyonel)
                var expandedEnd by remember { mutableStateOf(false) }
                Box {
                    OutlinedTextField(
                        value = endTimeStr,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        placeholder = { Text(stringResource(R.string.end), color = Color.DarkGray, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            disabledTextColor = Color.White,
                            disabledContainerColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                        ),
                        singleLine = true,
                        modifier = Modifier
                            .width(65.dp)
                            .background(Color(0xFF1A1A1C), RoundedCornerShape(12.dp))
                            .clickable { expandedEnd = true }
                    )

                    if (expandedEnd) {
                        AlertDialog(
                            onDismissRequest = { expandedEnd = false },
                            containerColor = Color(0xFF161618),
                            title = { Text(stringResource(R.string.end_time_optional), color = Color.White) },
                            text = {
                                WheelTimePicker(
                                    initialTime = if(endTimeStr.isEmpty()) timeStr else endTimeStr,
                                    onTimeSelected = { onEndTimeChanged(it) }
                                )
                            },
                            dismissButton = {
                                TextButton(onClick = { onEndTimeChanged(""); expandedEnd = false }) {
                                    Text(stringResource(R.string.clear), color = Color(0xFFFF6B6B))
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { expandedEnd = false }) {
                                    Text(stringResource(R.string.ok), color = Color(0xFF4CAF50))
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                colors.forEach { colorVal ->
                    val isSelected = selectedColor == colorVal
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(28.dp)
                            .background(Color(colorVal), CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 0.dp, 
                                color = if (isSelected) Color.White else Color.Transparent, 
                                shape = CircleShape
                            )
                            .clickable { onColorChanged(colorVal) }
                    )
                }
            }

            // Tekrar Et (Recurring) Tuşu
            Box(
                modifier = Modifier
                    .background(
                        if (isRecurring) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Transparent, 
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.dp, 
                        if (isRecurring) Color(0xFF4CAF50) else Color(0xFF333333), 
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onRecurringChanged(!isRecurring) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.recurring_weekly),
                    color = if (isRecurring) Color(0xFF4CAF50) else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
