package com.iatb.materialthemes.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.ColorPalette
import com.iatb.materialthemes.data.WeatherRepository
import com.iatb.materialthemes.data.WidgetContentMode
import com.iatb.materialthemes.data.WidgetPreferences

class Diagonal4x3WidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            WidgetUpdateScheduler.ACTION_WIDGET_TICK,
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_WALLPAPER_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED -> {
                com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()
                updateAllWidgets4x3(context)
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
        fun updateAllWidgets4x3(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, Diagonal4x3WidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val remoteViews = buildRemoteViews(context, appWidgetManager, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): RemoteViews {
            val angle = WidgetPreferences.getRotationAngle(context)
            val palette = WidgetPreferences.getColorPalette(context)
            val mode = WidgetPreferences.getContentMode(context)
            val transparency = WidgetPreferences.getTransparency(context)

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val viewMapping = mapOf(
                    SizeF(270f, 180f) to createRenderedView(context, WidgetSize.SIZE_4X3, angle, palette, mode, transparency),
                    SizeF(200f, 200f) to createRenderedView(context, WidgetSize.SIZE_3X3, angle, palette, mode, transparency),
                    SizeF(110f, 110f) to createRenderedView(context, WidgetSize.SIZE_4X3, angle, palette, mode, transparency)
                )
                RemoteViews(viewMapping)
            } else {
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 270)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
                val size = if (minWidth < 180 || minHeight < 160) WidgetSize.SIZE_4X3 else resolveWidgetSize(minWidth, minHeight)
                createRenderedView(context, size, angle, palette, mode, transparency)
            }
        }

        private fun resolveWidgetSize(minWidth: Int, minHeight: Int): WidgetSize {
            return when {
                minWidth >= 270 && minHeight >= 180 -> WidgetSize.SIZE_4X3
                minWidth >= 180 && minHeight >= 180 -> WidgetSize.SIZE_3X3
                minWidth >= 270 && minHeight < 160 -> WidgetSize.SIZE_4X2
                minWidth >= 180 && minHeight < 160 -> WidgetSize.SIZE_3X2
                else -> WidgetSize.SIZE_4X3
            }
        }

        private fun createRenderedView(
            context: Context,
            size: WidgetSize,
            angle: Float,
            palette: ColorPalette,
            mode: WidgetContentMode,
            transparency: Int = WidgetPreferences.getTransparency(context)
        ): RemoteViews {
            return DiagonalWidgetProvider.createPreviewRemoteViews(context, size, angle, palette, mode, transparency)
        }
    }
}
