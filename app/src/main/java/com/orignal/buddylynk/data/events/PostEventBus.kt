package com.orignal.buddylynk.data.events

import com.orignal.buddylynk.data.model.Post
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * PostEventBus - Global event broadcaster for post-related events
 * 
 * Used to notify components (like HomeScreen) when a new post is created
 * so the feed can be refreshed immediately without waiting for navigation.
 */
object PostEventBus {
    
    // Event for when a new post is created
    private val _newPostCreated = MutableSharedFlow<Post>(replay = 1)
    val newPostCreated: SharedFlow<Post> = _newPostCreated.asSharedFlow()
    
    // Event for when feed should refresh (general trigger)
    private val _feedRefreshRequest = MutableSharedFlow<Unit>(replay = 1)
    val feedRefreshRequest: SharedFlow<Unit> = _feedRefreshRequest.asSharedFlow()
    
    /**
     * Broadcast that a new post was created
     * HomeViewModel listens to this and adds the post to the top of the feed
     */
    suspend fun notifyNewPostCreated(post: Post) {
        _newPostCreated.emit(post)
    }
    
    /**
     * Request a feed refresh (used when post details aren't available)
     */
    suspend fun requestFeedRefresh() {
        _feedRefreshRequest.emit(Unit)
    }
}
