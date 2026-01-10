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
 * - HTTPS via CloudFront CDN
 * - Certificate Pinning for CloudFront
 * - Request timeout limits
 * - No sensitive data logging in release builds
 */
object ApiService {
    
    // Certificate pinning for HTTPS
    // Using Let's Encrypt certificates for app.buddylynk.com
    private val certificatePinner = CertificatePinner.Builder()
        // Let's Encrypt certificate chain for app.buddylynk.com
        .add("app.buddylynk.com", "sha256/kIdp6NNEd8rLSsI53aD5ESiJ4VNWq1EJVhmdOMj7JPs=")  // Let's Encrypt R3
        .add("app.buddylynk.com", "sha256/JSMzqOOrtyOT1kmau6zKhgT676hGgczD5VMdRMyJZFA=")  // ISRG Root X1
        .add("app.buddylynk.com", "sha256/C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=")  // ISRG Root X1 backup
        // CloudFront for media CDN
        .add("*.cloudfront.net", "sha256/kIdp6NNEd8rLSsl53aD5ESiJ4VNWq1EJVhmdOMj7JPs=")
        .add("*.cloudfront.net", "sha256/JSMzqOOrtyOT1kmau6zKhgT676hGgczD5VMdRMyJZFA=")
        .add("*.cloudfront.net", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
        .build()
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // SECURITY: Certificate pinning enabled for HTTPS API calls
        .certificatePinner(certificatePinner)
        .build()
    
    // Separate client for S3 uploads (no certificate pinning - S3 uses Amazon certificates)
    private val s3Client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS) // Longer timeout for large uploads
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
    
    // Data class for paginated feed response
    data class FeedResponse(
        val posts: List<JSONObject>,
        val hasMore: Boolean,
        val nextPage: Int?,
        val totalPosts: Int
    )
    
    suspend fun getFeed(page: Int = 0, limit: Int = 30): Result<FeedResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "${ApiConfig.Posts.FEED}?page=$page&limit=$limit"
            Log.d("ApiService", "getFeed: Calling $url")
            val request = buildAuthRequest(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            Log.d("ApiService", "getFeed: Response code=${response.code}, body length=${body.length}")
            
            if (response.isSuccessful) {
                // Handle both old array format and new paginated format
                val posts = mutableListOf<JSONObject>()
                var hasMore = false
                var nextPage: Int? = null
                var totalPosts = 0
                
                try {
                    // Try new paginated format first
                    val json = JSONObject(body)
                    if (json.has("posts")) {
                        val postsArray = json.getJSONArray("posts")
                        for (i in 0 until postsArray.length()) {
                            posts.add(postsArray.getJSONObject(i))
                        }
                        val pagination = json.optJSONObject("pagination")
                        hasMore = pagination?.optBoolean("hasMore", false) ?: false
                        nextPage = if (pagination?.isNull("nextPage") == false) pagination.optInt("nextPage") else null
                        totalPosts = pagination?.optInt("totalPosts", posts.size) ?: posts.size
                    }
                } catch (e: Exception) {
                    // Fallback: old array format
                    val array = JSONArray(body)
                    for (i in 0 until array.length()) {
                        posts.add(array.getJSONObject(i))
                    }
                    totalPosts = posts.size
                }
                
                Log.d("ApiService", "getFeed: Parsed ${posts.size} posts, hasMore=$hasMore, total=$totalPosts")
                Result.success(FeedResponse(posts, hasMore, nextPage, totalPosts))
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
    
    suspend fun sharePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest(ApiConfig.Posts.sharePost(postId))
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addComment(postId: String, text: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = org.json.JSONObject().apply {
                put("text", text)
            }
            
            val request = buildAuthRequest(ApiConfig.Posts.addComment(postId))
                .post(json.toString().toRequestBody("application/json".toMediaType()))
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
            
            Log.d("ApiService", "getPresignedUrl: requesting URL for $filename ($contentType) in $folder")
            Log.d("ApiService", "getPresignedUrl: authToken present: ${authToken != null}")
            
            val request = buildAuthRequest(ApiConfig.Upload.PRESIGN)
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            Log.d("ApiService", "getPresignedUrl: response code=${response.code}, body length=${body.length}")
            
            if (response.isSuccessful) {
                val result = JSONObject(body)
                Log.d("ApiService", "getPresignedUrl: success, uploadUrl present: ${result.has("uploadUrl")}")
                Result.success(result)
            } else {
                Log.e("ApiService", "getPresignedUrl: failed with code ${response.code}, body: $body")
                Result.failure(Exception("Failed to get upload URL: ${response.code} - $body"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "getPresignedUrl: exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun uploadToPresignedUrl(presignedUrl: String, data: ByteArray, contentType: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiService", "uploadToPresignedUrl (ByteArray): uploading ${data.size} bytes as $contentType")
            
            val request = Request.Builder()
                .url(presignedUrl)
                .put(data.toRequestBody(contentType.toMediaType()))
                .build()
            
            // Use s3Client without certificate pinning for S3 uploads
            val response = s3Client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e("ApiService", "uploadToPresignedUrl (ByteArray) exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload to presigned URL using streaming (Uri)
     * Prevents OOM for large media files
     */
    suspend fun uploadToPresignedUrl(
        presignedUrl: String, 
        uri: android.net.Uri, 
        contentResolver: android.content.ContentResolver,
        contentType: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            Log.d("ApiService", "uploadToPresignedUrl (Streaming): for $uri as $contentType")
            
            // Get content length
            val pfd = contentResolver.openFileDescriptor(uri, "r")
            val contentLength = pfd?.statSize ?: -1L
            pfd?.close()
            
            val requestBody = object : okhttp3.RequestBody() {
                override fun contentType() = contentType.toMediaType()
                override fun contentLength() = contentLength
                override fun writeTo(sink: okio.BufferedSink) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            sink.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }
            
            val request = Request.Builder()
                .url(presignedUrl)
                .put(requestBody)
                .build()
            
            val response = s3Client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "no body"
                Log.e("ApiService", "uploadToPresignedUrl (Streaming) failed: code=${response.code}, error: $errorBody")
            }
            
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e("ApiService", "uploadToPresignedUrl (Streaming) exception: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Multipart upload helper for large files (>100MB)
     * Splits file into 5MB chunks and uploads sequentially
     * @param onProgress Callback with progress (0.0 to 1.0)
     */
    suspend fun uploadLargeFile(
        uri: android.net.Uri,
        contentResolver: android.content.ContentResolver,
        filename: String,
        contentType: String,
        folder: String = "posts",
        onProgress: ((Float) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val CHUNK_SIZE = 5 * 1024 * 1024 // 5MB chunks
            
            // Get file size
            val pfd = contentResolver.openFileDescriptor(uri, "r")
            val fileSize = pfd?.statSize ?: 0L
            pfd?.close()
            
            if (fileSize <= 0) {
                return@withContext Result.failure(Exception("Empty file"))
            }
            
            val fileSizeMB = (fileSize / 1024 / 1024).toInt()
            Log.d("ApiService", "uploadLargeFile: ${fileSizeMB}MB file, using multipart upload")
            
            onProgress?.invoke(0.05f) // 5% - Starting
            
            // Start multipart upload
            val startResult = startMultipartUpload(filename, contentType, folder)
            if (startResult.isFailure) {
                return@withContext Result.failure(Exception("Failed to start multipart upload"))
            }
            
            val startJson = startResult.getOrThrow()
            val uploadId = startJson.getString("uploadId")
            val key = startJson.getString("key")
            val fileUrl = startJson.getString("fileUrl")
            
            // Calculate chunks
            val partCount = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
            Log.d("ApiService", "Uploading in $partCount chunks")
            
            onProgress?.invoke(0.1f) // 10% - Got upload ID
            
            // Get presigned URLs for all parts
            val urlsResult = getMultipartPresignedUrls(key, uploadId, partCount)
            if (urlsResult.isFailure) {
                abortMultipartUpload(key, uploadId)
                return@withContext Result.failure(Exception("Failed to get part URLs"))
            }
            
            val partUrls = urlsResult.getOrThrow()
            
            onProgress?.invoke(0.15f) // 15% - Got URLs
            
            // Upload chunks sequentially
            val completedParts = mutableListOf<Pair<Int, String>>()
            
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream"))
            
            try {
                inputStream.use { stream ->
                    val buffer = ByteArray(CHUNK_SIZE)
                    
                    for (chunkIndex in 0 until partCount) {
                        val partNumber = chunkIndex + 1
                        
                        // Read a chunk
                        val bytesRead = stream.read(buffer)
                        if (bytesRead <= 0) break
                        
                        // Create chunk data
                        val chunkData = if (bytesRead == CHUNK_SIZE) {
                            buffer
                        } else {
                            buffer.copyOfRange(0, bytesRead)
                        }
                        
                        val partUrl = partUrls.find { it.first == partNumber }?.second
                            ?: throw Exception("Missing URL for part $partNumber")
                        
                        // Upload chunk
                        val uploadResult = uploadPart(partUrl, chunkData)
                        if (uploadResult.isFailure) {
                            throw Exception("Failed to upload part $partNumber")
                        }
                        
                        val etag = uploadResult.getOrThrow()
                        completedParts.add(Pair(partNumber, etag))
                        
                        // Report progress (15% to 95% is upload phase)
                        val uploadProgress = 0.15f + (0.80f * partNumber / partCount)
                        onProgress?.invoke(uploadProgress)
                        
                        Log.d("ApiService", "Uploaded part $partNumber/$partCount (${(uploadProgress * 100).toInt()}%)")
                    }
                }
                
                onProgress?.invoke(0.97f) // 97% - Finalizing
                
                // Complete multipart upload
                val completeResult = completeMultipartUpload(key, uploadId, completedParts)
                if (completeResult.isFailure) {
                    throw Exception("Failed to complete multipart upload")
                }
                
                onProgress?.invoke(1.0f) // 100% - Done
                
                Log.d("ApiService", "Multipart upload complete: $fileUrl")
                Result.success(fileUrl)
                
            } catch (e: Exception) {
                Log.e("ApiService", "Multipart upload failed: ${e.message}")
                abortMultipartUpload(key, uploadId)
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e("ApiService", "uploadLargeFile exception: ${e.message}", e)
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
            
            android.util.Log.d("ApiService", "getBlockedUsers response: code=${response.code}, body=$body")
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val blocked = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    blocked.add(array.getString(i))
                }
                android.util.Log.d("ApiService", "Parsed ${blocked.size} blocked users: $blocked")
                Result.success(blocked)
            } else {
                android.util.Log.e("ApiService", "getBlockedUsers failed: ${response.code} - $body")
                Result.failure(Exception("Failed to get blocked users: ${response.code}"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ApiService", "getBlockedUsers exception: ${e.message}", e)
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
    
    // ========== LIKED POSTS ==========
    
    suspend fun getLikedPostIds(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/posts/liked/list").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val liked = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    liked.add(array.getString(i))
                }
                Result.success(liked)
            } else {
                Result.failure(Exception("Failed to get liked posts"))
            }
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
    
    // ========== GROUP INVITE LINKS ==========
    
    suspend fun getGroupInviteLinks(groupId: String): Result<List<JSONObject>> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/invite-links").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                val array = JSONArray(body)
                val links = mutableListOf<JSONObject>()
                for (i in 0 until array.length()) {
                    links.add(array.getJSONObject(i))
                }
                Result.success(links)
            } else {
                Result.failure(Exception("Failed to get invite links: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createGroupInviteLink(groupId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/invite-links")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to create invite link: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteGroupInviteLink(groupId: String, linkId: String, createNew: Boolean = false): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val url = "${ApiConfig.API_URL}/groups/$groupId/invite-links/$linkId" + 
                if (createNew) "?createNew=true" else ""
            val request = buildAuthRequest(url).delete().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to delete invite link: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun joinGroupViaInvite(inviteCode: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/join/$inviteCode")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                val errorMsg = try { JSONObject(body).optString("error", "Failed to join") } catch (e: Exception) { "Failed to join" }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== GROUP MEMBER MANAGEMENT ==========
    
    suspend fun getGroupMembers(groupId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/members").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to get members: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun promoteMember(groupId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/members/$userId/promote")
                .put("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun demoteMember(groupId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/members/$userId/demote")
                .put("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeGroupMember(groupId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/groups/$groupId/members/$userId")
                .delete()
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
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
    
    // ========== EVENTS ==========
    
    suspend fun getEvents(): Result<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                Result.success(JSONArray(body))
            } else {
                Result.failure(Exception("Failed to get events: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createEvent(
        title: String,
        description: String = "",
        date: String,
        time: String,
        location: String = "",
        category: String = "General",
        isOnline: Boolean = false,
        imageUrl: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("description", description)
                put("date", date)
                put("time", time)
                put("location", location)
                put("category", category)
                put("isOnline", isOnline)
                imageUrl?.let { put("imageUrl", it) }
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/events")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Log.d("ApiService", "Event created successfully: $body")
                Result.success(JSONObject(body))
            } else {
                Log.e("ApiService", "Failed to create event: ${response.code}")
                Result.failure(Exception("Failed to create event: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error creating event: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getEvent(eventId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to get event: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateEvent(
        eventId: String,
        title: String? = null,
        date: String? = null,
        time: String? = null,
        location: String? = null
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                title?.let { put("title", it) }
                date?.let { put("date", it) }
                time?.let { put("time", it) }
                location?.let { put("location", it) }
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId")
                .put(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to update event: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun toggleEventChat(eventId: String, enabled: Boolean): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("chatEnabled", enabled)
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId")
                .put(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getEventMembers(eventId: String): Result<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/members").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                Result.success(JSONArray(body))
            } else {
                Result.failure(Exception("Failed to get event members: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addEventMember(eventId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("userId", userId)
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/members")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun removeEventMember(eventId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/members/$userId")
                .delete()
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun promoteEventMember(eventId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/members/$userId/promote")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun demoteEventMember(eventId: String, userId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/members/$userId/demote")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getEventMessages(eventId: String): Result<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/messages").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                Result.success(JSONArray(body))
            } else {
                Result.failure(Exception("Failed to get event messages: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendEventMessage(eventId: String, content: String, mediaUrl: String? = null): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("content", content)
                mediaUrl?.let { put("mediaUrl", it) }
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/messages")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                val errorMsg = try { JSONObject(body).optString("error", "Failed to send message") } catch (e: Exception) { "Failed to send message" }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getEventInviteLinks(eventId: String): Result<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/invite-links").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                Result.success(JSONArray(body))
            } else {
                Result.failure(Exception("Failed to get event invite links: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createEventInviteLink(eventId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/invite-links")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to create event invite link: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun joinEventViaInvite(inviteCode: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/join/$inviteCode")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                val errorMsg = try { JSONObject(body).optString("error", "Failed to join event") } catch (e: Exception) { "Failed to join event" }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ========== EVENT JOIN/LEAVE (RSVP) ==========
    
    suspend fun joinEvent(eventId: String): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/join")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Log.d("ApiService", "Joined event successfully")
                Result.success(JSONObject(body))
            } else {
                val errorMsg = try { JSONObject(body).optString("error", "Failed to join event") } catch (e: Exception) { "Failed to join event" }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error joining event: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun leaveEvent(eventId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/$eventId/leave")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Log.d("ApiService", "Left event successfully")
            }
            Result.success(response.isSuccessful)
        } catch (e: Exception) {
            Log.e("ApiService", "Error leaving event: ${e.message}")
            Result.failure(e)
        }
    }
    
    // ========== PUBLIC EVENTS DISCOVERY ==========
    
    suspend fun getPublicEvents(): Result<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/public").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                Log.d("ApiService", "Got public events: ${body.length} bytes")
                Result.success(JSONArray(body))
            } else {
                Result.failure(Exception("Failed to get public events: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting public events: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getAllEvents(): Result<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/events/all").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "[]"
            
            if (response.isSuccessful) {
                Log.d("ApiService", "Got all events: ${body.length} bytes")
                Result.success(JSONArray(body))
            } else {
                Result.failure(Exception("Failed to get all events: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting all events: ${e.message}")
            Result.failure(e)
        }
    }
    
    // ========== OTT VIDEO STREAMING ==========
    
    suspend fun getOttVideos(category: String = "All"): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val url = if (category == "All") {
                "${ApiConfig.API_URL}/ott/videos"
            } else {
                "${ApiConfig.API_URL}/ott/videos?category=$category"
            }
            val request = buildAuthRequest(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting OTT videos: ${e.message}")
            null
        }
    }
    
    suspend fun getOttVideo(videoId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/videos/$videoId").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting OTT video: ${e.message}")
            null
        }
    }
    
    suspend fun getOttTrending(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/trending").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting trending OTT: ${e.message}")
            null
        }
    }
    
    suspend fun searchOttVideos(query: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/search?q=$query").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error searching OTT: ${e.message}")
            null
        }
    }
    
    suspend fun getMyOttVideos(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/my-videos").get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error getting my OTT videos: ${e.message}")
            null
        }
    }
    
    suspend fun likeOttVideo(videoId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/videos/$videoId/like")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error liking OTT video: ${e.message}")
            null
        }
    }
    
    suspend fun incrementOttView(videoId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/videos/$videoId/view")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error incrementing OTT view: ${e.message}")
            null
        }
    }
    
    suspend fun uploadOttVideo(
        title: String,
        description: String,
        videoUrl: String,
        thumbnailUrl: String,
        category: String,
        duration: Long
    ): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("description", description)
                put("videoUrl", videoUrl)
                put("thumbnailUrl", thumbnailUrl)
                put("category", category)
                put("duration", duration)
            }
            
            Log.d("ApiService", "uploadOttVideo: sending request with videoUrl=$videoUrl, thumbnailUrl=$thumbnailUrl")
            
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/videos")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
                
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            Log.d("ApiService", "uploadOttVideo: response code=${response.code}, body=$body")
            
            JSONObject(body)
        } catch (e: Exception) {
            Log.e("ApiService", "Error uploading OTT video: ${e.message}", e)
            null
        }
    }
    
    suspend fun deleteOttVideo(videoId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/videos/$videoId")
                .delete()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Log.d("ApiService", "Deleted OTT video: $videoId")
                Result.success(true)
            } else {
                val errorMsg = try { JSONObject(body).optString("message", "Delete failed") } catch (e: Exception) { "Delete failed" }
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Error deleting OTT video: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ========== MULTIPART UPLOAD (Faster for large files) ==========
    
    /**
     * Start multipart upload - returns uploadId and key
     */
    suspend fun startMultipartUpload(
        filename: String,
        contentType: String,
        folder: String = "ott"
    ): Result<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("filename", filename)
                put("contentType", contentType)
                put("folder", folder)
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/upload/multipart/start")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            
            if (response.isSuccessful) {
                Result.success(JSONObject(body))
            } else {
                Result.failure(Exception("Failed to start multipart upload"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Multipart start error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get presigned URLs for all parts at once
     */
    suspend fun getMultipartPresignedUrls(
        key: String,
        uploadId: String,
        partCount: Int
    ): Result<List<Pair<Int, String>>> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("key", key)
                put("uploadId", uploadId)
                put("partCount", partCount)
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/upload/multipart/presign-parts")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val result = JSONObject(body)
            
            if (response.isSuccessful && result.optBoolean("success")) {
                val partsArray = result.getJSONArray("parts")
                val parts = mutableListOf<Pair<Int, String>>()
                for (i in 0 until partsArray.length()) {
                    val part = partsArray.getJSONObject(i)
                    parts.add(Pair(part.getInt("partNumber"), part.getString("url")))
                }
                Result.success(parts)
            } else {
                Result.failure(Exception("Failed to get part URLs"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Multipart presign error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Upload a single part to presigned URL - returns ETag
     */
    suspend fun uploadPart(
        presignedUrl: String,
        data: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(presignedUrl)
                .put(data.toRequestBody())
                .build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val etag = response.header("ETag")?.replace("\"", "") ?: ""
                Result.success(etag)
            } else {
                Result.failure(Exception("Part upload failed: ${response.code}"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Part upload error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Complete multipart upload
     */
    suspend fun completeMultipartUpload(
        key: String,
        uploadId: String,
        parts: List<Pair<Int, String>> // partNumber to ETag
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val partsArray = org.json.JSONArray()
            parts.forEach { (partNumber, etag) ->
                partsArray.put(JSONObject().apply {
                    put("PartNumber", partNumber)
                    put("ETag", etag)
                })
            }
            
            val json = JSONObject().apply {
                put("key", key)
                put("uploadId", uploadId)
                put("parts", partsArray)
            }
            
            val request = buildAuthRequest("${ApiConfig.API_URL}/upload/multipart/complete")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            val result = JSONObject(body)
            
            if (response.isSuccessful && result.optBoolean("success")) {
                Result.success(result.optString("fileUrl"))
            } else {
                Result.failure(Exception("Failed to complete multipart upload"))
            }
        } catch (e: Exception) {
            Log.e("ApiService", "Multipart complete error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Abort multipart upload (cleanup on error)
     */
    suspend fun abortMultipartUpload(key: String, uploadId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("key", key)
                put("uploadId", uploadId)
            }
            val request = buildAuthRequest("${ApiConfig.API_URL}/upload/multipart/abort")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("ApiService", "Multipart abort error: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // =====================================================================
    // OTT Profile APIs - Saved Videos & Watch History
    // =====================================================================
    
    /**
     * Get user's saved videos
     */
    suspend fun getSavedVideos(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/saved")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            if (response.isSuccessful) JSONObject(body) else null
        } catch (e: Exception) {
            Log.e("ApiService", "Get saved videos error: ${e.message}", e)
            null
        }
    }
    
    /**
     * Toggle save/unsave a video
     */
    suspend fun toggleSaveVideo(videoId: String): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/videos/$videoId/save")
                .post("{}".toRequestBody("application/json".toMediaType()))
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            if (response.isSuccessful) JSONObject(body) else null
        } catch (e: Exception) {
            Log.e("ApiService", "Toggle save video error: ${e.message}", e)
            null
        }
    }
    
    /**
     * Get user's watch history
     */
    suspend fun getWatchHistory(): JSONObject? = withContext(Dispatchers.IO) {
        try {
            val request = buildAuthRequest("${ApiConfig.API_URL}/ott/history")
                .get()
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: "{}"
            if (response.isSuccessful) JSONObject(body) else null
        } catch (e: Exception) {
            Log.e("ApiService", "Get watch history error: ${e.message}", e)
            null
        }
    }
}

