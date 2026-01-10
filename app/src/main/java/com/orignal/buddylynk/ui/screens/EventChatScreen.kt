package com.orignal.buddylynk.ui.screens

import android.content.Intent
import android.net.Uri
// Toast removed - using PremiumPopup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.api.ApiService
import com.orignal.buddylynk.data.auth.AuthManager
import com.orignal.buddylynk.ui.viewmodel.EventChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Colors
private val DarkBg = Color(0xFF050505)
private val ZincDark = Color(0xFF0A0A0A)
private val Zinc800 = Color(0xFF27272A)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc600 = Color(0xFF52525B)
private val Zinc500 = Color(0xFF71717A)
private val Zinc400 = Color(0xFFA1A1AA)
private val CyanAccent = Color(0xFF22D3EE)
private val PurpleAccent = Color(0xFF8B5CF6)
private val PinkAccent = Color(0xFFEC4899)
private val OrangeAccent = Color(0xFFF97316)

/**
 * Event Chat Screen - Chat interface for a specific event
 * Features: Empty state, settings navigation via header, chat permissions, media upload
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventChatScreen(
    eventId: String,
    eventTitle: String,
    eventDate: String,
    eventTime: String,
    eventLocation: String,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    viewModel: EventChatViewModel = viewModel()
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Media state
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isUploadingMedia by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    
    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedMediaUri = uri
    }
    
    // Gallery picker for multiple types
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedMediaUri = uri
    }
    
    // Collect state from ViewModel
    val messages by viewModel.messages.collectAsState()
    val canSendMessages by viewModel.canSendMessages.collectAsState()
    val event by viewModel.event.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Load data on first composition
    LaunchedEffect(eventId) {
        viewModel.loadEvent(eventId)
        viewModel.loadMessages(eventId)
    }
    
    // Check if chat is disabled for display
    val chatEnabled = event?.chatEnabled ?: true
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    
    // Function to upload media and send message
    fun uploadAndSendMedia() {
        val uri = selectedMediaUri ?: return
        isUploadingMedia = true
        
        coroutineScope.launch {
            try {
                // Get content type
                val contentType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val isVideo = contentType.startsWith("video/")
                val extension = if (isVideo) "mp4" else "jpg"
                val filename = "event_${eventId}_${System.currentTimeMillis()}.$extension"
                
                // Get presigned URL
                val presignedResult = withContext(Dispatchers.IO) {
                    ApiService.getPresignedUrl(filename, contentType, "events")
                }
                
                presignedResult.onSuccess { presignedJson ->
                    val uploadUrl = presignedJson.optString("uploadUrl").ifEmpty { presignedJson.optString("presignedUrl") }
                    val fileUrl = presignedJson.optString("fileUrl")
                    
                    if (uploadUrl.isEmpty()) {
                        isUploadingMedia = false
                        return@onSuccess
                    }
                    
                    // Upload to S3 using streaming to prevent OOM
                    val uploadResult = withContext(Dispatchers.IO) {
                        ApiService.uploadToPresignedUrl(uploadUrl, uri, context.contentResolver, contentType)
                    }
                    
                    uploadResult.onSuccess {
                        // Send message with media URL
                        viewModel.sendMessageWithMedia(eventId, messageInput, fileUrl, if (isVideo) "video" else "image")
                        selectedMediaUri = null
                        messageInput = ""
                        // Silent success - media appears in chat
                    }.onFailure {
                        // Silent failure - user can retry
                    }
                }.onFailure {
                    // Silent failure - user can retry
                }
            } catch (e: Exception) {
                // Silent error handling
            } finally {
                isUploadingMedia = false
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Background pattern
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.02f))
        )
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with event info - clickable to open settings
            EventChatHeader(
                eventTitle = eventTitle,
                eventDate = eventDate,
                eventTime = eventTime,
                eventLocation = eventLocation,
                onBack = onNavigateBack,
                onSettingsClick = onNavigateToSettings
            )
            
            // Chat disabled banner
            if (!chatEnabled && currentUserRole == "member") {
                ChatDisabledBanner()
            }
            
            // Messages
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Event Welcome Banner
                item {
                    EventWelcomeBanner(
                        title = eventTitle,
                        date = eventDate,
                        time = eventTime,
                        location = eventLocation
                    )
                }
                
                // Empty state when no messages
                if (messages.isEmpty() && !isLoading) {
                    item {
                        EmptyMessagesState()
                    }
                }
                
                // Messages
                items(
                    items = messages,
                    key = { it.messageId }
                ) { message ->
                    EventMessageBubble(message = message)
                }
                
                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
        
        // Scroll to bottom button
        if (messages.isNotEmpty()) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, bottom = 160.dp)
                    .size(40.dp)
                    .background(Zinc800.copy(alpha = 0.9f), CircleShape)
                    .border(1.dp, OrangeAccent.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Scroll to bottom",
                    tint = OrangeAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Media preview (if selected)
        if (selectedMediaUri != null) {
            MediaPreviewOverlay(
                mediaUri = selectedMediaUri!!,
                onSend = { uploadAndSendMedia() },
                onCancel = { selectedMediaUri = null },
                isUploading = isUploadingMedia,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        
        // Input bar - only show if can send messages, moved up with more padding (32dp)
        if (canSendMessages && selectedMediaUri == null) {
            EventInputBar(
                value = messageInput,
                onValueChange = { messageInput = it },
                onSend = {
                    if (messageInput.isNotBlank()) {
                        viewModel.sendMessage(eventId, messageInput)
                        messageInput = ""
                        coroutineScope.launch {
                            if (messages.isNotEmpty()) {
                                listState.animateScrollToItem(messages.size - 1)
                            }
                        }
                    }
                },
                onAttach = { showAttachmentMenu = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.ime)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 32.dp) // Increased to 32dp
            )
        } else if (!canSendMessages) {
            // Read-only mode indicator
            ReadOnlyModeIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 16.dp, vertical = 32.dp)
            )
        }
        
        // Attachment menu
        if (showAttachmentMenu) {
            AttachmentMenuSheet(
                onDismiss = { showAttachmentMenu = false },
                onSelectImage = {
                    showAttachmentMenu = false
                    imagePickerLauncher.launch("image/*")
                },
                onSelectVideo = {
                    showAttachmentMenu = false
                    galleryPickerLauncher.launch("video/*")
                },
                onSelectFile = {
                    showAttachmentMenu = false
                    galleryPickerLauncher.launch("*/*")
                }
            )
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
private fun AttachmentMenuSheet(
    onDismiss: () -> Unit,
    onSelectImage: () -> Unit,
    onSelectVideo: () -> Unit,
    onSelectFile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc800)
                .clickable(enabled = false) {} // Prevent click-through
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Send Attachment",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Image
                AttachmentOption(
                    icon = Icons.Outlined.Image,
                    label = "Image",
                    color = OrangeAccent,
                    onClick = onSelectImage
                )
                
                // Video
                AttachmentOption(
                    icon = Icons.Outlined.Videocam,
                    label = "Video",
                    color = PinkAccent,
                    onClick = onSelectVideo
                )
                
                // File
                AttachmentOption(
                    icon = Icons.Outlined.InsertDriveFile,
                    label = "File",
                    color = CyanAccent,
                    onClick = onSelectFile
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Zinc400,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = Zinc400
        )
    }
}

@Composable
private fun MediaPreviewOverlay(
    mediaUri: Uri,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    isUploading: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Zinc800)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            // Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkBg),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = mediaUri,
                    contentDescription = "Selected media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                
                // Cancel button
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Send button
            Button(
                onClick = onSend,
                enabled = !isUploading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangeAccent
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Uploading...", color = Color.White)
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EventChatHeader(
    eventTitle: String,
    eventDate: String,
    eventTime: String,
    eventLocation: String,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ZincDark.copy(alpha = 0.95f))
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFFD4D4D8),
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Clickable event info area - opens settings
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onSettingsClick)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Event icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(OrangeAccent.copy(alpha = 0.3f), PinkAccent.copy(alpha = 0.3f))
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Event,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Event name and info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = eventTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "EVENT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = OrangeAccent,
                            modifier = Modifier
                                .background(OrangeAccent.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = "$eventDate • $eventTime",
                        fontSize = 12.sp,
                        color = Zinc400,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Settings icon
            IconButton(onClick = onSettingsClick) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Event Settings",
                    tint = Zinc400,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .align(Alignment.BottomCenter)
                .background(Color.White.copy(alpha = 0.1f))
        )
    }
}

@Composable
private fun ChatDisabledBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrangeAccent.copy(alpha = 0.15f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = OrangeAccent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Chat is currently disabled by admins",
                fontSize = 13.sp,
                color = OrangeAccent
            )
        }
    }
}

@Composable
private fun EmptyMessagesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Empty state icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Zinc800.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = Zinc500,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No messages yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = Zinc400
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Be the first to start the conversation!",
            fontSize = 14.sp,
            color = Zinc500,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ReadOnlyModeIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Zinc800.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = Zinc500,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Only admins can send messages",
                fontSize = 14.sp,
                color = Zinc500
            )
        }
    }
}

@Composable
private fun EventWelcomeBanner(
    title: String,
    date: String,
    time: String,
    location: String,
    description: String = ""
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Event Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        listOf(OrangeAccent, PinkAccent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Celebration,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Event details chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Date chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Zinc800)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = OrangeAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$date • $time",
                        fontSize = 12.sp,
                        color = Zinc400
                    )
                }
            }
            
            // Location chip
            if (location.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Zinc800)
                        .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = PinkAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = location,
                            fontSize = 12.sp,
                            color = Zinc400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        
        // Description (if available)
        if (description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = Zinc500,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to the event discussion! 🎊",
            fontSize = 14.sp,
            color = Zinc500
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Add to Calendar button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(CyanAccent.copy(alpha = 0.15f))
                .border(1.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable {
                    com.orignal.buddylynk.data.util.CalendarUtils.addEventToCalendar(
                        context = context,
                        title = title,
                        description = description,
                        location = location,
                        date = date,
                        time = time
                    )
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Add to Calendar",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = CyanAccent
            )
        }
    }
}

@Composable
private fun EventMessageBubble(message: EventChatViewModel.EventMessage) {
    val currentUserId = AuthManager.currentUser.value?.userId
    val isMine = message.senderId == currentUserId
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        // Avatar for others
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(PurpleAccent.copy(alpha = 0.5f), PinkAccent.copy(alpha = 0.5f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (message.senderAvatar != null) {
                    AsyncImage(
                        model = message.senderAvatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = message.senderName.take(1).uppercase(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            // Sender name
            if (!isMine) {
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Zinc400,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            
            // Media message
            if (!message.mediaUrl.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isMine) OrangeAccent.copy(alpha = 0.15f) else Color(0xFF1E1E22))
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        .padding(4.dp)
                ) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Media",
                        modifier = Modifier
                            .widthIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
            }
            
            // Text message with clickable links
            if (message.content.isNotEmpty()) {
                val context = LocalContext.current
                val textColor = if (isMine) Color.Black else Color.White
                val linkColor = if (isMine) Color.Blue else CyanAccent
                
                // Create annotated string with clickable URLs
                val annotatedText = buildAnnotatedString {
                    val urlPattern = "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)".toRegex()
                    val text = message.content
                    var lastIndex = 0
                    
                    urlPattern.findAll(text).forEach { matchResult ->
                        // Add text before the URL
                        if (matchResult.range.first > lastIndex) {
                            append(text.substring(lastIndex, matchResult.range.first))
                        }
                        // Add the URL with annotation
                        pushStringAnnotation(tag = "URL", annotation = matchResult.value)
                        withStyle(style = SpanStyle(
                            color = linkColor,
                            textDecoration = TextDecoration.Underline
                        )) {
                            append(matchResult.value)
                        }
                        pop()
                        lastIndex = matchResult.range.last + 1
                    }
                    // Add remaining text after last URL
                    if (lastIndex < text.length) {
                        append(text.substring(lastIndex))
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(
                                topStart = 20.dp,
                                topEnd = 20.dp,
                                bottomStart = if (isMine) 20.dp else 0.dp,
                                bottomEnd = if (isMine) 0.dp else 20.dp
                            )
                        )
                        .background(
                            if (isMine) OrangeAccent.copy(alpha = 0.85f)
                            else Color(0xFF1E1E22)
                        )
                        .then(
                            if (!isMine) Modifier.border(
                                0.5.dp,
                                Color.White.copy(alpha = 0.03f),
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp)
                            ) else Modifier
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Column {
                        ClickableText(
                            text = annotatedText,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = textColor,
                                lineHeight = 20.sp
                            ),
                            onClick = { offset ->
                                annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                    .firstOrNull()?.let { annotation ->
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Silent link open failure
                                        }
                                    }
                            }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatMessageTime(message.createdAt),
                            fontSize = 10.sp,
                            color = if (isMine) Color.Black.copy(alpha = 0.6f) else Zinc500
                        )
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(createdAt: String): String {
    // Simple time formatting - in production use proper date parsing
    return try {
        val parts = createdAt.split("T")
        if (parts.size > 1) {
            parts[1].take(5) // Get HH:mm
        } else {
            "Now"
        }
    } catch (e: Exception) {
        "Now"
    }
}

@Composable
private fun EventInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttach: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Zinc800)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Attachment button
        IconButton(
            onClick = onAttach,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Zinc700)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = "Attach Media",
                tint = OrangeAccent,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Text input
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            textStyle = TextStyle(
                fontSize = 15.sp,
                color = Color.White
            ),
            cursorBrush = SolidColor(OrangeAccent),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Message...",
                            fontSize = 15.sp,
                            color = Zinc500
                        )
                    }
                    innerTextField()
                }
            }
        )
        
        // Emoji button when empty
        if (value.isEmpty()) {
            IconButton(
                onClick = { },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Outlined.EmojiEmotions,
                    contentDescription = "Emoji",
                    tint = Zinc500,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        // Send button
        AnimatedVisibility(
            visible = value.isNotBlank(),
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Brush.linearGradient(listOf(OrangeAccent, PinkAccent)),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
