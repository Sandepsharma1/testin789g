package com.orignal.buddylynk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.data.model.OttVideo
import com.orignal.buddylynk.data.model.OttCategories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * OTT ViewModel - Manages OTT video streaming state
 */
class OttViewModel : ViewModel() {
    
    // Video feed
    private val _videos = MutableStateFlow<List<OttVideo>>(emptyList())
    val videos: StateFlow<List<OttVideo>> = _videos.asStateFlow()
    
    // Trending videos
    private val _trendingVideos = MutableStateFlow<List<OttVideo>>(emptyList())
    val trendingVideos: StateFlow<List<OttVideo>> = _trendingVideos.asStateFlow()
    
    // Current video being watched
    private val _currentVideo = MutableStateFlow<OttVideo?>(null)
    val currentVideo: StateFlow<OttVideo?> = _currentVideo.asStateFlow()
    
    // Search results
    private val _searchResults = MutableStateFlow<List<OttVideo>>(emptyList())
    val searchResults: StateFlow<List<OttVideo>> = _searchResults.asStateFlow()
    
    // My videos
    private val _myVideos = MutableStateFlow<List<OttVideo>>(emptyList())
    val myVideos: StateFlow<List<OttVideo>> = _myVideos.asStateFlow()
    
    // Saved videos
    private val _savedVideos = MutableStateFlow<List<OttVideo>>(emptyList())
    val savedVideos: StateFlow<List<OttVideo>> = _savedVideos.asStateFlow()
    
    // Watch history
    private val _watchHistory = MutableStateFlow<List<OttVideo>>(emptyList())
    val watchHistory: StateFlow<List<OttVideo>> = _watchHistory.asStateFlow()
    
    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()
    
    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()
    
    // Selected category
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()
    
    // Error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadVideos()
        loadTrendingVideos()
    }
    
    fun setCategory(category: String) {
        _selectedCategory.value = category
        loadVideos()
    }
    
    /**
     * Refresh all video feeds - call after upload completes
     */
    fun refreshVideos() {
        loadVideos()
        loadTrendingVideos()
    }
    
    fun loadVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getOttVideos(_selectedCategory.value)
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val videosJson = response.optJSONArray("videos")
                    val videoList = mutableListOf<OttVideo>()
                    
                    if (videosJson != null) {
                        for (i in 0 until videosJson.length()) {
                            val v = videosJson.getJSONObject(i)
                            videoList.add(parseVideo(v))
                        }
                    }
                    
                    _videos.value = videoList
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load videos"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadTrendingVideos() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getOttTrending()
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val videosJson = response.optJSONArray("videos")
                    val videoList = mutableListOf<OttVideo>()
                    
                    if (videosJson != null) {
                        for (i in 0 until videosJson.length()) {
                            val v = videosJson.getJSONObject(i)
                            videoList.add(parseVideo(v))
                        }
                    }
                    
                    _trendingVideos.value = videoList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun loadVideo(videoId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getOttVideo(videoId)
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val videoJson = response.optJSONObject("video")
                    if (videoJson != null) {
                        _currentVideo.value = parseVideo(videoJson)
                    }
                }
                
                // Increment view count
                withContext(Dispatchers.IO) {
                    ApiService.incrementOttView(videoId)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Failed to load video"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchVideos(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.searchOttVideos(query)
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val videosJson = response.optJSONArray("videos")
                    val videoList = mutableListOf<OttVideo>()
                    
                    if (videosJson != null) {
                        for (i in 0 until videosJson.length()) {
                            val v = videosJson.getJSONObject(i)
                            videoList.add(parseVideo(v))
                        }
                    }
                    
                    _searchResults.value = videoList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun loadMyVideos() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getMyOttVideos()
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val videosJson = response.optJSONArray("videos")
                    val videoList = mutableListOf<OttVideo>()
                    
                    if (videosJson != null) {
                        for (i in 0 until videosJson.length()) {
                            val v = videosJson.getJSONObject(i)
                            videoList.add(parseVideo(v))
                        }
                    }
                    
                    _myVideos.value = videoList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun likeVideo(videoId: String) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.likeOttVideo(videoId)
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val isLiked = response.optBoolean("isLiked", false)
                    val likeCount = response.optInt("likeCount", 0)
                    
                    // Update current video if it matches
                    _currentVideo.value?.let { current ->
                        if (current.videoId == videoId) {
                            _currentVideo.value = current.copy(
                                isLiked = isLiked,
                                likeCount = likeCount
                            )
                        }
                    }
                    
                    // Update in videos list
                    _videos.value = _videos.value.map { v ->
                        if (v.videoId == videoId) v.copy(isLiked = isLiked, likeCount = likeCount)
                        else v
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun uploadVideo(
        title: String,
        description: String,
        videoUrl: String,
        thumbnailUrl: String,
        category: String,
        duration: Long,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f
            
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.uploadOttVideo(
                        title = title,
                        description = description,
                        videoUrl = videoUrl,
                        thumbnailUrl = thumbnailUrl,
                        category = category,
                        duration = duration
                    )
                }
                
                _uploadProgress.value = 1f
                
                if (response != null && response.optBoolean("success", false)) {
                    loadMyVideos()
                    onSuccess()
                } else {
                    onError(response?.optString("message") ?: "Upload failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Upload failed")
            } finally {
                _isUploading.value = false
                _uploadProgress.value = 0f
            }
        }
    }
    
    fun clearCurrentVideo() {
        _currentVideo.value = null
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun deleteVideo(
        videoId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    ApiService.deleteOttVideo(videoId)
                }
                
                result.onSuccess {
                    // Remove from all lists
                    _videos.value = _videos.value.filter { it.videoId != videoId }
                    _trendingVideos.value = _trendingVideos.value.filter { it.videoId != videoId }
                    _myVideos.value = _myVideos.value.filter { it.videoId != videoId }
                    _searchResults.value = _searchResults.value.filter { it.videoId != videoId }
                    onSuccess()
                }.onFailure { error ->
                    onError(error.message ?: "Delete failed")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "Delete failed")
            }
        }
    }
    
    /**
     * Load user's saved videos
     */
    fun loadSavedVideos() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getSavedVideos()
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val videosJson = response.optJSONArray("videos")
                    val videoList = mutableListOf<OttVideo>()
                    
                    if (videosJson != null) {
                        for (i in 0 until videosJson.length()) {
                            val v = videosJson.getJSONObject(i)
                            videoList.add(parseVideo(v))
                        }
                    }
                    
                    _savedVideos.value = videoList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Load user's watch history
     */
    fun loadWatchHistory() {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.getWatchHistory()
                }
                
                if (response != null && response.optBoolean("success", false)) {
                    val videosJson = response.optJSONArray("videos")
                    val videoList = mutableListOf<OttVideo>()
                    
                    if (videosJson != null) {
                        for (i in 0 until videosJson.length()) {
                            val v = videosJson.getJSONObject(i)
                            videoList.add(parseVideo(v))
                        }
                    }
                    
                    _watchHistory.value = videoList
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Toggle save/unsave video
     */
    fun toggleSaveVideo(videoId: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiService.toggleSaveVideo(videoId)
                }
                
                val saved = response?.optBoolean("saved", false) ?: false
                onResult(saved)
                
                // Refresh saved videos list
                loadSavedVideos()
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false)
            }
        }
    }
    
    private fun parseVideo(json: JSONObject): OttVideo {
        return OttVideo(
            videoId = json.optString("videoId", ""),
            title = json.optString("title", ""),
            description = json.optString("description", ""),
            videoUrl = json.optString("videoUrl", ""),
            thumbnailUrl = json.optString("thumbnailUrl", ""),
            duration = json.optLong("duration", 0),
            viewCount = json.optInt("viewCount", 0),
            likeCount = json.optInt("likeCount", 0),
            commentCount = json.optInt("commentCount", 0),
            creatorId = json.optString("creatorId", ""),
            creatorName = json.optString("creatorName", ""),
            creatorAvatar = json.optString("creatorAvatar", null),
            category = json.optString("category", "General"),
            isLiked = json.optBoolean("isLiked", false),
            isSaved = json.optBoolean("isSaved", false),
            createdAt = json.optString("createdAt", ""),
            updatedAt = json.optString("updatedAt", "")
        )
    }
}

