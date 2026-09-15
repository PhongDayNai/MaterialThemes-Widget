package com.iatb.materialthemes.data.content.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iatb.materialthemes.data.content.model.ContentSourceEntity
import com.iatb.materialthemes.data.content.model.ContentType

@Dao
interface ContentSourceDao {

    @Query("SELECT * FROM content_source")
    suspend fun getAllSources(): List<ContentSourceEntity>

    @Query("SELECT * FROM content_source WHERE enabled = 1 AND type = :type")
    suspend fun getEnabledSources(type: ContentType): List<ContentSourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(sources: List<ContentSourceEntity>)

    @Query(
        """
        UPDATE content_source
        SET lastSyncAt = :syncTime, nextSyncAt = :nextSync, syncError = :error
        WHERE id = :sourceId
        """
    )
    suspend fun updateSyncResult(
        sourceId: String,
        syncTime: Long,
        nextSync: Long?,
        error: String?
    ): Int
}
