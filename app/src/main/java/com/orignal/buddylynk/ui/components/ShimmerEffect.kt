package com.orignal.buddylynk.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orignal.buddylynk.ui.theme.*

// =============================================================================
// PREMIUM SHIMMER EFFECT - Instagram-style loading animation
// =============================================================================

/**
 * Premium shimmer colors - darker for better contrast on dark theme
 */
private val ShimmerDark = Color(0xFF1A1A1C)
private val ShimmerMid = Color(0xFF2A2A2E)
private val ShimmerLight = Color(0xFF3A3A3E)

@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    // Premium shimmer with smoother, wider gradient sweep
    val shimmerColors = listOf(
        ShimmerDark,
        ShimmerMid,
        ShimmerLight,
        ShimmerMid,
        ShimmerDark
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1500f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "translate"
    )
    
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation - 600f, translateAnimation - 600f),
        end = Offset(translateAnimation, translateAnimation)
    )
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush)
    )
}

// =============================================================================
// SHIMMER CARD - Pre-built shimmer placeholder for cards
// =============================================================================

@Composable
fun ShimmerCard(
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        glassOpacity = 0.05f
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar + name row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerEffect(
                    modifier = Modifier.size(48.dp),
                    cornerRadius = 24.dp
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerEffect(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp)
                    )
                    ShimmerEffect(
                        modifier = Modifier
                            .width(80.dp)
                            .height(10.dp)
                    )
                }
            }
            // Content lines
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
            )
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
            )
        }
    }
}

// =============================================================================
// SHIMMER POST - Premium Instagram-style post skeleton
// =============================================================================

@Composable
fun ShimmerPost(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Row - Avatar + Username + Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with gradient ring effect
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(ShimmerDark),
                    contentAlignment = Alignment.Center
                ) {
                    ShimmerEffect(
                        modifier = Modifier.size(44.dp),
                        cornerRadius = 22.dp
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Username
                    ShimmerEffect(
                        modifier = Modifier
                            .width(120.dp)
                            .height(14.dp),
                        cornerRadius = 4.dp
                    )
                    // Timestamp
                    ShimmerEffect(
                        modifier = Modifier
                            .width(70.dp)
                            .height(10.dp),
                        cornerRadius = 4.dp
                    )
                }
            }
            
            // More options dots
            ShimmerEffect(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp),
                cornerRadius = 12.dp
            )
        }
        
        // Caption Text - 2 lines
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(12.dp),
                cornerRadius = 4.dp
            )
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(12.dp),
                cornerRadius = 4.dp
            )
        }
        
        // Image/Media placeholder - Large rounded card
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            cornerRadius = 16.dp
        )
        
        // Action Bar - Like, Comment, Share, Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Like button
                ShimmerEffect(
                    modifier = Modifier.size(26.dp),
                    cornerRadius = 13.dp
                )
                // Comment button
                ShimmerEffect(
                    modifier = Modifier.size(26.dp),
                    cornerRadius = 13.dp
                )
                // Share button
                ShimmerEffect(
                    modifier = Modifier.size(26.dp),
                    cornerRadius = 13.dp
                )
            }
            
            // Save/Bookmark button
            ShimmerEffect(
                modifier = Modifier.size(26.dp),
                cornerRadius = 13.dp
            )
        }
        
        // Engagement stats - Likes count
        ShimmerEffect(
            modifier = Modifier
                .width(100.dp)
                .height(12.dp),
            cornerRadius = 4.dp
        )
    }
}

// =============================================================================
// PULSING DOT - For online status, notifications
// =============================================================================

@Composable
fun PulsingDot(
    modifier: Modifier = Modifier,
    color: Color = StatusOnline,
    size: Dp = 10.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Box(
        modifier = modifier
            .size(size * scale)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha))
    )
}
