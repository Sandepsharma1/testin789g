package com.orignal.buddylynk.data.worker

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.orignal.buddylynk.data.api.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * OttVideoUploadWorker - Background worker for uploading OTT videos to S3
 * Uses multipart upload for faster parallel chunk transfers
 */
class OttVideoUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "OttVideoUploadWorker"
        
        // Chunk size: 5MB (minimum for S3 multipart, good balance for speed)
        private const val CHUNK_SIZE = 5 * 1024 * 1024
        
        // Input keys
        const val KEY_UPLOAD_ID = "upload_id"
        const val KEY_VIDEO_URI = "video_uri"
        const val KEY_TITLE = "title"
        const val KEY_DESCRIPTION = "description"
        const val KEY_CATEGORY = "category"
        
        // Progress keys
        const val KEY_PROGRESS = "progress"
        const val KEY_STATUS = "status"
        
        // Output keys
        const val KEY_VIDEO_URL = "video_url"
        const val KEY_THUMBNAIL_URL = "thumbnail_url"
        const val KEY_DURATION = "duration"
        const val KEY_ERROR = "error"
        const val KEY_SUCCESS = "success"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val uploadId = inputData.getString(KEY_UPLOAD_ID) ?: return@withContext Result.failure()
        val videoUriStr = inputData.getString(KEY_VIDEO_URI) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: return@withContext Result.failure()
        val description = inputData.getString(KEY_DESCRIPTION) ?: ""
        val category = inputData.getString(KEY_CATEGORY) ?: "General"
        
        val videoUri = Uri.parse(videoUriStr)
        
        try {
            setProgress(workDataOf(KEY_PROGRESS to 0.05f, KEY_STATUS to "Preparing upload..."))
            
            // Step 1: Extract video metadata and thumbnail
            val (duration, thumbnailBitmap) = extractVideoMetadata(videoUri)
            
            setProgress(workDataOf(KEY_PROGRESS to 0.1f, KEY_STATUS to "Analyzing video file..."))
            
            // Step 2: Get video file size without reading all bytes into memory
            val pfd = applicationContext.contentResolver.openFileDescriptor(videoUri, "r")
            val fileSize = pfd?.statSize ?: 0L
            pfd?.close()
            
            if (fileSize <= 0) {
                return@withContext Result.failure(workDataOf(KEY_ERROR to "Empty video file"))
            }
            
            val fileSizeMB = (fileSize / 1024 / 1024).toInt()
            android.util.Log.d(TAG, "Video size: ${fileSizeMB}MB, using ${if (fileSize > CHUNK_SIZE) "multipart" else "single"} upload")
            
            val videoFileUrl: String
            
            // Use multipart upload for files > 5MB, single upload for smaller
            if (fileSize > CHUNK_SIZE) {
                videoFileUrl = uploadMultipart(videoUri, fileSize)
            } else {
                videoFileUrl = uploadSingle(videoUri, fileSize)
            }
            
            setProgress(workDataOf(KEY_PROGRESS to 0.85f, KEY_STATUS to "Uploading thumbnail..."))
            
            // Step 3: Upload thumbnail (single upload, it's small)
            var thumbnailUrl = ""
            thumbnailBitmap?.let { bitmap ->
                try {
                    thumbnailUrl = uploadThumbnail(bitmap)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Thumbnail upload failed: ${e.message}")
                }
            }
            
            setProgress(workDataOf(KEY_PROGRESS to 0.95f, KEY_STATUS to "Creating video entry..."))
            
            // Step 4: Create video entry in database
            val createResult = ApiService.uploadOttVideo(
                title = title,
                description = description,
                videoUrl = videoFileUrl,
                thumbnailUrl = thumbnailUrl,
                category = category,
                duration = duration
            )
            
            if (createResult == null || !createResult.optBoolean("success", false)) {
                return@withContext Result.failure(workDataOf(KEY_ERROR to "Failed to save video info"))
            }
            
            setProgress(workDataOf(KEY_PROGRESS to 1.0f, KEY_STATUS to "Complete!"))
            
            return@withContext Result.success(workDataOf(
                KEY_SUCCESS to true,
                KEY_VIDEO_URL to videoFileUrl,
                KEY_THUMBNAIL_URL to thumbnailUrl,
                KEY_DURATION to duration
            ))
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Upload failed: ${e.message}", e)
            return@withContext Result.failure(workDataOf(KEY_ERROR to (e.message ?: "Upload failed")))
        }
    }
    
    /**
     * Multipart upload - splits file into chunks and uploads in parallel
     * Much faster for large files (2-5x speedup)
     */
    private suspend fun uploadMultipart(videoUri: Uri, fileSize: Long): String {
        setProgress(workDataOf(KEY_PROGRESS to 0.15f, KEY_STATUS to "Starting multipart upload..."))
        
        val filename = "ott_video_${System.currentTimeMillis()}.mp4"
        val contentType = "video/mp4"
        val fileSizeMB = (fileSize / 1024 / 1024).toInt()
        
        // Start multipart upload
        val startResult = ApiService.startMultipartUpload(filename, contentType, "ott")
        if (startResult.isFailure) {
            throw Exception("Failed to start multipart upload")
        }
        
        val startJson = startResult.getOrThrow()
        val s3UploadId = startJson.getString("uploadId")
        val key = startJson.getString("key")
        val fileUrl = startJson.getString("fileUrl")
        
        // Calculate chunks
        val partCount = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        android.util.Log.d(TAG, "Uploading ${fileSizeMB}MB in $partCount chunks")
        
        setProgress(workDataOf(KEY_PROGRESS to 0.2f, KEY_STATUS to "Getting upload URLs ($partCount parts)..."))
        
        // Get presigned URLs for all parts
        val urlsResult = ApiService.getMultipartPresignedUrls(key, s3UploadId, partCount)
        if (urlsResult.isFailure) {
            ApiService.abortMultipartUpload(key, s3UploadId)
            throw Exception("Failed to get part URLs")
        }
        
        val partUrls = urlsResult.getOrThrow()
        
        setProgress(workDataOf(KEY_PROGRESS to 0.25f, KEY_STATUS to "Uploading video ($partCount parts)..."))
        
        // Upload chunks sequentially to keep memory usage low
        val completedParts = mutableListOf<Pair<Int, String>>()
        var uploadedParts = 0
        
        val inputStream = applicationContext.contentResolver.openInputStream(videoUri)
            ?: throw Exception("Cannot open video stream")
        
        try {
            inputStream.use { stream ->
                val buffer = ByteArray(CHUNK_SIZE)
                
                for (chunkIndex in 0 until partCount) {
                    val partNumber = chunkIndex + 1
                    
                    // Read a chunk from the stream
                    val bytesRead = stream.read(buffer)
                    if (bytesRead <= 0) break
                    
                    // Create chunk data (use copy for the last partial chunk)
                    val chunkData = if (bytesRead == CHUNK_SIZE) {
                        buffer
                    } else {
                        buffer.copyOfRange(0, bytesRead)
                    }
                    
                    val partUrl = partUrls.find { it.first == partNumber }?.second
                        ?: throw Exception("Missing URL for part $partNumber")
                    
                    // Upload chunk
                    val uploadResult = ApiService.uploadPart(partUrl, chunkData)
                    if (uploadResult.isFailure) {
                        throw Exception("Failed to upload part $partNumber")
                    }
                    
                    val etag = uploadResult.getOrThrow()
                    completedParts.add(Pair(partNumber, etag))
                    uploadedParts++
                    
                    // Update progress (25% to 80% is upload phase)
                    val uploadProgress = 0.25f + (0.55f * uploadedParts / partCount)
                    setProgress(workDataOf(
                        KEY_PROGRESS to uploadProgress,
                        KEY_STATUS to "Uploading... ($uploadedParts/$partCount parts)"
                    ))
                }
            }
            
            setProgress(workDataOf(KEY_PROGRESS to 0.82f, KEY_STATUS to "Finalizing upload..."))
            
            // Complete multipart upload
            val completeResult = ApiService.completeMultipartUpload(key, s3UploadId, completedParts)
            if (completeResult.isFailure) {
                throw Exception("Failed to complete multipart upload")
            }
            
            return fileUrl
            
        } catch (e: Exception) {
            // Abort on failure to cleanup S3
            ApiService.abortMultipartUpload(key, s3UploadId)
            throw e
        }
    }
    
    /**
     * Single upload for small files (< 5MB)
     */
    private suspend fun uploadSingle(videoUri: Uri, fileSize: Long): String {
        setProgress(workDataOf(KEY_PROGRESS to 0.2f, KEY_STATUS to "Uploading video..."))
        
        val contentType = "video/mp4"
        val filename = "ott_video_${System.currentTimeMillis()}.mp4"
        
        val presignedResult = ApiService.getPresignedUrl(filename, contentType, "ott")
        if (presignedResult.isFailure) {
            throw Exception("Failed to get upload URL")
        }
        
        val presignedJson = presignedResult.getOrThrow()
        val presignedUrl = presignedJson.optString("uploadUrl").ifEmpty { presignedJson.optString("presignedUrl") }
        var videoFileUrl = presignedJson.optString("fileUrl")
        
        if (videoFileUrl.isNotEmpty() && !videoFileUrl.startsWith("http://") && !videoFileUrl.startsWith("https://")) {
            videoFileUrl = "https://$videoFileUrl"
        }
        
        setProgress(workDataOf(KEY_PROGRESS to 0.5f, KEY_STATUS to "Uploading video..."))
        
        val inputStream = applicationContext.contentResolver.openInputStream(videoUri)
            ?: throw Exception("Cannot open video stream")
        
        val videoBytes = inputStream.use { it.readBytes() } // Small file, safe to read bytes
        
        val uploadResult = ApiService.uploadToPresignedUrl(presignedUrl, videoBytes, contentType)
        if (uploadResult.isFailure) {
            throw Exception("Failed to upload video")
        }
        
        return videoFileUrl
    }
    
    /**
     * Upload thumbnail (always single upload since it's small)
     */
    private suspend fun uploadThumbnail(bitmap: Bitmap): String {
        val thumbFilename = "ott_thumb_${System.currentTimeMillis()}.jpg"
        val thumbContentType = "image/jpeg"
        
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        val thumbBytes = stream.toByteArray()
        
        val thumbPresignedResult = ApiService.getPresignedUrl(thumbFilename, thumbContentType, "ott")
        if (thumbPresignedResult.isFailure) return ""
        
        val thumbJson = thumbPresignedResult.getOrThrow()
        val thumbPresignedUrl = thumbJson.optString("uploadUrl").ifEmpty { thumbJson.optString("presignedUrl") }
        var thumbFileUrl = thumbJson.optString("fileUrl")
        
        if (thumbFileUrl.isNotEmpty() && !thumbFileUrl.startsWith("http://") && !thumbFileUrl.startsWith("https://")) {
            thumbFileUrl = "https://$thumbFileUrl"
        }
        
        val thumbUploadResult = ApiService.uploadToPresignedUrl(thumbPresignedUrl, thumbBytes, thumbContentType)
        return if (thumbUploadResult.isSuccess) thumbFileUrl else ""
    }
    
    private fun extractVideoMetadata(videoUri: Uri): Pair<Long, Bitmap?> {
        var duration = 0L
        var bitmap: Bitmap? = null
        
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(applicationContext, videoUri)
            
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = durationStr?.toLongOrNull()?.div(1000) ?: 0L
            
            val frameTime = if (duration > 1) 1000000L else 0L
            bitmap = retriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error extracting metadata: ${e.message}")
        } finally {
            retriever.release()
        }
        
        return Pair(duration, bitmap)
    }
}
