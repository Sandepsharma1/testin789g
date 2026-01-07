package com.orignal.buddylynk.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import coil.request.CachePolicy
import coil.request.ImageRequest

/**
 * Performance Optimization Utilities for Low-End Devices
 * 
 * Key optimizations:
 * - Reduced recomposition with stable parameters
 * - Optimized image loading with aggressive caching
 * - Hardware layer rendering for smooth scroll
 * - Memory-efficient composables
 */

/**
 * Modifier for smooth scrolling items
 * Uses hardware layer rendering for better performance
 */
fun Modifier.smoothScrollItem(): Modifier = this.graphicsLayer {
    // Enable hardware layer for smooth rendering during scroll
    // This prevents recomposition lag on low-end devices
}

/**
 * Optimized ImageRequest builder for feed images
 * Uses aggressive caching and reduced memory footprint
 */
@Composable
fun rememberOptimizedImageRequest(
    context: android.content.Context,
    imageUrl: String?,
    size: Int = 512 // Reduced size for feed thumbnails
): ImageRequest {
    return remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(150) // Faster crossfade
            .size(size) // Constrain image size for memory
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .allowHardware(true) // Use hardware bitmaps
            .build()
    }
}

/**
 * Stable wrapper for post data to prevent unnecessary recomposition
 */
@Stable
data class StablePostData(
    val postId: String,
    val mediaUrl: String?,
    val username: String?,
    val avatar: String?,
    val content: String,
    val isVideo: Boolean
) {
    companion object {
        fun fromPost(post: com.orignal.buddylynk.data.model.Post): StablePostData {
            val mediaUrl = post.mediaUrl ?: post.mediaUrls.firstOrNull()
            return StablePostData(
                postId = post.postId,
                mediaUrl = mediaUrl,
                username = post.username,
                avatar = post.userAvatar,
                content = post.content,
                isVideo = mediaUrl?.let { url ->
                    url.endsWith(".mp4", ignoreCase = true) ||
                    url.endsWith(".webm", ignoreCase = true) ||
                    url.endsWith(".mov", ignoreCase = true) ||
                    post.mediaType == "video"
                } ?: false
            )
        }
    }
}

/**
 * Configuration for scroll performance
 */
object ScrollPerformanceConfig {
    // Disable heavy animations during scroll
    const val DISABLE_ANIMATIONS_DURING_SCROLL = true
    
    // Reduce image quality for low-end devices
    const val LOW_END_IMAGE_SIZE = 384
    const val NORMAL_IMAGE_SIZE = 512
    
    // Debounce time for scroll-triggered operations
    const val SCROLL_DEBOUNCE_MS = 150L
    
    // Video polling interval (higher = less CPU usage)
    const val VIDEO_POLL_INTERVAL_MS = 500L
}
