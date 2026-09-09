package com.iatb.materialthemes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.iatb.materialthemes.MainActivity
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer

class OrganicWideWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetAnimationManager.ACTION_RUN_ENTER_ANIMATION -> {
                WidgetAnimationManager.triggerEnterAnimation(context, isFull = true)
            }
            Intent.ACTION_USER_PRESENT -> {
                WidgetAnimationManager.handleUserPresent(context)
            }
            WidgetUpdateScheduler.ACTION_WIDGET_TICK,
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_WALLPAPER_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED -> {
                com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()
                updateAllWidgets(context)
                WidgetUpdateScheduler.scheduleNextMinuteTick(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextMinuteTick(context)
        WeatherRepository.refreshWeather(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        WidgetUpdateScheduler.scheduleNextMinuteTick(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAllWidgets(context: Context, animProgress: Float = 1.0f) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, OrganicWideWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id, animProgress)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            animProgress: Float = 1.0f
        ) {
            val remoteViews = buildRemoteViews(context, appWidgetManager, appWidgetId, animProgress)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            animProgress: Float = 1.0f
        ): RemoteViews {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val viewMapping = mapOf(
                    SizeF(200f, 110f) to createRenderedView(context, WidgetSize.SIZE_4X2, animProgress),
                    SizeF(200f, 200f) to createRenderedView(context, WidgetSize.SIZE_3X3, animProgress),
                    SizeF(110f, 110f) to createRenderedView(context, WidgetSize.SIZE_2X2, animProgress)
                )
                RemoteViews(viewMapping)
            } else {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 200)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                val layoutSize = resolveLayout(minWidth, minHeight)
                createRenderedView(context, layoutSize, animProgress)
            }
        }

        private fun resolveLayout(minWidth: Int, minHeight: Int): WidgetSize {
            return when {
                minWidth >= 180 && minHeight >= 160 -> WidgetSize.SIZE_3X3
                else -> WidgetSize.SIZE_4X2
            }
        }

        private fun createRenderedView(
            context: Context,
            size: WidgetSize,
            animProgress: Float = 1.0f
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)
            val (wDp, hDp) = when (size) {
                WidgetSize.SIZE_4X2, WidgetSize.SIZE_3X2 -> 400 to 200
                WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> 300 to 300
                else -> 200 to 200
            }

            val density = context.resources.displayMetrics.density
            val bitmap = ShapeWidgetCanvasRenderer.render(
                context = context,
                category = WidgetCategory.ORGANIC,
                size = size,
                widthPx = (wDp * density).toInt(),
                heightPx = (hDp * density).toInt(),
                animProgress = animProgress
            )

            views.setImageViewBitmap(R.id.iv_canvas_render, bitmap)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_diagonal_root, pendingIntent)

            return views
        }
    }
}
