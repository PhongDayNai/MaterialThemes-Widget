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

    private const val KEY_PENDING_HOME_ANIMATION = "pending_home_animation"
    private const val KEY_LAST_ANIM_TIMESTAMP = "last_anim_timestamp"

    fun isPendingHomeAnimation(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_PENDING_HOME_ANIMATION, false)
    }

    fun setPendingHomeAnimation(context: Context, pending: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_PENDING_HOME_ANIMATION, pending).apply()
    }

    fun getLastAnimTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_ANIM_TIMESTAMP, 0L)
    }

    fun setLastAnimTimestamp(context: Context, timestamp: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_ANIM_TIMESTAMP, timestamp).apply()
    }

    private const val KEY_QUICK_PRESETS = "quick_presets_json"

    fun getDefaultPresets(context: Context): List<QuickPreset> {
        return listOf(
            QuickPreset("def_dynamic", context.getString(R.string.preset_dynamic), ColorPalette.DYNAMIC, 100, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_olive", context.getString(R.string.preset_olive), ColorPalette.OLIVE, 100, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_glass", context.getString(R.string.preset_monet_glass), ColorPalette.DYNAMIC, 70, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_slate", context.getString(R.string.preset_dark_slate), ColorPalette.SLATE, 90, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_teal", context.getString(R.string.preset_ocean_teal), ColorPalette.TEAL, 100, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_amber", context.getString(R.string.preset_warm_amber), ColorPalette.AMBER, 100, -45f, isEnabled = true, isDefault = true)
        )
    }

    fun getQuickPresets(context: Context): List<QuickPreset> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_QUICK_PRESETS, null)
        if (jsonStr.isNullOrBlank()) {
            val defaults = getDefaultPresets(context)
            saveQuickPresets(context, defaults)
            return defaults
        }
        return try {
            val jsonArray = org.json.JSONArray(jsonStr)
            val list = mutableListOf<QuickPreset>()
            for (i in 0 until jsonArray.length()) {
                list.add(QuickPreset.fromJsonObject(jsonArray.getJSONObject(i)))
            }
            if (list.isEmpty()) getDefaultPresets(context) else list
        } catch (_: Exception) {
            getDefaultPresets(context)
        }
    }

    fun saveQuickPresets(context: Context, presets: List<QuickPreset>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        for (p in presets) {
            jsonArray.put(p.toJsonObject())
        }
        prefs.edit().putString(KEY_QUICK_PRESETS, jsonArray.toString()).apply()
    }

    fun toggleQuickPreset(context: Context, id: String, isEnabled: Boolean) {
        val current = getQuickPresets(context).map {
            if (it.id == id) it.copy(isEnabled = isEnabled) else it
        }
        saveQuickPresets(context, current)
    }

    fun addQuickPreset(
        context: Context,
        title: String,
        palette: ColorPalette,
        transparency: Int,
        angle: Float
    ) {
        val current = getQuickPresets(context).toMutableList()
        val newPreset = QuickPreset(
            id = "preset_" + System.currentTimeMillis(),
            title = title,
            palette = palette,
            transparency = transparency,
            rotationAngle = angle,
            isEnabled = true,
            isDefault = false
        )
        current.add(newPreset)
        saveQuickPresets(context, current)
    }

    fun deleteQuickPreset(context: Context, id: String) {
        val current = getQuickPresets(context).filterNot { it.id == id }
        saveQuickPresets(context, current)
    }
}

data class QuickPreset(
    val id: String,
    val title: String,
    val palette: ColorPalette,
    val transparency: Int,
    val rotationAngle: Float = -45f,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false
) {
    fun toJsonObject(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("id", id)
            put("title", title)
            put("palette", palette.name)
            put("transparency", transparency)
            put("rotationAngle", rotationAngle.toDouble())
            put("isEnabled", isEnabled)
            put("isDefault", isDefault)
        }
    }

    companion object {
        fun fromJsonObject(obj: org.json.JSONObject): QuickPreset {
            val pal = try {
                ColorPalette.valueOf(obj.optString("palette", ColorPalette.OLIVE.name))
            } catch (_: Exception) {
                ColorPalette.OLIVE
            }
            return QuickPreset(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                title = obj.optString("title", "Preset"),
                palette = pal,
                transparency = obj.optInt("transparency", 100),
                rotationAngle = obj.optDouble("rotationAngle", -45.0).toFloat(),
                isEnabled = obj.optBoolean("isEnabled", true),
                isDefault = obj.optBoolean("isDefault", false)
            )
        }
    }
}

