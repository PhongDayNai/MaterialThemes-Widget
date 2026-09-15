package com.iatb.materialthemes.data.content.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "content",
    indices = [
        Index(value = ["type", "enabled", "language"]),
        Index(value = ["type", "lastShownAt"]),
        Index(value = ["contentHash"], unique = true)
    ]
)
data class ContentEntity(
    @PrimaryKey
    val id: String,
    val type: ContentType,
    val text: String,
    val author: String? = null,
    val category: String? = null,
    val language: String,
    val source: String,
    val sourceId: String? = null,
    val contentHash: String,
    val enabled: Boolean = true,
    val createdAt: Long,
    val lastShownAt: Long? = null,
    val shownCount: Int = 0
)
