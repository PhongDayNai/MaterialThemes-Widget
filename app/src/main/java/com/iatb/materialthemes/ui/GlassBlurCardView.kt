package com.iatb.materialthemes.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout

/**
 * Premium Frosted Glass Card with real-time blur sampled directly from the ambient background.
 *
 * Emulates high-end frosted glass (like camera GPS / iOS HUD overlay):
 * - Live pre-draw sampling of the background beneath the card.
 * - Downsampled fast 2-pass box blur.
 * - Luminous translucent frosted glass sheen (not dark or murky).
 * - Specular frosted outline border.
 * - Strictly bounded rounded ripple with zero rectangular artifacts.
 */
class GlassBlurCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    var cornerRadiusPx = 32f * density
        set(value) {
            field = value
            updatePath()
            setupRipple()
            invalidate()
        }

    var strokeWidthPx = 1.5f * density
        set(value) {
            field = value
            strokePaint.strokeWidth = value
            invalidate()
        }

    /**
     * Mức độ làm mờ (mặc định = 1).
     * - Tăng lên 2 hoặc 3: tăng độ mờ nhòe rõ nét hơn một chút.
     * - Tăng lên 4 hoặc 5: làm mờ rất mạnh, quầng sáng hòa lẫn mềm mịn.
     */
    var blurRadius: Int = 4
        set(value) {
            field = value.coerceAtLeast(1)
            refreshBlur()
        }

    private val clipPath = Path()
    private val rectF = RectF()



    // Delicate specular glass outline stroke
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        color = Color.parseColor("#4DFFFFFF") // Semi-transparent glass rim
    }

    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG).apply {
        isDither = true
    }

    private var blurredBitmap: Bitmap? = null
    private var targetBackgroundView: View? = null
    private val downsampleScale = 0.25f // 1/4 scale: crisp, clean sampling and fast blur
    private var isRenderingSnapshot = false

    private var lastRenderX = -1
    private var lastRenderY = -1
    private var lastWidth = -1
    private var lastHeight = -1

    private val preDrawListener = ViewTreeObserver.OnPreDrawListener {
        if (isShown && width > 0 && height > 0) {
            val loc = IntArray(2)
            getLocationInWindow(loc)
            if (loc[0] != lastRenderX || loc[1] != lastRenderY || width != lastWidth || height != lastHeight || blurredBitmap == null) {
                updateBlurBitmap(loc[0], loc[1])
            }
        }
        true
    }

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        setupRipple()
    }

    private fun setupRipple() {
        val mask = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = cornerRadiusPx
            setColor(Color.WHITE)
        }
        val rippleColor = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
        foreground = RippleDrawable(rippleColor, null, mask)
    }

    fun setTargetBackgroundView(view: View) {
        this.targetBackgroundView = view
        post {
            val loc = IntArray(2)
            getLocationInWindow(loc)
            updateBlurBitmap(loc[0], loc[1])
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        viewTreeObserver.addOnPreDrawListener(preDrawListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        try {
            viewTreeObserver.removeOnPreDrawListener(preDrawListener)
        } catch (_: Exception) {}
        blurredBitmap?.recycle()
        blurredBitmap = null
    }

    fun refreshBlur() {
        if (width <= 0 || height <= 0) return
        val loc = IntArray(2)
        getLocationInWindow(loc)
        updateBlurBitmap(loc[0], loc[1])
        invalidate()
    }

    private fun updateBlurBitmap(cardWindowX: Int, cardWindowY: Int) {
        val bg = targetBackgroundView ?: return
        val w = width
        val h = height
        if (w <= 0 || h <= 0 || isRenderingSnapshot) return

        val bw = (w * downsampleScale).toInt().coerceAtLeast(8)
        val bh = (h * downsampleScale).toInt().coerceAtLeast(8)

        try {
            var bmp = blurredBitmap
            if (bmp == null || bmp.isRecycled || bmp.width != bw || bmp.height != bh) {
                bmp?.recycle()
                bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
                blurredBitmap = bmp
            }

            val bgLoc = IntArray(2)
            bg.getLocationInWindow(bgLoc)

            val relX = (cardWindowX - bgLoc[0]).toFloat()
            val relY = (cardWindowY - bgLoc[1]).toFloat()

            isRenderingSnapshot = true

            if (bg is AmbientMeshBackgroundView) {
                // Mathematically exact sampling of the ambient background directly behind the card
                bg.renderRegion(
                    targetBitmap = bmp,
                    regionX = relX,
                    regionY = relY,
                    regionW = w.toFloat(),
                    regionH = h.toFloat()
                )
            } else {
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
                canvas.save()
                canvas.scale(downsampleScale, downsampleScale)
                canvas.translate(-relX, -relY)
                bg.draw(canvas)
                canvas.restore()
            }

            isRenderingSnapshot = false

            // Áp dụng độ làm mờ dựa trên biến blurRadius
            fastBoxBlur(bmp, blurRadius)

            lastRenderX = cardWindowX
            lastRenderY = cardWindowY
            lastWidth = w
            lastHeight = h
        } catch (_: Exception) {
            isRenderingSnapshot = false
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updatePath()
    }

    private fun updatePath() {
        rectF.set(0f, 0f, width.toFloat(), height.toFloat())
        clipPath.reset()
        clipPath.addRoundRect(rectF, cornerRadiusPx, cornerRadiusPx, Path.Direction.CW)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        canvas.clipPath(clipPath)

        // 1. Draw sampled and gently blurred ambient background from behind (100% transparent, NO cloudy or turbid tint!)
        val bmp = blurredBitmap
        if (bmp != null && !bmp.isRecycled) {
            canvas.drawBitmap(bmp, null, rectF, bitmapPaint)
        }

        // 2. Specular frosted glass border outline
        val halfStroke = strokeWidthPx / 2f
        val strokeRect = RectF(
            rectF.left + halfStroke,
            rectF.top + halfStroke,
            rectF.right - halfStroke,
            rectF.bottom - halfStroke
        )
        val r = (cornerRadiusPx - halfStroke).coerceAtLeast(0f)
        canvas.drawRoundRect(strokeRect, r, r, strokePaint)

        canvas.restore()
        super.onDraw(canvas)
    }

    /**
     * Fast 4-channel (A, R, G, B) box blur algorithm.
     * Computes exact moving average per channel so brightness and colors are perfectly preserved.
     */
    private fun fastBoxBlur(bitmap: Bitmap, radius: Int) {
        if (radius < 1) return
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return

        // Clamp radius to prevent out-of-bounds on tiny bitmaps
        val maxR = maxOf(1, minOf(w, h) / 2 - 1)
        val rad = radius.coerceIn(1, maxR)

        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = rad + rad + 1

        val a = IntArray(wh)
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var asum: Int
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(maxOf(w, h))

        // Precomputed division table for fast integer division
        val dv = IntArray(256 * div)
        for (idx in 0 until 256 * div) {
            dv[idx] = idx / div
        }

        yw = 0
        yi = 0

        for (curY in 0 until h) {
            asum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            for (curI in -rad..rad) {
                p = pix[yi + minOf(wm, maxOf(curI, 0))]
                asum += (p ushr 24)
                rsum += (p and 0xff0000) shr 16
                gsum += (p and 0x00ff00) shr 8
                bsum += p and 0x0000ff
            }
            for (curX in 0 until w) {
                a[yi] = dv[asum]
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                if (curY == 0) {
                    vmin[curX] = minOf(curX + rad + 1, wm)
                }
                val p1 = pix[yw + vmin[curX]]
                val p2 = pix[yw + maxOf(curX - rad, 0)]

                asum += (p1 ushr 24) - (p2 ushr 24)
                rsum += ((p1 and 0xff0000) - (p2 and 0xff0000)) shr 16
                gsum += ((p1 and 0x00ff00) - (p2 and 0x00ff00)) shr 8
                bsum += (p1 and 0x0000ff) - (p2 and 0x0000ff)
                yi++
            }
            yw += w
        }

        for (curX in 0 until w) {
            asum = 0
            rsum = 0
            gsum = 0
            bsum = 0
            yp = -rad * w
            for (curI in -rad..rad) {
                yi = maxOf(0, yp) + curX
                asum += a[yi]
                rsum += r[yi]
                gsum += g[yi]
                bsum += b[yi]
                yp += w
            }
            yi = curX
            for (curY in 0 until h) {
                pix[yi] = (dv[asum] shl 24) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                if (curX == 0) {
                    vmin[curY] = minOf(curY + rad + 1, hm) * w
                }
                val p1 = curX + vmin[curY]
                val p2 = curX + maxOf(curY - rad, 0) * w

                asum += a[p1] - a[p2]
                rsum += r[p1] - r[p2]
                gsum += g[p1] - g[p2]
                bsum += b[p1] - b[p2]
                yi += w
            }
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
    }
}
