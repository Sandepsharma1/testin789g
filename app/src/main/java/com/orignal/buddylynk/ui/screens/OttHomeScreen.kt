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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.model.OttVideo
import com.orignal.buddylynk.ui.viewmodel.OttViewModel

// Premium Colors
private val DarkBg = Color(0xFF050508)
private val CardBg = Color(0xFF12121A)
private val CardBgHover = Color(0xFF1A1A25)
private val AccentPink = Color(0xFFEC4899)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentCyan = Color(0xFF06B6D4)
private val GoldAccent = Color(0xFFFFD700)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB0B0B8)
private val TextTertiary = Color(0xFF6B7280)

/**
 * OTT Home Screen - Premium YouTube-like video feed
 * Scrollable header, no categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OttHomeScreen(
    onNavigateBack: () -> Unit,
    onVideoClick: (String) -> Unit,
    onUploadClick: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit, // Navigate to profile
    viewModel: OttViewModel = viewModel()
) {
    val videos by viewModel.videos.collectAsState()
    val trendingVideos by viewModel.trendingVideos.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Get current user for ownership check
    val currentUser by com.orignal.buddylynk.data.auth.AuthManager.currentUser.collectAsState()
    val currentUserId = currentUser?.userId
    
    // Get real-time OTT upload progress
    val ottUploads by com.orignal.buddylynk.data.upload.UploadManager.ottUploads.collectAsState()
    val activeUpload = ottUploads.values.firstOrNull { it.status == "uploading" || it.status == "pending" }
    val completedUpload = ottUploads.values.firstOrNull { it.status == "completed" }
    
    // Track last refresh to prevent duplicate refreshes
    var lastRefreshId by remember { mutableStateOf<String?>(null) }
    
    // Auto-refresh when upload completes
    LaunchedEffect(completedUpload?.uploadId) {
        completedUpload?.let { upload ->
            if (upload.uploadId != lastRefreshId) {
                lastRefreshId = upload.uploadId
                // Small delay to ensure backend has processed the video
                kotlinx.coroutines.delay(1500)
                viewModel.refreshVideos()
            }
        }
    }
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var videoToDelete by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Delete confirmation dialog
    if (showDeleteDialog && videoToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                videoToDelete = null
            },
            title = { Text("Delete Video?", color = TextPrimary) },
            text = { Text("This will permanently delete the video from your account and storage. This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        videoToDelete?.let { videoId ->
                            viewModel.deleteVideo(
                                videoId = videoId,
                                onSuccess = {
                                    android.widget.Toast.makeText(context, "Video deleted", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                onError = { error ->
                                    android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        showDeleteDialog = false
                        videoToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteDialog = false
                    videoToDelete = null
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBg
        )
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Premium Header (scrolls with content)
            item {
                PremiumOttHeader(
                    onNavigateBack = onNavigateBack,
                    onSearchClick = onSearchClick,
                    onProfileClick = onProfileClick
                )
            }
            
            // Real-time upload progress bar
            if (activeUpload != null) {
                item {
                    OttUploadProgressBar(
                        title = activeUpload.title,
                        progress = activeUpload.progress,
                        statusMessage = activeUpload.statusMessage
                    )
                }
            }
            
            // Trending Section
            if (trendingVideos.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "🔥 Trending Now",
                        subtitle = "Most watched videos this week"
                    )
                }
                
                items(trendingVideos.take(3)) { video ->
                    PremiumVideoCard(
                        video = video,
                        onClick = { onVideoClick(video.videoId) },
                        isTrending = true,
                        currentUserId = currentUserId,
                        onDelete = { videoId ->
                            videoToDelete = videoId
                            showDeleteDialog = true
                        }
                    )
                }
            }
            
            // Loading state
            if (isLoading && videos.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = AccentPink,
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
            
            // All Videos Section
            if (videos.isNotEmpty()) {
                item {
                    SectionTitle(
                        title = "📺 For You",
                        subtitle = "Discover amazing content"
                    )
                }
                
                items(videos) { video ->
                    PremiumVideoCard(
                        video = video,
                        onClick = { onVideoClick(video.videoId) },
                        currentUserId = currentUserId,
                        onDelete = { videoId ->
                            videoToDelete = videoId
                            showDeleteDialog = true
                        }
                    )
                }
            }
            
            // Empty state
            if (!isLoading && videos.isEmpty() && trendingVideos.isEmpty()) {
                item {
                    PremiumEmptyState()
                }
            }
        }
        
    }
}

@Composable
private fun PremiumOttHeader(
    onNavigateBack: () -> Unit,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    // Get current user for profile
    val currentUser by com.orignal.buddylynk.data.auth.AuthManager.currentUser.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBg)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button - minimal style
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable { onNavigateBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(14.dp))
        
        // Minimal Logo - Just B with gradient ring
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(AccentPink, AccentPurple)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "B",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Title - clean and minimal
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Buddylynk",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "OTT",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentCyan,
                letterSpacing = 1.5.sp
            )
        }
        
        // Search button - minimal
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
                .clickable { onSearchClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(10.dp))
        
        // Profile Avatar - replaces Upload button
        val username = currentUser?.username ?: "U"
        val letter = username.first().uppercaseChar()
        val avatarUrl = currentUser?.avatar
        
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(AccentPink, AccentPurple, AccentCyan, AccentPink)
                    )
                )
                .padding(2.dp)
                .clip(CircleShape)
                .clickable { onProfileClick() }, // Navigate to profile
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(DarkBg),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null && avatarUrl.isNotBlank() && avatarUrl != "null") {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Profile",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                } else {
                    Text(
                        text = letter.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPink
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        AccentPurple.copy(alpha = 0.4f),
                        AccentPink.copy(alpha = 0.3f),
                        AccentCyan.copy(alpha = 0.2f)
                    )
                )
            )
    ) {
        // Decorative elements
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 30.dp, y = (-30).dp)
                .background(
                    AccentPink.copy(alpha = 0.2f),
                    CircleShape
                )
                .blur(40.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Premium Videos",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Watch & upload unlimited content",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureBadge(icon = Icons.Filled.HighQuality, text = "4K")
                FeatureBadge(icon = Icons.Filled.Speed, text = "Fast")
                FeatureBadge(icon = Icons.Filled.CloudUpload, text = "Upload")
            }
        }
    }
}

@Composable
private fun FeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun PremiumVideoCard(
    video: OttVideo,
    onClick: () -> Unit,
    isTrending: Boolean = false,
    currentUserId: String? = null,
    onDelete: ((String) -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
                
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )
                
                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .background(
                            Color.Black.copy(alpha = 0.85f),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatDuration(video.duration),
                        fontSize = 12.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Trending badge
                if (isTrending) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(
                                Brush.linearGradient(listOf(AccentPink, AccentPurple)),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔥",
                                fontSize = 12.sp
                            )
                            Text(
                                text = "Trending",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Play icon overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Info section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Creator avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
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
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = video.creatorName,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text("•", color = TextTertiary, fontSize = 8.sp)
                        Text(
                            text = formatViewCount(video.viewCount),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text("•", color = TextTertiary, fontSize = 8.sp)
                        Text(
                            text = formatTimeAgo(video.createdAt),
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
                
                // More button with dropdown menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "More",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        // Delete option - only for video owner
                        val isOwner = currentUserId != null && video.creatorId == currentUserId
                        if (isOwner && onDelete != null) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            "Delete Video",
                                            color = Color(0xFFEF4444),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onDelete(video.videoId)
                                }
                            )
                        }
                        
                        // Share option (always visible)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Share,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Share", color = TextPrimary)
                                }
                            },
                            onClick = { showMenu = false }
                        )
                        
                        // Report option (for non-owners)
                        if (currentUserId == null || video.creatorId != currentUserId) {
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Flag,
                                            contentDescription = null,
                                            tint = TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text("Report", color = TextPrimary)
                                    }
                                },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Animated icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    Brush.linearGradient(
                        listOf(AccentPurple.copy(alpha = 0.2f), AccentPink.copy(alpha = 0.2f))
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.VideoLibrary,
                contentDescription = null,
                tint = AccentPink,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Text(
            text = "No videos yet",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Text(
            text = "Be the first to share amazing content!",
            fontSize = 15.sp,
            color = TextSecondary
        )
    }
}

/**
 * Real-time OTT Upload Progress Bar
 */
@Composable
private fun OttUploadProgressBar(
    title: String,
    progress: Float,
    statusMessage: String
) {
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = progress,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300),
        label = "progress"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated upload icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Brush.linearGradient(listOf(AccentPink, AccentPurple)),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CloudUpload,
                            contentDescription = "Uploading",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = title.take(25) + if (title.length > 25) "..." else "",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            color = AccentCyan
                        )
                    }
                }
                
                // Percentage
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentPink
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.linearGradient(listOf(AccentPink, AccentPurple))
                        )
                )
            }
        }
    }
}

// Helper functions
private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, mins, secs)
    } else {
        String.format("%d:%02d", mins, secs)
    }
}

private fun formatViewCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM views", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK views", count / 1_000.0)
        else -> "$count views"
    }
}

private fun formatTimeAgo(dateString: String): String {
    return "Recently"
}
