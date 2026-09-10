package com.iatb.materialthemes.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

/**
 * Custom ambient background view that draws random, small glowing color orbs
 * on a deep dark background.
 *
 * Requirements:
 * - Base background is always DARK.
 * - Glowing elements are SMALL orbs/blobs (not massive screen-covering washes).
 * - Count, position, and colors are completely RANDOMIZED on each screen/Activity creation.
 */
class AmbientMeshBackgroundView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private data class GlowOrb(
        val relX: Float,
        val relY: Float,
        val radiusPx: Float,
        val color: Int
    )

    private val orbs = mutableListOf<GlowOrb>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }

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
        // Pure unseeded Random so each Activity open produces a completely fresh constellation
        val rng = Random.Default
        val density = resources.displayMetrics.density

        // 1. Random count: strictly 1 to 4 points as requested
        val orbCount = rng.nextInt(1, 5)

        val shuffledColors = vibrantPalette.toList().shuffled(rng)

        for (i in 0 until orbCount) {
            // 2. Random positions: ensure good distribution across the screen
            val rx = when (i) {
                0 -> -0.05f + rng.nextFloat() * 1.10f
                1 -> if (rng.nextBoolean()) -0.05f + rng.nextFloat() * 0.45f else 0.55f + rng.nextFloat() * 0.50f
                else -> -0.05f + rng.nextFloat() * 1.10f
            }

            // Distribute vertically so buttons in the lower screen and preview in the upper screen both get light
            val ry = when (i) {
                0 -> 0.45f + rng.nextFloat() * 0.50f // Lower area (behind/near action buttons)
                1 -> 0.05f + rng.nextFloat() * 0.40f // Upper area (behind/near preview)
                else -> -0.05f + rng.nextFloat() * 1.10f
            }

            // 3. Radius: between 140dp and 260dp so 1-4 points create rich atmospheric ambient glows
            val rDp = 140f + rng.nextFloat() * 120f
            val radiusPx = rDp * density

            // Random vibrant color with bright glowing alpha at center
            val baseRgb = shuffledColors[i % shuffledColors.size]
            val alphaInt = (160 + rng.nextInt(75)) // 160 to 235 alpha (~63-92% opacity at core)
            val orbColor = Color.argb(alphaInt, Color.red(baseRgb), Color.green(baseRgb), Color.blue(baseRgb))

            orbs.add(GlowOrb(relX = rx, relY = ry, radiusPx = radiusPx, color = orbColor))
        }
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
