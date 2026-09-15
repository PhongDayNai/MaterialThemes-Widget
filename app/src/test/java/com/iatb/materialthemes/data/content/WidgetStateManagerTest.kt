package com.iatb.materialthemes.data.content

import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.model.WidgetStateEntity
import com.iatb.materialthemes.data.content.repository.ContentRepository
import com.iatb.materialthemes.data.content.selector.ContentSelector
import com.iatb.materialthemes.data.content.state.WidgetStateManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class WidgetStateManagerTest {

    private lateinit var fakeRepository: FakeContentRepository
    private lateinit var selector: ContentSelector
    private lateinit var stateManager: WidgetStateManager

    @Before
    fun setUp() {
        fakeRepository = FakeContentRepository()
        selector = ContentSelector(fakeRepository)
        stateManager = WidgetStateManager(fakeRepository, selector)

        // Seed some English and Vietnamese content
        fakeRepository.addContent(
            ContentEntity(
                id = "q_en_1",
                type = ContentType.QUOTE,
                text = "English quote 1",
                author = "Author 1",
                language = "en",
                source = "test",
                contentHash = "hash_en_1",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )
        fakeRepository.addContent(
            ContentEntity(
                id = "q_en_2",
                type = ContentType.QUOTE,
                text = "English quote 2",
                author = "Author 2",
                language = "en",
                source = "test",
                contentHash = "hash_en_2",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )
        fakeRepository.addContent(
            ContentEntity(
                id = "q_vi_1",
                type = ContentType.QUOTE,
                text = "Danh ngôn tiếng Việt 1",
                author = "Tác giả 1",
                language = "vi",
                source = "test",
                contentHash = "hash_vi_1",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )
        fakeRepository.addContent(
            ContentEntity(
                id = "q_vi_2",
                type = ContentType.QUOTE,
                text = "Danh ngôn tiếng Việt 2",
                author = "Tác giả 2",
                language = "vi",
                source = "test",
                contentHash = "hash_vi_2",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )
    }

    @Test
    fun createInitialState_setsLanguageFromSystemLocale() = runBlocking {
        // Vietnamese locale
        val (stateVi, contentVi) = stateManager.createInitialStateWithContent(
            widgetId = 101,
            type = ContentType.QUOTE,
            systemLocale = Locale.forLanguageTag("vi-VN")
        )
        assertEquals("vi", stateVi.languageFilter)
        assertEquals("vi", contentVi.language)
        assertEquals(WidgetStateManager.DEFAULT_INTERVAL_MINUTES, stateVi.changeIntervalMinutes)
        assertTrue(stateVi.nextChangeAt > stateVi.lastChangedAt)

        // English / Other locale
        val (stateEn, contentEn) = stateManager.createInitialStateWithContent(
            widgetId = 102,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )
        assertEquals("en", stateEn.languageFilter)
        assertEquals("en", contentEn.language)
    }

    @Test
    fun refreshContent_updatesOnlyTargetWidget() = runBlocking {
        val state1 = stateManager.createInitialState(
            widgetId = 201,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )
        val state2 = stateManager.createInitialState(
            widgetId = 202,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )

        val originalId1 = state1.contentId
        val originalId2 = state2.contentId

        // Manual refresh widget 201
        val result = stateManager.refreshContent(201)
        assertNotNull(result)

        val refreshedState1 = fakeRepository.getWidgetState(201)
        val untouchedState2 = fakeRepository.getWidgetState(202)

        assertNotEquals("Widget 201 must select a different quote on refresh", originalId1, refreshedState1?.contentId)
        assertEquals("Widget 202 must remain completely unchanged", originalId2, untouchedState2?.contentId)
        assertTrue(refreshedState1!!.nextChangeAt > refreshedState1.lastChangedAt)
    }

    @Test
    fun updateLanguageFilter_changesContentWhenCurrentBecomesInvalid() = runBlocking {
        // Initially English
        val (state, initialContent) = stateManager.createInitialStateWithContent(
            widgetId = 301,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )
        assertEquals("en", initialContent.language)

        // Switch filter to only Vietnamese
        val updateResult = stateManager.updateLanguageFilter(301, listOf("vi"))
        assertNotNull(updateResult)

        val (newState, newContent) = updateResult!!
        assertEquals("vi", newState.languageFilter)
        assertEquals("vi", newContent.language)
        assertNotEquals("Content must change to Vietnamese", initialContent.id, newContent.id)
    }

    @Test
    fun updateLanguageFilter_retainsContentWhenCurrentStillMatches() = runBlocking {
        // Initially English
        val (state, initialContent) = stateManager.createInitialStateWithContent(
            widgetId = 302,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )
        assertEquals("en", initialContent.language)

        // Add Vietnamese to existing English (now [en, vi])
        val updateResult = stateManager.updateLanguageFilter(302, listOf("en", "vi"))
        assertNotNull(updateResult)

        val (newState, newContent) = updateResult!!
        assertEquals("en,vi", newState.languageFilter)
        assertEquals("Should retain existing English quote because it still matches", initialContent.id, newContent.id)
    }

    @Test
    fun updateInterval_updatesMinutesAndRecalculatesNextChangeAt() = runBlocking {
        val state = stateManager.createInitialState(
            widgetId = 401,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )

        val newInterval = 180L // 3 hours
        val updated = stateManager.updateInterval(401, newInterval)

        assertNotNull(updated)
        assertEquals(newInterval, updated?.changeIntervalMinutes)
        assertEquals(state.contentId, updated?.contentId)
        assertTrue(updated!!.nextChangeAt > System.currentTimeMillis())
    }

    @Test
    fun deleteWidget_removesWidgetStateRecord() = runBlocking {
        stateManager.createInitialState(
            widgetId = 501,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )
        assertNotNull(fakeRepository.getWidgetState(501))

        stateManager.deleteWidget(501)
        assertNull(fakeRepository.getWidgetState(501))
    }

    @Test
    fun refreshContent_nonExistentWidget_returnsNull() = runBlocking {
        val result = stateManager.refreshContent(99999)
        assertNull("Refreshing non-existent widget must safely return null", result)
    }

    @Test
    fun updateInterval_nonExistentWidget_returnsNull() = runBlocking {
        val result = stateManager.updateInterval(99999, 60L)
        assertNull("Updating interval of non-existent widget must safely return null", result)
    }

    @Test
    fun updateLanguageFilter_withUppercaseAndSpaces_normalizesCorrectly() = runBlocking {
        val state = stateManager.createInitialState(
            widgetId = 601,
            type = ContentType.QUOTE,
            systemLocale = Locale.US
        )
        assertEquals("en", state.languageFilter)

        // Pass with uppercase and spaces: [" VI ", " En "]
        val updated = stateManager.updateLanguageFilter(601, listOf(" VI ", " En "))
        assertNotNull(updated)

        val (newState, newContent) = updated!!
        assertEquals("vi,en", newState.languageFilter)
        assertTrue(newContent.language in listOf("vi", "en"))
    }

    @Test
    fun parseAndFormatLanguages_handlesWhitespaceAndUppercase() {
        val formatted = stateManager.formatLanguages(listOf(" EN ", "vi", "En", "  "))
        assertEquals("en,vi", formatted)

        val parsed = stateManager.parseLanguages(" EN , vi , En , ")
        assertEquals(listOf("en", "vi"), parsed)

        // Empty fallback
        assertEquals(listOf("en"), stateManager.parseLanguages(""))
        assertEquals("en", stateManager.formatLanguages(emptyList()))
    }

    private class FakeContentRepository : ContentRepository {
        private val items = mutableListOf<ContentEntity>()
        private val widgetStates = mutableMapOf<Int, WidgetStateEntity>()

        fun addContent(item: ContentEntity) {
            items.add(item)
        }

        override suspend fun getWidgetState(widgetId: Int): WidgetStateEntity? = widgetStates[widgetId]

        override suspend fun saveWidgetState(state: WidgetStateEntity) {
            widgetStates[state.widgetId] = state
        }

        override suspend fun deleteWidgetState(widgetId: Int) {
            widgetStates.remove(widgetId)
        }

        override suspend fun getAllActiveWidgetStates(): List<WidgetStateEntity> = widgetStates.values.toList()

        override suspend fun getContentById(id: String): ContentEntity? = items.find { it.id == id }

        override suspend fun markContentAsShown(contentId: String) {
            val index = items.indexOfFirst { it.id == contentId }
            if (index != -1) {
                val item = items[index]
                items[index] = item.copy(
                    lastShownAt = System.currentTimeMillis(),
                    shownCount = item.shownCount + 1
                )
            }
        }

        override suspend fun getCandidateContent(
            type: ContentType,
            languages: List<String>,
            currentId: String?,
            limit: Int
        ): List<ContentEntity> {
            return items.filter { it.enabled && it.type == type && it.language in languages && it.id != currentId }
                .sortedWith(compareBy({ it.lastShownAt != null }, { it.lastShownAt ?: 0L }))
                .take(limit)
        }

        override suspend fun getFallbackContent(type: ContentType, language: String): ContentEntity {
            return ContentEntity(
                id = "fallback_${type.name.lowercase(Locale.ROOT)}_$language",
                type = type,
                text = "Fallback text for $language",
                author = if (type == ContentType.QUOTE) "Fallback Author" else null,
                language = language,
                source = "bundled",
                contentHash = "fallback_hash_$language",
                enabled = true,
                createdAt = 0L
            )
        }

        override suspend fun getTotalContentCount(): Int = items.size
    }
}
