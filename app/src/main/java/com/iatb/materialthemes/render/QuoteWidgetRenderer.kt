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
import com.iatb.materialthemes.ui.QuoteWidgetDetailActivity

object QuoteWidgetRenderer {

    fun render(
        context: Context,
        appWidgetId: Int,
        content: ContentEntity,
        state: WidgetStateEntity? = null,
        minWidthDp: Int = 180,
        minHeightDp: Int = 110
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_quote)

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
        views.setInt(R.id.widget_quote_bg, "setColorFilter", cardBgColor)

        // 2. Tint Quote Mark Icon
        views.setInt(R.id.ivQuoteIcon, "setColorFilter", accentColor)

        // 3. Set Quote Text & Sizing
        views.setTextViewText(R.id.tvQuoteText, content.text)
        views.setTextColor(R.id.tvQuoteText, textColor)

        if (minHeightDp < 95 || content.text.length > 200) {
            views.setTextViewTextSize(R.id.tvQuoteText, TypedValue.COMPLEX_UNIT_SP, 13f)
        } else if (minWidthDp >= 260 && minHeightDp >= 160) {
            views.setTextViewTextSize(R.id.tvQuoteText, TypedValue.COMPLEX_UNIT_SP, 17f)
        } else {
            views.setTextViewTextSize(R.id.tvQuoteText, TypedValue.COMPLEX_UNIT_SP, 15f)
        }

        // 4. Set Author Attribution
        val authorText = if (!content.author.isNullOrBlank()) {
            "— ${content.author}"
        } else {
            ""
        }
        views.setTextViewText(R.id.tvQuoteAuthor, authorText)
        views.setTextColor(R.id.tvQuoteAuthor, subTextColor)

        // 5. Category Chip (Display when widget has sufficient width)
        if (minWidthDp >= 210 && !content.category.isNullOrBlank()) {
            views.setTextViewText(R.id.tvQuoteCategory, content.category)
            views.setTextColor(R.id.tvQuoteCategory, subTextColor)
            views.setViewVisibility(R.id.tvQuoteCategory, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.tvQuoteCategory, View.GONE)
        }

        // 6. Click Handler -> Opens QuoteWidgetDetailActivity
        val clickIntent = Intent(context, QuoteWidgetDetailActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_quote_root, pendingIntent)

        return views
    }
}
