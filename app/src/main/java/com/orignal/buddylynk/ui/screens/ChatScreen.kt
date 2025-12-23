package com.orignal.buddylynk.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.model.Message
import com.orignal.buddylynk.ui.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.*

import com.orignal.buddylynk.data.calls.CallManager

// Locally defined colors to avoid import issues
private val LocalPremiumGradient = listOf(Color(0xFFFF6B6B), Color(0xFFFF8E72))
private val LocalGradientCyan = Color(0xFF4ECDC4)
private val LocalGradientPurple = Color(0xFF9B51E0)
private val LocalAccentGreen = Color(0xFF26DE81)
private val LocalTextTertiary = Color(0xFF5C5C66)

/**
 * ChatScreen - BuddyLynk Style UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToCall: () -> Unit, // New callback
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val partnerUser by viewModel.partnerUser.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Load conversation
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }
    
    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }
    
    Scaffold(
        containerColor = Color(0xFF000000),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            BuddyLynkChatHeader(
                username = partnerUser?.username ?: "Chat",
                avatar = partnerUser?.avatar,
                status = if (isOnline) "Active now" else "In sleep mode",
                onBackClick = onNavigateBack,
                onCallClick = { 
                    partnerUser?.let { user ->
                        CallManager.startCall(user.userId, user.username, user.avatar, isVideo = false)
                        onNavigateToCall()
                    }
                },
                onVideoClick = { 
                    partnerUser?.let { user ->
                        CallManager.startCall(user.userId, user.username, user.avatar, isVideo = true)
                        onNavigateToCall()
                    }
                },
                onInfoClick = { }
            )
        },
        bottomBar = {
            BuddyLynkChatInput(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                },
                onCameraClick = { },
                onMicClick = { },
                onGalleryClick = { },
                onStickerClick = { }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.size(40.dp)
                    )
                }
            } else if (messages.isEmpty()) {
                BuddyLynkEmptyChat()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { message ->
                        BuddyLynkMessageBubble(
                            message = message,
                            isFromMe = viewModel.isFromCurrentUser(message)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// BUDDYLYNK HEADER
// =============================================================================

@Composable
private fun BuddyLynkChatHeader(
    username: String,
    avatar: String?,
    status: String,
    onBackClick: () -> Unit,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    onInfoClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, Brush.linearGradient(LocalPremiumGradient), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
            ) {
                if (avatar != null) {
                    AsyncImage(
                        model = avatar,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.linearGradient(LocalPremiumGradient)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = username.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = status,
                    color = LocalAccentGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Filled.Call,
                    contentDescription = "Call",
                    tint = LocalGradientCyan,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            IconButton(onClick = onVideoClick) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = "Video",
                    tint = LocalGradientPurple,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF2A2A2A), Color.Transparent)
                    )
                )
                .align(Alignment.BottomCenter)
        )
    }
}

// =============================================================================
// BUDDYLYNK MESSAGE BUBBLE
// =============================================================================

@Composable
private fun BuddyLynkMessageBubble(
    message: Message,
    isFromMe: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isFromMe) 20.dp else 4.dp,
                        bottomEnd = if (isFromMe) 4.dp else 20.dp
                    )
                )
                .then(
                    if (isFromMe) {
                        Modifier.background(
                            brush = Brush.linearGradient(LocalPremiumGradient)
                        )
                    } else {
                        Modifier.background(Color(0xFF1E1E1E))
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    color = if (isFromMe) Color.White else Color(0xFFEEEEEE),
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTime(message.createdAt),
                    color = if (isFromMe) Color.White.copy(alpha = 0.7f) else Color(0xFF888888),
                    fontSize = 11.sp
                )
            }
        }
    }
}

// =============================================================================
// BUDDYLYNK INPUT BAR
// =============================================================================

@Composable
private fun BuddyLynkChatInput(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onStickerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val adaptiveBottomPadding = if (isKeyboardVisible) 4.dp else 12.dp
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. Camera Button (Gradient Circle) - Always visible on left (or hidden when typing if desired? Insta usually keeps it)
        // Actually, in the reference, it's there.
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(LocalPremiumGradient))
                .clickable(onClick = onCameraClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.CameraAlt,
                contentDescription = "Camera",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // 2. Input Pill
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 40.dp, max = 100.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF262626)) // Darker gray
                .border(
                    width = 1.dp,
                    color = Color(0xFF333333), // Subtle border
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = "Message...",
                        color = Color(0xFFAAAAAA),
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                } 
            }
            // Overlapping TextField
             androidx.compose.foundation.text.BasicTextField(
                value = messageText,
                onValueChange = onMessageChange,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                maxLines = 5,
                cursorBrush = SolidColor(LocalGradientPurple)
            )
        }

        // 3. Right Actions
        if (messageText.isNotBlank()) {
            // Typing -> Show Send Button
             Text(
                text = "Send",
                color = LocalGradientPurple, // Or gradient text?
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier
                    .clickable(onClick = onSendClick)
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            )
        } else {
            // Idle -> Show Mic, Gallery, Sticker
             IconButton(
                onClick = onMicClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Mic",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = "Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            IconButton(
                onClick = onStickerClick,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Face,
                    contentDescription = "Sticker",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun BuddyLynkEmptyChat() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    // FIXED: Apply alpha to colors, not the brush
                    .background(
                        Brush.linearGradient(
                            LocalPremiumGradient.map { it.copy(alpha = 0.1f) }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Email,
                    contentDescription = null,
                    tint = LocalGradientPurple,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Start chatting!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Send a message to break the ice 🧊",
                color = LocalTextTertiary,
                fontSize = 14.sp
            )
        }
    }
}

private fun formatTime(timestamp: String): String {
    return try {
        // Try parsing as milliseconds first (for locally created messages)
        val time = timestamp.toLongOrNull()
        if (time != null) {
            val date = Date(time)
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        
        // Try parsing as ISO date string (from backend database)
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        isoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val date = isoFormat.parse(timestamp)
        if (date != null) {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        
        // Fallback: try simple ISO format without milliseconds
        val simpleIsoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        simpleIsoFormat.timeZone = TimeZone.getTimeZone("UTC")
        val simpleDate = simpleIsoFormat.parse(timestamp)
        if (simpleDate != null) {
            return SimpleDateFormat("HH:mm", Locale.getDefault()).format(simpleDate)
        }
        
        ""
    } catch (e: Exception) {
        ""
    }
}
