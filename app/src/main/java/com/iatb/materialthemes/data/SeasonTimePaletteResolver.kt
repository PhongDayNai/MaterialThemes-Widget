package com.iatb.materialthemes.data

import android.content.Context
import android.graphics.Color
import java.util.Calendar

enum class Season {
    SPRING, // March, April, May (Tháng 3, 4, 5)
    SUMMER, // June, July, August (Tháng 6, 7, 8)
    AUTUMN, // September, October, November (Tháng 9, 10, 11)
    WINTER  // December, January, February (Tháng 12, 1, 2)
}

enum class DaySolarTimePhase {
    DAWN,      // Rạng sáng / Hừng đông (Sunrise - 45m .. Sunrise + 30m)
    DAYLIGHT,  // Ban ngày nắng đẹp (Sunrise + 30m .. Sunset - 45m)
    SUNSET,    // Hoàng hôn / Chiều tà (Sunset - 45m .. Sunset + 45m)
    EVENING,   // Tối / Đầu đêm (Sunset + 45m .. 22:30)
    MIDNIGHT   // Đêm khuya tĩnh mịch (22:30 .. Dawn)
}

data class SeasonSolarPalette(
    val season: Season,
    val phase: DaySolarTimePhase,
    val cardBgColor: Int,
    val pillBgColor: Int,
    val chipBgColor: Int,
    val progressStartColor: Int,
    val progressEndColor: Int,
    val accentColor: Int,
    val textColor: Int = 0xFFFFFFFF.toInt(),
    val subTextColor: Int = 0xB3FFFFFF.toInt(),
    val strokeColor: Int = 0x2EFFFFFF.toInt()
)

object SeasonTimePaletteResolver {

    fun getSeason(calendar: Calendar = Calendar.getInstance()): Season {
        return when (calendar.get(Calendar.MONTH)) {
            Calendar.MARCH, Calendar.APRIL, Calendar.MAY -> Season.SPRING
            Calendar.JUNE, Calendar.JULY, Calendar.AUGUST -> Season.SUMMER
            Calendar.SEPTEMBER, Calendar.OCTOBER, Calendar.NOVEMBER -> Season.AUTUMN
            else -> Season.WINTER
        }
    }

    fun getSolarPhase(
        nowMillis: Long = System.currentTimeMillis(),
        sunriseMillis: Long,
        sunsetMillis: Long
    ): DaySolarTimePhase {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val minuteOfDay = hour * 60 + minute

        // If sunrise/sunset are available
        if (sunriseMillis > 0 && sunsetMillis > sunriseMillis) {
            val dawnStart = sunriseMillis - 45 * 60 * 1000L
            val dawnEnd = sunriseMillis + 35 * 60 * 1000L
            val sunsetStart = sunsetMillis - 45 * 60 * 1000L
            val sunsetEnd = sunsetMillis + 45 * 60 * 1000L

            return when {
                nowMillis in dawnStart..dawnEnd -> DaySolarTimePhase.DAWN
                nowMillis in dawnEnd..sunsetStart -> DaySolarTimePhase.DAYLIGHT
                nowMillis in sunsetStart..sunsetEnd -> DaySolarTimePhase.SUNSET
                nowMillis in sunsetEnd..(sunsetMillis + 4 * 3600 * 1000L) || minuteOfDay < 22 * 60 + 30 -> DaySolarTimePhase.EVENING
                else -> DaySolarTimePhase.MIDNIGHT
            }
        }

        // Fallback based on hour of day
        return when (hour) {
            in 5..6 -> DaySolarTimePhase.DAWN
            in 7..16 -> DaySolarTimePhase.DAYLIGHT
            in 17..18 -> DaySolarTimePhase.SUNSET
            in 19..22 -> DaySolarTimePhase.EVENING
            else -> DaySolarTimePhase.MIDNIGHT
        }
    }

    fun resolveSeasonalPalette(
        nowMillis: Long = System.currentTimeMillis(),
        sunriseMillis: Long,
        sunsetMillis: Long
    ): SeasonSolarPalette {
        val season = getSeason()
        val phase = getSolarPhase(nowMillis, sunriseMillis, sunsetMillis)
        return getSeasonalSolarPalette(season, phase)
    }

    fun getSeasonalSolarPalette(
        season: Season,
        phase: DaySolarTimePhase
    ): SeasonSolarPalette {
        return when (season) {
            // MÙA XUÂN: Tươi mới, non tơ, hoa đào, ấm áp dịu dàng
            Season.SPRING -> when (phase) {
                DaySolarTimePhase.DAWN -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF2A1C24.toInt(),
                    pillBgColor = 0xFF382631.toInt(),
                    chipBgColor = 0x33FFFFFF.toInt(),
                    progressStartColor = 0xFFFF80AB.toInt(),
                    progressEndColor = 0xFFFFD54F.toInt(),
                    accentColor = 0xFFFF80AB.toInt()
                )
                DaySolarTimePhase.DAYLIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF182E22.toInt(),
                    pillBgColor = 0xFF223E2E.toInt(),
                    chipBgColor = 0x2EFFFFFF.toInt(),
                    progressStartColor = 0xFF81C784.toInt(),
                    progressEndColor = 0xFFFFEE58.toInt(),
                    accentColor = 0xFF81C784.toInt()
                )
                DaySolarTimePhase.SUNSET -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF2E1825.toInt(),
                    pillBgColor = 0xFF3D2132.toInt(),
                    chipBgColor = 0x33FFFFFF.toInt(),
                    progressStartColor = 0xFFF06292.toInt(),
                    progressEndColor = 0xFFFFB74D.toInt(),
                    accentColor = 0xFFF06292.toInt()
                )
                DaySolarTimePhase.EVENING -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF161824.toInt(),
                    pillBgColor = 0xFF1F2233.toInt(),
                    chipBgColor = 0x24FFFFFF.toInt(),
                    progressStartColor = 0xFF7986CB.toInt(),
                    progressEndColor = 0xFFBA68C8.toInt(),
                    accentColor = 0xFF9FA8DA.toInt()
                )
                DaySolarTimePhase.MIDNIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF10121A.toInt(),
                    pillBgColor = 0xFF171A24.toInt(),
                    chipBgColor = 0x20FFFFFF.toInt(),
                    progressStartColor = 0xFF5C6BC0.toInt(),
                    progressEndColor = 0xFF7E57C2.toInt(),
                    accentColor = 0xFF7986CB.toInt()
                )
            }

            // MÙA HẠ: Nắng vàng rực rỡ, trời biển xanh ngắt, nhiệt đới lộng lẫy
            Season.SUMMER -> when (phase) {
                DaySolarTimePhase.DAWN -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF212836.toInt(),
                    pillBgColor = 0xFF2E384A.toInt(),
                    chipBgColor = 0x2EFFFFFF.toInt(),
                    progressStartColor = 0xFFFFCA28.toInt(),
                    progressEndColor = 0xFF4FC3F7.toInt(),
                    accentColor = 0xFFFFD54F.toInt()
                )
                DaySolarTimePhase.DAYLIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF12283A.toInt(),
                    pillBgColor = 0xFF1A3852.toInt(),
                    chipBgColor = 0x2EFFFFFF.toInt(),
                    progressStartColor = 0xFF29B6F6.toInt(),
                    progressEndColor = 0xFFFFCA28.toInt(),
                    accentColor = 0xFF4FC3F7.toInt()
                )
                DaySolarTimePhase.SUNSET -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF3E1610.toInt(),
                    pillBgColor = 0xFF521E16.toInt(),
                    chipBgColor = 0x33FFFFFF.toInt(),
                    progressStartColor = 0xFFFF7043.toInt(),
                    progressEndColor = 0xFFFFB300.toInt(),
                    accentColor = 0xFFFF7043.toInt()
                )
                DaySolarTimePhase.EVENING -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF101826.toInt(),
                    pillBgColor = 0xFF182236.toInt(),
                    chipBgColor = 0x24FFFFFF.toInt(),
                    progressStartColor = 0xFF42A5F5.toInt(),
                    progressEndColor = 0xFF26C6DA.toInt(),
                    accentColor = 0xFF81D4FA.toInt()
                )
                DaySolarTimePhase.MIDNIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF0C121C.toInt(),
                    pillBgColor = 0xFF121A28.toInt(),
                    chipBgColor = 0x20FFFFFF.toInt(),
                    progressStartColor = 0xFF1E88E5.toInt(),
                    progressEndColor = 0xFF00ACC1.toInt(),
                    accentColor = 0xFF4FC3F7.toInt()
                )
            }

            // MÙA THU: Hổ phách ấm áp, vàng nâu gỗ lá phong, hoàng hôn tím cam đồng lãng mạn
            Season.AUTUMN -> when (phase) {
                DaySolarTimePhase.DAWN -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF2B1D16.toInt(),
                    pillBgColor = 0xFF3C291F.toInt(),
                    chipBgColor = 0x30FFFFFF.toInt(),
                    progressStartColor = 0xFFFFB74D.toInt(),
                    progressEndColor = 0xFFFF8A65.toInt(),
                    accentColor = 0xFFFFA726.toInt()
                )
                DaySolarTimePhase.DAYLIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF302015.toInt(),
                    pillBgColor = 0xFF422C1D.toInt(),
                    chipBgColor = 0x2EFFFFFF.toInt(),
                    progressStartColor = 0xFFFFB300.toInt(),
                    progressEndColor = 0xFFFF7043.toInt(),
                    accentColor = 0xFFFFB74D.toInt()
                )
                DaySolarTimePhase.SUNSET -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF361824.toInt(),
                    pillBgColor = 0xFF492231.toInt(),
                    chipBgColor = 0x33FFFFFF.toInt(),
                    progressStartColor = 0xFFFF8A65.toInt(),
                    progressEndColor = 0xFFBA68C8.toInt(),
                    accentColor = 0xFFFF8A65.toInt()
                )
                DaySolarTimePhase.EVENING -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF1C1513.toInt(),
                    pillBgColor = 0xFF281E1B.toInt(),
                    chipBgColor = 0x24FFFFFF.toInt(),
                    progressStartColor = 0xFFFFA726.toInt(),
                    progressEndColor = 0xFF8D6E63.toInt(),
                    accentColor = 0xFFFFB74D.toInt()
                )
                DaySolarTimePhase.MIDNIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF120E0D.toInt(),
                    pillBgColor = 0xFF1A1412.toInt(),
                    chipBgColor = 0x20FFFFFF.toInt(),
                    progressStartColor = 0xFFFF8F00.toInt(),
                    progressEndColor = 0xFF6D4C41.toInt(),
                    accentColor = 0xFFD7CCC8.toInt()
                )
            }

            // MÙA ĐÔNG: Băng giá tuyết lạnh, lam khói sapphire, xám bạc tinh khiết
            Season.WINTER -> when (phase) {
                DaySolarTimePhase.DAWN -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF18222C.toInt(),
                    pillBgColor = 0xFF222F3D.toInt(),
                    chipBgColor = 0x2CFFFFFF.toInt(),
                    progressStartColor = 0xFF81D4FA.toInt(),
                    progressEndColor = 0xFFB0BEC5.toInt(),
                    accentColor = 0xFF90CAF9.toInt()
                )
                DaySolarTimePhase.DAYLIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF14202B.toInt(),
                    pillBgColor = 0xFF1C2D3D.toInt(),
                    chipBgColor = 0x2CFFFFFF.toInt(),
                    progressStartColor = 0xFF4FC3F7.toInt(),
                    progressEndColor = 0xFF80CBC4.toInt(),
                    accentColor = 0xFF80D8FF.toInt()
                )
                DaySolarTimePhase.SUNSET -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF221A28.toInt(),
                    pillBgColor = 0xFF2F2437.toInt(),
                    chipBgColor = 0x30FFFFFF.toInt(),
                    progressStartColor = 0xFF9575CD.toInt(),
                    progressEndColor = 0xFFFF8A80.toInt(),
                    accentColor = 0xFFB39DDB.toInt()
                )
                DaySolarTimePhase.EVENING -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF0E141D.toInt(),
                    pillBgColor = 0xFF151D2A.toInt(),
                    chipBgColor = 0x24FFFFFF.toInt(),
                    progressStartColor = 0xFF64B5F6.toInt(),
                    progressEndColor = 0xFF78909C.toInt(),
                    accentColor = 0xFF81D4FA.toInt()
                )
                DaySolarTimePhase.MIDNIGHT -> SeasonSolarPalette(
                    season = season, phase = phase,
                    cardBgColor = 0xFF0A0D13.toInt(),
                    pillBgColor = 0xFF10141D.toInt(),
                    chipBgColor = 0x20FFFFFF.toInt(),
                    progressStartColor = 0xFF42A5F5.toInt(),
                    progressEndColor = 0xFF546E7A.toInt(),
                    accentColor = 0xFF90CAF9.toInt()
                )
            }
        }
    }
}
