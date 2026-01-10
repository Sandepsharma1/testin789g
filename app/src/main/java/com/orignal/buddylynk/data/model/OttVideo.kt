package com.orignal.buddylynk.data.model

/**
 * OTT Video - YouTube-like long video content
 */
data class OttVideo(
    val videoId: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val duration: Long = 0,           // Duration in seconds
    val viewCount: Int = 0,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorAvatar: String? = null,
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val createdAt: String = "",
    val updatedAt: String = ""
)

/**
 * OTT Video Categories
 */
object OttCategories {
    val categories = listOf(
        OttCategory("All", "📺", 0xFF6366F1),
        OttCategory("Music", "🎵", 0xFFEC4899),
        OttCategory("Gaming", "🎮", 0xFF10B981),
        OttCategory("Education", "📚", 0xFFF59E0B),
        OttCategory("Entertainment", "🎬", 0xFF8B5CF6),
        OttCategory("Sports", "⚽", 0xFF22C55E),
        OttCategory("Technology", "💻", 0xFF3B82F6),
        OttCategory("News", "📰", 0xFFEF4444),
        OttCategory("Comedy", "😂", 0xFFF97316),
        OttCategory("Vlogs", "📹", 0xFF06B6D4)
    )
}

data class OttCategory(
    val name: String,
    val emoji: String,
    val color: Long
)

/**
 * OTT Comment
 */
data class OttComment(
    val commentId: String = "",
    val videoId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String? = null,
    val content: String = "",
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: String = ""
)
