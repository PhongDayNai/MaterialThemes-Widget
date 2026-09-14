package com.iatb.materialthemes

import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import com.iatb.materialthemes.data.DynamicThemeExtractor
import com.iatb.materialthemes.widget.BatteryWidgetProvider
import com.iatb.materialthemes.widget.DiagonalWidgetProvider
import com.iatb.materialthemes.widget.OrganicWidgetProvider
import com.iatb.materialthemes.widget.ScallopWidgetProvider

class MaterialThemesApp : Application() {

    private val batteryWidgetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    DynamicThemeExtractor.invalidateCache()
                    BatteryWidgetProvider.updateAllWidgets(context, forceCharging = true)
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    DynamicThemeExtractor.invalidateCache()
                    BatteryWidgetProvider.updateAllWidgets(context, forceCharging = false)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val isCharging = if (plugged > 0) {
                        true
                    } else if (plugged == 0) {
                        false
                    } else if (status != -1) {
                        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                    } else null
                    DynamicThemeExtractor.invalidateCache()
                    BatteryWidgetProvider.updateAllWidgets(context, forceCharging = isCharging)
                }
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED",
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_USER_PRESENT -> {
                    DynamicThemeExtractor.invalidateCache()
                    BatteryWidgetProvider.updateAllWidgets(context)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Listen for real-time wallpaper color changes (API 27+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                val wm = getSystemService(WallpaperManager::class.java)
                    ?: WallpaperManager.getInstance(this)
                wm?.addOnColorsChangedListener({ colors, _ ->
                    DynamicThemeExtractor.onWallpaperColorsChanged(colors)
                    DiagonalWidgetProvider.updateAllWidgets(this)
                    OrganicWidgetProvider.updateAllWidgets(this)
                    ScallopWidgetProvider.updateAllWidgets(this)
                    BatteryWidgetProvider.updateAllWidgets(this)
                }, Handler(Looper.getMainLooper()))
            } catch (_: Exception) {
            }
        }

        // 2. Listen for real-time system theme / configuration changes (Monet, Dark/Light mode)
        registerComponentCallbacks(object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) {
                DynamicThemeExtractor.invalidateCache()
                DiagonalWidgetProvider.updateAllWidgets(this@MaterialThemesApp)
                OrganicWidgetProvider.updateAllWidgets(this@MaterialThemesApp)
                ScallopWidgetProvider.updateAllWidgets(this@MaterialThemesApp)
                BatteryWidgetProvider.updateAllWidgets(this@MaterialThemesApp)
            }

            override fun onLowMemory() {}
        })

        // 3. Dynamic receiver for real-time battery % and charging changes (0ms latency)
        // ACTION_BATTERY_CHANGED cannot be received in manifest receivers on Android 8.0+
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        try {
            ContextCompat.registerReceiver(
                this,
                batteryWidgetReceiver,
                batteryFilter,
                ContextCompat.RECEIVER_EXPORTED
            )
        } catch (_: Exception) {
            try {
                registerReceiver(batteryWidgetReceiver, batteryFilter)
            } catch (_: Exception) {}
        }
    }
}
