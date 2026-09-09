package com.iatb.materialthemes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.iatb.materialthemes.MainActivity
import com.iatb.materialthemes.R

class OrganicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
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
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Multi-size RemoteViews for Android 12+ (API 31+)
                val viewMapping = mapOf(
                    SizeF(110f, 110f) to createViews(context, R.layout.widget_organic_2x2),
                    SizeF(200f, 110f) to createViews(context, R.layout.widget_organic_4x2),
                    SizeF(110f, 180f) to createViews(context, R.layout.widget_organic_2x3),
                    SizeF(200f, 200f) to createViews(context, R.layout.widget_organic_3x3)
                )
                RemoteViews(viewMapping)
            } else {
                // Fallback for Android < 12
                val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
                val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
                val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
                val layoutId = resolveLayout(minWidth, minHeight)
                createViews(context, layoutId)
            }
        }

        private fun resolveLayout(minWidth: Int, minHeight: Int): Int {
            return when {
                minWidth >= 180 && minHeight < 160 -> R.layout.widget_organic_4x2
                minWidth >= 180 && minHeight >= 160 -> R.layout.widget_organic_3x3
                minWidth < 160 && minHeight >= 160 -> R.layout.widget_organic_2x3
                else -> R.layout.widget_organic_2x2
            }
        }

        private fun createViews(context: Context, layoutId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, layoutId)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.widget_organic_root, pendingIntent)
            return views
        }
    }
}
