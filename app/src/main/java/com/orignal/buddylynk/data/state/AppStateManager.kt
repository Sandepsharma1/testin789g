package com.orignal.buddylynk.data.state

import android.content.Context
import android.content.SharedPreferences
import com.orignal.buddylynk.BuddyLynkApplication

/**
 * AppStateManager - Centralized state persistence manager
 * 
 * Saves and restores UI state for screens that need persistence:
 * - TeamUp: Selected team, scroll position
 * - Chat: Selected conversation, scroll position
 * - Home: Temporary state (cleared on refresh)
 */
object AppStateManager {
    
    private const val PREFS_NAME = "buddylynk_app_state"
    
    // TeamUp state keys
    private const val KEY_TEAMUP_SELECTED_TEAM = "teamup_selected_team"
    private const val KEY_TEAMUP_SCROLL_INDEX = "teamup_scroll_index"
    private const val KEY_TEAMUP_SCROLL_OFFSET = "teamup_scroll_offset"
    private const val KEY_TEAMUP_INNER_VIEW_OPEN = "teamup_inner_view_open"
    
    // Chat state keys
    private const val KEY_CHAT_SCROLL_INDEX = "chat_scroll_index"
    private const val KEY_CHAT_SCROLL_OFFSET = "chat_scroll_offset"
    private const val KEY_CHAT_SELECTED_CONVERSATION = "chat_selected_conversation"
    private const val KEY_CHAT_DRAFT_MESSAGE = "chat_draft_message"
    
    // Home state keys (temporary - cleared on refresh)
    private const val KEY_HOME_LAST_REFRESH_TIME = "home_last_refresh_time"
    
    private val prefs: SharedPreferences by lazy {
        BuddyLynkApplication.appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // =========================================================================
    // TeamUp State
    // =========================================================================
    
    data class TeamUpState(
        val selectedTeamId: String?,
        val scrollIndex: Int,
        val scrollOffset: Int,
        val isInnerViewOpen: Boolean
    )
    
    fun saveTeamUpState(
        selectedTeamId: String?,
        scrollIndex: Int,
        scrollOffset: Int,
        isInnerViewOpen: Boolean
    ) {
        prefs.edit().apply {
            putString(KEY_TEAMUP_SELECTED_TEAM, selectedTeamId)
            putInt(KEY_TEAMUP_SCROLL_INDEX, scrollIndex)
            putInt(KEY_TEAMUP_SCROLL_OFFSET, scrollOffset)
            putBoolean(KEY_TEAMUP_INNER_VIEW_OPEN, isInnerViewOpen)
            apply()
        }
    }
    
    fun getTeamUpState(): TeamUpState {
        return TeamUpState(
            selectedTeamId = prefs.getString(KEY_TEAMUP_SELECTED_TEAM, null),
            scrollIndex = prefs.getInt(KEY_TEAMUP_SCROLL_INDEX, 0),
            scrollOffset = prefs.getInt(KEY_TEAMUP_SCROLL_OFFSET, 0),
            isInnerViewOpen = prefs.getBoolean(KEY_TEAMUP_INNER_VIEW_OPEN, false)
        )
    }
    
    fun clearTeamUpState() {
        prefs.edit().apply {
            remove(KEY_TEAMUP_SELECTED_TEAM)
            remove(KEY_TEAMUP_SCROLL_INDEX)
            remove(KEY_TEAMUP_SCROLL_OFFSET)
            remove(KEY_TEAMUP_INNER_VIEW_OPEN)
            apply()
        }
    }
    
    // =========================================================================
    // Chat State
    // =========================================================================
    
    data class ChatState(
        val scrollIndex: Int,
        val scrollOffset: Int,
        val selectedConversationId: String?,
        val draftMessage: String?
    )
    
    fun saveChatState(
        scrollIndex: Int,
        scrollOffset: Int,
        selectedConversationId: String? = null,
        draftMessage: String? = null
    ) {
        prefs.edit().apply {
            putInt(KEY_CHAT_SCROLL_INDEX, scrollIndex)
            putInt(KEY_CHAT_SCROLL_OFFSET, scrollOffset)
            putString(KEY_CHAT_SELECTED_CONVERSATION, selectedConversationId)
            putString(KEY_CHAT_DRAFT_MESSAGE, draftMessage)
            apply()
        }
    }
    
    fun getChatState(): ChatState {
        return ChatState(
            scrollIndex = prefs.getInt(KEY_CHAT_SCROLL_INDEX, 0),
            scrollOffset = prefs.getInt(KEY_CHAT_SCROLL_OFFSET, 0),
            selectedConversationId = prefs.getString(KEY_CHAT_SELECTED_CONVERSATION, null),
            draftMessage = prefs.getString(KEY_CHAT_DRAFT_MESSAGE, null)
        )
    }
    
    fun saveChatDraft(conversationId: String, message: String) {
        prefs.edit().putString("chat_draft_$conversationId", message).apply()
    }
    
    fun getChatDraft(conversationId: String): String? {
        return prefs.getString("chat_draft_$conversationId", null)
    }
    
    fun clearChatDraft(conversationId: String) {
        prefs.edit().remove("chat_draft_$conversationId").apply()
    }
    
    fun clearChatState() {
        prefs.edit().apply {
            remove(KEY_CHAT_SCROLL_INDEX)
            remove(KEY_CHAT_SCROLL_OFFSET)
            remove(KEY_CHAT_SELECTED_CONVERSATION)
            remove(KEY_CHAT_DRAFT_MESSAGE)
            apply()
        }
    }
    
    // =========================================================================
    // Home State (Temporary - cleared on refresh)
    // =========================================================================
    
    fun saveHomeRefreshTime() {
        prefs.edit().putLong(KEY_HOME_LAST_REFRESH_TIME, System.currentTimeMillis()).apply()
    }
    
    fun getLastHomeRefreshTime(): Long {
        return prefs.getLong(KEY_HOME_LAST_REFRESH_TIME, 0L)
    }
    
    fun clearHomeCache() {
        prefs.edit().apply {
            remove(KEY_HOME_LAST_REFRESH_TIME)
            apply()
        }
    }
    
    // =========================================================================
    // Clear All State (for logout)
    // =========================================================================
    
    fun clearAllState() {
        prefs.edit().clear().apply()
    }
}
