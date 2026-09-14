package com.iatb.materialthemes.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

object DayProgressIndicatorRenderer {

    /**
     * Renders a flowing, organic curved horizontal progress bar into a Bitmap.
     * Conforms to Spec 5: "Không sử dụng progress bar dạng đường thẳng truyền thống.
     * Progress được thể hiện bằng một đường cong chạy theo chiều ngang."
     */
    fun drawCurvedHorizontalBitmap(
        widthPx: Int,
        heightPx: Int,
        progress: Float,
        trackColor: Int,
        progressColor: Int,
        strokeWidthPx: Float = -1f
    ): Bitmap {
        val w = max(widthPx, 100)
        val h = max(heightPx, 30)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val stroke = if (strokeWidthPx > 0f) strokeWidthPx else (h * 0.16f).coerceIn(6f, 18f)
        val padX = stroke * 1.4f
        val padY = stroke * 1.4f

        val activeW = w - 2 * padX
        val activeH = h - 2 * padY
        val centerY = h / 2f

        // Construct harmonic organic wave path
        val fullPath = Path().apply {
            moveTo(padX, centerY)
            // Left gentle crest
            cubicTo(
                padX + activeW * 0.14f, centerY - activeH * 0.44f,
                padX + activeW * 0.36f, centerY - activeH * 0.44f,
                padX + activeW * 0.50f, centerY
            )
            // Right gentle trough
            cubicTo(
                padX + activeW * 0.64f, centerY + activeH * 0.44f,
                padX + activeW * 0.86f, centerY + activeH * 0.44f,
                padX + activeW, centerY
            )
        }

        // Track Paint
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = trackColor
        }
        canvas.drawPath(fullPath, trackPaint)

        // Active Segment Paint
        val clampedProgress = progress.coerceIn(0f, 1f)
        if (clampedProgress > 0.001f) {
            val pathMeasure = PathMeasure(fullPath, false)
            val totalLength = pathMeasure.length
            val progressLength = totalLength * clampedProgress

            val progressPath = Path()
            pathMeasure.getSegment(0f, progressLength, progressPath, true)

            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                color = progressColor
            }
            canvas.drawPath(progressPath, progressPaint)

            // Accent knob / bead at current head
            val pos = FloatArray(2)
            val tan = FloatArray(2)
            if (pathMeasure.getPosTan(progressLength, pos, tan)) {
                val knobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = progressColor
                }
                canvas.drawCircle(pos[0], pos[1], stroke * 0.75f, knobPaint)

                val innerKnobPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
                canvas.drawCircle(pos[0], pos[1], stroke * 0.38f, innerKnobPaint)
            }
        }

        return bitmap
    }

    /**
     * Renders a circular or arc progress indicator into a Bitmap for square/compact layouts.
     * Conforms to Spec 5: "Circular: Dành cho widget có tỷ lệ gần vuông... Arc: Một cung tròn biểu diễn phần thời gian đã trôi qua."
     */
    fun drawCircularArcBitmap(
        sizePx: Int,
        progress: Float,
        trackColor: Int,
        progressColor: Int,
        strokeWidthPx: Float = -1f,
        isFullCircle: Boolean = false
    ): Bitmap {
        val s = max(sizePx, 80)
        val bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val stroke = if (strokeWidthPx > 0f) strokeWidthPx else (s * 0.09f).coerceIn(6f, 20f)
        val pad = stroke / 2f + 4f
        val rect = RectF(pad, pad, s - pad, s - pad)

        val startAngle = if (isFullCircle) -90f else 135f
        val sweepMax = if (isFullCircle) 360f else 270f

        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            color = trackColor
        }
        canvas.drawArc(rect, startAngle, sweepMax, false, trackPaint)

        val clampedProgress = progress.coerceIn(0f, 1f)
        if (clampedProgress > 0.001f) {
            val activeSweep = sweepMax * clampedProgress
            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                color = progressColor
            }
            canvas.drawArc(rect, startAngle, activeSweep, false, progressPaint)
        }

        return bitmap
    }
}
