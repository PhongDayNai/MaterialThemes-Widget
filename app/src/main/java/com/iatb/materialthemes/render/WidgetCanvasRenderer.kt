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

        val rad = Math.toRadians(angle.toDouble())
        val dist = (pillLength - pillThickness) / 2f

        val lobe1X = (cx + dist * cos(rad)).toFloat()
        val lobe1Y = (cy + dist * sin(rad)).toFloat()
        val lobe2X = (cx - dist * cos(rad)).toFloat()
        val lobe2Y = (cy - dist * sin(rad)).toFloat()

        // When angle is near flat (< 5 deg), sort left-to-right; when tilted, sort top-to-bottom
        val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
            if (lobe1X <= lobe2X) listOf(lobe1X, lobe1Y, lobe2X, lobe2Y) else listOf(lobe2X, lobe2Y, lobe1X, lobe1Y)
        } else {
            if (lobe1Y <= lobe2Y) listOf(lobe1X, lobe1Y, lobe2X, lobe2Y) else listOf(lobe2X, lobe2Y, lobe1X, lobe1Y)
        }

        val lobeRadius = pillThickness / 2f

        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(
                    canvas,
                    context.getString(R.string.sample_temp_11),
                    firstX,
                    firstY,
                    maxSizePx = pillThickness * 0.46f,
                    maxWidth = lobeRadius * 1.6f,
                    textColor = palette.textColor,
                    isBold = false
                )
                val iconSize = (lobeRadius * 1.15f).toInt()
                drawDrawable(
                    context,
                    canvas,
                    R.drawable.ic_weather_night_cloudy,
                    secondX.toInt(),
                    secondY.toInt(),
                    iconSize,
                    iconSize
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
                    maxSizePx = pillThickness * 0.44f,
                    maxWidth = lobeRadius * 1.5f,
                    textColor = palette.textColor,
                    isBold = true
                )
                drawFittedSingleLineText(
                    canvas,
                    minStr,
                    secondX,
                    secondY,
                    maxSizePx = pillThickness * 0.44f,
                    maxWidth = lobeRadius * 1.5f,
                    textColor = palette.textColor,
                    isBold = true
                )
            }
            WidgetContentMode.COMBO -> {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(
                    canvas,
                    timeStr,
                    firstX,
                    firstY,
                    maxSizePx = pillThickness * 0.32f,
                    maxWidth = lobeRadius * 1.7f,
                    textColor = palette.textColor,
                    isBold = true
                )
                val iconSize = (lobeRadius * 1.05f).toInt()
                drawDrawable(
                    context,
                    canvas,
                    R.drawable.ic_weather_night_cloudy,
                    secondX.toInt(),
                    secondY.toInt(),
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
        val cy = h / 2f

        // Left tilted pill dimensions
        val leftW = if (isExtraWide) min(w * 0.32f, h * 0.84f) else min(w * 0.44f, h * 0.84f)
        val leftH = leftW * 0.54f

        // Right side container dimensions: sleek, horizontal pill/card
        val rightW = if (isExtraWide) w * 0.64f else w * 0.52f
        val rightH = h * 0.74f
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

        // Draw content in left tilted pill lobes
        val tiltRadLeft = Math.toRadians(angle.toDouble())
        val dist = (leftW - leftH) / 2f
        val l1x = (leftCx + dist * cos(tiltRadLeft)).toFloat()
        val l1y = (cy + dist * sin(tiltRadLeft)).toFloat()
        val l2x = (leftCx - dist * cos(tiltRadLeft)).toFloat()
        val l2y = (cy - dist * sin(tiltRadLeft)).toFloat()

        val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
            if (l1x <= l2x) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        } else {
            if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        }

        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(
                    canvas,
                    context.getString(R.string.sample_temp_11),
                    firstX,
                    firstY,
                    maxSizePx = leftH * 0.50f,
                    maxWidth = leftH * 0.78f,
                    textColor = palette.textColor
                )
                val iconSize = (leftH * 0.52f).toInt()
                drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, secondX.toInt(), secondY.toInt(), iconSize, iconSize)
            }
            WidgetContentMode.CLOCK -> {
                val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, hourStr, firstX, firstY, maxSizePx = leftH * 0.46f, maxWidth = leftH * 0.75f, textColor = palette.textColor, isBold = true)
                drawFittedSingleLineText(canvas, minStr, secondX, secondY, maxSizePx = leftH * 0.46f, maxWidth = leftH * 0.75f, textColor = palette.textColor, isBold = true)
            }
            WidgetContentMode.COMBO -> {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, timeStr, firstX, firstY, maxSizePx = leftH * 0.38f, maxWidth = leftH * 0.85f, textColor = palette.textColor, isBold = true)
                val iconSize = (leftH * 0.46f).toInt()
                drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, secondX.toInt(), secondY.toInt(), iconSize, iconSize)
            }
        }

        val cardRadius = rightH * 0.42f
        drawRoundedCard(canvas, rightCx, cy, rightW, rightH, cardRadius, rightAngle, palette.secondaryBgColor)

        // Safe usable area inside right container with generous vertical clearance from curved borders
        val tiltRad = Math.toRadians(abs(rightAngle).toDouble())
        val safeInnerW = (rightW * 0.82f - (rightH * sin(tiltRad)).toFloat()).coerceAtLeast(rightW * 0.65f)
        val safeInnerH = rightH * 0.70f

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
                        context.getString(R.string.sample_location),
                        subLeftCx,
                        cy - safeInnerH * 0.24f,
                        maxSizePx = rightH * 0.20f,
                        maxWidth = subColW,
                        textColor = palette.textColor,
                        isBold = true
                    )
                    drawStaticLayoutText(
                        canvas,
                        context.getString(R.string.sample_weather_condition),
                        subLeftCx,
                        cy + safeInnerH * 0.16f,
                        maxWidth = subColW,
                        maxHeight = safeInnerH * 0.45f,
                        maxLines = 2,
                        initialTextSizePx = rightH * 0.15f,
                        textColor = (0xCCFFFFFF.toInt() and palette.textColor),
                        isBold = false
                    )

                    // Right sub-column: Temp range & Wind/Humidity
                    drawFittedSingleLineText(
                        canvas,
                        context.getString(R.string.sample_temp_range),
                        subRightCx,
                        cy - safeInnerH * 0.20f,
                        maxSizePx = rightH * 0.16f,
                        maxWidth = subColW,
                        textColor = palette.textColor,
                        isBold = true
                    )
                    drawFittedSingleLineText(
                        canvas,
                        context.getString(R.string.sample_wind),
                        subRightCx,
                        cy + safeInnerH * 0.06f,
                        maxSizePx = rightH * 0.14f,
                        maxWidth = subColW,
                        textColor = (0xBBFFFFFF.toInt() and palette.textColor)
                    )
                    drawFittedSingleLineText(
                        canvas,
                        context.getString(R.string.sample_humidity),
                        subRightCx,
                        cy + safeInnerH * 0.28f,
                        maxSizePx = rightH * 0.14f,
                        maxWidth = subColW,
                        textColor = (0xBBFFFFFF.toInt() and palette.textColor)
                    )
                }
                WidgetContentMode.CLOCK -> {
                    val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())

                    drawFittedSingleLineText(canvas, dayStr, subLeftCx, cy - safeInnerH * 0.18f, maxSizePx = rightH * 0.22f, maxWidth = subColW, textColor = palette.textColor, isBold = true)
                    drawStaticLayoutText(canvas, dateStr, subLeftCx, cy + safeInnerH * 0.16f, maxWidth = subColW, maxHeight = safeInnerH * 0.40f, maxLines = 2, initialTextSizePx = rightH * 0.15f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))

                    drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), subRightCx, cy, maxSizePx = rightH * 0.16f, maxWidth = subColW, textColor = palette.textColor, isBold = true)
                }
                WidgetContentMode.COMBO -> {
                    val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                    drawFittedSingleLineText(canvas, context.getString(R.string.sample_location), subLeftCx, cy - safeInnerH * 0.22f, maxSizePx = rightH * 0.20f, maxWidth = subColW, textColor = palette.textColor, isBold = true)
                    drawStaticLayoutText(canvas, context.getString(R.string.sample_weather_condition), subLeftCx, cy + safeInnerH * 0.16f, maxWidth = subColW, maxHeight = safeInnerH * 0.45f, maxLines = 2, initialTextSizePx = rightH * 0.15f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))

                    drawFittedSingleLineText(canvas, dateStr, subRightCx, cy - safeInnerH * 0.12f, maxSizePx = rightH * 0.16f, maxWidth = subColW, textColor = palette.textColor, isBold = true)
                    drawFittedSingleLineText(canvas, context.getString(R.string.sample_temp_range), subRightCx, cy + safeInnerH * 0.18f, maxSizePx = rightH * 0.14f, maxWidth = subColW, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                }
            }
        } else {
            // 3x2: Single integrated column with auto-wrapping condition text and safe vertical clearance
            when (mode) {
                WidgetContentMode.WEATHER -> {
                    // Line 1: Location (e.g. "Hanoi")
                    drawFittedSingleLineText(
                        canvas,
                        context.getString(R.string.sample_location),
                        rightCx,
                        cy - safeInnerH * 0.26f,
                        maxSizePx = rightH * 0.15f,
                        maxWidth = safeInnerW,
                        textColor = palette.textColor,
                        isBold = true
                    )

                    // Line 2: Weather condition (e.g. "Partly Cloudy" -> wrapped to 2 lines if needed!)
                    drawStaticLayoutText(
                        canvas,
                        context.getString(R.string.sample_weather_condition),
                        rightCx,
                        cy + safeInnerH * 0.04f,
                        maxWidth = safeInnerW,
                        maxHeight = safeInnerH * 0.34f,
                        maxLines = 2,
                        initialTextSizePx = rightH * 0.14f,
                        textColor = (0xCCFFFFFF.toInt() and palette.textColor),
                        isBold = false,
                        alignment = Layout.Alignment.ALIGN_CENTER
                    )

                    // Line 3: Temp range (e.g. "H: 31° • L: 23°")
                    drawFittedSingleLineText(
                        canvas,
                        context.getString(R.string.sample_temp_range),
                        rightCx,
                        cy + safeInnerH * 0.30f,
                        maxSizePx = rightH * 0.12f,
                        maxWidth = safeInnerW,
                        textColor = (0xBBFFFFFF.toInt() and palette.textColor),
                        isBold = false
                    )
                }
                WidgetContentMode.CLOCK -> {
                    val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val dateStr = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(Date())

                    drawFittedSingleLineText(canvas, dayStr, rightCx, cy - safeInnerH * 0.26f, maxSizePx = rightH * 0.18f, maxWidth = safeInnerW, textColor = palette.textColor, isBold = true)
                    drawStaticLayoutText(canvas, dateStr, rightCx, cy + safeInnerH * 0.04f, maxWidth = safeInnerW, maxHeight = safeInnerH * 0.34f, maxLines = 2, initialTextSizePx = rightH * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                    drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), rightCx, cy + safeInnerH * 0.30f, maxSizePx = rightH * 0.12f, maxWidth = safeInnerW, textColor = (0xBBFFFFFF.toInt() and palette.textColor))
                }
                WidgetContentMode.COMBO -> {
                    val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                    drawFittedSingleLineText(canvas, context.getString(R.string.sample_location), rightCx, cy - safeInnerH * 0.26f, maxSizePx = rightH * 0.16f, maxWidth = safeInnerW, textColor = palette.textColor, isBold = true)
                    drawStaticLayoutText(canvas, context.getString(R.string.sample_weather_condition), rightCx, cy + safeInnerH * 0.04f, maxWidth = safeInnerW, maxHeight = safeInnerH * 0.34f, maxLines = 2, initialTextSizePx = rightH * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                    drawFittedSingleLineText(canvas, dateStr, rightCx, cy + safeInnerH * 0.30f, maxSizePx = rightH * 0.13f, maxWidth = safeInnerW, textColor = (0xBBFFFFFF.toInt() and palette.textColor))
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
        pillCount: Int
    ) {
        val tiltRad = Math.toRadians(abs(angle).toDouble().coerceAtMost(45.0))

        // Ensure pill size fits within canvas bounds both horizontally and vertically
        val pillHeight = (h / (pillCount + 0.35f)) * 0.68f
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
                    val dist = (maxAllowedW - pillHeight) / 2f
                    val l1x = (cx + dist * cos(rad)).toFloat()
                    val l1y = (cy + dist * sin(rad)).toFloat()
                    val l2x = (cx - dist * cos(rad)).toFloat()
                    val l2y = (cy - dist * sin(rad)).toFloat()

                    val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
                        if (l1x <= l2x) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
                    } else {
                        if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
                    }

                    when (mode) {
                        WidgetContentMode.WEATHER -> {
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_temp_11), firstX, firstY, maxSizePx = pillHeight * 0.48f, maxWidth = pillHeight * 0.85f, textColor = palette.textColor)
                            val iconSize = (pillHeight * 0.52f).toInt()
                            drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, secondX.toInt(), secondY.toInt(), iconSize, iconSize)
                        }
                        WidgetContentMode.CLOCK -> {
                            val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                            val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, hourStr, firstX, firstY, maxSizePx = pillHeight * 0.44f, maxWidth = pillHeight * 0.80f, textColor = palette.textColor, isBold = true)
                            drawFittedSingleLineText(canvas, minStr, secondX, secondY, maxSizePx = pillHeight * 0.44f, maxWidth = pillHeight * 0.80f, textColor = palette.textColor, isBold = true)
                        }
                        WidgetContentMode.COMBO -> {
                            val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, timeStr, firstX, firstY, maxSizePx = pillHeight * 0.36f, maxWidth = pillHeight * 0.88f, textColor = palette.textColor, isBold = true)
                            val iconSize = (pillHeight * 0.48f).toInt()
                            drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, secondX.toInt(), secondY.toInt(), iconSize, iconSize)
                        }
                    }
                }
                1 -> {
                    // Second pill: Subtle tilt with auto-wrapped text so condition text never pokes out!
                    val subAngle = (angle * 0.16f).coerceIn(-8f, 8f)
                    drawTiltedPill(canvas, cx, cy, maxAllowedW, pillHeight, subAngle, pillBgColor)

                    val safeW = maxAllowedW * 0.72f
                    when (mode) {
                        WidgetContentMode.WEATHER -> {
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_location), cx, cy - pillHeight * 0.22f, maxSizePx = pillHeight * 0.20f, maxWidth = safeW, textColor = palette.textColor, isBold = true)
                            drawStaticLayoutText(canvas, context.getString(R.string.sample_weather_condition), cx, cy + pillHeight * 0.04f, maxWidth = safeW, maxHeight = pillHeight * 0.36f, maxLines = 2, initialTextSizePx = pillHeight * 0.15f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_temp_range), cx, cy + pillHeight * 0.28f, maxSizePx = pillHeight * 0.13f, maxWidth = safeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor))
                        }
                        WidgetContentMode.CLOCK -> {
                            val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                            val dateStr = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, dayStr, cx, cy - pillHeight * 0.18f, maxSizePx = pillHeight * 0.22f, maxWidth = safeW, textColor = palette.textColor, isBold = true)
                            drawFittedSingleLineText(canvas, dateStr, cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                        }
                        WidgetContentMode.COMBO -> {
                            val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_location), cx, cy - pillHeight * 0.20f, maxSizePx = pillHeight * 0.20f, maxWidth = safeW, textColor = palette.textColor, isBold = true)
                            drawStaticLayoutText(canvas, context.getString(R.string.sample_weather_condition), cx, cy + pillHeight * 0.04f, maxWidth = safeW, maxHeight = pillHeight * 0.36f, maxLines = 2, initialTextSizePx = pillHeight * 0.15f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                            drawFittedSingleLineText(canvas, dateStr, cx, cy + pillHeight * 0.28f, maxSizePx = pillHeight * 0.13f, maxWidth = safeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor))
                        }
                    }
                }
                2 -> {
                    // Third pill for 2x4: Next alarm & stats
                    val subAngle = (angle * 0.10f).coerceIn(-5f, 5f)
                    drawTiltedPill(canvas, cx, cy, maxAllowedW, pillHeight, subAngle, pillBgColor)
                    val safeW = maxAllowedW * 0.72f

                    when (mode) {
                        WidgetContentMode.WEATHER -> {
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_humidity), cx, cy - pillHeight * 0.16f, maxSizePx = pillHeight * 0.20f, maxWidth = safeW, textColor = palette.textColor, isBold = true)
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_wind), cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                        }
                        WidgetContentMode.CLOCK -> {
                            val dateStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), cx, cy - pillHeight * 0.16f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = palette.textColor, isBold = true)
                            drawFittedSingleLineText(canvas, dateStr, cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                        }
                        WidgetContentMode.COMBO -> {
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_next_alarm), cx, cy - pillHeight * 0.16f, maxSizePx = pillHeight * 0.18f, maxWidth = safeW, textColor = palette.textColor, isBold = true)
                            drawFittedSingleLineText(canvas, context.getString(R.string.sample_temp_range), cx, cy + pillHeight * 0.18f, maxSizePx = pillHeight * 0.16f, maxWidth = safeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
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
        mode: WidgetContentMode
    ) {
        val topH = h * 0.48f
        val btmH = h * 0.44f

        val p1w = w * 0.44f
        val p1h = topH * 0.76f
        val rightSubAngle = (-angle * 0.16f).coerceIn(-8f, 8f)

        val p1Rad = Math.toRadians(abs(angle).toDouble())
        val p1HalfV = ((p1w / 2f) * sin(p1Rad) + (p1h * 0.65f / 2f) * cos(p1Rad)).toFloat()

        // Safe top margin ensuring zero top clipping
        val safeTopMargin = (h * 0.035f).coerceAtLeast(12f)
        val p1cy = p1HalfV + safeTopMargin
        val p2cy = p1cy

        val p1RightTipX = ((p1w - p1h * 0.65f) / 2f) * cos(p1Rad).toFloat() + (p1h * 0.65f / 2f)
        val p2Rad = Math.toRadians(abs(rightSubAngle).toDouble())
        val p2LeftTipX = ((p1w - p1h * 0.65f) / 2f) * cos(p2Rad).toFloat() + (p1h * 0.65f / 2f)

        val topGap = (w * 0.02f).coerceIn(6f, 12f)
        val topTotalW = (p1w / 2f) + p1RightTipX + topGap + p2LeftTipX + (p1w / 2f)
        val topStartX = (w - topTotalW) / 2f

        val p1cx = topStartX + (p1w / 2f)
        val p2cx = p1cx + p1RightTipX + topGap + p2LeftTipX

        // Pill 1: Left tilted element (Temp & Icon or Clock)
        drawTiltedPill(canvas, p1cx, p1cy, p1w, p1h * 0.65f, angle, palette.bgColor)

        val rad = Math.toRadians(angle.toDouble())
        val dist = (p1w - p1h * 0.65f) / 2f
        val l1x = (p1cx + dist * cos(rad)).toFloat()
        val l1y = (p1cy + dist * sin(rad)).toFloat()
        val l2x = (p1cx - dist * cos(rad)).toFloat()
        val l2y = (p1cy - dist * sin(rad)).toFloat()

        val (firstX, firstY, secondX, secondY) = if (abs(angle) < 5f) {
            if (l1x <= l2x) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        } else {
            if (l1y <= l2y) listOf(l1x, l1y, l2x, l2y) else listOf(l2x, l2y, l1x, l1y)
        }

        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(canvas, context.getString(R.string.sample_temp_11), firstX, firstY, maxSizePx = p1h * 0.36f, maxWidth = p1h * 0.50f, textColor = palette.textColor)
                val iconSize = (p1h * 0.38f).toInt()
                drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, secondX.toInt(), secondY.toInt(), iconSize, iconSize)
            }
            WidgetContentMode.CLOCK -> {
                val hourStr = SimpleDateFormat("HH", Locale.getDefault()).format(Date())
                val minStr = SimpleDateFormat("mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, hourStr, firstX, firstY, maxSizePx = p1h * 0.34f, maxWidth = p1h * 0.48f, textColor = palette.textColor, isBold = true)
                drawFittedSingleLineText(canvas, minStr, secondX, secondY, maxSizePx = p1h * 0.34f, maxWidth = p1h * 0.48f, textColor = palette.textColor, isBold = true)
            }
            WidgetContentMode.COMBO -> {
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, timeStr, firstX, firstY, maxSizePx = p1h * 0.28f, maxWidth = p1h * 0.55f, textColor = palette.textColor, isBold = true)
                val iconSize = (p1h * 0.36f).toInt()
                drawDrawable(context, canvas, R.drawable.ic_weather_night_cloudy, secondX.toInt(), secondY.toInt(), iconSize, iconSize)
            }
        }

        // Pill 2: Right info pill with auto-wrapping condition text so it never pokes out!
        drawTiltedPill(canvas, p2cx, p2cy, p1w, p1h * 0.65f, rightSubAngle, palette.secondaryBgColor)

        val p2SafeW = p1w * 0.76f
        when (mode) {
            WidgetContentMode.WEATHER -> {
                drawFittedSingleLineText(canvas, context.getString(R.string.sample_location), p2cx, p2cy - p1h * 0.16f, maxSizePx = p1h * 0.18f, maxWidth = p2SafeW, textColor = palette.textColor, isBold = true)
                drawStaticLayoutText(canvas, context.getString(R.string.sample_weather_condition), p2cx, p2cy + p1h * 0.04f, maxWidth = p2SafeW, maxHeight = p1h * 0.30f, maxLines = 2, initialTextSizePx = p1h * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                drawFittedSingleLineText(canvas, context.getString(R.string.sample_temp_range), p2cx, p2cy + p1h * 0.22f, maxSizePx = p1h * 0.12f, maxWidth = p2SafeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor))
            }
            WidgetContentMode.CLOCK -> {
                val dayStr = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                val dateStr = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, dayStr, p2cx, p2cy - p1h * 0.12f, maxSizePx = p1h * 0.18f, maxWidth = p2SafeW, textColor = palette.textColor, isBold = true)
                drawFittedSingleLineText(canvas, dateStr, p2cx, p2cy + p1h * 0.14f, maxSizePx = p1h * 0.15f, maxWidth = p2SafeW, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
            }
            WidgetContentMode.COMBO -> {
                val dateStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date())
                drawFittedSingleLineText(canvas, context.getString(R.string.sample_location), p2cx, p2cy - p1h * 0.16f, maxSizePx = p1h * 0.18f, maxWidth = p2SafeW, textColor = palette.textColor, isBold = true)
                drawStaticLayoutText(canvas, context.getString(R.string.sample_weather_condition), p2cx, p2cy + p1h * 0.04f, maxWidth = p2SafeW, maxHeight = p1h * 0.30f, maxLines = 2, initialTextSizePx = p1h * 0.14f, textColor = (0xCCFFFFFF.toInt() and palette.textColor))
                drawFittedSingleLineText(canvas, dateStr, p2cx, p2cy + p1h * 0.22f, maxSizePx = p1h * 0.12f, maxWidth = p2SafeW, textColor = (0xBBFFFFFF.toInt() and palette.textColor))
            }
        }

        // Bottom Card: Forecast or stats
        val btmCx = w / 2f
        val btmCy = h - btmH * 0.52f
        val btmW = w * 0.92f
        val btmTiltAngle = (angle * 0.08f).coerceIn(-5f, 5f)
        drawRoundedCard(canvas, btmCx, btmCy, btmW, btmH * 0.82f, btmH * 0.34f, btmTiltAngle, palette.secondaryBgColor)

        // 4 mini forecast items with precise column distribution
        val hours = listOf(R.string.sample_forecast_time_1, R.string.sample_forecast_time_2, R.string.sample_forecast_time_3, R.string.sample_forecast_time_4)
        val temps = listOf(R.string.sample_forecast_temp_1, R.string.sample_forecast_temp_2, R.string.sample_forecast_temp_3, R.string.sample_forecast_temp_4)
        val icons = listOf(R.drawable.ic_weather_sunny, R.drawable.ic_weather_sunny, R.drawable.ic_weather_partly_cloudy, R.drawable.ic_weather_rainy)

        val colWidth = (btmW * 0.88f) / 4f
        val startX = btmCx - (colWidth * 1.5f)

        for (k in 0..3) {
            val itemX = startX + k * colWidth

            drawFittedSingleLineText(
                canvas,
                context.getString(hours[k]),
                itemX,
                btmCy - btmH * 0.18f,
                maxSizePx = btmH * 0.16f,
                maxWidth = colWidth * 0.90f,
                textColor = (0xBBFFFFFF.toInt() and palette.textColor)
            )

            val miniIconSize = (btmH * 0.26f).toInt()
            drawDrawable(context, canvas, icons[k], itemX.toInt(), btmCy.toInt(), miniIconSize, miniIconSize)

            drawFittedSingleLineText(
                canvas,
                context.getString(temps[k]),
                itemX,
                btmCy + btmH * 0.28f,
                maxSizePx = btmH * 0.18f,
                maxWidth = colWidth * 0.90f,
                textColor = palette.textColor,
                isBold = true
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
     * Draws multi-line wrapped text using StaticLayout with automatic font scaling down
     * and truncation if needed, ensuring text NEVER bleeds outside the container.
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
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_CENTER
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
     * centering it precisely on (cx, cy).
     */
    private fun drawFittedSingleLineText(
        canvas: Canvas,
        text: String,
        cx: Float,
        cy: Float,
        maxSizePx: Float,
        maxWidth: Float,
        textColor: Int,
        isBold: Boolean = false
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
