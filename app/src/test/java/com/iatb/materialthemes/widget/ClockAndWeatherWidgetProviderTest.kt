package com.iatb.materialthemes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetSize
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ClockAndWeatherWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var appWidgetManager: AppWidgetManager

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        appWidgetManager = AppWidgetManager.getInstance(context)
    }

    @Test
    fun diagonalWidgetProvider_buildRemoteViews_returnsSingleRemoteView() {
        val shadowManager = shadowOf(appWidgetManager)
        val widgetId = shadowManager.createWidget(DiagonalWidgetProvider::class.java, R.layout.widget_canvas_container)

        val remoteViews = DiagonalWidgetProvider.buildRemoteViews(context, appWidgetManager, widgetId)
        assertNotNull(remoteViews)

        // Ensure updateAppWidget succeeds without IllegalArgumentException / memory crash
        DiagonalWidgetProvider.updateAppWidget(context, appWidgetManager, widgetId)
    }

    @Test
    fun organicWidgetProvider_buildRemoteViews_returnsSingleRemoteView() {
        val shadowManager = shadowOf(appWidgetManager)
        val widgetId = shadowManager.createWidget(OrganicWidgetProvider::class.java, R.layout.widget_canvas_container)

        val remoteViews = OrganicWidgetProvider.buildRemoteViews(context, appWidgetManager, widgetId)
        assertNotNull(remoteViews)

        OrganicWidgetProvider.updateAppWidget(context, appWidgetManager, widgetId)
    }

    @Test
    fun scallopWidgetProvider_buildRemoteViews_returnsSingleRemoteView() {
        val shadowManager = shadowOf(appWidgetManager)
        val widgetId = shadowManager.createWidget(ScallopWidgetProvider::class.java, R.layout.widget_canvas_container)

        val remoteViews = ScallopWidgetProvider.buildRemoteViews(context, appWidgetManager, widgetId)
        assertNotNull(remoteViews)

        ScallopWidgetProvider.updateAppWidget(context, appWidgetManager, widgetId)
    }

    @Test
    fun widgetProviders_onConfigurationChanged_updatesWithoutCrash() {
        val shadowManager = shadowOf(appWidgetManager)
        val dId = shadowManager.createWidget(DiagonalWidgetProvider::class.java, R.layout.widget_canvas_container)
        val oId = shadowManager.createWidget(OrganicWidgetProvider::class.java, R.layout.widget_canvas_container)
        val sId = shadowManager.createWidget(ScallopWidgetProvider::class.java, R.layout.widget_canvas_container)

        val intent = Intent(Intent.ACTION_CONFIGURATION_CHANGED)
        val provider = DiagonalWidgetProvider()
        // Should execute updateAllWidgets for all registered clock & weather providers without crashing
        provider.onReceive(context, intent)
    }

    @Test
    fun widgetProviders_onAppWidgetOptionsChanged_updatesSuccessfully() {
        val shadowManager = shadowOf(appWidgetManager)
        val widgetId = shadowManager.createWidget(DiagonalWidgetProvider::class.java, R.layout.widget_canvas_container)

        val options = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 300)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 200)
        }

        val provider = DiagonalWidgetProvider()
        provider.onAppWidgetOptionsChanged(context, appWidgetManager, widgetId, options)

        val savedSize = com.iatb.materialthemes.data.WidgetPreferences.getWidgetSavedSize(context, widgetId)
        assertNotNull(savedSize)
    }
}
