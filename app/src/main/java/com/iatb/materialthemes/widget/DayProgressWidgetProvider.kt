package com.iatb.materialthemes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.DayProgressInfo
import com.iatb.materialthemes.data.DayProgressPreferences
import com.iatb.materialthemes.data.DayProgressRepository
import com.iatb.materialthemes.data.ResolvedPaletteColors
import com.iatb.materialthemes.render.DayProgressIndicatorRenderer
import com.iatb.materialthemes.ui.DayProgressWidgetDetailActivity

class DayProgressWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_WALLPAPER_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            DayProgressScheduler.ACTION_DAY_PROGRESS_TICK,
            ACTION_UPDATE_DAY_PROGRESS_WIDGET -> {
                com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()
                updateAllWidgets(context)
                DayProgressScheduler.scheduleNextMinuteTick(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        DayProgressScheduler.scheduleNextMinuteTick(context)
        updateAllWidgets(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        DayProgressScheduler.cancelSchedule(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        DayProgressScheduler.scheduleNextMinuteTick(context)
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
        const val ACTION_UPDATE_DAY_PROGRESS_WIDGET = "com.iatb.materialthemes.action.UPDATE_DAY_PROGRESS_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, DayProgressWidgetProvider::class.java)
            val ids = try {
                appWidgetManager.getAppWidgetIds(component)
            } catch (_: Exception) {
                IntArray(0)
            }
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            try {
                val remoteViews = buildRemoteViews(context, appWidgetManager, appWidgetId)
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            } catch (e: Exception) {
                android.util.Log.e("DayProgressWidget", "Error updating widget $appWidgetId", e)
            }
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): RemoteViews {
            val info = DayProgressRepository.getDayProgress(context)
            val colors = DayProgressPreferences.resolveDayProgressColors(context, info.sunriseMillis, info.sunsetMillis)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val viewMapping = mapOf(
                    // Horizontal Rows (130dp - 380dp width, ~60dp height)
                    SizeF(130f, 60f) to buildRowRemoteViews(context, 130, 60, info, colors, appWidgetId),
                    SizeF(220f, 60f) to buildRowRemoteViews(context, 220, 60, info, colors, appWidgetId),
                    SizeF(300f, 60f) to buildRowRemoteViews(context, 300, 60, info, colors, appWidgetId),
                    SizeF(380f, 60f) to buildRowRemoteViews(context, 380, 60, info, colors, appWidgetId),

                    // Square / Compact Layouts (~100dp - 140dp square)
                    SizeF(110f, 110f) to buildCompactRemoteViews(context, 110, 110, info, colors, appWidgetId),
                    SizeF(140f, 140f) to buildCompactRemoteViews(context, 140, 140, info, colors, appWidgetId),

                    // Standard Cards (200dp - 380dp width, ~130dp height)
                    SizeF(200f, 130f) to buildCardRemoteViews(context, 200, 130, info, colors, appWidgetId),
                    SizeF(300f, 130f) to buildCardRemoteViews(context, 300, 130, info, colors, appWidgetId),
                    SizeF(380f, 130f) to buildCardRemoteViews(context, 380, 130, info, colors, appWidgetId),

                    // Tall / Large Layouts (~200dp+ height)
                    SizeF(200f, 220f) to buildTallRemoteViews(context, 200, 220, info, colors, appWidgetId),
                    SizeF(300f, 220f) to buildTallRemoteViews(context, 300, 220, info, colors, appWidgetId),
                    SizeF(380f, 220f) to buildTallRemoteViews(context, 380, 220, info, colors, appWidgetId)
                )
                return RemoteViews(viewMapping)
            }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val isLandscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
            val maxW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)

            val widthDp = if (isLandscape) {
                if (maxW > 0) maxW else minW.takeIf { it > 0 } ?: 260
            } else {
                if (minW > 0) minW else maxW.takeIf { it > 0 } ?: 260
            }

            val heightDp = if (isLandscape) {
                if (minH > 0) minH else maxH.takeIf { it > 0 } ?: 60
            } else {
                if (maxH > 0) maxH else minH.takeIf { it > 0 } ?: 138
            }

            return if (heightDp < 95) {
                buildRowRemoteViews(context, widthDp, heightDp, info, colors, appWidgetId)
            } else if (widthDp <= 150 && heightDp <= 150) {
                buildCompactRemoteViews(context, widthDp, heightDp, info, colors, appWidgetId)
            } else if (heightDp < 180) {
                buildCardRemoteViews(context, widthDp, heightDp, info, colors, appWidgetId)
            } else {
                buildTallRemoteViews(context, widthDp, heightDp, info, colors, appWidgetId)
            }
        }

        private fun attachClickIntent(context: Context, views: RemoteViews, appWidgetId: Int = 0) {
            val clickIntent = Intent(context, DayProgressWidgetDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (appWidgetId != 0) {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId + 50000,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_day_progress_root, pendingIntent)
        }

        fun buildRowRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            info: DayProgressInfo,
            palette: DayProgressPreferences.DayProgressThemeColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_row)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setInt(R.id.iv_row_card_bg, "setColorFilter", palette.pillBgColor)
            views.setInt(R.id.iv_row_card_bg, "setImageAlpha", Color.alpha(palette.pillBgColor))

            views.setTextViewText(R.id.tv_row_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_row_percentage, palette.textColor)

            views.setTextViewText(R.id.tv_row_date, info.fullDateString)
            views.setTextColor(R.id.tv_row_date, palette.subTextColor)

            views.setImageViewResource(R.id.iv_row_phase_icon, info.phaseIconResId)
            views.setTextViewText(R.id.tv_row_sunrise, info.sunriseFormatted)
            views.setTextViewText(R.id.tv_row_sunset, info.sunsetFormatted)

            // Render slim elegant curved horizontal wave
            val density = context.resources.displayMetrics.density
            val curveW = ((widthDp - 140) * density).toInt().coerceAtLeast((120 * density).toInt())
            val curveH = (32 * density).toInt()
            val curveBitmap = DayProgressIndicatorRenderer.drawCurvedHorizontalBitmap(
                context = context,
                widthPx = curveW,
                heightPx = curveH,
                progress = info.progress,
                sunriseProgress = info.sunriseProgress,
                sunsetProgress = info.sunsetProgress,
                sunriseTimeStr = "",
                sunsetTimeStr = "",
                isDaylight = info.isDaylight,
                trackColor = palette.textColor,
                progressColor = palette.accentColor,
                strokeWidthPx = 3.2f * density,
                progressStartColor = palette.progressStartColor,
                progressEndColor = palette.progressEndColor
            )
            views.setImageViewBitmap(R.id.iv_row_curved_progress, curveBitmap)

            return views
        }

        fun buildCompactRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            info: DayProgressInfo,
            palette: DayProgressPreferences.DayProgressThemeColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_compact)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setTextViewText(R.id.tv_compact_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_compact_percentage, palette.textColor)

            views.setTextViewText(R.id.tv_compact_date, info.dateFormatted)
            views.setTextColor(R.id.tv_compact_date, palette.subTextColor)

            views.setImageViewResource(R.id.iv_compact_phase_icon, info.phaseIconResId)
            views.setTextViewText(R.id.tv_compact_solar_summary, "${info.sunriseFormatted} • ${info.sunsetFormatted}")

            // Circular XML Progress Bar
            views.setProgressBar(R.id.pb_compact_progress, 100, info.percentage, false)

            return views
        }

        fun buildCardRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            info: DayProgressInfo,
            palette: DayProgressPreferences.DayProgressThemeColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_card)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setInt(R.id.iv_card_bg, "setColorFilter", palette.pillBgColor)
            views.setInt(R.id.iv_card_bg, "setImageAlpha", Color.alpha(palette.pillBgColor))

            views.setTextViewText(R.id.tv_card_day_of_week, info.dayOfWeek)
            views.setTextColor(R.id.tv_card_day_of_week, palette.textColor)

            views.setTextViewText(R.id.tv_card_date, info.dateFormatted)
            views.setTextColor(R.id.tv_card_date, palette.subTextColor)

            views.setTextViewText(R.id.tv_card_phase, info.phaseTitle)
            views.setImageViewResource(R.id.iv_card_phase_icon, info.phaseIconResId)

            views.setTextViewText(R.id.tv_card_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_card_percentage, palette.textColor)
            views.setTextColor(R.id.tv_card_label, palette.subTextColor)

            // Render slim elegant curved horizontal wave with milestone labels directly under nodes
            val density = context.resources.displayMetrics.density
            val curveW = ((widthDp - 36) * density).toInt().coerceAtLeast((220 * density).toInt())
            val curveH = (58 * density).toInt()
            val curveBitmap = DayProgressIndicatorRenderer.drawCurvedHorizontalBitmap(
                context = context,
                widthPx = curveW,
                heightPx = curveH,
                progress = info.progress,
                sunriseProgress = info.sunriseProgress,
                sunsetProgress = info.sunsetProgress,
                sunriseTimeStr = info.sunriseFormatted,
                sunsetTimeStr = info.sunsetFormatted,
                isDaylight = info.isDaylight,
                trackColor = palette.textColor,
                progressColor = palette.accentColor,
                strokeWidthPx = 3.6f * density,
                progressStartColor = palette.progressStartColor,
                progressEndColor = palette.progressEndColor
            )
            views.setImageViewBitmap(R.id.iv_card_curved_progress, curveBitmap)

            // Center Summary
            views.setTextViewText(R.id.tv_card_center_summary, DayProgressRepository.formatElapsedSummary(context, info))

            // Bottom Chips: Remaining Time & Daylight Duration
            views.setTextViewText(R.id.tv_card_time_remaining, info.remainingTimeText)
            views.setTextViewText(R.id.tv_card_daylight, info.daylightDurationText)

            return views
        }

        fun buildTallRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            info: DayProgressInfo,
            palette: DayProgressPreferences.DayProgressThemeColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_tall)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setInt(R.id.iv_tall_bg, "setColorFilter", palette.pillBgColor)
            views.setInt(R.id.iv_tall_bg, "setImageAlpha", Color.alpha(palette.pillBgColor))

            views.setTextViewText(R.id.tv_tall_date, info.fullDateString)
            views.setTextColor(R.id.tv_tall_date, palette.textColor)

            views.setTextViewText(R.id.tv_tall_phase, info.phaseTitle)
            views.setImageViewResource(R.id.iv_tall_phase_icon, info.phaseIconResId)

            views.setTextViewText(R.id.tv_tall_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_tall_percentage, palette.textColor)

            views.setTextViewText(R.id.tv_tall_summary, "${DayProgressRepository.formatElapsedSummary(context, info)} • ${info.remainingTimeText}")
            views.setTextColor(R.id.tv_tall_summary, palette.subTextColor)

            // Render slim elegant curved horizontal wave with milestone labels directly under nodes
            val density = context.resources.displayMetrics.density
            val curveW = ((widthDp - 40) * density).toInt().coerceAtLeast((240 * density).toInt())
            val curveH = (62 * density).toInt()
            val curveBitmap = DayProgressIndicatorRenderer.drawCurvedHorizontalBitmap(
                context = context,
                widthPx = curveW,
                heightPx = curveH,
                progress = info.progress,
                sunriseProgress = info.sunriseProgress,
                sunsetProgress = info.sunsetProgress,
                sunriseTimeStr = info.sunriseFormatted,
                sunsetTimeStr = info.sunsetFormatted,
                isDaylight = info.isDaylight,
                trackColor = palette.textColor,
                progressColor = palette.accentColor,
                strokeWidthPx = 3.8f * density,
                progressStartColor = palette.progressStartColor,
                progressEndColor = palette.progressEndColor
            )
            views.setImageViewBitmap(R.id.iv_tall_curved_progress, curveBitmap)

            // Bottom 3 Solar Cards
            views.setTextViewText(R.id.tv_tall_sunrise, info.sunriseFormatted)
            views.setTextViewText(R.id.tv_tall_daylight_duration, info.daylightDurationText)
            views.setTextViewText(R.id.tv_tall_sunset, info.sunsetFormatted)

            return views
        }
    }
}
