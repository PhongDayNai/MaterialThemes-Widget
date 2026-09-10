package com.iatb.materialthemes.widget

import android.content.Context
import android.widget.RemoteViews
import com.iatb.materialthemes.R
import com.iatb.materialthemes.WidgetCategory
import com.iatb.materialthemes.WidgetSize
import com.iatb.materialthemes.data.WidgetContentMode

object WidgetClickRouter {

    fun getContainerLayoutId(size: WidgetSize): Int {
        return when (size) {
            WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> R.layout.widget_canvas_container_3block
            WidgetSize.SIZE_3X2, WidgetSize.SIZE_4X2 -> R.layout.widget_canvas_container_2block
            else -> R.layout.widget_canvas_container
        }
    }

    fun bindClickZones(
        views: RemoteViews,
        context: Context,
        category: WidgetCategory,
        size: WidgetSize,
        mode: WidgetContentMode = WidgetContentMode.COMBO,
        locationName: String = ""
    ) {
        val clockIntent = WidgetClickRouterActivity.createPendingIntent(
            context,
            WidgetClickRouterActivity.TARGET_CLOCK,
            locationName,
            requestCode = 101
        )
        val weatherIntent = WidgetClickRouterActivity.createPendingIntent(
            context,
            WidgetClickRouterActivity.TARGET_WEATHER,
            locationName,
            requestCode = 102
        )
        val locationIntent = WidgetClickRouterActivity.createPendingIntent(
            context,
            WidgetClickRouterActivity.TARGET_LOCATION,
            locationName,
            requestCode = 103
        )

        when (size) {
            WidgetSize.SIZE_3X3, WidgetSize.SIZE_4X3 -> {
                // Layout 3 khối (widget_canvas_container_3block)
                // Khối 1 (Trái trên): Luôn là Đồng hồ
                views.setOnClickPendingIntent(R.id.touch_zone_block1, clockIntent)

                // Khối 2 (Phải trên):
                // - Diagonal: Location hoặc Weather
                // - Organic / Scallop: Pebble Weather Card
                val block2Intent = if (category == WidgetCategory.DIAGONAL) locationIntent else weatherIntent
                views.setOnClickPendingIntent(R.id.touch_zone_block2, block2Intent)

                // Khối 3 (Dải dưới full width): Thời tiết / Dự báo theo giờ
                views.setOnClickPendingIntent(R.id.touch_zone_block3, weatherIntent)
            }

            WidgetSize.SIZE_3X2, WidgetSize.SIZE_4X2 -> {
                // Layout 2 khối (widget_canvas_container_2block)
                // Cột trái: Đồng hồ (hoặc Thời tiết nếu mode là WEATHER)
                val leftIntent = if (mode == WidgetContentMode.WEATHER) weatherIntent else clockIntent
                views.setOnClickPendingIntent(R.id.touch_zone_left, leftIntent)

                // Cột phải:
                // - Phía trên: Location / Maps
                // - Phía dưới: Weather
                views.setOnClickPendingIntent(R.id.touch_zone_top_right, locationIntent)
                views.setOnClickPendingIntent(R.id.touch_zone_bottom_right, weatherIntent)
            }

            else -> {
                // Layout 2x2 hoặc các kích cỡ dọc (widget_canvas_container)
                when (mode) {
                    WidgetContentMode.CLOCK -> {
                        views.setOnClickPendingIntent(R.id.touch_zone_top, clockIntent)
                        views.setOnClickPendingIntent(R.id.touch_zone_bottom, clockIntent)
                    }
                    WidgetContentMode.WEATHER -> {
                        views.setOnClickPendingIntent(R.id.touch_zone_top, weatherIntent)
                        views.setOnClickPendingIntent(R.id.touch_zone_bottom, weatherIntent)
                    }
                    WidgetContentMode.COMBO -> {
                        views.setOnClickPendingIntent(R.id.touch_zone_top, clockIntent)
                        views.setOnClickPendingIntent(R.id.touch_zone_bottom, weatherIntent)
                    }
                }
            }
        }

        // Fallback root click nếu người dùng chạm vào phần lề hoặc nền widget
        views.setOnClickPendingIntent(R.id.widget_diagonal_root, clockIntent)
    }
}
