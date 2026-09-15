package com.iatb.materialthemes.data.content.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iatb.materialthemes.data.content.model.WidgetStateEntity

@Dao
interface WidgetStateDao {

    @Query("SELECT * FROM widget_state WHERE widgetId = :widgetId LIMIT 1")
    suspend fun getState(widgetId: Int): WidgetStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: WidgetStateEntity)

    @Query("DELETE FROM widget_state WHERE widgetId = :widgetId")
    suspend fun deleteState(widgetId: Int): Int

    @Query("SELECT * FROM widget_state")
    suspend fun getAllActiveStates(): List<WidgetStateEntity>
}
