package com.orignal.buddylynk.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.orignal.buddylynk.ui.theme.*
import com.orignal.buddylynk.ui.utils.DeviceCapabilities

// =============================================================================
// UNIFIED DARK THEME BACKGROUND - Instagram-style Adaptive Rendering
// =============================================================================

// Standard dark theme colors for consistency
private val UnifiedDarkBg = Color(0xFF0A0A0A)
private val UnifiedDarkBgSecondary = Color(0xFF0F0F13)

/**
 * Instagram-style Adaptive Background
 * 
 * Automatically adjusts rendering complexity based on device capabilities:
 * - LOW: Solid color (maximum performance)
 * - MEDIUM: Simple gradient (balanced)
 * - HIGH: Gradient with one overlay
 * - ULTRA: Full gradient with multiple overlays
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val profile = remember { DeviceCapabilities.getPerformanceProfile(context) }
    
    when (profile.tier) {
        // LOW: Solid color for maximum performance
        DeviceCapabilities.PerformanceProfile.Tier.LOW -> {
            Box(
                modifier = modifier.background(UnifiedDarkBg)
            ) {
                content()
            }
        }
        
        // MEDIUM: Simple vertical gradient
        DeviceCapabilities.PerformanceProfile.Tier.MEDIUM -> {
            Box(
                modifier = modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            UnifiedDarkBg,
                            UnifiedDarkBgSecondary,
                            UnifiedDarkBg
                        )
                    )
                )
            ) {
                content()
            }
        }
        
        // HIGH: Gradient with one subtle overlay
        DeviceCapabilities.PerformanceProfile.Tier.HIGH -> {
            Box(
                modifier = modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            UnifiedDarkBg,
                            UnifiedDarkBgSecondary,
                            UnifiedDarkBg
                        )
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GradientPurple.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                center = Offset(0.3f, 0.2f),
                                radius = 1500f
                            )
                        )
                )
                content()
            }
        }
        
        // ULTRA: Full gradient with multiple overlays
        DeviceCapabilities.PerformanceProfile.Tier.ULTRA -> {
            Box(
                modifier = modifier.background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            UnifiedDarkBg,
                            UnifiedDarkBgSecondary,
                            UnifiedDarkBg
                        )
                    )
                )
            ) {
                // Purple glow overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GradientPurple.copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                center = Offset(0.3f, 0.2f),
                                radius = 1500f
                            )
                        )
                )
                // Cyan glow overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    GradientCyan.copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                center = Offset(0.8f, 0.7f),
                                radius = 1200f
                            )
                        )
                )
                content()
            }
        }
    }
}


// =============================================================================
// MESH GRADIENT BACKGROUND - Lightweight alternative
// =============================================================================

@Composable
fun MeshGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    DarkBackground,
                    DarkSurface,
                    DarkBackground
                )
            )
        )
    ) {
        content()
    }
}

// =============================================================================
// SIMPLE DARK BACKGROUND - Maximum performance
// =============================================================================

@Composable
fun SimpleBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.background(DarkBackground)
    ) {
        content()
    }
}

// =============================================================================
// AURORA BACKGROUND - Static version for performance
// =============================================================================

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Static aurora-like gradient - no animations for performance
    Box(
        modifier = modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    DarkBackground,
                    Color(0xFF0A0A14),
                    DarkBackground
                )
            )
        )
    ) {
        // Aurora gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            GradientPurple.copy(alpha = 0.08f),
                            GradientCyan.copy(alpha = 0.05f),
                            GradientPink.copy(alpha = 0.06f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )
        
        content()
    }
}
