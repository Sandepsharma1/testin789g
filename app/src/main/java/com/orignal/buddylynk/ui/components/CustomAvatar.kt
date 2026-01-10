package com.orignal.buddylynk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs

/**
 * Custom Avatar Component with colorful fallback
 * Shows colorful gradient background with initials/emoji when no image available
 */

// Avatar color gradients - vibrant premium colors
private val avatarGradients = listOf(
    listOf(Color(0xFFF97316), Color(0xFFEF4444)),  // Orange to Red
    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),  // Purple to Pink
    listOf(Color(0xFF22D3EE), Color(0xFF3B82F6)),  // Cyan to Blue
    listOf(Color(0xFF10B981), Color(0xFF06B6D4)),  // Green to Cyan
    listOf(Color(0xFFF59E0B), Color(0xFFD97706)),  // Amber to Orange
    listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),  // Indigo to Purple
    listOf(Color(0xFFEC4899), Color(0xFFDB2777)),  // Pink shades
    listOf(Color(0xFF14B8A6), Color(0xFF059669)),  // Teal shades
)

// Fun emojis for groups/channels without names
private val groupEmojis = listOf("🚀", "⚡", "🔥", "💎", "🌟", "🎮", "🎯", "💫", "🌈", "🎪")

/**
 * Custom Avatar that shows a colorful gradient with initials when no image
 */
@Composable
fun CustomAvatar(
    imageUrl: String?,
    name: String,
    size: Dp = 56.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    fontSize: TextUnit = 20.sp,
    modifier: Modifier = Modifier
) {
    val hasImage = !imageUrl.isNullOrBlank() && imageUrl != "null"
    
    // Generate consistent gradient based on name hash
    val gradientIndex = remember(name) {
        abs(name.hashCode()) % avatarGradients.size
    }
    val gradient = avatarGradients[gradientIndex]
    
    // Get display text (initials or emoji)
    val displayText = remember(name) {
        when {
            name.isBlank() -> groupEmojis[abs(name.hashCode()) % groupEmojis.size]
            name.length == 1 -> name.uppercase()
            name.contains(" ") -> {
                val parts = name.split(" ").filter { it.isNotBlank() }
                if (parts.size >= 2) {
                    "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
                } else {
                    name.take(2).uppercase()
                }
            }
            else -> name.take(2).uppercase()
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Colorful gradient background with initials
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayText,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Circle avatar variant
 */
@Composable
fun CircleAvatar(
    imageUrl: String?,
    name: String,
    size: Dp = 48.dp,
    fontSize: TextUnit = 18.sp,
    modifier: Modifier = Modifier
) {
    CustomAvatar(
        imageUrl = imageUrl,
        name = name,
        size = size,
        shape = CircleShape,
        fontSize = fontSize,
        modifier = modifier
    )
}

/**
 * Square avatar with rounded corners
 */
@Composable
fun SquareAvatar(
    imageUrl: String?,
    name: String,
    size: Dp = 56.dp,
    cornerRadius: Dp = 16.dp,
    fontSize: TextUnit = 20.sp,
    modifier: Modifier = Modifier
) {
    CustomAvatar(
        imageUrl = imageUrl,
        name = name,
        size = size,
        shape = RoundedCornerShape(cornerRadius),
        fontSize = fontSize,
        modifier = modifier
    )
}
