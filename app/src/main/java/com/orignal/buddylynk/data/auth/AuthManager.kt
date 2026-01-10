package com.orignal.buddylynk.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Authentication Manager - Handles login state and session persistence
 * SECURITY: Uses EncryptedSharedPreferences for JWT token storage
 */
object AuthManager {
    
    private const val PREFS_NAME = "buddylynk_auth"
    private const val SECURE_PREFS_NAME = "buddylynk_secure_auth"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USERNAME = "username"
    private const val KEY_EMAIL = "email"
    private const val KEY_AVATAR = "avatar"
    private const val KEY_BANNER = "banner"
    private const val KEY_BIO = "bio"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_JWT_TOKEN = "jwt_token"
    
    private var prefs: SharedPreferences? = null
    private var securePrefs: SharedPreferences? = null  // For sensitive data like JWT
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()
    
    /**
     * Initialize AuthManager with context - call from Application or MainActivity
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // SECURITY: Use EncryptedSharedPreferences for JWT token
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            securePrefs = EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback to regular prefs if encryption fails (older devices)
            securePrefs = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)
        }
        
        loadSavedSession()
    }
    
    /**
     * Load saved session on app start
     */
    private fun loadSavedSession() {
        prefs?.let { p ->
            val isLoggedIn = p.getBoolean(KEY_IS_LOGGED_IN, false)
            android.util.Log.d("AuthManager", "loadSavedSession: isLoggedIn=$isLoggedIn")
            
            if (isLoggedIn) {
                val userId = p.getString(KEY_USER_ID, null)
                val username = p.getString(KEY_USERNAME, null)
                val email = p.getString(KEY_EMAIL, null)
                val avatar = p.getString(KEY_AVATAR, null)
                val banner = p.getString(KEY_BANNER, null)
                val bio = p.getString(KEY_BIO, null)
                val jwtToken = securePrefs?.getString(KEY_JWT_TOKEN, null)
                
                android.util.Log.d("AuthManager", "loadSavedSession: userId=$userId, username=$username, email=$email, avatar=${avatar?.take(50)}, token=${jwtToken?.take(20)}...")
                
                // Set token first so API calls work
                jwtToken?.let { ApiService.setAuthToken(it) }
                
                // Load user if we have at least userId
                if (userId != null) {
                    _currentUser.value = User(
                        userId = userId,
                        username = username ?: email?.substringBefore("@") ?: "User",
                        email = email ?: "",
                        avatar = avatar,
                        banner = banner,
                        bio = bio
                    )
                    _isLoggedIn.value = true
                    android.util.Log.d("AuthManager", "loadSavedSession: User loaded - ${_currentUser.value?.username}")
                } else if (jwtToken != null) {
                    // We have a token but no userId - mark as logged in, will refresh from API
                    _isLoggedIn.value = true
                    android.util.Log.d("AuthManager", "loadSavedSession: Token found but no userId, will refresh from API")
                }
            }
        }
    }
    
    /**
     * Save login session (for API login with JWT token)
     */
    fun login(user: User, jwtToken: String? = null) {
        prefs?.edit()?.apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, user.userId)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_AVATAR, user.avatar)
            putString(KEY_BANNER, user.banner)
            putString(KEY_BIO, user.bio)
            apply()
        }
        
        // SECURITY: Store JWT token in encrypted storage
        jwtToken?.let { token ->
            securePrefs?.edit()?.putString(KEY_JWT_TOKEN, token)?.apply()
            ApiService.setAuthToken(token)
        }
        
        _currentUser.value = user
        _isLoggedIn.value = true
    }
    
    /**
     * Logout and clear session
     */
    fun logout() {
        prefs?.edit()?.clear()?.apply()
        securePrefs?.edit()?.clear()?.apply()  // Clear encrypted JWT token
        _currentUser.value = null
        _isLoggedIn.value = false
        ApiService.setAuthToken(null)
    }
    
    /**
     * Check if user is logged in
     */
    fun isUserLoggedIn(): Boolean = _isLoggedIn.value
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String? = _currentUser.value?.userId
    
    /**
     * Update current user (for profile edits)
     */
    fun updateCurrentUser(user: User) {
        prefs?.edit()?.apply {
            putString(KEY_USER_ID, user.userId)
            putString(KEY_USERNAME, user.username)
            putString(KEY_EMAIL, user.email)
            putString(KEY_AVATAR, user.avatar)
            putString(KEY_BANNER, user.banner)
            putString(KEY_BIO, user.bio)
            apply()
        }
        _currentUser.value = user
    }
    
    /**
     * Refresh current user data from backend API (to get latest profile info)
     */
    suspend fun refreshCurrentUser() {
        android.util.Log.d("AuthManager", "refreshCurrentUser called - currentUser: ${_currentUser.value?.username}, token: ${ApiService.getAuthToken()?.take(20)}...")
        
        try {
            val userId = _currentUser.value?.userId
            val currentUsername = _currentUser.value?.username
            
            // Check if we need to refresh (no user, or username is placeholder)
            val needsFullRefresh = userId == null || currentUsername.isNullOrEmpty() || currentUsername == "User"
            
            if (userId != null && !needsFullRefresh) {
                // Have valid userId and username, just refresh to get latest
                android.util.Log.d("AuthManager", "Refreshing user by userId: $userId")
                val freshUser = com.orignal.buddylynk.data.repository.BackendRepository.getUser(userId)
                if (freshUser != null) {
                    _currentUser.value = freshUser
                    updateCurrentUser(freshUser)
                    android.util.Log.d("AuthManager", "Refreshed user: ${freshUser.username}, avatar: ${freshUser.avatar}")
                }
            } else if (ApiService.getAuthToken() != null) {
                // Need full refresh from /users/me
                android.util.Log.d("AuthManager", "Getting current user from /users/me API")
                val result = ApiService.getCurrentUser()
                android.util.Log.d("AuthManager", "getCurrentUser result: ${result.isSuccess}")
                
                result.getOrNull()?.let { json ->
                    android.util.Log.d("AuthManager", "Parsing user JSON: ${json.toString().take(200)}...")
                    val user = parseUserFromJson(json)
                    if (user != null && user.userId.isNotEmpty()) {
                        _currentUser.value = user
                        _isLoggedIn.value = true
                        updateCurrentUser(user)
                        android.util.Log.d("AuthManager", "Got current user from API: ${user.username}, avatar: ${user.avatar}")
                    } else {
                        android.util.Log.e("AuthManager", "Parsed user is null or has empty userId")
                    }
                } ?: run {
                    android.util.Log.e("AuthManager", "getCurrentUser returned null result")
                }
            } else {
                android.util.Log.d("AuthManager", "No token available, skipping refresh")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthManager", "Failed to refresh user: ${e.message}", e)
        }
    }
    
    private fun parseUserFromJson(json: org.json.JSONObject): com.orignal.buddylynk.data.model.User? {
        return try {
            // Handle avatar - check multiple possible field names
            val avatarRaw = json.optString("avatar", "").ifEmpty { 
                json.optString("profileImage", "").ifEmpty { 
                    json.optString("avatarUrl", "") 
                } 
            }
            val avatar = avatarRaw.takeIf { it.isNotBlank() }
            
            // Handle username
            val username = json.optString("username", "").ifEmpty { 
                json.optString("displayName", "").ifEmpty { 
                    json.optString("name", "") 
                } 
            }
            
            com.orignal.buddylynk.data.model.User(
                userId = json.optString("userId", json.optString("_id", "")),
                email = json.optString("email", ""),
                username = username,
                avatar = avatar,
                bio = json.optString("bio", "").takeIf { it.isNotBlank() },
                followersCount = json.optInt("followerCount", json.optInt("followersCount", 0)),
                followingCount = json.optInt("followingCount", 0),
                postsCount = json.optInt("postsCount", 0),
                createdAt = json.optString("createdAt", "")
            )
        } catch (e: Exception) {
            null
        }
    }
}
