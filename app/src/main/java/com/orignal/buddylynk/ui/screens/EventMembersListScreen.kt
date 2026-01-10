package com.orignal.buddylynk.ui.screens

// Toast removed
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.ui.viewmodel.EventChatViewModel

// Colors
private val ZincDark = Color(0xFF0A0A0A)
private val Zinc800 = Color(0xFF27272A)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc400 = Color(0xFFA1A1AA)
private val OrangeAccent = Color(0xFFF97316)
private val PinkAccent = Color(0xFFEC4899)
private val CyanAccent = Color(0xFF06B6D4)
private val GreenAccent = Color(0xFF10B981)
private val YellowAccent = Color(0xFFF59E0B)
private val RedAccent = Color(0xFFEF4444)

/**
 * Event Members List Screen - Shows all members with long-press admin actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMembersListScreen(
    eventId: String,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {},
    viewModel: EventChatViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Load data
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
        viewModel.loadMembers(eventId)
    }
    
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    
    val isOwnerOrAdmin = currentUserRole == "owner" || currentUserRole == "admin"
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZincDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = { Text("Members", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* Search */ }) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZincDark
                )
            )
            
            // Members count
            Text(
                text = "${members.size} members",
                fontSize = 13.sp,
                color = Zinc400,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Long-press instruction
            if (isOwnerOrAdmin) {
                Text(
                    text = "ðŸ’¡ Long-press on a member to manage",
                    fontSize = 12.sp,
                    color = OrangeAccent.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            
            // Members list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(members) { member ->
                    EventMemberListItem(
                        member = member,
                        showAdminActions = isOwnerOrAdmin && member.role != "owner",
                        currentUserRole = currentUserRole,
                        onChatClick = { onNavigateToChat(member.userId) },
                        onPromote = {
                            viewModel.promoteMember(eventId, member.userId)
                            // Silent
                        },
                        onDemote = {
                            viewModel.demoteMember(eventId, member.userId)
                            // Silent
                        },
                        onRemove = {
                            viewModel.removeMember(eventId, member.userId)
                            // Silent
                        }
                    )
                }
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
private fun EventMemberListItem(
    member: EventChatViewModel.Member,
    showAdminActions: Boolean,
    currentUserRole: String,
    onChatClick: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        if (showAdminActions) {
                            showMenu = true
                        }
                    },
                    onTap = {
                        // Optional: handle tap
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(OrangeAccent.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (member.avatar != null) {
                AsyncImage(
                    model = member.avatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = member.username.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrangeAccent
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Name and role
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.username,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Role badge
                when (member.role) {
                    "owner" -> {
                        Spacer(modifier = Modifier.width(8.dp))
                        EventRoleBadge(role = "Owner", color = YellowAccent)
                    }
                    "admin" -> {
                        Spacer(modifier = Modifier.width(8.dp))
                        EventRoleBadge(role = "Admin", color = GreenAccent)
                    }
                }
            }
            
            // Full name or status
            Text(
                text = member.fullName.ifEmpty { "Member" },
                fontSize = 13.sp,
                color = Zinc400
            )
        }
        
        // Chat button
        IconButton(onClick = onChatClick) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Chat",
                tint = CyanAccent,
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Dropdown menu for admin actions (long-press)
        Box {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                offset = DpOffset((-40).dp, 0.dp),
                containerColor = Zinc700
            ) {
                // Promote/Demote
                if (member.role == "admin") {
                    DropdownMenuItem(
                        text = { Text("Demote to member", color = YellowAccent) },
                        onClick = {
                            showMenu = false
                            onDemote()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.ArrowDownward,
                                contentDescription = null,
                                tint = YellowAccent
                            )
                        }
                    )
                } else if (member.role == "member") {
                    DropdownMenuItem(
                        text = { Text("Promote to admin", color = GreenAccent) },
                        onClick = {
                            showMenu = false
                            onPromote()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.ArrowUpward,
                                contentDescription = null,
                                tint = GreenAccent
                            )
                        }
                    )
                }
                
                // Send message
                DropdownMenuItem(
                    text = { Text("Send message", color = Color.White) },
                    onClick = {
                        showMenu = false
                        onChatClick()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = CyanAccent
                        )
                    }
                )
                
                // Remove (only for owner)
                if (currentUserRole == "owner") {
                    DropdownMenuItem(
                        text = { Text("Remove from event", color = RedAccent) },
                        onClick = {
                            showMenu = false
                            onRemove()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.PersonRemove,
                                contentDescription = null,
                                tint = RedAccent
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRoleBadge(
    role: String,
    color: Color
) {
    Text(
        text = role,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
