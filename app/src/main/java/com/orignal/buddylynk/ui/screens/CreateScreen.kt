package com.orignal.buddylynk.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.data.repository.BackendRepository
import com.orignal.buddylynk.data.aws.S3Service
import com.orignal.buddylynk.data.model.Post
import com.orignal.buddylynk.ui.components.*
import com.orignal.buddylynk.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLive: () -> Unit = {},
    onNavigateToEvents: () -> Unit = {},
    onNavigateToAddStory: () -> Unit = {}
) {
    var postText by remember { mutableStateOf("") }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var mediaType by remember { mutableStateOf<String?>(null) } // "image" or "video"
    var isPosting by remember { mutableStateOf(false) }
    var postSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by AuthManager.currentUser.collectAsState()
    
    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            mediaType = "image"
        }
    }
    
    // Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            mediaType = "video"
        }
    }
    
    // Any media picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedMediaUri = it
            // Determine type from URI
            val mimeType = context.contentResolver.getType(it)
            mediaType = when {
                mimeType?.startsWith("image") == true -> "image"
                mimeType?.startsWith("video") == true -> "video"
                else -> "image"
            }
        }
    }
    
    // Camera capture - uses image picker as fallback since camera needs more setup
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // For production, save bitmap to URI and set selectedMediaUri
        // For now, open image picker as camera alternative
        if (bitmap != null) {
            mediaType = "image"
            // Bitmap captured - in production, save to file and use URI
        }
    }
    
    AnimatedGradientBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextPrimary
                    )
                }
                
                Text(
                    text = "Create Post",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                
                // Post button
                GradientButton(
                    text = when {
                        isPosting -> "Posting..."
                        postSuccess -> "Posted! ✓"
                        else -> "Post"
                    },
                    onClick = {
                        if (postText.isNotBlank() || selectedMediaUri != null) {
                            isPosting = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val postId = UUID.randomUUID().toString()
                                    var mediaUrl: String? = null
                                    
                                    // Upload media if selected
                                    selectedMediaUri?.let { uri ->
                                        mediaUrl = S3Service.uploadPostMedia(
                                            context = context,
                                            postId = postId,
                                            mediaUri = uri,
                                            isVideo = mediaType == "video"
                                        )
                                    }
                                    
                                    // Create post
                                    val post = Post(
                                        postId = postId,
                                        userId = currentUser?.userId ?: "",
                                        username = currentUser?.username,
                                        userAvatar = currentUser?.avatar,
                                        content = postText,
                                        mediaUrl = mediaUrl,
                                        mediaType = mediaType,
                                        createdAt = System.currentTimeMillis().toString()
                                    )
                                    
                                    val success = BackendRepository.createPost(post)
                                    isPosting = false
                                    
                                    if (success) {
                                        postSuccess = true
                                        kotlinx.coroutines.delay(1000)
                                        onNavigateBack()
                                    } else {
                                        errorMessage = "Failed to create post"
                                    }
                                } catch (e: Exception) {
                                    isPosting = false
                                    errorMessage = "Error: ${e.message}"
                                }
                            }
                        } else {
                            errorMessage = "Add some text or media"
                        }
                    },
                    enabled = (postText.isNotBlank() || selectedMediaUri != null) && !isPosting,
                    gradient = if (postSuccess) listOf(AccentGreen, AccentGreen) else PremiumGradient
                )
            }
            
            // Error message
            errorMessage?.let {
                Text(
                    text = it,
                    color = LikeRed,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Content area - enhanced design
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 16.dp),
                cornerRadius = 24.dp,
                glassOpacity = 0.1f
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // User info with gradient ring
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Avatar with gradient ring
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.sweepGradient(VibrantGradient)
                                    )
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.linearGradient(PremiumGradient)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentUser?.username?.firstOrNull()?.uppercase() ?: "U",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.username ?: "User",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            // Visibility selector
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Public,
                                    contentDescription = null,
                                    tint = GradientCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Public",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GradientCyan,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    tint = GradientCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Text input with better styling
                    OutlinedTextField(
                        value = postText,
                        onValueChange = { postText = it },
                        placeholder = {
                            Text(
                                text = "What's on your mind?",
                                color = TextTertiary,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = GradientCyan,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Media preview
                    AnimatedVisibility(
                        visible = selectedMediaUri != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            if (mediaType == "image") {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(selectedMediaUri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Selected image",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Video placeholder
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(GradientPurple.copy(alpha = 0.3f), GradientCyan.copy(alpha = 0.3f))
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PlayCircle,
                                            contentDescription = null,
                                            tint = TextPrimary,
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Video Selected",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                            
                            // Remove media button
                            IconButton(
                                onClick = { 
                                    selectedMediaUri = null
                                    mediaType = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(DarkBackground.copy(alpha = 0.7f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Remove",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom spacer to keep content above bottom nav bar
            Spacer(modifier = Modifier.height(8.dp))
            
            // Stories, Live & Events Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Add Story Button
                AnimatedQuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.AddCircle,
                    label = "Story",
                    gradientColors = listOf(GradientPurple, GradientPink),
                    onClick = onNavigateToAddStory
                )
                
                // Live Button
                AnimatedQuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Videocam,
                    label = "Live",
                    gradientColors = listOf(GradientCoral, GradientOrange),
                    onClick = onNavigateToLive
                )
                
                // Events Button
                AnimatedQuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Event,
                    label = "Events",
                    gradientColors = listOf(GradientTeal, GradientCyan),
                    onClick = onNavigateToEvents
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action bar - Media buttons
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                cornerRadius = 16.dp,
                glassOpacity = 0.12f
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MediaActionButton(
                        icon = Icons.Filled.Image,
                        label = "Photo",
                        color = GradientCoral,
                        onClick = { imagePickerLauncher.launch("image/*") }
                    )
                    MediaActionButton(
                        icon = Icons.Filled.Videocam,
                        label = "Video",
                        color = GradientTeal,
                        onClick = { videoPickerLauncher.launch("video/*") }
                    )
                    MediaActionButton(
                        icon = Icons.Filled.Attachment,
                        label = "File",
                        color = GradientPurple,
                        onClick = { mediaPickerLauncher.launch("*/*") }
                    )
                    MediaActionButton(
                        icon = Icons.Filled.CameraAlt,
                        label = "Camera",
                        color = GradientBlue,
                        onClick = { cameraLauncher.launch(null) }
                    )
                }
            }
            
            // Spacer for bottom nav bar - increased to prevent overlap
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
private fun MediaActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        // Rounded square button with gradient
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0.8f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Animated Quick Action Card with pulse animation for Live/Events
 */
@Composable
private fun AnimatedQuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    // Pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )
    
    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAnim"
    )
    
    // Icon rotation
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotateAnim"
    )
    
    Box(
        modifier = modifier
            .scale(pulse)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(gradientColors.map { it.copy(alpha = glowAlpha) })
            )
            .clickable(onClick = onClick)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(
                    brush = Brush.linearGradient(gradientColors)
                )
                .padding(vertical = 16.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            rotationZ = iconRotation
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

