package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.min

/**
 * Animated biometric fingerprint view.
 * Features crisp, airy, well-spaced Material Design ridges that animate periodically (every 5 seconds)
 * and respond directly to user touch (expand on press down, collapse back on release).
 */
class AnimatedFingerprintView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private data class FingerprintRidge(
        val path: Path,
        val deltaX: Float,
        val deltaY: Float,
        val deltaRotation: Float = 0f,
        val deltaScale: Float = 1f
    )

    private val ridges = mutableListOf<FingerprintRidge>()

    private var currentDisplacementFraction = 0f
    private var animator: ValueAnimator? = null

    private val disperseInterpolator = DecelerateInterpolator(1.6f)
    private val returnInterpolator = OvershootInterpolator(2.4f)

    private val animRunnable = Runnable {
        triggerDisperseAnimation()
    }

    companion object {
        private const val ANIM_INTERVAL_MS = 5000L
        private const val ANIM_DURATION_MS = 1000L
    }

    init {
        val tv = TypedValue()
        val defaultColor = if (context.theme.resolveAttribute(androidx.appcompat.R.attr.colorPrimary, tv, true)) {
            tv.data
        } else {
            0xFF006874.toInt()
        }
        paint.color = defaultColor

        initFingerprintRidges()
    }

    fun setFingerprintColor(color: Int) {
        paint.color = color
        invalidate()
    }

    private fun initFingerprintRidges() {
        ridges.clear()

        // Ridge 0: Top arch ridge
        // M8,4.6C9.2,3.8 10.6,3.4 12,3.4C13.4,3.4 14.8,3.8 16,4.6
        val r0 = Path().apply {
            moveTo(8f, 4.6f)
            cubicTo(9.2f, 3.8f, 10.6f, 3.4f, 12f, 3.4f)
            cubicTo(13.4f, 3.4f, 14.8f, 3.8f, 16f, 4.6f)
        }
        ridges.add(FingerprintRidge(r0, deltaX = 0f, deltaY = -1.6f, deltaScale = 1.05f))

        // Ridge 1: Upper middle arch ridge
        // M4.8,9.5C6.8,7.5 9.3,6.4 12,6.4C14.7,6.4 17.2,7.5 19.2,9.5
        val r1 = Path().apply {
            moveTo(4.8f, 9.5f)
            cubicTo(6.8f, 7.5f, 9.3f, 6.4f, 12f, 6.4f)
            cubicTo(14.7f, 6.4f, 17.2f, 7.5f, 19.2f, 9.5f)
        }
        ridges.add(FingerprintRidge(r1, deltaX = 0f, deltaY = -1.0f, deltaScale = 1.05f))

        // Ridge 2: Left outer ridge
        // M3.4,13.5C3.4,12 3.8,10.6 4.6,9.4
        val r2 = Path().apply {
            moveTo(3.4f, 13.5f)
            cubicTo(3.4f, 12f, 3.8f, 10.6f, 4.6f, 9.4f)
        }
        ridges.add(FingerprintRidge(r2, deltaX = -1.5f, deltaY = 0.4f, deltaRotation = -4f))

        // Ridge 3: Main middle whorl ridge
        // M8,12.2C8,10 9.8,8.2 12,8.2C14.2,8.2 16,10 16,12.2C16,15.4 13.8,18 11.5,19.6C10.2,20.5 8.8,20.8 7.5,20.8
        val r3 = Path().apply {
            moveTo(8f, 12.2f)
            cubicTo(8f, 10f, 9.8f, 8.2f, 12f, 8.2f)
            cubicTo(14.2f, 8.2f, 16f, 10f, 16f, 12.2f)
            cubicTo(16f, 15.4f, 13.8f, 18f, 11.5f, 19.6f)
            cubicTo(10.2f, 20.5f, 8.8f, 20.8f, 7.5f, 20.8f)
        }
        ridges.add(FingerprintRidge(r3, deltaX = -0.3f, deltaY = 0.6f, deltaScale = 1.05f))

        // Ridge 4: Inner core ridge
        // M12,11.5C11.2,11.5 10.5,12.2 10.5,13.2C10.5,14.8 12,15.8 13.2,16.6C14.2,17.2 15,18.1 15,19.3
        val r4 = Path().apply {
            moveTo(12f, 11.5f)
            cubicTo(11.2f, 11.5f, 10.5f, 12.2f, 10.5f, 13.2f)
            cubicTo(10.5f, 14.8f, 12f, 15.8f, 13.2f, 16.6f)
            cubicTo(14.2f, 17.2f, 15f, 18.1f, 15f, 19.3f)
        }
        ridges.add(FingerprintRidge(r4, deltaX = 0.5f, deltaY = 0.6f, deltaScale = 0.96f))

        // Ridge 5: Right outer ridge
        // M20.6,13.5C20.6,15 20,16.6 19,18
        val r5 = Path().apply {
            moveTo(20.6f, 13.5f)
            cubicTo(20.6f, 15f, 20f, 16.6f, 19f, 18f)
        }
        ridges.add(FingerprintRidge(r5, deltaX = 1.5f, deltaY = 0.4f, deltaRotation = 4f))
    }

    /**
     * Expands ridges outward smoothly on user touch down.
     */
    fun expand() {
        animator?.cancel()
        removeCallbacks(animRunnable)

        animator = ValueAnimator.ofFloat(currentDisplacementFraction, 1.0f).apply {
            duration = 200L
            interpolator = DecelerateInterpolator(1.8f)
            addUpdateListener { anim ->
                currentDisplacementFraction = anim.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    /**
     * Collapses ridges back to exact home position with overshoot spring bounce on user touch up/cancel.
     */
    fun collapse() {
        animator?.cancel()
        removeCallbacks(animRunnable)

        animator = ValueAnimator.ofFloat(currentDisplacementFraction, 0f).apply {
            duration = 380L
            interpolator = OvershootInterpolator(2.2f)
            addUpdateListener { anim ->
                currentDisplacementFraction = anim.animatedValue as Float
                invalidate()
            }
            doOnEndListener {
                currentDisplacementFraction = 0f
                invalidate()
                if (isAttachedToWindow) {
                    postDelayed(animRunnable, ANIM_INTERVAL_MS)
                }
            }
            start()
        }
    }

    /**
     * Executes the automatic disperse and snap-back spring animation every 5 seconds.
     */
    fun triggerDisperseAnimation() {
        animator?.cancel()
        removeCallbacks(animRunnable)

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIM_DURATION_MS
            addUpdateListener { anim ->
                val progress = anim.animatedValue as Float
                currentDisplacementFraction = if (progress <= 0.36f) {
                    val p = progress / 0.36f
                    disperseInterpolator.getInterpolation(p)
                } else {
                    val p = (progress - 0.36f) / 0.64f
                    1f - returnInterpolator.getInterpolation(p)
                }
                invalidate()
            }
            doOnEndListener {
                currentDisplacementFraction = 0f
                invalidate()
                if (isAttachedToWindow) {
                    postDelayed(animRunnable, ANIM_INTERVAL_MS)
                }
            }
            start()
        }
    }

    private inline fun ValueAnimator.doOnEndListener(crossinline action: () -> Unit) {
        addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                action()
            }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        postDelayed(animRunnable, ANIM_INTERVAL_MS)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        removeCallbacks(animRunnable)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility != VISIBLE) {
            animator?.cancel()
            removeCallbacks(animRunnable)
            currentDisplacementFraction = 0f
            invalidate()
        } else if (isAttachedToWindow) {
            postDelayed(animRunnable, ANIM_INTERVAL_MS)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density
        // Base icon is ~22dp at rest, perfectly centered with buffer space so expanding ridges never clip.
        val baseIconSize = min(min(w, h) * 0.55f, 22f * density).coerceAtLeast(1f)
        val scale = baseIconSize / 24f

        val centerX = w / 2f
        val centerY = h / 2f

        canvas.save()
        canvas.translate(centerX, centerY)
        canvas.scale(scale, scale)

        // 1.8f stroke width in 24-unit space (will be scaled by canvas to exactly 1.8dp on screen)
        paint.strokeWidth = 1.8f

        for (ridge in ridges) {
            canvas.save()
            val dispX = ridge.deltaX * currentDisplacementFraction
            val dispY = ridge.deltaY * currentDisplacementFraction
            val rot = ridge.deltaRotation * currentDisplacementFraction
            val scl = 1f + (ridge.deltaScale - 1f) * currentDisplacementFraction

            canvas.translate(dispX, dispY)
            if (rot != 0f) {
                canvas.rotate(rot)
            }
            if (scl != 1f) {
                canvas.scale(scl, scl)
            }
            canvas.translate(-12f, -12f)

            canvas.drawPath(ridge.path, paint)
            canvas.restore()
        }
        canvas.restore()
    }
}
