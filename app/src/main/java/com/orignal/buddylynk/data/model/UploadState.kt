package com.orignal.buddylynk.data.model

import android.net.Uri

/**
 * Upload State - Tracks background media upload progress
 */
data class UploadState(
    val uploadId: String,
    val mediaUris: List<Uri>,
    val caption: String,
    val progress: Float = 0f,           // 0.0 to 1.0
    val currentMediaIndex: Int = 0,     // Which media is being uploaded
    val totalMedia: Int = 0,
    val status: UploadStatus = UploadStatus.PENDING,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val uploadedUrls: List<String> = emptyList()
)

/**
 * Upload Status enum
 */
enum class UploadStatus {
    PENDING,        // Waiting to start
    UPLOADING,      // Currently uploading
    PROCESSING,     // Creating post after upload
    COMPLETED,      // Successfully posted
    FAILED,         // Upload failed
    CANCELLED       // User cancelled
}
