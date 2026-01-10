package com.orignal.buddylynk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.model.UploadState
import com.orignal.buddylynk.data.model.UploadStatus
import com.orignal.buddylynk.data.upload.UploadManager

// Premium Colors
private val DarkBg = Color(0xFF12121A)
private val CardBg = Color(0xFF1A1A25)
private val AccentPink = Color(0xFFEC4899)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF22C55E)
private val AccentRed = Color(0xFFEF4444)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFFB0B0B8)

/**
 * Upload Progress Indicator - Shows on Home page during uploads
 * Premium animated progress bar with status
 */
@Composable
fun UploadProgressIndicator(
    modifier: Modifier = Modifier,
    onUploadComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val uploads by UploadManager.activeUploads.collectAsState()
    
    // Get visible uploads
    val visibleUploads = remember(uploads) {
        uploads.values
            .filter { it.status != UploadStatus.CANCELLED }
            .sortedByDescending { it.createdAt }
    }
    
    // Trigger refresh on complete
    LaunchedEffect(uploads) {
        uploads.values.find { it.status == UploadStatus.COMPLETED }?.let {
            onUploadComplete()
            // Auto-dismiss after delay
            kotlinx.coroutines.delay(3000)
            UploadManager.dismissUpload(it.uploadId)
        }
    }
    
    AnimatedVisibility(
        visible = visibleUploads.isNotEmpty(),
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(DarkBg)
        ) {
            visibleUploads.forEach { upload ->
                UploadProgressCard(
                    upload = upload,
                    onRetry = { UploadManager.retryUpload(context, upload.uploadId) },
                    onCancel = { UploadManager.cancelUpload(context, upload.uploadId) },
                    onDismiss = { UploadManager.dismissUpload(upload.uploadId) }
                )
            }
        }
    }
}

@Composable
private fun UploadProgressCard(
    upload: UploadState,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_offset"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail preview
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (upload.mediaUris.isNotEmpty()) {
                        AsyncImage(
                            model = upload.mediaUris.first(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    // Status overlay
                    when (upload.status) {
                        UploadStatus.UPLOADING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AccentPink,
                                strokeWidth = 2.dp
                            )
                        }
                        UploadStatus.COMPLETED -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AccentGreen.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        UploadStatus.FAILED -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AccentRed.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        else -> {}
                    }
                }
                
                // Status text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (upload.status) {
                            UploadStatus.PENDING -> "Preparing upload..."
                            UploadStatus.UPLOADING -> "Uploading ${upload.currentMediaIndex + 1}/${upload.totalMedia}..."
                            UploadStatus.PROCESSING -> "Creating post..."
                            UploadStatus.COMPLETED -> "Posted successfully!"
                            UploadStatus.FAILED -> "Upload failed"
                            UploadStatus.CANCELLED -> "Cancelled"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (upload.status) {
                            UploadStatus.COMPLETED -> AccentGreen
                            UploadStatus.FAILED -> AccentRed
                            else -> TextPrimary
                        }
                    )
                    
                    if (upload.status == UploadStatus.UPLOADING) {
                        Text(
                            text = "${(upload.progress * 100).toInt()}%",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    
                    if (upload.status == UploadStatus.FAILED && upload.errorMessage != null) {
                        Text(
                            text = upload.errorMessage,
                            fontSize = 11.sp,
                            color = AccentRed.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
                
                // Action buttons
                when (upload.status) {
                    UploadStatus.PENDING, UploadStatus.UPLOADING -> {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Cancel",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    UploadStatus.FAILED -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = onRetry,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Refresh,
                                    contentDescription = "Retry",
                                    tint = AccentPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    UploadStatus.COMPLETED -> {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Dismiss",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    else -> {}
                }
            }
            
            // Animated progress bar
            if (upload.status == UploadStatus.UPLOADING || upload.status == UploadStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    // Animated gradient progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(upload.progress.coerceIn(0.05f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AccentPink, AccentPurple, AccentPink),
                                    startX = shimmerOffset * 500f - 250f,
                                    endX = shimmerOffset * 500f + 250f
                                ),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}
