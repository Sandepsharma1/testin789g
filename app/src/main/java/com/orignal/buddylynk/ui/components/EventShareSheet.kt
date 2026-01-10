package com.orignal.buddylynk.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Premium Colors
private val SheetBg = Color(0xFF1A1A1E)
private val CardBg = Color(0xFF252530)
private val AccentGreen = Color(0xFF22C55E)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentOrange = Color(0xFFF97316)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF9CA3AF)
private val BorderColor = Color.White.copy(alpha = 0.1f)

// Group data class for TeamUp selection
data class ShareableGroup(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val memberCount: Int = 0
)

/**
 * Event Share Bottom Sheet
 * Options: Share via Link (with QR) or Share to TeamUp Groups
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventShareSheet(
    visible: Boolean,
    eventId: String,
    eventTitle: String,
    groups: List<ShareableGroup> = emptyList(),
    onDismiss: () -> Unit,
    onShareToGroup: (groupId: String) -> Unit,
    onCopyLink: () -> Unit = {}
) {
    if (!visible) return
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Link, 1 = TeamUp
    var showCopiedFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    
    // Generate event share link
    val shareLink = "https://app.buddylynk.com/invite/event/$eventId"
    
    // Generate QR code bitmap
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(eventId) {
        qrBitmap = withContext(Dispatchers.Default) {
            generateQRCode(shareLink, 400)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        contentColor = TextPrimary,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Share Event",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = eventTitle,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Tab Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Link Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 0) AccentBlue else Color.Transparent)
                        .clickable { selectedTab = 0 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Link,
                            contentDescription = null,
                            tint = if (selectedTab == 0) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Link",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedTab == 0) Color.White else TextSecondary
                        )
                    }
                }
                
                // TeamUp Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedTab == 1) AccentGreen else Color.Transparent)
                        .clickable { selectedTab = 1 }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Groups,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "TeamUp",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (selectedTab == 1) Color.White else TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Content based on tab
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "share_tab_content"
            ) { tab ->
                if (tab == 0) {
                    // Link & QR Code Tab
                    LinkShareContent(
                        shareLink = shareLink,
                        qrBitmap = qrBitmap,
                        showCopiedFeedback = showCopiedFeedback,
                        onCopyLink = {
                            clipboardManager.setText(AnnotatedString(shareLink))
                            showCopiedFeedback = true
                            onCopyLink()
                        }
                    )
                } else {
                    // TeamUp Groups Tab
                    TeamUpShareContent(
                        groups = groups,
                        onShareToGroup = onShareToGroup
                    )
                }
            }
            
            // Reset copied feedback
            LaunchedEffect(showCopiedFeedback) {
                if (showCopiedFeedback) {
                    kotlinx.coroutines.delay(2000)
                    showCopiedFeedback = false
                }
            }
        }
    }
}

@Composable
private fun LinkShareContent(
    shareLink: String,
    qrBitmap: Bitmap?,
    showCopiedFeedback: Boolean,
    onCopyLink: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // QR Code
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Event QR Code",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    color = AccentBlue,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        Text(
            text = "Scan to join event",
            fontSize = 14.sp,
            color = TextSecondary
        )
        
        // Copy Link Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .clickable { onCopyLink() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Outlined.Link,
                contentDescription = null,
                tint = AccentBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = shareLink,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showCopiedFeedback) AccentGreen else AccentBlue)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (showCopiedFeedback) "Copied!" else "Copy",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
        
        // Share via apps button
        Button(
            onClick = { /* TODO: Open system share */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange)
        ) {
            Icon(
                Icons.Outlined.Share,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Share via apps",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TeamUpShareContent(
    groups: List<ShareableGroup>,
    onShareToGroup: (groupId: String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (groups.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "No groups yet",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "Join or create a TeamUp group\nto share events with friends",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Text(
                text = "Select a group to share",
                fontSize = 14.sp,
                color = TextSecondary
            )
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groups) { group ->
                    GroupShareItem(
                        group = group,
                        onClick = { onShareToGroup(group.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupShareItem(
    group: ShareableGroup,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Group Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AccentGreen.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            if (group.imageUrl != null) {
                AsyncImage(
                    model = group.imageUrl,
                    contentDescription = group.name,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = group.name.take(2).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
        }
        
        // Group Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${group.memberCount} members",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        
        // Share button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(AccentGreen.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Share",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentGreen
                )
            }
        }
    }
}

/**
 * Generate QR Code bitmap from string
 */
private fun generateQRCode(content: String, size: Int): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
