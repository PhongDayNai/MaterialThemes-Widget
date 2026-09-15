package com.iatb.materialthemes.data.content

import com.iatb.materialthemes.data.content.model.ContentType
import com.iatb.materialthemes.data.content.util.ContentNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContentNormalizerTest {

    @Test
    fun normalizeText_trimsWhitespaceAndReplacesQuotes() {
        val raw = "  “Stay hungry,   stay foolish.”  "
        val normalized = ContentNormalizer.normalizeText(raw)
        assertEquals("\"Stay hungry, stay foolish.\"", normalized)
    }

    @Test
    fun normalizeText_replacesSingleCurlyQuotes() {
        val raw = "‘Knowledge is power’"
        val normalized = ContentNormalizer.normalizeText(raw)
        assertEquals("'Knowledge is power'", normalized)
    }

    @Test
    fun calculateHash_isDeterministicAndCaseInsensitive() {
        val hash1 = ContentNormalizer.calculateHash(
            ContentType.QUOTE,
            "\"Be the change that you wish to see in the world.\"",
            "Mahatma Gandhi"
        )
        val hash2 = ContentNormalizer.calculateHash(
            ContentType.QUOTE,
            "  “be the change that you wish to see in the world.”  ",
            "mahatma gandhi  "
        )
        assertEquals(hash1, hash2)
    }

    @Test
    fun calculateHash_differentiatesBetweenQuoteAndThought() {
        val text = "What is one thing you are grateful for today?"
        val quoteHash = ContentNormalizer.calculateHash(ContentType.QUOTE, text, null)
        val thoughtHash = ContentNormalizer.calculateHash(ContentType.THOUGHT, text, null)
        assertNotEquals(quoteHash, thoughtHash)
    }

    @Test
    fun calculateHash_differentiatesDifferentAuthors() {
        val text = "Live in the present."
        val hashA = ContentNormalizer.calculateHash(ContentType.QUOTE, text, "Author A")
        val hashB = ContentNormalizer.calculateHash(ContentType.QUOTE, text, "Author B")
        assertNotEquals(hashA, hashB)
    }

    @Test
    fun calculateHash_omitsAuthorForThought() {
        val text = "What is one thing you are grateful for today?"
        val hashWithAuthor = ContentNormalizer.calculateHash(ContentType.THOUGHT, text, "Ignored Author")
        val hashWithoutAuthor = ContentNormalizer.calculateHash(ContentType.THOUGHT, text, null)
        assertEquals("Author must be omitted for THOUGHT", hashWithoutAuthor, hashWithAuthor)
    }

    @Test
    fun calculateHash_handlesTurkishLocaleInvariance() {
        val defaultLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"))
            val hashTr = ContentNormalizer.calculateHash(ContentType.QUOTE, "INSPIRATION", "AUTHOR")
            java.util.Locale.setDefault(java.util.Locale.US)
            val hashUs = ContentNormalizer.calculateHash(ContentType.QUOTE, "INSPIRATION", "AUTHOR")
            assertEquals("Hash must be identical across different system locales (Locale.ROOT)", hashUs, hashTr)
        } finally {
            java.util.Locale.setDefault(defaultLocale)
        }
    }
}
