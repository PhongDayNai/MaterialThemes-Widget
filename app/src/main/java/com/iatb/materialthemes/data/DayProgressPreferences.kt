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

    fun resolveColors(context: Context): ResolvedPaletteColors {
        val palette = getEffectivePalette(context)
        val customColor = getEffectiveCustomColor(context)
        val transparency = getEffectiveTransparency(context)

        val rawColors = when (palette) {
            ColorPalette.DYNAMIC -> DynamicThemeExtractor.getDynamicPalette(context)
            ColorPalette.CUSTOM -> DynamicThemeExtractor.createHarmoniousTonesFromColor(customColor)
            else -> ResolvedPaletteColors(
                bgColor = palette.bgColor,
                secondaryBgColor = palette.secondaryBgColor,
                tertiaryBgColor = palette.tertiaryBgColor,
                textColor = palette.textColor
            )
        }

        val alphaInt = ((transparency.coerceIn(0, 100) / 100f) * 255).toInt()
        fun applyBgAlpha(color: Int): Int {
            return Color.argb(alphaInt, Color.red(color), Color.green(color), Color.blue(color))
        }

        return ResolvedPaletteColors(
            bgColor = applyBgAlpha(rawColors.bgColor),
            secondaryBgColor = applyBgAlpha(rawColors.secondaryBgColor),
            tertiaryBgColor = applyBgAlpha(rawColors.tertiaryBgColor),
            textColor = rawColors.textColor
        )
    }
}
