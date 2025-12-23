package com.orignal.buddylynk.data.api

/**
 * API Configuration for BuddyLynk
 * 
 * All API calls go through the secure backend - NO AWS keys here!
 */
object ApiConfig {
    // Direct EC2 access (CloudFront CDN available at d2cwas7x7omdpp.cloudfront.net)
    const val BASE_URL = "http://52.0.95.126:3000"
    const val API_URL = "$BASE_URL/api"
    
    // Media CDN for images/videos
    const val MEDIA_CDN_URL = "https://buddylynk-media-bucket-2024.s3.us-east-1.amazonaws.com"
    
    // API Endpoints
    object Auth {
        const val LOGIN = "$API_URL/auth/login"
        const val REGISTER = "$API_URL/auth/register"
        const val GOOGLE_AUTH = "$API_URL/auth/google"
    }
    
    object Users {
        const val ME = "$API_URL/users/me"
        fun getUser(userId: String) = "$API_URL/users/$userId"
        fun search(query: String) = "$API_URL/users/search/$query"
    }
    
    object Posts {
        const val FEED = "$API_URL/posts/feed"
        const val CREATE = "$API_URL/posts"
        fun getPost(postId: String) = "$API_URL/posts/$postId"
        fun likePost(postId: String) = "$API_URL/posts/$postId/like"
        fun userPosts(userId: String) = "$API_URL/posts/user/$userId"
    }
    
    object Upload {
        const val PRESIGN = "$API_URL/upload/presign"
    }
    
    object Groups {
        const val LIST = "$API_URL/groups"
        const val CREATE = "$API_URL/groups"
        fun getGroup(groupId: String) = "$API_URL/groups/$groupId"
        fun messages(groupId: String) = "$API_URL/groups/$groupId/messages"
    }
    
    object Follows {
        fun follow(userId: String) = "$API_URL/follows/$userId"
        fun followers(userId: String) = "$API_URL/follows/$userId/followers"
        fun following(userId: String) = "$API_URL/follows/$userId/following"
        fun check(userId: String) = "$API_URL/follows/check/$userId"
    }
    
    object Messages {
        const val CONVERSATIONS = "$API_URL/messages/conversations"
        fun chat(userId: String) = "$API_URL/messages/$userId"
    }
    
    object Stories {
        const val LIST = "$API_URL/stories"
        const val CREATE = "$API_URL/stories"
    }
}
