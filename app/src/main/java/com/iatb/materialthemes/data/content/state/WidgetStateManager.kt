package com.iatb.materialthemes.data.content.state

import android.content.Context
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.model.WidgetStateEntity
import com.iatb.materialthemes.data.content.repository.ContentRepository
import com.iatb.materialthemes.data.content.repository.ContentRepositoryImpl
import com.iatb.materialthemes.data.content.selector.ContentSelector
import java.util.Locale

class WidgetStateManager(
    private val repository: ContentRepository,
    private val selector: ContentSelector
) {

    /**
     * Initializes default persistent state when a new widget instance is placed on the home screen.
     */
    suspend fun createInitialState(
        widgetId: Int,
        type: ContentType,
        systemLocale: Locale = Locale.getDefault()
    ): WidgetStateEntity {
        return createInitialStateInternal(widgetId, type, systemLocale).first
    }

    suspend fun createInitialStateWithContent(
        widgetId: Int,
        type: ContentType,
        systemLocale: Locale = Locale.getDefault()
    ): Pair<WidgetStateEntity, ContentEntity> {
        return createInitialStateInternal(widgetId, type, systemLocale)
    }

    private suspend fun createInitialStateInternal(
        widgetId: Int,
        type: ContentType,
        systemLocale: Locale
    ): Pair<WidgetStateEntity, ContentEntity> {
        val initialLang = if (systemLocale.language.lowercase(Locale.ROOT) == "vi") "vi" else "en"
        val languageFilter = initialLang
        val defaultIntervalMinutes = DEFAULT_INTERVAL_MINUTES

        val content = selector.selectNextContent(
            type = type,
            selectedLanguages = listOf(initialLang),
            currentContentId = null
        )

        val now = System.currentTimeMillis()
        repository.markContentAsShown(content.id)

        val state = WidgetStateEntity(
            widgetId = widgetId,
            type = type,
            contentId = content.id,
            languageFilter = languageFilter,
            changeIntervalMinutes = defaultIntervalMinutes,
            lastChangedAt = now,
            nextChangeAt = now + (defaultIntervalMinutes * 60 * 1000L),
            updatedAt = now
        )

        repository.saveWidgetState(state)
        return Pair(state, content)
    }

    suspend fun getState(widgetId: Int): WidgetStateEntity? {
        return repository.getWidgetState(widgetId)
    }

    suspend fun getAllActiveStates(): List<WidgetStateEntity> {
        return repository.getAllActiveWidgetStates()
    }

    /**
     * Handles manual refresh triggered from the configuration screen.
     * Updates only the specified [widgetId] without affecting any other widgets.
     */
    suspend fun refreshContent(widgetId: Int): Pair<WidgetStateEntity, ContentEntity>? {
        val state = repository.getWidgetState(widgetId) ?: return null
        val languages = parseLanguages(state.languageFilter)

        val nextContent = selector.selectNextContent(
            type = state.type,
            selectedLanguages = languages,
            currentContentId = state.contentId
        )

        val now = System.currentTimeMillis()
        repository.markContentAsShown(nextContent.id)

        val updatedState = state.copy(
            contentId = nextContent.id,
            lastChangedAt = now,
            nextChangeAt = now + (state.changeIntervalMinutes * 60 * 1000L),
            updatedAt = now
        )

        repository.saveWidgetState(updatedState)
        return Pair(updatedState, nextContent)
    }

    /**
     * Automatic content rotation triggered by scheduler when nextChangeAt is reached.
     */
    suspend fun rotateContent(widgetId: Int): Pair<WidgetStateEntity, ContentEntity>? {
        return refreshContent(widgetId)
    }

    /**
     * Updates language filter. If current content no longer matches the new filter,
     * immediately selects a new valid candidate for this widget.
     */
    suspend fun updateLanguageFilter(
        widgetId: Int,
        newLanguages: List<String>
    ): Pair<WidgetStateEntity, ContentEntity>? {
        val state = repository.getWidgetState(widgetId) ?: return null
        val validLanguages = newLanguages
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
            .ifEmpty { listOf("en") }
        val formattedFilter = validLanguages.joinToString(",")

        val currentContent = state.contentId?.let { repository.getContentById(it) }
        val isCurrentStillValid = currentContent != null &&
            currentContent.enabled &&
            currentContent.type == state.type &&
            currentContent.language.lowercase(Locale.ROOT) in validLanguages

        val now = System.currentTimeMillis()

        return if (isCurrentStillValid) {
            val updatedState = state.copy(
                languageFilter = formattedFilter,
                updatedAt = now
            )
            repository.saveWidgetState(updatedState)
            Pair(updatedState, currentContent!!)
        } else {
            val nextContent = selector.selectNextContent(
                type = state.type,
                selectedLanguages = validLanguages,
                currentContentId = state.contentId
            )
            repository.markContentAsShown(nextContent.id)

            val updatedState = state.copy(
                contentId = nextContent.id,
                languageFilter = formattedFilter,
                lastChangedAt = now,
                nextChangeAt = now + (state.changeIntervalMinutes * 60 * 1000L),
                updatedAt = now
            )
            repository.saveWidgetState(updatedState)
            Pair(updatedState, nextContent)
        }
    }

    /**
     * Updates the automatic rotation interval and recalculates nextChangeAt.
     */
    suspend fun updateInterval(widgetId: Int, intervalMinutes: Long): WidgetStateEntity? {
        val state = repository.getWidgetState(widgetId) ?: return null
        val now = System.currentTimeMillis()
        val updatedState = state.copy(
            changeIntervalMinutes = intervalMinutes,
            nextChangeAt = now + (intervalMinutes * 60 * 1000L),
            updatedAt = now
        )
        repository.saveWidgetState(updatedState)
        return updatedState
    }

    suspend fun deleteWidget(widgetId: Int) {
        repository.deleteWidgetState(widgetId)
    }

    fun parseLanguages(filter: String): List<String> {
        val list = filter.split(",")
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
        return list.ifEmpty { listOf("en") }
    }

    fun formatLanguages(languages: List<String>): String {
        val list = languages
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
            .ifEmpty { listOf("en") }
        return list.joinToString(",")
    }

    companion object {
        const val DEFAULT_INTERVAL_MINUTES = 1440L // 1 day

        @Volatile
        private var instance: WidgetStateManager? = null

        fun getInstance(context: Context): WidgetStateManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val repo = ContentRepositoryImpl.getInstance(context)
                    val selector = ContentSelector(repo)
                    WidgetStateManager(repo, selector).also { instance = it }
                }
            }
        }
    }
}
