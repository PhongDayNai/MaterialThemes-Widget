package com.iatb.materialthemes.widget

import android.content.Context
import com.iatb.materialthemes.data.WidgetPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

object WidgetAnimationManager {

    const val ACTION_RUN_ENTER_ANIMATION = "com.iatb.materialthemes.ACTION_RUN_ENTER_ANIMATION"

    private var animationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun triggerEnterAnimation(context: Context, isFull: Boolean) {
        animationJob?.cancel()
        animationJob = scope.launch {
            val frames = if (isFull) {
                floatArrayOf(0.25f, 0.60f, 0.85f, 1.02f, 1.0f)
            } else {
                floatArrayOf(0.70f, 0.90f, 1.0f)
            }
            val frameDelay = if (isFull) 55L else 45L

            for (progress in frames) {
                updateAllWidgetsWithProgress(context, progress)
                delay(frameDelay)
            }
            // Final static pass to guarantee clean final frame
            updateAllWidgetsWithProgress(context, 1.0f)
        }
    }

    fun handleUserPresent(context: Context) {
        val now = System.currentTimeMillis()
        val lastTime = WidgetPreferences.getLastAnimTimestamp(context)

        val nowCal = Calendar.getInstance().apply { timeInMillis = now }
        val lastCal = Calendar.getInstance().apply { timeInMillis = lastTime }

        val isFirstUnlockToday = lastTime == 0L ||
                nowCal.get(Calendar.YEAR) != lastCal.get(Calendar.YEAR) ||
                nowCal.get(Calendar.DAY_OF_YEAR) != lastCal.get(Calendar.DAY_OF_YEAR)

        val isCooldownPassed = (now - lastTime) >= 60 * 60 * 1000L

        when {
            isFirstUnlockToday -> {
                WidgetPreferences.setLastAnimTimestamp(context, now)
                triggerEnterAnimation(context, isFull = true)
            }
            isCooldownPassed -> {
                WidgetPreferences.setLastAnimTimestamp(context, now)
                triggerEnterAnimation(context, isFull = false)
            }
            else -> {
                updateAllWidgetsWithProgress(context, 1.0f)
            }
        }
    }

    fun updateAllWidgetsWithProgress(context: Context, animProgress: Float) {
        DiagonalWidgetProvider.updateAllWidgets(context, animProgress)
        OrganicWidgetProvider.updateAllWidgets(context, animProgress)
        ScallopWidgetProvider.updateAllWidgets(context, animProgress)
    }
}
