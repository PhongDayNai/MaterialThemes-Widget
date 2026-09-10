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

class OrganicWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_USER_PRESENT,
            WidgetAnimationManager.ACTION_RUN_ENTER_ANIMATION,
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
            val component = ComponentName(context, OrganicWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id, animProgress)
            }
            OrganicWideWidgetProvider.updateAllWidgets(context, animProgress)
            Organic4x3WidgetProvider.updateAllWidgets(context, animProgress)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val viewMapping = mapOf(
                    SizeF(110f, 110f) to createRenderedView(context, WidgetSize.SIZE_2X2, animProgress),
                    SizeF(205f, 110f) to createRenderedView(context, WidgetSize.SIZE_3X2, animProgress),
                    SizeF(292f, 110f) to createRenderedView(context, WidgetSize.SIZE_4X2, animProgress),
                    SizeF(205f, 250f) to createRenderedView(context, WidgetSize.SIZE_3X3, animProgress),
                    SizeF(292f, 250f) to createRenderedView(context, WidgetSize.SIZE_4X3, animProgress)
                )
                return RemoteViews(viewMapping)
            }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val size = resolveWidgetSize(minWidth, minHeight)
            return createRenderedView(context, size, animProgress)
        }

        private fun resolveWidgetSize(minWidth: Int, minHeight: Int): WidgetSize {
            return if (minHeight >= 250) {
                if (minWidth >= 292) WidgetSize.SIZE_4X3 else WidgetSize.SIZE_3X3
            } else {
                when {
                    minWidth >= 292 -> WidgetSize.SIZE_4X2
                    minWidth >= 205 -> WidgetSize.SIZE_3X2
                    else -> WidgetSize.SIZE_2X2
                }
            }
        }

        private fun createRenderedView(
            context: Context,
            size: WidgetSize,
            animProgress: Float = 1.0f
        ): RemoteViews {
            val layoutId = WidgetClickRouter.getContainerLayoutId(size)
            val views = RemoteViews(context.packageName, layoutId)
            val (wDp, hDp) = when (size) {
                WidgetSize.SIZE_2X2 -> 200 to 200
                WidgetSize.SIZE_3X2 -> 300 to 200
                WidgetSize.SIZE_4X2 -> 400 to 200
                WidgetSize.SIZE_3X3 -> 300 to 300
                WidgetSize.SIZE_4X3 -> 400 to 300
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

            val weather = WeatherRepository.getWeatherData(context)
            WidgetClickRouter.bindClickZones(
                views = views,
                context = context,
                category = WidgetCategory.ORGANIC,
                size = size,
                locationName = weather.locationName
            )

            return views
        }
    }
}
