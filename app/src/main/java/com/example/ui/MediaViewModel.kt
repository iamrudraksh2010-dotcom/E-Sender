package com.example.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MediaItem
import com.example.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat

class MediaViewModel(private val repository: MediaRepository) : ViewModel() {

    // Expose media list reactively from DB
    val mediaItems: StateFlow<List<MediaItem>> = repository.allMediaItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Handles files picked from the standard photo/video gallery picker
    fun addPickedMedia(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resolver = context.contentResolver
                val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                
                // Extract original filename
                var fileName = "media_${System.currentTimeMillis()}"
                resolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                fileName = name
                            }
                        }
                    }
                }

                // Add extension if missing
                if (!fileName.contains(".") && mimeType.contains("/")) {
                    val ext = mimeType.substringAfter("/")
                    fileName = "$fileName.$ext"
                }

                // Copy stream to private internal cache
                val mediaDir = File(context.filesDir, "uploads").apply { mkdirs() }
                val targetFile = File(mediaDir, fileName)
                
                resolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val sizeBytes = targetFile.length()
                val sizeStr = formatFileSize(sizeBytes)

                // Enqueue and send automatically
                repository.queueMediaItem(
                    filePath = targetFile.absolutePath,
                    fileName = fileName,
                    mimeType = mimeType,
                    fileSize = sizeStr,
                    externalScope = viewModelScope
                )
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Failed to process picked media", e)
            }
        }
    }

    // Creates an empty file in cached directory and registers its content:// URI (FileProvider)
    fun createTempFileUri(context: Context, isVideo: Boolean): Uri {
        val extension = if (isVideo) ".mp4" else ".jpg"
        val prefix = if (isVideo) "VID_" else "IMG_"
        val tempDir = File(context.cacheDir, "camera_temp").apply { mkdirs() }
        val tempFile = File.createTempFile(
            "${prefix}${System.currentTimeMillis()}_",
            extension,
            tempDir
        )
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, tempFile)
    }

    // Processes a captured file saved on a FileProvider URI
    fun confirmCameraCapture(context: Context, uri: Uri, isVideo: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Find file corresponding to FileProvider URI in private files
                // Standard authorities format translates to File provider paths
                val authority = "${context.packageName}.fileprovider"
                
                // Copy the file from FileProvider inputstream to the safe internal files folder
                val contextResolver = context.contentResolver
                val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
                val prefix = if (isVideo) "capture_vid" else "capture_img"
                val extension = if (isVideo) ".mp4" else ".jpg"
                val finalFileName = "${prefix}_${System.currentTimeMillis()}$extension"

                val mediaDir = File(context.filesDir, "uploads").apply { mkdirs() }
                val targetFile = File(mediaDir, finalFileName)

                contextResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }

                val sizeBytes = targetFile.length()
                // Clean temporary file to keep device storage clean
                try {
                    contextResolver.delete(uri, null, null)
                } catch (e: Exception) {
                    // Fail silently, cache is cleared periodically by Android
                }

                if (sizeBytes <= 0) {
                    Log.e("MediaViewModel", "Captured file size is 0 bytes - skipping.")
                    return@launch
                }

                val sizeStr = formatFileSize(sizeBytes)

                repository.queueMediaItem(
                    filePath = targetFile.absolutePath,
                    fileName = finalFileName,
                    mimeType = mimeType,
                    fileSize = sizeStr,
                    externalScope = viewModelScope
                )
            } catch (e: Exception) {
                Log.e("MediaViewModel", "Failed to preserve camera captured file", e)
            }
        }
    }

    fun retryItem(item: MediaItem) {
        repository.retryMediaItem(item, viewModelScope)
    }

    fun deleteItem(item: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMediaItem(item)
        }
    }

    private fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 Bytes"
        val units = arrayOf("Bytes", "KB", "MB", "GB")
        val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.toDouble())).toInt()
        val formattedValue = DecimalFormat("#,##0.00").format(
            sizeBytes / Math.pow(1024.toDouble(), digitGroups.toDouble())
        )
        return "$formattedValue ${units[digitGroups]}"
    }
}

// Custom ViewModel Factory
class MediaViewModelFactory(private val repository: MediaRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
