package com.orignal.buddylynk.data.model

/**
 * Event data class - Represents a Buddylynk event
 * Stored in DynamoDB Events table
 */
data class Event(
    val eventId: String,
    val title: String,
    val description: String = "",
    val date: String, // e.g. "Dec 15, 2025"
    val time: String, // e.g. "7:00 PM"
    val location: String = "",
    val isOnline: Boolean = false,
    val attendeesCount: Int = 0,
    val organizerId: String,
    val createdAt: String,
    val category: String = "General", // e.g. "Tech", "Music", "Gaming"
    val imageUrl: String? = null, // Cover image URL from S3
    val chatEnabled: Boolean = true, // When false, only admin/owner can send messages
    val memberIds: List<String> = emptyList(), // Users who have joined the event
    val adminIds: List<String> = emptyList(), // Users who can manage the event
    val isPublic: Boolean = true, // Whether event shows in discovery
    val isJoined: Boolean = false, // Whether current user has joined (populated by API)
    val isOwner: Boolean = false, // Whether current user is the owner (populated by API)
    val attendeeAvatars: List<String> = emptyList() // First 5 attendee avatars for preview
)
