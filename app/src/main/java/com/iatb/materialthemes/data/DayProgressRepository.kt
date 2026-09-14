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
    val isDst: Boolean,
    // Solar information
    val sunriseMillis: Long,
    val sunsetMillis: Long,
    val sunriseFormatted: String,
    val sunsetFormatted: String,
    val sunriseProgress: Float,
    val sunsetProgress: Float,
    val isDaylight: Boolean,
    val phaseTitle: String,
    val phaseIconResId: Int,
    val daylightDurationText: String,
    val remainingTimeText: String
)

object DayProgressRepository {

    fun getDayProgress(context: Context, nowMillis: Long = System.currentTimeMillis()): DayProgressInfo {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()

        val startOfDayMillis: Long
        val startOfNextDayMillis: Long
        val timezoneId: String
        val timezoneOffset: String
        val isDst: Boolean
        val tzOffsetMillis: Int

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
            tzOffsetMillis = zonedDateTime.offset.totalSeconds * 1000
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
            val rawOffset = tz.getOffset(nowMillis)
            val offsetHours = rawOffset / 3600000
            val offsetMins = Math.abs((rawOffset % 3600000) / 60000)
            timezoneOffset = String.format(Locale.US, "GMT%+03d:%02d", offsetHours, offsetMins)
            isDst = tz.inDaylightTime(Date(nowMillis))
            tzOffsetMillis = rawOffset
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

        // Solar Calculations (Sunrise & Sunset)
        val (lat, lon) = resolveLocationCoords(context, tzOffsetMillis)
        val (sunriseMillis, sunsetMillis) = calculateSolarTimes(
            startOfDayMillis = startOfDayMillis,
            lat = lat,
            lon = lon,
            timezoneOffsetMillis = tzOffsetMillis
        )

        val timeFormat = SimpleDateFormat("HH:mm", locale)
        val sunriseFormatted = timeFormat.format(Date(sunriseMillis))
        val sunsetFormatted = timeFormat.format(Date(sunsetMillis))

        val sunriseProgress = ((sunriseMillis - startOfDayMillis).toFloat() / totalMillisInDay.toFloat()).coerceIn(0.05f, 0.95f)
        val sunsetProgress = ((sunsetMillis - startOfDayMillis).toFloat() / totalMillisInDay.toFloat()).coerceIn(0.05f, 0.95f)

        val isDaylight = nowMillis in sunriseMillis..sunsetMillis

        // Phase determination
        val dawnWindow = 40 * 60 * 1000L
        val duskWindow = 40 * 60 * 1000L

        val phaseTitle: String
        val phaseIconResId: Int

        when {
            nowMillis in (sunriseMillis - dawnWindow)..(sunriseMillis + dawnWindow / 2) -> {
                phaseTitle = context.getString(R.string.day_phase_dawn)
                phaseIconResId = R.drawable.ic_sunrise
            }
            nowMillis in (sunsetMillis - duskWindow / 2)..(sunsetMillis + duskWindow) -> {
                phaseTitle = context.getString(R.string.day_phase_dusk)
                phaseIconResId = R.drawable.ic_sunset
            }
            isDaylight -> {
                phaseTitle = context.getString(R.string.day_phase_daylight)
                phaseIconResId = R.drawable.ic_weather_sunny
            }
            else -> {
                phaseTitle = context.getString(R.string.day_phase_night)
                phaseIconResId = R.drawable.ic_moon
            }
        }

        // Daylight Duration
        val daylightDurationMs = (sunsetMillis - sunriseMillis).coerceAtLeast(0L)
        val daylightHours = daylightDurationMs / 3600000L
        val daylightMins = (daylightDurationMs % 3600000L) / 60000L
        val daylightDurationText = context.getString(R.string.day_progress_daylight_duration, daylightHours, daylightMins)

        // Remaining Time
        val remainingMillis = (totalMillisInDay - elapsedMillis).coerceAtLeast(0L)
        val remHours = remainingMillis / 3600000L
        val remMins = (remainingMillis % 3600000L) / 60000L
        val remainingTimeText = context.getString(R.string.day_progress_remaining_format, remHours, remMins)

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
            isDst = isDst,
            sunriseMillis = sunriseMillis,
            sunsetMillis = sunsetMillis,
            sunriseFormatted = sunriseFormatted,
            sunsetFormatted = sunsetFormatted,
            sunriseProgress = sunriseProgress,
            sunsetProgress = sunsetProgress,
            isDaylight = isDaylight,
            phaseTitle = phaseTitle,
            phaseIconResId = phaseIconResId,
            daylightDurationText = daylightDurationText,
            remainingTimeText = remainingTimeText
        )
    }

    private fun resolveLocationCoords(context: Context, tzOffsetMillis: Int): Pair<Double, Double> {
        try {
            val prefs = context.getSharedPreferences("weather_cache_prefs", Context.MODE_PRIVATE)
            val latStr = prefs.getString("cached_lat", null)
            val lonStr = prefs.getString("cached_lon", null)
            if (latStr != null && lonStr != null) {
                return Pair(latStr.toDouble(), lonStr.toDouble())
            }
        } catch (_: Exception) {}

        // Fallback based on timezone / country
        val locale = Locale.getDefault()
        return if (locale.country == "VN" || TimeZone.getDefault().id.contains("Ho_Chi_Minh", ignoreCase = true)) {
            Pair(16.0544, 108.2022) // Central Vietnam (Da Nang coordinates)
        } else {
            val offsetHours = tzOffsetMillis / 3600000.0
            Pair(21.0, offsetHours * 15.0)
        }
    }

    private fun calculateSolarTimes(
        startOfDayMillis: Long,
        lat: Double,
        lon: Double,
        timezoneOffsetMillis: Int
    ): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = startOfDayMillis }
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)

        val eqtime = 229.18 * (0.000075 + 0.001868 * kotlin.math.cos(gamma) - 0.032077 * kotlin.math.sin(gamma)
                - 0.014615 * kotlin.math.cos(2 * gamma) - 0.040849 * kotlin.math.sin(2 * gamma))

        val decl = 0.006918 - 0.399912 * kotlin.math.cos(gamma) + 0.070257 * kotlin.math.sin(gamma)
                - 0.006758 * kotlin.math.cos(2 * gamma) + 0.000907 * kotlin.math.sin(2 * gamma)
                - 0.002697 * kotlin.math.cos(3 * gamma) + 0.00148 * kotlin.math.sin(3 * gamma)

        val latRad = Math.toRadians(lat)
        val zenithRad = Math.toRadians(90.833)

        var cosHa = (kotlin.math.cos(zenithRad) - kotlin.math.sin(latRad) * kotlin.math.sin(decl)) /
                (kotlin.math.cos(latRad) * kotlin.math.cos(decl))
        cosHa = cosHa.coerceIn(-1.0, 1.0)
        val haDeg = Math.toDegrees(kotlin.math.acos(cosHa))

        val solarNoonUtcMinutes = 720.0 - 4.0 * lon - eqtime
        val sunriseUtcMinutes = solarNoonUtcMinutes - 4.0 * haDeg
        val sunsetUtcMinutes = solarNoonUtcMinutes + 4.0 * haDeg

        val offsetMinutes = timezoneOffsetMillis / 60000.0
        val sunriseLocalMinutes = (sunriseUtcMinutes + offsetMinutes).coerceIn(0.0, 1439.0)
        val sunsetLocalMinutes = (sunsetUtcMinutes + offsetMinutes).coerceIn(0.0, 1439.0)

        val sunriseMillis = startOfDayMillis + (sunriseLocalMinutes * 60000.0).toLong()
        val sunsetMillis = startOfDayMillis + (sunsetLocalMinutes * 60000.0).toLong()

        return Pair(sunriseMillis, sunsetMillis)
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
