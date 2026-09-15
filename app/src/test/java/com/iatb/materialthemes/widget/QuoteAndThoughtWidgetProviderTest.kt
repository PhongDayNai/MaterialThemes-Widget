package com.iatb.materialthemes.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.room.Room
import com.iatb.materialthemes.data.content.database.ContentDatabase
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.repository.ContentRepositoryImpl
import com.iatb.materialthemes.data.content.state.WidgetStateManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuoteAndThoughtWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var database: ContentDatabase
    private lateinit var appWidgetManager: AppWidgetManager

    @Before
    fun setUp() = runBlocking {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, ContentDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        appWidgetManager = AppWidgetManager.getInstance(context)

        // Seed some quotes and thoughts into database
        val q1 = ContentEntity(
            id = "q_prov_1",
            type = ContentType.QUOTE,
            text = "The journey of a thousand miles begins with one step.",
            author = "Lao Tzu",
            language = "en",
            source = "test",
            contentHash = "hq1",
            enabled = true,
            createdAt = 1000L
        )
        val q2 = ContentEntity(
            id = "q_prov_2",
            type = ContentType.QUOTE,
            text = "An unexamined life is not worth living.",
            author = "Socrates",
            language = "en",
            source = "test",
            contentHash = "hq2",
            enabled = true,
            createdAt = 1000L
        )
        val t1 = ContentEntity(
            id = "t_prov_1",
            type = ContentType.THOUGHT,
            text = "What is truly important right now?",
            author = null,
            language = "en",
            source = "test",
            contentHash = "ht1",
            enabled = true,
            createdAt = 1000L
        )
        database.contentDao().insertAll(listOf(q1, q2, t1))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun quoteWidgetProvider_renderWidget_preservesContentOnRecreate() = runBlocking {
        val shadowManager = org.robolectric.Shadows.shadowOf(appWidgetManager)
        val widgetId = shadowManager.createWidget(QuoteWidgetProvider::class.java, com.iatb.materialthemes.R.layout.widget_quote)

        // Initial render
        QuoteWidgetProvider.renderWidget(context, appWidgetManager, widgetId)
        val stateManager = WidgetStateManager.getInstance(context)
        val firstState = stateManager.getState(widgetId)
        assertNotNull("Widget state must be initialized", firstState)
        val initialContentId = firstState?.contentId
        assertNotNull("Initial content ID must exist", initialContentId)

        // Simulate Recreate / Redraw (e.g. orientation change, screen on, configuration change)
        QuoteWidgetProvider.renderWidget(context, appWidgetManager, widgetId)
        val secondState = stateManager.getState(widgetId)

        // Invariant 3: Recreate must NEVER rotate content; it must preserve initial contentId
        assertEquals(
            "Recreating or redrawing widget must preserve exact same contentId",
            initialContentId,
            secondState?.contentId
        )
    }

    @Test
    fun thoughtWidgetProvider_renderWidget_preservesContentOnRecreate() = runBlocking {
        val shadowManager = org.robolectric.Shadows.shadowOf(appWidgetManager)
        val widgetId = shadowManager.createWidget(ThoughtWidgetProvider::class.java, com.iatb.materialthemes.R.layout.widget_thought)

        // Initial render
        ThoughtWidgetProvider.renderWidget(context, appWidgetManager, widgetId)
        val stateManager = WidgetStateManager.getInstance(context)
        val firstState = stateManager.getState(widgetId)
        assertNotNull("Widget state must be initialized", firstState)
        val initialContentId = firstState?.contentId
        assertNotNull("Initial content ID must exist", initialContentId)

        // Simulate Recreate
        ThoughtWidgetProvider.renderWidget(context, appWidgetManager, widgetId)
        val secondState = stateManager.getState(widgetId)

        assertEquals(
            "Recreating thought widget must preserve exact same contentId",
            initialContentId,
            secondState?.contentId
        )
    }

    @Test
    fun quoteWidgetProvider_onDeleted_cleansUpState() = runBlocking {
        val shadowManager = org.robolectric.Shadows.shadowOf(appWidgetManager)
        val widgetId = shadowManager.createWidget(QuoteWidgetProvider::class.java, com.iatb.materialthemes.R.layout.widget_quote)
        val stateManager = WidgetStateManager.getInstance(context)

        QuoteWidgetProvider.renderWidget(context, appWidgetManager, widgetId)
        assertNotNull(stateManager.getState(widgetId))

        stateManager.deleteWidget(widgetId)
        assertNull(stateManager.getState(widgetId))
    }
}
