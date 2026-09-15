package com.iatb.materialthemes.data.content.repository

import android.content.Context
import com.iatb.materialthemes.data.content.database.ContentDatabase
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.model.WidgetStateEntity

class ContentRepositoryImpl(
    private val context: Context,
    private val database: ContentDatabase
) : ContentRepository {

    private val contentDao = database.contentDao()
    private val widgetStateDao = database.widgetStateDao()

    override suspend fun getWidgetState(widgetId: Int): WidgetStateEntity? {
        return widgetStateDao.getState(widgetId)
    }

    override suspend fun saveWidgetState(state: WidgetStateEntity) {
        widgetStateDao.insertOrUpdate(state)
    }

    override suspend fun deleteWidgetState(widgetId: Int) {
        widgetStateDao.deleteState(widgetId)
    }

    override suspend fun getAllActiveWidgetStates(): List<WidgetStateEntity> {
        return widgetStateDao.getAllActiveStates()
    }

    override suspend fun getContentById(id: String): ContentEntity? {
        return contentDao.getById(id)
    }

    override suspend fun markContentAsShown(contentId: String) {
        contentDao.updateShown(contentId, System.currentTimeMillis())
    }

    override suspend fun getCandidateContent(
        type: ContentType,
        languages: List<String>,
        currentId: String?,
        limit: Int
    ): List<ContentEntity> {
        if (languages.isEmpty()) return emptyList()
        return contentDao.getCandidates(type, languages, currentId, limit)
    }

    override suspend fun getFallbackContent(type: ContentType, language: String): ContentEntity {
        val fromDb = contentDao.getRandomFallback(type, language)
        if (fromDb != null) return fromDb

        // Resolve localized strings based on the requested language
        val localizedContext = try {
            val locale = java.util.Locale.forLanguageTag(language)
            val config = android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(locale)
            }
            context.createConfigurationContext(config)
        } catch (e: Exception) {
            context
        }

        val fallbackQuoteText = try {
            localizedContext.getString(com.iatb.materialthemes.R.string.utility_fallback_quote_text)
        } catch (e: Exception) {
            if (language == "vi") "Cách duy nhất để tạo nên sự nghiệp vĩ đại là yêu lấy việc bạn làm."
            else "The only way to do great work is to love what you do."
        }

        val fallbackQuoteAuthor = try {
            localizedContext.getString(com.iatb.materialthemes.R.string.utility_fallback_quote_author)
        } catch (e: Exception) {
            "Steve Jobs"
        }

        val fallbackThoughtText = try {
            localizedContext.getString(com.iatb.materialthemes.R.string.utility_fallback_thought_text)
        } catch (e: Exception) {
            if (language == "vi") "Điều gì khiến bạn cảm thấy biết ơn nhất trong ngày hôm nay?"
            else "What is one thing you are grateful for today?"
        }

        return when (type) {
            ContentType.QUOTE -> ContentEntity(
                id = "fallback_quote_$language",
                type = ContentType.QUOTE,
                text = fallbackQuoteText,
                author = fallbackQuoteAuthor,
                category = "Inspirational",
                language = language,
                source = "bundled",
                contentHash = "fallback_quote_$language",
                enabled = true,
                createdAt = 0L
            )
            ContentType.THOUGHT -> ContentEntity(
                id = "fallback_thought_$language",
                type = ContentType.THOUGHT,
                text = fallbackThoughtText,
                author = null,
                category = "Gratitude",
                language = language,
                source = "bundled",
                contentHash = "fallback_thought_$language",
                enabled = true,
                createdAt = 0L
            )
        }
    }

    override suspend fun getTotalContentCount(): Int {
        return contentDao.getTotalCount()
    }

    companion object {
        @Volatile
        private var instance: ContentRepositoryImpl? = null

        fun getInstance(context: Context): ContentRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: ContentRepositoryImpl(
                    context.applicationContext,
                    ContentDatabase.getInstance(context.applicationContext)
                ).also { instance = it }
            }
        }
    }
}
