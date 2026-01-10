package com.orignal.buddylynk.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.orignal.buddylynk.ui.viewmodel.*

// Premium Colors
private val PremiumPurple = Color(0xFF8B5CF6)
private val PremiumIndigo = Color(0xFF6366F1)
private val PremiumPink = Color(0xFFEC4899)
private val DarkBg = Color(0xFF000000)
private val CardBg = Color(0xFF18181B)
private val BorderColor = Color(0xFF27272A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreatePostViewModel = viewModel()
) {
    val context = LocalContext.current
    val imageUri by viewModel.selectedImageUri.collectAsState()
    val mediaItems by viewModel.mediaItems.collectAsState()
    val caption by viewModel.caption.collectAsState()
    val location by viewModel.location.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val adjustments by viewModel.adjustments.collectAsState()
    val uploadMode by viewModel.uploadMode.collectAsState()
    val editTab by viewModel.editTab.collectAsState()
    val postState by viewModel.postState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val overallProgress by viewModel.overallProgress.collectAsState()
    val currentUploadingIndex by viewModel.currentUploadingIndex.collectAsState()

    // Multi-media picker (up to 20 images/videos) - Supports both photos and videos
    val multiMediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 20)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addMediaItems(uris, context)
        }
    }
    
    // Legacy single image picker (fallback)
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addMediaItems(listOf(it), context) }
    }

    // Success State
    if (postState == PostState.SUCCESS) {
        SuccessScreen(onDismiss = {
            viewModel.clearSelection()
            onNavigateBack()
        })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Ambient Background Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .offset(y = (-100).dp)
                .blur(100.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PremiumIndigo.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top Navigation Bar
            TopNavBar(
                hasImage = imageUri != null,
                isPosting = postState == PostState.UPLOADING,
                onBackClick = {
                    if (imageUri != null) viewModel.clearSelection()
                    else onNavigateBack()
                },
                onShareClick = { viewModel.createPost(context) }
            )

            // Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                if (mediaItems.isEmpty()) {
                    // Upload Mode Selection - Multi-media picker
                    UploadModeSelector(
                        currentMode = uploadMode,
                        onModeChange = { viewModel.setUploadMode(it) },
                        onSelectMedia = { 
                            multiMediaPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                    )
                } else {
                    // Multi-Media Editor with Progress
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Upload Progress Bar (when uploading)
                        if (postState == PostState.UPLOADING) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(CardBg)
                                    .padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = PremiumIndigo,
                                        strokeWidth = 2.dp
                                    )
                                    Text(
                                        text = "Uploading ${currentUploadingIndex + 1}/${mediaItems.size}...",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${overallProgress.toInt()}%",
                                        color = PremiumIndigo,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { overallProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = PremiumIndigo,
                                    trackColor = BorderColor
                                )
                            }
                        }

                        // Error Message
                        if (errorMessage != null && postState == PostState.ERROR) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF7F1D1D))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Error, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Text(errorMessage ?: "Error", color = Color.White, fontSize = 13.sp)
                            }
                        }

                        // Media Thumbnails Row (scrollable)
                        Text(
                            text = "${mediaItems.size}/${CreatePostViewModel.MAX_MEDIA_ITEMS} media selected",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(mediaItems.size) { index ->
                                val item = mediaItems[index]
                                MediaThumbnail(
                                    item = item,
                                    index = index,
                                    isCurrentlyUploading = currentUploadingIndex == index,
                                    onRemove = { viewModel.removeMediaItem(index) },
                                    onMoveUp = { viewModel.moveMediaUp(index) },
                                    onMoveDown = { viewModel.moveMediaDown(index) },
                                    onRetry = { viewModel.retryUpload(index, context) },
                                    canMoveUp = index > 0,
                                    canMoveDown = index < mediaItems.size - 1
                                )
                            }
                            
                            // Add More button
                            if (mediaItems.size < CreatePostViewModel.MAX_MEDIA_ITEMS) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardBg)
                                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                            .clickable { 
                                                multiMediaPicker.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                                )
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Filled.Add,
                                            contentDescription = "Add more",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Main Preview (first media item)
                        val firstMedia = mediaItems.firstOrNull()
                        if (firstMedia != null) {
                            ImageEditorContent(
                                mediaItem = firstMedia,
                                selectedFilter = selectedFilter,
                                adjustments = adjustments,
                                filters = viewModel.filters,
                                editTab = editTab,
                                caption = caption,
                                location = location,
                                onFilterSelect = { viewModel.setFilter(it) },
                                onTabChange = { viewModel.setEditTab(it) },
                                onBrightnessChange = { viewModel.setBrightness(it) },
                                onContrastChange = { viewModel.setContrast(it) },
                                onSaturationChange = { viewModel.setSaturation(it) },
                                onCaptionChange = { viewModel.setCaption(it) },
                                onLocationChange = { viewModel.setLocation(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Media Thumbnail with remove, reorder, and status indicators
@Composable
private fun MediaThumbnail(
    item: MediaItem,
    index: Int,
    isCurrentlyUploading: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRetry: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean
) {
    val context = LocalContext.current
    val isVideo = item.mediaType == "video"
    
    Box(
        modifier = Modifier.size(80.dp)
    ) {
        // Thumbnail image - use VideoFrameDecoder for videos
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .apply {
                    if (isVideo) {
                        decoderFactory { result, options, _ ->
                            VideoFrameDecoder(result.source, options)
                        }
                        videoFrameMillis(1000) // Get frame at 1 second
                    }
                }
                .crossfade(true)
                .build(),
            contentDescription = "Media ${index + 1}",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .border(
                    width = if (item.uploadState == MediaUploadState.SUCCESS) 2.dp else 1.dp,
                    color = when (item.uploadState) {
                        MediaUploadState.SUCCESS -> Color(0xFF22C55E)
                        MediaUploadState.FAILED -> Color(0xFFEF4444)
                        MediaUploadState.UPLOADING -> PremiumIndigo
                        else -> BorderColor
                    },
                    shape = RoundedCornerShape(12.dp)
                ),
            contentScale = ContentScale.Crop
        )
        
        // Video indicator overlay
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Video",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Index badge
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.7f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(22.dp)
                .background(Color.Black.copy(alpha = 0.7f), CircleShape)
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "Remove",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
        
        // Upload status overlay
        when (item.uploadState) {
            MediaUploadState.UPLOADING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
            }
            MediaUploadState.FAILED -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .clickable { onRetry() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Retry",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            MediaUploadState.SUCCESS -> {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Done",
                    tint = Color(0xFF22C55E),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(18.dp)
                )
            }
            else -> {}
        }
        
        // Reorder controls (shown at bottom)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.6f)),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (canMoveUp) {
                IconButton(
                    onClick = onMoveUp,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowLeft,
                        contentDescription = "Move left",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (canMoveDown) {
                IconButton(
                    onClick = onMoveDown,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Move right",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TopNavBar(
    hasImage: Boolean,
    isPosting: Boolean,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val currentUser = com.orignal.buddylynk.data.auth.AuthManager.currentUser.collectAsState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(60.dp)
            .background(DarkBg.copy(alpha = 0.8f))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Back Button + User Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = if (hasImage) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Close,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            // User Profile Avatar (like React design)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PremiumIndigo, PremiumPink)
                        )
                    )
                    .padding(1.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(DarkBg),
                    contentAlignment = Alignment.Center
                ) {
                    val avatar = currentUser.value?.avatar
                    if (avatar != null) {
                        AsyncImage(
                            model = avatar,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Center: Title (only when no image)
        if (!hasImage) {
            Text(
                text = "New Post",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Right: Share Button
        AnimatedVisibility(
            visible = hasImage,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Button(
                onClick = onShareClick,
                enabled = !isPosting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(24.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                } else {
                    Text("Share", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Spacer when no image
        if (!hasImage) {
            Spacer(modifier = Modifier.width(48.dp))
        }
    }
}

@Composable
private fun UploadModeSelector(
    currentMode: UploadMode,
    onModeChange: (UploadMode) -> Unit,
    onSelectMedia: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Mode Tabs
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeTab(
                icon = Icons.Outlined.Folder,
                isSelected = currentMode == UploadMode.FILES,
                onClick = { onModeChange(UploadMode.FILES) }
            )
            ModeTab(
                icon = Icons.Outlined.CameraAlt,
                isSelected = currentMode == UploadMode.CAMERA,
                onClick = { onModeChange(UploadMode.CAMERA) }
            )
            ModeTab(
                icon = Icons.Outlined.Event,
                isSelected = currentMode == UploadMode.EVENT,
                onClick = { onModeChange(UploadMode.EVENT) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Mode Content
        when (currentMode) {
            UploadMode.FILES -> FilesUploadCard(onSelectMedia = onSelectMedia)
            UploadMode.CAMERA -> CameraCard()
            UploadMode.EVENT -> EventCard()
        }
    }
}

@Composable
private fun ModeTab(
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (isSelected) CardBg.copy(alpha = 0.8f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) PremiumIndigo else Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FilesUploadCard(onSelectMedia: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(32.dp))
            .background(CardBg.copy(alpha = 0.3f))
            .border(1.dp, BorderColor, RoundedCornerShape(32.dp))
            .clickable(onClick = onSelectMedia),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Icons for both photo and video
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                        .border(1.dp, BorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = PremiumIndigo,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                        .border(1.dp, BorderColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = PremiumPink,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Select Photos & Videos",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to select multiple media",
                color = Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Up to 20 items per post",
                color = PremiumIndigo.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun CameraCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black)
            .border(1.dp, BorderColor, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = null,
                tint = Color.Gray.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Camera Mode", color = Color.Gray, fontSize = 16.sp)
        }
    }
}

@Composable
private fun EventCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 5f)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(CardBg.copy(alpha = 0.8f), DarkBg)
                )
            )
            .border(1.dp, BorderColor, RoundedCornerShape(32.dp))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PremiumIndigo.copy(alpha = 0.1f))
                    .border(1.dp, PremiumIndigo.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Event,
                    contentDescription = null,
                    tint = PremiumIndigo,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "New Event",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Host a meetup, party, or\nlive session for your community.",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Create Event",
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageEditorContent(
    mediaItem: MediaItem,
    selectedFilter: ImageFilter,
    adjustments: ImageAdjustments,
    filters: List<ImageFilter>,
    editTab: EditTab,
    caption: String,
    location: String,
    onFilterSelect: (ImageFilter) -> Unit,
    onTabChange: (EditTab) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onCaptionChange: (String) -> Unit,
    onLocationChange: (String) -> Unit
) {
    val context = LocalContext.current
    val isVideo = mediaItem.mediaType == "video"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Image/Video Preview with Filters
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .clip(RoundedCornerShape(32.dp))
                .background(CardBg)
        ) {
            // Use VideoFrameDecoder for videos
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaItem.uri)
                    .apply {
                        if (isVideo) {
                            decoderFactory { result, options, _ ->
                                VideoFrameDecoder(result.source, options)
                            }
                            videoFrameMillis(1000) // Get frame at 1 second
                        }
                    }
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = if (!isVideo) getColorFilter(selectedFilter, adjustments) else null
            )
            
            // Play icon overlay for videos
            if (isVideo) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(72.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            // Maximize Button (like React design)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                    .clickable { /* TODO: Fullscreen */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Tab Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg.copy(alpha = 0.8f))
                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                EditTabButton(
                    icon = Icons.Outlined.AutoAwesome,
                    label = "Filters",
                    isSelected = editTab == EditTab.FILTER,
                    onClick = { onTabChange(EditTab.FILTER) }
                )
                EditTabButton(
                    icon = Icons.Outlined.Tune,
                    label = "Edit",
                    isSelected = editTab == EditTab.EDIT,
                    onClick = { onTabChange(EditTab.EDIT) }
                )
            }
        }

        // Filter/Edit Content
        AnimatedContent(
            targetState = editTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "editTab"
        ) { tab ->
            // Filters/adjustments only apply to images, not videos
            if (!isVideo) {
                when (tab) {
                    EditTab.FILTER -> FilterSelector(
                        filters = filters,
                        selectedFilter = selectedFilter,
                        imageUri = mediaItem.uri,
                        adjustments = adjustments,
                        onSelect = onFilterSelect
                    )
                    EditTab.EDIT -> AdjustmentsPanel(
                        adjustments = adjustments,
                        onBrightnessChange = onBrightnessChange,
                        onContrastChange = onContrastChange,
                        onSaturationChange = onSaturationChange
                    )
                }
            }
        }

        // Caption Input
        CaptionInput(
            caption = caption,
            onCaptionChange = onCaptionChange
        )

        // Metadata Options
        MetadataOptions(
            location = location,
            onLocationChange = onLocationChange
        )
    }
}

@Composable
private fun EditTabButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) CardBg else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) Color.White else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun FilterSelector(
    filters: List<ImageFilter>,
    selectedFilter: ImageFilter,
    imageUri: Uri,
    adjustments: ImageAdjustments,
    onSelect: (ImageFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(filters) { filter ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSelect(filter) }
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(
                            width = if (selectedFilter == filter) 2.dp else 0.dp,
                            color = if (selectedFilter == filter) PremiumIndigo else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = getColorFilter(filter, adjustments)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = filter.name,
                    color = if (selectedFilter == filter) PremiumIndigo else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AdjustmentsPanel(
    adjustments: ImageAdjustments,
    onBrightnessChange: (Float) -> Unit,
    onContrastChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBg.copy(alpha = 0.4f))
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        AdjustmentSlider(
            label = "Brightness",
            icon = Icons.Outlined.WbSunny,
            value = adjustments.brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0.5f..1.5f
        )
        AdjustmentSlider(
            label = "Contrast",
            icon = Icons.Outlined.Contrast,
            value = adjustments.contrast,
            onValueChange = onContrastChange,
            valueRange = 0.5f..1.5f
        )
        AdjustmentSlider(
            label = "Saturation",
            icon = Icons.Outlined.WaterDrop,
            value = adjustments.saturation,
            onValueChange = onSaturationChange,
            valueRange = 0f..2f
        )
    }
}

@Composable
private fun AdjustmentSlider(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(label, color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                text = "${(value * 100).toInt()}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(CardBg, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun CaptionInput(
    caption: String,
    onCaptionChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg.copy(alpha = 0.3f))
            .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        BasicTextField(
            value = caption,
            onValueChange = onCaptionChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            cursorBrush = SolidColor(PremiumPurple),
            decorationBox = { innerTextField ->
                Box {
                    if (caption.isEmpty()) {
                        Text(
                            "Write a caption...",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Tag, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.EmojiEmotions, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun MetadataOptions(
    location: String,
    onLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MetadataButton(
            icon = Icons.Outlined.LocationOn,
            label = location.ifEmpty { "Add Location" },
            isActive = location.isNotEmpty(),
            onClick = { if (location.isEmpty()) onLocationChange("New York, USA") },
            onClear = { onLocationChange("") }
        )
        MetadataButton(
            icon = Icons.Outlined.MusicNote,
            label = "Add Music",
            isActive = false,
            onClick = { }
        )
        MetadataButton(
            icon = Icons.Outlined.MoreHoriz,
            label = "Advanced Settings",
            isActive = false,
            onClick = { }
        )
    }
}

@Composable
private fun MetadataButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg.copy(alpha = 0.2f))
            .border(1.dp, Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isActive) PremiumIndigo.copy(alpha = 0.2f) else CardBg.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) PremiumIndigo else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                color = if (isActive) Color.White else Color.Gray,
                fontSize = 14.sp,
                fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
            )
        }
        if (isActive && onClear != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            }
        } else {
            Text(
                "Add",
                color = Color.Gray.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SuccessScreen(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2500)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        // Background Glow
        Box(
            modifier = Modifier
                .size(256.dp)
                .blur(120.dp)
                .background(PremiumIndigo.copy(alpha = 0.3f), CircleShape)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PremiumIndigo, PremiumPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Uploaded",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your moment has been shared.",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}

// Helper function for color filters
private fun getColorFilter(filter: ImageFilter, adjustments: ImageAdjustments): ColorFilter {
    val colorMatrix = ColorMatrix()

    // Apply adjustments
    colorMatrix.setToScale(
        adjustments.brightness,
        adjustments.brightness,
        adjustments.brightness,
        1f
    )

    // Apply saturation from adjustments
    val satMatrix = ColorMatrix()
    satMatrix.setToSaturation(adjustments.saturation * filter.saturation)
    colorMatrix.timesAssign(satMatrix)

    // Apply grayscale if filter requires it
    if (filter.grayscale) {
        val grayMatrix = ColorMatrix()
        grayMatrix.setToSaturation(0f)
        colorMatrix.timesAssign(grayMatrix)
    }

    return ColorFilter.colorMatrix(colorMatrix)
}
