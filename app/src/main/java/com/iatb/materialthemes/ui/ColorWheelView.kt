package com.iatb.materialthemes.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class ColorWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * resources.displayMetrics.density
        color = Color.WHITE
        setShadowLayer(4f * resources.displayMetrics.density, 0f, 2f, 0x66000000)
    }
    private val indicatorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var centerX = 0f
    private var centerY = 0f
    private var wheelRadius = 0f

    private var currentHue = 180f
    private var currentSat = 0.8f
    private var currentColor = Color.CYAN

    var onColorChanged: ((Int) -> Unit)? = null

    private val sweepColors = intArrayOf(
        Color.RED,
        Color.MAGENTA,
        Color.BLUE,
        Color.CYAN,
        Color.GREEN,
        Color.YELLOW,
        Color.RED
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        val padding = 16f * resources.displayMetrics.density
        wheelRadius = (min(w, h) / 2f) - padding

        if (wheelRadius > 0) {
            huePaint.shader = SweepGradient(centerX, centerY, sweepColors, null)
            satPaint.shader = RadialGradient(
                centerX,
                centerY,
                wheelRadius,
                Color.WHITE,
                0x00FFFFFF,
                Shader.TileMode.CLAMP
            )
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (wheelRadius <= 0) return

        // Draw hue circle
        canvas.drawCircle(centerX, centerY, wheelRadius, huePaint)
        // Draw saturation overlay (white in center fading to pure color at edge)
        canvas.drawCircle(centerX, centerY, wheelRadius, satPaint)

        // Calculate indicator position
        val angleRad = Math.toRadians(currentHue.toDouble())
        val dist = currentSat * wheelRadius
        val indX = (centerX + dist * cos(angleRad)).toFloat()
        val indY = (centerY + dist * sin(angleRad)).toFloat()

        val indicatorRadius = 14f * resources.displayMetrics.density
        indicatorFillPaint.color = currentColor
        canvas.drawCircle(indX, indY, indicatorRadius, indicatorFillPaint)
        canvas.drawCircle(indX, indY, indicatorRadius, indicatorPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                currentSat = (dist / wheelRadius).coerceIn(0.05f, 1f)

                var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (angleDeg < 0) angleDeg += 360f
                currentHue = angleDeg

                updateCurrentColor()
                invalidate()
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateCurrentColor() {
        currentColor = Color.HSVToColor(floatArrayOf(currentHue, currentSat, 0.95f))
        onColorChanged?.invoke(currentColor)
    }

    fun setColor(color: Int) {
        currentColor = color
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        currentHue = hsv[0]
        currentSat = hsv[1].coerceIn(0.05f, 1f)
        invalidate()
    }

    fun getColor(): Int = currentColor
}
