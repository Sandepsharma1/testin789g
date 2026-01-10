package com.orignal.buddylynk.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

// Premium minimal colors
private val PopupBg = Color(0xFF1A1A1E)
private val PopupBorder = Color.White.copy(alpha = 0.08f)
private val AccentGreen = Color(0xFF22C55E)
private val AccentRed = Color(0xFFEF4444)
private val AccentBlue = Color(0xFF3B82F6)
private val AccentOrange = Color(0xFFF59E0B)
private val TextPrimary = Color.White
private val TextSecondary = Color(0xFF9CA3AF)

/**
 * Premium Popup Types
 */
enum class PopupType {
    SUCCESS, ERROR, WARNING, INFO, CONFIRM, LOADING
}

/**
 * Premium Popup - Dynamic, glassmorphic, modern design
 * Enhanced with animations and effects
 */
@Composable
fun PremiumPopup(
    visible: Boolean,
    type: PopupType = PopupType.INFO,
    title: String,
    message: String = "",
    confirmText: String = "OK",
    cancelText: String = "Cancel",
    showCancel: Boolean = false,
    onConfirm: () -> Unit = {},
    onCancel: () -> Unit = {},
    onDismiss: () -> Unit = {},
    icon: ImageVector? = null,
    content: @Composable (() -> Unit)? = null
) {
    if (!visible) return
    
    // Dynamic entrance animation
    val animatedScale = remember { Animatable(0.8f) }
    val animatedAlpha = remember { Animatable(0f) }
    
    LaunchedEffect(visible) {
        if (visible) {
            // Parallel animations for smooth entrance
            launch {
                animatedScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.6f,
                        stiffness = 400f
                    )
                )
            }
            launch {
                animatedAlpha.animateTo(1f, animationSpec = tween(200))
            }
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = type != PopupType.LOADING,
            dismissOnClickOutside = type != PopupType.LOADING,
            usePlatformDefaultWidth = false
        )
    ) {
        // Dynamic scrim with animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f * animatedAlpha.value))
                .clickable(
                    enabled = type != PopupType.LOADING,
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            // Glassmorphic Popup Card
            Card(
                modifier = Modifier
                    .widthIn(min = 300.dp, max = 360.dp)
                    .padding(24.dp)
                    .graphicsLayer {
                        scaleX = animatedScale.value
                        scaleY = animatedScale.value
                        alpha = animatedAlpha.value
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { /* Prevent dismiss on card click */ },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = PopupBg.copy(alpha = 0.95f)),
                border = BorderStroke(1.5.dp, 
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Close button for non-loading popups
                    if (type != PopupType.LOADING) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .offset(x = 8.dp, y = (-12).dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.05f))
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    // Dynamic animated icon
                    DynamicPopupIcon(type, icon)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Title with gradient for success/error
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    
                    // Message
                    if (message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                    
                    // Custom content
                    content?.let {
                        Spacer(modifier = Modifier.height(16.dp))
                        it()
                    }
                    
                    // Loading indicator with pulse animation
                    if (type == PopupType.LOADING) {
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "loading")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(600),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale },
                            color = AccentBlue,
                            strokeWidth = 3.dp
                        )
                    }
                    
                    // Dynamic buttons
                    if (type != PopupType.LOADING) {
                        Spacer(modifier = Modifier.height(28.dp))
                        DynamicPopupButtons(
                            type = type,
                            confirmText = confirmText,
                            cancelText = cancelText,
                            showCancel = showCancel || type == PopupType.CONFIRM,
                            onConfirm = onConfirm,
                            onCancel = onCancel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicPopupIcon(type: PopupType, customIcon: ImageVector?) {
    val icon = customIcon ?: when (type) {
        PopupType.SUCCESS -> Icons.Outlined.CheckCircle
        PopupType.ERROR -> Icons.Outlined.Cancel
        PopupType.WARNING -> Icons.Outlined.Warning
        PopupType.INFO -> Icons.Outlined.Info
        PopupType.CONFIRM -> Icons.Outlined.HelpOutline
        PopupType.LOADING -> null
    }
    
    val color = when (type) {
        PopupType.SUCCESS -> AccentGreen
        PopupType.ERROR -> AccentRed
        PopupType.WARNING -> AccentOrange
        PopupType.INFO -> AccentBlue
        PopupType.CONFIRM -> AccentBlue
        PopupType.LOADING -> AccentBlue
    }
    
    if (icon != null) {
        // Pulse animation for icon
        val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
        val iconScale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Box(
            modifier = Modifier
                .size(72.dp)
                .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            color.copy(alpha = 0.2f),
                            color.copy(alpha = 0.08f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

@Composable
private fun DynamicPopupButtons(
    type: PopupType,
    confirmText: String,
    cancelText: String,
    showCancel: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val buttonColor = when (type) {
        PopupType.SUCCESS -> AccentGreen
        PopupType.ERROR -> AccentRed
        PopupType.WARNING -> AccentOrange
        else -> AccentBlue
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (showCancel) Arrangement.spacedBy(12.dp) else Arrangement.Center
    ) {
        // Cancel button with hover effect
        if (showCancel) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.1f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
            ) {
                Text(
                    text = cancelText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        
        // Gradient confirm button
        Button(
            onClick = onConfirm,
            modifier = Modifier
                .then(if (showCancel) Modifier.weight(1f) else Modifier.fillMaxWidth())
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                text = confirmText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

/**
 * Quick helper functions for common popup types
 */
@Composable
fun SuccessPopup(
    visible: Boolean,
    title: String,
    message: String = "",
    onDismiss: () -> Unit
) {
    PremiumPopup(
        visible = visible,
        type = PopupType.SUCCESS,
        title = title,
        message = message,
        confirmText = "Done",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun ErrorPopup(
    visible: Boolean,
    title: String = "Error",
    message: String,
    onDismiss: () -> Unit
) {
    PremiumPopup(
        visible = visible,
        type = PopupType.ERROR,
        title = title,
        message = message,
        confirmText = "OK",
        onConfirm = onDismiss,
        onDismiss = onDismiss
    )
}

@Composable
fun ConfirmPopup(
    visible: Boolean,
    title: String,
    message: String = "",
    confirmText: String = "Confirm",
    cancelText: String = "Cancel",
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    PremiumPopup(
        visible = visible,
        type = PopupType.CONFIRM,
        title = title,
        message = message,
        confirmText = confirmText,
        cancelText = cancelText,
        showCancel = true,
        onConfirm = onConfirm,
        onCancel = onCancel,
        onDismiss = onCancel
    )
}

@Composable
fun LoadingPopup(
    visible: Boolean,
    title: String = "Please wait...",
    message: String = ""
) {
    PremiumPopup(
        visible = visible,
        type = PopupType.LOADING,
        title = title,
        message = message
    )
}

/**
 * Premium Toast - Top notification banner (Instagram style)
 */
@Composable
fun PremiumToast(
    visible: Boolean,
    type: PopupType = PopupType.INFO,
    message: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        val bgColor = when (type) {
            PopupType.SUCCESS -> AccentGreen
            PopupType.ERROR -> AccentRed
            PopupType.WARNING -> AccentOrange
            else -> AccentBlue
        }
        
        val icon = when (type) {
            PopupType.SUCCESS -> Icons.Outlined.CheckCircle
            PopupType.ERROR -> Icons.Outlined.Cancel
            PopupType.WARNING -> Icons.Outlined.Warning
            else -> Icons.Outlined.Info
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
