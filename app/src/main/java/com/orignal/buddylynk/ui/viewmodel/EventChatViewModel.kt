package com.orignal.buddylynk.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.data.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * ViewModel for Event Chat functionality
 * Handles messages, member management, and chat permissions
 */
class EventChatViewModel : ViewModel() {

    companion object {
        private const val TAG = "EventChatViewModel"
    }

    // Event data
    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event.asStateFlow()

    // Members list
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    // Chat messages
    private val _messages = MutableStateFlow<List<EventMessage>>(emptyList())
    val messages: StateFlow<List<EventMessage>> = _messages.asStateFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Can the current user send messages?
    private val _canSendMessages = MutableStateFlow(true)
    val canSendMessages: StateFlow<Boolean> = _canSendMessages.asStateFlow()

    // Current user role
    private val _currentUserRole = MutableStateFlow("member")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    // Invite links
    private val _inviteLinks = MutableStateFlow<List<InviteLink>>(emptyList())
    val inviteLinks: StateFlow<List<InviteLink>> = _inviteLinks.asStateFlow()

    /**
     * Load event details
     */
    fun loadEvent(eventId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = ApiService.getEvent(eventId)
                result.onSuccess { eventJson ->
                    val event = parseEvent(eventJson)
                    _event.value = event
                    updateCanSendMessages(event)
                    updateCurrentUserRole(event)
                    Log.d(TAG, "Loaded event: ${event.title}")
                }.onFailure {
                    Log.e(TAG, "Failed to load event: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading event: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load event members
     */
    fun loadMembers(eventId: String) {
        viewModelScope.launch {
            try {
                val result = ApiService.getEventMembers(eventId)
                result.onSuccess { membersJson ->
                    _members.value = parseMembersList(membersJson)
                    Log.d(TAG, "Loaded ${_members.value.size} members")
                }.onFailure {
                    Log.e(TAG, "Failed to load members: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading members: ${e.message}")
            }
        }
    }

    /**
     * Load chat messages
     */
    fun loadMessages(eventId: String) {
        viewModelScope.launch {
            try {
                val result = ApiService.getEventMessages(eventId)
                result.onSuccess { messagesJson ->
                    _messages.value = parseMessagesList(messagesJson)
                    Log.d(TAG, "Loaded ${_messages.value.size} messages")
                }.onFailure {
                    Log.e(TAG, "Failed to load messages: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading messages: ${e.message}")
            }
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(eventId: String, content: String) {
        if (!_canSendMessages.value || content.isBlank()) return

        viewModelScope.launch {
            try {
                val result = ApiService.sendEventMessage(eventId, content)
                result.onSuccess { messageJson ->
                    val newMessage = parseMessage(messageJson)
                    _messages.value = _messages.value + newMessage
                    Log.d(TAG, "Message sent successfully")
                }.onFailure {
                    Log.e(TAG, "Failed to send message: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message: ${e.message}")
            }
        }
    }

    /**
     * Send a message with media attachment
     */
    fun sendMessageWithMedia(eventId: String, content: String, mediaUrl: String, mediaType: String) {
        if (!_canSendMessages.value) return

        viewModelScope.launch {
            try {
                val result = ApiService.sendEventMessage(eventId, content, mediaUrl)
                result.onSuccess { messageJson ->
                    val newMessage = parseMessage(messageJson)
                    _messages.value = _messages.value + newMessage
                    Log.d(TAG, "Media message sent successfully")
                }.onFailure {
                    Log.e(TAG, "Failed to send media message: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending media message: ${e.message}")
            }
        }
    }

    /**
     * Update event details
     */
    fun updateEvent(
        eventId: String,
        title: String? = null,
        date: String? = null,
        time: String? = null,
        location: String? = null
    ) {
        viewModelScope.launch {
            try {
                val result = ApiService.updateEvent(eventId, title, date, time, location)
                result.onSuccess { eventJson ->
                    val event = parseEvent(eventJson)
                    _event.value = event
                    Log.d(TAG, "Event updated successfully")
                }.onFailure {
                    Log.e(TAG, "Failed to update event: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating event: ${e.message}")
            }
        }
    }

    /**
     * Toggle chat enabled/disabled
     */
    fun toggleChatEnabled(eventId: String) {
        viewModelScope.launch {
            val currentEvent = _event.value ?: return@launch
            val newEnabled = !currentEvent.chatEnabled

            try {
                val result = ApiService.toggleEventChat(eventId, newEnabled)
                result.onSuccess {
                    _event.value = currentEvent.copy(chatEnabled = newEnabled)
                    updateCanSendMessages(_event.value!!)
                    Log.d(TAG, "Chat ${if (newEnabled) "enabled" else "disabled"}")
                }.onFailure {
                    Log.e(TAG, "Failed to toggle chat: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling chat: ${e.message}")
            }
        }
    }

    /**
     * Promote a member to admin
     */
    fun promoteMember(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = ApiService.promoteEventMember(eventId, userId)
                result.onSuccess {
                    // Update local state
                    _members.value = _members.value.map { member ->
                        if (member.userId == userId) member.copy(role = "admin") else member
                    }
                    Log.d(TAG, "Member promoted to admin")
                }.onFailure {
                    Log.e(TAG, "Failed to promote member: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error promoting member: ${e.message}")
            }
        }
    }

    /**
     * Demote an admin to member
     */
    fun demoteMember(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = ApiService.demoteEventMember(eventId, userId)
                result.onSuccess {
                    _members.value = _members.value.map { member ->
                        if (member.userId == userId) member.copy(role = "member") else member
                    }
                    Log.d(TAG, "Admin demoted to member")
                }.onFailure {
                    Log.e(TAG, "Failed to demote member: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error demoting member: ${e.message}")
            }
        }
    }

    /**
     * Remove a member from the event
     */
    fun removeMember(eventId: String, userId: String) {
        viewModelScope.launch {
            try {
                val result = ApiService.removeEventMember(eventId, userId)
                result.onSuccess {
                    _members.value = _members.value.filter { it.userId != userId }
                    Log.d(TAG, "Member removed")
                }.onFailure {
                    Log.e(TAG, "Failed to remove member: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing member: ${e.message}")
            }
        }
    }

    /**
     * Load invite links
     */
    fun loadInviteLinks(eventId: String) {
        viewModelScope.launch {
            try {
                val result = ApiService.getEventInviteLinks(eventId)
                result.onSuccess { linksJson ->
                    _inviteLinks.value = parseInviteLinksList(linksJson)
                    Log.d(TAG, "Loaded ${_inviteLinks.value.size} invite links")
                }.onFailure {
                    Log.e(TAG, "Failed to load invite links: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading invite links: ${e.message}")
            }
        }
    }

    /**
     * Create new invite link
     */
    fun createInviteLink(eventId: String) {
        viewModelScope.launch {
            try {
                val result = ApiService.createEventInviteLink(eventId)
                result.onSuccess { linkJson ->
                    val newLink = parseInviteLink(linkJson)
                    _inviteLinks.value = _inviteLinks.value + newLink
                    Log.d(TAG, "Invite link created: ${newLink.url}")
                }.onFailure {
                    Log.e(TAG, "Failed to create invite link: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating invite link: ${e.message}")
            }
        }
    }

    // Helper functions

    private fun updateCanSendMessages(event: Event) {
        val currentUserId = AuthManager.currentUser.value?.userId ?: ""
        val isOwner = event.organizerId == currentUserId
        val isAdmin = event.adminIds.contains(currentUserId)

        _canSendMessages.value = event.chatEnabled || isOwner || isAdmin
    }

    private fun updateCurrentUserRole(event: Event) {
        val currentUserId = AuthManager.currentUser.value?.userId ?: ""
        _currentUserRole.value = when {
            event.organizerId == currentUserId -> "owner"
            event.adminIds.contains(currentUserId) -> "admin"
            else -> "member"
        }
    }

    private fun parseEvent(json: JSONObject): Event {
        return Event(
            eventId = json.optString("eventId"),
            title = json.optString("title"),
            description = json.optString("description", ""),
            date = json.optString("date"),
            time = json.optString("time"),
            location = json.optString("location", ""),
            isOnline = json.optBoolean("isOnline", false),
            attendeesCount = json.optInt("attendeesCount", 0),
            organizerId = json.optString("organizerId"),
            createdAt = json.optString("createdAt"),
            category = json.optString("category", "General"),
            imageUrl = json.optString("imageUrl", null),
            chatEnabled = json.optBoolean("chatEnabled", true),
            memberIds = parseStringList(json.optJSONArray("memberIds")),
            adminIds = parseStringList(json.optJSONArray("adminIds"))
        )
    }

    private fun parseStringList(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }

    private fun parseMembersList(array: JSONArray): List<Member> {
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Member(
                userId = obj.optString("userId"),
                username = obj.optString("username", "User"),
                avatar = obj.optString("avatar", null),
                fullName = obj.optString("fullName", ""),
                role = obj.optString("role", "member")
            )
        }
    }

    private fun parseMessagesList(array: JSONArray): List<EventMessage> {
        return (0 until array.length()).map { i ->
            parseMessage(array.getJSONObject(i))
        }
    }

    private fun parseMessage(obj: JSONObject): EventMessage {
        return EventMessage(
            messageId = obj.optString("messageId"),
            content = obj.optString("content"),
            senderId = obj.optString("senderId"),
            senderName = obj.optString("senderName", "User"),
            senderAvatar = obj.optString("senderAvatar", null),
            createdAt = obj.optString("createdAt"),
            mediaUrl = obj.optString("mediaUrl", null)
        )
    }

    private fun parseInviteLinksList(array: JSONArray): List<InviteLink> {
        return (0 until array.length()).map { i ->
            parseInviteLink(array.getJSONObject(i))
        }
    }

    private fun parseInviteLink(obj: JSONObject): InviteLink {
        return InviteLink(
            linkId = obj.optString("linkId"),
            code = obj.optString("code"),
            url = obj.optString("url"),
            createdAt = obj.optString("createdAt"),
            isActive = obj.optBoolean("isActive", true)
        )
    }

    // Data classes
    data class Member(
        val userId: String,
        val username: String,
        val avatar: String?,
        val fullName: String,
        val role: String // "owner", "admin", "member"
    )

    data class EventMessage(
        val messageId: String,
        val content: String,
        val senderId: String,
        val senderName: String,
        val senderAvatar: String?,
        val createdAt: String,
        val mediaUrl: String?
    )

    data class InviteLink(
        val linkId: String,
        val code: String,
        val url: String,
        val createdAt: String,
        val isActive: Boolean
    )
}
