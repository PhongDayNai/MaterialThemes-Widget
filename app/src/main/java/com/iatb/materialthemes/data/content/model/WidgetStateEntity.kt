package com.iatb.materialthemes.data.content.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widget_state")
data class WidgetStateEntity(
    @PrimaryKey
    val widgetId: Int,
    val type: ContentType,
    val contentId: String? = null,
    val languageFilter: String,
    val changeIntervalMinutes: Long,
    val lastChangedAt: Long,
    val nextChangeAt: Long,
    val updatedAt: Long
)
