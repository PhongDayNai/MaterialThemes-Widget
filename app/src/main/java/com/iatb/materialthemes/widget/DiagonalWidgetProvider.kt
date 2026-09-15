package com.iatb.materialthemes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.iatb.materialthemes.MainActivity
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences
import com.iatb.materialthemes.render.WidgetCanvasRenderer

class DiagonalWidgetProvider : AppWidgetProvider() {

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
                OrganicWidgetProvider.updateAllWidgets(context)
                ScallopWidgetProvider.updateAllWidgets(context)
                BatteryWidgetProvider.updateAllWidgets(context)
                WidgetUpdateScheduler.scheduleNextMinuteTick(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextMinuteTick(context)
        WeatherRepository.refreshWeather(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateScheduler.cancelSchedule(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        ActiveWidgetManager.recordProviderUpdate(context, DiagonalWidgetProvider::class.java, appWidgetIds)
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
        ActiveWidgetManager.recordOptionsChanged(context, DiagonalWidgetProvider::class.java, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        fun createPreviewRemoteViews(
            context: Context,
            size: WidgetSize,
            angle: Float,
            palette: ColorPalette,
            mode: WidgetContentMode,
            transparency: Int = WidgetPreferences.getTransparency(context),
            animProgress: Float = 1.0f,
            appWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
        ): RemoteViews {
            return createRenderedView(context, size, angle, palette, mode, transparency, animProgress, appWidgetId)
        }

        fun updateAllWidgets(context: Context, animProgress: Float = 1.0f) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, DiagonalWidgetProvider::class.java)
            val ids = try {
                appWidgetManager.getAppWidgetIds(component)
            } catch (_: Exception) {
                IntArray(0)
            }
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
            try {
                val remoteViews = buildRemoteViews(context, appWidgetManager, appWidgetId, animProgress)
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            } catch (e: Exception) {
                android.util.Log.e("DiagonalWidgetProvider", "Error updating widget $appWidgetId", e)
            }
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            animProgress: Float = 1.0f
        ): RemoteViews {
            val angle = WidgetPreferences.getRotationAngle(context)
            val palette = WidgetPreferences.getColorPalette(context)
            val mode = WidgetPreferences.getContentMode(context)
            val transparency = WidgetPreferences.getTransparency(context)

            val options = try {
                appWidgetManager.getAppWidgetOptions(appWidgetId)
            } catch (_: Exception) {
                null
            }
            val size = WidgetPreferences.getWidgetSavedSize(context, appWidgetId)
                ?: ActiveWidgetManager.resolveWidgetSize(context, options, WidgetSize.SIZE_2X2)
            return createRenderedView(context, size, angle, palette, mode, transparency, animProgress, appWidgetId)
        }

        private fun createRenderedView(
            context: Context,
            size: WidgetSize,
            angle: Float,
            palette: ColorPalette,
            mode: WidgetContentMode,
            transparency: Int = WidgetPreferences.getTransparency(context),
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
            val weather = WeatherRepository.getWeatherData(context)
            val bitmap = WidgetCanvasRenderer.render(
                context,
                (wDp * density).toInt(),
                (hDp * density).toInt(),
                angle,
                palette,
                mode,
                size,
                weather,
                transparency,
                animProgress = animProgress
            )

            views.setImageViewBitmap(R.id.iv_canvas_render, bitmap)

            WidgetClickRouter.bindClickZones(
                views = views,
                context = context,
                category = WidgetCategory.DIAGONAL,
                size = size,
                mode = mode,
                locationName = weather.locationName,
                appWidgetId = appWidgetId
            )

            return views
        }
    }
}
