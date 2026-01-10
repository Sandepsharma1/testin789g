package com.orignal.buddylynk.ui.screens

// Toast removed
import androidx.compose.foundation.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.ui.viewmodel.TeamInfoViewModel

// Colors
private val ZincDark = Color(0xFF0A0A0A)
private val Zinc800 = Color(0xFF27272A)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc400 = Color(0xFFA1A1AA)
private val PurpleAccent = Color(0xFF8B5CF6)
private val CyanAccent = Color(0xFF06B6D4)
private val GreenAccent = Color(0xFF10B981)
private val YellowAccent = Color(0xFFF59E0B)
private val RedAccent = Color(0xFFEF4444)

/**
 * Members List Screen - Shows subscribers or administrators
 * For admins: shows promote/demote options
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MembersListScreen(
    groupId: String,
    showAdminsOnly: Boolean, // true = Administrators, false = Subscribers
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit, // Navigate to DM with userId
    viewModel: TeamInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Load data
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }
    
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Filter members based on mode
    val displayMembers = if (showAdminsOnly) {
        members.filter { it.role == "admin" || it.role == "owner" }
    } else {
        members.filter { it.role == "subscriber" }
    }
    
    var selectedMemberId by remember { mutableStateOf<String?>(null) }
    var showMemberOptions by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ZincDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = { 
                    Text(
                        if (showAdminsOnly) "Administrators" else "Subscribers",
                        color = Color.White
                    )
                },
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
                    if (!showAdminsOnly) {
                        // Search button for subscribers
                        IconButton(onClick = { /* Search */ }) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = "Search",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ZincDark
                )
            )
            
            // Members count
            Text(
                text = "${displayMembers.size} ${if (showAdminsOnly) "administrators" else "subscribers"}",
                fontSize = 13.sp,
                color = Zinc400,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Members list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(displayMembers) { member ->
                    MemberListItem(
                        member = member,
                        showAdminActions = showAdminsOnly,
                        onClick = {
                            selectedMemberId = member.userId
                            showMemberOptions = true
                        },
                        onChatClick = {
                            onNavigateToChat(member.userId)
                        },
                        onPromote = {
                            viewModel.promoteMember(member.userId)
                            // Silent
                        },
                        onDemote = {
                            viewModel.demoteMember(member.userId)
                            // Silent
                        },
                        onRemove = {
                            viewModel.removeMember(member.userId)
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
                CircularProgressIndicator(color = PurpleAccent)
            }
        }
    }
}

@Composable
private fun MemberListItem(
    member: TeamInfoViewModel.Member,
    showAdminActions: Boolean,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    onPromote: () -> Unit,
    onDemote: () -> Unit,
    onRemove: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PurpleAccent.copy(alpha = 0.3f)),
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
                    color = PurpleAccent
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
                if (member.role == "owner") {
                    Spacer(modifier = Modifier.width(8.dp))
                    RoleBadge(role = "Owner", color = YellowAccent)
                } else if (member.role == "admin") {
                    Spacer(modifier = Modifier.width(8.dp))
                    RoleBadge(role = "Admin", color = GreenAccent)
                }
            }
            
            // Last seen or status
            Text(
                text = "last seen recently",
                fontSize = 13.sp,
                color = Zinc400
            )
        }
        
        // Actions
        if (showAdminActions && member.role != "owner") {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "Options",
                        tint = Zinc400
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = Zinc700
                ) {
                    // Chat option
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
                    
                    // Promote/Demote
                    if (member.role == "admin") {
                        DropdownMenuItem(
                            text = { Text("Demote to subscriber", color = YellowAccent) },
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
                    } else {
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
                    
                    // Remove
                    DropdownMenuItem(
                        text = { Text("Remove from group", color = RedAccent) },
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
        } else {
            // Just show chat button for subscribers list
            IconButton(onClick = onChatClick) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Chat",
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun RoleBadge(
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
