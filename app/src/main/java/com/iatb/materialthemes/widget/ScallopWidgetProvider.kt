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
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.render.ShapeWidgetCanvasRenderer

class ScallopWidgetProvider : AppWidgetProvider() {

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
        ActiveWidgetManager.recordProviderUpdate(context, ScallopWidgetProvider::class.java, appWidgetIds)
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
        ActiveWidgetManager.recordOptionsChanged(context, ScallopWidgetProvider::class.java, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun updateAllWidgets(context: Context, animProgress: Float = 1.0f) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ScallopWidgetProvider::class.java)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val viewMapping = mapOf(
                    SizeF(110f, 110f) to createPreviewRemoteViews(context, WidgetSize.SIZE_2X2, animProgress, appWidgetId),
                    SizeF(205f, 110f) to createPreviewRemoteViews(context, WidgetSize.SIZE_3X2, animProgress, appWidgetId),
                    SizeF(270f, 110f) to createPreviewRemoteViews(context, WidgetSize.SIZE_4X2, animProgress, appWidgetId),
                    SizeF(110f, 250f) to createPreviewRemoteViews(context, WidgetSize.SIZE_2X3, animProgress, appWidgetId),
                    SizeF(205f, 250f) to createPreviewRemoteViews(context, WidgetSize.SIZE_3X3, animProgress, appWidgetId),
                    SizeF(270f, 250f) to createPreviewRemoteViews(context, WidgetSize.SIZE_4X3, animProgress, appWidgetId),
                    SizeF(110f, 340f) to createPreviewRemoteViews(context, WidgetSize.SIZE_2X4, animProgress, appWidgetId)
                )
                return RemoteViews(viewMapping)
            }

            val options = try {
                appWidgetManager.getAppWidgetOptions(appWidgetId)
            } catch (_: Exception) {
                null
            }
            val size = WidgetPreferences.getWidgetSavedSize(context, appWidgetId)
                ?: ActiveWidgetManager.resolveWidgetSize(context, options, WidgetSize.SIZE_2X2)
            return createPreviewRemoteViews(context, size, animProgress, appWidgetId)
        }

        fun createPreviewRemoteViews(
            context: Context,
            size: WidgetSize,
            animProgress: Float = 1.0f,
            appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        ): RemoteViews {
            val layoutId = WidgetClickRouter.getContainerLayoutId(size)
            val views = RemoteViews(context.packageName, layoutId)
            val (wDp, hDp) = when (size) {
                WidgetSize.SIZE_2X2 -> 200 to 200
                WidgetSize.SIZE_3X2 -> 300 to 200
                WidgetSize.SIZE_4X2 -> 400 to 200
                WidgetSize.SIZE_2X3 -> 200 to 300
                WidgetSize.SIZE_2X4 -> 200 to 400
                WidgetSize.SIZE_3X3 -> 300 to 300
                WidgetSize.SIZE_4X3 -> 400 to 300
            }

            val density = context.resources.displayMetrics.density
            val bitmap = ShapeWidgetCanvasRenderer.render(
                context = context,
                category = WidgetCategory.SCALLOP,
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
                category = WidgetCategory.SCALLOP,
                size = size,
                locationName = weather.locationName,
                appWidgetId = appWidgetId
            )

            return views
        }
    }
}
