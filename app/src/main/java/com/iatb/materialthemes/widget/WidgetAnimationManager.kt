package com.iatb.materialthemes.widget

import android.content.Context

object WidgetAnimationManager {

    const val ACTION_RUN_ENTER_ANIMATION = "com.iatb.materialthemes.ACTION_RUN_ENTER_ANIMATION"

    /**
     * Home screen frame-by-frame animation is temporarily disabled per user request.
     * Directly updates all widgets to the final static frame (1.0f) in a single fast pass.
     */
    fun triggerEnterAnimation(context: Context, isFull: Boolean = true) {
        updateAllWidgetsWithProgress(context, 1.0f)
    }

    fun handleUserPresent(context: Context) {
        updateAllWidgetsWithProgress(context, 1.0f)
    }

    fun updateAllWidgetsWithProgress(context: Context, animProgress: Float = 1.0f) {
        DiagonalWidgetProvider.updateAllWidgets(context, animProgress)
        OrganicWidgetProvider.updateAllWidgets(context, animProgress)
        ScallopWidgetProvider.updateAllWidgets(context, animProgress)
        DiagonalWideWidgetProvider.updateAllWidgets(context, animProgress)
        OrganicWideWidgetProvider.updateAllWidgets(context, animProgress)
        ScallopWideWidgetProvider.updateAllWidgets(context, animProgress)
        Diagonal4x3WidgetProvider.updateAllWidgets(context, animProgress)
        Organic4x3WidgetProvider.updateAllWidgets(context, animProgress)
        Scallop4x3WidgetProvider.updateAllWidgets(context, animProgress)
    }
}
