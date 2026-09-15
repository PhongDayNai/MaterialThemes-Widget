package com.iatb.materialthemes.data.content

import android.content.Context
import androidx.room.Room
import com.iatb.materialthemes.data.content.database.ContentDao
import com.iatb.materialthemes.data.content.database.ContentDatabase
import com.iatb.materialthemes.data.content.database.ContentSourceDao
import com.iatb.materialthemes.data.content.database.WidgetStateDao
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentSourceEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.model.WidgetStateEntity
import com.iatb.materialthemes.data.content.repository.ContentRepositoryImpl
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomContentDaoAndRepositoryTest {

    private lateinit var database: ContentDatabase
    private lateinit var contentDao: ContentDao
    private lateinit var widgetStateDao: WidgetStateDao
    private lateinit var contentSourceDao: ContentSourceDao
    private lateinit var repository: ContentRepositoryImpl
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        contentDao = database.contentDao()
        widgetStateDao = database.widgetStateDao()
        contentSourceDao = database.contentSourceDao()
        repository = ContentRepositoryImpl(context, database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun contentDao_getCandidates_filtersByTypeAndLanguageAndExcludesCurrentId() = runBlocking {
        val q1 = ContentEntity(
            id = "q1",
            type = ContentType.QUOTE,
            text = "Quote 1 English",
            author = "Author 1",
            category = "Inspirational",
            language = "en",
            source = "test",
            contentHash = "hash1",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        val q2 = ContentEntity(
            id = "q2",
            type = ContentType.QUOTE,
            text = "Quote 2 English",
            author = "Author 2",
            category = "Inspirational",
            language = "en",
            source = "test",
            contentHash = "hash2",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = 5000L,
            shownCount = 1
        )
        val qVi = ContentEntity(
            id = "q3",
            type = ContentType.QUOTE,
            text = "Trích dẫn Tiếng Việt",
            author = "Tác giả",
            category = "Khắc kỷ",
            language = "vi",
            source = "test",
            contentHash = "hash3",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        val thought1 = ContentEntity(
            id = "t1",
            type = ContentType.THOUGHT,
            text = "Thought 1 English",
            author = null,
            category = "Mindfulness",
            language = "en",
            source = "test",
            contentHash = "hash4",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )

        contentDao.insertAll(listOf(q1, q2, qVi, thought1))

        // 1. Filter English Quotes excluding q1
        val enCandidates = contentDao.getCandidates(
            type = ContentType.QUOTE,
            languages = listOf("en"),
            excludeId = "q1",
            limit = 10
        )
        assertEquals(1, enCandidates.size)
        assertEquals("q2", enCandidates[0].id)

        // 2. Filter Vietnamese Quotes only
        val viCandidates = contentDao.getCandidates(
            type = ContentType.QUOTE,
            languages = listOf("vi"),
            excludeId = null,
            limit = 10
        )
        assertEquals(1, viCandidates.size)
        assertEquals("q3", viCandidates[0].id)
        assertEquals("vi", viCandidates[0].language)

        // 3. Priority: Unshown (lastShownAt == null) must precede already shown
        val allEnQuotes = contentDao.getCandidates(
            type = ContentType.QUOTE,
            languages = listOf("en"),
            excludeId = null,
            limit = 10
        )
        assertEquals(2, allEnQuotes.size)
        assertEquals("Unshown quote must come first", "q1", allEnQuotes[0].id)
        assertEquals("Shown quote comes second", "q2", allEnQuotes[1].id)
    }

    @Test
    fun contentDao_updateShown_updatesTimestampAndIncrementsCount() = runBlocking {
        val q = ContentEntity(
            id = "q_update",
            type = ContentType.QUOTE,
            text = "Test Quote",
            author = "Author",
            category = null,
            language = "en",
            source = "test",
            contentHash = "hash_update",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        contentDao.insertAll(listOf(q))

        val now = 123456789L
        val rowsAffected = contentDao.updateShown("q_update", now)
        assertEquals(1, rowsAffected)

        val updated = contentDao.getById("q_update")
        assertNotNull(updated)
        assertEquals(now, updated?.lastShownAt)
        assertEquals(1, updated?.shownCount)
    }

    @Test
    fun contentDao_getRandomFallback_strictlyMatchesRequestedLanguage() = runBlocking {
        val qEn = ContentEntity(
            id = "q_en",
            type = ContentType.QUOTE,
            text = "English Quote",
            author = "Author",
            category = null,
            language = "en",
            source = "test",
            contentHash = "hash_en",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        val qVi = ContentEntity(
            id = "q_vi",
            type = ContentType.QUOTE,
            text = "Tiếng Việt Quote",
            author = "Tác giả",
            category = null,
            language = "vi",
            source = "test",
            contentHash = "hash_vi",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        contentDao.insertAll(listOf(qEn, qVi))

        val viFallback = contentDao.getRandomFallback(ContentType.QUOTE, "vi")
        assertNotNull(viFallback)
        assertEquals("vi", viFallback?.language)
        assertEquals("q_vi", viFallback?.id)

        val enFallback = contentDao.getRandomFallback(ContentType.QUOTE, "en")
        assertNotNull(enFallback)
        assertEquals("en", enFallback?.language)
        assertEquals("q_en", enFallback?.id)
    }

    @Test
    fun widgetStateDao_crudOperations_workCorrectly() = runBlocking {
        val state = WidgetStateEntity(
            widgetId = 101,
            type = ContentType.QUOTE,
            contentId = "q1",
            languageFilter = "en,vi",
            changeIntervalMinutes = 1440,
            lastChangedAt = 1000L,
            nextChangeAt = 2000L,
            updatedAt = 1000L
        )

        widgetStateDao.insertOrUpdate(state)
        val fetched = widgetStateDao.getState(101)
        assertNotNull(fetched)
        assertEquals(101, fetched?.widgetId)
        assertEquals("en,vi", fetched?.languageFilter)

        val allStates = widgetStateDao.getAllActiveStates()
        assertEquals(1, allStates.size)

        widgetStateDao.deleteState(101)
        assertNull(widgetStateDao.getState(101))
    }

    @Test
    fun contentSourceDao_crudOperations_workCorrectly() = runBlocking {
        val source = ContentSourceEntity(
            id = "src1",
            name = "Quotable",
            url = "https://api.quotable.io",
            type = ContentType.QUOTE,
            enabled = true,
            lastSyncAt = null,
            nextSyncAt = null,
            syncError = null
        )

        contentSourceDao.insertOrUpdate(listOf(source))
        val sources = contentSourceDao.getEnabledSources(ContentType.QUOTE)
        assertEquals(1, sources.size)
        assertEquals("src1", sources[0].id)

        contentSourceDao.updateSyncResult("src1", 5000L, 10000L, null)
        val updatedSources = contentSourceDao.getAllSources()
        assertEquals(5000L, updatedSources[0].lastSyncAt)
        assertEquals(10000L, updatedSources[0].nextSyncAt)
    }

    @Test
    fun contentRepository_getFallbackContent_returnsCorrectLanguageAndNoNull() = runBlocking {
        // Empty DB test
        val viFallback = repository.getFallbackContent(ContentType.QUOTE, "vi")
        assertEquals("vi", viFallback.language)
        assertTrue(viFallback.text.isNotEmpty())
        assertTrue(viFallback.author?.isNotEmpty() == true)

        val enFallback = repository.getFallbackContent(ContentType.THOUGHT, "en")
        assertEquals("en", enFallback.language)
        assertTrue(enFallback.text.isNotEmpty())
        assertNull(enFallback.author)

        // Non-empty DB test
        val customViQuote = ContentEntity(
            id = "custom_vi",
            type = ContentType.QUOTE,
            text = "Một câu danh ngôn tiếng Việt từ DB",
            author = "Tác giả DB",
            category = "Triết lý",
            language = "vi",
            source = "bundled",
            contentHash = "hash_custom_vi",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        contentDao.insertAll(listOf(customViQuote))

        val dbViFallback = repository.getFallbackContent(ContentType.QUOTE, "vi")
        assertEquals("custom_vi", dbViFallback.id)
        assertEquals("vi", dbViFallback.language)
    }

    @Test
    fun contentSelector_withRealRoomDatabase_selectsAndPrioritizesUnshown() = runBlocking {
        val q1 = ContentEntity(
            id = "room_q1",
            type = ContentType.QUOTE,
            text = "Quote 1",
            author = "Author 1",
            language = "en",
            source = "test",
            contentHash = "h1",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = 5000L,
            shownCount = 1
        )
        val q2 = ContentEntity(
            id = "room_q2",
            type = ContentType.QUOTE,
            text = "Quote 2 Unshown",
            author = "Author 2",
            language = "en",
            source = "test",
            contentHash = "h2",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        contentDao.insertAll(listOf(q1, q2))

        val selector = com.iatb.materialthemes.data.content.selector.ContentSelector(repository)
        val selected = selector.selectNextContent(
            type = ContentType.QUOTE,
            selectedLanguages = listOf("en"),
            currentContentId = null
        )
        assertEquals("Must pick unshown item from Room database", "room_q2", selected.id)

        val nextSelected = selector.selectNextContent(
            type = ContentType.QUOTE,
            selectedLanguages = listOf("en"),
            currentContentId = "room_q2"
        )
        assertEquals("Must exclude current item", "room_q1", nextSelected.id)
    }

    @Test
    fun widgetStateManager_withRealRoomDatabase_createsAndRefreshesState() = runBlocking {
        val qEn = ContentEntity(
            id = "room_state_en",
            type = ContentType.QUOTE,
            text = "State Quote EN",
            author = "Author EN",
            language = "en",
            source = "test",
            contentHash = "hse",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        val qVi = ContentEntity(
            id = "room_state_vi",
            type = ContentType.QUOTE,
            text = "State Quote VI",
            author = "Author VI",
            language = "vi",
            source = "test",
            contentHash = "hsv",
            enabled = true,
            createdAt = 1000L,
            lastShownAt = null,
            shownCount = 0
        )
        contentDao.insertAll(listOf(qEn, qVi))

        val selector = com.iatb.materialthemes.data.content.selector.ContentSelector(repository)
        val stateManager = com.iatb.materialthemes.data.content.state.WidgetStateManager(repository, selector)

        val state = stateManager.createInitialState(
            widgetId = 999,
            type = ContentType.QUOTE,
            systemLocale = java.util.Locale.US
        )
        assertEquals("en", state.languageFilter)
        assertEquals("room_state_en", state.contentId)

        // Verify persisted in Room DB
        val persisted = widgetStateDao.getState(999)
        assertNotNull(persisted)
        assertEquals("room_state_en", persisted?.contentId)

        // Update language filter to vi
        val updated = stateManager.updateLanguageFilter(999, listOf("vi"))
        assertNotNull(updated)
        assertEquals("vi", updated?.first?.languageFilter)
        assertEquals("room_state_vi", updated?.first?.contentId)

        // Verify persisted updated state in Room DB
        val persistedUpdated = widgetStateDao.getState(999)
        assertEquals("room_state_vi", persistedUpdated?.contentId)
        assertEquals("vi", persistedUpdated?.languageFilter)
    }
}
