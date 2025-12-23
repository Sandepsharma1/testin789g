package com.orignal.buddylynk.ui.components

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.orignal.buddylynk.data.model.Post
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Premium Colors matching React
private val DarkBg = Color(0xFF050505)
private val CardBg = Color(0xFF1A1A1A)
private val BorderWhite10 = Color.White.copy(alpha = 0.1f)
private val CyanGlow = Color(0xFF22D3EE)
private val IndigoAccent = Color(0xFF6366F1)

/**
 * Format timestamp to relative time (e.g., "1 min ago", "2 hours ago")
 */
private fun formatTimeAgo(createdAt: String): String {
    if (createdAt.isBlank()) return ""
    
    return try {
        // Parse ISO8601 timestamp
        val timestamp = try {
            java.time.Instant.parse(createdAt).toEpochMilli()
        } catch (e: Exception) {
            // Try parsing as epoch millis string
            createdAt.toLongOrNull() ?: return ""
        }
        
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        val weeks = days / 7
        val months = days / 30
        
        when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            weeks < 4 -> "${weeks}w ago"
            months < 12 -> "${months}mo ago"
            else -> "${days / 365}y ago"
        }
    } catch (e: Exception) {
        ""
    }
}

/**
 * Premium Post Card with "Action Capsule" design from React
 * Features:
 * - Double-tap to like with shockwave animation
 * - Spinning neon ring for users with stories
 * - Capsule-style action buttons (like React design)
 * - Bookmark button with active state
 */
@Composable
fun PremiumPostCard(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean,
    hasStatus: Boolean = false,
    isOwner: Boolean = false,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onMoreClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onReport: () -> Unit = {},
    onBlock: () -> Unit = {},
    onAvatarLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLikeAnimation by remember { mutableStateOf(false) }
    var cardScale by remember { mutableStateOf(1f) }
    val scope = rememberCoroutineScope()

    // Animate card scale
    val animatedScale by animateFloatAsState(
        targetValue = cardScale,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "cardScale"
    )

    // Double tap handler
    val handleDoubleTap = {
        if (!isLiked) {
            onLike()
        }
        showLikeAnimation = true
        cardScale = 0.98f
        scope.launch {
            delay(600)
            showLikeAnimation = false
            cardScale = 1f
        }
    }

    Column(modifier = modifier) {
        // Image Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .scale(animatedScale)
                .clip(RoundedCornerShape(32.dp))
                .background(CardBg)
                .border(1.dp, BorderWhite10, RoundedCornerShape(32.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { handleDoubleTap() }
                    )
                }
        ) {
            // Post Image/Video - Support for multiple media
            val allMediaUrls = if (post.mediaUrls.isNotEmpty()) {
                post.mediaUrls
            } else if (!post.mediaUrl.isNullOrBlank()) {
                listOf(post.mediaUrl)
            } else {
                emptyList()
            }
            
            val context = LocalContext.current
            
            if (allMediaUrls.isNotEmpty()) {
                var currentPage by remember { mutableIntStateOf(0) }
                
                Box(modifier = Modifier.fillMaxSize()) {
                    // Media display with swipe support
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = androidx.compose.foundation.pager.rememberPagerState { allMediaUrls.size }.also { 
                            currentPage = it.currentPage 
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val mediaUrl = allMediaUrls[page]
                        val isVideo = mediaUrl.endsWith(".mp4", ignoreCase = true) || 
                                       mediaUrl.endsWith(".webm", ignoreCase = true) ||
                                       post.mediaType == "video"
                        
                        if (isVideo) {
                            // Video Player with ExoPlayer + Loading indicator
                            var isBuffering by remember { mutableStateOf(true) }
                            
                            val exoPlayer = remember(mediaUrl) {
                                ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(MediaItem.fromUri(Uri.parse(mediaUrl)))
                                    repeatMode = Player.REPEAT_MODE_ONE
                                    volume = 0f // Muted by default
                                    addListener(object : Player.Listener {
                                        override fun onPlaybackStateChanged(state: Int) {
                                            isBuffering = state == Player.STATE_BUFFERING
                                        }
                                    })
                                    prepare()
                                    playWhenReady = true
                                }
                            }
                            
                            DisposableEffect(mediaUrl) {
                                onDispose {
                                    exoPlayer.release()
                                }
                            }
                            
                            Box(modifier = Modifier.fillMaxSize()) {
                                AndroidView(
                                    factory = { ctx ->
                                        PlayerView(ctx).apply {
                                            player = exoPlayer
                                            useController = false
                                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                            layoutParams = FrameLayout.LayoutParams(
                                                ViewGroup.LayoutParams.MATCH_PARENT,
                                                ViewGroup.LayoutParams.MATCH_PARENT
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                                
                                // Show loading shimmer while buffering
                                if (isBuffering) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(CardBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                                        CircularProgressIndicator(
                                            color = CyanGlow,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Optimized Image display with shimmer loading
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(mediaUrl)
                                    .crossfade(300)
                                    .build(),
                                contentDescription = "Post media ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    // Shimmer loading placeholder
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(CardBg)
                                    ) {
                                        ShimmerEffect(modifier = Modifier.fillMaxSize())
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFF1A1A2E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.BrokenImage,
                                            contentDescription = "Error loading image",
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                    
                    // Pagination dots (only show if multiple media)
                    if (allMediaUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            repeat(allMediaUrls.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(if (index == currentPage) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == currentPage) 
                                                Color.White 
                                            else 
                                                Color.White.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                        
                        // Page counter badge (top right)
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${currentPage + 1}/${allMediaUrls.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // No media - show placeholder/content background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF1A1A2E),
                                    Color(0xFF252547)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = post.content.take(100),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }

            // Shockwave Like Animation
            if (showLikeAnimation) {
                LikeShockwaveAnimation()
            }

            // Top User Info Bar
            UserInfoBar(
                username = post.username ?: "User",
                avatar = post.userAvatar,
                createdAt = post.createdAt,
                hasStatus = hasStatus,
                isOwner = isOwner,
                onUserClick = { onUserClick(post.userId) },
                onMoreClick = onMoreClick,
                onEdit = onEdit,
                onDelete = onDelete,
                onReport = onReport,
                onBlock = onBlock,
                onAvatarLongPress = onAvatarLongPress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Capsule + Bookmark
        ActionCapsuleRow(
            post = post,
            isLiked = isLiked,
            isSaved = isSaved,
            onLike = onLike,
            onComment = onComment,
            onShare = onShare,
            onSave = onSave
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Caption
        CaptionSection(
            username = post.username ?: "User",
            caption = post.content,
            timeAgo = formatTimeAgo(post.createdAt)
        )
    }
}

@Composable
private fun LikeShockwaveAnimation() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Outer ring 1
        val ring1Scale by rememberInfiniteTransition(label = "ring1").animateFloat(
            initialValue = 0f,
            targetValue = 4f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring1Scale"
        )
        val ring1Alpha by rememberInfiniteTransition(label = "ring1Alpha").animateFloat(
            initialValue = 0.6f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(700, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ring1Alpha"
        )
        
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(ring1Scale)
                .border(2.dp, Color.White.copy(alpha = ring1Alpha), CircleShape)
        )

        // Center Heart
        Icon(
            imageVector = Icons.Filled.Favorite,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .scale(1.2f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserInfoBar(
    username: String,
    avatar: String?,
    createdAt: String,
    hasStatus: Boolean,
    isOwner: Boolean,
    onUserClick: () -> Unit,
    onMoreClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit,
    onBlock: () -> Unit,
    onAvatarLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User Avatar + Name Pill - CLICKABLE to navigate to profile
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(1.dp, BorderWhite10, RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = { onUserClick() },
                    onLongClick = { onAvatarLongPress() }
                )
                .padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar with Spinning Neon Ring (if has status)
            Box {
                if (hasStatus) {
                    SpinningNeonRing()
                }
                
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatar)
                        .crossfade(200)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.Black, CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                if (hasStatus) {
                    // Pulsing status dot
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(CyanGlow)
                            .border(2.dp, Color.Black, CircleShape)
                    )
                }
            }
            
            // Username only (no time)
            Text(
                text = username,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // More Button with Animated Popup Menu
        var showMenu by remember { mutableStateOf(false) }
        val menuScale by animateFloatAsState(
            targetValue = if (showMenu) 1f else 0.8f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
            label = "menuScale"
        )
        
        Box {
            // 3-Dot Button with press animation
            var isPressed by remember { mutableStateOf(false) }
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.9f else 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                label = "buttonScale"
            )
            
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(36.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f)) // Glassy transparent
                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            }
                        )
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(18.dp)
                )
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .scale(menuScale)
                    .widthIn(min = 260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0D0D1A)) // Solid dark base
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1F1F3D).copy(alpha = 0.9f),
                                Color(0xFF12122A).copy(alpha = 0.95f),
                                Color(0xFF0A0A15)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.5f),
                                Color(0xFFEC4899).copy(alpha = 0.3f),
                                Color(0xFF8B5CF6).copy(alpha = 0.4f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(vertical = 16.dp, horizontal = 8.dp)
            ) {
                if (isOwner) {
                    // Owner options - Edit and Delete with hover effect
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF818CF8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Edit Post",
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Modify your post",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    
                    // Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 16.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        "Delete Post",
                                        color = Color(0xFFF87171),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Remove permanently",
                                        color = Color(0xFFEF4444).copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                } else {
                    // Non-owner options - Report with premium styling
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                // Icon with gradient background
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFFBBF24).copy(alpha = 0.25f),
                                                    Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Flag,
                                        contentDescription = null,
                                        tint = Color(0xFFFCD34D),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Report Post",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Report inappropriate content",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onReport()
                        }
                    )
                    
                    // Divider line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.1f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    
                    // Block User with premium styling
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp, horizontal = 8.dp)
                            ) {
                                // Icon with gradient background
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFFEF4444).copy(alpha = 0.25f),
                                                    Color(0xFFDC2626).copy(alpha = 0.15f)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Block,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Block User",
                                        color = Color(0xFFF87171),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Hide all their content",
                                        color = Color(0xFFEF4444).copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = {
                            showMenu = false
                            onBlock()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpinningNeonRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(38.dp)
            .offset(x = (-3).dp, y = (-3).dp)
            .rotate(rotation)
            .clip(CircleShape)
            .background(
                Brush.sweepGradient(
                    colors = listOf(
                        Color.Transparent,
                        CyanGlow,
                        Color(0xFF3B82F6) // Blue
                    )
                )
            )
    )
}

@Composable
private fun ActionCapsuleRow(
    post: Post,
    isLiked: Boolean,
    isSaved: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Action Capsule
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(CardBg)
                .border(1.dp, BorderWhite10, RoundedCornerShape(28.dp))
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like Button + Count
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isLiked) Color.White.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable { onLike() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFEF4444) else Color(0xFFE4E4E7),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = formatCount(post.likesCount),
                    color = if (isLiked) Color.White else Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Divider
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .height(24.dp)
                    .background(Color(0xFF3F3F46).copy(alpha = 0.8f))
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Comment
            Row(
                modifier = Modifier
                    .clickable { onComment() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = formatCount(post.commentsCount),
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share
            Row(
                modifier = Modifier
                    .clickable { onShare() }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = formatCount(post.sharesCount),
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Views
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Views",
                    tint = Color(0xFFA1A1AA),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = formatCount(post.viewsCount),
                    color = Color(0xFFA1A1AA),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Detached Bookmark Circle (Save)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSaved) IndigoAccent.copy(alpha = 0.1f) else CardBg)
                .border(
                    1.dp,
                    if (isSaved) IndigoAccent.copy(alpha = 0.3f) else BorderWhite10,
                    CircleShape
                )
                .clickable { onSave() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = "Save",
                tint = if (isSaved) IndigoAccent else Color(0xFFA1A1AA),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CaptionSection(
    username: String,
    caption: String,
    timeAgo: String
) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Caption Text
        Text(
            text = buildString {
                append(username)
                append("  ")
                append(caption)
            },
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 20.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // Time
        Text(
            text = timeAgo.uppercase(),
            color = Color(0xFF71717A),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
    }
}

// Helper functions
private fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}k"
        else -> count.toString()
    }
}
