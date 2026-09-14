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
    DYNAMIC(R.string.theme_dynamic, 0, 0, 0, 0),
    CUSTOM(R.string.theme_custom, 0, 0, 0, 0)
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
    private const val KEY_CUSTOM_COLOR = "custom_color_seed"
    private const val KEY_TEMP_UNIT = "temp_unit"

    fun getTemperatureUnit(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TEMP_UNIT, "C") ?: "C"
    }

    fun setTemperatureUnit(context: Context, unit: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TEMP_UNIT, unit).apply()
    }

    fun getCustomColor(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_CUSTOM_COLOR, 0xFF00796B.toInt())
    }

    fun setCustomColor(context: Context, color: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_CUSTOM_COLOR, color).apply()
    }

    fun getCustomPalette(context: Context): ResolvedPaletteColors {
        val seed = getCustomColor(context)
        return DynamicThemeExtractor.createHarmoniousTonesFromColor(seed)
    }

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

    private const val KEY_LAST_ACTIVE_WIDGET_ID = "last_active_widget_id"
    private const val KEY_HAS_ACTIVE_HOME_SCREEN_WIDGET = "has_active_home_screen_widget"

    fun getLastActiveWidgetId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LAST_ACTIVE_WIDGET_ID, android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)
    }

    fun setLastActiveWidgetId(context: Context, id: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LAST_ACTIVE_WIDGET_ID, id).apply()
    }

    fun getWidgetSavedSize(context: Context, appWidgetId: Int): WidgetSize? {
        if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val name = prefs.getString("widget_size_$appWidgetId", null) ?: return null
        return try {
            WidgetSize.valueOf(name)
        } catch (_: Exception) {
            null
        }
    }

    fun setWidgetSavedSize(context: Context, appWidgetId: Int, size: WidgetSize) {
        if (appWidgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("widget_size_$appWidgetId", size.name).apply()
    }

    fun hasActiveHomeScreenWidget(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HAS_ACTIVE_HOME_SCREEN_WIDGET, false)
    }

    fun setHasActiveHomeScreenWidget(context: Context, has: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HAS_ACTIVE_HOME_SCREEN_WIDGET, has).apply()
    }

    private const val KEY_APPLIED_PRESET_ID = "applied_preset_id"

    fun getAppliedPresetId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APPLIED_PRESET_ID, null)
    }

    fun setAppliedPresetId(context: Context, id: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_APPLIED_PRESET_ID, id).apply()
    }

    private const val KEY_QUICK_PRESETS = "quick_presets_json"

    fun getDefaultPresets(context: Context): List<QuickPreset> {
        return listOf(
            QuickPreset("def_dynamic", context.getString(R.string.preset_dynamic), ColorPalette.DYNAMIC, 100, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_olive", context.getString(R.string.preset_olive), ColorPalette.OLIVE, 100, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_clean", context.getString(R.string.preset_clean_minimal), ColorPalette.SLATE, 70, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_slate", context.getString(R.string.preset_dark_slate), ColorPalette.SLATE, 90, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_teal", context.getString(R.string.preset_ocean_teal), ColorPalette.TEAL, 100, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_amber", context.getString(R.string.preset_warm_amber), ColorPalette.AMBER, 100, -45f, isEnabled = true, isDefault = true),
            QuickPreset("def_crimson", context.getString(R.string.preset_crimson), ColorPalette.CRIMSON, 100, -45f, isEnabled = true, isDefault = true)
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

    fun findDuplicatePreset(
        context: Context,
        palette: ColorPalette,
        transparency: Int,
        angle: Float,
        category: WidgetCategory = WidgetCategory.DIAGONAL,
        contentMode: WidgetContentMode = WidgetContentMode.WEATHER,
        excludeId: String? = null
    ): QuickPreset? {
        val presets = getQuickPresets(context)
        return presets.firstOrNull { preset ->
            (excludeId == null || preset.id != excludeId) &&
            preset.palette == palette &&
            preset.transparency == transparency &&
            preset.contentMode == contentMode &&
            (if (category == WidgetCategory.DIAGONAL) kotlin.math.abs(preset.rotationAngle - angle) < 0.5f else true)
        }
    }

    fun isPresetTitleTaken(context: Context, title: String, excludeId: String? = null): Boolean {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return false
        val presets = getQuickPresets(context)
        return presets.any { preset ->
            (excludeId == null || preset.id != excludeId) &&
            (preset.getLocalizedTitle(context).equals(trimmed, ignoreCase = true) ||
             preset.title.equals(trimmed, ignoreCase = true))
        }
    }

    fun addQuickPreset(
        context: Context,
        title: String,
        palette: ColorPalette,
        transparency: Int,
        angle: Float = -45f,
        contentMode: WidgetContentMode = WidgetContentMode.WEATHER,
        category: WidgetCategory = WidgetCategory.DIAGONAL,
        size: WidgetSize = WidgetSize.SIZE_2X2
    ): QuickPreset {
        val current = getQuickPresets(context).toMutableList()
        val newPreset = QuickPreset(
            id = "preset_" + System.currentTimeMillis(),
            title = title,
            palette = palette,
            transparency = transparency,
            rotationAngle = angle,
            isEnabled = true,
            isDefault = false,
            category = category,
            size = size,
            contentMode = contentMode
        )
        current.add(newPreset)
        saveQuickPresets(context, current)
        return newPreset
    }

    fun updateQuickPreset(
        context: Context,
        id: String,
        title: String,
        palette: ColorPalette,
        transparency: Int,
        angle: Float = -45f,
        contentMode: WidgetContentMode = WidgetContentMode.WEATHER,
        category: WidgetCategory = WidgetCategory.DIAGONAL,
        size: WidgetSize = WidgetSize.SIZE_2X2
    ) {
        val current = getQuickPresets(context).map { preset ->
            if (preset.id == id) {
                preset.copy(
                    title = title,
                    palette = palette,
                    transparency = transparency,
                    rotationAngle = angle,
                    contentMode = contentMode
                )
            } else {
                preset
            }
        }
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
    val isDefault: Boolean = false,
    val category: WidgetCategory = WidgetCategory.DIAGONAL,
    val size: WidgetSize = WidgetSize.SIZE_2X2,
    val contentMode: WidgetContentMode = WidgetContentMode.WEATHER
) {
    fun getLocalizedTitle(context: Context): String {
        return when (id) {
            "def_dynamic" -> context.getString(R.string.preset_dynamic)
            "def_olive" -> context.getString(R.string.preset_olive)
            "def_clean" -> context.getString(R.string.preset_clean_minimal)
            "def_slate" -> context.getString(R.string.preset_dark_slate)
            "def_teal" -> context.getString(R.string.preset_ocean_teal)
            "def_amber" -> context.getString(R.string.preset_warm_amber)
            "def_crimson" -> context.getString(R.string.preset_crimson)
            else -> title
        }
    }

    fun toJsonObject(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("id", id)
            put("title", title)
            put("palette", palette.name)
            put("transparency", transparency)
            put("rotationAngle", rotationAngle.toDouble())
            put("isEnabled", isEnabled)
            put("isDefault", isDefault)
            put("category", category.name)
            put("size", size.name)
            put("contentMode", contentMode.name)
        }
    }

    companion object {
        fun fromJsonObject(obj: org.json.JSONObject): QuickPreset {
            val pal = try {
                ColorPalette.valueOf(obj.optString("palette", ColorPalette.OLIVE.name))
            } catch (_: Exception) {
                ColorPalette.OLIVE
            }
            val cat = try {
                WidgetCategory.valueOf(obj.optString("category", WidgetCategory.DIAGONAL.name))
            } catch (_: Exception) {
                WidgetCategory.DIAGONAL
            }
            val sz = try {
                WidgetSize.valueOf(obj.optString("size", WidgetSize.SIZE_2X2.name))
            } catch (_: Exception) {
                WidgetSize.SIZE_2X2
            }
            val cm = try {
                WidgetContentMode.valueOf(obj.optString("contentMode", WidgetContentMode.WEATHER.name))
            } catch (_: Exception) {
                WidgetContentMode.WEATHER
            }
            return QuickPreset(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                title = obj.optString("title", "Preset"),
                palette = pal,
                transparency = obj.optInt("transparency", 100),
                rotationAngle = obj.optDouble("rotationAngle", -45.0).toFloat(),
                isEnabled = obj.optBoolean("isEnabled", true),
                isDefault = obj.optBoolean("isDefault", false),
                category = cat,
                size = sz,
                contentMode = cm
            )
        }
    }
}

