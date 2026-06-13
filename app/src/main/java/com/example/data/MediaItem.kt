package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_items")
data class MediaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val filePath: String,
    val fileName: String,
    val fileSize: String,
    val mimeType: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = STATUS_PENDING,
    val errorReason: String? = null
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_SENDING = "SENDING"
        const val STATUS_SENT = "SENT"
        const val STATUS_FAILED = "FAILED"
    }
}
