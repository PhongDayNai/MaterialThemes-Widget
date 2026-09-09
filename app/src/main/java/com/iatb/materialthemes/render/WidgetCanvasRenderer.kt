package com.iatb.materialthemes.render

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.WidgetContentMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object WidgetCanvasRenderer {

    fun render(
        context: Context,
        widthPx: Int,
        heightPx: Int,
        angleDeg: Float,
        palette: ColorPalette,
        contentMode: WidgetContentMode,
        size: WidgetSize
    ): Bitmap {
        val w = if (widthPx <= 0) 400 else widthPx
        val h = if (heightPx <= 0) 400 else heightPx

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (size) {
            WidgetSize.SIZE_2X2 -> render2x2(context, canvas, w, h, angleDeg, palette, contentMode)
            WidgetSize.SIZE_3X2, WidgetSize.SIZE_4X2 -> renderWide(context, canvas, w, h, angleDeg, palette, contentMode, size == WidgetSize.SIZE_4X2)
            WidgetSize.SIZE_2X3 -> renderTall(context, canvas, w, h, angleDeg, palette, contentMode, pillCount = 2)
            WidgetSize.SIZE_2X4 -> renderTall(context, canvas, w, h, angleDeg, palette, contentMode, pillCount = 3)
            WidgetSize.SIZE_3X3 -> render3x3(context, canvas, w, h, angleDeg, palette, contentMode)
        }

        return bitmap
    }

    private fun render2x2(
        context: Context,
        canvas: Canvas,
        w: Int,
        h: Int,
        angle: Float,
        palette: ColorPalette,
        mode: WidgetContentMode
    ) {
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)

        val pillLength = minDim * 0.90f
        val pillThickness = minDim * 0.58f

        drawTiltedPill(canvas, cx, cy, pillLength, pillThickness, angle, palette.bgColor)

        // Calculate lobe centers in unrotated coordinate system then rotate
        val rad = Math.toRadians(angle.toDouble())
        val dist = (pillLength - pillThickness) / 2f

        // One lobe (top-right when angle is negative like -45)
        val lobe1X = (cx + dist * cos(rad)).toFloat()
        val lobe1Y = (cy + dist * sin(rad)).toFloat()

        // Opposite lobe (bottom-left)
        val lobe2X = (cx - dist * cos(rad)).toFloat()
        val lobe2Y = (cy - dist * sin(rad)).toFloat()

        // Decide which lobe is higher (lower Y)
        val (topLobeX, topLobeY, btmLobeX, btmLobeY) = if (lobe1Y <= lobe2Y) {
            listOf(lobe1X, lobe1Y, lobe2X, lobe2Y)
        } else {
            listOf(lobe2X, lobe2Y, lobe1X, lobe1Y)
        }

        val lobeRadius = pillThickness / 2f

        when (mode) {
            WidgetContentMode.WEATHER -> {
                // Top lobe: Temperature text (upright)
                drawFittedText(
                    canvas,
                    context.getString(R.string.sample_temp_11),
                    topLobeX,
                    topLobeY,
                    maxSizeSp = 46f,
                    maxWidth = lobeRadius * 1.6f,
                    textColor = palette.textColor
                )

                // Bottom lobe: Weather Icon (upright)
                val iconSize = (lobeRadius * 1.15f).toInt()
                drawDrawable(
                    context,
                    canvas,
                    R.drawable.ic_weather_night_cloudy,
                    btmLobeX.toInt(),
                    btmLobeY.toInt(),
                    iconSize,
                    iconSize
                )
            }
            WidgetContentMode.CLOCK -> {
                val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())

                drawFittedText(
                    canvas,
                    hourStr,
                    topLobeX,
                    topLobeY,
                    maxSizeSp = 42f,
                    maxWidth = lobeRadius * 1.5f,
                    textColor = palette.textColor
                )

                drawFittedText(
                    canvas,
                    minStr,
                    btmLobeX,
                    btmLobeY,
                    maxSizeSp = 42f,
                    maxWidth = lobeRadius * 1.5f,
                    textColor = palette.textColor
                )
            }
            WidgetContentMode.COMBO -> {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                drawFittedText(
                    canvas,
                    timeStr,
                    topLobeX,
                    topLobeY,
                    maxSizeSp = 28f,
                    maxWidth = lobeRadius * 1.7f,
                    textColor = palette.textColor
                )

                val iconSize = (lobeRadius * 1.05f).toInt()
                drawDrawable(
                    context,
                    canvas,
                    R.drawable.ic_weather_night_cloudy,
                    btmLobeX.toInt(),
                    btmLobeY.toInt(),
                    iconSize,
                    iconSize
                )
            }
        }
    }

    private fun renderWide(
        context: Context,
        canvas: Canvas,
        w: Int,
        h: Int,
        angle: Float,
        palette: ColorPalette,
        mode: WidgetContentMode,
        isExtraWide: Boolean
    ) {
        // Two tilted elements: Left is tilted pill, Right is info capsule
        val leftCx = w * 0.28f
        val cy = h / 2f
        val pillDim = min(w * 0.45f, h * 0.88f)

        // Draw left tilted pill
        drawTiltedPill(canvas, leftCx, cy, pillDim, pillDim * 0.62f, angle, palette.bgColor)

        // Draw temperature & icon in left tilted pill
        val rad = Math.toRadians(angle.toDouble())
        val dist = (pillDim - pillDim * 0.62f) / 2f
        val l1x = (leftCx + dist * cos(rad)).toFloat()
        val l1y = (cy + dist * sin(rad)).toFloat()
        val l2x = (leftCx - dist * cos(rad)).toFloat()
        val l2y = (cy - dist * sin(rad)).toFloat()

        val (topX, topY, btmX, btmY) = if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)

        drawFittedText(canvas, context.getString(R.string.sample_temp_11), topX, topY, 34f, pillDim * 0.45f, palette.textColor)
        val iconSize = (pillDim * 0.32f).toInt()
        drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, btmX.toInt(), btmY.toInt(), iconSize, iconSize)

        // Right side: Tilted pill or card with location & details
        val rightCx = w * (if (isExtraWide) 0.68f else 0.65f)
        val rightW = w * (if (isExtraWide) 0.52f else 0.46f)
        val rightH = h * 0.76f

        drawTiltedPill(canvas, rightCx, cy, rightW, rightH, angle * 0.3f, palette.secondaryBgColor)

        // Draw text info inside right container
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textColor
            textAlign = Paint.Align.LEFT
            typeface = Typeface.DEFAULT_BOLD
            textSize = h * 0.16f
        }

        val textStartX = rightCx - rightW * 0.35f
        canvas.drawText(context.getString(R.string.sample_location), textStartX, cy - h * 0.12f, paint)

        paint.apply {
            textSize = h * 0.12f
            typeface = Typeface.DEFAULT
            color = 0xCCFFFFFF.toInt() and palette.textColor
        }
        canvas.drawText(context.getString(R.string.sample_weather_condition), textStartX, cy + h * 0.08f, paint)

        if (isExtraWide) {
            paint.textSize = h * 0.11f
            canvas.drawText(context.getString(R.string.sample_temp_range), textStartX, cy + h * 0.24f, paint)
        }
    }

    private fun renderTall(
        context: Context,
        canvas: Canvas,
        w: Int,
        h: Int,
        angle: Float,
        palette: ColorPalette,
        mode: WidgetContentMode,
        pillCount: Int
    ) {
        val pillHeight = (h / pillCount.toFloat()) * 0.82f
        val pillWidth = w * 0.90f
        val spacing = h / pillCount.toFloat()

        for (i in 0 until pillCount) {
            val cy = spacing * (i + 0.5f)
            val cx = w / 2f

            val pillBgColor = if (i == 0) palette.bgColor else palette.secondaryBgColor
            drawTiltedPill(canvas, cx, cy, pillWidth, pillHeight, angle, pillBgColor)

            // Content inside each stacked pill
            when (i) {
                0 -> {
                    // Top pill: Temp + Icon
                    val rad = Math.toRadians(angle.toDouble())
                    val dist = (pillWidth - pillHeight) / 2f
                    val l1x = (cx + dist * cos(rad)).toFloat()
                    val l1y = (cy + dist * sin(rad)).toFloat()
                    val l2x = (cx - dist * cos(rad)).toFloat()
                    val l2y = (cy - dist * sin(rad)).toFloat()
                    val (topX, topY, btmX, btmY) = if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)

                    drawFittedText(canvas, context.getString(R.string.sample_temp_11), topX, topY, 32f, pillHeight * 0.6f, palette.textColor)
                    val iconSize = (pillHeight * 0.5f).toInt()
                    drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, btmX.toInt(), btmY.toInt(), iconSize, iconSize)
                }
                1 -> {
                    // Middle pill: City & Condition
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = palette.textColor
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                        textSize = pillHeight * 0.32f
                    }
                    canvas.drawText(context.getString(R.string.sample_location), cx, cy - pillHeight * 0.05f, textPaint)

                    textPaint.apply {
                        textSize = pillHeight * 0.24f
                        typeface = Typeface.DEFAULT
                        this.color = 0xCCFFFFFF.toInt() and palette.textColor
                    }
                    canvas.drawText(context.getString(R.string.sample_weather_condition), cx, cy + pillHeight * 0.26f, textPaint)
                }
                2 -> {
                    // Bottom pill for 2x4: Next alarm & Date
                    val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        this.color = palette.textColor
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT_BOLD
                        textSize = pillHeight * 0.28f
                    }
                    canvas.drawText(dateStr, cx, cy - pillHeight * 0.05f, textPaint)

                    textPaint.apply {
                        textSize = pillHeight * 0.22f
                        typeface = Typeface.DEFAULT
                        this.color = 0xCCFFFFFF.toInt() and palette.textColor
                    }
                    canvas.drawText(context.getString(R.string.sample_temp_range), cx, cy + pillHeight * 0.26f, textPaint)
                }
            }
        }
    }

    private fun render3x3(
        context: Context,
        canvas: Canvas,
        w: Int,
        h: Int,
        angle: Float,
        palette: ColorPalette,
        mode: WidgetContentMode
    ) {
        // Large layout with 2 diagonal pills on top and a wide forecast card on bottom
        val topH = h * 0.52f
        val btmH = h * 0.38f

        val p1w = w * 0.44f
        val p1h = topH * 0.85f
        val p1cx = w * 0.27f
        val p1cy = topH * 0.5f

        // Pill 1: Temp & Icon
        drawTiltedPill(canvas, p1cx, p1cy, p1w, p1h * 0.65f, angle, palette.bgColor)

        val rad = Math.toRadians(angle.toDouble())
        val dist = (p1w - p1h * 0.65f) / 2f
        val l1x = (p1cx + dist * cos(rad)).toFloat()
        val l1y = (p1cy + dist * sin(rad)).toFloat()
        val l2x = (p1cx - dist * cos(rad)).toFloat()
        val l2y = (p1cy - dist * sin(rad)).toFloat()
        val (topX, topY, btmX, btmY) = if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)

        drawFittedText(canvas, context.getString(R.string.sample_temp_11), topX, topY, 34f, p1h * 0.5f, palette.textColor)
        val iconSize = (p1h * 0.38f).toInt()
        drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, btmX.toInt(), btmY.toInt(), iconSize, iconSize)

        // Pill 2: Location & Condition
        val p2cx = w * 0.73f
        val p2cy = topH * 0.5f
        drawTiltedPill(canvas, p2cx, p2cy, p1w, p1h * 0.65f, -angle, palette.secondaryBgColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.textColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            textSize = p1h * 0.22f
        }
        canvas.drawText(context.getString(R.string.sample_location), p2cx, p2cy - p1h * 0.05f, paint)

        paint.apply {
            textSize = p1h * 0.16f
            typeface = Typeface.DEFAULT
            color = 0xCCFFFFFF.toInt() and palette.textColor
        }
        canvas.drawText(context.getString(R.string.sample_weather_condition), p2cx, p2cy + p1h * 0.18f, paint)

        // Bottom Forecast Pill
        val btmCx = w / 2f
        val btmCy = h - btmH * 0.55f
        drawTiltedPill(canvas, btmCx, btmCy, w * 0.92f, btmH * 0.85f, angle * 0.1f, palette.secondaryBgColor)

        // 4 mini forecast items
        val hours = listOf(R.string.sample_forecast_time_1, R.string.sample_forecast_time_2, R.string.sample_forecast_time_3, R.string.sample_forecast_time_4)
        val temps = listOf(R.string.sample_forecast_temp_1, R.string.sample_forecast_temp_2, R.string.sample_forecast_temp_3, R.string.sample_forecast_temp_4)
        val icons = listOf(R.drawable.ic_weather_sunny, R.drawable.ic_weather_sunny, R.drawable.ic_weather_partly_cloudy, R.drawable.ic_weather_rainy)

        val stepX = (w * 0.80f) / 4f
        val startX = btmCx - (stepX * 1.5f)

        for (k in 0..3) {
            val itemX = startX + k * stepX
            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xBBFFFFFF.toInt() and palette.textColor
                textAlign = Paint.Align.CENTER
                textSize = btmH * 0.18f
            }
            canvas.drawText(context.getString(hours[k]), itemX, btmCy - btmH * 0.16f, timePaint)

            val miniIconSize = (btmH * 0.28f).toInt()
            drawDrawable(context, canvas, icons[k], itemX.toInt(), btmCy.toInt(), miniIconSize, miniIconSize)

            val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = palette.textColor
                textAlign = Paint.Align.CENTER
                typeface = Typeface.DEFAULT_BOLD
                textSize = btmH * 0.20f
            }
            canvas.drawText(context.getString(temps[k]), itemX, btmCy + btmH * 0.32f, tempPaint)
        }
    }

    private fun drawTiltedPill(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        length: Float,
        thickness: Float,
        angleDeg: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        canvas.save()
        canvas.rotate(angleDeg, cx, cy)

        val left = cx - length / 2f
        val top = cy - thickness / 2f
        val right = cx + length / 2f
        val bottom = cy + thickness / 2f
        val radius = thickness / 2f

        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, radius, radius, paint)

        canvas.restore()
    }

    private fun drawFittedText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        maxSizeSp: Float,
        maxWidth: Float,
        textColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = maxSizeSp * 2f
        }

        // Measure text and scale down if needed so it NEVER overflows
        var measuredW = paint.measureText(text)
        if (measuredW > maxWidth && maxWidth > 0f) {
            paint.textSize = paint.textSize * (maxWidth / measuredW)
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textCenterY = cy - bounds.exactCenterY()

        canvas.drawText(text, cx, textCenterY, paint)
    }

    private fun drawDrawable(
        context: Context,
        canvas: Canvas,
        drawableResId: Int,
        cx: Int,
        cy: Int,
        width: Int,
        height: Int
    ) {
        val drawable = ContextCompat.getDrawable(context, drawableResId) ?: return
        val halfW = width / 2
        val halfH = height / 2
        drawable.setBounds(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        drawable.draw(canvas)
    }
}
