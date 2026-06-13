package com.example.data

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File

class MediaRepository(
    private val mediaDao: MediaDao,
    private val emailSender: EmailSender
) {
    val allMediaItems: Flow<List<MediaItem>> = mediaDao.getAllMediaItems()

    // Inserts the item, then triggers the async upload
    suspend fun queueMediaItem(
        filePath: String,
        fileName: String,
        mimeType: String,
        fileSize: String,
        externalScope: CoroutineScope
    ) {
        val newItem = MediaItem(
            filePath = filePath,
            fileName = fileName,
            fileSize = fileSize,
            mimeType = mimeType,
            status = MediaItem.STATUS_PENDING
        )
        val generatedId = mediaDao.insertMediaItem(newItem)
        val savedItem = newItem.copy(id = generatedId)

        // Dispatch email sending in the background
        externalScope.launch(Dispatchers.IO) {
            sendMediaItemInternal(savedItem)
        }
    }

    // Allows manually retrying a previously failed item
    fun retryMediaItem(item: MediaItem, externalScope: CoroutineScope) {
        externalScope.launch(Dispatchers.IO) {
            sendMediaItemInternal(item)
        }
    }

    suspend fun deleteMediaItem(item: MediaItem) {
        // Delete the physical cache file
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("MediaRepository", "Failed to delete file", e)
        }
        mediaDao.deleteMediaItemById(item.id)
    }

    private suspend fun sendMediaItemInternal(item: MediaItem) {
        // 1. Update status to SENDING
        val sendingItem = item.copy(status = MediaItem.STATUS_SENDING, errorReason = null)
        mediaDao.updateMediaItem(sendingItem)

        // 2. Perform the send operation
        val file = File(item.filePath)
        val result = emailSender.sendEmailWithAttachment(
            file = file,
            fileName = item.fileName,
            mimeType = item.mimeType,
            fileSizeString = item.fileSize
        )

        // 3. Handle result
        result.fold(
            onSuccess = { successMsg ->
                Log.d("MediaRepository", "Uploaded successfully: $successMsg")
                val sentItem = sendingItem.copy(status = MediaItem.STATUS_SENT, errorReason = null)
                mediaDao.updateMediaItem(sentItem)
            },
            onFailure = { throwable ->
                val errorMsg = throwable.message ?: "Unknown error"
                Log.e("MediaRepository", "Failed to upload: $errorMsg")
                val failedItem = sendingItem.copy(
                    status = MediaItem.STATUS_FAILED,
                    errorReason = errorMsg
                )
                mediaDao.updateMediaItem(failedItem)
            }
        )
    }
}
