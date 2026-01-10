package com.orignal.buddylynk.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.data.model.Post
import com.orignal.buddylynk.data.repository.BackendRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaUploadWorker - Background worker for uploading media to S3
 * Survives app kill, device restart, and network changes
 */
class MediaUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    
    companion object {
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_MEDIA_URIS = "media_uris"
        const val KEY_CAPTION = "caption"
        const val KEY_PROGRESS = "progress"
        const val KEY_CURRENT_INDEX = "current_index"
        const val KEY_ERROR = "error"
        const val KEY_POST_ID = "post_id"
        
        private const val TAG = "MediaUploadWorker"
    }
    
    override suspend fun doWork(): Result {
        val uploadId = inputData.getString(KEY_UPLOAD_ID) ?: return Result.failure(
            workDataOf(KEY_ERROR to "Missing upload ID")
        )
        
        val uriStrings = inputData.getStringArray(KEY_MEDIA_URIS) ?: return Result.failure(
            workDataOf(KEY_ERROR to "Missing media URIs")
        )
        
        val caption = inputData.getString(KEY_CAPTION) ?: ""
        
        val user = AuthManager.currentUser.value
        if (user == null) {
            Log.e(TAG, "No user logged in")
            return Result.failure(workDataOf(KEY_ERROR to "Not logged in"))
        }
        
        Log.d(TAG, "Starting upload $uploadId with ${uriStrings.size} media items")
        
        val uploadedUrls = mutableListOf<String>()
        var detectedMediaType = "image"
        
        try {
            // Upload each media file
            for ((index, uriString) in uriStrings.withIndex()) {
                val uri = Uri.parse(uriString)
                
                // Update progress
                val progressPercent = index.toFloat() / uriStrings.size
                setProgress(workDataOf(
                    KEY_PROGRESS to progressPercent,
                    KEY_CURRENT_INDEX to index
                ))
                
                Log.d(TAG, "Uploading media ${index + 1}/${uriStrings.size}")
                
                // Upload to S3
                val url = uploadMediaToS3(uri, uploadId, index)
                
                if (url != null) {
                    uploadedUrls.add(url)
                    
                    // Check if video
                    val contentType = applicationContext.contentResolver.getType(uri) ?: ""
                    if (contentType.startsWith("video")) {
                        detectedMediaType = "video"
                    }
                    
                    Log.d(TAG, "Media ${index + 1} uploaded: $url")
                } else {
                    Log.e(TAG, "Failed to upload media ${index + 1}")
                    return Result.retry()
                }
            }
            
            // Update progress to uploading complete
            setProgress(workDataOf(
                KEY_PROGRESS to 0.9f,
                KEY_CURRENT_INDEX to uriStrings.size
            ))
            
            // Create the post
            Log.d(TAG, "Creating post with ${uploadedUrls.size} media items")
            
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
            dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val createdAtISO = dateFormat.format(java.util.Date())
            
            val post = Post(
                postId = "post_${System.currentTimeMillis()}",
                userId = user.userId,
                username = user.username,
                userAvatar = user.avatar,
                content = caption.ifBlank { "📸" },
                mediaUrl = uploadedUrls.firstOrNull(),
                mediaUrls = uploadedUrls,
                mediaType = detectedMediaType,
                createdAt = createdAtISO,
                likesCount = 0,
                commentsCount = 0,
                viewsCount = 0
            )
            
            val success = withContext(Dispatchers.IO) {
                BackendRepository.createPost(post)
            }
            
            if (success) {
                Log.d(TAG, "Post created successfully: ${post.postId}")
                return Result.success(workDataOf(KEY_POST_ID to post.postId))
            } else {
                Log.e(TAG, "Failed to create post")
                return Result.failure(workDataOf(KEY_ERROR to "Failed to create post"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Upload failed", e)
            
            // Check if we should retry
            if (runAttemptCount < 3) {
                return Result.retry()
            }
            
            return Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Unknown error")))
        }
    }
    
    /**
     * Upload a single media file to S3
     */
    private suspend fun uploadMediaToS3(uri: Uri, uploadId: String, index: Int): String? = withContext(Dispatchers.IO) {
        try {
            // Get content type and filename
            val contentType = applicationContext.contentResolver.getType(uri) ?: "image/jpeg"
            val isVideo = contentType.startsWith("video")
            val extension = if (isVideo) "mp4" else "jpg"
            val folder = "posts"
            val filename = "${uploadId}_$index.$extension"
            
            // Get presigned URL
            val presignResult = ApiService.getPresignedUrl(filename, contentType, folder)
            
            presignResult.onSuccess { presignedJson ->
                val uploadUrl = presignedJson.optString("uploadUrl", "")
                val fileUrl = presignedJson.optString("fileUrl", "")
                
                if (uploadUrl.isBlank() || fileUrl.isBlank()) {
                    Log.e(TAG, "Empty presigned URL")
                    return@withContext null
                }
                
                // Upload to S3
                val uploadResult = ApiService.uploadToPresignedUrl(
                    uploadUrl, 
                    uri, 
                    applicationContext.contentResolver, 
                    contentType
                )
                
                uploadResult.onSuccess {
                    return@withContext fileUrl
                }.onFailure { error ->
                    Log.e(TAG, "S3 upload failed: ${error.message}")
                }
            }.onFailure { error ->
                Log.e(TAG, "Presigned URL failed: ${error.message}")
            }
            
            null
        } catch (e: Exception) {
            Log.e(TAG, "Upload error", e)
            null
        }
    }
}
