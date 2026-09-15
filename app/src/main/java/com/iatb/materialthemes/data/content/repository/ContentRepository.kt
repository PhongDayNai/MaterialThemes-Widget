package com.iatb.materialthemes.data.content.repository

import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.model.WidgetStateEntity

interface ContentRepository {
    suspend fun getWidgetState(widgetId: Int): WidgetStateEntity?
    suspend fun saveWidgetState(state: WidgetStateEntity)
    suspend fun deleteWidgetState(widgetId: Int)
    suspend fun getAllActiveWidgetStates(): List<WidgetStateEntity>
    suspend fun getContentById(id: String): ContentEntity?
    suspend fun markContentAsShown(contentId: String)
    suspend fun getCandidateContent(
        type: ContentType,
        languages: List<String>,
        currentId: String?,
        limit: Int = 30
    ): List<ContentEntity>
    suspend fun getFallbackContent(type: ContentType, language: String = "en"): ContentEntity
    suspend fun getTotalContentCount(): Int
}
