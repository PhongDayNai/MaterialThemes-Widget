package com.iatb.materialthemes.widget

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock

class WidgetClickRouterActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val widgetId = intent.getIntExtra(EXTRA_APPWIDGET_ID, android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID)
            val catName = intent.getStringExtra(EXTRA_CATEGORY)
            val sizeName = intent.getStringExtra(EXTRA_SIZE)
            if (catName != null && sizeName != null) {
                try {
                    val cat = com.iatb.materialthemes.WidgetCategory.valueOf(catName)
                    val sz = com.iatb.materialthemes.WidgetSize.valueOf(sizeName)
                    ActiveWidgetManager.recordActiveWidget(this, widgetId, cat, sz)
                } catch (_: Exception) {}
            }

            val target = intent.getStringExtra(EXTRA_TARGET) ?: TARGET_CLOCK
            val location = intent.getStringExtra(EXTRA_LOCATION).orEmpty()

            when (target) {
                TARGET_CLOCK -> handleClock()
                TARGET_WEATHER -> handleWeather(location)
                TARGET_LOCATION -> handleLocation(location)
            }
        } catch (_: Exception) {
            // Đảm bảo không bao giờ crash nếu hệ thống gặp lỗi
        } finally {
            finish()
        }
    }

    private fun handleClock() {
        // 1. Thử gửi Intent chuẩn của Android
        val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (isIntentResolvable(alarmIntent)) {
            startActivity(alarmIntent)
            return
        }

        // 2. Fallback duyệt qua các ứng dụng đồng hồ OEM phổ biến
        val clockPackages = listOf(
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.coloros.alarmclock",
            "com.oplus.alarmclock",
            "com.vivo.alarmclock"
        )
        for (pkg in clockPackages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(launchIntent)
                return
            }
        }

        // 3. Fallback cuối cùng
        val fallbackAlarm = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (isIntentResolvable(fallbackAlarm)) {
            startActivity(fallbackAlarm)
        }
    }

    private fun handleWeather(location: String) {
        // 1. Thử mở app thời tiết của hãng sản xuất
        val weatherPackages = listOf(
            "com.miui.weather2",
            "com.sec.android.daemonapp",
            "com.coloros.weather",
            "com.oplus.weather",
            "com.huawei.android.totemweather",
            "com.google.android.apps.weather",
            "com.vivo.weather"
        )
        for (pkg in weatherPackages) {
            val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(launchIntent)
                return
            }
        }

        // 2. Fallback: Tìm kiếm thời tiết trên Google (giao diện thời tiết tương tác của Google)
        val query = if (location.isNotBlank()) "thời tiết $location" else "thời tiết"
        val searchIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(searchIntent)
    }

    private fun handleLocation(location: String) {
        val query = location.ifBlank { "Hanoi" }
        val encodedQuery = Uri.encode(query)

        // 1. Thử mở app Google Maps hoặc app bản đồ tương đương qua URI geo:
        val geoUri = Uri.parse("geo:0,0?q=$encodedQuery")
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // Ưu tiên package Google Maps nếu có
            if (packageManager.getLaunchIntentForPackage("com.google.android.apps.maps") != null) {
                setPackage("com.google.android.apps.maps")
            }
        }

        if (isIntentResolvable(mapIntent)) {
            startActivity(mapIntent)
        } else {
            // 2. Fallback: Mở Google Maps phiên bản Web
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encodedQuery")
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(webIntent)
        }
    }

    private fun isIntentResolvable(intent: Intent): Boolean {
        return intent.resolveActivity(packageManager) != null
    }

    companion object {
        const val ACTION_CLICK_ZONE = "com.iatb.materialthemes.action.CLICK_ZONE"

        const val EXTRA_TARGET = "extra_target"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_APPWIDGET_ID = "extra_appwidget_id"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_SIZE = "extra_size"

        const val TARGET_CLOCK = "target_clock"
        const val TARGET_WEATHER = "target_weather"
        const val TARGET_LOCATION = "target_location"

        fun createPendingIntent(
            context: Context,
            target: String,
            locationName: String = "",
            requestCode: Int = 0,
            appWidgetId: Int = android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID,
            category: com.iatb.materialthemes.WidgetCategory? = null,
            size: com.iatb.materialthemes.WidgetSize? = null
        ): PendingIntent {
            val intent = Intent(context, WidgetClickRouterActivity::class.java).apply {
                action = "$ACTION_CLICK_ZONE.$target.$appWidgetId"
                putExtra(EXTRA_TARGET, target)
                putExtra(EXTRA_LOCATION, locationName)
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
                if (category != null) putExtra(EXTRA_CATEGORY, category.name)
                if (size != null) putExtra(EXTRA_SIZE, size.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val finalRequestCode = if (appWidgetId != android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
                appWidgetId * 10 + (requestCode % 10)
            } else {
                requestCode
            }
            return PendingIntent.getActivity(
                context,
                finalRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
