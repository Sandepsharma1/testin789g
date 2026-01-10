package com.orignal.buddylynk.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.model.Event
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Colors inline for simplicity
private val DarkBg = Color(0xFF050505)
private val Zinc800 = Color(0xFF27272A)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc500 = Color(0xFF71717A)
private val Zinc400 = Color(0xFFA1A1AA)
private val CyanAccent = Color(0xFF22D3EE)
private val PurpleAccent = Color(0xFF8B5CF6)
private val PinkAccent = Color(0xFFEC4899)
private val OrangeAccent = Color(0xFFF97316)
private val GreenAccent = Color(0xFF22C55E)

// Category colors for display
private val categoryColors = mapOf(
    "Music" to Color(0xFFEC4899),
    "Tech" to Color(0xFF6366F1),
    "Gaming" to Color(0xFF10B981),
    "Education" to Color(0xFFF59E0B),
    "Fitness" to Color(0xFFEF4444),
    "Food" to Color(0xFFF97316),
    "Art" to Color(0xFF8B5CF6),
    "Business" to Color(0xFF3B82F6),
    "Social" to Color(0xFFEC4899),
    "Sports" to Color(0xFF22C55E),
    "Other" to Color(0xFF71717A),
    "General" to Color(0xFF6366F1)
)

private val categoryEmojis = mapOf(
    "Music" to "🎵",
    "Tech" to "💻",
    "Gaming" to "🎮",
    "Education" to "📚",
    "Fitness" to "🏋️",
    "Food" to "🍕",
    "Art" to "🎨",
    "Business" to "💼",
    "Social" to "🎉",
    "Sports" to "⚽",
    "Other" to "📌",
    "General" to "📅"
)

/**
 * EventsScreen - Discover and manage events
 * Features: My Events / Discover tabs, Join button, Countdown timer, Categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEventChat: (eventId: String, title: String, date: String, time: String, location: String) -> Unit = { _, _, _, _, _ -> },
    onNavigateToCreateEvent: () -> Unit = {},
    viewModel: com.orignal.buddylynk.ui.viewmodel.EventsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val events by viewModel.events.collectAsState()
    val discoverEvents by viewModel.discoverEvents.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadEvents()
    }
    
    val filters = listOf("All", "Today", "This Week", "Online")
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "Events",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                IconButton(onClick = onNavigateToCreateEvent) {
                    Icon(
                        imageVector = Icons.Outlined.AddCircle,
                        contentDescription = "Create Event",
                        tint = CyanAccent
                    )
                }
            }
            
            // Tab Bar: My Events | Discover
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Zinc800.copy(alpha = 0.5f)),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                EventTabButton(
                    text = "My Events",
                    isSelected = selectedTab == "my_events",
                    onClick = { viewModel.setTab("my_events") },
                    modifier = Modifier.weight(1f)
                )
                EventTabButton(
                    text = "Discover",
                    isSelected = selectedTab == "discover",
                    onClick = { viewModel.setTab("discover") },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Filter Chips (only show on My Events tab)
            if (selectedTab == "my_events") {
                LazyRow(
                    modifier = Modifier.padding(vertical = 8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filters) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanAccent,
                                selectedLabelColor = Color.White,
                                containerColor = Zinc800,
                                labelColor = Zinc400
                            )
                        )
                    }
                }
            }
            
            // Events List
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanAccent)
                    }
                } else {
                    val displayEvents = if (selectedTab == "my_events") events else discoverEvents
                    
                    if (displayEvents.isEmpty()) {
                        EventEmptyState(
                            isDiscoverTab = selectedTab == "discover",
                            onCreateEvent = onNavigateToCreateEvent
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Featured Event (first event on My Events tab)
                            if (selectedTab == "my_events" && displayEvents.isNotEmpty()) {
                                item {
                                    val feat = displayEvents.first()
                                    EventFeaturedCard(
                                        event = feat,
                                        onClick = {
                                            onNavigateToEventChat(
                                                feat.eventId,
                                                feat.title,
                                                feat.date,
                                                feat.time,
                                                feat.location
                                            )
                                        }
                                    )
                                }
                                
                                if (displayEvents.size > 1) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "All Events",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                            
                            // Event Cards
                            val eventsToShow = if (selectedTab == "my_events" && displayEvents.size > 1) {
                                displayEvents.drop(1)
                            } else {
                                displayEvents
                            }
                            
                            items(eventsToShow) { event ->
                                EventListCard(
                                    event = event,
                                    showJoinButton = selectedTab == "discover",
                                    onJoinClick = { viewModel.joinEvent(event.eventId) },
                                    onLeaveClick = { viewModel.leaveEvent(event.eventId) },
                                    onChatClick = {
                                        onNavigateToEventChat(
                                            event.eventId,
                                            event.title,
                                            event.date,
                                            event.time,
                                            event.location
                                        )
                                    }
                                )
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.linearGradient(
                    if (isSelected) listOf(CyanAccent, PurpleAccent)
                    else listOf(Color.Transparent, Color.Transparent)
                )
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color.White else Zinc400
        )
    }
}

@Composable
private fun EventEmptyState(
    isDiscoverTab: Boolean,
    onCreateEvent: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Zinc800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Event,
                    contentDescription = null,
                    tint = Zinc500,
                    modifier = Modifier.size(40.dp)
                )
            }
            
            Text(
                text = if (isDiscoverTab) "No public events yet" else "No events found",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Zinc400
            )
            
            Text(
                text = "Be the first to create one!",
                fontSize = 14.sp,
                color = Zinc500
            )
            
            Button(
                onClick = onCreateEvent,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Event")
            }
        }
    }
}

@Composable
private fun EventFeaturedCard(
    event: Event,
    onClick: () -> Unit
) {
    val categoryColor = categoryColors[event.category] ?: PurpleAccent
    val categoryEmoji = categoryEmojis[event.category] ?: "📅"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
    ) {
        // Background - either image or gradient
        if (event.imageUrl != null) {
            AsyncImage(
                model = event.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(categoryColor, categoryColor.copy(alpha = 0.6f))
                        )
                    )
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top row: Featured badge + Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "⭐ Featured",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$categoryEmoji ${event.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Column {
                // Countdown timer
                SimpleEventCountdown(date = event.date, time = event.time)
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${event.date} • ${event.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.People,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${event.attendeesCount} going",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EventListCard(
    event: Event,
    showJoinButton: Boolean,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val categoryColor = categoryColors[event.category] ?: PurpleAccent
    val categoryEmoji = categoryEmojis[event.category] ?: "📅"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Zinc800.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Cover image or date box
                if (event.imageUrl != null) {
                    AsyncImage(
                        model = event.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(categoryColor, categoryColor.copy(alpha = 0.7f)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = categoryEmoji, fontSize = 24.sp)
                            Text(
                                text = event.date.split(" ").firstOrNull() ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Event Info
                Column(modifier = Modifier.weight(1f)) {
                    // Category badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$categoryEmoji ${event.category}",
                            fontSize = 10.sp,
                            color = categoryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (event.isOnline) Icons.Filled.Videocam else Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = Zinc500,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (event.location.isNotEmpty()) event.location else "Online",
                            style = MaterialTheme.typography.bodySmall,
                            color = Zinc500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Zinc500,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${event.date} • ${event.time}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Zinc500
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Bottom row: Countdown + Attendees + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Countdown
                SimpleEventCountdown(date = event.date, time = event.time, compact = true)
                
                // Attendees count
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Filled.People,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${event.attendeesCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (showJoinButton) {
                        if (event.isJoined) {
                            OutlinedButton(
                                onClick = onLeaveClick,
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.Check, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Joined", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = onJoinClick,
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                            ) {
                                Icon(Icons.Filled.Add, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Join", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    // Chat button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(PurpleAccent, PinkAccent)))
                            .clickable(onClick = onChatClick)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Forum,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Chat",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleEventCountdown(
    date: String,
    time: String,
    compact: Boolean = false
) {
    var countdownText by remember { mutableStateOf("") }
    var isLive by remember { mutableStateOf(false) }
    var isEnded by remember { mutableStateOf(false) }
    
    LaunchedEffect(date, time) {
        while (true) {
            try {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US)
                val eventDateTime = dateFormat.parse("$date $time")
                
                if (eventDateTime != null) {
                    val now = Date()
                    val diff = eventDateTime.time - now.time
                    
                    when {
                        diff < 0 -> {
                            isEnded = true
                            isLive = false
                            countdownText = "Ended"
                        }
                        diff < 3600000 -> {
                            val minutes = (diff / 60000).toInt()
                            isLive = minutes < 5
                            countdownText = if (isLive) "Live Now!" else "${minutes}m"
                        }
                        diff < 86400000 -> {
                            val hours = (diff / 3600000).toInt()
                            val mins = ((diff % 3600000) / 60000).toInt()
                            countdownText = "${hours}h ${mins}m"
                        }
                        else -> {
                            val days = (diff / 86400000).toInt()
                            countdownText = "${days}d"
                        }
                    }
                }
            } catch (e: Exception) {
                countdownText = ""
            }
            delay(60000)
        }
    }
    
    if (countdownText.isNotEmpty()) {
        val bgColor = when {
            isLive -> GreenAccent
            isEnded -> Zinc500
            else -> OrangeAccent
        }
        
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(if (compact) 6.dp else 8.dp))
                .background(bgColor.copy(alpha = if (compact) 0.15f else 0.2f))
                .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 2.dp else 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isLive) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GreenAccent)
                    )
                }
                Text(
                    text = if (isLive) "LIVE" else if (!compact) "Starts in $countdownText" else countdownText,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = bgColor
                )
            }
        }
    }
}
