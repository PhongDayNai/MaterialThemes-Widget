package com.iatb.materialthemes.data.content.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "content_source")
data class ContentSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val url: String,
    val type: ContentType,
    val enabled: Boolean = true,
    val lastSyncAt: Long? = null,
    val nextSyncAt: Long? = null,
    val syncError: String? = null
)
