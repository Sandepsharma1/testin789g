package com.orignal.buddylynk.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.model.OttVideo
import com.orignal.buddylynk.ui.viewmodel.OttViewModel

// Premium Colors
private val DarkBg = Color(0xFF0A0A0F)
private val CardBg = Color(0xFF16161F)
private val AccentPink = Color(0xFFEC4899)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF22D3EE)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF9CA3AF)

/**
 * OTT Profile Screen - Shows user's uploads, saved videos, and watch history
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OttProfileScreen(
    onNavigateBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onUploadClick: () -> Unit, // Navigate to upload screen
    viewModel: OttViewModel = viewModel()
) {
    val myVideos by viewModel.myVideos.collectAsState()
    val savedVideos by viewModel.savedVideos.collectAsState()
    val watchHistory by viewModel.watchHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val currentUser by com.orignal.buddylynk.data.auth.AuthManager.currentUser.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        ProfileTab("Watched", Icons.Outlined.History),
        ProfileTab("Saved", Icons.Outlined.Bookmark),
        ProfileTab("Uploads", Icons.Outlined.VideoLibrary)
    )
    
    // Load data on first launch
    LaunchedEffect(Unit) {
        // Refresh current user to get latest profile data
        com.orignal.buddylynk.data.auth.AuthManager.refreshCurrentUser()
        
        viewModel.loadMyVideos()
        viewModel.loadSavedVideos()
        viewModel.loadWatchHistory()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Header
        ProfileHeader(
            username = currentUser?.username ?: "User",
            avatarUrl = currentUser?.avatar,
            onNavigateBack = onNavigateBack,
            onUploadClick = onUploadClick
        )
        
        // Stats Row
        ProfileStats(
            uploads = myVideos.size,
            saved = savedVideos.size,
            watched = watchHistory.size
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = CardBg,
            contentColor = AccentPink
        ) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                tab.title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    },
                    selectedContentColor = AccentPink,
                    unselectedContentColor = TextSecondary
                )
            }
        }
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentPink)
                    }
                }
                else -> {
                    val currentVideos = when (selectedTab) {
                        0 -> watchHistory
                        1 -> savedVideos
                        2 -> myVideos
                        else -> emptyList()
                    }
                    
                    if (currentVideos.isEmpty()) {
                        EmptyState(
                            tab = tabs[selectedTab].title,
                            icon = tabs[selectedTab].icon
                        )
                    } else {
                        VideoGrid(
                            videos = currentVideos,
                            onVideoClick = onVideoClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    username: String,
    avatarUrl: String?,
    onNavigateBack: () -> Unit,
    onUploadClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onNavigateBack) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Profile Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(AccentPink, AccentPurple))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null && avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Profile",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = username.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column {
            Text(
                text = username,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "My OTT Profile",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Upload Button
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(AccentPink, AccentPurple))
                )
                .clickable { onUploadClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Upload",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ProfileStats(
    uploads: Int,
    saved: Int,
    watched: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem("Uploads", uploads, AccentPink)
        StatItem("Saved", saved, AccentPurple)
        StatItem("Watched", watched, AccentCyan)
    }
}

@Composable
private fun StatItem(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun VideoGrid(
    videos: List<OttVideo>,
    onVideoClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(videos) { video ->
            VideoGridItem(
                video = video,
                onClick = { onVideoClick(video.videoId) }
            )
        }
    }
}

@Composable
private fun VideoGridItem(
    video: OttVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Play icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(40.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Info
            Column(
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${video.viewCount} views",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun EmptyState(tab: String, icon: ImageVector) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(CardBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No $tab Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when (tab) {
                "Uploads" -> "Upload a video to see it here"
                "Saved" -> "Save videos to watch later"
                else -> "Videos you watch will appear here"
            },
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

private data class ProfileTab(
    val title: String,
    val icon: ImageVector
)

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", mins, secs)
}
