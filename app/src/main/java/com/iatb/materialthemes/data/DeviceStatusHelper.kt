package com.iatb.materialthemes.data

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.iatb.materialthemes.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Helper to inspect live device status for widgets:
 * - Real alarm time if an alarm is actively scheduled.
 * - Battery percentage fallback if no alarm is set.
 */
object DeviceStatusHelper {

    /**
     * Returns a formatted status string:
     * - If an alarm is set: localized string such as "Báo thức: 07:00" / "Alarm: 7:00 AM".
     * - If no alarm is set: localized battery level such as "Pin: 85%" / "Battery: 85%".
     */
    fun getAlarmOrBatteryStatus(context: Context): String {
        val alarmStr = getNextAlarmFormatted(context)
        if (alarmStr != null) {
            return alarmStr
        }
        return getBatteryStatusFormatted(context)
    }

    /**
     * Inspects AlarmManager for the next active alarm clock.
     * Returns formatted alarm string or null if no alarm is scheduled.
     */
    fun getNextAlarmFormatted(context: Context): String? {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return null
            val nextAlarm = alarmManager.nextAlarmClock ?: return null
            val triggerTime = nextAlarm.triggerTime
            if (triggerTime <= 0L) return null

            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val pattern = if (is24Hour) "HH:mm" else "h:mm a"
            val timeFormatted = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(triggerTime))
            context.getString(R.string.status_next_alarm, timeFormatted)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns localized battery status (e.g. "Pin: 85%" or "Battery: 85%").
     */
    fun getBatteryStatusFormatted(context: Context): String {
        val batteryPct = getBatteryPercentage(context)
        return if (batteryPct != null) {
            context.getString(R.string.status_battery_level, batteryPct)
        } else {
            context.getString(R.string.status_battery_default)
        }
    }

    /**
     * Queries the sticky ACTION_BATTERY_CHANGED broadcast to get battery level.
     */
    fun getBatteryPercentage(context: Context): Int? {
        return try {
            val batteryStatus: Intent? = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100).toInt()
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
