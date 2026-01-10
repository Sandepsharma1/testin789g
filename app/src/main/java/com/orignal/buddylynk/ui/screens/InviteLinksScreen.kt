package com.orignal.buddylynk.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
// Toast removed
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.ui.viewmodel.TeamInfoViewModel

// Colors matching reference
private val DarkBg = Color(0xFF0D1B2A)
private val CardBg = Color(0xFF1B2838)
private val Zinc700 = Color(0xFF3F3F46)
private val Zinc400 = Color(0xFFA1A1AA)
private val CyanAccent = Color(0xFF00BCD4)
private val RedAccent = Color(0xFFEF5350)
private val OrangeAvatar = Color(0xFFFFB74D)

/**
 * Invite Links Screen - Matching Telegram-style design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteLinksScreen(
    groupId: String,
    groupName: String,
    groupAvatar: String?,
    onBack: () -> Unit,
    viewModel: TeamInfoViewModel = viewModel()
) {
    val context = LocalContext.current
    
    LaunchedEffect(groupId) {
        viewModel.loadGroupInfo(groupId)
    }
    
    val inviteLinks by viewModel.inviteLinks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showQrDialog by remember { mutableStateOf(false) }
    var selectedLinkForQr by remember { mutableStateOf<String?>(null) }
    var expandedMenuLinkId by remember { mutableStateOf<String?>(null) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = { 
                    Text(
                        "Invite Links", 
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Avatar and description
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Duck Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(OrangeAvatar),
                        contentAlignment = Alignment.Center
                    ) {
                        if (groupAvatar != null && groupAvatar.isNotEmpty()) {
                            AsyncImage(
                                model = groupAvatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("ðŸ¦†", fontSize = 40.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Anyone on Buddylynk will be able to join\nyour channel by following this link.",
                        fontSize = 14.sp,
                        color = Zinc400,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
                
                // Invite Link section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Invite Link",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = CyanAccent,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Show empty state if no links
                    if (inviteLinks.isEmpty() && !isLoading) {
                        // Empty state card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CardBg),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = Zinc400,
                                    modifier = Modifier.size(48.dp)
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "No Invite Links Yet",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "Create a link to invite others to join this channel",
                                    fontSize = 14.sp,
                                    color = Zinc400,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Links list
                    inviteLinks.forEach { link ->
                        InviteLinkCard(
                            url = link.url,
                            showMenu = expandedMenuLinkId == link.linkId,
                            onMenuClick = {
                                expandedMenuLinkId = if (expandedMenuLinkId == link.linkId) null else link.linkId
                            },
                            onDismissMenu = { expandedMenuLinkId = null },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Invite Link", link.url)
                                clipboard.setPrimaryClip(clip)
                                // Silent
                            },
                            onShowQr = {
                                expandedMenuLinkId = null
                                selectedLinkForQr = link.url
                                showQrDialog = true
                            },
                            onDelete = {
                                expandedMenuLinkId = null
                                viewModel.deleteInviteLink(link.linkId, createNew = true)
                                // Silent
                            }
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Create New Link button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                // Silent
                                viewModel.createInviteLink() 
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Create a New Link",
                            fontSize = 16.sp,
                            color = CyanAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Info text
                    Text(
                        text = "You can create additional invite links that have limited time, number of users or require a paid subscription.",
                        fontSize = 13.sp,
                        color = Zinc400,
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        // Loading
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyanAccent)
            }
        }
        
        // QR Dialog
        if (showQrDialog && selectedLinkForQr != null) {
            QrCodeDialog(
                url = selectedLinkForQr!!,
                onDismiss = { 
                    showQrDialog = false 
                    selectedLinkForQr = null
                }
            )
        }
    }
}

@Composable
private fun InviteLinkCard(
    url: String,
    showMenu: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    onCopy: () -> Unit,
    onShowQr: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // URL row with menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = url.removePrefix("https://"),
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                
                Box {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "Options",
                            tint = Zinc400
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = onDismissMenu,
                        containerColor = CardBg
                    ) {
                        DropdownMenuItem(
                            text = { Text("Get QR code", color = Color.White) },
                            onClick = onShowQr,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.QrCode2,
                                    contentDescription = null,
                                    tint = Zinc400
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete URL", color = RedAccent) },
                            onClick = onDelete,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = RedAccent
                                )
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Buttons row - Copy and Get QR code side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Copy button
                Button(
                    onClick = onCopy,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                
                // Get QR code button
                OutlinedButton(
                    onClick = onShowQr,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Zinc400.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        Icons.Outlined.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Get QR code", fontSize = 14.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Revoke Link row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDelete() }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = RedAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Revoke Link",
                    fontSize = 14.sp,
                    color = RedAccent
                )
            }
        }
    }
}

@Composable
private fun QrCodeDialog(
    url: String,
    onDismiss: () -> Unit
) {
    // Generate QR code bitmap
    val qrBitmap = remember(url) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(url, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "QR Code",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (qrBitmap != null) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    } else {
                        Icon(
                            Icons.Outlined.QrCode2,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(150.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = url.removePrefix("https://"),
                    fontSize = 14.sp,
                    color = Zinc400,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                ) {
                    Text("Close", color = Color.Black)
                }
            }
        }
    }
}

