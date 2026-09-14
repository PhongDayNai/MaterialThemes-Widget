package com.iatb.materialthemes.data

import android.content.Context
import android.os.Build
import com.iatb.materialthemes.R
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class DayProgressInfo(
    val percentage: Int,
    val progress: Float,
    val dayOfWeek: String,
    val dateFormatted: String,
    val fullDateString: String,
    val timeFormatted: String,
    val startOfDayMillis: Long,
    val startOfNextDayMillis: Long,
    val elapsedMillis: Long,
    val totalMillisInDay: Long,
    val timezoneId: String,
    val timezoneOffset: String,
    val isDst: Boolean
)

object DayProgressRepository {

    fun getDayProgress(context: Context, nowMillis: Long = System.currentTimeMillis()): DayProgressInfo {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()

        val startOfDayMillis: Long
        val startOfNextDayMillis: Long
        val timezoneId: String
        val timezoneOffset: String
        val isDst: Boolean

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val zoneId = ZoneId.systemDefault()
            val zonedDateTime = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zoneId)
            val localDate = zonedDateTime.toLocalDate()
            val startOfDay = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val startOfNextDay = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

            startOfDayMillis = startOfDay
            startOfNextDayMillis = startOfNextDay
            timezoneId = zoneId.id
            timezoneOffset = zonedDateTime.offset.id
            isDst = zoneId.rules.isDaylightSavings(zonedDateTime.toInstant())
        } else {
            val tz = TimeZone.getDefault()
            val calendar = Calendar.getInstance(tz).apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            startOfDayMillis = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            startOfNextDayMillis = calendar.timeInMillis

            timezoneId = tz.id
            val offsetHours = tz.getOffset(nowMillis) / 3600000
            val offsetMins = Math.abs((tz.getOffset(nowMillis) % 3600000) / 60000)
            timezoneOffset = String.format(Locale.US, "GMT%+03d:%02d", offsetHours, offsetMins)
            isDst = tz.inDaylightTime(Date(nowMillis))
        }

        val totalMillisInDay = (startOfNextDayMillis - startOfDayMillis).coerceAtLeast(1L)
        val elapsedMillis = (nowMillis - startOfDayMillis).coerceIn(0L, totalMillisInDay)
        val progress = (elapsedMillis.toDouble() / totalMillisInDay.toDouble()).coerceIn(0.0, 1.0).toFloat()
        val percentage = (progress * 100f).roundToInt().coerceIn(0, 100)

        // Date and time formatting
        val date = Date(nowMillis)
        val dayOfWeekFormat = SimpleDateFormat("EEEE", locale)
        val dayOfWeek = dayOfWeekFormat.format(date).replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

        val shortFormatPattern = try {
            context.getString(R.string.format_day_progress_date_short)
        } catch (_: Exception) {
            if (locale.language == "vi") "d 'thg' M" else "MMM d"
        }
        val dateFormatted = try {
            SimpleDateFormat(shortFormatPattern, locale).format(date)
        } catch (_: Exception) {
            if (locale.language == "vi") {
                val cal = Calendar.getInstance(locale).apply { time = date }
                "${cal.get(Calendar.DAY_OF_MONTH)} thg ${cal.get(Calendar.MONTH) + 1}"
            } else {
                SimpleDateFormat("MMM d", locale).format(date)
            }
        }
        val fullDateString = try {
            context.getString(R.string.format_day_progress_date_full, dayOfWeek, dateFormatted)
        } catch (_: Exception) {
            "$dayOfWeek, $dateFormatted"
        }
        val timeFormatted = SimpleDateFormat("HH:mm", locale).format(date)

        return DayProgressInfo(
            percentage = percentage,
            progress = progress,
            dayOfWeek = dayOfWeek,
            dateFormatted = dateFormatted,
            fullDateString = fullDateString,
            timeFormatted = timeFormatted,
            startOfDayMillis = startOfDayMillis,
            startOfNextDayMillis = startOfNextDayMillis,
            elapsedMillis = elapsedMillis,
            totalMillisInDay = totalMillisInDay,
            timezoneId = timezoneId,
            timezoneOffset = timezoneOffset,
            isDst = isDst
        )
    }

    fun formatElapsedSummary(context: Context, info: DayProgressInfo): String {
        val elapsedHours = info.elapsedMillis / 3600000L
        val elapsedMins = (info.elapsedMillis % 3600000L) / 60000L
        val totalHours = info.totalMillisInDay / 3600000L
        return context.getString(
            R.string.day_progress_summary_format,
            elapsedHours,
            elapsedMins,
            totalHours
        )
    }
}
