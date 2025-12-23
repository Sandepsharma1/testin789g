package com.orignal.buddylynk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.data.repository.BackendRepository
import com.orignal.buddylynk.data.moderation.ModerationService
import com.orignal.buddylynk.data.model.Post
import com.orignal.buddylynk.data.redis.RedisService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HomeViewModel - Real-time posts with Redis caching
 */
class HomeViewModel : ViewModel() {
    
    // Current user ID for ownership checks
    val currentUserId: String = AuthManager.currentUser.value?.userId ?: ""
    
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Trending posts from Redis
    private val _trendingPosts = MutableStateFlow<List<String>>(emptyList())
    val trendingPosts: StateFlow<List<String>> = _trendingPosts.asStateFlow()
    
    // Blocked users list
    private val _blockedUsers = MutableStateFlow<Set<String>>(emptySet())
    val blockedUsers: StateFlow<Set<String>> = _blockedUsers.asStateFlow()
    
    // Saved post IDs
    private val _savedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIds: StateFlow<Set<String>> = _savedPostIds.asStateFlow()
    
    init {
        loadBlockedUsers()
        loadSavedPosts()
        loadPosts()
        loadTrending()
        startAutoRefresh()
    }
    
    /**
     * Load blocked users from ModerationService (local + synced state)
     */
    private fun loadBlockedUsers() {
        if (currentUserId.isBlank()) return
        
        // First, load blocked users from API
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("HomeViewModel", "Initializing ModerationService...")
                ModerationService.init()
                
                // Get initial blocked users
                val blocked = ModerationService.blockedUsers.value
                _blockedUsers.value = blocked
                android.util.Log.d("HomeViewModel", "Loaded ${blocked.size} blocked users from API")
                
                // Re-filter posts if we have any
                if (blocked.isNotEmpty() && _posts.value.isNotEmpty()) {
                    _posts.value = _posts.value.filter { post -> post.userId !in blocked }
                    android.util.Log.d("HomeViewModel", "Filtered posts, remaining: ${_posts.value.size}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading blocked users: ${e.message}")
            }
        }
        
        // Observe blocked users changes for real-time updates
        viewModelScope.launch {
            ModerationService.blockedUsers.collect { blocked ->
                if (blocked != _blockedUsers.value) {
                    _blockedUsers.value = blocked
                    android.util.Log.d("HomeViewModel", "Blocked users changed: ${blocked.size} users")
                    
                    // Re-filter posts when blocked list changes
                    if (blocked.isNotEmpty() && _posts.value.isNotEmpty()) {
                        _posts.value = _posts.value.filter { post -> post.userId !in blocked }
                    }
                }
            }
        }
    }
    
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                android.util.Log.d("HomeViewModel", "Starting to load posts from BackendRepository...")
                
                // ALWAYS fetch fresh blocked users from API BEFORE loading posts
                try {
                    val freshBlockedIds = BackendRepository.getBlockedUsers()
                    _blockedUsers.value = freshBlockedIds.toSet()
                    android.util.Log.d("HomeViewModel", "Loaded ${freshBlockedIds.size} blocked users for filtering")
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to load blocked users: ${e.message}")
                }
                
                // Fetch posts via BackendRepository (API or DynamoDB based on USE_API)
                val fetchedPosts = BackendRepository.getFeedPosts()
                android.util.Log.d("HomeViewModel", "Fetched: ${fetchedPosts.size} posts via BackendRepository")
                
                if (fetchedPosts.isEmpty()) {
                    android.util.Log.d("HomeViewModel", "No posts found.")
                    _posts.value = emptyList()
                } else {
                    // Filter out blocked users using fresh list
                    val blockedSet = _blockedUsers.value
                    val filteredPosts = fetchedPosts.filter { post -> 
                        post.userId !in blockedSet 
                    }
                    android.util.Log.d("HomeViewModel", "Filtered ${fetchedPosts.size} -> ${filteredPosts.size} posts (blocked ${blockedSet.size} users)")
                    
                    // Priority boost: User's posts from last 20 minutes appear at TOP
                    val now = System.currentTimeMillis()
                    val twentyMinutesAgo = now - (20 * 60 * 1000) // 20 minutes in ms

                    val (boostedPosts, otherPosts) = filteredPosts.partition { post ->
                        // Check if post is from current user AND within last 20 minutes
                        post.userId == currentUserId && isWithinTimeWindow(post.createdAt, twentyMinutesAgo)
                    }
                    
                    // Boosted posts first (sorted by newest), then others (sorted by newest)
                    val sortedPosts = boostedPosts.sortedByDescending { it.createdAt } + 
                                     otherPosts.sortedByDescending { it.createdAt }
                    
                    // Show posts IMMEDIATELY - no waiting for Redis
                    _posts.value = sortedPosts
                    _isLoading.value = false
                    
                    // Enhance with Redis views in BACKGROUND (non-blocking)
                    launch {
                        try {
                            val enhancedPosts = sortedPosts.map { post ->
                                val redisViews = RedisService.getViews(post.postId)
                                post.copy(viewsCount = maxOf(post.viewsCount, redisViews.toInt()))
                            }
                            _posts.value = enhancedPosts
                        } catch (e: Exception) {
                            // Redis enhancement failed, keep original posts
                            android.util.Log.w("HomeViewModel", "Redis enhancement failed: ${e.message}")
                        }
                    }
                    return@launch
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading posts", e)
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Helper: Check if post createdAt timestamp is within the time window
    private fun isWithinTimeWindow(createdAt: String, thresholdMs: Long): Boolean {
        return try {
            val formatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            formatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val postTime = formatter.parse(createdAt.take(19))?.time ?: 0L
            postTime >= thresholdMs
        } catch (e: Exception) {
            false
        }
    }

    // Add a newly created post to the TOP of the feed immediately
    fun addNewPost(post: Post) {
        val currentPosts = _posts.value.toMutableList()
        // Remove any existing post with same ID (in case of duplicate)
        currentPosts.removeAll { it.postId == post.postId }
        // Add new post to the TOP
        currentPosts.add(0, post)
        _posts.value = currentPosts
        android.util.Log.d("HomeViewModel", "Added new post ${post.postId} to top of feed")
    }
    
    // Refresh and force user's posts to show at top
    fun refreshWithUserPostsFirst() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val fetchedPosts = BackendRepository.getFeedPosts()
                // Sort: user's posts first (by createdAt desc), then others
                val sortedPosts = fetchedPosts.sortedWith(
                    compareByDescending<Post> { it.userId == currentUserId }
                        .thenByDescending { it.createdAt }
                )
                _posts.value = sortedPosts
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // Seeding removed for production

    
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                // Fetch fresh blocked users
                val freshBlockedIds = BackendRepository.getBlockedUsers()
                _blockedUsers.value = freshBlockedIds.toSet()
                
                val fetchedPosts = BackendRepository.getFeedPosts()
                
                // Filter blocked users
                val blockedSet = _blockedUsers.value
                val filteredPosts = fetchedPosts.filter { post -> post.userId !in blockedSet }
                
                // Enhance with Redis counters
                val enhancedPosts = filteredPosts.map { post ->
                    val redisViews = RedisService.getViews(post.postId)
                    post.copy(viewsCount = maxOf(post.viewsCount, redisViews.toInt()))
                }
                
                _posts.value = enhancedPosts
                loadTrending()
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    private fun loadTrending() {
        viewModelScope.launch {
            try {
                val trending = RedisService.getTrendingPosts(limit = 20)
                _trendingPosts.value = trending
            } catch (e: Exception) {
                // Silent fail for trending
            }
        }
    }
    
    // Auto-refresh every 30 seconds for real-time feel
    private fun startAutoRefresh() {
        viewModelScope.launch {
            try {
                while (true) {
                    delay(30_000) // 30 seconds
                    try {
                        // Fetch fresh blocked users
                        val freshBlockedIds = BackendRepository.getBlockedUsers()
                        _blockedUsers.value = freshBlockedIds.toSet()
                        
                        val fetchedPosts = BackendRepository.getFeedPosts()
                        
                        // Filter blocked users
                        val blockedSet = _blockedUsers.value
                        val filteredPosts = fetchedPosts.filter { post -> post.userId !in blockedSet }
                        
                        val enhancedPosts = filteredPosts.map { post ->
                            val redisViews = RedisService.getViews(post.postId)
                            post.copy(viewsCount = maxOf(post.viewsCount, redisViews.toInt()))
                        }
                        _posts.value = enhancedPosts
                    } catch (e: Exception) {
                        // Silent fail on auto-refresh
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Normal cancellation when ViewModel is cleared - do nothing
            }
        }
    }
    
    /**
     * Like post - INSTANT UI update, then async DB
     */
    fun likePost(postId: String) {
        // Get current post state
        val currentPost = _posts.value.find { it.postId == postId } ?: return
        val newIsLiked = !currentPost.isLiked
        val newCount = if (newIsLiked) currentPost.likesCount + 1 else (currentPost.likesCount - 1).coerceAtLeast(0)
        
        // UPDATE UI IMMEDIATELY (no coroutine delay!)
        _posts.value = _posts.value.map { post ->
            if (post.postId == postId) {
                post.copy(isLiked = newIsLiked, likesCount = newCount)
            } else post
        }
        
        // Update Redis + DynamoDB in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (newIsLiked) {
                    RedisService.incrementLikes(postId)
                    RedisService.incrementPostScore(postId, 2.0)
                } else {
                    RedisService.decrementLikes(postId)
                }
                // Save to API permanently (via BackendRepository)
                BackendRepository.likePost(postId)
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error updating likes: ${e.message}")
            }
        }
    }
    
    /**
     * Load saved posts from API
     */
    private fun loadSavedPosts() {
        if (currentUserId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedIds = BackendRepository.getSavedPostIds()
                _savedPostIds.value = savedIds.toSet()
                android.util.Log.d("HomeViewModel", "Loaded ${savedIds.size} saved posts from API")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading saved posts: ${e.message}")
            }
        }
    }
    
    /**
     * Toggle save/bookmark post - INSTANT UI update, then save to DB
     */
    fun bookmarkPost(postId: String) {
        val isSaved = _savedPostIds.value.contains(postId)
        
        // Update UI immediately
        if (isSaved) {
            _savedPostIds.value = _savedPostIds.value - postId
        } else {
            _savedPostIds.value = _savedPostIds.value + postId
        }
        
        // Update post isBookmarked state
        _posts.value = _posts.value.map { post ->
            if (post.postId == postId) {
                post.copy(isBookmarked = !isSaved)
            } else post
        }
        
        // Persist to API in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (isSaved) {
                    BackendRepository.unsavePost(postId)
                    android.util.Log.d("HomeViewModel", "Unsaved post $postId via API")
                } else {
                    BackendRepository.savePost(postId)
                    android.util.Log.d("HomeViewModel", "Saved post $postId via API")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error saving/unsaving post: ${e.message}")
            }
        }
    }
    
    /**
     * Share post - INSTANT UI update, then save to DB
     */
    fun sharePost(postId: String) {
        // Get current post
        val currentPost = _posts.value.find { it.postId == postId } ?: return
        val newCount = currentPost.sharesCount + 1
        
        android.util.Log.d("HomeViewModel", "sharePost: old=${currentPost.sharesCount}, new=$newCount")
        
        // UPDATE UI IMMEDIATELY (main thread, no coroutine)
        _posts.value = _posts.value.map { post ->
            if (post.postId == postId) {
                post.copy(sharesCount = newCount)
            } else post
        }
        
        android.util.Log.d("HomeViewModel", "sharePost: state updated, list size=${_posts.value.size}")
        
        // Update Redis + DynamoDB in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RedisService.incrementShares(postId)
                RedisService.incrementPostScore(postId, 3.0)
                // Shares tracked via Redis for now (no API endpoint yet)
                android.util.Log.d("HomeViewModel", "Share count updated to $newCount via Redis")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Share update error: ${e.message}")
            }
        }
    }
    
    /**
     * Track view - update Redis, DynamoDB, and local state
     */
    fun incrementViews(postId: String) {
        // Get current count first
        val currentPost = _posts.value.find { it.postId == postId }
        val newViewCount = (currentPost?.viewsCount ?: 0) + 1
        
        // Update local state immediately
        _posts.value = _posts.value.map { post ->
            if (post.postId == postId) {
                post.copy(viewsCount = newViewCount)
            } else post
        }
        
        // Update Redis + DynamoDB in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                RedisService.incrementViews(postId)
                RedisService.incrementPostScore(postId, 0.1)
                // Views tracked via Redis for now (no API endpoint yet)
                android.util.Log.d("HomeViewModel", "View count updated to $newViewCount via Redis")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Views update error: ${e.message}")
            }
        }
    }
    
    /**
     * Delete post (own posts only) via API
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val success = BackendRepository.deletePost(postId)
                if (success) {
                    // Remove from local state
                    _posts.value = _posts.value.filter { it.postId != postId }
                    android.util.Log.d("HomeViewModel", "Post $postId deleted via API")
                } else {
                    _error.value = "Failed to delete post"
                }
            } catch (e: Exception) {
                _error.value = "Failed to delete post"
            }
        }
    }
    
    /**
     * Block user - instantly hide their posts and save to DB
     */
    fun blockUser(userId: String) {
        // Immediately add to blocked set and hide posts
        _blockedUsers.value = _blockedUsers.value + userId
        _posts.value = _posts.value.filter { it.userId != userId }
        
        // Save to API in background
        viewModelScope.launch(Dispatchers.IO) {
            try {
                BackendRepository.blockUser(userId)
                android.util.Log.d("HomeViewModel", "User $userId blocked via API")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error blocking user: ${e.message}")
            }
        }
    }
    
    /**
     * Report post via API (TODO: implement report API)
     */
    fun reportPost(postId: String) {
        viewModelScope.launch {
            try {
                // TODO: Implement report API endpoint
                android.util.Log.d("HomeViewModel", "Report submitted for post $postId")
            } catch (e: Exception) {
                _error.value = "Failed to report post"
            }
        }
    }
}
