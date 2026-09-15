package com.iatb.materialthemes.data.content.database

import androidx.room.TypeConverter
import com.iatb.materialthemes.data.content.model.ContentType

class ContentConverters {

    @TypeConverter
    fun fromContentType(value: ContentType): String = value.name

    @TypeConverter
    fun toContentType(value: String): ContentType = try {
        ContentType.valueOf(value)
    } catch (e: Exception) {
        ContentType.QUOTE
    }
}
