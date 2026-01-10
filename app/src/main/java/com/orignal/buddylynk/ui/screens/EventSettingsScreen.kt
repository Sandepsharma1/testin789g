package com.orignal.buddylynk.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
// Toast removed - using PremiumPopup
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orignal.buddylynk.ui.viewmodel.EventChatViewModel

// Colors
private val ZincDark = Color(0xFF0A0A0A)
private val Zinc800 = Color(0xFF27272A)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc600 = Color(0xFF52525B)
private val Zinc400 = Color(0xFFA1A1AA)
private val OrangeAccent = Color(0xFFF97316)
private val PinkAccent = Color(0xFFEC4899)
private val CyanAccent = Color(0xFF06B6D4)
private val GreenAccent = Color(0xFF10B981)
private val RedAccent = Color(0xFFEF4444)

/**
 * Event Settings Screen - Edit event details, manage members, toggle chat
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventSettingsScreen(
    eventId: String,
    onBack: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToInviteLinks: () -> Unit = {},
    viewModel: EventChatViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Load event data
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
        viewModel.loadMembers(eventId)
        viewModel.loadInviteLinks(eventId)
    }
    
    val event by viewModel.event.collectAsState()
    val members by viewModel.members.collectAsState()
    val inviteLinks by viewModel.inviteLinks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    
    // Edit state
    var editingTitle by remember { mutableStateOf(false) }
    var editedTitle by remember { mutableStateOf("") }
    var editingDate by remember { mutableStateOf(false) }
    var editedDate by remember { mutableStateOf("") }
    var editingTime by remember { mutableStateOf(false) }
    var editedTime by remember { mutableStateOf("") }
    var editingLocation by remember { mutableStateOf(false) }
    var editedLocation by remember { mutableStateOf("") }
    
    // Initialize edit values
    LaunchedEffect(event) {
        event?.let {
            editedTitle = it.title
            editedDate = it.date
            editedTime = it.time
            editedLocation = it.location
        }
    }
    
    val isOwnerOrAdmin = currentUserRole == "owner" || currentUserRole == "admin"
    val memberCount = members.size
    val adminCount = members.count { it.role == "admin" || it.role == "owner" }
    val primaryInviteLink = inviteLinks.firstOrNull()?.url ?: ""
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZincDark)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with back button
            item {
                TopAppBar(
                    title = { Text("Event Settings", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ZincDark
                    )
                )
            }
            
            // Event Avatar and Name Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Event Icon
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(OrangeAccent, PinkAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Event,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(50.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Event Name
                    Text(
                        text = event?.title ?: "Event",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Role badge
                    Text(
                        text = when (currentUserRole) {
                            "owner" -> "👑 You are the owner"
                            "admin" -> "⭐ You are an admin"
                            else -> "👤 Member"
                        },
                        fontSize = 14.sp,
                        color = Zinc400
                    )
                }
            }
            
            // Divider
            item {
                HorizontalDivider(color = Zinc800, thickness = 8.dp)
            }
            
            // EVENT DETAILS Section
            item {
                Text(
                    text = "EVENT DETAILS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Zinc400,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            
            // Date Row
            item {
                SettingsInfoRow(
                    icon = Icons.Outlined.CalendarToday,
                    iconColor = OrangeAccent,
                    title = "Date",
                    subtitle = event?.date ?: "",
                    onClick = {
                        if (isOwnerOrAdmin) {
                            // TODO: Show date picker
                        }
                    },
                    showEdit = isOwnerOrAdmin
                )
            }
            
            // Time Row
            item {
                SettingsInfoRow(
                    icon = Icons.Outlined.AccessTime,
                    iconColor = CyanAccent,
                    title = "Time",
                    subtitle = event?.time ?: "",
                    onClick = {
                        if (isOwnerOrAdmin) {
                            // TODO: Show time picker
                        }
                    },
                    showEdit = isOwnerOrAdmin
                )
            }
            
            // Location Row
            item {
                SettingsInfoRow(
                    icon = Icons.Outlined.LocationOn,
                    iconColor = PinkAccent,
                    title = "Location",
                    subtitle = event?.location?.ifEmpty { "Not set" } ?: "Not set",
                    onClick = {
                        if (isOwnerOrAdmin) {
                            // TODO: Show location editor
                        }
                    },
                    showEdit = isOwnerOrAdmin
                )
            }
            
            // Divider
            item {
                HorizontalDivider(color = Zinc800, thickness = 8.dp)
            }
            
            // INVITE LINK Section (only for owner/admin)
            if (isOwnerOrAdmin) {
                item {
                    Text(
                        text = "INVITE LINK",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Zinc400,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateToInviteLinks)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OrangeAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Link,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Invite link",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (primaryInviteLink.isNotEmpty()) 
                                    primaryInviteLink.removePrefix("https://") 
                                else "Tap to create",
                                fontSize = 13.sp,
                                color = Zinc400,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        // Copy button
                        if (primaryInviteLink.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Invite Link", primaryInviteLink)
                                    clipboard.setPrimaryClip(clip)
                                    // Silent copy - visual feedback via button
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Zinc400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        // Create new link button
                        if (primaryInviteLink.isEmpty()) {
                            IconButton(
                                onClick = { viewModel.createInviteLink(eventId) }
                            ) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = "Create",
                                    tint = OrangeAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    HorizontalDivider(color = Zinc800, thickness = 8.dp)
                }
            }
            
            // MEMBERS Section
            item {
                Text(
                    text = "MEMBERS",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Zinc400,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            
            // Add Members Row (only for owner/admin)
            if (isOwnerOrAdmin) {
                item {
                    SettingsInfoRow(
                        icon = Icons.Outlined.PersonAdd,
                        iconColor = GreenAccent,
                        title = "Add Members",
                        subtitle = "Share invite link to add members",
                        onClick = {
                            if (primaryInviteLink.isEmpty()) {
                                viewModel.createInviteLink(eventId)
                            }
                            // Copy link
                            if (primaryInviteLink.isNotEmpty()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Invite Link", primaryInviteLink)
                                clipboard.setPrimaryClip(clip)
                                // Silent copy
                            }
                        },
                        showAdd = true
                    )
                }
            }
            
            // Members List Row
            item {
                SettingsInfoRow(
                    icon = Icons.Outlined.Groups,
                    iconColor = CyanAccent,
                    title = "View Members",
                    subtitle = "$memberCount members",
                    onClick = onNavigateToMembers,
                    trailingText = "$memberCount"
                )
            }
            
            // Admins Row
            item {
                SettingsInfoRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    iconColor = Color(0xFFF59E0B),
                    title = "Administrators",
                    subtitle = null,
                    onClick = onNavigateToMembers,
                    trailingText = "$adminCount"
                )
            }
            
            // Divider
            item {
                HorizontalDivider(color = Zinc800, thickness = 8.dp)
            }
            
            // CHAT SETTINGS Section (only for owner/admin)
            if (isOwnerOrAdmin) {
                item {
                    Text(
                        text = "CHAT SETTINGS",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Zinc400,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                // Chat Enable/Disable Toggle
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleChatEnabled(eventId) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OrangeAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (event?.chatEnabled == true) Icons.Outlined.ChatBubble else Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = OrangeAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chat Enabled",
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (event?.chatEnabled == true) 
                                    "All members can send messages" 
                                else 
                                    "Only admins can send messages",
                                fontSize = 13.sp,
                                color = Zinc400
                            )
                        }
                        
                        Switch(
                            checked = event?.chatEnabled ?: true,
                            onCheckedChange = { viewModel.toggleChatEnabled(eventId) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = OrangeAccent,
                                uncheckedThumbColor = Zinc400,
                                uncheckedTrackColor = Zinc700
                            )
                        )
                    }
                }
                
                item {
                    HorizontalDivider(color = Zinc800, thickness = 8.dp)
                }
            }
            
            // DANGER ZONE (only for owner)
            if (currentUserRole == "owner") {
                item {
                    Text(
                        text = "DANGER ZONE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = RedAccent,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                
                item {
                    SettingsInfoRow(
                        icon = Icons.Outlined.Delete,
                        iconColor = RedAccent,
                        title = "Delete Event",
                        subtitle = "This action cannot be undone",
                        onClick = {
                            // TODO: Show confirmation dialog
                        },
                        showEdit = false
                    )
                }
            }
            
            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
        
        // Loading indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = OrangeAccent)
            }
        }
    }
}

@Composable
private fun SettingsInfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    showEdit: Boolean = false,
    showAdd: Boolean = false,
    trailingText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Title and subtitle
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Zinc400,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        // Trailing
        if (trailingText != null) {
            Text(
                text = trailingText,
                fontSize = 14.sp,
                color = Zinc400
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        when {
            showEdit -> {
                Icon(
                    Icons.Outlined.Edit,
                    contentDescription = "Edit",
                    tint = Zinc400,
                    modifier = Modifier.size(18.dp)
                )
            }
            showAdd -> {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add",
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            else -> {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Zinc400
                )
            }
        }
    }
}
