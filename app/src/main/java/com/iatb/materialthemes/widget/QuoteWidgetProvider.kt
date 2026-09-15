package com.iatb.materialthemes.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.repository.ContentRepositoryImpl
import com.iatb.materialthemes.data.content.state.WidgetStateManager
import com.iatb.materialthemes.render.QuoteWidgetRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QuoteWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_WALLPAPER_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            ACTION_UPDATE_QUOTE_WIDGET -> {
                DynamicThemeExtractor.invalidateCache()
                updateAllWidgets(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    renderWidget(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                renderWidget(context, appWidgetManager, appWidgetId, newOptions)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stateManager = WidgetStateManager.getInstance(context)
                for (appWidgetId in appWidgetIds) {
                    stateManager.deleteWidget(appWidgetId)
                }
            } finally {
                pendingResult?.finish()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_QUOTE_WIDGET = "com.iatb.materialthemes.action.UPDATE_QUOTE_WIDGET"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, QuoteWidgetProvider::class.java)
            val ids = try {
                appWidgetManager.getAppWidgetIds(component)
            } catch (_: Exception) {
                IntArray(0)
            }
            if (ids.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                for (id in ids) {
                    renderWidget(context, appWidgetManager, id)
                }
            }
        }

        suspend fun renderWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            options: Bundle? = null
        ) {
            val stateManager = WidgetStateManager.getInstance(context)
            val repository = ContentRepositoryImpl.getInstance(context)

            // Invariant: Always get or initialize existing state; DO NOT rotate content on redraw/recreate
            val state = stateManager.getState(appWidgetId)
                ?: stateManager.createInitialState(appWidgetId, ContentType.QUOTE)

            val content = state.contentId?.let { repository.getContentById(it) }
                ?: repository.getFallbackContent(ContentType.QUOTE, state.languageFilter)

            val widgetOptions = options ?: try {
                appWidgetManager.getAppWidgetOptions(appWidgetId)
            } catch (_: Exception) {
                null
            }
            val minWidthDp = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180) ?: 180
            val minHeightDp = widgetOptions?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110) ?: 110

            val remoteViews = QuoteWidgetRenderer.render(
                context = context,
                appWidgetId = appWidgetId,
                content = content,
                state = state,
                minWidthDp = minWidthDp,
                minHeightDp = minHeightDp
            )

            try {
                appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            } catch (_: Exception) {
            }
        }
    }
}
