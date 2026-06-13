package com.example.data

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class EmailSender {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun sendEmailWithAttachment(
        file: File,
        fileName: String,
        mimeType: String,
        fileSizeString: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.SENDGRID_API_KEY
        val senderEmailRaw = BuildConfig.SENDGRID_SENDER_EMAIL
        val senderNameRaw = BuildConfig.SENDGRID_SENDER_NAME

        val senderEmail = if (senderEmailRaw.isNullOrBlank() || senderEmailRaw.contains("SENDER_EMAIL")) {
            "noreply@aistudio-media-sender.com"
        } else {
            senderEmailRaw
        }

        val senderName = if (senderNameRaw.isNullOrBlank() || senderNameRaw.contains("SENDER_NAME")) {
            "Media Auto-Sender"
        } else {
            senderNameRaw
        }

        val destinationEmail = "rudrakshsinghlion@gmail.com"

        // Basic verification of API key
        if (apiKey.isNullOrBlank() || 
            apiKey == "YOUR_SENDGRID_API_KEY_HERE" || 
            apiKey == "MY_GEMINI_API_KEY" || 
            apiKey.contains("SENDGRID_API_KEY")
        ) {
            val errMsg = "API Key not configured. Please add SENDGRID_API_KEY to secrets panel in AI Studio."
            Log.e("EmailSender", errMsg)
            return@withContext Result.failure(Exception(errMsg))
        }

        try {
            if (!file.exists()) {
                return@withContext Result.failure(Exception("Media file does not exist locally: ${file.absolutePath}"))
            }

            val fileBytes = file.readBytes()
            if (fileBytes.isEmpty()) {
                return@withContext Result.failure(Exception("File is empty, cannot send."))
            }

            val base64Content = Base64.encodeToString(fileBytes, Base64.NO_WRAP)

            // Construct manual JSON payload for SendGrid
            // We string-escape variables safe from JSON breakages
            val escapedFileName = escapeJson(fileName)
            val escapedMimeType = escapeJson(mimeType)
            val escapedSenderEmail = escapeJson(senderEmail)
            val escapedSenderName = escapeJson(senderName)
            val escapedDestEmail = escapeJson(destinationEmail)
            val fileTypeWord = if (mimeType.startsWith("video")) "Video" else "Photo"

            val jsonPayload = """
                {
                  "personalizations": [
                    {
                      "to": [
                        {
                          "email": "$escapedDestEmail"
                        }
                      ],
                      "subject": "📸 New $fileTypeWord Uploaded: $escapedFileName"
                    }
                  ],
                  "from": {
                    "email": "$escapedSenderEmail",
                    "name": "$escapedSenderName"
                  },
                  "content": [
                    {
                      "type": "text/html",
                      "value": "<h3>New Media File Added!</h3><p>A new $fileTypeWord was captured/added and transferred automatically to your mailbox.</p><ul><li><b>Filename:</b> $escapedFileName</li><li><b>Size:</b> $fileSizeString</li><li><b>Mime-Type:</b> $escapedMimeType</li><li><b>Added at:</b> ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date())}</li></ul><p><i>Sent automatically from your Jetpack Compose Media Auto-Sender App.</i></p>"
                    }
                  ],
                  "attachments": [
                    {
                      "content": "$base64Content",
                      "filename": "$escapedFileName",
                      "type": "$escapedMimeType",
                      "disposition": "attachment"
                    }
                  ]
                }
            """.trimIndent()

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val requestBody = jsonPayload.toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.sendgrid.com/v3/mail/send")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Sent successfully to $destinationEmail via SendGrid.")
                } else {
                    val responseBody = response.body?.string() ?: ""
                    Log.e("EmailSender", "SendGrid failed with code ${response.code}: $responseBody")
                    
                    val formattedError = when {
                        response.code == 401 || responseBody.contains("unauthorized", ignoreCase = true) -> {
                            "Invalid SendGrid API Key. Please verify your SENDGRID_API_KEY secret."
                        }
                        responseBody.contains("Sender Identity", ignoreCase = true) || responseBody.contains("from address", ignoreCase = true) || response.code == 400 || response.code == 403 -> {
                            "SendGrid Error: Unverified Sender. Go to sendgrid.com and verify your sender email structure, or match SENDGRID_SENDER_EMAIL to your verified single sender."
                        }
                        else -> {
                            "SendGrid Error (Code ${response.code}): $responseBody"
                        }
                    }
                    Result.failure(Exception(formattedError))
                }
            }
        } catch (e: Exception) {
            Log.e("EmailSender", "Failed to dispatch email", e)
            Result.failure(e)
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
