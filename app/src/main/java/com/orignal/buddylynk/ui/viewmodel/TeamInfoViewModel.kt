package com.orignal.buddylynk.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orignal.buddylynk.data.api.ApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * ViewModel for Team Info, Invite Links, and Members management
 */
class TeamInfoViewModel : ViewModel() {
    
    private val TAG = "TeamInfoViewModel"
    
    // Current group ID
    private var currentGroupId: String = ""
    
    // Invite Links
    data class InviteLink(
        val linkId: String,
        val code: String,
        val url: String,
        val createdAt: String,
        val createdBy: String,
        val isActive: Boolean
    )
    
    private val _inviteLinks = MutableStateFlow<List<InviteLink>>(emptyList())
    val inviteLinks: StateFlow<List<InviteLink>> = _inviteLinks.asStateFlow()
    
    // Members
    data class Member(
        val userId: String,
        val username: String,
        val avatar: String?,
        val role: String, // "owner", "admin", "subscriber"
        val joinedAt: String?
    )
    
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()
    
    private val _adminCount = MutableStateFlow(0)
    val adminCount: StateFlow<Int> = _adminCount.asStateFlow()
    
    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Notifications
    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load group info including invite links and members
     */
    fun loadGroupInfo(groupId: String) {
        currentGroupId = groupId
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load invite links
                loadInviteLinks(groupId)
                
                // Load members
                loadMembers(groupId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading group info", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load invite links for a group
     */
    private suspend fun loadInviteLinks(groupId: String) {
        try {
            val result = ApiService.getGroupInviteLinks(groupId)
            if (result.isSuccess) {
                val links = result.getOrNull()?.map { json ->
                    InviteLink(
                        linkId = json.optString("linkId", ""),
                        code = json.optString("code", ""),
                        url = json.optString("url", ""),
                        createdAt = json.optString("createdAt", ""),
                        createdBy = json.optString("createdBy", ""),
                        isActive = json.optBoolean("isActive", true)
                    )
                } ?: emptyList()
                _inviteLinks.value = links
                Log.d(TAG, "Loaded ${links.size} invite links")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading invite links", e)
        }
    }
    
    /**
     * Load members for a group
     */
    private suspend fun loadMembers(groupId: String) {
        try {
            val result = ApiService.getGroupMembers(groupId)
            if (result.isSuccess) {
                val json = result.getOrNull()
                if (json != null) {
                    _totalCount.value = json.optInt("totalCount", 0)
                    _adminCount.value = json.optInt("adminCount", 0)
                    
                    val membersArray = json.optJSONArray("members")
                    val membersList = mutableListOf<Member>()
                    if (membersArray != null) {
                        for (i in 0 until membersArray.length()) {
                            val memberJson = membersArray.getJSONObject(i)
                            membersList.add(Member(
                                userId = memberJson.optString("userId", ""),
                                username = memberJson.optString("username", "Unknown"),
                                avatar = memberJson.optString("avatar", null),
                                role = memberJson.optString("role", "subscriber"),
                                joinedAt = memberJson.optString("joinedAt", null)
                            ))
                        }
                    }
                    _members.value = membersList
                    Log.d(TAG, "Loaded ${membersList.size} members")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading members", e)
        }
    }
    
    /**
     * Create a new invite link
     */
    fun createInviteLink() {
        if (currentGroupId.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ApiService.createGroupInviteLink(currentGroupId)
                if (result.isSuccess) {
                    val json = result.getOrNull()
                    if (json != null) {
                        val newLink = InviteLink(
                            linkId = json.optString("linkId", ""),
                            code = json.optString("code", ""),
                            url = json.optString("url", ""),
                            createdAt = json.optString("createdAt", ""),
                            createdBy = json.optString("createdBy", ""),
                            isActive = true
                        )
                        _inviteLinks.value = _inviteLinks.value + newLink
                        Log.d(TAG, "Created new invite link: ${newLink.url}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating invite link", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete/revoke an invite link and optionally create a new one
     */
    fun deleteInviteLink(linkId: String, createNew: Boolean = false) {
        if (currentGroupId.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ApiService.deleteGroupInviteLink(currentGroupId, linkId, createNew)
                if (result.isSuccess) {
                    val json = result.getOrNull()
                    
                    // Remove the old link
                    _inviteLinks.value = _inviteLinks.value.filter { it.linkId != linkId }
                    
                    // If a new link was created, add it
                    if (createNew && json != null) {
                        val newLinkJson = json.optJSONObject("newLink")
                        if (newLinkJson != null) {
                            val newLink = InviteLink(
                                linkId = newLinkJson.optString("linkId", ""),
                                code = newLinkJson.optString("code", ""),
                                url = newLinkJson.optString("url", ""),
                                createdAt = newLinkJson.optString("createdAt", ""),
                                createdBy = newLinkJson.optString("createdBy", ""),
                                isActive = true
                            )
                            _inviteLinks.value = _inviteLinks.value + newLink
                        }
                    }
                    
                    Log.d(TAG, "Deleted invite link: $linkId, createNew: $createNew")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting invite link", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Promote a member to admin
     */
    fun promoteMember(userId: String) {
        if (currentGroupId.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ApiService.promoteMember(currentGroupId, userId)
                if (result.isSuccess && result.getOrNull() == true) {
                    // Update local state
                    _members.value = _members.value.map { member ->
                        if (member.userId == userId) {
                            member.copy(role = "admin")
                        } else member
                    }
                    _adminCount.value = _members.value.count { it.role == "admin" || it.role == "owner" }
                    Log.d(TAG, "Promoted member: $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error promoting member", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Demote an admin to subscriber
     */
    fun demoteMember(userId: String) {
        if (currentGroupId.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ApiService.demoteMember(currentGroupId, userId)
                if (result.isSuccess && result.getOrNull() == true) {
                    _members.value = _members.value.map { member ->
                        if (member.userId == userId) {
                            member.copy(role = "subscriber")
                        } else member
                    }
                    _adminCount.value = _members.value.count { it.role == "admin" || it.role == "owner" }
                    Log.d(TAG, "Demoted member: $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error demoting member", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Remove a member from the group
     */
    fun removeMember(userId: String) {
        if (currentGroupId.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ApiService.removeGroupMember(currentGroupId, userId)
                if (result.isSuccess && result.getOrNull() == true) {
                    _members.value = _members.value.filter { it.userId != userId }
                    _totalCount.value = _members.value.size
                    _adminCount.value = _members.value.count { it.role == "admin" || it.role == "owner" }
                    Log.d(TAG, "Removed member: $userId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing member", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Toggle notifications
     */
    fun toggleNotifications() {
        _notificationsEnabled.value = !_notificationsEnabled.value
    }
    
    /**
     * Get subscribers (non-admin members)
     */
    fun getSubscribers(): List<Member> {
        return _members.value.filter { it.role == "subscriber" }
    }
    
    /**
     * Get administrators (admins and owner)
     */
    fun getAdministrators(): List<Member> {
        return _members.value.filter { it.role == "admin" || it.role == "owner" }
    }
    
    /**
     * Refresh all data
     */
    fun refresh() {
        if (currentGroupId.isNotEmpty()) {
            loadGroupInfo(currentGroupId)
        }
    }
}
