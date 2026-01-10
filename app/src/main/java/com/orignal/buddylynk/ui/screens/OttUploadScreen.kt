package com.orignal.buddylynk.ui.screens

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.orignal.buddylynk.data.model.OttCategories
import com.orignal.buddylynk.data.upload.UploadManager
import com.orignal.buddylynk.ui.viewmodel.OttViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Premium Colors
private val DarkBg = Color(0xFF0A0A0F)
private val CardBg = Color(0xFF16161F)
private val InputBg = Color(0xFF1E1E28)
private val AccentPink = Color(0xFFEC4899)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentGreen = Color(0xFF22C55E)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF9CA3AF)
private val TextTertiary = Color(0xFF6B7280)
private val BorderColor = Color.White.copy(alpha = 0.1f)

/**
 * OTT Upload Screen - Upload videos with background uploading
 * Videos upload in background, user can navigate away immediately
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OttUploadScreen(
    onNavigateBack: () -> Unit,
    onUploadSuccess: () -> Unit,
    viewModel: OttViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var showError by remember { mutableStateOf<String?>(null) }
    var videoDuration by remember { mutableStateOf(0L) }
    var autoThumbnailBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var isUploading by remember { mutableStateOf(false) } // Prevent double-click
    
    // Video picker - just selects video, doesn't upload yet
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            videoUri = selectedUri
            isProcessing = true
            
            // Extract metadata in background
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val retriever = MediaMetadataRetriever()
                        try {
                            retriever.setDataSource(context, selectedUri)
                            
                            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            videoDuration = durationStr?.toLongOrNull()?.div(1000) ?: 0L
                            
                            val frameTime = if (videoDuration > 1) 1000000L else 0L
                            autoThumbnailBitmap = retriever.getFrameAtTime(frameTime, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        } finally {
                            retriever.release()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OttUpload", "Error extracting metadata: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }
    
    // Check if ready to submit (also check not already uploading to prevent double-click)
    val canSubmit = title.isNotBlank() && videoUri != null && !isProcessing && !isUploading
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                
                Text(
                    text = "Upload Video",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Background upload info banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(AccentPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.CloudUpload,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Background Upload",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Video will upload while you continue using the app",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Video Picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg)
                    .border(2.dp, BorderColor, RoundedCornerShape(16.dp))
                    .clickable { videoPicker.launch("video/*") },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isProcessing -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = AccentPink)
                            Text("Processing video...", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                    videoUri != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Text("Video selected!", color = AccentGreen, fontSize = 14.sp)
                            if (videoDuration > 0) {
                                Text(
                                    "Duration: ${videoDuration / 60}:${String.format("%02d", videoDuration % 60)}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Text("Tap to change", color = TextTertiary, fontSize = 12.sp)
                        }
                    }
                    else -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        Brush.linearGradient(listOf(AccentPink, AccentPurple)),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                text = "Select Video",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "MP4, MOV, or WebM • Max 500MB",
                                fontSize = 13.sp,
                                color = TextTertiary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Thumbnail Preview (auto-generated)
            if (autoThumbnailBitmap != null) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "Thumbnail (Auto-generated)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = autoThumbnailBitmap!!.asImageBitmap(),
                            contentDescription = "Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp)
                                .background(AccentPurple, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AUTO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Title input
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Title",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    placeholder = { Text("Enter video title", color = TextTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedBorderColor = AccentPink,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Text(
                    text = "${title.length}/100",
                    fontSize = 12.sp,
                    color = TextTertiary,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description input
            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Description",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    placeholder = { Text("Tell viewers about your video", color = TextTertiary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = InputBg,
                        unfocusedContainerColor = InputBg,
                        focusedBorderColor = AccentPink,
                        unfocusedBorderColor = BorderColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Upload button - starts background upload
            Button(
                onClick = {
                    if (canSubmit && videoUri != null) {
                        // Immediately disable button to prevent double-click
                        isUploading = true
                        
                        // Start background upload
                        UploadManager.startOttUpload(
                            context = context,
                            videoUri = videoUri!!,
                            title = title,
                            description = description,
                            category = "General"
                        )
                        
                        // Show toast and navigate back immediately
                        Toast.makeText(
                            context,
                            "Upload started! Video will upload in background.",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        onUploadSuccess()
                    }
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPink,
                    disabledContainerColor = AccentPink.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Upload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Upload",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(100.dp))
        }
        
        // Error snackbar
        showError?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                action = {
                    TextButton(onClick = { showError = null }) {
                        Text("Dismiss", color = Color.White)
                    }
                },
                containerColor = Color(0xFFDC2626)
            ) {
                Text(error)
            }
        }
    }
}
