package com.iatb.materialthemes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
            val remoteViews = buildRemoteViews(context, appWidgetManager, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ): RemoteViews {
            val info = DayProgressRepository.getDayProgress(context)
            val colors = DayProgressPreferences.resolveColors(context)

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
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)

            return if (minH < 95) {
                buildRowRemoteViews(context, minW, minH, info, colors, appWidgetId)
            } else if (minW <= 150 && minH <= 150) {
                buildCompactRemoteViews(context, minW, minH, info, colors, appWidgetId)
            } else if (minH < 180) {
                buildCardRemoteViews(context, minW, minH, info, colors, appWidgetId)
            } else {
                buildTallRemoteViews(context, minW, minH, info, colors, appWidgetId)
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
            palette: ResolvedPaletteColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_row)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setInt(R.id.iv_row_card_bg, "setColorFilter", palette.secondaryBgColor)
            views.setInt(R.id.iv_row_card_bg, "setImageAlpha", Color.alpha(palette.secondaryBgColor))

            views.setTextViewText(R.id.tv_row_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_row_percentage, palette.textColor)

            views.setTextViewText(R.id.tv_row_date, info.fullDateString)
            views.setTextColor(R.id.tv_row_date, palette.textColor)

            // Render organic curved horizontal indicator bitmap
            val density = context.resources.displayMetrics.density
            val curveW = ((widthDp - 100) * density).toInt().coerceAtLeast((120 * density).toInt())
            val curveH = (32 * density).toInt()

            val trackColor = androidx.core.graphics.ColorUtils.setAlphaComponent(palette.textColor, 36)
            val progressColor = palette.textColor
            val curveBitmap = DayProgressIndicatorRenderer.drawCurvedHorizontalBitmap(
                curveW, curveH, info.progress, trackColor, progressColor, strokeWidthPx = 7f * density
            )
            views.setImageViewBitmap(R.id.iv_row_curved_progress, curveBitmap)

            return views
        }

        fun buildCompactRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            info: DayProgressInfo,
            palette: ResolvedPaletteColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_compact)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setTextViewText(R.id.tv_compact_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_compact_percentage, palette.textColor)

            views.setTextViewText(R.id.tv_compact_date, info.dateFormatted)
            views.setTextColor(R.id.tv_compact_date, palette.textColor)

            val density = context.resources.displayMetrics.density
            val arcSize = (74 * density).toInt()
            val trackColor = androidx.core.graphics.ColorUtils.setAlphaComponent(palette.textColor, 36)
            val progressColor = palette.textColor

            val arcBitmap = DayProgressIndicatorRenderer.drawCircularArcBitmap(
                arcSize, info.progress, trackColor, progressColor, strokeWidthPx = 6.5f * density, isFullCircle = true
            )
            views.setImageViewBitmap(R.id.iv_compact_circular_progress, arcBitmap)

            return views
        }

        fun buildCardRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            info: DayProgressInfo,
            palette: ResolvedPaletteColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_card)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setInt(R.id.iv_card_bg, "setColorFilter", palette.secondaryBgColor)
            views.setInt(R.id.iv_card_bg, "setImageAlpha", Color.alpha(palette.secondaryBgColor))

            views.setTextViewText(R.id.tv_card_day_of_week, info.dayOfWeek)
            views.setTextColor(R.id.tv_card_day_of_week, palette.textColor)

            views.setTextViewText(R.id.tv_card_date, info.dateFormatted)
            views.setTextColor(R.id.tv_card_date, palette.textColor)

            views.setTextViewText(R.id.tv_card_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_card_percentage, palette.textColor)

            views.setTextViewText(R.id.tv_card_time_summary, info.timeFormatted)
            views.setTextColor(R.id.tv_card_time_summary, palette.textColor)

            views.setTextColor(R.id.tv_card_label, palette.textColor)

            // Render curved horizontal indicator
            val density = context.resources.displayMetrics.density
            val curveW = ((widthDp - 40) * density).toInt().coerceAtLeast((180 * density).toInt())
            val curveH = (46 * density).toInt()
            val trackColor = androidx.core.graphics.ColorUtils.setAlphaComponent(palette.textColor, 36)
            val progressColor = palette.textColor

            val curveBitmap = DayProgressIndicatorRenderer.drawCurvedHorizontalBitmap(
                curveW, curveH, info.progress, trackColor, progressColor, strokeWidthPx = 8f * density
            )
            views.setImageViewBitmap(R.id.iv_card_curved_progress, curveBitmap)

            return views
        }

        fun buildTallRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            info: DayProgressInfo,
            palette: ResolvedPaletteColors,
            appWidgetId: Int = 0
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_day_progress_tall)
            attachClickIntent(context, views, appWidgetId)

            views.setInt(R.id.widget_day_progress_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_day_progress_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            views.setInt(R.id.iv_tall_bg, "setColorFilter", palette.secondaryBgColor)
            views.setInt(R.id.iv_tall_bg, "setImageAlpha", Color.alpha(palette.secondaryBgColor))

            views.setTextViewText(R.id.tv_tall_percentage, "${info.percentage}%")
            views.setTextColor(R.id.tv_tall_percentage, palette.textColor)

            views.setTextColor(R.id.tv_tall_label, palette.textColor)

            views.setTextViewText(R.id.tv_tall_time, info.timeFormatted)
            views.setTextColor(R.id.tv_tall_time, palette.textColor)

            views.setTextViewText(R.id.tv_tall_date, info.fullDateString)
            views.setTextColor(R.id.tv_tall_date, palette.textColor)

            val density = context.resources.displayMetrics.density
            val curveW = ((widthDp - 50) * density).toInt().coerceAtLeast((200 * density).toInt())
            val curveH = (52 * density).toInt()
            val trackColor = androidx.core.graphics.ColorUtils.setAlphaComponent(palette.textColor, 36)
            val progressColor = palette.textColor

            val curveBitmap = DayProgressIndicatorRenderer.drawCurvedHorizontalBitmap(
                curveW, curveH, info.progress, trackColor, progressColor, strokeWidthPx = 9f * density
            )
            views.setImageViewBitmap(R.id.iv_tall_curved_progress, curveBitmap)

            return views
        }
    }
}
