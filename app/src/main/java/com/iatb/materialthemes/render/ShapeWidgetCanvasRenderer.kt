package com.iatb.materialthemes.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.ResolvedPaletteColors
import com.iatb.materialthemes.data.WeatherData
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.LruCache
import kotlin.math.cos
import kotlin.math.sin

object ShapeWidgetCanvasRenderer {

    private data class CachedLayout(
        val category: WidgetCategory,
        val size: WidgetSize,
        val widthPx: Int,
        val heightPx: Int,
        val minuteKey: String,
        val tempKey: String,
        val fullView: View,
        val block1Bitmap: Bitmap?,
        val block2Bitmap: Bitmap?,
        val block3Bitmap: Bitmap?,
        val b1x: Float,
        val b1y: Float,
        val b1cx: Float,
        val b1cy: Float,
        val b2x: Float,
        val b2y: Float,
        val b2cx: Float,
        val b2cy: Float,
        val b3x: Float,
        val b3y: Float,
        val b3cx: Float,
        val b3cy: Float
    )

    private val layoutCache = LruCache<String, CachedLayout>(8)

    fun invalidateCache() {
        layoutCache.evictAll()
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun render(
        context: Context,
        category: WidgetCategory,
        size: WidgetSize,
        widthPx: Int,
        heightPx: Int,
        palette: ColorPalette = WidgetPreferences.getColorPalette(context),
        transparency: Int = WidgetPreferences.getTransparency(context),
        weather: WeatherData = WeatherRepository.getWeatherData(context),
        animProgress: Float = 1.0f
    ): Bitmap {
        val w = if (widthPx <= 0) 400 else widthPx
        val h = if (heightPx <= 0) 400 else heightPx

        val minuteKey = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val tempKey = weather.currentTempLabel

        val layoutData = getOrCreateLayout(context, category, size, w, h, palette, transparency, weather, minuteKey, tempKey)

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (animProgress >= 1.0f) {
            layoutData.fullView.draw(canvas)
            return bitmap
        }

        renderAnimatedBlocks(canvas, layoutData, size, w, h, animProgress)
        return bitmap
    }

    private fun getOrCreateLayout(
        context: Context,
        category: WidgetCategory,
        size: WidgetSize,
        w: Int,
        h: Int,
        palette: ColorPalette,
        transparency: Int,
        weather: WeatherData,
        minuteKey: String,
        tempKey: String
    ): CachedLayout {
        val cacheKey = "$category-$size-$w-$h-$palette-$transparency-$minuteKey-$tempKey"
        val existing = layoutCache.get(cacheKey)
        if (existing != null) {
            return existing
        }

        val layoutId = resolveLayoutId(category, size)
        val view = LayoutInflater.from(context).inflate(layoutId, null)

        bindDataToView(context, category, view, size, weather, palette, transparency)

        view.measure(
            View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, w, h)

        val rootGroup = view as? ViewGroup
        var b1bmp: Bitmap? = null
        var b2bmp: Bitmap? = null
        var b3bmp: Bitmap? = null
        var b1x = 0f; var b1y = 0f; var b1cx = w / 2f; var b1cy = h / 2f
        var b2x = 0f; var b2y = 0f; var b2cx = w / 2f; var b2cy = h / 2f
        var b3x = 0f; var b3y = 0f; var b3cx = w / 2f; var b3cy = h / 2f

        if (size == WidgetSize.SIZE_3X3 || size == WidgetSize.SIZE_4X3) {
            val topRow = rootGroup?.getChildAt(0) as? ViewGroup
            val block1 = topRow?.getChildAt(0)
            val block2 = topRow?.getChildAt(1)
            val block3 = rootGroup?.getChildAt(1)

            val topRowLeft = topRow?.left ?: 0
            val topRowTop = topRow?.top ?: 0

            if (block1 != null) {
                b1x = (topRowLeft + block1.left).toFloat()
                b1y = (topRowTop + block1.top).toFloat()
                b1cx = b1x + block1.width / 2f
                b1cy = b1y + block1.height / 2f
                b1bmp = snapshotView(block1)
            }
            if (block2 != null) {
                b2x = (topRowLeft + block2.left).toFloat()
                b2y = (topRowTop + block2.top).toFloat()
                b2cx = b2x + block2.width / 2f
                b2cy = b2y + block2.height / 2f
                b2bmp = snapshotView(block2)
            }
            if (block3 != null) {
                b3x = block3.left.toFloat()
                b3y = block3.top.toFloat()
                b3cx = b3x + block3.width / 2f
                b3cy = b3y + block3.height / 2f
                b3bmp = snapshotView(block3)
            }
        } else if (size == WidgetSize.SIZE_4X2 || size == WidgetSize.SIZE_3X2) {
            val block1 = rootGroup?.getChildAt(0)
            val block2 = rootGroup?.getChildAt(1)

            if (block1 != null) {
                b1x = block1.left.toFloat()
                b1y = block1.top.toFloat()
                b1cx = b1x + block1.width / 2f
                b1cy = b1y + block1.height / 2f
                b1bmp = snapshotView(block1)
            }
            if (block2 != null) {
                b2x = block2.left.toFloat()
                b2y = block2.top.toFloat()
                b2cx = b2x + block2.width / 2f
                b2cy = b2y + block2.height / 2f
                b2bmp = snapshotView(block2)
            }
        } else {
            val block1 = rootGroup?.getChildAt(0) ?: view
            b1x = block1.left.toFloat()
            b1y = block1.top.toFloat()
            b1cx = b1x + block1.width / 2f
            b1cy = b1y + block1.height / 2f
            b1bmp = snapshotView(block1)
        }

        val newCache = CachedLayout(
            category = category,
            size = size,
            widthPx = w,
            heightPx = h,
            minuteKey = minuteKey,
            tempKey = tempKey,
            fullView = view,
            block1Bitmap = b1bmp,
            block2Bitmap = b2bmp,
            block3Bitmap = b3bmp,
            b1x = b1x, b1y = b1y, b1cx = b1cx, b1cy = b1cy,
            b2x = b2x, b2y = b2y, b2cx = b2cx, b2cy = b2cy,
            b3x = b3x, b3y = b3y, b3cx = b3cx, b3cy = b3cy
        )
        layoutCache.put(cacheKey, newCache)
        return newCache
    }

    private fun snapshotView(v: View): Bitmap? {
        val w = v.width
        val h = v.height
        if (w <= 0 || h <= 0) return null
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        v.draw(c)
        return bmp
    }

    private fun resolveLayoutId(category: WidgetCategory, size: WidgetSize): Int {
        val isOrganic = category == WidgetCategory.ORGANIC
        return when (size) {
            WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> {
                if (isOrganic) R.layout.widget_organic_3x3 else R.layout.widget_scallop_3x3
            }
            WidgetSize.SIZE_4X2, WidgetSize.SIZE_3X2 -> {
                if (isOrganic) R.layout.widget_organic_4x2 else R.layout.widget_scallop_4x2
            }
            else -> {
                if (isOrganic) R.layout.widget_organic_2x2 else R.layout.widget_scallop_2x2
            }
        }
    }

    private fun bindDataToView(
        context: Context,
        category: WidgetCategory,
        view: View,
        size: WidgetSize,
        weather: WeatherData,
        palette: ColorPalette,
        transparency: Int
    ) {
        val now = Date()
        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        val hourPattern = if (is24Hour) "HH" else "hh"
        val hourStr = SimpleDateFormat(hourPattern, Locale.getDefault()).format(now)
        val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(now)
        val dateFormatPattern = try {
            val resId = if (size == WidgetSize.SIZE_4X2 && category == WidgetCategory.SCALLOP) {
                R.string.format_clock_date_full
            } else {
                R.string.format_clock_date_short
            }
            context.getString(resId)
        } catch (_: Exception) {
            "EEE, d MMM"
        }
        val dateStr = SimpleDateFormat(dateFormatPattern, Locale.getDefault()).format(now)

        view.findViewById<android.widget.TextClock>(R.id.clock_hour)?.apply {
            format12Hour = null
            format24Hour = null
            text = hourStr
        } ?: run {
            view.findViewById<TextView>(R.id.clock_hour)?.text = hourStr
        }

        view.findViewById<android.widget.TextClock>(R.id.clock_minute)?.apply {
            format12Hour = null
            format24Hour = null
            text = minStr
        } ?: run {
            view.findViewById<TextView>(R.id.clock_minute)?.text = minStr
        }

        view.findViewById<android.widget.TextClock>(R.id.clock_date)?.apply {
            format12Hour = null
            format24Hour = null
            text = dateStr
        } ?: run {
            view.findViewById<TextView>(R.id.clock_date)?.text = dateStr
        }

        view.findViewById<TextView>(R.id.tv_temp)?.text = weather.currentTempLabel
        view.findViewById<TextView>(R.id.tv_condition)?.text = weather.conditionLabel
        view.findViewById<ImageView>(R.id.img_weather)?.setImageResource(weather.currentIconResId)

        if (size == WidgetSize.SIZE_3X3 || size == WidgetSize.SIZE_4X3) {
            val rootGroup = view as? ViewGroup
            val bottomRow = rootGroup?.getChildAt(1) as? ViewGroup
            if (bottomRow != null) {
                val forecasts = weather.hourlyForecasts.take(bottomRow.childCount)
                for (i in 0 until bottomRow.childCount) {
                    val col = bottomRow.getChildAt(i) as? ViewGroup ?: continue
                    if (i < forecasts.size) {
                        val item = forecasts[i]
                        (col.getChildAt(0) as? TextView)?.text = item.timeLabel
                        (col.getChildAt(1) as? ImageView)?.setImageResource(item.iconResId)
                        (col.getChildAt(2) as? TextView)?.text = item.tempLabel
                    }
                }
            }
        }

        applyPaletteAndTransparency(context, category, view, size, palette, transparency)
    }

    private fun applyPaletteAndTransparency(
        context: Context,
        category: WidgetCategory,
        view: View,
        size: WidgetSize,
        palette: ColorPalette,
        transparency: Int
    ) {
        val rawColors = when (palette) {
            ColorPalette.DYNAMIC -> DynamicThemeExtractor.getDynamicPalette(context)
            ColorPalette.CUSTOM -> WidgetPreferences.getCustomPalette(context)
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

        val resolved = ResolvedPaletteColors(
            bgColor = applyBgAlpha(rawColors.bgColor),
            secondaryBgColor = applyBgAlpha(rawColors.secondaryBgColor),
            tertiaryBgColor = applyBgAlpha(rawColors.tertiaryBgColor),
            textColor = rawColors.textColor
        )

        val rootGroup = view as? ViewGroup ?: return

        when (size) {
            WidgetSize.SIZE_2X2, WidgetSize.SIZE_2X3, WidgetSize.SIZE_2X4 -> {
                val block1 = rootGroup.getChildAt(0)
                block1?.background?.mutate()?.setTint(resolved.bgColor)
            }
            WidgetSize.SIZE_4X2, WidgetSize.SIZE_3X2 -> {
                val block1 = rootGroup.getChildAt(0)
                val block2 = rootGroup.getChildAt(1)
                block1?.background?.mutate()?.setTint(resolved.bgColor)
                block2?.background?.mutate()?.setTint(resolved.secondaryBgColor)
            }
            WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> {
                val topRow = rootGroup.getChildAt(0) as? ViewGroup
                val block1 = topRow?.getChildAt(0)
                val block2 = topRow?.getChildAt(1)
                val block3 = rootGroup.getChildAt(1)

                block1?.background?.mutate()?.setTint(resolved.bgColor)
                block2?.background?.mutate()?.setTint(resolved.secondaryBgColor)
                block3?.background?.mutate()?.setTint(resolved.tertiaryBgColor)

                // Sub-forecast columns in block 3
                val bottomRow = block3 as? ViewGroup
                if (bottomRow != null) {
                    for (i in 0 until bottomRow.childCount) {
                        val col = bottomRow.getChildAt(i) as? ViewGroup ?: continue
                        (col.getChildAt(0) as? TextView)?.setTextColor(resolved.textColor)
                        (col.getChildAt(1) as? ImageView)?.setColorFilter(resolved.textColor)
                        (col.getChildAt(2) as? TextView)?.setTextColor(resolved.textColor)
                    }
                }
            }
            else -> {
                val block1 = rootGroup.getChildAt(0)
                block1?.background?.mutate()?.setTint(resolved.bgColor)
            }
        }

        // Apply text and icon colors with high contrast
        view.findViewById<TextView>(R.id.clock_hour)?.setTextColor(resolved.textColor)
        view.findViewById<TextView>(R.id.clock_minute)?.setTextColor(resolved.textColor)
        view.findViewById<TextView>(R.id.clock_date)?.setTextColor(resolved.textColor)
        view.findViewById<TextView>(R.id.tv_temp)?.setTextColor(resolved.textColor)
        view.findViewById<TextView>(R.id.tv_condition)?.setTextColor(resolved.textColor)
        view.findViewById<ImageView>(R.id.img_weather)?.setColorFilter(resolved.textColor)
    }

    private fun renderAnimatedBlocks(
        canvas: Canvas,
        data: CachedLayout,
        size: WidgetSize,
        w: Int,
        h: Int,
        animProgress: Float
    ) {
        if (size == WidgetSize.SIZE_3X3 || size == WidgetSize.SIZE_4X3) {
            // Block 1: Left flower/scallop clock (0.00 -> 0.65)
            drawAnimatedBlock(
                canvas, data.block1Bitmap,
                data.b1x, data.b1y, data.b1cx, data.b1cy,
                animProgress, 0.00f, 0.65f
            )

            // Block 2: Right pebble/leaf weather card (0.15 -> 0.85)
            drawAnimatedBlock(
                canvas, data.block2Bitmap,
                data.b2x, data.b2y, data.b2cx, data.b2cy,
                animProgress, 0.15f, 0.85f
            )

            // Block 3: Bottom hourly forecast card (0.30 -> 1.00, slide up)
            drawAnimatedBlock(
                canvas, data.block3Bitmap,
                data.b3x, data.b3y, data.b3cx, data.b3cy,
                animProgress, 0.30f, 1.00f,
                slideUpDistance = h * 0.08f
            )
        } else if (size == WidgetSize.SIZE_4X2 || size == WidgetSize.SIZE_3X2) {
            // Block 1: Left clock (0.00 -> 0.75)
            drawAnimatedBlock(
                canvas, data.block1Bitmap,
                data.b1x, data.b1y, data.b1cx, data.b1cy,
                animProgress, 0.00f, 0.75f
            )

            // Block 2: Right weather card (0.20 -> 1.00)
            drawAnimatedBlock(
                canvas, data.block2Bitmap,
                data.b2x, data.b2y, data.b2cx, data.b2cy,
                animProgress, 0.20f, 1.00f
            )
        } else {
            // 2x2 centerpiece (0.00 -> 1.00)
            drawAnimatedBlock(
                canvas, data.block1Bitmap,
                data.b1x, data.b1y, data.b1cx, data.b1cy,
                animProgress, 0.00f, 1.00f
            )
        }
    }

    private fun drawAnimatedBlock(
        canvas: Canvas,
        bitmap: Bitmap?,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        animProgress: Float,
        startP: Float,
        endP: Float,
        slideUpDistance: Float = 0f
    ) {
        if (bitmap == null) return
        val tau = when {
            animProgress <= startP -> 0f
            animProgress >= endP -> 1f
            else -> (animProgress - startP) / (endP - startP)
        }
        if (tau <= 0.001f) return

        // Smooth continuous sinusoidal Jelly spring curve
        val scale = if (tau < 0.72f) {
            val t = tau / 0.72f
            0.80f + 0.24f * sin(t * (Math.PI / 2).toFloat())
        } else {
            val t = (tau - 0.72f) / 0.28f
            1.04f - 0.04f * (0.5f - 0.5f * cos(t * Math.PI.toFloat()))
        }

        val alpha = (0.20f + 0.80f * sin(tau * (Math.PI / 2).toFloat())).coerceIn(0f, 1f)
        bitmapPaint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        val offsetY = (1f - sin(tau * (Math.PI / 2).toFloat())) * slideUpDistance

        canvas.save()
        canvas.translate(0f, offsetY)
        canvas.scale(scale, scale, cx, cy)
        canvas.drawBitmap(bitmap, bx, by, bitmapPaint)
        canvas.restore()
    }
}
