package com.iatb.materialthemes.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.iatb.materialthemes.R
import kotlin.math.max

object DayProgressIndicatorRenderer {

    /**
     * Renders an elegant, organic curved horizontal wave progress bar with Sunrise and Sunset milestones,
     * daylight ambient zone, active celestial cursor (Sun or Moon), and milestone icon+time labels
     * positioned directly below the sunrise and sunset nodes on the curve.
     */
    fun drawCurvedHorizontalBitmap(
        context: Context? = null,
        widthPx: Int,
        heightPx: Int,
        progress: Float,
        sunriseProgress: Float,
        sunsetProgress: Float,
        sunriseTimeStr: String = "",
        sunsetTimeStr: String = "",
        isDaylight: Boolean = true,
        trackColor: Int = Color.WHITE,
        progressColor: Int = Color.parseColor("#FFA000"),
        strokeWidthPx: Float = -1f,
        progressStartColor: Int = Color.parseColor("#FFA000"),
        progressEndColor: Int = Color.parseColor("#FF6F00")
    ): Bitmap {
        val w = max(widthPx, 240)
        val h = max(heightPx, 40)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val density = context?.resources?.displayMetrics?.density ?: 2.5f
        val hasLabels = sunriseTimeStr.isNotEmpty() && sunsetTimeStr.isNotEmpty() && h >= (44 * density).toInt()

        // Refined, slim stroke width for high elegance
        val stroke = if (strokeWidthPx > 0f) strokeWidthPx else (h * 0.08f).coerceIn(5f, 10f)
        val padX = stroke * 1.8f + 8f
        val activeW = w - 2 * padX

        // If time labels are drawn below the nodes, place curve slightly higher in the canvas
        val centerY = if (hasLabels) h * 0.35f else h / 2f
        val waveAmplitude = if (hasLabels) (h * 0.22f).coerceIn(8f, 22f) else (h * 0.32f).coerceIn(10f, 36f)
        val baseY = centerY + waveAmplitude * 0.50f
        val peakY = centerY - waveAmplitude * 0.85f

        // 1. Build smooth diurnal solar arc across the 24h day (cresting at solar noon)
        val fullPath = Path().apply {
            moveTo(padX, baseY)
            // Left half: rises smoothly from midnight base through morning sunrise towards noon zenith
            cubicTo(
                padX + activeW * 0.18f, baseY,
                padX + activeW * 0.34f, peakY,
                padX + activeW * 0.50f, peakY
            )
            // Right half: descends smoothly from noon zenith through evening sunset down to midnight base
            cubicTo(
                padX + activeW * 0.66f, peakY,
                padX + activeW * 0.82f, baseY,
                padX + activeW, baseY
            )
        }

        val pathMeasure = PathMeasure(fullPath, false)
        val totalLength = pathMeasure.length

        // 2. Draw 24-hour Base Track (Background)
        val baseTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = ColorUtils.setAlphaComponent(trackColor, 40)
        }
        canvas.drawPath(fullPath, baseTrackPaint)

        // 3. Draw Daylight Zone Segment (Between Sunrise & Sunset)
        val clampedSunrise = sunriseProgress.coerceIn(0.05f, 0.90f)
        val clampedSunset = sunsetProgress.coerceIn(clampedSunrise + 0.05f, 0.95f)

        val daylightStartLen = totalLength * clampedSunrise
        val daylightEndLen = totalLength * clampedSunset
        val daylightPath = Path()
        pathMeasure.getSegment(daylightStartLen, daylightEndLen, daylightPath, true)

        val daylightTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            this.strokeWidth = stroke * 1.05f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = ColorUtils.setAlphaComponent(Color.parseColor("#FFA726"), 60)
        }
        canvas.drawPath(daylightPath, daylightTrackPaint)

        // 4. Draw Active Progress Curve with Seasonal Gradient
        val clampedProgress = progress.coerceIn(0f, 1f)
        if (clampedProgress > 0.001f) {
            val progressLength = totalLength * clampedProgress
            val progressPath = Path()
            pathMeasure.getSegment(0f, progressLength, progressPath, true)

            val activeGrad = LinearGradient(
                padX, peakY, padX + activeW * clampedProgress, baseY,
                intArrayOf(progressStartColor, progressEndColor),
                null,
                Shader.TileMode.CLAMP
            )

            val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                this.strokeWidth = stroke * 1.05f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                shader = activeGrad
            }
            canvas.drawPath(progressPath, progressPaint)
        }

        val pos = FloatArray(2)
        val tan = FloatArray(2)

        // Setup text paint for milestone times
        val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f * density
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        }

        // 5. Sunrise Milestone Node and Label (right below node position pos[0])
        if (pathMeasure.getPosTan(daylightStartLen, pos, tan)) {
            val sunriseColor = Color.parseColor("#FFA726")

            // Subtle vertical tick marker
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sunriseColor
                strokeWidth = stroke * 0.40f
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
            }
            val tickH = stroke * 1.0f
            canvas.drawLine(pos[0], pos[1] - tickH, pos[0], pos[1] + tickH, tickPaint)

            // Sunrise Dot Node
            val sunriseDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFF3E0")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(pos[0], pos[1], stroke * 0.50f, sunriseDotPaint)

            // Label directly below the node: only icon and time
            if (hasLabels && context != null) {
                val iconSize = (11f * density).toInt()
                val spacing = 3f * density
                timePaint.color = Color.parseColor("#FFB74D")
                val textW = timePaint.measureText(sunriseTimeStr)
                val totalW = iconSize + spacing + textW
                val labelLeft = (pos[0] - totalW / 2f).coerceIn(6f, w - totalW - 6f)
                val labelTop = pos[1] + stroke * 0.6f + 5f * density

                // Subtle guide stem line connecting node to label
                val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ColorUtils.setAlphaComponent(sunriseColor, 75)
                    strokeWidth = 1f * density
                }
                canvas.drawLine(pos[0], pos[1] + stroke * 0.6f, pos[0], labelTop - 2f * density, stemPaint)

                // Draw Sunrise Vector Icon
                val sunriseDrawable = ContextCompat.getDrawable(context, R.drawable.ic_sunrise)
                sunriseDrawable?.let { d ->
                    d.setBounds(labelLeft.toInt(), labelTop.toInt(), (labelLeft + iconSize).toInt(), (labelTop + iconSize).toInt())
                    d.draw(canvas)
                }

                // Draw Time Text (e.g. "05:35")
                val textX = labelLeft + iconSize + spacing
                val textY = labelTop + iconSize * 0.85f
                canvas.drawText(sunriseTimeStr, textX, textY, timePaint)
            }
        }

        // 6. Sunset Milestone Node and Label (right below node position pos[0])
        if (pathMeasure.getPosTan(daylightEndLen, pos, tan)) {
            val sunsetColor = Color.parseColor("#FF7043")

            // Subtle vertical tick marker
            val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = sunsetColor
                strokeWidth = stroke * 0.40f
                strokeCap = Paint.Cap.ROUND
                style = Paint.Style.STROKE
            }
            val tickH = stroke * 1.0f
            canvas.drawLine(pos[0], pos[1] - tickH, pos[0], pos[1] + tickH, tickPaint)

            // Sunset Dot Node
            val sunsetDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FBE9E7")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(pos[0], pos[1], stroke * 0.50f, sunsetDotPaint)

            // Label directly below the node: only icon and time
            if (hasLabels && context != null) {
                val iconSize = (11f * density).toInt()
                val spacing = 3f * density
                timePaint.color = Color.parseColor("#FF8A65")
                val textW = timePaint.measureText(sunsetTimeStr)
                val totalW = iconSize + spacing + textW
                val labelLeft = (pos[0] - totalW / 2f).coerceIn(6f, w - totalW - 6f)
                val labelTop = pos[1] + stroke * 0.6f + 5f * density

                // Subtle guide stem line connecting node to label
                val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ColorUtils.setAlphaComponent(sunsetColor, 75)
                    strokeWidth = 1f * density
                }
                canvas.drawLine(pos[0], pos[1] + stroke * 0.6f, pos[0], labelTop - 2f * density, stemPaint)

                // Draw Sunset Vector Icon
                val sunsetDrawable = ContextCompat.getDrawable(context, R.drawable.ic_sunset)
                sunsetDrawable?.let { d ->
                    d.setBounds(labelLeft.toInt(), labelTop.toInt(), (labelLeft + iconSize).toInt(), (labelTop + iconSize).toInt())
                    d.draw(canvas)
                }

                // Draw Time Text (e.g. "17:50")
                val textX = labelLeft + iconSize + spacing
                val textY = labelTop + iconSize * 0.85f
                canvas.drawText(sunsetTimeStr, textX, textY, timePaint)
            }
        }

        // 7. Active Celestial Cursor (Glowing Sun disc or Moonlight disc)
        val curProgressLen = totalLength * clampedProgress
        if (pathMeasure.getPosTan(curProgressLen, pos, tan)) {
            val cx = pos[0]
            val cy = pos[1]

            if (isDaylight) {
                // Soft Solar Halo
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#4DFFB74D")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, stroke * 1.8f, haloPaint)

                // Sun Core Disc
                val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#FFA000")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, stroke * 1.05f, corePaint)

                // Center Specular Glow
                val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, stroke * 0.45f, specPaint)
            } else {
                // Lunar Halo
                val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#3880D8FF")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, stroke * 1.8f, haloPaint)

                // Moon Disc
                val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#80D8FF")
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, stroke * 1.0f, corePaint)

                // Specular Center
                val specPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }
                canvas.drawCircle(cx, cy, stroke * 0.42f, specPaint)
            }
        }

        return bitmap
    }
}
