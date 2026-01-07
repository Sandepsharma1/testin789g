package com.orignal.buddylynk.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
private val Zinc600 = Color(0xFF52525B)
private val Zinc400 = Color(0xFFA1A1AA)
private val PurpleAccent = Color(0xFF8B5CF6)
private val PinkAccent = Color(0xFFEC4899)
private val CyanAccent = Color(0xFF06B6D4)

/**
 * Team Info Screen - Shows channel/group info like Telegram
 * Displays: Avatar, Name, Type, Invite Link, Notifications, Subscribers, Administrators
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamInfoScreen(
    groupId: String,
    groupName: String,
    groupAvatar: String?,
    groupType: String, // "group" or "channel"
    memberCount: Int,
    onBack: () -> Unit,
    onNavigateToInviteLinks: () -> Unit,
    onNavigateToSubscribers: () -> Unit,
    onNavigateToAdmins: () -> Unit,
    viewModel: TeamInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    val isChannel = groupType == "channel"
    
    // Load data
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }
    
    val inviteLinks by viewModel.inviteLinks.collectAsState()
    val members by viewModel.members.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    
    val primaryInviteLink = inviteLinks.firstOrNull()?.url ?: ""
    val adminCount = members.count { it.role == "admin" || it.role == "owner" }
    
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
                    title = { Text("Channel info", color = Color.White) },
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
                        IconButton(onClick = { /* Edit */ }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = ZincDark
                    )
                )
            }
            
            // Avatar and Name Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(PurpleAccent.copy(alpha = 0.3f), PinkAccent.copy(alpha = 0.3f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (groupAvatar != null) {
                            AsyncImage(
                                model = groupAvatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = PurpleAccent,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Name
                    Text(
                        text = groupName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Type and member count
                    Text(
                        text = if (isChannel) "Public channel • $memberCount subscribers" 
                               else "Group • $memberCount members",
                        fontSize = 14.sp,
                        color = Zinc400
                    )
                }
            }
            
            // Divider
            item {
                HorizontalDivider(color = Zinc800, thickness = 8.dp)
            }
            
            // INFO Section
            item {
                Text(
                    text = "INFO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Zinc400,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            
            // Invite Link Row
            item {
                InfoRow(
                    icon = Icons.Outlined.Link,
                    iconColor = PurpleAccent,
                    title = "Invite link",
                    subtitle = if (primaryInviteLink.isNotEmpty()) 
                        primaryInviteLink.removePrefix("https://") 
                    else "No invite link",
                    onClick = onNavigateToInviteLinks,
                    trailingContent = {
                        Row {
                            // QR Code
                            IconButton(
                                onClick = { /* Show QR */ },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.QrCode2,
                                    contentDescription = "QR Code",
                                    tint = Zinc400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            // Copy
                            IconButton(
                                onClick = {
                                    if (primaryInviteLink.isNotEmpty()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Invite Link", primaryInviteLink)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy",
                                    tint = Zinc400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                )
            }
            
            // Notifications Row
            item {
                InfoRow(
                    icon = Icons.Outlined.Notifications,
                    iconColor = Color(0xFFEF4444),
                    title = "Notifications",
                    subtitle = if (notificationsEnabled) "On" else "Off",
                    onClick = { viewModel.toggleNotifications() },
                    trailingContent = {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PurpleAccent,
                                uncheckedThumbColor = Zinc400,
                                uncheckedTrackColor = Zinc700
                            )
                        )
                    }
                )
            }
            
            // Divider
            item {
                HorizontalDivider(color = Zinc800, thickness = 8.dp)
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
            
            // Subscribers Row
            item {
                InfoRow(
                    icon = Icons.Outlined.Groups,
                    iconColor = CyanAccent,
                    title = if (isChannel) "Subscribers" else "Members",
                    subtitle = "$memberCount ${if (isChannel) "subscribers" else "members"}",
                    onClick = onNavigateToSubscribers,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$memberCount",
                                fontSize = 14.sp,
                                color = Zinc400
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Zinc400
                            )
                        }
                    }
                )
            }
            
            // Administrators Row
            item {
                InfoRow(
                    icon = Icons.Outlined.AdminPanelSettings,
                    iconColor = Color(0xFFF59E0B),
                    title = "Administrators",
                    subtitle = null,
                    onClick = onNavigateToAdmins,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$adminCount",
                                fontSize = 14.sp,
                                color = Zinc400
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Zinc400
                            )
                        }
                    }
                )
            }
            
            // Divider
            item {
                HorizontalDivider(color = Zinc800, thickness = 8.dp)
            }
            
            // Channel Settings Row
            item {
                InfoRow(
                    icon = Icons.Outlined.Settings,
                    iconColor = Zinc400,
                    title = "Channel settings",
                    subtitle = null,
                    onClick = { /* Navigate to settings */ },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Zinc400
                        )
                    }
                )
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
                CircularProgressIndicator(color = PurpleAccent)
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
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
        
        // Trailing content
        trailingContent()
    }
}
