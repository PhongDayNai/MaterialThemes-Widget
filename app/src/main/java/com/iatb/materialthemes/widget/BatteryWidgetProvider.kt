package com.iatb.materialthemes.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.bluetooth.BluetoothDevice
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.BatteryDeviceItem
import com.iatb.materialthemes.data.BatteryDeviceType
import com.iatb.materialthemes.data.BatteryPreferences
import com.iatb.materialthemes.data.BatteryRepository
import com.iatb.materialthemes.data.ResolvedPaletteColors
import com.iatb.materialthemes.ui.BatteryWidgetDetailActivity

class BatteryWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> {
                com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()
                updateAllWidgets(context, forceCharging = true)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()
                updateAllWidgets(context, forceCharging = false)
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
                com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()
                updateAllWidgets(context, forceCharging = isCharging)
                WidgetUpdateScheduler.scheduleNextMinuteTick(context)
            }
            Intent.ACTION_USER_PRESENT,
            Intent.ACTION_TIME_TICK,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_WALLPAPER_CHANGED,
            Intent.ACTION_CONFIGURATION_CHANGED,
            BluetoothDevice.ACTION_ACL_CONNECTED,
            BluetoothDevice.ACTION_ACL_DISCONNECTED,
            "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED",
            WidgetUpdateScheduler.ACTION_WIDGET_TICK,
            ACTION_UPDATE_BATTERY_WIDGET -> {
                com.iatb.materialthemes.data.DynamicThemeExtractor.invalidateCache()
                updateAllWidgets(context)
                WidgetUpdateScheduler.scheduleNextMinuteTick(context)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetUpdateScheduler.scheduleNextMinuteTick(context)
        updateAllWidgets(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        WidgetUpdateScheduler.scheduleNextMinuteTick(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    companion object {
        const val ACTION_UPDATE_BATTERY_WIDGET = "com.iatb.materialthemes.action.UPDATE_BATTERY_WIDGET"

        fun updateAllWidgets(context: Context, forceCharging: Boolean? = null) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val component = ComponentName(context, BatteryWidgetProvider::class.java)
            val ids = try {
                appWidgetManager.getAppWidgetIds(component)
            } catch (_: Exception) {
                IntArray(0)
            }
            for (id in ids) {
                updateAppWidget(context, appWidgetManager, id, forceCharging)
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            forceCharging: Boolean? = null
        ) {
            val remoteViews = buildRemoteViews(context, appWidgetManager, appWidgetId, forceCharging)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }

        fun buildRemoteViews(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            forceCharging: Boolean? = null
        ): RemoteViews {
            val devices = BatteryRepository.getBatteryDevices(context, forceCharging)
            val colors = BatteryPreferences.resolveColors(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val viewMapping = mapOf(
                    // Height x1: ~54dp to 80dp
                    SizeF(130f, 60f) to buildRowRemoteViews(context, 130, 60, devices, colors),
                    SizeF(220f, 60f) to buildRowRemoteViews(context, 220, 60, devices, colors),
                    SizeF(300f, 60f) to buildRowRemoteViews(context, 300, 60, devices, colors),
                    SizeF(380f, 60f) to buildRowRemoteViews(context, 380, 60, devices, colors),

                    // Height x2: ~110dp to 170dp
                    SizeF(130f, 130f) to buildCardRemoteViews(context, 130, 130, devices, colors),
                    SizeF(220f, 130f) to buildCardRemoteViews(context, 220, 130, devices, colors),
                    SizeF(300f, 130f) to buildCardRemoteViews(context, 300, 130, devices, colors),
                    SizeF(380f, 130f) to buildCardRemoteViews(context, 380, 130, devices, colors),

                    // Height x3/x4: ~180dp+
                    SizeF(200f, 220f) to buildTallRemoteViews(context, 200, 220, devices, colors),
                    SizeF(300f, 220f) to buildTallRemoteViews(context, 300, 220, devices, colors),
                    SizeF(380f, 220f) to buildTallRemoteViews(context, 380, 220, devices, colors)
                )
                return RemoteViews(viewMapping)
            }

            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 260)
            val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 60)

            return if (minH < 95) {
                buildRowRemoteViews(context, minW, minH, devices, colors)
            } else if (minH < 180) {
                buildCardRemoteViews(context, minW, minH, devices, colors)
            } else {
                buildTallRemoteViews(context, minW, minH, devices, colors)
            }
        }

        private fun attachClickIntent(context: Context, views: RemoteViews) {
            val clickIntent = Intent(context, BatteryWidgetDetailActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_battery_root, pendingIntent)
        }

        fun buildRowRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            devices: List<BatteryDeviceItem>,
            palette: ResolvedPaletteColors
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_battery_row)
            attachClickIntent(context, views)

            // Background & Opacity
            views.setInt(R.id.widget_battery_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_battery_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            val deviceCount = devices.size
            if (deviceCount <= 1 || widthDp < 200) {
                views.setViewVisibility(R.id.container_single_device, View.VISIBLE)
                views.setViewVisibility(R.id.container_multi_devices, View.GONE)

                val phone = devices.firstOrNull() ?: BatteryDeviceItem(
                    id = "default_phone",
                    name = context.getString(R.string.battery_title_phone),
                    type = BatteryDeviceType.PHONE,
                    levelPercent = 100,
                    isCharging = false,
                    statusText = context.getString(R.string.battery_status_discharging)
                )

                views.setInt(R.id.iv_row_card_bg, "setColorFilter", palette.secondaryBgColor)
                views.setInt(R.id.iv_row_card_bg, "setImageAlpha", Color.alpha(palette.secondaryBgColor))

                views.setInt(R.id.iv_row_icon_badge_bg, "setColorFilter", palette.textColor)
                views.setInt(R.id.iv_row_icon_badge_bg, "setImageAlpha", 30)

                views.setImageViewResource(R.id.iv_row_device_icon, phone.iconResId)
                views.setInt(R.id.iv_row_device_icon, "setColorFilter", palette.textColor)

                views.setTextViewText(R.id.tv_row_device_name, phone.name)
                views.setTextColor(R.id.tv_row_device_name, palette.textColor)

                views.setTextViewText(R.id.tv_row_device_status, phone.statusText ?: "")
                views.setTextColor(R.id.tv_row_device_status, palette.textColor)

                views.setTextViewText(R.id.tv_row_device_pct, "${phone.levelPercent}%")
                views.setTextColor(R.id.tv_row_device_pct, palette.textColor)

                views.setInt(R.id.iv_row_progress_track, "setColorFilter", palette.textColor)
                views.setInt(R.id.iv_row_progress_track, "setImageAlpha", 45)

                views.setInt(R.id.iv_row_progress_fill, "setColorFilter", palette.textColor)
                views.setInt(R.id.iv_row_progress_fill, "setImageLevel", (phone.levelPercent * 100).coerceIn(0, 10000))

                views.setViewVisibility(R.id.iv_row_device_icon, View.VISIBLE)
                if (phone.isCharging) {
                    views.setViewVisibility(R.id.flipper_row_charging, View.VISIBLE)
                    views.setViewVisibility(R.id.flipper_row_pct_charging, View.GONE)

                    views.setInt(R.id.iv_row_bolt_1, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_row_bolt_1, "setImageAlpha", 80)

                    views.setInt(R.id.iv_row_bolt_2, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_row_bolt_2, "setImageAlpha", 200)
                } else {
                    views.setViewVisibility(R.id.flipper_row_charging, View.GONE)
                    views.setViewVisibility(R.id.flipper_row_pct_charging, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.container_single_device, View.GONE)
                views.setViewVisibility(R.id.container_multi_devices, View.VISIBLE)

                // Device 1
                val dev1 = devices.getOrNull(0)
                if (dev1 != null) {
                    views.setViewVisibility(R.id.col_device_1, View.VISIBLE)
                    views.setImageViewResource(R.id.iv_col_1_icon, dev1.iconResId)
                    views.setInt(R.id.iv_col_1_icon, "setColorFilter", palette.textColor)
                    views.setViewVisibility(R.id.iv_col_1_icon, View.VISIBLE)

                    views.setTextViewText(R.id.tv_col_1_name, dev1.name)
                    views.setTextColor(R.id.tv_col_1_name, palette.textColor)
                    views.setTextViewText(R.id.tv_col_1_pct, "${dev1.levelPercent}%")
                    views.setTextColor(R.id.tv_col_1_pct, palette.textColor)

                    views.setInt(R.id.iv_col_1_progress_track, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_col_1_progress_track, "setImageAlpha", 45)
                    views.setInt(R.id.iv_col_1_progress_fill, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_col_1_progress_fill, "setImageLevel", (dev1.levelPercent * 100).coerceIn(0, 10000))

                    if (dev1.isCharging) {
                        views.setViewVisibility(R.id.flipper_col_1_charging, View.VISIBLE)
                        views.setInt(R.id.iv_col_1_bolt_1, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_col_1_bolt_1, "setImageAlpha", 80)
                        views.setInt(R.id.iv_col_1_bolt_2, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_col_1_bolt_2, "setImageAlpha", 200)
                    } else {
                        views.setViewVisibility(R.id.flipper_col_1_charging, View.GONE)
                    }
                }

                // Device 2
                val dev2 = devices.getOrNull(1)
                if (dev2 != null) {
                    views.setViewVisibility(R.id.col_device_2, View.VISIBLE)
                    views.setImageViewResource(R.id.iv_col_2_icon, dev2.iconResId)
                    views.setInt(R.id.iv_col_2_icon, "setColorFilter", palette.textColor)
                    views.setViewVisibility(R.id.iv_col_2_icon, View.VISIBLE)

                    views.setTextViewText(R.id.tv_col_2_name, dev2.name)
                    views.setTextColor(R.id.tv_col_2_name, palette.textColor)
                    views.setTextViewText(R.id.tv_col_2_pct, "${dev2.levelPercent}%")
                    views.setTextColor(R.id.tv_col_2_pct, palette.textColor)

                    views.setInt(R.id.iv_col_2_progress_track, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_col_2_progress_track, "setImageAlpha", 45)
                    views.setInt(R.id.iv_col_2_progress_fill, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_col_2_progress_fill, "setImageLevel", (dev2.levelPercent * 100).coerceIn(0, 10000))

                    if (dev2.isCharging) {
                        views.setViewVisibility(R.id.flipper_col_2_charging, View.VISIBLE)
                        views.setInt(R.id.iv_col_2_bolt_1, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_col_2_bolt_1, "setImageAlpha", 80)
                        views.setInt(R.id.iv_col_2_bolt_2, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_col_2_bolt_2, "setImageAlpha", 200)
                    } else {
                        views.setViewVisibility(R.id.flipper_col_2_charging, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.col_device_2, View.GONE)
                }

                // Device 3
                val dev3 = devices.getOrNull(2)
                if (dev3 != null && widthDp >= 320) {
                    views.setViewVisibility(R.id.col_device_3, View.VISIBLE)
                    views.setImageViewResource(R.id.iv_col_3_icon, dev3.iconResId)
                    views.setInt(R.id.iv_col_3_icon, "setColorFilter", palette.textColor)
                    views.setViewVisibility(R.id.iv_col_3_icon, View.VISIBLE)

                    views.setTextViewText(R.id.tv_col_3_name, dev3.name)
                    views.setTextColor(R.id.tv_col_3_name, palette.textColor)
                    views.setTextViewText(R.id.tv_col_3_pct, "${dev3.levelPercent}%")
                    views.setTextColor(R.id.tv_col_3_pct, palette.textColor)

                    views.setInt(R.id.iv_col_3_progress_track, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_col_3_progress_track, "setImageAlpha", 45)
                    views.setInt(R.id.iv_col_3_progress_fill, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_col_3_progress_fill, "setImageLevel", (dev3.levelPercent * 100).coerceIn(0, 10000))

                    if (dev3.isCharging) {
                        views.setViewVisibility(R.id.flipper_col_3_charging, View.VISIBLE)
                        views.setInt(R.id.iv_col_3_bolt_1, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_col_3_bolt_1, "setImageAlpha", 80)
                        views.setInt(R.id.iv_col_3_bolt_2, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_col_3_bolt_2, "setImageAlpha", 200)
                    } else {
                        views.setViewVisibility(R.id.flipper_col_3_charging, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.col_device_3, View.GONE)
                }
            }

            return views
        }

        fun buildCardRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            devices: List<BatteryDeviceItem>,
            palette: ResolvedPaletteColors
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_battery_card)
            attachClickIntent(context, views)

            views.setInt(R.id.widget_battery_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_battery_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            val deviceCount = devices.size
            if (deviceCount <= 1 || widthDp < 200) {
                views.setViewVisibility(R.id.hero_single_device, View.VISIBLE)
                views.setViewVisibility(R.id.card_multi_devices, View.GONE)

                val phone = devices.firstOrNull() ?: BatteryDeviceItem(
                    id = "default_phone",
                    name = context.getString(R.string.battery_title_phone),
                    type = BatteryDeviceType.PHONE,
                    levelPercent = 100,
                    isCharging = false,
                    statusText = context.getString(R.string.battery_status_discharging)
                )

                views.setInt(R.id.iv_hero_card_bg, "setColorFilter", palette.secondaryBgColor)
                views.setInt(R.id.iv_hero_card_bg, "setImageAlpha", Color.alpha(palette.secondaryBgColor))

                views.setInt(R.id.iv_hero_icon_badge_bg, "setColorFilter", palette.textColor)
                views.setInt(R.id.iv_hero_icon_badge_bg, "setImageAlpha", 30)

                views.setImageViewResource(R.id.iv_hero_icon, phone.iconResId)
                views.setInt(R.id.iv_hero_icon, "setColorFilter", palette.textColor)
                views.setViewVisibility(R.id.iv_hero_icon, View.VISIBLE)

                views.setTextViewText(R.id.tv_hero_name, phone.name)
                views.setTextColor(R.id.tv_hero_name, palette.textColor)

                views.setTextViewText(R.id.tv_hero_status, phone.statusText ?: "")
                views.setTextColor(R.id.tv_hero_status, palette.textColor)

                views.setTextViewText(R.id.tv_hero_pct, "${phone.levelPercent}%")
                views.setTextColor(R.id.tv_hero_pct, palette.textColor)

                views.setInt(R.id.iv_hero_track, "setColorFilter", palette.textColor)
                views.setInt(R.id.iv_hero_track, "setImageAlpha", 45)

                views.setInt(R.id.iv_hero_fill, "setColorFilter", palette.textColor)
                views.setInt(R.id.iv_hero_fill, "setImageLevel", (phone.levelPercent * 100).coerceIn(0, 10000))

                if (phone.isCharging) {
                    views.setViewVisibility(R.id.flipper_hero_charging, View.VISIBLE)
                    views.setViewVisibility(R.id.flipper_hero_pct_charging, View.GONE)
                    views.setInt(R.id.iv_hero_bolt_1, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_hero_bolt_1, "setImageAlpha", 80)
                    views.setInt(R.id.iv_hero_bolt_2, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_hero_bolt_2, "setImageAlpha", 200)
                } else {
                    views.setViewVisibility(R.id.flipper_hero_charging, View.GONE)
                    views.setViewVisibility(R.id.flipper_hero_pct_charging, View.GONE)
                }
            } else {
                views.setViewVisibility(R.id.hero_single_device, View.GONE)
                views.setViewVisibility(R.id.card_multi_devices, View.VISIBLE)

                val rowBgColor = palette.secondaryBgColor

                // Row 1
                val dev1 = devices.getOrNull(0)
                if (dev1 != null) {
                    views.setViewVisibility(R.id.card_row_1, View.VISIBLE)
                    views.setInt(R.id.iv_card_row_bg_1, "setColorFilter", rowBgColor)
                    views.setInt(R.id.iv_card_row_bg_1, "setImageAlpha", Color.alpha(rowBgColor))

                    views.setImageViewResource(R.id.iv_card_row_icon_1, dev1.iconResId)
                    views.setInt(R.id.iv_card_row_icon_1, "setColorFilter", palette.textColor)
                    views.setViewVisibility(R.id.iv_card_row_icon_1, View.VISIBLE)

                    views.setTextViewText(R.id.tv_card_row_name_1, dev1.name)
                    views.setTextColor(R.id.tv_card_row_name_1, palette.textColor)

                    views.setTextViewText(R.id.tv_card_row_status_1, dev1.statusText ?: "")
                    views.setTextColor(R.id.tv_card_row_status_1, palette.textColor)

                    views.setTextViewText(R.id.tv_card_row_pct_1, "${dev1.levelPercent}%")
                    views.setTextColor(R.id.tv_card_row_pct_1, palette.textColor)

                    views.setInt(R.id.iv_card_row_progress_track_1, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_card_row_progress_track_1, "setImageAlpha", 45)
                    views.setInt(R.id.iv_card_row_progress_fill_1, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_card_row_progress_fill_1, "setImageLevel", (dev1.levelPercent * 100).coerceIn(0, 10000))

                    if (dev1.isCharging) {
                        views.setViewVisibility(R.id.flipper_card_row_charging_1, View.VISIBLE)
                        views.setInt(R.id.iv_card_row_bolt_1_a, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_card_row_bolt_1_a, "setImageAlpha", 80)
                        views.setInt(R.id.iv_card_row_bolt_1_b, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_card_row_bolt_1_b, "setImageAlpha", 200)
                    } else {
                        views.setViewVisibility(R.id.flipper_card_row_charging_1, View.GONE)
                    }
                }

                // Row 2
                val dev2 = devices.getOrNull(1)
                if (dev2 != null) {
                    views.setViewVisibility(R.id.card_row_2, View.VISIBLE)
                    views.setInt(R.id.iv_card_row_bg_2, "setColorFilter", rowBgColor)
                    views.setInt(R.id.iv_card_row_bg_2, "setImageAlpha", Color.alpha(rowBgColor))

                    views.setImageViewResource(R.id.iv_card_row_icon_2, dev2.iconResId)
                    views.setInt(R.id.iv_card_row_icon_2, "setColorFilter", palette.textColor)
                    views.setViewVisibility(R.id.iv_card_row_icon_2, View.VISIBLE)

                    views.setTextViewText(R.id.tv_card_row_name_2, dev2.name)
                    views.setTextColor(R.id.tv_card_row_name_2, palette.textColor)

                    views.setTextViewText(R.id.tv_card_row_status_2, dev2.statusText ?: "")
                    views.setTextColor(R.id.tv_card_row_status_2, palette.textColor)

                    views.setTextViewText(R.id.tv_card_row_pct_2, "${dev2.levelPercent}%")
                    views.setTextColor(R.id.tv_card_row_pct_2, palette.textColor)

                    views.setInt(R.id.iv_card_row_progress_track_2, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_card_row_progress_track_2, "setImageAlpha", 45)
                    views.setInt(R.id.iv_card_row_progress_fill_2, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_card_row_progress_fill_2, "setImageLevel", (dev2.levelPercent * 100).coerceIn(0, 10000))

                    if (dev2.isCharging) {
                        views.setViewVisibility(R.id.flipper_card_row_charging_2, View.VISIBLE)
                        views.setInt(R.id.iv_card_row_bolt_2_a, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_card_row_bolt_2_a, "setImageAlpha", 80)
                        views.setInt(R.id.iv_card_row_bolt_2_b, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_card_row_bolt_2_b, "setImageAlpha", 200)
                    } else {
                        views.setViewVisibility(R.id.flipper_card_row_charging_2, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.card_row_2, View.GONE)
                }

                // Row 3
                val dev3 = devices.getOrNull(2)
                if (dev3 != null && heightDp >= 140) {
                    views.setViewVisibility(R.id.card_row_3, View.VISIBLE)
                    views.setInt(R.id.iv_card_row_bg_3, "setColorFilter", rowBgColor)
                    views.setInt(R.id.iv_card_row_bg_3, "setImageAlpha", Color.alpha(rowBgColor))

                    views.setImageViewResource(R.id.iv_card_row_icon_3, dev3.iconResId)
                    views.setInt(R.id.iv_card_row_icon_3, "setColorFilter", palette.textColor)
                    views.setViewVisibility(R.id.iv_card_row_icon_3, View.VISIBLE)

                    views.setTextViewText(R.id.tv_card_row_name_3, dev3.name)
                    views.setTextColor(R.id.tv_card_row_name_3, palette.textColor)

                    views.setTextViewText(R.id.tv_card_row_status_3, dev3.statusText ?: "")
                    views.setTextColor(R.id.tv_card_row_status_3, palette.textColor)

                    views.setTextViewText(R.id.tv_card_row_pct_3, "${dev3.levelPercent}%")
                    views.setTextColor(R.id.tv_card_row_pct_3, palette.textColor)

                    views.setInt(R.id.iv_card_row_progress_track_3, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_card_row_progress_track_3, "setImageAlpha", 45)
                    views.setInt(R.id.iv_card_row_progress_fill_3, "setColorFilter", palette.textColor)
                    views.setInt(R.id.iv_card_row_progress_fill_3, "setImageLevel", (dev3.levelPercent * 100).coerceIn(0, 10000))

                    if (dev3.isCharging) {
                        views.setViewVisibility(R.id.flipper_card_row_charging_3, View.VISIBLE)
                        views.setInt(R.id.iv_card_row_bolt_3_a, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_card_row_bolt_3_a, "setImageAlpha", 80)
                        views.setInt(R.id.iv_card_row_bolt_3_b, "setColorFilter", palette.textColor)
                        views.setInt(R.id.iv_card_row_bolt_3_b, "setImageAlpha", 200)
                    } else {
                        views.setViewVisibility(R.id.flipper_card_row_charging_3, View.GONE)
                    }
                } else {
                    views.setViewVisibility(R.id.card_row_3, View.GONE)
                }
            }

            return views
        }

        fun buildTallRemoteViews(
            context: Context,
            widthDp: Int,
            heightDp: Int,
            devices: List<BatteryDeviceItem>,
            palette: ResolvedPaletteColors
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_battery_tall)
            attachClickIntent(context, views)

            views.setInt(R.id.widget_battery_bg, "setColorFilter", palette.bgColor)
            views.setInt(R.id.widget_battery_bg, "setImageAlpha", Color.alpha(palette.bgColor))

            val rowBgColor = palette.secondaryBgColor

            val rowConfigs = listOf(
                Triple(R.id.tall_row_1, R.id.iv_tall_row_bg_1, R.id.iv_tall_row_icon_1) to
                        Triple(R.id.flipper_tall_row_charging_1, R.id.iv_tall_row_bolt_1_a, R.id.iv_tall_row_bolt_1_b),
                Triple(R.id.tall_row_2, R.id.iv_tall_row_bg_2, R.id.iv_tall_row_icon_2) to
                        Triple(R.id.flipper_tall_row_charging_2, R.id.iv_tall_row_bolt_2_a, R.id.iv_tall_row_bolt_2_b),
                Triple(R.id.tall_row_3, R.id.iv_tall_row_bg_3, R.id.iv_tall_row_icon_3) to
                        Triple(R.id.flipper_tall_row_charging_3, R.id.iv_tall_row_bolt_3_a, R.id.iv_tall_row_bolt_3_b),
                Triple(R.id.tall_row_4, R.id.iv_tall_row_bg_4, R.id.iv_tall_row_icon_4) to
                        Triple(R.id.flipper_tall_row_charging_4, R.id.iv_tall_row_bolt_4_a, R.id.iv_tall_row_bolt_4_b)
            )

            val textConfigs = listOf(
                Triple(R.id.tv_tall_row_name_1, R.id.tv_tall_row_status_1, R.id.tv_tall_row_pct_1) to
                        Pair(R.id.iv_tall_row_progress_track_1, R.id.iv_tall_row_progress_fill_1),
                Triple(R.id.tv_tall_row_name_2, R.id.tv_tall_row_status_2, R.id.tv_tall_row_pct_2) to
                        Pair(R.id.iv_tall_row_progress_track_2, R.id.iv_tall_row_progress_fill_2),
                Triple(R.id.tv_tall_row_name_3, R.id.tv_tall_row_status_3, R.id.tv_tall_row_pct_3) to
                        Pair(R.id.iv_tall_row_progress_track_3, R.id.iv_tall_row_progress_fill_3),
                Triple(R.id.tv_tall_row_name_4, R.id.tv_tall_row_status_4, R.id.tv_tall_row_pct_4) to
                        Pair(R.id.iv_tall_row_progress_track_4, R.id.iv_tall_row_progress_fill_4)
            )

            for (i in 0 until 4) {
                val rowConfig = rowConfigs[i]
                val textConfig = textConfigs[i]
                val rowId = rowConfig.first.first
                val bgId = rowConfig.first.second
                val iconId = rowConfig.first.third
                val flipperId = rowConfig.second.first
                val bolt1Id = rowConfig.second.second
                val bolt2Id = rowConfig.second.third

                val nameId = textConfig.first.first
                val statusId = textConfig.first.second
                val pctId = textConfig.first.third
                val trackId = textConfig.second.first
                val fillId = textConfig.second.second

                val device = devices.getOrNull(i)
                if (device != null) {
                    views.setViewVisibility(rowId, View.VISIBLE)
                    views.setInt(bgId, "setColorFilter", rowBgColor)
                    views.setInt(bgId, "setImageAlpha", Color.alpha(rowBgColor))

                    views.setImageViewResource(iconId, device.iconResId)
                    views.setInt(iconId, "setColorFilter", palette.textColor)
                    views.setViewVisibility(iconId, View.VISIBLE)

                    views.setTextViewText(nameId, device.name)
                    views.setTextColor(nameId, palette.textColor)

                    views.setTextViewText(statusId, device.statusText ?: "")
                    views.setTextColor(statusId, palette.textColor)

                    views.setTextViewText(pctId, "${device.levelPercent}%")
                    views.setTextColor(pctId, palette.textColor)

                    views.setInt(trackId, "setColorFilter", palette.textColor)
                    views.setInt(trackId, "setImageAlpha", 45)
                    views.setInt(fillId, "setColorFilter", palette.textColor)
                    views.setInt(fillId, "setImageLevel", (device.levelPercent * 100).coerceIn(0, 10000))

                    if (flipperId != 0 && device.isCharging) {
                        views.setViewVisibility(flipperId, View.VISIBLE)
                        views.setInt(bolt1Id, "setColorFilter", palette.textColor)
                        views.setInt(bolt1Id, "setImageAlpha", 80)
                        views.setInt(bolt2Id, "setColorFilter", palette.textColor)
                        views.setInt(bolt2Id, "setImageAlpha", 200)
                    } else if (flipperId != 0) {
                        views.setViewVisibility(flipperId, View.GONE)
                    }
                } else {
                    views.setViewVisibility(rowId, View.GONE)
                }
            }

            return views
        }
    }
}
