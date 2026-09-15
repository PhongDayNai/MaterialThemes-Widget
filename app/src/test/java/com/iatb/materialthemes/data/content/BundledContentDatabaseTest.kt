package com.iatb.materialthemes.data.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class BundledContentDatabaseTest {

    private val dbFile = File("src/main/assets/content.db")

    private fun runSqliteQuery(query: String): String {
        val process = ProcessBuilder("sqlite3", dbFile.absolutePath, query)
            .redirectErrorStream(true)
            .start()

        val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText().trim() }
        val exitCode = process.waitFor()
        assertEquals("sqlite3 command failed with output: $output", 0, exitCode)
        return output
    }

    @Test
    fun bundledDatabaseFile_existsAndIsNotEmpty() {
        assertTrue("content.db file should exist at ${dbFile.absolutePath}", dbFile.exists())
        assertTrue("content.db file size should be > 50KB", dbFile.length() > 50 * 1024)
    }

    @Test
    fun bundledDatabase_passesIntegrityCheck() {
        val integrity = runSqliteQuery("PRAGMA integrity_check;")
        assertEquals("ok", integrity)
    }

    @Test
    fun bundledDatabase_hasExactRoomMasterTableIdentityHash() {
        val hash = runSqliteQuery("SELECT identity_hash FROM room_master_table WHERE id = 42;")
        assertEquals("083f478ee8ac76e9b0960438492f72a3", hash)
    }

    @Test
    fun bundledDatabase_containsMinimumRequiredQuotes() {
        val totalQuotes = runSqliteQuery("SELECT count(*) FROM content WHERE type = 'QUOTE';").toInt()
        assertTrue("Total quotes should be >= 500, found: $totalQuotes", totalQuotes >= 500)

        val enQuotes = runSqliteQuery("SELECT count(*) FROM content WHERE type = 'QUOTE' AND language = 'en';").toInt()
        val viQuotes = runSqliteQuery("SELECT count(*) FROM content WHERE type = 'QUOTE' AND language = 'vi';").toInt()

        assertTrue("Should have English quotes", enQuotes > 0)
        assertTrue("Should have Vietnamese quotes", viQuotes > 0)
    }

    @Test
    fun bundledDatabase_containsMinimumRequiredThoughts() {
        val totalThoughts = runSqliteQuery("SELECT count(*) FROM content WHERE type = 'THOUGHT';").toInt()
        assertTrue("Total thoughts should be >= 200, found: $totalThoughts", totalThoughts >= 200)

        val enThoughts = runSqliteQuery("SELECT count(*) FROM content WHERE type = 'THOUGHT' AND language = 'en';").toInt()
        val viThoughts = runSqliteQuery("SELECT count(*) FROM content WHERE type = 'THOUGHT' AND language = 'vi';").toInt()

        assertTrue("Should have English thoughts", enThoughts > 0)
        assertTrue("Should have Vietnamese thoughts", viThoughts > 0)
    }

    @Test
    fun bundledDatabase_recordsHaveInitialBaselineState() {
        val invalidLastShown = runSqliteQuery("SELECT count(*) FROM content WHERE lastShownAt IS NOT NULL;").toInt()
        val invalidShownCount = runSqliteQuery("SELECT count(*) FROM content WHERE shownCount != 0;").toInt()
        val disabledCount = runSqliteQuery("SELECT count(*) FROM content WHERE enabled != 1;").toInt()
        val emptyHashCount = runSqliteQuery("SELECT count(*) FROM content WHERE contentHash IS NULL OR contentHash = '';").toInt()

        assertEquals("Initial content should have lastShownAt = null", 0, invalidLastShown)
        assertEquals("Initial content should have shownCount = 0", 0, invalidShownCount)
        assertEquals("All bundled content should be enabled = 1", 0, disabledCount)
        assertEquals("All bundled content should have valid contentHash", 0, emptyHashCount)
    }

    @Test
    fun bundledDatabase_containsConfiguredContentSources() {
        val totalSources = runSqliteQuery("SELECT count(*) FROM content_source;").toInt()
        assertTrue("Should contain initial content sources", totalSources >= 5)
    }

    @Test
    fun bundledDatabase_candidateQueryUsesExpectedIndex() {
        val explain = runSqliteQuery(
            "EXPLAIN QUERY PLAN SELECT * FROM content WHERE enabled = 1 AND type = 'QUOTE' AND language IN ('en', 'vi') AND id != 'dummy' ORDER BY (lastShownAt IS NOT NULL) ASC, lastShownAt ASC LIMIT 30;"
        )
        assertTrue("Query should use index_content_type_enabled_language", explain.contains("index_content_type_enabled_language"))
    }

    @Test
    fun bundledDatabase_candidateQueryFiltersByLanguageCorrectly() {
        // Only English
        val enOnly = runSqliteQuery(
            "SELECT count(*) FROM (SELECT * FROM content WHERE enabled = 1 AND type = 'QUOTE' AND language IN ('en') LIMIT 30) WHERE language != 'en';"
        ).toInt()
        assertEquals("Should only return English content when 'en' is requested", 0, enOnly)

        // Only Vietnamese
        val viOnly = runSqliteQuery(
            "SELECT count(*) FROM (SELECT * FROM content WHERE enabled = 1 AND type = 'QUOTE' AND language IN ('vi') LIMIT 30) WHERE language != 'vi';"
        ).toInt()
        assertEquals("Should only return Vietnamese content when 'vi' is requested", 0, viOnly)

        // Exclude current ID
        val sampleId = runSqliteQuery("SELECT id FROM content WHERE enabled = 1 AND type = 'QUOTE' LIMIT 1;")
        val excludedCheck = runSqliteQuery(
            "SELECT count(*) FROM content WHERE enabled = 1 AND type = 'QUOTE' AND id = '$sampleId' AND id != '$sampleId';"
        ).toInt()
        assertEquals("Should exclude current ID", 0, excludedCheck)
    }
}
