package com.orignal.buddylynk.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.model.OttVideo
import com.orignal.buddylynk.ui.components.VideoPlayer
import com.orignal.buddylynk.ui.viewmodel.OttViewModel

// Premium Colors
private val DarkBg = Color(0xFF0A0A0F)
private val CardBg = Color(0xFF16161F)
private val AccentPink = Color(0xFFEC4899)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF22D3EE)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF9CA3AF)
private val TextTertiary = Color(0xFF6B7280)

/**
 * OTT Video Player Screen - Full video playback with details
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OttVideoPlayerScreen(
    videoId: String,
    onNavigateBack: () -> Unit,
    onCreatorClick: (String) -> Unit,
    viewModel: OttViewModel = viewModel()
) {
    val video by viewModel.currentVideo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }
    
    // Handle back press when in fullscreen
    androidx.activity.compose.BackHandler(enabled = isFullScreen) {
        isFullScreen = false
    }
    
    LaunchedEffect(videoId) {
        viewModel.loadVideo(videoId)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        if (isLoading && video == null) {
            // Loading
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPink)
            }
        } else if (video != null) {
            val currentVideo = video!!
            
            // FULLSCREEN MODE
            if (isFullScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .systemBarsPadding()
                ) {
                    VideoPlayer(
                        videoUrl = currentVideo.videoUrl,
                        thumbnailUrl = currentVideo.thumbnailUrl,
                        modifier = Modifier.fillMaxSize(),
                        autoPlay = true,
                        showControls = true,
                        isFullScreen = true,
                        onFullScreenToggle = { isFullScreen = false }
                    )
                    
                    // Close fullscreen button
                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(44.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // NORMAL MODE
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Video Player - Adapts to video's natural size
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 320.dp)
                    ) {
                        VideoPlayer(
                            videoUrl = currentVideo.videoUrl,
                            thumbnailUrl = currentVideo.thumbnailUrl,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = true,
                            showControls = true,
                            isFullScreen = false,
                            onFullScreenToggle = { isFullScreen = true }
                        )
                        
                        // Back button overlay
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .statusBarsPadding()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    }
                    
                    // Content
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        // Title and stats
                        item {
                            VideoInfoSection(
                                video = currentVideo,
                                isDescriptionExpanded = isDescriptionExpanded,
                                onExpandClick = { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                        }
                        
                        // Action buttons
                        item {
                            ActionButtonsRow(
                                video = currentVideo,
                                onLikeClick = { viewModel.likeVideo(currentVideo.videoId) }
                            )
                        }
                        
                        // Creator info
                        item {
                            CreatorSection(
                                video = currentVideo,
                                onCreatorClick = { onCreatorClick(currentVideo.creatorId) }
                            )
                        }
                        
                        // Divider
                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                color = Color.White.copy(alpha = 0.1f)
                            )
                        }
                        
                        // Comments placeholder
                        item {
                            Text(
                                text = "Comments coming soon...",
                                fontSize = 14.sp,
                                color = TextTertiary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearCurrentVideo()
        }
    }
}

@Composable
private fun VideoInfoSection(
    video: OttVideo,
    isDescriptionExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title
        Text(
            text = video.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Stats row
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatViewCount(video.viewCount),
                fontSize = 13.sp,
                color = TextSecondary
            )
            Text("•", color = TextTertiary, fontSize = 10.sp)
            Text(
                text = video.category,
                fontSize = 13.sp,
                color = AccentCyan
            )
            Text("•", color = TextTertiary, fontSize = 10.sp)
            Text(
                text = formatTimeAgo(video.createdAt),
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        
        // Description
        if (video.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBg)
                    .clickable(onClick = onExpandClick)
                    .padding(12.dp)
            ) {
                Text(
                    text = video.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                
                if (video.description.length > 100) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isDescriptionExpanded) "Show less" else "Show more",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentPink
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    video: OttVideo,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Like
        ActionButton(
            icon = if (video.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            label = formatCount(video.likeCount),
            isActive = video.isLiked,
            onClick = onLikeClick
        )
        
        // Share
        ActionButton(
            icon = Icons.Outlined.Share,
            label = "Share",
            onClick = { /* TODO: Share */ }
        )
        
        // Save
        ActionButton(
            icon = if (video.isSaved) Icons.Filled.BookmarkAdded else Icons.Outlined.BookmarkAdd,
            label = "Save",
            isActive = video.isSaved,
            onClick = { /* TODO: Save */ }
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) AccentPink else TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isActive) AccentPink else TextSecondary
        )
    }
}

@Composable
private fun CreatorSection(
    video: OttVideo,
    onCreatorClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCreatorClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(AccentPink, AccentPurple))
                ),
            contentAlignment = Alignment.Center
        ) {
            if (video.creatorAvatar != null) {
                AsyncImage(
                    model = video.creatorAvatar,
                    contentDescription = video.creatorName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = video.creatorName.take(1).uppercase(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        // Name
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = video.creatorName,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = "Creator",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        
        // Subscribe button
        Button(
            onClick = { /* TODO: Subscribe */ },
            colors = ButtonDefaults.buttonColors(containerColor = AccentPink),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Subscribe",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// Helper functions
private fun formatViewCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM views", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK views", count / 1_000.0)
        else -> "$count views"
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

private fun formatTimeAgo(dateString: String): String {
    // Simple time ago - in production would use proper date parsing
    return "Recently uploaded"
}
