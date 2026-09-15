package com.iatb.materialthemes.render

import android.appwidget.AppWidgetManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.iatb.materialthemes.R
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuoteAndThoughtWidgetRendererTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun quoteRenderer_bindsQuoteTextAndAuthorCorrectly() {
        val quote = ContentEntity(
            id = "q_test_1",
            type = ContentType.QUOTE,
            text = "Be the change that you wish to see in the world.",
            author = "Mahatma Gandhi",
            category = "Inspirational",
            language = "en",
            source = "test",
            contentHash = "hash1",
            enabled = true,
            createdAt = 1000L
        )

        val remoteViews = QuoteWidgetRenderer.render(
            context = context,
            appWidgetId = 1001,
            content = quote,
            minWidthDp = 240,
            minHeightDp = 120
        )
        assertNotNull(remoteViews)

        // Inflate RemoteViews into FrameLayout to verify rendered layout
        val root = FrameLayout(context)
        val inflated = remoteViews.apply(context, root)

        val tvQuote = inflated.findViewById<TextView>(R.id.tvQuoteText)
        val tvAuthor = inflated.findViewById<TextView>(R.id.tvQuoteAuthor)
        val tvCategory = inflated.findViewById<TextView>(R.id.tvQuoteCategory)

        assertNotNull(tvQuote)
        assertEquals("Be the change that you wish to see in the world.", tvQuote.text.toString())

        assertNotNull(tvAuthor)
        assertEquals("— Mahatma Gandhi", tvAuthor.text.toString())

        assertNotNull(tvCategory)
        assertEquals("Inspirational", tvCategory.text.toString())
        assertEquals(View.VISIBLE, tvCategory.visibility)
    }

    @Test
    fun quoteRenderer_handlesVietnameseDiacriticsCleanly() {
        val viQuote = ContentEntity(
            id = "q_vi_test",
            type = ContentType.QUOTE,
            text = "Học, học nữa, học mãi. Không có việc gì khó, chỉ sợ lòng không bền.",
            author = "V.I. Lê-nin",
            category = "Học tập",
            language = "vi",
            source = "test",
            contentHash = "hash_vi",
            enabled = true,
            createdAt = 1000L
        )

        val remoteViews = QuoteWidgetRenderer.render(
            context = context,
            appWidgetId = 1002,
            content = viQuote,
            minWidthDp = 200,
            minHeightDp = 100
        )

        val root = FrameLayout(context)
        val inflated = remoteViews.apply(context, root)

        val tvQuote = inflated.findViewById<TextView>(R.id.tvQuoteText)
        val tvAuthor = inflated.findViewById<TextView>(R.id.tvQuoteAuthor)

        assertEquals("Học, học nữa, học mãi. Không có việc gì khó, chỉ sợ lòng không bền.", tvQuote.text.toString())
        assertEquals("— V.I. Lê-nin", tvAuthor.text.toString())
    }

    @Test
    fun quoteRenderer_hidesCategoryWhenNarrow() {
        val quote = ContentEntity(
            id = "q_narrow",
            type = ContentType.QUOTE,
            text = "Stay hungry, stay foolish.",
            author = "Steve Jobs",
            category = "Inspirational",
            language = "en",
            source = "test",
            contentHash = "hash_narrow",
            enabled = true,
            createdAt = 1000L
        )

        val remoteViews = QuoteWidgetRenderer.render(
            context = context,
            appWidgetId = 1003,
            content = quote,
            minWidthDp = 160, // narrow
            minHeightDp = 100
        )

        val root = FrameLayout(context)
        val inflated = remoteViews.apply(context, root)
        val tvCategory = inflated.findViewById<TextView>(R.id.tvQuoteCategory)

        assertEquals(View.GONE, tvCategory.visibility)
    }

    @Test
    fun quoteRenderer_handlesNullAuthorGracefully() {
        val quote = ContentEntity(
            id = "q_null_author",
            type = ContentType.QUOTE,
            text = "An anonymous quote without author.",
            author = null,
            category = null,
            language = "en",
            source = "test",
            contentHash = "hash_null",
            enabled = true,
            createdAt = 1000L
        )

        val remoteViews = QuoteWidgetRenderer.render(
            context = context,
            appWidgetId = 1004,
            content = quote
        )

        val root = FrameLayout(context)
        val inflated = remoteViews.apply(context, root)
        val tvAuthor = inflated.findViewById<TextView>(R.id.tvQuoteAuthor)

        assertEquals("", tvAuthor.text.toString())
    }

    @Test
    fun thoughtRenderer_bindsThoughtTextWithoutAuthor() {
        val thought = ContentEntity(
            id = "t_test_1",
            type = ContentType.THOUGHT,
            text = "What is one small kindness you can offer yourself today?",
            author = null,
            category = "Mindfulness",
            language = "en",
            source = "test",
            contentHash = "hash_thought",
            enabled = true,
            createdAt = 1000L
        )

        val remoteViews = ThoughtWidgetRenderer.render(
            context = context,
            appWidgetId = 2001,
            content = thought,
            minWidthDp = 250,
            minHeightDp = 120
        )

        val root = FrameLayout(context)
        val inflated = remoteViews.apply(context, root)

        val tvThought = inflated.findViewById<TextView>(R.id.tvThoughtText)
        val tvCategory = inflated.findViewById<TextView>(R.id.tvThoughtCategory)

        assertNotNull(tvThought)
        assertEquals("What is one small kindness you can offer yourself today?", tvThought.text.toString())

        assertNotNull(tvCategory)
        assertEquals("Mindfulness", tvCategory.text.toString())
        assertEquals(View.VISIBLE, tvCategory.visibility)
    }

    @Test
    fun thoughtRenderer_handlesVietnameseDiacriticsCleanly() {
        val viThought = ContentEntity(
            id = "t_vi_test",
            type = ContentType.THOUGHT,
            text = "Điều gì khiến tâm trí bạn bình yên nhất lúc này?",
            author = null,
            category = "Chánh niệm",
            language = "vi",
            source = "test",
            contentHash = "hash_vi_thought",
            enabled = true,
            createdAt = 1000L
        )

        val remoteViews = ThoughtWidgetRenderer.render(
            context = context,
            appWidgetId = 2002,
            content = viThought,
            minWidthDp = 220,
            minHeightDp = 110
        )

        val root = FrameLayout(context)
        val inflated = remoteViews.apply(context, root)
        val tvThought = inflated.findViewById<TextView>(R.id.tvThoughtText)

        assertEquals("Điều gì khiến tâm trí bạn bình yên nhất lúc này?", tvThought.text.toString())
    }
}
