package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_items ORDER BY timestamp DESC")
    fun getAllMediaItems(): Flow<List<MediaItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaItem(item: MediaItem): Long

    @Update
    suspend fun updateMediaItem(item: MediaItem)

    @Delete
    suspend fun deleteMediaItem(item: MediaItem)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteMediaItemById(id: Long)

    @Query("SELECT * FROM media_items WHERE status = 'PENDING'")
    suspend fun getPendingMediaItems(): List<MediaItem>
}
