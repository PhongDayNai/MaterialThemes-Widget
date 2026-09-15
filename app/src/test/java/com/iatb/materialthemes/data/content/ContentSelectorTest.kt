package com.iatb.materialthemes.data.content

import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.model.WidgetStateEntity
import com.iatb.materialthemes.data.content.repository.ContentRepository
import com.iatb.materialthemes.data.content.selector.ContentSelector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContentSelectorTest {

    private lateinit var fakeRepository: FakeContentRepository
    private lateinit var selector: ContentSelector

    @Before
    fun setUp() {
        fakeRepository = FakeContentRepository()
        selector = ContentSelector(fakeRepository)
    }

    @Test
    fun selectNextContent_strictlyAdheresToSelectedLanguage() = runBlocking {
        // Setup 5 English quotes and 3 Vietnamese quotes
        for (i in 1..5) {
            fakeRepository.addContent(
                ContentEntity(
                    id = "en_$i",
                    type = ContentType.QUOTE,
                    text = "Quote $i in English",
                    author = "Author $i",
                    language = "en",
                    source = "test",
                    contentHash = "hash_en_$i",
                    enabled = true,
                    createdAt = 1000L,
                    lastShownAt = null,
                    shownCount = 0
                )
            )
        }
        for (i in 1..3) {
            fakeRepository.addContent(
                ContentEntity(
                    id = "vi_$i",
                    type = ContentType.QUOTE,
                    text = "Trích dẫn $i tiếng Việt",
                    author = "Tác giả $i",
                    language = "vi",
                    source = "test",
                    contentHash = "hash_vi_$i",
                    enabled = true,
                    createdAt = 1000L,
                    lastShownAt = null,
                    shownCount = 0
                )
            )
        }

        // When only Vietnamese is selected
        for (repeat in 1..10) {
            val selected = selector.selectNextContent(
                type = ContentType.QUOTE,
                selectedLanguages = listOf("vi"),
                currentContentId = null
            )
            assertEquals("Language must be Vietnamese", "vi", selected.language)
            assertTrue("Must belong to Vietnamese items", selected.id.startsWith("vi_"))
        }

        // When only English is selected
        for (repeat in 1..10) {
            val selected = selector.selectNextContent(
                type = ContentType.QUOTE,
                selectedLanguages = listOf("en"),
                currentContentId = null
            )
            assertEquals("Language must be English", "en", selected.language)
            assertTrue("Must belong to English items", selected.id.startsWith("en_"))
        }
    }

    @Test
    fun selectNextContent_avoidsImmediateRepetition() = runBlocking {
        fakeRepository.addContent(
            ContentEntity(
                id = "item_A",
                type = ContentType.QUOTE,
                text = "Quote A",
                author = "Author",
                language = "en",
                source = "test",
                contentHash = "hash_A",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )
        fakeRepository.addContent(
            ContentEntity(
                id = "item_B",
                type = ContentType.QUOTE,
                text = "Quote B",
                author = "Author",
                language = "en",
                source = "test",
                contentHash = "hash_B",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )

        val first = selector.selectNextContent(
            type = ContentType.QUOTE,
            selectedLanguages = listOf("en"),
            currentContentId = "item_A"
        )
        assertNotEquals("Should not immediately select the same content", "item_A", first.id)
        assertEquals("item_B", first.id)
    }

    @Test
    fun selectNextContent_prefersUnshownContentFirst() = runBlocking {
        // 3 items already shown
        for (i in 1..3) {
            fakeRepository.addContent(
                ContentEntity(
                    id = "shown_$i",
                    type = ContentType.QUOTE,
                    text = "Quote shown $i",
                    author = "Author",
                    language = "en",
                    source = "test",
                    contentHash = "hash_shown_$i",
                    enabled = true,
                    createdAt = 1000L,
                    lastShownAt = 5000L + i,
                    shownCount = 1
                )
            )
        }

        // 1 brand new unshown item
        fakeRepository.addContent(
            ContentEntity(
                id = "unshown_new",
                type = ContentType.QUOTE,
                text = "New Quote",
                author = "Author",
                language = "en",
                source = "test",
                contentHash = "hash_new",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )

        val selected = selector.selectNextContent(
            type = ContentType.QUOTE,
            selectedLanguages = listOf("en"),
            currentContentId = null
        )
        assertEquals("Must select the unshown item first", "unshown_new", selected.id)
    }

    @Test
    fun selectNextContent_fallbackDoesNotViolateSelectedLanguage() = runBlocking {
        // Repository has ONLY English content, but user requests Vietnamese
        fakeRepository.addContent(
            ContentEntity(
                id = "only_en",
                type = ContentType.QUOTE,
                text = "English Only",
                author = "Author",
                language = "en",
                source = "test",
                contentHash = "hash_en_only",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )

        val fallback = selector.selectNextContent(
            type = ContentType.QUOTE,
            selectedLanguages = listOf("vi"),
            currentContentId = null
        )

        // Strict invariant: fallback must match requested language ("vi"), NEVER silently switch to "en"
        assertEquals("vi", fallback.language)
    }

    @Test
    fun selectNextContent_retainsValidCurrentContentIfNoCandidates() = runBlocking {
        val current = ContentEntity(
            id = "current_valid",
            type = ContentType.QUOTE,
            text = "Single valid quote",
            author = "Author",
            language = "vi",
            source = "test",
            contentHash = "hash_valid",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = 5000L,
            shownCount = 1
        )
        fakeRepository.addContent(current)

        // Only 1 item exists, and it is excluded as currentId
        val result = selector.selectNextContent(
            type = ContentType.QUOTE,
            selectedLanguages = listOf("vi"),
            currentContentId = "current_valid"
        )

        assertEquals("Should retain current valid content when no alternatives exist", "current_valid", result.id)
    }

    @Test
    fun selectNextContent_multiLanguage_returnsBalancedCandidates() = runBlocking {
        // Seed 10 English and 10 Vietnamese quotes
        for (i in 1..10) {
            fakeRepository.addContent(
                ContentEntity(
                    id = "en_$i",
                    type = ContentType.QUOTE,
                    text = "Quote $i EN",
                    author = "Author $i",
                    language = "en",
                    source = "test",
                    contentHash = "hash_en_$i",
                    enabled = true,
                    createdAt = 1000L,
                    lastShownAt = null,
                    shownCount = 0
                )
            )
            fakeRepository.addContent(
                ContentEntity(
                    id = "vi_$i",
                    type = ContentType.QUOTE,
                    text = "Quote $i VI",
                    author = "Author $i",
                    language = "vi",
                    source = "test",
                    contentHash = "hash_vi_$i",
                    enabled = true,
                    createdAt = 1000L,
                    lastShownAt = null,
                    shownCount = 0
                )
            )
        }

        val languagesFound = mutableSetOf<String>()
        // Repeat 20 times to verify both languages are chosen
        for (repeat in 1..20) {
            val selected = selector.selectNextContent(
                type = ContentType.QUOTE,
                selectedLanguages = listOf("en", "vi"),
                currentContentId = null
            )
            languagesFound.add(selected.language)
        }

        assertTrue("Must include English content", languagesFound.contains("en"))
        assertTrue("Must include Vietnamese content (no starvation)", languagesFound.contains("vi"))
    }

    @Test
    fun selectNextContent_isolatesContentType() = runBlocking {
        fakeRepository.addContent(
            ContentEntity(
                id = "quote_item",
                type = ContentType.QUOTE,
                text = "Quote Text",
                author = "Author",
                language = "en",
                source = "test",
                contentHash = "h_quote",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )
        fakeRepository.addContent(
            ContentEntity(
                id = "thought_item",
                type = ContentType.THOUGHT,
                text = "Thought Text",
                author = null,
                language = "en",
                source = "test",
                contentHash = "h_thought",
                enabled = true,
                createdAt = 1000L,
                lastShownAt = null,
                shownCount = 0
            )
        )

        for (repeat in 1..10) {
            val selectedThought = selector.selectNextContent(
                type = ContentType.THOUGHT,
                selectedLanguages = listOf("en"),
                currentContentId = null
            )
            assertEquals(ContentType.THOUGHT, selectedThought.type)
            assertEquals("thought_item", selectedThought.id)

            val selectedQuote = selector.selectNextContent(
                type = ContentType.QUOTE,
                selectedLanguages = listOf("en"),
                currentContentId = null
            )
            assertEquals(ContentType.QUOTE, selectedQuote.type)
            assertEquals("quote_item", selectedQuote.id)
        }
    }

    @Test
    fun selectNextContent_whenAllShown_picksFromOldestCandidates() = runBlocking {
        // Add 10 items, all shown, with distinct lastShownAt timestamps
        for (i in 1..10) {
            fakeRepository.addContent(
                ContentEntity(
                    id = "shown_item_$i",
                    type = ContentType.QUOTE,
                    text = "Quote $i",
                    author = "Author $i",
                    language = "en",
                    source = "test",
                    contentHash = "h_$i",
                    enabled = true,
                    createdAt = 1000L,
                    lastShownAt = 1000L * i, // oldest is shown_item_1
                    shownCount = 1
                )
            )
        }

        // The top 5 oldest shown are shown_item_1 to shown_item_5
        val validOldestIds = (1..5).map { "shown_item_$it" }.toSet()
        for (repeat in 1..20) {
            val selected = selector.selectNextContent(
                type = ContentType.QUOTE,
                selectedLanguages = listOf("en"),
                currentContentId = null
            )
            assertTrue(
                "Selected id ${selected.id} must be among top oldest shown: $validOldestIds",
                validOldestIds.contains(selected.id)
            )
        }
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
                id = "fallback_${type.name.lowercase()}_$language",
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
