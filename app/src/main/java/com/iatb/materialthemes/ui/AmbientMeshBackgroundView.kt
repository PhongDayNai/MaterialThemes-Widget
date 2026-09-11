package com.iatb.materialthemes.ui

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom ambient background view that draws random, small glowing color orbs
 * on a deep dark background.
 *
 * Requirements:
 * - Base background is always DARK.
 * - Glowing elements are SMALL orbs/blobs (not massive screen-covering washes).
 * - Count, position, and colors are completely RANDOMIZED on each screen/Activity creation.
 * - Supports smooth floating & breathing ambient animations.
 * - Supports smooth animated transition when regenerating background constellation on resume.
 */
class AmbientMeshBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class OrbMotionMode {
        CLOCKWISE,
        COUNTER_CLOCKWISE,
        LINEAR,
        RANDOM_OPEN_CURVE,
        FIGURE_EIGHT
    }

    data class GlowOrb(
        var baseRelX: Float,
        var baseRelY: Float,
        var relX: Float,
        var relY: Float,
        var baseRadiusPx: Float,
        var radiusPx: Float,
        var color: Int,
        var ampX: Float = 0f,
        var ampY: Float = 0f,
        var freqX: Float = 0f,
        var freqY: Float = 0f,
        var phase: Float = 0f,
        var motionMode: OrbMotionMode = OrbMotionMode.CLOCKWISE,
        var linearAngle: Float = 0f,
        var curveRatio1: Float = 2.414f,
        var curveRatio2: Float = 3.1415f
    )

    private val orbs = mutableListOf<GlowOrb>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }

    private var ambientAnimator: ValueAnimator? = null
    private var transitionAnimator: ValueAnimator? = null
    private val argbEvaluator = ArgbEvaluator()

    // Deep dark background color
    private val baseDarkColor = Color.parseColor("#090C10")

    // Vibrant neon/pastel glowing color palette for random selection
    private val vibrantPalette = intArrayOf(
        Color.parseColor("#38BDF8"), // Electric Sky Blue
        Color.parseColor("#818CF8"), // Soft Indigo
        Color.parseColor("#C084FC"), // Vibrant Purple
        Color.parseColor("#F472B6"), // Radiant Pink / Rose
        Color.parseColor("#FB7185"), // Coral Rose
        Color.parseColor("#FBBF24"), // Warm Amber
        Color.parseColor("#34D399"), // Mint / Emerald
        Color.parseColor("#2DD4BF"), // Electric Teal
        Color.parseColor("#60A5FA"), // Deep Sky
        Color.parseColor("#A78BFA"), // Lavender Neon
        Color.parseColor("#F87171")  // Soft Crimson
    )

    init {
        orbs.addAll(generateRandomOrbs())
    }

    private fun generateRandomOrbs(): List<GlowOrb> {
        val rng = Random.Default
        val screenWidthPx = resources.displayMetrics.widthPixels.toFloat()

        // 1. Random count: 1 to 4 points
        val orbCount = rng.nextInt(1, 5)
        val shuffledColors = vibrantPalette.toList().shuffled(rng)
        val result = mutableListOf<GlowOrb>()

        // 2. Sizing: 30-50% for 3+ points, 35-65% for 1-2 points
        val (minRadiusPx, maxRadiusPx) = if (orbCount >= 3) {
            (screenWidthPx * 0.30f) to (screenWidthPx * 0.50f)
        } else {
            (screenWidthPx * 0.35f) to (screenWidthPx * 0.65f)
        }

        for (i in 0 until orbCount) {
            var rx: Float
            var ry: Float
            var attempts = 0

            // Ensure points are well-spaced across the canvas
            do {
                rx = when (i) {
                    0 -> -0.05f + rng.nextFloat() * 1.10f
                    1 -> if (rng.nextBoolean()) -0.05f + rng.nextFloat() * 0.45f else 0.55f + rng.nextFloat() * 0.50f
                    else -> -0.05f + rng.nextFloat() * 1.10f
                }

                ry = when (i) {
                    0 -> 0.45f + rng.nextFloat() * 0.50f // Lower area (behind action buttons)
                    1 -> 0.05f + rng.nextFloat() * 0.40f // Upper area (behind preview)
                    else -> -0.05f + rng.nextFloat() * 1.10f
                }
                attempts++
                val tooClose = result.any { existing ->
                    val dx = existing.baseRelX - rx
                    val dy = existing.baseRelY - ry
                    (dx * dx + dy * dy) < 0.08f // Minimum distance threshold
                }
            } while (tooClose && attempts < 10)

            val radiusPx = minRadiusPx + rng.nextFloat() * (maxRadiusPx - minRadiusPx)

            val baseRgb = shuffledColors[i % shuffledColors.size]
            val alphaInt = if (orbCount >= 3) (125 + rng.nextInt(55)) else (155 + rng.nextInt(70))
            val orbColor = Color.argb(alphaInt, Color.red(baseRgb), Color.green(baseRgb), Color.blue(baseRgb))

            val ampX = 0.040f + rng.nextFloat() * 0.035f
            val ampY = 0.040f + rng.nextFloat() * 0.035f
            val freqX = 0.75f + rng.nextFloat() * 0.75f
            val freqY = 0.75f + rng.nextFloat() * 0.75f
            val phase = rng.nextFloat() * (2 * Math.PI.toFloat())

            // Pick diverse motion patterns: clockwise, counter-clockwise, linear in any direction, random open curve
            val modes = OrbMotionMode.values()
            val chosenMode = modes[rng.nextInt(modes.size)]
            val linearAngle = rng.nextFloat() * (2 * Math.PI.toFloat())
            val ratio1 = 1.9f + rng.nextFloat() * 1.6f
            val ratio2 = 2.6f + rng.nextFloat() * 1.9f

            result.add(
                GlowOrb(
                    baseRelX = rx,
                    baseRelY = ry,
                    relX = rx,
                    relY = ry,
                    baseRadiusPx = radiusPx,
                    radiusPx = radiusPx,
                    color = orbColor,
                    ampX = ampX,
                    ampY = ampY,
                    freqX = freqX,
                    freqY = freqY,
                    phase = phase,
                    motionMode = chosenMode,
                    linearAngle = linearAngle,
                    curveRatio1 = ratio1,
                    curveRatio2 = ratio2
                )
            )
        }
        return result
    }

    /**
     * Captures a snapshot of the current state of all glowing orbs.
     */
    fun getOrbsSnapshot(): List<GlowOrb> {
        return orbs.map { it.copy() }
    }

    /**
     * Sets the glowing orbs directly from an existing state snapshot.
     */
    fun setOrbs(newOrbs: List<GlowOrb>) {
        transitionAnimator?.cancel()
        stopAmbientAnimation()
        orbs.clear()
        orbs.addAll(newOrbs.map { it.copy() })
        invalidate()
    }

    /**
     * Smoothly transitions from current ambient background constellation to a brand new one.
     * Uses nearest-neighbor pairing so existing orbs glide directly to their closest target (old -> new),
     * while surplus orbs smoothly shrink to zero or bloom in from zero.
     */
    fun transitionToNewConstellation(
        durationMs: Long = 950L,
        onUpdate: (() -> Unit)? = null,
        onComplete: (() -> Unit)? = null
    ) {
        transitionAnimator?.cancel()

        val newTargets = generateRandomOrbs()
        val currentSnapshots = ArrayList(orbs)

        data class TransitionSlot(
            val startRelX: Float, val targetRelX: Float,
            val startRelY: Float, val targetRelY: Float,
            val startRadius: Float, val targetRadius: Float,
            val startColor: Int, val targetColor: Int,
            val targetOrb: GlowOrb
        )

        val slots = mutableListOf<TransitionSlot>()
        val availableOld = currentSnapshots.toMutableList()
        val availableNew = newTargets.toMutableList()

        // 1. Nearest-Neighbor Pairing: Closest points pair together and glide smoothly (old -> new)
        while (availableOld.isNotEmpty() && availableNew.isNotEmpty()) {
            var bestOldIdx = -1
            var bestNewIdx = -1
            var minDistanceSq = Float.MAX_VALUE

            for (oIdx in availableOld.indices) {
                val old = availableOld[oIdx]
                for (nIdx in availableNew.indices) {
                    val new = availableNew[nIdx]
                    val dx = old.relX - new.relX
                    val dy = old.relY - new.relY
                    val distSq = dx * dx + dy * dy
                    if (distSq < minDistanceSq) {
                        minDistanceSq = distSq
                        bestOldIdx = oIdx
                        bestNewIdx = nIdx
                    }
                }
            }

            if (bestOldIdx != -1 && bestNewIdx != -1) {
                val matchedOld = availableOld.removeAt(bestOldIdx)
                val matchedNew = availableNew.removeAt(bestNewIdx)
                slots.add(
                    TransitionSlot(
                        startRelX = matchedOld.relX, targetRelX = matchedNew.relX,
                        startRelY = matchedOld.relY, targetRelY = matchedNew.relY,
                        startRadius = matchedOld.radiusPx, targetRadius = matchedNew.radiusPx,
                        startColor = matchedOld.color, targetColor = matchedNew.color,
                        targetOrb = matchedNew
                    )
                )
            } else {
                break
            }
        }

        // 2. Surplus old orbs shrink down to 0 and fade out at their current location
        for (leftoverOld in availableOld) {
            val transparentColor = Color.argb(0, Color.red(leftoverOld.color), Color.green(leftoverOld.color), Color.blue(leftoverOld.color))
            slots.add(
                TransitionSlot(
                    startRelX = leftoverOld.relX, targetRelX = leftoverOld.relX,
                    startRelY = leftoverOld.relY, targetRelY = leftoverOld.relY,
                    startRadius = leftoverOld.radiusPx, targetRadius = 0f,
                    startColor = leftoverOld.color, targetColor = transparentColor,
                    targetOrb = leftoverOld
                )
            )
        }

        // 3. Surplus new orbs bloom/expand from radius 0 and fade in at their target location
        for (leftoverNew in availableNew) {
            val transparentColor = Color.argb(0, Color.red(leftoverNew.color), Color.green(leftoverNew.color), Color.blue(leftoverNew.color))
            slots.add(
                TransitionSlot(
                    startRelX = leftoverNew.relX, targetRelX = leftoverNew.relX,
                    startRelY = leftoverNew.relY, targetRelY = leftoverNew.relY,
                    startRadius = 0f, targetRadius = leftoverNew.radiusPx,
                    startColor = transparentColor, targetColor = leftoverNew.color,
                    targetOrb = leftoverNew
                )
            )
        }

        val finalTargets = slots.filter { it.targetRadius > 1f }.map { slot ->
            slot.targetOrb.copy(
                relX = slot.targetRelX,
                relY = slot.targetRelY,
                radiusPx = slot.targetRadius,
                color = slot.targetColor,
                baseRelX = slot.targetRelX,
                baseRelY = slot.targetRelY,
                baseRadiusPx = slot.targetRadius
            )
        }

        transitionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener { animator ->
                val f = animator.animatedValue as Float
                orbs.clear()
                for (slot in slots) {
                    val r = slot.startRadius + (slot.targetRadius - slot.startRadius) * f
                    if (r > 1f) {
                        val x = slot.startRelX + (slot.targetRelX - slot.startRelX) * f
                        val y = slot.startRelY + (slot.targetRelY - slot.startRelY) * f
                        val c = argbEvaluator.evaluate(f, slot.startColor, slot.targetColor) as Int
                        val orb = slot.targetOrb.copy(
                            relX = x,
                            relY = y,
                            radiusPx = r,
                            color = c,
                            baseRelX = x,
                            baseRelY = y,
                            baseRadiusPx = r
                        )
                        orbs.add(orb)
                    }
                }
                invalidate()
                onUpdate?.invoke()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    orbs.clear()
                    orbs.addAll(finalTargets)
                    invalidate()
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    private var lastFrameUptimeMs = 0L
    private var accumulatedSeconds = 0f

    /**
     * Starts continuous smooth floating and pulsing animation of ambient color orbs.
     * Integrates diverse motion patterns: clockwise, counter-clockwise, linear oscillations across various angles,
     * and smooth non-repeating open curves. Uses monotonic time integration to prevent any frame boundary jumps.
     */
    fun startAmbientAnimation(durationMs: Long = 6000L) {
        if (ambientAnimator?.isRunning == true) return
        lastFrameUptimeMs = SystemClock.uptimeMillis()

        ambientAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val now = SystemClock.uptimeMillis()
                if (lastFrameUptimeMs > 0L) {
                    val dt = ((now - lastFrameUptimeMs) / 1000f).coerceIn(0f, 0.08f)
                    accumulatedSeconds += dt
                }
                lastFrameUptimeMs = now

                val baseSpeed = (2 * Math.PI.toFloat()) / (durationMs / 1000f)
                val t = accumulatedSeconds * baseSpeed

                for (orb in orbs) {
                    val angleX = t * orb.freqX + orb.phase
                    val angleY = t * orb.freqY + orb.phase

                    val (dx, dy) = when (orb.motionMode) {
                        OrbMotionMode.CLOCKWISE -> {
                            Pair(
                                sin(angleX) * orb.ampX,
                                cos(angleY) * orb.ampY
                            )
                        }
                        OrbMotionMode.COUNTER_CLOCKWISE -> {
                            Pair(
                                cos(angleX) * orb.ampX,
                                sin(angleY) * orb.ampY
                            )
                        }
                        OrbMotionMode.LINEAR -> {
                            val osc = sin(angleX)
                            Pair(
                                cos(orb.linearAngle) * osc * orb.ampX * 1.3f,
                                sin(orb.linearAngle) * osc * orb.ampY * 1.3f
                            )
                        }
                        OrbMotionMode.RANDOM_OPEN_CURVE -> {
                            // Non-closed smooth random wandering trajectory using incommensurate harmonic multipliers
                            val xDrift = sin(angleX) * 0.62f + sin(angleX * orb.curveRatio1 + 1.25f) * 0.38f
                            val yDrift = cos(angleY * 1.35f + 0.65f) * 0.62f + sin(angleY * orb.curveRatio2 + 2.15f) * 0.38f
                            Pair(
                                xDrift * orb.ampX * 1.25f,
                                yDrift * orb.ampY * 1.25f
                            )
                        }
                        OrbMotionMode.FIGURE_EIGHT -> {
                            Pair(
                                sin(angleX) * orb.ampX * 1.15f,
                                sin(2f * angleY) * orb.ampY * 1.15f
                            )
                        }
                    }

                    orb.relX = orb.baseRelX + dx
                    orb.relY = orb.baseRelY + dy
                    val pulse = 1f + 0.12f * sin(t * 1.5f + orb.phase)
                    orb.radiusPx = orb.baseRadiusPx * pulse
                }
                invalidate()
            }
            start()
        }
    }

    /**
     * Stops ambient animation.
     */
    fun stopAmbientAnimation() {
        ambientAnimator?.cancel()
        ambientAnimator = null
        lastFrameUptimeMs = 0L
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAmbientAnimation()
        transitionAnimator?.cancel()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        drawMeshOnCanvas(canvas, w, h)
    }

    /**
     * Renders the ambient mesh directly to any canvas
     */
    fun drawMeshOnCanvas(canvas: Canvas, w: Float, h: Float) {
        // 1. Paint the solid deep dark background
        canvas.drawColor(baseDarkColor)

        // 2. Draw each glowing orb with smooth radial falloff into complete transparency
        for (orb in orbs) {
            val cx = orb.relX * w
            val cy = orb.relY * h
            val radius = orb.radiusPx

            val shader = RadialGradient(
                cx, cy, radius,
                intArrayOf(orb.color, Color.TRANSPARENT),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            canvas.drawCircle(cx, cy, radius, paint)
        }
        paint.shader = null
    }

    /**
     * Accurately renders a specific rectangular window of the ambient mesh into targetBitmap,
     * mapped precisely to the card's relative position on this background.
     */
    fun renderRegion(
        targetBitmap: Bitmap,
        regionX: Float,
        regionY: Float,
        regionW: Float,
        regionH: Float
    ) {
        val canvas = Canvas(targetBitmap)
        val bw = targetBitmap.width.toFloat()
        val bh = targetBitmap.height.toFloat()
        if (regionW <= 0f || regionH <= 0f || bw <= 0f || bh <= 0f) return

        val scaleX = bw / regionW
        val scaleY = bh / regionH

        // 1. Clear target bitmap to complete transparency (not opaque black!)
        canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)

        // 2. Draw orbs mapped directly into targetBitmap coordinates
        val viewW = this.width.toFloat().coerceAtLeast(1f)
        val viewH = this.height.toFloat().coerceAtLeast(1f)

        for (orb in orbs) {
            val cx = orb.relX * viewW
            val cy = orb.relY * viewH
            val r = orb.radiusPx

            // Quick AABB overlap test: does orb intersect this card window?
            if (cx + r < regionX || cx - r > regionX + regionW ||
                cy + r < regionY || cy - r > regionY + regionH) {
                continue
            }

            val mappedCx = (cx - regionX) * scaleX
            val mappedCy = (cy - regionY) * scaleY
            val mappedR = r * scaleX

            val shader = RadialGradient(
                mappedCx, mappedCy, mappedR,
                intArrayOf(orb.color, Color.TRANSPARENT),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
            paint.shader = shader
            canvas.drawCircle(mappedCx, mappedCy, mappedR, paint)
        }
        paint.shader = null
    }
}
