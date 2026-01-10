package com.orignal.buddylynk.data.upload

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.orignal.buddylynk.data.model.UploadState
import com.orignal.buddylynk.data.model.UploadStatus
import com.orignal.buddylynk.data.worker.MediaUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * UploadManager - Manages background media uploads
 * Singleton that tracks all active uploads
 */
object UploadManager {
    
    private val _activeUploads = MutableStateFlow<Map<String, UploadState>>(emptyMap())
    val activeUploads: StateFlow<Map<String, UploadState>> = _activeUploads.asStateFlow()
    
    // Store pending uploads for retry
    private val pendingData = ConcurrentHashMap<String, PendingUploadData>()
    
    data class PendingUploadData(
        val mediaUris: List<Uri>,
        val caption: String
    )
    
    /**
     * Start a background upload
     */
    fun startUpload(
        context: Context,
        mediaUris: List<Uri>,
        caption: String
    ): String {
        val uploadId = UUID.randomUUID().toString()
        
        // Store pending data for retry
        pendingData[uploadId] = PendingUploadData(mediaUris, caption)
        
        // Create initial upload state
        val uploadState = UploadState(
            uploadId = uploadId,
            mediaUris = mediaUris,
            caption = caption,
            progress = 0f,
            currentMediaIndex = 0,
            totalMedia = mediaUris.size,
            status = UploadStatus.PENDING
        )
        
        // Add to active uploads
        _activeUploads.value = _activeUploads.value + (uploadId to uploadState)
        
        // Convert URIs to strings for WorkManager
        val uriStrings = mediaUris.map { it.toString() }.toTypedArray()
        
        // Create work request with input data
        val inputData = workDataOf(
            MediaUploadWorker.KEY_UPLOAD_ID to uploadId,
            MediaUploadWorker.KEY_MEDIA_URIS to uriStrings,
            MediaUploadWorker.KEY_CAPTION to caption
        )
        
        // Configure work request with constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<MediaUploadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(uploadId)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        
        // Enqueue work
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uploadId,
                ExistingWorkPolicy.KEEP,
                uploadWork
            )
        
        // Observe work progress
        observeWorkProgress(context, uploadId)
        
        return uploadId
    }
    
    /**
     * Observe WorkManager progress updates
     */
    private fun observeWorkProgress(context: Context, uploadId: String) {
        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(uploadId)
            .observeForever { workInfoList ->
                workInfoList?.firstOrNull()?.let { workInfo ->
                    val currentUpload = _activeUploads.value[uploadId] ?: return@let
                    
                    val updatedUpload = when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat(MediaUploadWorker.KEY_PROGRESS, 0f)
                            val currentIndex = workInfo.progress.getInt(MediaUploadWorker.KEY_CURRENT_INDEX, 0)
                            currentUpload.copy(
                                status = UploadStatus.UPLOADING,
                                progress = progress,
                                currentMediaIndex = currentIndex
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            pendingData.remove(uploadId)
                            currentUpload.copy(
                                status = UploadStatus.COMPLETED,
                                progress = 1f
                            )
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(MediaUploadWorker.KEY_ERROR)
                            currentUpload.copy(
                                status = UploadStatus.FAILED,
                                errorMessage = error ?: "Upload failed"
                            )
                        }
                        WorkInfo.State.CANCELLED -> {
                            pendingData.remove(uploadId)
                            currentUpload.copy(status = UploadStatus.CANCELLED)
                        }
                        else -> currentUpload
                    }
                    
                    _activeUploads.value = _activeUploads.value + (uploadId to updatedUpload)
                }
            }
    }
    
    /**
     * Retry a failed upload
     */
    fun retryUpload(context: Context, uploadId: String): Boolean {
        val pending = pendingData[uploadId] ?: return false
        val currentUpload = _activeUploads.value[uploadId] ?: return false
        
        if (currentUpload.status != UploadStatus.FAILED) return false
        
        // Cancel old work
        WorkManager.getInstance(context).cancelUniqueWork(uploadId)
        
        // Restart with same ID
        val uriStrings = pending.mediaUris.map { it.toString() }.toTypedArray()
        
        val inputData = workDataOf(
            MediaUploadWorker.KEY_UPLOAD_ID to uploadId,
            MediaUploadWorker.KEY_MEDIA_URIS to uriStrings,
            MediaUploadWorker.KEY_CAPTION to pending.caption
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<MediaUploadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(uploadId)
            .build()
        
        // Update state
        _activeUploads.value = _activeUploads.value + (uploadId to currentUpload.copy(
            status = UploadStatus.PENDING,
            progress = 0f,
            errorMessage = null
        ))
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(uploadId, ExistingWorkPolicy.REPLACE, uploadWork)
        
        observeWorkProgress(context, uploadId)
        
        return true
    }
    
    /**
     * Cancel an upload
     */
    fun cancelUpload(context: Context, uploadId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uploadId)
        pendingData.remove(uploadId)
        _activeUploads.value = _activeUploads.value - uploadId
    }
    
    /**
     * Dismiss a completed/failed upload from UI
     */
    fun dismissUpload(uploadId: String) {
        val upload = _activeUploads.value[uploadId] ?: return
        if (upload.status == UploadStatus.COMPLETED || 
            upload.status == UploadStatus.FAILED ||
            upload.status == UploadStatus.CANCELLED) {
            pendingData.remove(uploadId)
            _activeUploads.value = _activeUploads.value - uploadId
        }
    }
    
    /**
     * Check if there are any active uploads
     */
    fun hasActiveUploads(): Boolean {
        return _activeUploads.value.any { (_, state) ->
            state.status == UploadStatus.PENDING || state.status == UploadStatus.UPLOADING
        }
    }
    
    /**
     * Get uploads that need UI updates (not completed)
     */
    fun getVisibleUploads(): List<UploadState> {
        return _activeUploads.value.values
            .filter { it.status != UploadStatus.CANCELLED }
            .sortedByDescending { it.createdAt }
    }
    
    // ========== OTT VIDEO UPLOADS ==========
    
    private val _ottUploads = MutableStateFlow<Map<String, OttUploadState>>(emptyMap())
    val ottUploads: StateFlow<Map<String, OttUploadState>> = _ottUploads.asStateFlow()
    
    data class OttUploadState(
        val uploadId: String,
        val title: String,
        val progress: Float = 0f,
        val status: String = "pending",
        val statusMessage: String = "Preparing upload...",
        val createdAt: Long = System.currentTimeMillis()
    )
    
    /**
     * Start OTT video background upload
     */
    fun startOttUpload(
        context: Context,
        videoUri: Uri,
        title: String,
        description: String,
        category: String
    ): String {
        val uploadId = java.util.UUID.randomUUID().toString()
        
        // Create initial upload state
        val uploadState = OttUploadState(
            uploadId = uploadId,
            title = title,
            progress = 0f,
            status = "pending",
            statusMessage = "Preparing upload..."
        )
        
        _ottUploads.value = _ottUploads.value + (uploadId to uploadState)
        
        // Create work request
        val inputData = workDataOf(
            com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_UPLOAD_ID to uploadId,
            com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_VIDEO_URI to videoUri.toString(),
            com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_TITLE to title,
            com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_DESCRIPTION to description,
            com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_CATEGORY to category
        )
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val uploadWork = OneTimeWorkRequestBuilder<com.orignal.buddylynk.data.worker.OttVideoUploadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .addTag(uploadId)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uploadId,
                ExistingWorkPolicy.KEEP,
                uploadWork
            )
        
        observeOttUploadProgress(context, uploadId)
        
        return uploadId
    }
    
    private fun observeOttUploadProgress(context: Context, uploadId: String) {
        WorkManager.getInstance(context)
            .getWorkInfosByTagLiveData(uploadId)
            .observeForever { workInfoList ->
                workInfoList?.firstOrNull()?.let { workInfo ->
                    val currentUpload = _ottUploads.value[uploadId] ?: return@let
                    
                    val updatedUpload = when (workInfo.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getFloat(
                                com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_PROGRESS, 0f
                            )
                            val status = workInfo.progress.getString(
                                com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_STATUS
                            ) ?: "Uploading..."
                            currentUpload.copy(
                                status = "uploading",
                                progress = progress,
                                statusMessage = status
                            )
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            currentUpload.copy(
                                status = "completed",
                                progress = 1f,
                                statusMessage = "Upload complete!"
                            )
                        }
                        WorkInfo.State.FAILED -> {
                            val error = workInfo.outputData.getString(
                                com.orignal.buddylynk.data.worker.OttVideoUploadWorker.KEY_ERROR
                            )
                            currentUpload.copy(
                                status = "failed",
                                statusMessage = error ?: "Upload failed"
                            )
                        }
                        WorkInfo.State.CANCELLED -> {
                            currentUpload.copy(status = "cancelled", statusMessage = "Cancelled")
                        }
                        else -> currentUpload
                    }
                    
                    _ottUploads.value = _ottUploads.value + (uploadId to updatedUpload)
                }
            }
    }
    
    fun cancelOttUpload(context: Context, uploadId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uploadId)
        _ottUploads.value = _ottUploads.value - uploadId
    }
    
    fun dismissOttUpload(uploadId: String) {
        val upload = _ottUploads.value[uploadId] ?: return
        if (upload.status == "completed" || upload.status == "failed" || upload.status == "cancelled") {
            _ottUploads.value = _ottUploads.value - uploadId
        }
    }
    
    fun hasActiveOttUploads(): Boolean {
        return _ottUploads.value.any { (_, state) ->
            state.status == "pending" || state.status == "uploading"
        }
    }
}

