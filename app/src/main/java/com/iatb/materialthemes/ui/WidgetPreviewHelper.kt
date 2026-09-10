package com.iatb.materialthemes.ui

import android.animation.ValueAnimator
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer
import com.iatb.materialthemes.render.WidgetCanvasRenderer

object WidgetPreviewHelper {

    fun applyPressScaleEffect(view: View) {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(120).start()
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()
                }
            }
            false
        }
    }

    fun updatePreview(
        context: Context,
        container: FrameLayout,
        category: WidgetCategory,
        size: WidgetSize,
        angle: Float,
        palette: ColorPalette,
        contentMode: WidgetContentMode,
        transparency: Int,
        animate: Boolean = false,
        activeAnimator: ValueAnimator? = null
    ): ValueAnimator? {
        activeAnimator?.cancel()

        val density = context.resources.displayMetrics.density

        val (wDp, hDp) = when (size) {
            WidgetSize.SIZE_2X2 -> 160 to 160
            WidgetSize.SIZE_3X2 -> 240 to 160
            WidgetSize.SIZE_4X2 -> 290 to 150
            WidgetSize.SIZE_2X3 -> 130 to 180
            WidgetSize.SIZE_2X4 -> 120 to 190
            WidgetSize.SIZE_3X3 -> 190 to 190
            WidgetSize.SIZE_4X3 -> 250 to 190
        }

        val widthPx = (wDp * density).toInt()
        val heightPx = (hDp * density).toInt()

        container.removeAllViews()

        val weather = WeatherRepository.getWeatherData(context)
        val imageView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val params = FrameLayout.LayoutParams(widthPx, heightPx).apply {
            gravity = Gravity.CENTER
        }
        container.addView(imageView, params)

        if (category == WidgetCategory.DIAGONAL) {
            if (animate) {
                val animator = ValueAnimator.ofFloat(0.00f, 1.0f).apply {
                    duration = 340L
                    interpolator = DecelerateInterpolator(1.2f)
                    addUpdateListener { va ->
                        val p = va.animatedValue as Float
                        val bmp = WidgetCanvasRenderer.render(
                            context,
                            widthPx,
                            heightPx,
                            angle,
                            palette,
                            contentMode,
                            size,
                            weather,
                            transparency,
                            animProgress = p
                        )
                        imageView.setImageBitmap(bmp)
                    }
                }
                animator.start()
                return animator
            } else {
                val bitmap = WidgetCanvasRenderer.render(
                    context,
                    widthPx,
                    heightPx,
                    angle,
                    palette,
                    contentMode,
                    size,
                    weather,
                    transparency,
                    animProgress = 1.0f
                )
                imageView.setImageBitmap(bitmap)
                return null
            }
        } else {
            if (animate) {
                val animator = ValueAnimator.ofFloat(0.00f, 1.0f).apply {
                    duration = 340L
                    interpolator = DecelerateInterpolator(1.2f)
                    addUpdateListener { va ->
                        val p = va.animatedValue as Float
                        val bmp = ShapeWidgetCanvasRenderer.render(
                            context = context,
                            category = category,
                            size = size,
                            widthPx = widthPx,
                            heightPx = heightPx,
                            weather = weather,
                            animProgress = p
                        )
                        imageView.setImageBitmap(bmp)
                    }
                }
                animator.start()
                return animator
            } else {
                val bitmap = ShapeWidgetCanvasRenderer.render(
                    context = context,
                    category = category,
                    size = size,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    weather = weather,
                    animProgress = 1.0f
                )
                imageView.setImageBitmap(bitmap)
                return null
            }
        }
    }
}
