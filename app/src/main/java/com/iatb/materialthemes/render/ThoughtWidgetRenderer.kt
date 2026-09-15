package com.iatb.materialthemes.render

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.data.SeasonTimePaletteResolver
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.WidgetStateEntity
import com.iatb.materialthemes.ui.ThoughtWidgetDetailActivity

object ThoughtWidgetRenderer {

    fun render(
        context: Context,
        appWidgetId: Int,
        content: ContentEntity,
        state: WidgetStateEntity? = null,
        minWidthDp: Int = 180,
        minHeightDp: Int = 110
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_thought)

        // Extract dynamic colors (Material You wallpaper / Seasonal solar palette)
        val dynamicPalette = DynamicThemeExtractor.getDynamicPalette(context)
        val seasonal = SeasonTimePaletteResolver.resolveSeasonalPalette(
            nowMillis = System.currentTimeMillis(),
            sunriseMillis = 0L,
            sunsetMillis = 0L
        )

        val cardBgColor = dynamicPalette.bgColor
        val textColor = dynamicPalette.textColor
        val subTextColor = seasonal.subTextColor
        val accentColor = seasonal.accentColor

        // 1. Tint Card Surface
        views.setInt(R.id.widget_thought_bg, "setColorFilter", cardBgColor)

        // 2. Tint Thought Spark Icon
        views.setInt(R.id.ivThoughtIcon, "setColorFilter", accentColor)

        // 3. Set Thought Reflective Text & Sizing
        views.setTextViewText(R.id.tvThoughtText, content.text)
        views.setTextColor(R.id.tvThoughtText, textColor)

        if (minHeightDp < 95 || content.text.length > 200) {
            views.setTextViewTextSize(R.id.tvThoughtText, TypedValue.COMPLEX_UNIT_SP, 13f)
        } else if (minWidthDp >= 260 && minHeightDp >= 160) {
            views.setTextViewTextSize(R.id.tvThoughtText, TypedValue.COMPLEX_UNIT_SP, 17f)
        } else {
            views.setTextViewTextSize(R.id.tvThoughtText, TypedValue.COMPLEX_UNIT_SP, 15f)
        }

        // 4. Category Chip (Display when widget has sufficient width)
        if (minWidthDp >= 210 && !content.category.isNullOrBlank()) {
            views.setTextViewText(R.id.tvThoughtCategory, content.category)
            views.setTextColor(R.id.tvThoughtCategory, subTextColor)
            views.setViewVisibility(R.id.tvThoughtCategory, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.tvThoughtCategory, View.GONE)
        }

        // 5. Click Handler -> Opens ThoughtWidgetDetailActivity
        val clickIntent = Intent(context, ThoughtWidgetDetailActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_thought_root, pendingIntent)

        return views
    }
}
