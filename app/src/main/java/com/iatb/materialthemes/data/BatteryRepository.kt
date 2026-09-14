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

    fun getBatteryDevices(context: Context, forceCharging: Boolean? = null): List<BatteryDeviceItem> {
        val result = mutableListOf<BatteryDeviceItem>()

        // 1. Phone Battery
        val phoneBattery = getPhoneBattery(context, forceCharging)
        result.add(phoneBattery)

        // 2. Connected Bluetooth Accessories
        if (hasBluetoothPermission(context)) {
            val btDevices = getConnectedBluetoothDevices(context)
            result.addAll(btDevices)
        }

        return result
    }

    private fun getPhoneBattery(context: Context, forceCharging: Boolean? = null): BatteryDeviceItem {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val bmCapacity = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        val bmIsCharging = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bm?.isCharging
        } else null
        val bmStatus = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) ?: -1

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryIntent = context.registerReceiver(null, intentFilter)

        val level = if (bmCapacity in 0..100) {
            bmCapacity
        } else {
            val lvl = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (lvl >= 0 && scale > 0) {
                (lvl * 100 / scale.toFloat()).toInt().coerceIn(0, 100)
            } else {
                100
            }
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
            // Not charging: Read OS-provided usage time remaining if available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val bsm = context.getSystemService("batterystats")
                    if (bsm != null) {
                        val statsMethod = bsm.javaClass.getMethod("getBatteryUsageStats")
                        val usageStats = statsMethod.invoke(bsm)
                        if (usageStats != null) {
                            val timeMethod = usageStats.javaClass.getMethod("getBatteryTimeRemainingMs")
                            val remainingMs = timeMethod.invoke(usageStats) as? Long ?: -1L
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
                            }
                        }
                    }
                } catch (_: Exception) {
                    // Protected permission or unavailable on ROM
                }
            }
            if (statusText == null) {
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
        return try {
            val method = device.javaClass.getMethod("getBatteryLevel")
            val level = method.invoke(device) as? Int ?: -1
            if (level in 0..100) level else -1
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
}
