package com.iatb.materialthemes.data.content.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentType

@Dao
interface ContentDao {

    @Query("SELECT * FROM content WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ContentEntity?

    @Query(
        """
        SELECT * FROM content
        WHERE enabled = 1
          AND type = :type
          AND language IN (:languages)
          AND (:excludeId IS NULL OR id != :excludeId)
        ORDER BY (lastShownAt IS NOT NULL) ASC, lastShownAt ASC
        LIMIT :limit
        """
    )
    suspend fun getCandidates(
        type: ContentType,
        languages: List<String>,
        excludeId: String?,
        limit: Int
    ): List<ContentEntity>

    @Query("UPDATE content SET lastShownAt = :timestamp, shownCount = shownCount + 1 WHERE id = :id")
    suspend fun updateShown(id: String, timestamp: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ContentEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(items: List<ContentEntity>): List<Long>

    @Query("SELECT COUNT(*) FROM content WHERE type = :type AND language = :language AND enabled = 1")
    suspend fun countByTypeAndLanguage(type: ContentType, language: String): Int

    @Query("SELECT COUNT(*) FROM content")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM content WHERE type = :type AND language = :language AND enabled = 1 LIMIT 1")
    suspend fun getRandomFallback(type: ContentType, language: String): ContentEntity?
}
