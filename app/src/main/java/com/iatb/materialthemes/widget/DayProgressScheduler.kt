package com.iatb.materialthemes.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object DayProgressScheduler {

    private const val TAG = "DayProgressScheduler"
    const val ACTION_DAY_PROGRESS_TICK = "com.iatb.materialthemes.action.DAY_PROGRESS_TICK"
    private const val REQUEST_CODE = 9924

    fun scheduleNextMinuteTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DayProgressWidgetProvider::class.java).apply {
            action = ACTION_DAY_PROGRESS_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Calculate top of next minute (00s, 000ms)
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var triggerAtMillis = calendar.timeInMillis
        if (triggerAtMillis <= now + 1000L) {
            calendar.add(Calendar.MINUTE, 1)
            triggerAtMillis = calendar.timeInMillis
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled day progress tick in ${(triggerAtMillis - now) / 1000}s (at ${calendar.time})")
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm permission not granted, falling back to inexact alarm", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelSchedule(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DayProgressWidgetProvider::class.java).apply {
            action = ACTION_DAY_PROGRESS_TICK
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
