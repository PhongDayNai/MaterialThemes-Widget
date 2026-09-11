package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.sin

/**
 * Circular Arc Progress View that is open at the bottom (270-degree sweep from 135° to 405°).
 * Features authentic optical neon Gaussian bloom glow, an illuminated celestial sparkle star at the center,
 * and a radiant continuous-decay optical flare at the progress tip.
 */
class CircularArcProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    private val startAngle = 135f
    private val totalSweepAngle = 270f

    private var strokeWidthPx = 7f * density
    private val arcBounds = RectF()

    // 1. Subtle background track
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#20FFFFFF")
    }

    // 2. Wide atmospheric bloom (Gaussian diffusion - broad luminous halo along full arc)
    private val outerBloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // 3. Concentrated corona bloom (Medium Gaussian falloff along full arc)
    private val coronaBloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // 4. Intense near-field core bloom along full arc
    private val coreBloomPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    // 5. Main vibrant cyan/blue neon tube
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#38BDF8")
    }

    // 6. White-hot core inside the progress arc for incandescent filament emission
    private val progressCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#E6FFFFFF")
    }

    // 7. Center celestial star paints
    private val starAuraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val starFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val starRayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = Color.parseColor("#D9FFFFFF")
    }

    // 8. Optical lens flare paint at progress tip
    private val tipGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val starPath = Path()

    private var progressFraction = 0f
    private var progressAnimator: ValueAnimator? = null

    // Breathing glow animation (650ms oscillating pulse)
    private var pulseAnimator: ValueAnimator? = null
    private var pulsePhase = 0f // 0f..1f

    init {
        // Enable software layer for accurate Gaussian BlurMaskFilter and shadow layers
        setLayerType(LAYER_TYPE_SOFTWARE, null)

        trackPaint.strokeWidth = strokeWidthPx
        progressPaint.strokeWidth = strokeWidthPx
        progressCorePaint.strokeWidth = 2.4f * density
        starRayPaint.strokeWidth = 1.6f * density

        // Tier 1: Wide Gaussian atmospheric halo along the entire progress arc
        outerBloomPaint.strokeWidth = strokeWidthPx * 2.2f
        outerBloomPaint.maskFilter = BlurMaskFilter(18f * density, BlurMaskFilter.Blur.NORMAL)

        // Tier 2: Focused corona bloom along the entire progress arc
        coronaBloomPaint.strokeWidth = strokeWidthPx * 1.4f
        coronaBloomPaint.maskFilter = BlurMaskFilter(8.5f * density, BlurMaskFilter.Blur.NORMAL)

        // Tier 3: Intense near-field core bloom along the entire progress arc
        coreBloomPaint.strokeWidth = strokeWidthPx * 1.1f
        coreBloomPaint.maskFilter = BlurMaskFilter(3.5f * density, BlurMaskFilter.Blur.NORMAL)

        // Slight micro-softness on neon tube for seamless optical bleed
        progressPaint.maskFilter = BlurMaskFilter(1.2f * density, BlurMaskFilter.Blur.NORMAL)
        progressCorePaint.maskFilter = BlurMaskFilter(0.8f * density, BlurMaskFilter.Blur.NORMAL)

        starFillPaint.setShadowLayer(
            14f * density,
            0f,
            0f,
            Color.parseColor("#38BDF8")
        )

        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                pulsePhase = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val pad = strokeWidthPx + (22f * density)
        arcBounds.set(pad, pad, w - pad, h - pad)

        // Gradient for progress from Radiant Cyan to Sky Blue
        val shader = LinearGradient(
            arcBounds.left, arcBounds.top,
            arcBounds.right, arcBounds.bottom,
            Color.parseColor("#22D3EE"), // Vibrant Cyan
            Color.parseColor("#38BDF8"), // Electric Sky Blue
            Shader.TileMode.CLAMP
        )
        progressPaint.shader = shader
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (arcBounds.width() <= 0 || arcBounds.height() <= 0) return

        val cx = arcBounds.centerX()
        val cy = arcBounds.centerY()

        // 1. Draw track arc (open at bottom: 135° -> 405°)
        canvas.drawArc(arcBounds, startAngle, totalSweepAngle, false, trackPaint)

        // 2. Draw optical glowing progress arc with radiant multi-stage Gaussian bloom along full arc
        if (progressFraction > 0f) {
            val sweep = totalSweepAngle * progressFraction

            // Tier 1: Broad atmospheric bloom across full arc (Gaussian falloff, no hard edges)
            val outerAlpha = (95 + (45 * pulsePhase)).toInt() // 95..140
            outerBloomPaint.color = Color.argb(outerAlpha, 56, 189, 248)
            canvas.drawArc(arcBounds, startAngle, sweep, false, outerBloomPaint)

            // Tier 2: Focused corona bloom across full arc (Rich vibrant electric cyan)
            val coronaAlpha = (150 + (55 * pulsePhase)).toInt() // 150..205
            coronaBloomPaint.color = Color.argb(coronaAlpha, 34, 211, 238)
            canvas.drawArc(arcBounds, startAngle, sweep, false, coronaBloomPaint)

            // Tier 3: Intense near-field core bloom across full arc
            val coreAlpha = (200 + (50 * pulsePhase)).toInt() // 200..250
            coreBloomPaint.color = Color.argb(coreAlpha, 56, 189, 248)
            canvas.drawArc(arcBounds, startAngle, sweep, false, coreBloomPaint)

            // Main neon tube
            canvas.drawArc(arcBounds, startAngle, sweep, false, progressPaint)

            // Hot core filament in the center of stroke
            val filamentSweep = (sweep - 1.2f).coerceAtLeast(0.1f)
            val filamentAlpha = (220 + (35 * pulsePhase)).toInt()
            progressCorePaint.color = Color.argb(filamentAlpha, 255, 255, 255)
            canvas.drawArc(arcBounds, startAngle + 0.6f, filamentSweep, false, progressCorePaint)

            // Proportional optical flare at progress tip (continuous decay, harmonious with radiant arc)
            drawCometTip(canvas, startAngle + sweep)
        }

        // 3. Draw celestial sparkle star in center (glowing, breathing, no percent text)
        drawCenterSparkleStar(canvas, cx, cy)
    }

    /**
     * Draws an illuminated multi-layered celestial diamond star in the center with breathing aura glow.
     */
    private fun drawCenterSparkleStar(canvas: Canvas, cx: Float, cy: Float) {
        // 1. Radial breathing aura glow behind star
        val auraRadius = (28f + 8f * pulsePhase) * density
        val auraShader = RadialGradient(
            cx, cy, auraRadius,
            intArrayOf(
                Color.argb((120 + 70 * pulsePhase).toInt(), 56, 189, 248),
                Color.argb((40 + 30 * pulsePhase).toInt(), 34, 211, 238),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )
        starAuraPaint.shader = auraShader
        canvas.drawCircle(cx, cy, auraRadius, starAuraPaint)

        // 2. Build 4-point astroid diamond star with curved concave edges
        val rMain = (16f + 2.5f * pulsePhase) * density
        val rWaist = (3.8f + 0.5f * pulsePhase) * density

        starPath.reset()
        // Top vertex
        starPath.moveTo(cx, cy - rMain)
        // Curve to Right
        starPath.quadTo(cx + rWaist, cy - rWaist, cx + rMain, cy)
        // Curve to Bottom
        starPath.quadTo(cx + rWaist, cy + rWaist, cx, cy + rMain)
        // Curve to Left
        starPath.quadTo(cx - rWaist, cy + rWaist, cx - rMain, cy)
        // Curve back to Top
        starPath.quadTo(cx - rWaist, cy - rWaist, cx, cy - rMain)
        starPath.close()

        canvas.drawPath(starPath, starFillPaint)

        // 3. Four diagonal sparkle rays at 45°
        val rayLen = (7f + 2f * pulsePhase) * density
        val rayDist = (5.5f + 1.5f * pulsePhase) * density
        val rayAlpha = (130 + 80 * pulsePhase).toInt()
        starRayPaint.alpha = rayAlpha

        canvas.drawLine(cx + rayDist, cy + rayDist, cx + rayDist + rayLen, cy + rayDist + rayLen, starRayPaint)
        canvas.drawLine(cx - rayDist, cy - rayDist, cx - rayDist - rayLen, cy - rayDist - rayLen, starRayPaint)
        canvas.drawLine(cx + rayDist, cy - rayDist, cx + rayDist + rayLen, cy - rayDist - rayLen, starRayPaint)
        canvas.drawLine(cx - rayDist, cy + rayDist, cx - rayDist - rayLen, cy + rayDist + rayLen, starRayPaint)

        // 4. White-hot center diamond sparkle
        val centerCoreRadius = (2.8f + 0.6f * pulsePhase) * density
        canvas.drawCircle(cx, cy, centerCoreRadius, starFillPaint)
    }

    /**
     * Draws an optical lens flare at the leading progress tip with continuous decay.
     * Has zero hard circular boundaries.
     */
    private fun drawCometTip(canvas: Canvas, angleDeg: Float) {
        val rad = Math.toRadians(angleDeg.toDouble())
        val radius = arcBounds.width() / 2f
        val tx = arcBounds.centerX() + (radius * cos(rad)).toFloat()
        val ty = arcBounds.centerY() + (radius * sin(rad)).toFloat()

        // Optical lens flare with continuous decay (white center -> cyan -> transparent, NO hard circular border)
        val flareRadius = (12f + 3f * pulsePhase) * density
        val flareShader = RadialGradient(
            tx, ty, flareRadius,
            intArrayOf(
                Color.argb((220 + 35 * pulsePhase).toInt(), 255, 255, 255), // Radiant hot center
                Color.argb((140 + 45 * pulsePhase).toInt(), 34, 211, 238),  // Luminous cyan
                Color.argb((45 + 25 * pulsePhase).toInt(), 56, 189, 248),   // Soft sky blue halo
                Color.TRANSPARENT                                           // Absolute fade
            ),
            floatArrayOf(0f, 0.22f, 0.58f, 1f),
            Shader.TileMode.CLAMP
        )
        tipGlowPaint.shader = flareShader
        canvas.drawCircle(tx, ty, flareRadius, tipGlowPaint)
    }

    fun setProgress(progress: Float) {
        progressFraction = progress.coerceIn(0f, 1f)
        invalidate()
    }

    fun animateProgress(
        from: Float = 0f,
        to: Float = 1f,
        durationMs: Long = 1800L,
        onUpdate: ((Float) -> Unit)? = null,
        onEnd: (() -> Unit)? = null
    ) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(from, to).apply {
            duration = durationMs
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Float
                progressFraction = value
                invalidate()
                onUpdate?.invoke(value)
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    onEnd?.invoke()
                }
            })
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (pulseAnimator?.isRunning != true) {
            startPulseAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        progressAnimator?.cancel()
        pulseAnimator?.cancel()
    }
}
