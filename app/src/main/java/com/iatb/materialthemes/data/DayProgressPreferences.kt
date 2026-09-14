package com.iatb.materialthemes.data

import android.content.Context
import android.graphics.Color

object DayProgressPreferences {
    private const val PREFS_NAME = "day_progress_widget_preferences"
    private const val KEY_FOLLOW_CLOCK_WEATHER = "follow_clock_weather"
    private const val KEY_DAY_PROGRESS_PALETTE = "day_progress_palette"
    private const val KEY_DAY_PROGRESS_CUSTOM_COLOR = "day_progress_custom_color"
    private const val KEY_DAY_PROGRESS_TRANSPARENCY = "day_progress_transparency"

    fun isFollowClockWeather(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FOLLOW_CLOCK_WEATHER, true)
    }

    fun setFollowClockWeather(context: Context, follow: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FOLLOW_CLOCK_WEATHER, follow).apply()
    }

    fun getColorPalette(context: Context): ColorPalette {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_DAY_PROGRESS_PALETTE, ColorPalette.DYNAMIC.name)
        return try {
            ColorPalette.valueOf(name ?: ColorPalette.DYNAMIC.name)
        } catch (_: Exception) {
            ColorPalette.DYNAMIC
        }
    }

    fun setColorPalette(context: Context, palette: ColorPalette) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DAY_PROGRESS_PALETTE, palette.name).apply()
    }

    fun getCustomColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DAY_PROGRESS_CUSTOM_COLOR, 0xFF3F51B5.toInt())
    }

    fun setCustomColor(context: Context, color: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DAY_PROGRESS_CUSTOM_COLOR, color).apply()
    }

    fun getTransparency(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DAY_PROGRESS_TRANSPARENCY, 100)
    }

    fun setTransparency(context: Context, transparency: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DAY_PROGRESS_TRANSPARENCY, transparency.coerceIn(0, 100)).apply()
    }

    fun getEffectiveTransparency(context: Context): Int {
        return if (isFollowClockWeather(context)) {
            WidgetPreferences.getTransparency(context)
        } else {
            getTransparency(context)
        }
    }

    fun getEffectivePalette(context: Context): ColorPalette {
        return if (isFollowClockWeather(context)) {
            WidgetPreferences.getColorPalette(context)
        } else {
            getColorPalette(context)
        }
    }

    fun getEffectiveCustomColor(context: Context): Int {
        return if (isFollowClockWeather(context)) {
            WidgetPreferences.getCustomColor(context)
        } else {
            getCustomColor(context)
        }
    }

    data class DayProgressThemeColors(
        val bgColor: Int,
        val pillBgColor: Int,
        val chipBgColor: Int,
        val textColor: Int,
        val subTextColor: Int,
        val progressStartColor: Int,
        val progressEndColor: Int,
        val accentColor: Int
    )

    fun resolveDayProgressColors(
        context: Context,
        sunriseMillis: Long = 0L,
        sunsetMillis: Long = 0L
    ): DayProgressThemeColors {
        val transparency = getTransparency(context)
        val alphaInt = ((transparency.coerceIn(0, 100) / 100f) * 255).toInt()

        fun applyAlpha(color: Int): Int {
            return Color.argb(
                ((Color.alpha(color) / 255f) * (alphaInt / 255f) * 255).toInt().coerceIn(0, 255),
                Color.red(color),
                Color.green(color),
                Color.blue(color)
            )
        }

        val seasonal = SeasonTimePaletteResolver.resolveSeasonalPalette(
            nowMillis = System.currentTimeMillis(),
            sunriseMillis = sunriseMillis,
            sunsetMillis = sunsetMillis
        )
        return DayProgressThemeColors(
            bgColor = applyAlpha(seasonal.cardBgColor),
            pillBgColor = applyAlpha(seasonal.pillBgColor),
            chipBgColor = applyAlpha(seasonal.chipBgColor),
            textColor = seasonal.textColor,
            subTextColor = seasonal.subTextColor,
            progressStartColor = seasonal.progressStartColor,
            progressEndColor = seasonal.progressEndColor,
            accentColor = seasonal.accentColor
        )
    }

    fun resolveColors(context: Context): ResolvedPaletteColors {
        val themed = resolveDayProgressColors(context)
        return ResolvedPaletteColors(
            bgColor = themed.bgColor,
            secondaryBgColor = themed.pillBgColor,
            tertiaryBgColor = themed.chipBgColor,
            textColor = themed.textColor
        )
    }
}
