package com.iatb.materialthemes.data

import android.Manifest
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.iatb.materialthemes.R

object BatteryRepository {

    private val deviceBatteryCache = java.util.concurrent.ConcurrentHashMap<String, Int>()

    fun updateCachedBatteryLevel(address: String, level: Int) {
        if (level in 0..100) {
            deviceBatteryCache[address] = level
        }
    }

    fun hasBluetoothPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getBatteryDevices(
        context: Context,
        forceCharging: Boolean? = null,
        batteryIntent: Intent? = null
    ): List<BatteryDeviceItem> {
        val result = mutableListOf<BatteryDeviceItem>()

        // 1. Phone Battery
        val phoneBattery = getPhoneBattery(context, forceCharging, batteryIntent)
        result.add(phoneBattery)

        // 2. Connected Bluetooth Accessories
        if (hasBluetoothPermission(context)) {
            val btDevices = getConnectedBluetoothDevices(context)
            result.addAll(btDevices)
        }

        return result
    }

    private fun getPhoneBattery(
        context: Context,
        forceCharging: Boolean? = null,
        batteryIntent: Intent? = null
    ): BatteryDeviceItem {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val bmCapacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val bmIsCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bm?.isCharging
        } else null
        val bmStatus = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1

        val actualIntent = batteryIntent ?: try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.registerReceiver(
                    context,
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                    ContextCompat.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }
        } catch (_: Exception) {
            null
        }

        val lvl = actualIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = actualIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val level = if (lvl >= 0 && scale > 0) {
            (lvl * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
        } else if (bmCapacity in 0..100) {
            bmCapacity
        } else {
            100
        }

        val status = if (bmStatus != -1 && bmStatus != 0) {
            bmStatus
        } else {
            batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        }

        val plugged = batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
        val isCharging = forceCharging ?: (
            if (plugged > 0) {
                true
            } else if (plugged == 0) {
                false
            } else {
                bmIsCharging ?: (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
            }
        )

        var statusText: String? = null
        if (isCharging && (status == BatteryManager.BATTERY_STATUS_FULL || level >= 100)) {
            statusText = context.getString(R.string.battery_status_full)
        } else if (isCharging) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    val remainingMs = bm?.computeChargeTimeRemaining() ?: -1L
                    if (remainingMs > 0) {
                        val totalMinutes = remainingMs / (1000 * 60)
                        val hours = totalMinutes / 60
                        val mins = totalMinutes % 60
                        val timeStr = if (hours > 0) {
                            context.getString(R.string.battery_time_hours_mins, hours, mins)
                        } else {
                            context.getString(R.string.battery_time_mins, mins)
                        }
                        statusText = context.getString(R.string.battery_time_remaining_charge, timeStr)
                    }
                } catch (_: Exception) {
                    // Fall back to simple charging label
                }
            }
            if (statusText == null) {
                statusText = context.getString(R.string.battery_status_charging)
            }
        } else {
            // Track battery discharge rate
            DischargeTracker.onBatteryLevelChanged(context, level, isCharging = false)

            // Not charging: Read OS-provided usage time remaining if available
            val remainingMs = getBatteryUsageTimeRemainingMs(context, level)
            if (remainingMs > 0) {
                val totalMinutes = remainingMs / (1000 * 60)
                val hours = totalMinutes / 60
                val mins = totalMinutes % 60
                val timeStr = if (hours > 0) {
                    context.getString(R.string.battery_time_hours_mins, hours, mins)
                } else {
                    context.getString(R.string.battery_time_mins, mins)
                }
                statusText = context.getString(R.string.battery_time_remaining_usage, timeStr)
            } else {
                statusText = context.getString(R.string.battery_status_discharging)
            }
        }

        return BatteryDeviceItem(
            id = "device_phone",
            name = context.getString(R.string.battery_title_phone),
            type = BatteryDeviceType.PHONE,
            levelPercent = level,
            isCharging = isCharging,
            statusText = statusText
        )
    }

    private fun getConnectedBluetoothDevices(context: Context): List<BatteryDeviceItem> {
        val result = mutableListOf<BatteryDeviceItem>()
        try {
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter ?: return emptyList()
            if (!adapter.isEnabled) return emptyList()

            val bondedDevices = adapter.bondedDevices ?: return emptyList()
            for (device in bondedDevices) {
                val batteryLevel = getDeviceBatteryLevel(device)
                val isConnected = isDeviceConnected(device)

                // Include device if it is reported as connected, or has a valid battery reading
                if (isConnected || batteryLevel in 0..100) {
                    val type = classifyDevice(device)
                    val deviceName = try {
                        device.name ?: device.address
                    } catch (_: SecurityException) {
                        device.address
                    }

                    result.add(
                        BatteryDeviceItem(
                            id = device.address,
                            name = deviceName,
                            type = type,
                            levelPercent = if (batteryLevel in 0..100) batteryLevel else -1,
                            isCharging = false,
                            statusText = null
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Graceful fallback for permission or hardware issues
        }
        return result
    }

    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    private fun getDeviceBatteryLevel(device: BluetoothDevice): Int {
        val cached = deviceBatteryCache[device.address]
        if (cached != null && cached in 0..100) return cached

        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) {
                deviceBatteryCache[device.address] = level
                level
            } else -1
        } catch (_: Exception) {
            -1
        }
    }

    private fun classifyDevice(device: BluetoothDevice): BatteryDeviceType {
        return try {
            val btClass = device.bluetoothClass ?: return BatteryDeviceType.GENERIC_BLUETOOTH
            val major = btClass.majorDeviceClass
            val devClass = btClass.deviceClass

            when {
                devClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES ||
                        devClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET -> {
                    BatteryDeviceType.HEADPHONES
                }
                devClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER ||
                        devClass == BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX -> {
                    BatteryDeviceType.SPEAKER
                }
                devClass == BluetoothClass.Device.WEARABLE_WRIST_WATCH ||
                        major == BluetoothClass.Device.Major.WEARABLE -> {
                    BatteryDeviceType.WATCH
                }
                major == BluetoothClass.Device.Major.AUDIO_VIDEO -> {
                    BatteryDeviceType.HEADPHONES
                }
                else -> BatteryDeviceType.GENERIC_BLUETOOTH
            }
        } catch (_: Exception) {
            BatteryDeviceType.GENERIC_BLUETOOTH
        }
    }

    fun hasBatteryStatsPermission(context: Context): Boolean {
        val hasManifest = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BATTERY_STATS
        ) == PackageManager.PERMISSION_GRANTED

        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
        val hasUsage = try {
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps?.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps?.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    context.packageName
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }

        return hasManifest || hasUsage
    }

    fun getBatteryUsageTimeRemainingMs(context: Context, level: Int): Long {
        // 1. Try OS BatteryStatsManager API 31+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val bsm = context.getSystemService("batterystats")
                if (bsm != null) {
                    val queryClass = Class.forName("android.os.BatteryUsageStatsQuery")
                    val builderClass = Class.forName("android.os.BatteryUsageStatsQuery\$Builder")
                    val builder = builderClass.getConstructor().newInstance()
                    val query = builderClass.getMethod("build").invoke(builder)
                    val statsMethod = bsm.javaClass.getMethod("getBatteryUsageStats", queryClass)
                    val usageStats = statsMethod.invoke(bsm, query)
                    if (usageStats != null) {
                        val timeMethod = usageStats.javaClass.getMethod("getBatteryTimeRemainingMs")
                        val remainingMs = timeMethod.invoke(usageStats) as? Long ?: -1L
                        if (remainingMs > 0) return remainingMs
                    }
                }
            } catch (_: Exception) {}
        }

        // 2. If permission is granted or active, compute from discharge tracking
        if (hasBatteryStatsPermission(context)) {
            val minsPerPercent = DischargeTracker.getAverageMinutesPerPercent(context)
            if (level > 0 && minsPerPercent > 0) {
                return (level * minsPerPercent * 60 * 1000L).toLong()
            }
        }

        return -1L
    }
}

object DischargeTracker {
    private const val PREFS_NAME = "battery_discharge_tracker"
    private const val KEY_LAST_LEVEL = "last_level"
    private const val KEY_LAST_TIMESTAMP = "last_timestamp"
    private const val KEY_AVG_MINS_PER_PERCENT = "avg_mins_per_percent"

    fun onBatteryLevelChanged(context: Context, newLevel: Int, isCharging: Boolean) {
        if (isCharging || newLevel <= 0 || newLevel > 100) return

        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastLevel = sp.getInt(KEY_LAST_LEVEL, -1)
        val lastTime = sp.getLong(KEY_LAST_TIMESTAMP, -1L)
        val now = System.currentTimeMillis()

        if (lastLevel > newLevel && lastTime > 0) {
            val levelDiff = lastLevel - newLevel
            val timeDiffMins = (now - lastTime) / (1000 * 60)
            if (timeDiffMins in 1..180) {
                val minsPerPercent = timeDiffMins / levelDiff.toFloat()
                val currentAvg = sp.getFloat(KEY_AVG_MINS_PER_PERCENT, 10.5f)
                val newAvg = (currentAvg * 0.7f + minsPerPercent * 0.3f).coerceIn(4f, 25f)
                sp.edit()
                    .putFloat(KEY_AVG_MINS_PER_PERCENT, newAvg)
                    .putInt(KEY_LAST_LEVEL, newLevel)
                    .putLong(KEY_LAST_TIMESTAMP, now)
                    .apply()
                return
            }
        }

        sp.edit()
            .putInt(KEY_LAST_LEVEL, newLevel)
            .putLong(KEY_LAST_TIMESTAMP, now)
            .apply()
    }

    fun getAverageMinutesPerPercent(context: Context): Float {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getFloat(KEY_AVG_MINS_PER_PERCENT, 10.5f)
    }
}
