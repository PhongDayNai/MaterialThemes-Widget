package com.iatb.materialthemes.data

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils

data class ResolvedPaletteColors(
    val bgColor: Int,
    val secondaryBgColor: Int,
    val tertiaryBgColor: Int,
    val textColor: Int
)

object DynamicThemeExtractor {

    @Volatile
    private var cachedPalette: ResolvedPaletteColors? = null

    fun onWallpaperColorsChanged(colors: WallpaperColors?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 && colors != null) {
            val primaryArgb = colors.primaryColor.toArgb()
            val secondaryArgb = colors.secondaryColor?.toArgb()
            val tertiaryArgb = colors.tertiaryColor?.toArgb()
            cachedPalette = createHarmoniousTones(primaryArgb, secondaryArgb, tertiaryArgb)
        } else {
            cachedPalette = null
        }
    }

    fun invalidateCache() {
        cachedPalette = null
    }

    fun getDynamicPalette(context: Context): ResolvedPaletteColors {
        // Return active cached palette if available
        cachedPalette?.let { return it }

        // 1. Query WallpaperManager real-time wallpaper colors (API 27+)
        // Directly reflects wallpaper changes instantly without waiting for system Monet overlay compilation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wm = context.getSystemService(WallpaperManager::class.java)
                    ?: WallpaperManager.getInstance(context)
                val wpColors = wm.getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                if (wpColors != null) {
                    val primaryArgb = wpColors.primaryColor.toArgb()
                    val secondaryArgb = wpColors.secondaryColor?.toArgb()
                    val tertiaryArgb = wpColors.tertiaryColor?.toArgb()

                    val palette = createHarmoniousTones(primaryArgb, secondaryArgb, tertiaryArgb)
                    cachedPalette = palette
                    return palette
                }
            } catch (_: Exception) {
            }
        }

        // 2. Fallback to Android 12+ (API 31+) Monet system dynamic colors
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val a1 = ContextCompat.getColor(context, android.R.color.system_accent1_800)
                val a2 = ContextCompat.getColor(context, android.R.color.system_accent2_800)
                val a3 = ContextCompat.getColor(context, android.R.color.system_accent3_800)
                val text = ContextCompat.getColor(context, android.R.color.system_accent1_100)

                // Verify that colors are valid and distinct
                if (a1 != 0 && a2 != 0 && a3 != 0) {
                    val palette = ResolvedPaletteColors(
                        bgColor = a1,
                        secondaryBgColor = a2,
                        tertiaryBgColor = a3,
                        textColor = text
                    )
                    cachedPalette = palette
                    return palette
                }
            } catch (_: Exception) {
            }
        }

        // 3. Fallback default elegant Material tone (Olive/Sage inspired)
        return ResolvedPaletteColors(
            bgColor = 0xFF2A341E.toInt(),
            secondaryBgColor = 0xFF3F4B2D.toInt(),
            tertiaryBgColor = 0xFF54633C.toInt(),
            textColor = 0xFFE3E8D8.toInt()
        )
    }

    fun createHarmoniousTonesFromColor(primary: Int): ResolvedPaletteColors {
        return createHarmoniousTones(primary, null, null)
    }

    private fun createHarmoniousTones(
        primary: Int,
        secondary: Int?,
        tertiary: Int?
    ): ResolvedPaletteColors {
        val primaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(primary, primaryHsl)

        // Generate dark rich container tones matching Material You specs
        val bg1 = ColorUtils.HSLToColor(
            floatArrayOf(
                primaryHsl[0],
                (primaryHsl[1] * 0.65f).coerceIn(0.18f, 0.70f),
                0.18f
            )
        )

        val bg2 = if (secondary != null) {
            val secHsl = FloatArray(3)
            ColorUtils.colorToHSL(secondary, secHsl)
            ColorUtils.HSLToColor(
                floatArrayOf(
                    secHsl[0],
                    (secHsl[1] * 0.60f).coerceIn(0.18f, 0.65f),
                    0.26f
                )
            )
        } else {
            ColorUtils.HSLToColor(
                floatArrayOf(
                    (primaryHsl[0] + 16f) % 360f,
                    (primaryHsl[1] * 0.60f).coerceIn(0.18f, 0.65f),
                    0.27f
                )
            )
        }

        val bg3 = if (tertiary != null) {
            val tertHsl = FloatArray(3)
            ColorUtils.colorToHSL(tertiary, tertHsl)
            ColorUtils.HSLToColor(
                floatArrayOf(
                    tertHsl[0],
                    (tertHsl[1] * 0.55f).coerceIn(0.15f, 0.60f),
                    0.34f
                )
            )
        } else {
            ColorUtils.HSLToColor(
                floatArrayOf(
                    (primaryHsl[0] + 36f) % 360f,
                    (primaryHsl[1] * 0.50f).coerceIn(0.15f, 0.60f),
                    0.35f
                )
            )
        }

        // High contrast light text matching primary hue
        val text = ColorUtils.HSLToColor(
            floatArrayOf(
                primaryHsl[0],
                0.20f,
                0.92f
            )
        )

        return ResolvedPaletteColors(
            bgColor = bg1,
            secondaryBgColor = bg2,
            tertiaryBgColor = bg3,
            textColor = text
        )
    }
}
