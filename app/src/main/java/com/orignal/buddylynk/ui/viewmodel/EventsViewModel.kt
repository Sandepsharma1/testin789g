package com.orignal.buddylynk.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.data.model.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

class EventsViewModel : ViewModel() {

    companion object {
        private const val TAG = "EventsViewModel"
    }

    // My events (created or joined)
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events.asStateFlow()

    // Discover events (all public events)
    private val _discoverEvents = MutableStateFlow<List<Event>>(emptyList())
    val discoverEvents: StateFlow<List<Event>> = _discoverEvents.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    // Current tab: "my_events" or "discover"
    private val _selectedTab = MutableStateFlow("my_events")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    init {
        Log.d(TAG, "ViewModel initialized, loading events...")
        loadEvents()
    }

    fun setTab(tab: String) {
        _selectedTab.value = tab
        if (tab == "discover" && _discoverEvents.value.isEmpty()) {
            loadDiscoverEvents()
        }
    }

    fun loadEvents() {
        viewModelScope.launch {
            Log.d(TAG, "=== loadEvents() START ===")
            _isLoading.value = true
            try {
                val result = ApiService.getEvents()
                result.onSuccess { eventsJson ->
                    val eventsList = parseEventsList(eventsJson)
                    Log.d(TAG, "Loaded ${eventsList.size} my events")
                    _events.value = eventsList
                }.onFailure { error ->
                    Log.e(TAG, "API FAILURE: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "EXCEPTION in loadEvents: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadDiscoverEvents() {
        viewModelScope.launch {
            Log.d(TAG, "=== loadDiscoverEvents() START ===")
            _isLoading.value = true
            try {
                val result = ApiService.getAllEvents()
                result.onSuccess { eventsJson ->
                    val eventsList = parseEventsList(eventsJson)
                    Log.d(TAG, "Loaded ${eventsList.size} discover events")
                    _discoverEvents.value = eventsList
                }.onFailure { error ->
                    Log.e(TAG, "Failed to load discover events: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading discover events: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
        viewModelScope.launch {
            try {
                val result = ApiService.getEvents()
                result.onSuccess { eventsJson ->
                    val allEvents = parseEventsList(eventsJson)
                    _events.value = when(filter) {
                        "All" -> allEvents
                        "Online" -> allEvents.filter { it.isOnline }
                        else -> allEvents
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error filtering events: ${e.message}")
            }
        }
    }

    fun joinEvent(eventId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Joining event: $eventId")
                val result = ApiService.joinEvent(eventId)
                result.onSuccess { response ->
                    Log.d(TAG, "Joined event successfully")
                    // Refresh both lists
                    loadEvents()
                    loadDiscoverEvents()
                }.onFailure { error ->
                    Log.e(TAG, "Failed to join event: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error joining event: ${e.message}")
            }
        }
    }

    fun leaveEvent(eventId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Leaving event: $eventId")
                val result = ApiService.leaveEvent(eventId)
                result.onSuccess { success ->
                    if (success) {
                        Log.d(TAG, "Left event successfully")
                        // Refresh both lists
                        loadEvents()
                        loadDiscoverEvents()
                    }
                }.onFailure { error ->
                    Log.e(TAG, "Failed to leave event: ${error.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error leaving event: ${e.message}")
            }
        }
    }

    fun createEvent(title: String, date: String, time: String, location: String = "Online") {
        Log.d(TAG, "createEvent() called: $title, $date, $time, $location")
        viewModelScope.launch {
            try {
                val result = ApiService.createEvent(
                    title = title,
                    date = date,
                    time = time,
                    location = location
                )
                result.onSuccess { eventJson ->
                    Log.d(TAG, "Event created successfully: ${eventJson.optString("eventId")}")
                    loadEvents()
                }.onFailure {
                    Log.e(TAG, "Failed to create event: ${it.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating event: ${e.message}")
            }
        }
    }

    private fun parseEventsList(array: JSONArray): List<Event> {
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            Event(
                eventId = obj.optString("eventId"),
                title = obj.optString("title"),
                description = obj.optString("description", ""),
                date = obj.optString("date"),
                time = obj.optString("time"),
                location = obj.optString("location", ""),
                isOnline = obj.optBoolean("isOnline", false),
                attendeesCount = obj.optInt("attendeesCount", 0),
                organizerId = obj.optString("organizerId"),
                createdAt = obj.optString("createdAt"),
                category = obj.optString("category", "General"),
                imageUrl = if (obj.has("imageUrl") && !obj.isNull("imageUrl")) obj.optString("imageUrl") else null,
                chatEnabled = obj.optBoolean("chatEnabled", true),
                memberIds = parseStringList(obj.optJSONArray("memberIds")),
                adminIds = parseStringList(obj.optJSONArray("adminIds")),
                isPublic = obj.optBoolean("isPublic", true),
                isJoined = obj.optBoolean("isJoined", false),
                isOwner = obj.optBoolean("isOwner", false),
                attendeeAvatars = parseStringList(obj.optJSONArray("attendeeAvatars"))
            )
        }.sortedByDescending { it.createdAt }
    }

    private fun parseStringList(array: org.json.JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { array.getString(it) }
    }
}
