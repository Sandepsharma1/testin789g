package com.orignal.buddylynk.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Unified Dark Theme Background for all BuddyLynk screens
 * 
 * Provides consistent dark theming across the app with:
 * - Rich dark gradient background
 * - Optional animated accent orbs
 * - Consistent with BuddyLynk design system
 */

// Standard dark theme colors
object DarkTheme {
    val Background = Color(0xFF0A0A0A)          // Pure dark
    val BackgroundSecondary = Color(0xFF0F0F13) // Slightly lighter
    val BackgroundTertiary = Color(0xFF18181D)  // Cards
    val Surface = Color(0xFF1A1A1F)             // Elevated surfaces
    val SurfaceVariant = Color(0xFF242429)      // Higher elevation
    
    // Gradient colors for backgrounds
    val GradientStart = Color(0xFF0A0A0A)
    val GradientMiddle = Color(0xFF0F0F13)
    val GradientEnd = Color(0xFF0A0A0A)
    
    // Accent gradients (for orbs/effects)
    val AccentPurple = Color(0xFF6B21A8)
    val AccentIndigo = Color(0xFF6366F1)
    val AccentCyan = Color(0xFF00D9FF)
    val AccentPink = Color(0xFFEC4899)
}

/**
 * Standard dark gradient background used across all screens
 */
val DarkGradientBackground = Brush.verticalGradient(
    colors = listOf(
        DarkTheme.GradientStart,
        DarkTheme.GradientMiddle,
        DarkTheme.GradientEnd
    )
)

/**
 * Premium dark gradient with subtle purple tint
 */
val PremiumDarkGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0A0A0A),
        Color(0xFF0D0810),
        Color(0xFF1A1025).copy(alpha = 0.5f),
        Color(0xFF0A0A0A)
    )
)

/**
 * Unified background modifier for all screens
 */
fun Modifier.darkThemeBackground(): Modifier = this
    .fillMaxSize()
    .background(DarkGradientBackground)

/**
 * Premium background modifier with purple accent
 */
fun Modifier.premiumDarkBackground(): Modifier = this
    .fillMaxSize()
    .background(PremiumDarkGradient)

/**
 * DarkThemeBackground Composable
 * 
 * A consistent dark background wrapper for all screens.
 * 
 * @param modifier Optional modifier
 * @param usePremiumGradient Whether to use premium gradient with purple accent
 * @param content Screen content
 */
@Composable
fun DarkThemeBackground(
    modifier: Modifier = Modifier,
    usePremiumGradient: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (usePremiumGradient) PremiumDarkGradient else DarkGradientBackground
            )
    ) {
        content()
    }
}

/**
 * DarkThemeScaffold - Unified scaffold with dark background
 * Use this as a replacement for Box/Column in screen composables
 */
@Composable
fun DarkThemeScaffold(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkGradientBackground)
            .padding(contentPadding)
    ) {
        content()
    }
}
