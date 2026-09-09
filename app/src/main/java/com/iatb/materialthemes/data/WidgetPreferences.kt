package com.iatb.materialthemes.data

import android.content.Context
import android.graphics.Color
import com.iatb.materialthemes.R

import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize

enum class ColorPalette(
    val labelResId: Int,
    val bgColor: Int,
    val textColor: Int,
    val secondaryBgColor: Int,
    val tertiaryBgColor: Int
) {
    OLIVE(R.string.theme_olive, 0xFF343B14.toInt(), 0xFFE7E5CE.toInt(), 0xFF4D5620.toInt(), 0xFF626E2A.toInt()),
    TEAL(R.string.theme_teal, 0xFF13383B.toInt(), 0xFFC9F0F4.toInt(), 0xFF235054.toInt(), 0xFF37676C.toInt()),
    SLATE(R.string.theme_slate, 0xFF1D2124.toInt(), 0xFFE6E8EB.toInt(), 0xFF2E343A.toInt(), 0xFF434C54.toInt()),
    AMBER(R.string.theme_amber, 0xFF45330E.toInt(), 0xFFFBE2BA.toInt(), 0xFF5E4717.toInt(), 0xFF7D5F22.toInt()),
    CRIMSON(R.string.theme_crimson, 0xFF44181F.toInt(), 0xFFFCD7DC.toInt(), 0xFF632731.toInt(), 0xFF823743.toInt()),
    DYNAMIC(R.string.theme_dynamic, 0, 0, 0, 0)
}

enum class WidgetContentMode(val labelResId: Int) {
    WEATHER(R.string.content_weather),
    CLOCK(R.string.content_clock),
    COMBO(R.string.content_combo)
}

object WidgetPreferences {
    private const val PREFS_NAME = "widget_preferences"
    private const val KEY_ROTATION_ANGLE = "rotation_angle"
    private const val KEY_COLOR_PALETTE = "color_palette"
    private const val KEY_CONTENT_MODE = "content_mode"
    private const val KEY_TRANSPARENCY = "transparency"
    private const val KEY_CATEGORY = "widget_category"
    private const val KEY_SIZE = "widget_size"

    fun getRotationAngle(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_ROTATION_ANGLE, -45f)
    }

    fun setRotationAngle(context: Context, angle: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(KEY_ROTATION_ANGLE, angle).apply()
    }

    fun getColorPalette(context: Context): ColorPalette {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_COLOR_PALETTE, ColorPalette.OLIVE.name)
        return try {
            ColorPalette.valueOf(name ?: ColorPalette.OLIVE.name)
        } catch (e: Exception) {
            ColorPalette.OLIVE
        }
    }

    fun setColorPalette(context: Context, palette: ColorPalette) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_COLOR_PALETTE, palette.name).apply()
    }

    fun getContentMode(context: Context): WidgetContentMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_CONTENT_MODE, WidgetContentMode.WEATHER.name)
        return try {
            WidgetContentMode.valueOf(name ?: WidgetContentMode.WEATHER.name)
        } catch (e: Exception) {
            WidgetContentMode.WEATHER
        }
    }

    fun setContentMode(context: Context, mode: WidgetContentMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CONTENT_MODE, mode.name).apply()
    }

    fun getTransparency(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_TRANSPARENCY, 100)
    }

    fun setTransparency(context: Context, value: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_TRANSPARENCY, value.coerceIn(0, 100)).apply()
    }

    fun getCategory(context: Context): WidgetCategory {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_CATEGORY, WidgetCategory.DIAGONAL.name)
        return try {
            WidgetCategory.valueOf(name ?: WidgetCategory.DIAGONAL.name)
        } catch (_: Exception) {
            WidgetCategory.DIAGONAL
        }
    }

    fun setCategory(context: Context, category: WidgetCategory) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CATEGORY, category.name).apply()
    }

    fun getSize(context: Context): WidgetSize {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString(KEY_SIZE, WidgetSize.SIZE_2X2.name)
        return try {
            WidgetSize.valueOf(name ?: WidgetSize.SIZE_2X2.name)
        } catch (_: Exception) {
            WidgetSize.SIZE_2X2
        }
    }

    fun setSize(context: Context, size: WidgetSize) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SIZE, size.name).apply()
    }
}
