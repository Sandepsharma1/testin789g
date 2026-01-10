package com.orignal.buddylynk.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.data.repository.BackendRepository
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.data.model.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ImageFilter(
    val name: String,
    val saturation: Float = 1f,
    val contrast: Float = 1f,
    val brightness: Float = 1f,
    val grayscale: Boolean = false,
    val sepia: Float = 0f
)

data class ImageAdjustments(
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
)

enum class UploadMode { FILES, CAMERA, EVENT }
enum class EditTab { FILTER, EDIT }
enum class PostState { IDLE, UPLOADING, SUCCESS, ERROR }

// Media item with upload state tracking
enum class MediaUploadState { PENDING, UPLOADING, SUCCESS, FAILED }

data class MediaItem(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val mediaType: String = "image", // "image" or "video"
    val uploadState: MediaUploadState = MediaUploadState.PENDING,
    val uploadProgress: Float = 0f,
    val uploadedUrl: String? = null,
    val errorMessage: String? = null
)

class CreatePostViewModel : ViewModel() {

    companion object {
        private const val TAG = "CreatePostViewModel"
        const val MAX_MEDIA_ITEMS = 100 // Effectively unlimited
    }

    // Filters
    val filters = listOf(
        ImageFilter("Normal"),
        ImageFilter("Vivid", saturation = 1.5f, contrast = 1.1f),
        ImageFilter("Noir", grayscale = true, contrast = 1.25f, brightness = 0.9f),
        ImageFilter("Vintage", sepia = 0.5f, contrast = 0.9f, brightness = 1.1f),
        ImageFilter("Glacial", saturation = 0.8f, brightness = 1.05f),
        ImageFilter("Golden", sepia = 0.3f, saturation = 1.2f),
        ImageFilter("Drama", contrast = 1.25f, saturation = 1.1f, brightness = 0.9f)
    )

    // State - Multi-media support
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    // Legacy single image support (for backward compatibility with UI)
    private val _selectedImageUri = MutableStateFlow<Uri?>(null)
    val selectedImageUri: StateFlow<Uri?> = _selectedImageUri.asStateFlow()

    private val _caption = MutableStateFlow("")
    val caption: StateFlow<String> = _caption.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    private val _selectedFilter = MutableStateFlow(filters[0])
    val selectedFilter: StateFlow<ImageFilter> = _selectedFilter.asStateFlow()

    private val _adjustments = MutableStateFlow(ImageAdjustments())
    val adjustments: StateFlow<ImageAdjustments> = _adjustments.asStateFlow()

    private val _uploadMode = MutableStateFlow(UploadMode.FILES)
    val uploadMode: StateFlow<UploadMode> = _uploadMode.asStateFlow()

    private val _editTab = MutableStateFlow(EditTab.FILTER)
    val editTab: StateFlow<EditTab> = _editTab.asStateFlow()

    private val _postState = MutableStateFlow(PostState.IDLE)
    val postState: StateFlow<PostState> = _postState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Upload progress (0-100)
    private val _overallProgress = MutableStateFlow(0f)
    val overallProgress: StateFlow<Float> = _overallProgress.asStateFlow()

    private val _currentUploadingIndex = MutableStateFlow(-1)
    val currentUploadingIndex: StateFlow<Int> = _currentUploadingIndex.asStateFlow()

    // ========== MULTI-MEDIA ACTIONS ==========

    /**
     * Add multiple media items (up to MAX_MEDIA_ITEMS)
     */
    fun addMediaItems(uris: List<Uri>, context: Context? = null) {
        val current = _mediaItems.value.toMutableList()
        val remaining = MAX_MEDIA_ITEMS - current.size
        
        if (remaining <= 0) {
            Log.w(TAG, "Maximum $MAX_MEDIA_ITEMS media items reached")
            return
        }
        
        val toAdd = uris.take(remaining).map { uri ->
            // Use ContentResolver for reliable MIME type detection
            val mimeType = context?.contentResolver?.getType(uri)
            val type = when {
                mimeType?.startsWith("video") == true -> "video"
                mimeType?.startsWith("image") == true -> "image"
                uri.toString().contains("video", ignoreCase = true) -> "video"
                uri.path?.contains("video", ignoreCase = true) == true -> "video"
                else -> "image"
            }
            Log.d(TAG, "Adding media: $uri, mimeType=$mimeType, type=$type")
            MediaItem(uri = uri, mediaType = type)
        }
        
        current.addAll(toAdd)
        _mediaItems.value = current
        
        // Update legacy single image for backward compatibility
        if (current.isNotEmpty()) {
            _selectedImageUri.value = current.first().uri
        }
        
        Log.d(TAG, "Added ${toAdd.size} media items. Total: ${current.size}")
    }

    /**
     * Remove media item by index
     */
    fun removeMediaItem(index: Int) {
        val current = _mediaItems.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _mediaItems.value = current
            
            // Update legacy single image
            _selectedImageUri.value = current.firstOrNull()?.uri
            
            Log.d(TAG, "Removed media at index $index. Remaining: ${current.size}")
        }
    }

    /**
     * Reorder media items (move from index to index)
     */
    fun reorderMedia(fromIndex: Int, toIndex: Int) {
        val current = _mediaItems.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _mediaItems.value = current
            
            // Update legacy single image
            _selectedImageUri.value = current.firstOrNull()?.uri
            
            Log.d(TAG, "Reordered media from $fromIndex to $toIndex")
        }
    }

    /**
     * Move media item up in the list
     */
    fun moveMediaUp(index: Int) {
        if (index > 0) {
            reorderMedia(index, index - 1)
        }
    }

    /**
     * Move media item down in the list
     */
    fun moveMediaDown(index: Int) {
        if (index < _mediaItems.value.size - 1) {
            reorderMedia(index, index + 1)
        }
    }

    /**
     * Retry failed upload for a specific item
     */
    fun retryUpload(index: Int, context: Context) {
        val current = _mediaItems.value.toMutableList()
        if (index in current.indices && current[index].uploadState == MediaUploadState.FAILED) {
            current[index] = current[index].copy(
                uploadState = MediaUploadState.PENDING,
                errorMessage = null
            )
            _mediaItems.value = current
            
            // Retry the entire post creation
            createPost(context)
        }
    }

    // ========== LEGACY SINGLE IMAGE ACTIONS (for backward compatibility) ==========

    fun setImageUri(uri: Uri?) {
        if (uri != null) {
            addMediaItems(listOf(uri))
        } else {
            _mediaItems.value = emptyList()
            _selectedImageUri.value = null
        }
    }

    fun setCaption(text: String) {
        _caption.value = text
    }

    fun setLocation(loc: String) {
        _location.value = loc
    }

    fun setFilter(filter: ImageFilter) {
        _selectedFilter.value = filter
    }

    fun setAdjustments(adj: ImageAdjustments) {
        _adjustments.value = adj
    }

    fun setBrightness(value: Float) {
        _adjustments.value = _adjustments.value.copy(brightness = value)
    }

    fun setContrast(value: Float) {
        _adjustments.value = _adjustments.value.copy(contrast = value)
    }

    fun setSaturation(value: Float) {
        _adjustments.value = _adjustments.value.copy(saturation = value)
    }

    fun setUploadMode(mode: UploadMode) {
        _uploadMode.value = mode
    }

    fun setEditTab(tab: EditTab) {
        _editTab.value = tab
    }

    fun clearSelection() {
        _mediaItems.value = emptyList()
        _selectedImageUri.value = null
        _caption.value = ""
        _location.value = ""
        _selectedFilter.value = filters[0]
        _adjustments.value = ImageAdjustments()
        _postState.value = PostState.IDLE
        _errorMessage.value = null
        _overallProgress.value = 0f
        _currentUploadingIndex.value = -1
    }

    // ========== CREATE POST WITH MULTI-MEDIA ==========

    /**
     * Upload all media and create post
     */
    fun createPost(context: Context) {
        val items = _mediaItems.value
        if (items.isEmpty()) {
            Log.e(TAG, "No media selected")
            _errorMessage.value = "Please select at least one image or video"
            return
        }
        
        val user = AuthManager.currentUser.value
        if (user == null) {
            Log.e(TAG, "No user logged in")
            _errorMessage.value = "Please log in first"
            return
        }

        viewModelScope.launch {
            _postState.value = PostState.UPLOADING
            _errorMessage.value = null
            _overallProgress.value = 0f

            try {
                val postId = "post_${System.currentTimeMillis()}"
                val uploadedUrls = mutableListOf<String>()
                var hasAnyFailure = false
                var detectedMediaType = "image"

                // Upload each media item
                for ((index, item) in items.withIndex()) {
                    // Skip already uploaded items
                    if (item.uploadState == MediaUploadState.SUCCESS && item.uploadedUrl != null) {
                        uploadedUrls.add(item.uploadedUrl)
                        continue
                    }

                    _currentUploadingIndex.value = index
                    updateMediaItemState(index, MediaUploadState.UPLOADING)

                    Log.d(TAG, "Uploading media ${index + 1}/${items.size}: ${item.uri}")

                    try {
                        val contentType = context.contentResolver.getType(item.uri) ?: 
                            if (item.mediaType == "video") "video/mp4" else "image/jpeg"
                        
                        val extension = when {
                            contentType.contains("png") -> "png"
                            contentType.contains("gif") -> "gif"
                            contentType.contains("video") -> "mp4"
                            contentType.contains("webp") -> "webp"
                            else -> "jpg"
                        }
                        
                        val filename = "${postId}_${index}.$extension"
                        
                        // Get file size to determine upload method
                        val pfd = context.contentResolver.openFileDescriptor(item.uri, "r")
                        val fileSize = pfd?.statSize ?: 0L
                        pfd?.close()
                        
                        val fileSizeMB = (fileSize / 1024 / 1024).toInt()
                        Log.d(TAG, "File size: ${fileSizeMB}MB")
                        
                        // Use multipart upload for files > 100MB
                        if (fileSize > 100 * 1024 * 1024) {
                            Log.d(TAG, "Using multipart upload for large file (${fileSizeMB}MB)")
                            
                            val uploadResult = ApiService.uploadLargeFile(
                                item.uri,
                                context.contentResolver,
                                filename,
                                contentType,
                                "posts"
                            ) { progress ->
                                // Real-time progress update for this item
                                val baseProgress = (index.toFloat() / items.size) * 100f
                                val itemProgress = (progress * 100f) / items.size
                                _overallProgress.value = baseProgress + itemProgress
                                
                                // Also update individual item progress
                                updateMediaItemProgress(index, progress)
                            }
                            
                            if (uploadResult.isSuccess) {
                                val fileUrl = uploadResult.getOrThrow()
                                uploadedUrls.add(fileUrl)
                                updateMediaItemState(index, MediaUploadState.SUCCESS, uploadedUrl = fileUrl)
                                
                                if (item.mediaType == "video") {
                                    detectedMediaType = "video"
                                }
                                
                                Log.d(TAG, "Uploaded ${index + 1}/${items.size}: $fileUrl")
                            } else {
                                updateMediaItemState(index, MediaUploadState.FAILED, "Upload failed")
                                hasAnyFailure = true
                            }
                        } else {
                            // Regular streaming upload for smaller files
                            Log.d(TAG, "Getting presigned URL for $filename...")
                            Log.d(TAG, "Auth token present: ${ApiService.getAuthToken() != null}")
                            
                            val presignResult = ApiService.getPresignedUrl(filename, contentType, "posts")
                            
                            if (presignResult.isFailure) {
                                val errorMsg = presignResult.exceptionOrNull()?.message ?: "Unknown presign error"
                                Log.e(TAG, "Presign failed: $errorMsg")
                                updateMediaItemState(index, MediaUploadState.FAILED, errorMsg)
                                hasAnyFailure = true
                                continue
                            }
                            
                            val presignData = presignResult.getOrNull()
                            
                            if (presignData == null) {
                                updateMediaItemState(index, MediaUploadState.FAILED, "Empty presign response")
                                hasAnyFailure = true
                                continue
                            }

                            val uploadUrl = presignData.optString("uploadUrl", "")
                            val fileUrl = presignData.optString("fileUrl", "")

                            if (uploadUrl.isEmpty() || fileUrl.isEmpty()) {
                                updateMediaItemState(index, MediaUploadState.FAILED, "Invalid upload URL")
                                hasAnyFailure = true
                                continue
                            }

                            // Upload to S3 using streaming
                            val uploadResult = ApiService.uploadToPresignedUrl(
                                uploadUrl, 
                                item.uri, 
                                context.contentResolver, 
                                contentType
                            )
                            
                            if (uploadResult.isSuccess) {
                                uploadedUrls.add(fileUrl)
                                updateMediaItemState(index, MediaUploadState.SUCCESS, uploadedUrl = fileUrl)
                                
                                if (item.mediaType == "video") {
                                    detectedMediaType = "video"
                                }
                                
                                Log.d(TAG, "Uploaded ${index + 1}/${items.size}: $fileUrl")
                            } else {
                                updateMediaItemState(index, MediaUploadState.FAILED, "Upload failed")
                                hasAnyFailure = true
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error uploading media $index: ${e.message}")
                        updateMediaItemState(index, MediaUploadState.FAILED, e.message)
                        hasAnyFailure = true
                    }

                    // Update overall progress
                    _overallProgress.value = ((index + 1).toFloat() / items.size) * 100f
                }

                _currentUploadingIndex.value = -1

                // Check if we have any successful uploads
                if (uploadedUrls.isEmpty()) {
                    _postState.value = PostState.ERROR
                    _errorMessage.value = "All uploads failed. Tap retry on individual items."
                    return@launch
                }

                // Create the post with all uploaded URLs
                Log.d(TAG, "Creating post with ${uploadedUrls.size} media items")
                
                val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
                dateFormat.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val createdAtISO = dateFormat.format(java.util.Date())

                val post = Post(
                    postId = postId,
                    userId = user.userId,
                    username = user.username,
                    userAvatar = user.avatar,
                    content = _caption.value.ifBlank { "📸" },
                    mediaUrl = uploadedUrls.firstOrNull(),
                    mediaUrls = uploadedUrls,
                    mediaType = detectedMediaType,
                    createdAt = createdAtISO,
                    likesCount = 0,
                    commentsCount = 0,
                    viewsCount = 0
                )

                val success = BackendRepository.createPost(post)
                
                if (success) {
                    Log.d(TAG, "Post created successfully with ${uploadedUrls.size} media items!")
                    _postState.value = PostState.SUCCESS
                    
                    if (hasAnyFailure) {
                        _errorMessage.value = "Post created but some media failed to upload"
                    }
                } else {
                    _postState.value = PostState.ERROR
                    _errorMessage.value = "Failed to create post. Please try again."
                    Log.e(TAG, "BackendRepository.createPost returned false")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error in createPost: ${e.message}", e)
                _postState.value = PostState.ERROR
                _errorMessage.value = e.message ?: "Unknown error occurred"
            }
        }
    }

    private fun updateMediaItemState(
        index: Int, 
        state: MediaUploadState, 
        errorMessage: String? = null,
        uploadedUrl: String? = null
    ) {
        val current = _mediaItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(
                uploadState = state,
                errorMessage = errorMessage,
                uploadedUrl = uploadedUrl ?: current[index].uploadedUrl
            )
            _mediaItems.value = current
        }
    }
    
    private fun updateMediaItemProgress(index: Int, progress: Float) {
        val current = _mediaItems.value.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(
                uploadProgress = progress
            )
            _mediaItems.value = current
        }
    }
}
