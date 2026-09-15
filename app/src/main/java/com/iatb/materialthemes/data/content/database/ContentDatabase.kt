package com.iatb.materialthemes.data.content.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.iatb.materialthemes.data.content.model.ContentEntity
import com.iatb.materialthemes.data.content.model.ContentSourceEntity
import com.iatb.materialthemes.data.content.model.WidgetStateEntity

@Database(
    entities = [
        ContentEntity::class,
        ContentSourceEntity::class,
        WidgetStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(ContentConverters::class)
abstract class ContentDatabase : RoomDatabase() {

    abstract fun contentDao(): ContentDao
    abstract fun widgetStateDao(): WidgetStateDao
    abstract fun contentSourceDao(): ContentSourceDao

    companion object {
        private const val DATABASE_NAME = "content.db"

        @Volatile
        private var instance: ContentDatabase? = null

        fun getInstance(context: Context): ContentDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): ContentDatabase {
            return Room.databaseBuilder(
                context,
                ContentDatabase::class.java,
                DATABASE_NAME
            )
                .createFromAsset(DATABASE_NAME)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
