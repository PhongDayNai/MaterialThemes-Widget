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
        fun createPreviewRemoteViews(
            context: Context,
            size: WidgetSize,
            angle: Float,
            palette: ColorPalette,
            mode: WidgetContentMode,
            transparency: Int = WidgetPreferences.getTransparency(context),
            animProgress: Float = 1.0f
        ): RemoteViews {
            return createRenderedView(context, size, angle, palette, mode, transparency, animProgress)
        }

        fun updateAllWidgets(context: Context, animProgress: Float = 1.0f) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DiagonalWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id, animProgress)
            }
            Diagonal4x3WidgetProvider.updateAllWidgets4x3(context, animProgress)
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
            val angle = WidgetPreferences.getRotationAngle(context)
            val palette = WidgetPreferences.getColorPalette(context)
            val mode = WidgetPreferences.getContentMode(context)
            val transparency = WidgetPreferences.getTransparency(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val viewMapping = mapOf(
                    SizeF(110f, 110f) to createRenderedView(context, WidgetSize.SIZE_2X2, angle, palette, mode, transparency, animProgress),
                    SizeF(205f, 110f) to createRenderedView(context, WidgetSize.SIZE_3X2, angle, palette, mode, transparency, animProgress),
                    SizeF(292f, 110f) to createRenderedView(context, WidgetSize.SIZE_4X2, angle, palette, mode, transparency, animProgress),
                    SizeF(110f, 250f) to createRenderedView(context, WidgetSize.SIZE_2X3, angle, palette, mode, transparency, animProgress),
                    SizeF(110f, 350f) to createRenderedView(context, WidgetSize.SIZE_2X4, angle, palette, mode, transparency, animProgress),
                    SizeF(205f, 250f) to createRenderedView(context, WidgetSize.SIZE_3X3, angle, palette, mode, transparency, animProgress),
                    SizeF(292f, 250f) to createRenderedView(context, WidgetSize.SIZE_4X3, angle, palette, mode, transparency, animProgress)
                )
                return RemoteViews(viewMapping)
            }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val size = resolveWidgetSize(minWidth, minHeight)
            return createRenderedView(context, size, angle, palette, mode, transparency, animProgress)
        }

        private fun resolveWidgetSize(minWidth: Int, minHeight: Int): WidgetSize {
            return if (minHeight >= 250) {
                when {
                    minWidth >= 292 -> WidgetSize.SIZE_4X3
                    minWidth >= 205 -> WidgetSize.SIZE_3X3
                    minHeight >= 350 -> WidgetSize.SIZE_2X4
                    else -> WidgetSize.SIZE_2X3
                }
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
            angle: Float,
            palette: ColorPalette,
            mode: WidgetContentMode,
            transparency: Int = WidgetPreferences.getTransparency(context),
            animProgress: Float = 1.0f
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_canvas_container)

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
