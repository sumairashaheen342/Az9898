package com.example.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "vision_items")
data class VisionItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,               // "IMAGE", "VIDEO", "ANALYSIS"
    val prompt: String,             // user's text prompt or task description
    val responseText: String?,      // text output, descriptions, analysis
    val mediaData: String?,         // base64 encoded jpeg image data or video url/uri
    val mimeType: String?,          // "image/jpeg" or "video/mp4"
    val modelName: String,          // e.g., gemini-3.1-flash-image-preview
    val imageSize: String?,         // "1K", "2K", "4K"
    val aspectRatio: String?,       // "1:1", "16:9", etc.
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface VisionDao {
    @Query("SELECT * FROM vision_items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<VisionItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VisionItem)

    @Query("DELETE FROM vision_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("DELETE FROM vision_items")
    suspend fun clearAll()
}

@Database(entities = [VisionItem::class], version = 1, exportSchema = false)
abstract class VisionDatabase : RoomDatabase() {
    abstract fun visionDao(): VisionDao

    companion object {
        @Volatile
        private var INSTANCE: VisionDatabase? = null

        fun getDatabase(context: Context): VisionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VisionDatabase::class.java,
                    "vision_app_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
