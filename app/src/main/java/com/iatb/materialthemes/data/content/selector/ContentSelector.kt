package com.iatb.materialthemes.data.content.selector

import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.repository.ContentRepository
import java.util.Locale

class ContentSelector(
    private val repository: ContentRepository
) {

    /**
     * Selects the next content item based on priority rules:
     * 1. Matches requested [type].
     * 2. Matches [selectedLanguages] (normalized, balanced across languages without starvation).
     * 3. Excludes [currentContentId] to avoid immediate repetition.
     * 4. Prefers content that has never been shown (lastShownAt == null).
     * 5. Prefers content with the oldest lastShownAt.
     * 6. Random tie-breaker among equivalent top candidates.
     * 7. Fallback strictly within [selectedLanguages] (never cross-language fallback).
     */
    suspend fun selectNextContent(
        type: ContentType,
        selectedLanguages: List<String>,
        currentContentId: String?
    ): ContentEntity {
        val languages = selectedLanguages
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .distinct()
            .ifEmpty { listOf("en") }

        val candidates = if (languages.size > 1) {
            val perLangLimit = (30 / languages.size).coerceAtLeast(5)
            languages.flatMap { lang ->
                repository.getCandidateContent(
                    type = type,
                    languages = listOf(lang),
                    currentId = currentContentId,
                    limit = perLangLimit
                )
            }.sortedWith(compareBy({ it.lastShownAt != null }, { it.lastShownAt ?: 0L }))
        } else {
            repository.getCandidateContent(
                type = type,
                languages = languages,
                currentId = currentContentId,
                limit = 30
            )
        }

        if (candidates.isNotEmpty()) {
            val unshown = candidates.filter { it.lastShownAt == null }
            if (unshown.isNotEmpty()) {
                return unshown.random()
            }

            // All candidates in this pool have been shown before.
            // Candidates are ordered by lastShownAt ASC.
            // Pick randomly among top 5 oldest shown items for subtle variety.
            val topCandidates = candidates.take(5)
            return topCandidates.random()
        }

        // Candidates pool is empty (no alternatives found or only currentId existed)
        if (currentContentId != null) {
            val current = repository.getContentById(currentContentId)
            if (current != null && current.enabled && current.type == type && current.language in languages) {
                // Current item is still completely valid and matches the filter: keep it
                return current
            }
        }

        // Safe fallback strictly adhering to the first selected language
        val fallbackLang = languages.firstOrNull() ?: "en"
        return repository.getFallbackContent(type, fallbackLang)
    }
}
