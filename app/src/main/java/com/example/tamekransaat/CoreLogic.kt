package com.example.tamekransaat

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random

// Sistemin 3 ana deterministik durumu
enum class AppState { AMBIENT, FOCUS, CALENDAR }
enum class CalendarTab { GROWTH, BIORHYTHM, WEEKLY_SCHEDULE, MONTHLY }
enum class NoteType(val displayName: String) { 
    NOTE("Not"), 
    TASK("Görev"), 
    EVENT("Etkinlik");
    
    fun getDisplayName(context: Context): String {
        return when (this) {
            NOTE -> context.getString(R.string.type_note)
            TASK -> context.getString(R.string.type_task)
            EVENT -> context.getString(R.string.type_event)
        }
    }
}

data class DayNote(
    val text: String, 
    val color: Long, 
    val timeStr: String = "09:00", 
    val type: NoteType = NoteType.NOTE,
    val isRecurring: Boolean = false,
    val endTimeStr: String = "" // Yeni: Bitiş zamanı aralığı için (Opsiyonel)
)

// SharedPreferences tabanlı Local Storage
class PreferencesManager(context: Context) {
    private val prefs = context.getSharedPreferences("tamekransaat_prefs", Context.MODE_PRIVATE)
    
    fun getFocusDuration(): Int = prefs.getInt("focus_duration", 25)
    fun saveFocusDuration(duration: Int) = prefs.edit().putInt("focus_duration", duration).apply()
    
    var focusEndTime: Long
        get() = prefs.getLong("focus_end_time", 0L)
        set(value) = prefs.edit().putLong("focus_end_time", value).apply()

    var isFocusRunning: Boolean
        get() = prefs.getBoolean("is_focus_running", false)
        set(value) = prefs.edit().putBoolean("is_focus_running", value).apply()

    var focusCompletedInBackground: Boolean
        get() = prefs.getBoolean("focus_completed_in_background", false)
        set(value) = prefs.edit().putBoolean("focus_completed_in_background", value).apply()

    var focusOriginalDuration: Int // minutes
        get() = prefs.getInt("focus_original_duration", 25)
        set(value) = prefs.edit().putInt("focus_original_duration", value).apply()

    var dontAskDndAgain: Boolean
        get() = prefs.getBoolean("dont_ask_dnd_again", false)
        set(value) = prefs.edit().putBoolean("dont_ask_dnd_again", value).apply()
        
    var hasSeenCalendarTutorial: Boolean
        get() = prefs.getBoolean("has_seen_calendar_tutorial", false)
        set(value) = prefs.edit().putBoolean("has_seen_calendar_tutorial", value).apply()
    
    // Gerçek Odaklanma Sürelerini Gün Bazında Kaydetmek İçin
    fun addFocusMinute(dateStr: String) {
        val current = prefs.getInt("focus_date_$dateStr", 0)
        prefs.edit().putInt("focus_date_$dateStr", current + 1).apply()
    }

    fun getFocusMinutes(dateStr: String): Int = prefs.getInt("focus_date_$dateStr", 0)

    // Belirli bir haftanın (0: Bu hafta, 1: Geçen hafta vs.) toplam odak süresini hesaplar
    fun getWeeklyFocusMinutes(weeksAgo: Int): Int {
        val today = LocalDate.now()
        val startOfTargetWeek = today.minusWeeks(weeksAgo.toLong()).with(DayOfWeek.MONDAY)
        var total = 0
        for (i in 0..6) {
            val date = startOfTargetWeek.plusDays(i.toLong())
            total += getFocusMinutes(date.toString())
        }
        return total
    }

    // Odak Isı Haritası (Bioritim) için Saate Göre Veri Kaydı
    fun addFocusMinuteToHeatmap(dayOfWeek: Int, hourOfDay: Int) {
        val key = "heatmap_${dayOfWeek}_${hourOfDay}"
        val current = prefs.getInt(key, 0)
        prefs.edit().putInt(key, current + 1).apply()
    }

    fun getHeatmapMinute(dayOfWeek: Int, hourOfDay: Int): Int {
        return prefs.getInt("heatmap_${dayOfWeek}_${hourOfDay}", 0)
    }

    // Takvim notları için fonksiyonlar (Dinamik sınır)
    fun getNotesForDate(dateStr: String): List<DayNote> {
        val list = mutableListOf<DayNote>()
        val date = try { LocalDate.parse(dateStr) } catch(e: Exception) { LocalDate.now() }
        val dayOfWeek = date.dayOfWeek.value

        // Kayıtlı not sayısını al (eski sürüm uyumluluğu için varsayılan 3)
        val count = prefs.getInt("note_count_${dateStr}", 3)
        val maxChecks = maxOf(count, 3)

        for (i in 0 until maxChecks) {
            val text = prefs.getString("note_${dateStr}_$i", null)
            if (!text.isNullOrEmpty()) {
                val color = prefs.getLong("note_color_${dateStr}_$i", 0xFFE91E63)
                val timeStr = prefs.getString("note_time_${dateStr}_$i", "09:00") ?: "09:00"
                val endTimeStr = prefs.getString("note_end_time_${dateStr}_$i", "") ?: ""
                val typeStr = prefs.getString("note_type_${dateStr}_$i", "NOTE") ?: "NOTE"
                val type = try { NoteType.valueOf(typeStr) } catch(e: Exception) { NoteType.NOTE }
                list.add(DayNote(text, color, timeStr, type, false, endTimeStr))
            } else {
                // O güne özel not yoksa, bu haftanın o gününe (örn: Cuma) ait tekrar eden not var mı diye bak
                val rText = prefs.getString("recurring_note_${dayOfWeek}_$i", null)
                if (!rText.isNullOrEmpty()) {
                    val rColor = prefs.getLong("recurring_note_color_${dayOfWeek}_$i", 0xFFE91E63)
                    val rTimeStr = prefs.getString("recurring_note_time_${dayOfWeek}_$i", "09:00") ?: "09:00"
                    val rEndTimeStr = prefs.getString("recurring_note_end_time_${dayOfWeek}_$i", "") ?: ""
                    val rTypeStr = prefs.getString("recurring_note_type_${dayOfWeek}_$i", "NOTE") ?: "NOTE"
                    val rType = try { NoteType.valueOf(rTypeStr) } catch(e: Exception) { NoteType.NOTE }
                    list.add(DayNote(rText, rColor, rTimeStr, rType, true, rEndTimeStr))
                }
            }
        }
        
        return list
    }

    fun saveNotesForDate(dateStr: String, notes: List<DayNote>) {
        val date = try { LocalDate.parse(dateStr) } catch(e: Exception) { LocalDate.now() }
        val dayOfWeek = date.dayOfWeek.value

        val editor = prefs.edit()
        
        // Öncekileri temizlemek için mevcut limiti al (silinecek eski kalıntı kalmasın)
        val oldCount = prefs.getInt("note_count_${dateStr}", 3)
        val limit = maxOf(oldCount, 10) // Ne olur ne olmaz 10'a kadar tara ve temizle

        // Önce o güne ait tüm eski kayıtları (ve o günün tekrar edenlerini) temizle ki üstüne yazılabilsin
        for (i in 0 until limit) {
            editor.remove("note_${dateStr}_$i")
                  .remove("note_color_${dateStr}_$i")
                  .remove("note_time_${dateStr}_$i")
                  .remove("note_end_time_${dateStr}_$i")
                  .remove("note_type_${dateStr}_$i")
                  .remove("recurring_note_${dayOfWeek}_$i")
                  .remove("recurring_note_color_${dayOfWeek}_$i")
                  .remove("recurring_note_time_${dayOfWeek}_$i")
                  .remove("recurring_note_end_time_${dayOfWeek}_$i")
                  .remove("recurring_note_type_${dayOfWeek}_$i")
        }
        
        // Kaç tane not kaydettiğimizi tutalım ki okurken bilelim
        editor.putInt("note_count_${dateStr}", notes.size)
        
        // Yeni notları kaydet
        notes.forEachIndexed { index, dayNote ->
            if (dayNote.isRecurring) {
                // Eğer tekrar ediyorsa, günün tarihine değil, haftanın gününe (DayOfWeek) kaydet
                editor.putString("recurring_note_${dayOfWeek}_$index", dayNote.text)
                      .putLong("recurring_note_color_${dayOfWeek}_$index", dayNote.color)
                      .putString("recurring_note_time_${dayOfWeek}_$index", dayNote.timeStr)
                      .putString("recurring_note_end_time_${dayOfWeek}_$index", dayNote.endTimeStr)
                      .putString("recurring_note_type_${dayOfWeek}_$index", dayNote.type.name)
            } else {
                // Sadece o güne özgüyse, direkt spesifik tarihe kaydet
                editor.putString("note_${dateStr}_$index", dayNote.text)
                      .putLong("note_color_${dateStr}_$index", dayNote.color)
                      .putString("note_time_${dateStr}_$index", dayNote.timeStr)
                      .putString("note_end_time_${dateStr}_$index", dayNote.endTimeStr)
                      .putString("note_type_${dateStr}_$index", dayNote.type.name)
            }
        }
        
        // Asenkron yerine anında ve garantili senkron kaydetmesi için commit kullanıyoruz
        editor.commit()
    }
}

// OLED Burn-In (Ekran Yanması) Koruması: Ekranı her 3 dakikada bir rastgele 1-3 piksel kaydırır
@Composable
fun rememberBurnInOffset(): State<IntOffset> {
    val offset = remember { mutableStateOf(IntOffset.Zero) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3 * 60 * 1000L) // 3 dakika bekle
            // X ve Y eksenlerinde -3 ile +3 piksel arası kaydırma
            offset.value = IntOffset(Random.nextInt(-3, 3), Random.nextInt(-3, 3))
        }
    }
    return offset
}