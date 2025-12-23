package com.orignal.buddylynk.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * API Service for secure backend communication
 * NO AWS KEYS - All access via JWT-authenticated API calls
 * 
 * Security Features:
 * - Certificate Pinning (when using HTTPS)
 * - Request timeout limits
 * - No sensitive data logging in release builds
 */
object ApiService {
    
    // Certificate pinning for HTTPS endpoints (add your server's certificate hash)
    // To get the hash, run: openssl s_client -connect yourdomain.com:443 | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
    private val certificatePinner = CertificatePinner.Builder()
        // Add your production domain certificate pins here when using HTTPS
        // .add("yourdomain.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
        // .add("*.cloudfront.net", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        .build()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Enable certificate pinning when using HTTPS
        // .certificatePinner(certificatePinner)
        .build()
    
    private var authToken: String? = null
    
    fun setAuthToken(token: String?) {
        authToken = token
    }
    
    fun getAuthToken(): String? = authToken
    
    private fun buildAuthRequest(url: String): Request.Builder {
        return Request.Builder()
            .url(url)
            .apply {
                authToken?.let { header("Authorization", "Bearer $it") }
            }
    }
    
    // ========== AUTH ==========
    
    suspend fun login(email: String, password: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            
            val request = Request.Builder()
                .url(ApiConfig.Auth.LOGIN)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                val result = JSONObject(body)
                authToken = result.optString("token")
                Result.success(result)
            } else {
                Result.failure(Exception(JSONObject(body).optString("error", "Login failed")))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Login error", e)
            Result.failure(e)
        }
    }
    
    suspend fun register(email: String, username: String, password: String, fullName: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("username", username)
                put("password", password)
                put("fullName", fullName)
            }
            
            val request = Request.Builder()
                .url(ApiConfig.Auth.REGISTER)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                val result = JSONObject(body)
                authToken = result.optString("token")
                Result.success(result)
            } else {
                Result.failure(Exception(JSONObject(body).optString("error", "Registration failed")))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Register error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Google Auth - authenticate with Google credentials, get JWT token
     */
    suspend fun googleAuth(email: String, displayName: String?, avatar: String?): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                displayName?.let { put("displayName", it) }
                avatar?.let { put("avatar", it) }
            }
            
            val request = Request.Builder()
                .url(ApiConfig.Auth.GOOGLE_AUTH)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                val result = JSONObject(body)
                authToken = result.optString("token")
                Log.d("ApiService", "Google auth successful, token set: ${authToken?.take(20)}...")
                Result.success(result)
            } else {
                Result.failure(Exception(JSONObject(body).optString("error", "Google auth failed")))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Google auth error", e)
            Result.failure(e)
        }
    }
    
    // ========== USERS ==========
    
    suspend fun getCurrentUser(): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Users.ME).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to get user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUser(userId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Users.getUser(userId)).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchUsers(query: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Users.search(query)).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val users = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    users.add(array.getJSONObject(i))
                }
                Result.success(users)
            } else {
                Result.failure(Exception("Search failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getRecommendedUsers(limit: Int = 20): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val url = "${ApiConfig.API_URL}/users/recommended/list?limit=$limit"
            Log.d("ApiService", "getRecommendedUsers: calling $url")
            val request = buildAuthRequest(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            Log.d("ApiService", "getRecommendedUsers: response code=${response.code}, body length=${body.length}")
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                Log.d("ApiService", "getRecommendedUsers: parsed ${array.length()} users")
                val users = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    users.add(array.getJSONObject(i))
                }
                Result.success(users)
            } else {
                Log.e("ApiService", "getRecommendedUsers: failed with body=$body")
                Result.failure(Exception("Get recommended users failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "getRecommendedUsers: exception", e)
            Result.failure(e)
        }
    }
    
    // ========== POSTS ==========
    
    suspend fun getFeed(): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiService", "getFeed: Calling ${ApiConfig.Posts.FEED}")
            val request = buildAuthRequest(ApiConfig.Posts.FEED).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            Log.d("ApiService", "getFeed: Response code=${response.code}, body length=${body.length}")
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val posts = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    posts.add(array.getJSONObject(i))
                }
                Log.d("ApiService", "getFeed: Parsed ${posts.size} posts successfully")
                Result.success(posts)
            } else {
                Log.e("ApiService", "getFeed: Failed with code ${response.code}, body: ${body.take(200)}")
                Result.failure(Exception("Failed to load feed: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "getFeed: Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun createPost(content: String, mediaUrls: List<String>, mediaType: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("content", content)
                put("mediaUrls", JSONArray(mediaUrls))
                put("mediaType", mediaType)
            }
            
            val request = buildAuthRequest(ApiConfig.Posts.CREATE)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to create post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun likePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Posts.likePost(postId))
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== UPLOAD (Pre-signed URLs) ==========
    
    suspend fun getPresignedUrl(filename: String, contentType: String, folder: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("filename", filename)
                put("contentType", contentType)
                put("folder", folder)
            }
            
            val request = buildAuthRequest(ApiConfig.Upload.PRESIGN)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to get upload URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun uploadToPresignedUrl(presignedUrl: String, data: ByteArray, contentType: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(presignedUrl)
                .put(data.toRequestBody(contentType.toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== GROUPS ==========
    
    suspend fun getGroups(): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Groups.LIST).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val groups = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    groups.add(array.getJSONObject(i))
                }
                Result.success(groups)
            } else {
                Result.failure(Exception("Failed to get groups"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get a single group by ID (includes embedded posts array)
     */
    suspend fun getGroup(groupId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to get group: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting group: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun createGroup(name: String, description: String?, isPublic: Boolean): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("name", name)
                put("description", description)
                put("isPublic", isPublic)
            }
            
            val request = buildAuthRequest(ApiConfig.Groups.CREATE)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to create group"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== FOLLOWS ==========
    
    suspend fun follow(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Follows.follow(userId))
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unfollow(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Follows.follow(userId))
                .delete()
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun isFollowing(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Follows.check(userId)).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body).optBoolean("isFollowing", false))
            } else {
                Result.failure(Exception("Failed to check follow status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFollowers(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/follows/$userId/followers")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val followers = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    item?.optString("followerId")?.let { followers.add(it) }
                }
                Result.success(followers)
            } else {
                Result.failure(Exception("Failed to get followers"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getFollowing(userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("${ApiConfig.BASE_URL}/api/follows/$userId/following")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val following = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                    item?.optString("followingId")?.let { following.add(it) }
                }
                Result.success(following)
            } else {
                Result.failure(Exception("Failed to get following"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== MESSAGES ==========
    
    suspend fun getConversations(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Messages.CONVERSATIONS).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val partners = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    partners.add(array.getString(i))
                }
                Result.success(partners)
            } else {
                Result.failure(Exception("Failed to get conversations"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getMessages(userId: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Messages.chat(userId)).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val messages = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    messages.add(array.getJSONObject(i))
                }
                Result.success(messages)
            } else {
                Result.failure(Exception("Failed to get messages"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendMessage(userId: String, content: String, mediaUrl: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("content", content)
                if (mediaUrl != null) put("mediaUrl", mediaUrl)
            }
            
            val request = buildAuthRequest(ApiConfig.Messages.chat(userId))
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to send message"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== BLOCKS ==========
    
    suspend fun getBlockedUsers(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/blocks").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val blocked = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    blocked.add(array.getString(i))
                }
                Result.success(blocked)
            } else {
                Result.failure(Exception("Failed to get blocked users"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun blockUser(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/blocks/$userId")
                .post("".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unblockUser(userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/blocks/$userId").delete().build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== SAVED POSTS ==========
    
    suspend fun getSavedPostIds(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/posts/saved/list").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val saved = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    saved.add(array.getString(i))
                }
                Result.success(saved)
            } else {
                Result.failure(Exception("Failed to get saved posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun savePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/posts/$postId/save")
                .post("".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unsavePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/posts/$postId/save").delete().build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== DELETE POST ==========
    
    suspend fun deletePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/posts/$postId").delete().build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== GROUP MESSAGES ==========
    
    suspend fun getGroupMessages(groupId: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/messages").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val messages = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    messages.add(array.getJSONObject(i))
                }
                Result.success(messages)
            } else {
                Result.failure(Exception("Failed to get group messages: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendGroupMessage(groupId: String, content: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("content", content)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/messages").post(body).build()
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(responseBody))
            } else {
                Result.failure(Exception("Failed to send message: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== FCM TOKEN ==========
    
    suspend fun updateFcmToken(token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("fcmToken", token)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = buildAuthRequest("${ApiConfig.API_URL}/users/fcm-token")
                .put(body)
                .build()
            val response = client.newCall(request).execute()
            
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e("ApiService", "Failed to update FCM token", e)
            Result.failure(e)
        }
    }
}
