package com.iatb.materialthemes.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.WeatherData
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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
        size: WidgetSize,
        weather: WeatherData = WeatherRepository.getWeatherData(context)
    ): Bitmap {
        val w = if (widthPx <= 0) 400 else widthPx
        val h = if (heightPx <= 0) 400 else heightPx

        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (size) {
            WidgetSize.SIZE_2X2 -> render2x2(context, canvas, w, h, angleDeg, palette, contentMode, weather)
            WidgetSize.SIZE_3X2, WidgetSize.SIZE_4X2 -> renderWide(context, canvas, w, h, angleDeg, palette, contentMode, size == WidgetSize.SIZE_4X2, weather)
            WidgetSize.SIZE_2X3 -> renderTall(context, canvas, w, h, angleDeg, palette, contentMode, pillCount = 2, weather = weather)
            WidgetSize.SIZE_2X4 -> renderTall(context, canvas, w, h, angleDeg, palette, contentMode, pillCount = 3, weather = weather)
            WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> render3x3(context, canvas, w, h, angleDeg, palette, contentMode, is4x3 = (size == WidgetSize.SIZE_4X3), weather = weather)
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
        mode: WidgetContentMode,
        weather: WeatherData
    ) {
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)

        val pillLength = minDim * 0.90f
        val pillThickness = minDim * 0.54f

        drawTiltedPill(canvas, cx, cy, pillLength, pillThickness, angle, palette.bgColor)

        val rad = Math.toRadians(angle.toDouble())
        val lobeDist = (pillLength - pillThickness) / 2f
        val dist = lobeDist * 0.88f

        val lobe1X = (cx + dist * cos(rad)).toFloat()
        val lobe1Y = (cy + dist * sin(rad)).toFloat()
        val lobe2X = (cx - dist * cos(rad)).toFloat()
        val lobe2Y = (cy - dist * sin(rad)).toFloat()

        val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
            if (lobe1X <= lobe2X) listOf(lobe1X, lobe1Y, lobe2X, lobe2Y) else listOf(lobe2X, lobe2Y, lobe1X, lobe1Y)
        } else {
            if (lobe1Y <= lobe2Y) listOf(lobe1X, lobe1Y, lobe2X, lobe2Y) else listOf(lobe2X, lobe2Y, lobe1X, lobe1Y)
        }

        val lobeRadius = pillThickness / 2f
        val contentAngle = (angle * 0.35f).coerceIn(-18f, 18f)

        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(
                    canvas,
                    weather.currentTempLabel,
                    firstX,
                    firstY,
                    maxSizePx = pillThickness * 0.48f,
                    maxWidth = lobeRadius * 1.5f,
                    textColor = palette.textColor,
                    isBold = true,
                    contentAngleDeg = contentAngle
                )
                val iconSize = (lobeRadius * 1.15f).toInt()
                drawDrawable(
                    context,
                    canvas,
                    weather.currentIconResId,
                    secondX.toInt(),
                    secondY.toInt(),
                    iconSize,
                    iconSize,
                    contentAngleDeg = contentAngle
                )
            }
            WidgetContentMode.CLOCK -> {
                val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())

                drawFittedSingleLineText(
                    canvas,
                    hourStr,
                    firstX,
                    firstY,
                    maxSizePx = pillThickness * 0.46f,
                    maxWidth = lobeRadius * 1.5f,
                    textColor = palette.textColor,
                    isBold = true,
                    contentAngleDeg = contentAngle
                )
                drawFittedSingleLineText(
                    canvas,
                    minStr,
                    secondX,
                    secondY,
                    maxSizePx = pillThickness * 0.46f,
                    maxWidth = lobeRadius * 1.5f,
                    textColor = palette.textColor,
                    isBold = true,
                    contentAngleDeg = contentAngle
                )
            }
            WidgetContentMode.COMBO -> {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(
                    canvas,
                    timeStr,
                    firstX,
                    firstY,
                    maxSizePx = pillThickness * 0.36f,
                    maxWidth = lobeRadius * 1.6f,
                    textColor = palette.textColor,
                    isBold = true,
                    contentAngleDeg = contentAngle
                )
                val iconSize = (lobeRadius * 0.88f).toInt()
                drawComboWeatherLobe(
                    context,
                    canvas,
                    weather.currentIconResId,
                    weather.currentTempLabel,
                    secondX,
                    secondY,
                    iconSize,
                    pillThickness * 0.36f,
                    palette.textColor,
                    contentAngle
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
        isExtraWide: Boolean,
        weather: WeatherData
    ) {
        val cy = h / 2f

        // Left tilted pill dimensions
        val leftW = if (isExtraWide) min(w * 0.33f, h * 0.85f) else min(w * 0.44f, h * 0.85f)
        val leftH = leftW * 0.58f

        // Right side container dimensions: sleek, horizontal pill/card
        val rightW = if (isExtraWide) w * 0.64f else w * 0.52f
        val rightH = h * 0.76f
        val rightAngle = (angle * 0.12f).coerceIn(-6f, 6f)

        // Calculate rightmost visual extent of the left tilted pill's cap
        val rad = Math.toRadians(abs(angle).toDouble())
        val leftRightTipX = ((leftW - leftH) / 2f) * cos(rad).toFloat() + (leftH / 2f)

        // Tight, elegant gap between shapes (6-10dp) so they never look far apart
        val gap = (w * 0.018f).coerceIn(6f, 10f)

        // Center both shapes together as a single unified lockup in the canvas
        val totalGroupW = (leftW / 2f) + leftRightTipX + gap + rightW
        val startX = (w - totalGroupW) / 2f

        val leftCx = startX + (leftW / 2f)
        val rightCx = leftCx + leftRightTipX + gap + (rightW / 2f)

        drawTiltedPill(canvas, leftCx, cy, leftW, leftH, angle, palette.bgColor)

        // Draw content in left tilted pill lobes with subtle tilt
        val tiltRadLeft = Math.toRadians(angle.toDouble())
        val lobeDistLeft = (leftW - leftH) / 2f
        val dist = lobeDistLeft * 0.88f
        val l1x = (leftCx + dist * cos(tiltRadLeft)).toFloat()
        val l1y = (cy + dist * sin(tiltRadLeft)).toFloat()
        val l2x = (leftCx - dist * cos(tiltRadLeft)).toFloat()
        val l2y = (cy - dist * sin(tiltRadLeft)).toFloat()

        val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
            if (l1x <= l2x) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        } else {
            if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        }

        val leftContentAngle = (angle * 0.35f).coerceIn(-18f, 18f)

        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(
                    canvas,
                    weather.currentTempLabel,
                    firstX,
                    firstY,
                    maxSizePx = leftH * 0.52f,
                    maxWidth = leftH * 0.78f,
                    textColor = palette.textColor,
                    isBold = true,
                    contentAngleDeg = leftContentAngle
                )
                val iconSize = (leftH * 0.55f).toInt()
                drawDrawable(context, canvas, weather.currentIconResId, secondX.toInt(), secondY.toInt(), iconSize, iconSize, contentAngleDeg = leftContentAngle)
            }
            WidgetContentMode.CLOCK -> {
                val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, hourStr, firstX, firstY, maxSizePx = leftH * 0.48f, maxWidth = leftH * 0.75f, textColor = palette.textColor, isBold = true, contentAngleDeg = leftContentAngle)
                drawFittedSingleLineText(canvas, minStr, secondX, secondY, maxSizePx = leftH * 0.48f, maxWidth = leftH * 0.75f, textColor = palette.textColor, isBold = true, contentAngleDeg = leftContentAngle)
            }
            WidgetContentMode.COMBO -> {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, timeStr, firstX, firstY, maxSizePx = leftH * 0.40f, maxWidth = leftH * 0.85f, textColor = palette.textColor, isBold = true, contentAngleDeg = leftContentAngle)
                val iconSize = (leftH * 0.40f).toInt()
                drawComboWeatherLobe(context, canvas, weather.currentIconResId, weather.currentTempLabel, secondX, secondY, iconSize, leftH * 0.36f, palette.textColor, leftContentAngle)
            }
        }

        val cardRadius = rightH * 0.42f
        drawRoundedCard(canvas, rightCx, cy, rightW, rightH, cardRadius, rightAngle, palette.secondaryBgColor)

        // Safe usable area inside right container with generous vertical clearance from curved borders
        val tiltRad = Math.toRadians(abs(rightAngle).toDouble())
        val safeInnerW = (rightW * 0.82f - (rightH * sin(tiltRad)).toFloat()).coerceAtLeast(rightW * 0.65f)
        val safeInnerH = rightH * 0.70f
        val cardContentAngle = rightAngle * 0.75f

        if (isExtraWide) {
            // 4x2: Split right container into 2 balanced columns (Left: Info, Right: Weather stats / Forecast)
            val subColW = safeInnerW * 0.48f
            val subLeftCx = rightCx - safeInnerW * 0.25f
            val subRightCx = rightCx + safeInnerW * 0.25f

            when (mode) {
                WidgetContentMode.WEATHER -> {
                    // Left sub-column: Location + Condition (wrapped)
                    drawFittedSingleLineText(
                        canvas,
                        weather.locationName,
                        subLeftCx,
                        cy - safeInnerH * 0.24f,
                        maxSizePx = rightH * 0.18f,
                        maxWidth = subColW,
                        textColor = palette.textColor,
                        isBold = true,
                        contentAngleDeg = cardContentAngle
                    )
                    drawStaticLayoutText(
                        canvas,
                        weather.conditionLabel,
                        subLeftCx,
                        cy + safeInnerH * 0.16f,
                        maxWidth = subColW,
                        maxHeight = safeInnerH * 0.42f,
                        maxLines = 2,
                        initialTextSizePx = rightH * 0.14f,
                        textColor = (0xCCFFFFFF.toInt() and palette.textColor),
                        isBold = false,
                        contentAngleDeg = cardContentAngle
                    )

                    // Right sub-column: Temp range & Wind/Humidity
                    drawFittedSingleLineText(
                        canvas,
                        weather.tempRangeLabel,
                        subRightCx,
                        cy - safeInnerH * 0.20f,
                        maxSizePx = rightH * 0.15f,
                        maxWidth = subColW,
                        textColor = palette.textColor,
                        isBold = true,
                        contentAngleDeg = cardContentAngle
                    )
                    drawFittedSingleLineText(
                        canvas,
                        weather.windLabel,
                        subRightCx,
                        cy + safeInnerH * 0.06f,
                        maxSizePx = rightH * 0.13f,
                        maxWidth = subColW,
                        textColor = (0xBBFFFFFF.toInt() and palette.textColor),
                        contentAngleDeg = cardContentAngle
                    )
                    drawFittedSingleLineText(
                        canvas,
                        weather.humidityLabel,
                        subRightCx,
                        cy + safeInnerH * 0.28f,
                        maxSizePx = rightH * 0.13f,
                        maxWidth = subColW,
                        textColor = (0xBBFFFFFF.toInt() and palette.textColor),
                        contentAngleDeg = cardContentAngle
                    )
                }
                WidgetContentMode.CLOCK -> {
                    val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())

                    drawFittedSingleLineText(canvas, dayStr, subLeftCx, cy - safeInnerH * 0.18f, maxSizePx = rightH * 0.20f, maxWidth = subColW, textColor = palette.textColor, isBold = true, contentAngleDeg = cardContentAngle)
                    drawStaticLayoutText(canvas, dateStr, subLeftCx, cy + safeInnerH * 0.16f, maxWidth = subColW, maxHeight = safeInnerH * 0.40f, maxLines = 2, initialTextSizePx = rightH * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = cardContentAngle)

                    drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), subRightCx, cy, maxSizePx = rightH * 0.15f, maxWidth = subColW, textColor = palette.textColor, isBold = true, contentAngleDeg = cardContentAngle)
                }
                WidgetContentMode.COMBO -> {
                    val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                    val tempAndCondition = "${weather.currentTempLabel} • ${weather.conditionLabel}"
                    drawFittedSingleLineText(canvas, weather.locationName, subLeftCx, cy - safeInnerH * 0.22f, maxSizePx = rightH * 0.18f, maxWidth = subColW, textColor = palette.textColor, isBold = true, contentAngleDeg = cardContentAngle)
                    drawStaticLayoutText(canvas, tempAndCondition, subLeftCx, cy + safeInnerH * 0.16f, maxWidth = subColW, maxHeight = safeInnerH * 0.42f, maxLines = 2, initialTextSizePx = rightH * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = cardContentAngle)

                    drawFittedSingleLineText(canvas, dateStr, subRightCx, cy - safeInnerH * 0.12f, maxSizePx = rightH * 0.15f, maxWidth = subColW, textColor = palette.textColor, isBold = true, contentAngleDeg = cardContentAngle)
                    drawFittedSingleLineText(canvas, weather.tempRangeLabel, subRightCx, cy + safeInnerH * 0.18f, maxSizePx = rightH * 0.13f, maxWidth = subColW, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = cardContentAngle)
                }
            }
        } else {
            // 3x2: Single integrated column with auto-wrapping condition text, safe clearance, and subtle tilt
            when (mode) {
                WidgetContentMode.WEATHER -> {
                    // Line 1: Location (e.g. "Hanoi")
                    drawFittedSingleLineText(
                        canvas,
                        weather.locationName,
                        rightCx,
                        cy - safeInnerH * 0.26f,
                        maxSizePx = rightH * 0.15f,
                        maxWidth = safeInnerW,
                        textColor = palette.textColor,
                        isBold = true,
                        contentAngleDeg = cardContentAngle
                    )

                    // Line 2: Weather condition (e.g. "Partly Cloudy" -> wrapped to 2 lines if needed!)
                    drawStaticLayoutText(
                        canvas,
                        weather.conditionLabel,
                        rightCx,
                        cy + safeInnerH * 0.04f,
                        maxWidth = safeInnerW,
                        maxHeight = safeInnerH * 0.34f,
                        maxLines = 2,
                        initialTextSizePx = rightH * 0.14f,
                        textColor = (0xCCFFFFFF.toInt() and palette.textColor),
                        isBold = false,
                        alignment = Layout.Alignment.ALIGN_CENTER,
                        contentAngleDeg = cardContentAngle
                    )

                    // Line 3: Temp range (e.g. "H: 31° • L: 23°")
                    drawFittedSingleLineText(
                        canvas,
                        weather.tempRangeLabel,
                        rightCx,
                        cy + safeInnerH * 0.30f,
                        maxSizePx = rightH * 0.12f,
                        maxWidth = safeInnerW,
                        textColor = (0xBBFFFFFF.toInt() and palette.textColor),
                        isBold = false,
                        contentAngleDeg = cardContentAngle
                    )
                }
                WidgetContentMode.CLOCK -> {
                    val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())

                    drawFittedSingleLineText(canvas, dayStr, rightCx, cy - safeInnerH * 0.26f, maxSizePx = rightH * 0.18f, maxWidth = safeInnerW, textColor = palette.textColor, isBold = true, contentAngleDeg = cardContentAngle)
                    drawStaticLayoutText(canvas, dateStr, rightCx, cy + safeInnerH * 0.04f, maxWidth = safeInnerW, maxHeight = safeInnerH * 0.34f, maxLines = 2, initialTextSizePx = rightH * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = cardContentAngle)
                    drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), rightCx, cy + safeInnerH * 0.30f, maxSizePx = rightH * 0.12f, maxWidth = safeInnerW, textColor = (0xBBFFFFFF.toInt() and palette.textColor), contentAngleDeg = cardContentAngle)
                }
                WidgetContentMode.COMBO -> {
                    val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                    val tempAndCondition = "${weather.currentTempLabel} • ${weather.conditionLabel}"
                    drawFittedSingleLineText(canvas, weather.locationName, rightCx, cy - safeInnerH * 0.26f, maxSizePx = rightH * 0.16f, maxWidth = safeInnerW, textColor = palette.textColor, isBold = true, contentAngleDeg = cardContentAngle)
                    drawStaticLayoutText(canvas, tempAndCondition, rightCx, cy + safeInnerH * 0.04f, maxWidth = safeInnerW, maxHeight = safeInnerH * 0.34f, maxLines = 2, initialTextSizePx = rightH * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = cardContentAngle)
                    drawFittedSingleLineText(canvas, dateStr, rightCx, cy + safeInnerH * 0.30f, maxSizePx = rightH * 0.13f, maxWidth = safeInnerW, textColor = (0xBBFFFFFF.toInt() and palette.textColor), contentAngleDeg = cardContentAngle)
                }
            }
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
        pillCount: Int,
        weather: WeatherData
    ) {
        val tiltRad = Math.toRadians(abs(angle).toDouble().coerceAtMost(45.0))

        // Ensure pill size fits within canvas bounds both horizontally and vertically
        val pillHeight = (h / (pillCount + 0.35f)) * 0.72f
        val maxAllowedW = min(
            w * 0.86f,
            ((w * 0.92f) - (pillHeight * sin(tiltRad).toFloat())) / cos(tiltRad).toFloat().coerceAtLeast(0.5f)
        )

        // Vertical half-extent of the top tilted pill
        val pillHalfV = ((maxAllowedW / 2f) * sin(tiltRad) + (pillHeight / 2f) * cos(tiltRad)).toFloat()

        // Safe margins to ensure zero clipping at y = 0 and y = h
        val safeMargin = (h * 0.035f).coerceAtLeast(12f)
        val topCy = pillHalfV + safeMargin
        val bottomCy = h - (pillHeight / 2f) - safeMargin
        val verticalSpan = (bottomCy - topCy).coerceAtLeast(0f)

        val cx = w / 2f

        for (i in 0 until pillCount) {
            val cy = if (pillCount <= 1) h / 2f else topCy + (i.toFloat() / (pillCount - 1f)) * verticalSpan
            val pillBgColor = if (i == 0) palette.bgColor else palette.secondaryBgColor

            when (i) {
                0 -> {
                    // Top pill: Tilted at selected angle
                    drawTiltedPill(canvas, cx, cy, maxAllowedW, pillHeight, angle, pillBgColor)

                    val rad = Math.toRadians(angle.toDouble())
                    val lobeDistTall = (maxAllowedW - pillHeight) / 2f
                    val dist = lobeDistTall * 0.88f
                    val l1x = (cx + dist * cos(rad)).toFloat()
                    val l1y = (cy + dist * sin(rad)).toFloat()
                    val l2x = (cx - dist * cos(rad)).toFloat()
                    val l2y = (cy - dist * sin(rad)).toFloat()

                    val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
                        if (l1x <= l2x) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
                    } else {
                        if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
                    }

                    val contentAngle = (angle * 0.35f).coerceIn(-18f, 18f)

                    when (mode) {
                        WidgetContentMode.WEATHER -> {
                            drawFittedSingleLineText(canvas, weather.currentTempLabel, firstX, firstY, maxSizePx = pillHeight * 0.52f, maxWidth = pillHeight * 0.85f, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            val iconSize = (pillHeight * 0.56f).toInt()
                            drawDrawable(context, canvas, weather.currentIconResId, secondX.toInt(), secondY.toInt(), iconSize, iconSize, contentAngleDeg = contentAngle)
                        }
                        WidgetContentMode.CLOCK -> {
                            val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                            val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, hourStr, firstX, firstY, maxSizePx = pillHeight * 0.46f, maxWidth = pillHeight * 0.80f, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            drawFittedSingleLineText(canvas, minStr, secondX, secondY, maxSizePx = pillHeight * 0.46f, maxWidth = pillHeight * 0.80f, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                        }
                        WidgetContentMode.COMBO -> {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, timeStr, firstX, firstY, maxSizePx = pillHeight * 0.38f, maxWidth = pillHeight * 0.88f, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            val iconSize = (pillHeight * 0.40f).toInt()
                            drawComboWeatherLobe(context, canvas, weather.currentIconResId, weather.currentTempLabel, secondX, secondY, iconSize, pillHeight * 0.36f, palette.textColor, contentAngle)
                        }
                    }
                }
                1 -> {
                    // Second pill: Subtle tilt with auto-wrapped text matching container angle
                    val subAngle = (angle * 0.16f).coerceIn(-8f, 8f)
                    drawTiltedPill(canvas, cx, cy, maxAllowedW, pillHeight, subAngle, pillBgColor)

                    val safeW = maxAllowedW * 0.72f
                    val contentAngle = subAngle * 0.75f

                    when (mode) {
                        WidgetContentMode.WEATHER -> {
                            drawFittedSingleLineText(canvas, weather.locationName, cx, cy - pillHeight * 0.22f, maxSizePx = pillHeight * 0.20f, maxWidth = safeW, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            drawStaticLayoutText(canvas, weather.conditionLabel, cx, cy + pillHeight * 0.04f, maxWidth = safeW, maxHeight = pillHeight * 0.36f, maxLines = 2, initialTextSizePx = pillHeight * 0.15f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                            drawFittedSingleLineText(canvas, weather.tempRangeLabel, cx, cy + pillHeight * 0.28f, maxSizePx = pillHeight * 0.13f, maxWidth = safeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                        }
                        WidgetContentMode.CLOCK -> {
                            val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                            val dateStr = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, dayStr, cx, cy - pillHeight * 0.18f, maxSizePx = pillHeight * 0.22f, maxWidth = safeW, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            drawFittedSingleLineText(canvas, dateStr, cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                        }
                        WidgetContentMode.COMBO -> {
                            val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                            val tempAndCondition = "${weather.currentTempLabel} • ${weather.conditionLabel}"
                            drawFittedSingleLineText(canvas, weather.locationName, cx, cy - pillHeight * 0.20f, maxSizePx = pillHeight * 0.20f, maxWidth = safeW, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            drawStaticLayoutText(canvas, tempAndCondition, cx, cy + pillHeight * 0.04f, maxWidth = safeW, maxHeight = pillHeight * 0.36f, maxLines = 2, initialTextSizePx = pillHeight * 0.15f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                            drawFittedSingleLineText(canvas, dateStr, cx, cy + pillHeight * 0.28f, maxSizePx = pillHeight * 0.13f, maxWidth = safeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                        }
                    }
                }
                2 -> {
                    // Third pill for 2x4: Next alarm & stats
                    val subAngle = (angle * 0.10f).coerceIn(-5f, 5f)
                    drawTiltedPill(canvas, cx, cy, maxAllowedW, pillHeight, subAngle, pillBgColor)
                    val safeW = maxAllowedW * 0.72f
                    val contentAngle = subAngle * 0.75f

                    when (mode) {
                        WidgetContentMode.WEATHER -> {
                            drawFittedSingleLineText(canvas, weather.humidityLabel, cx, cy - pillHeight * 0.16f, maxSizePx = pillHeight * 0.20f, maxWidth = safeW, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            drawFittedSingleLineText(canvas, weather.windLabel, cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                        }
                        WidgetContentMode.CLOCK -> {
                            val dateStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), cx, cy - pillHeight * 0.16f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            drawFittedSingleLineText(canvas, dateStr, cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                        }
                        WidgetContentMode.COMBO -> {
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), cx, cy - pillHeight * 0.16f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = palette.textColor, isBold = true, contentAngleDeg = contentAngle)
                            drawFittedSingleLineText(canvas, weather.tempRangeLabel, cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.16f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = contentAngle)
                        }
                    }
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
        mode: WidgetContentMode,
        is4x3: Boolean = false,
        weather: WeatherData
    ) {
        val p1w = if (is4x3) min(w * 0.42f, h * 0.48f) else min(w * 0.45f, h * 0.48f)
        val pillThickness = p1w * 0.52f
        val rightSubAngle = (-angle * 0.16f).coerceIn(-8f, 8f)

        val p1Rad = Math.toRadians(abs(angle).toDouble())
        val p1HalfV = ((p1w / 2f) * sin(p1Rad) + (pillThickness / 2f) * cos(p1Rad)).toFloat()

        val p2Rad = Math.toRadians(abs(rightSubAngle).toDouble())
        val p2HalfV = ((p1w / 2f) * sin(p2Rad) + (pillThickness / 2f) * cos(p2Rad)).toFloat()

        val btmTiltAngle = (angle * 0.08f).coerceIn(-5f, 5f)
        val btmTiltRad = Math.toRadians(abs(btmTiltAngle).toDouble())
        val btmCardH = if (is4x3) h * 0.33f else h * 0.33f
        val btmHalfV = ((w * 0.92f / 2f) * sin(btmTiltRad) + (btmCardH / 2f) * cos(btmTiltRad)).toFloat()

        // Reduced vertical gap between top pills and bottom card (tight, cohesive, zero clipping)
        val verticalGap = (h * 0.02f).coerceIn(4f, 8f)
        val totalGroupV = (p1HalfV * 2f) + verticalGap + (btmHalfV * 2f)
        val startY = ((h - totalGroupV) / 2f).coerceAtLeast(h * 0.025f)

        val p1cy = startY + p1HalfV
        // Stagger Pill 2 slightly so it doesn't float far above the bottom card
        val p2cy = p1cy + ((p1HalfV - p2HalfV) * 0.35f)
        val btmCy = p1cy + p1HalfV + verticalGap + btmHalfV

        val p1RightTipX = ((p1w - pillThickness) / 2f) * cos(p1Rad).toFloat() + (pillThickness / 2f)
        val p2LeftTipX = ((p1w - pillThickness) / 2f) * cos(p2Rad).toFloat() + (pillThickness / 2f)

        val topGap = (w * 0.02f).coerceIn(6f, 10f)
        val topTotalW = (p1w / 2f) + p1RightTipX + topGap + p2LeftTipX + (p1w / 2f)
        val topStartX = (w - topTotalW) / 2f

        val p1cx = topStartX + (p1w / 2f)
        val p2cx = p1cx + p1RightTipX + topGap + p2LeftTipX

        // Pill 1: Left tilted element (Temp & Icon or Clock)
        drawTiltedPill(canvas, p1cx, p1cy, p1w, pillThickness, angle, palette.bgColor)

        val rad = Math.toRadians(angle.toDouble())
        // Moderate lobe distance: 88% of cap center distance, prevents collision between text and icon
        val lobeDist = (p1w - pillThickness) / 2f
        val dist = lobeDist * 0.88f
        val l1x = (p1cx + dist * cos(rad)).toFloat()
        val l1y = (p1cy + dist * sin(rad)).toFloat()
        val l2x = (p1cx - dist * cos(rad)).toFloat()
        val l2y = (p1cy - dist * sin(rad)).toFloat()

        val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
            if (l1x <= l2x) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        } else {
            if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        }

        val p1ContentAngle = (angle * 0.35f).coerceIn(-18f, 18f)

        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(
                    canvas,
                    weather.currentTempLabel,
                    firstX,
                    firstY,
                    maxSizePx = pillThickness * 0.44f,
                    maxWidth = pillThickness * 0.72f,
                    textColor = palette.textColor,
                    isBold = true,
                    contentAngleDeg = p1ContentAngle
                )
                val iconSize = (pillThickness * 0.46f).toInt()
                drawDrawable(context, canvas, weather.currentIconResId, secondX.toInt(), secondY.toInt(), iconSize, iconSize, contentAngleDeg = p1ContentAngle)
            }
            WidgetContentMode.CLOCK -> {
                val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, hourStr, firstX, firstY, maxSizePx = pillThickness * 0.40f, maxWidth = pillThickness * 0.68f, textColor = palette.textColor, isBold = true, contentAngleDeg = p1ContentAngle)
                drawFittedSingleLineText(canvas, minStr, secondX, secondY, maxSizePx = pillThickness * 0.40f, maxWidth = pillThickness * 0.68f, textColor = palette.textColor, isBold = true, contentAngleDeg = p1ContentAngle)
            }
            WidgetContentMode.COMBO -> {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, timeStr, firstX, firstY, maxSizePx = pillThickness * 0.32f, maxWidth = pillThickness * 0.76f, textColor = palette.textColor, isBold = true, contentAngleDeg = p1ContentAngle)
                val iconSize = (pillThickness * 0.36f).toInt()
                drawComboWeatherLobe(context, canvas, weather.currentIconResId, weather.currentTempLabel, secondX, secondY, iconSize, pillThickness * 0.34f, palette.textColor, p1ContentAngle)
            }
        }

        // Pill 2: Right info pill with auto-wrapping condition text and subtle tilt matching container
        drawTiltedPill(canvas, p2cx, p2cy, p1w, pillThickness, rightSubAngle, palette.secondaryBgColor)

        val p2SafeW = p1w * 0.78f
        val p2ContentAngle = rightSubAngle * 0.75f

        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(canvas, weather.locationName, p2cx, p2cy - pillThickness * 0.22f, maxSizePx = pillThickness * 0.22f, maxWidth = p2SafeW, textColor = palette.textColor, isBold = true, contentAngleDeg = p2ContentAngle)
                drawStaticLayoutText(canvas, weather.conditionLabel, p2cx, p2cy + pillThickness * 0.04f, maxWidth = p2SafeW, maxHeight = pillThickness * 0.36f, maxLines = 2, initialTextSizePx = pillThickness * 0.16f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = p2ContentAngle)
                drawFittedSingleLineText(canvas, weather.tempRangeLabel, p2cx, p2cy + pillThickness * 0.25f, maxSizePx = pillThickness * 0.14f, maxWidth = p2SafeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor), contentAngleDeg = p2ContentAngle)
            }
            WidgetContentMode.CLOCK -> {
                val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                val dateStr = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, dayStr, p2cx, p2cy - pillThickness * 0.14f, maxSizePx = pillThickness * 0.20f, maxWidth = p2SafeW, textColor = palette.textColor, isBold = true, contentAngleDeg = p2ContentAngle)
                drawFittedSingleLineText(canvas, dateStr, p2cx, p2cy + pillThickness * 0.16f, maxSizePx = pillThickness * 0.17f, maxWidth = p2SafeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = p2ContentAngle)
            }
            WidgetContentMode.COMBO -> {
                val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                val tempAndCondition = "${weather.currentTempLabel} • ${weather.conditionLabel}"
                drawFittedSingleLineText(canvas, weather.locationName, p2cx, p2cy - pillThickness * 0.22f, maxSizePx = pillThickness * 0.22f, maxWidth = p2SafeW, textColor = palette.textColor, isBold = true, contentAngleDeg = p2ContentAngle)
                drawStaticLayoutText(canvas, tempAndCondition, p2cx, p2cy + pillThickness * 0.04f, maxWidth = p2SafeW, maxHeight = pillThickness * 0.36f, maxLines = 2, initialTextSizePx = pillThickness * 0.16f, textColor = (0xCCFFFFFF.toInt() and palette.textColor), contentAngleDeg = p2ContentAngle)
                drawFittedSingleLineText(canvas, dateStr, p2cx, p2cy + pillThickness * 0.25f, maxSizePx = pillThickness * 0.14f, maxWidth = p2SafeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor), contentAngleDeg = p2ContentAngle)
            }
        }

        // Bottom Card: Forecast or stats
        val btmW = w * 0.92f
        drawRoundedCard(canvas, w / 2f, btmCy, btmW, btmCardH, btmCardH * 0.38f, btmTiltAngle, palette.secondaryBgColor)

        val btmContentAngle = btmTiltAngle * 0.75f

        // Expanded 4x3 has 6 forecast slots, 3x3 has 4 forecast slots
        val forecastItems = if (is4x3) {
            weather.hourlyForecasts.take(6)
        } else {
            weather.hourlyForecasts.take(4)
        }

        val count = forecastItems.size
        val colWidth = if (count > 0) (btmW * 0.88f) / count.toFloat() else btmW * 0.22f
        val startX = (w / 2f) - (colWidth * (count - 1) / 2f)

        for (k in 0 until count) {
            val item = forecastItems[k]
            val itemX = startX + k * colWidth

            drawFittedSingleLineText(
                canvas,
                item.timeLabel,
                itemX,
                btmCy - btmCardH * 0.24f,
                maxSizePx = btmCardH * 0.20f,
                maxWidth = colWidth * 0.90f,
                textColor = (0xBBFFFFFF.toInt() and palette.textColor),
                contentAngleDeg = btmContentAngle
            )

            val miniIconSize = (btmCardH * 0.32f).toInt()
            drawDrawable(context, canvas, item.iconResId, itemX.toInt(), btmCy.toInt(), miniIconSize, miniIconSize, contentAngleDeg = btmContentAngle)

            drawFittedSingleLineText(
                canvas,
                item.tempLabel,
                itemX,
                btmCy + btmCardH * 0.26f,
                maxSizePx = btmCardH * 0.20f,
                maxWidth = colWidth * 0.90f,
                textColor = palette.textColor,
                isBold = true,
                contentAngleDeg = btmContentAngle
            )
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
        if (angleDeg != 0f) {
            canvas.rotate(angleDeg, cx, cy)
        }

        val left = cx - length / 2f
        val top = cy - thickness / 2f
        val right = cx + length / 2f
        val bottom = cy + thickness / 2f
        val radius = thickness / 2f

        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, radius, radius, paint)

        canvas.restore()
    }

    private fun drawRoundedCard(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        width: Float,
        height: Float,
        radius: Float,
        angleDeg: Float,
        color: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }

        canvas.save()
        if (angleDeg != 0f) {
            canvas.rotate(angleDeg, cx, cy)
        }

        val left = cx - width / 2f
        val top = cy - height / 2f
        val right = cx + width / 2f
        val bottom = cy + height / 2f

        val rect = RectF(left, top, right, bottom)
        canvas.drawRoundRect(rect, radius, radius, paint)

        canvas.restore()
    }

    /**
     * Draws multi-line wrapped text using StaticLayout with automatic font scaling down,
     * optional subtle content angle tilt, and truncation if needed.
     */
    private fun drawStaticLayoutText(
        canvas: Canvas,
        text: CharSequence,
        cx: Float,
        cy: Float,
        maxWidth: Float,
        maxHeight: Float,
        maxLines: Int = 2,
        initialTextSizePx: Float,
        minTextSizePx: Float = 10f,
        textColor: Int,
        isBold: Boolean = false,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER,
        contentAngleDeg: Float = 0f
    ) {
        if (maxWidth <= 0f || maxHeight <= 0f || text.isEmpty()) return

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = textColor
            typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        val widthInt = maxWidth.toInt().coerceAtLeast(1)
        var currentSize = initialTextSizePx
        var layout: StaticLayout

        while (true) {
            textPaint.textSize = currentSize
            val builder = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, widthInt)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1.05f)
                .setIncludePad(false)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
            layout = builder.build()

            if ((layout.lineCount <= maxLines && layout.height <= maxHeight) || currentSize <= minTextSizePx) {
                break
            }
            currentSize = (currentSize - 1.5f).coerceAtLeast(minTextSizePx)
            if (currentSize == minTextSizePx) {
                layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint.apply { textSize = minTextSizePx }, widthInt)
                    .setAlignment(alignment)
                    .setLineSpacing(0f, 1.05f)
                    .setIncludePad(false)
                    .setMaxLines(maxLines)
                    .setEllipsize(TextUtils.TruncateAt.END)
                    .build()
                break
            }
        }

        canvas.save()
        if (contentAngleDeg != 0f) {
            canvas.rotate(contentAngleDeg, cx, cy)
        }
        val startX = when (alignment) {
            Layout.Alignment.ALIGN_CENTER -> cx - (layout.width / 2f)
            Layout.Alignment.ALIGN_OPPOSITE -> cx - layout.width
            else -> cx - (maxWidth / 2f)
        }
        val startY = cy - (layout.height / 2f)

        canvas.translate(startX, startY)
        layout.draw(canvas)
        canvas.restore()
    }

    /**
     * Measures single line text and scales down font size if it exceeds maxWidth,
     * centering it precisely on (cx, cy), with optional subtle content angle tilt.
     */
    private fun drawFittedSingleLineText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        maxSizePx: Float,
        maxWidth: Float,
        textColor: Int,
        isBold: Boolean = false,
        contentAngleDeg: Float = 0f
    ) {
        if (text.isEmpty() || maxWidth <= 0f) return

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = textColor
            textAlign = Paint.Align.CENTER
            typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            textSize = maxSizePx
        }

        val measuredW = paint.measureText(text)
        if (measuredW > maxWidth && maxWidth > 0f) {
            paint.textSize = (paint.textSize * (maxWidth / measuredW)).coerceAtLeast(8f)
        }

        canvas.save()
        if (contentAngleDeg != 0f) {
            canvas.rotate(contentAngleDeg, cx, cy)
        }

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val textCenterY = cy - bounds.exactCenterY()

        canvas.drawText(text, cx, textCenterY, paint)
        canvas.restore()
    }

    private fun drawDrawable(
        context: Context,
        canvas: Canvas,
        drawableResId: Int,
        cx: Int,
        cy: Int,
        width: Int,
        height: Int,
        contentAngleDeg: Float = 0f
    ) {
        val drawable = ContextCompat.getDrawable(context, drawableResId) ?: return
        canvas.save()
        if (contentAngleDeg != 0f) {
            canvas.rotate(contentAngleDeg, cx.toFloat(), cy.toFloat())
        }
        val halfW = width / 2
        val halfH = height / 2
        drawable.setBounds(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
        drawable.draw(canvas)
        canvas.restore()
    }

    private fun drawComboWeatherLobe(
        context: Context,
        canvas: Canvas,
        iconRes: Int,
        tempStr: String,
        cx: Float,
        cy: Float,
        iconSize: Int,
        textSizePx: Float,
        textColor: Int,
        contentAngleDeg: Float
    ) {
        val drawable = ContextCompat.getDrawable(context, iconRes) ?: return
        canvas.save()
        canvas.translate(cx, cy)
        if (contentAngleDeg != 0f) {
            canvas.rotate(contentAngleDeg)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = textSizePx
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }

        val textBounds = Rect()
        paint.getTextBounds(tempStr, 0, tempStr.length, textBounds)
        val textWidth = paint.measureText(tempStr)
        val gap = iconSize * 0.15f

        val totalW = iconSize + gap + textWidth
        val startX = -totalW / 2f

        val iconLeft = startX.toInt()
        val iconTop = (-iconSize / 2f).toInt()
        drawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
        drawable.draw(canvas)

        val textX = startX + iconSize + gap
        val textY = -textBounds.exactCenterY()
        canvas.drawText(tempStr, textX, textY, paint)

        canvas.restore()
    }
}
