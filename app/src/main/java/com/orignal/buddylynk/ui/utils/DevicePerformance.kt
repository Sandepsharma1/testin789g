package com.orignal.buddylynk.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Device Performance Detector
 * 
 * Detects if device is low-end and adjusts UI accordingly:
 * - Reduces animations
 * - Simplifies backgrounds
 * - Lower image quality
 * - Disables heavy effects
 */
object DevicePerformance {
    
    // Performance tier enum
    enum class Tier {
        LOW,      // <2GB RAM, old CPU - disable most effects
        MEDIUM,   // 2-4GB RAM - reduced effects
        HIGH      // >4GB RAM - full effects
    }
    
    private var cachedTier: Tier? = null
    
    /**
     * Get device performance tier based on RAM and CPU
     */
    fun getTier(context: Context): Tier {
        cachedTier?.let { return it }
        
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        // Total RAM in GB
        val totalRamGb = memoryInfo.totalMem / (1024 * 1024 * 1024.0)
        
        // Check if device is low-end
        val isLowRam = activityManager.isLowRamDevice
        
        // Determine tier
        val tier = when {
            isLowRam || totalRamGb < 2.0 -> Tier.LOW
            totalRamGb < 4.0 -> Tier.MEDIUM
            else -> Tier.HIGH
        }
        
        cachedTier = tier
        android.util.Log.d("DevicePerformance", "Device tier: $tier (RAM: ${String.format("%.1f", totalRamGb)}GB, lowRam: $isLowRam)")
        return tier
    }
    
    /**
     * Check if we should use reduced animations
     */
    fun shouldReduceAnimations(context: Context): Boolean {
        return getTier(context) == Tier.LOW
    }
    
    /**
     * Check if we should use simplified backgrounds
     */
    fun shouldUseSimpleBackground(context: Context): Boolean {
        return getTier(context) != Tier.HIGH
    }
    
    /**
     * Get recommended image size based on device tier
     */
    fun getRecommendedImageSize(context: Context): Int {
        return when (getTier(context)) {
            Tier.LOW -> 256    // Very low memory
            Tier.MEDIUM -> 384 // Medium devices
            Tier.HIGH -> 512   // Full quality
        }
    }
    
    /**
     * Get video poll interval (higher = less CPU usage)
     */
    fun getVideoPollInterval(context: Context): Long {
        return when (getTier(context)) {
            Tier.LOW -> 1000L    // 1 second - minimal CPU
            Tier.MEDIUM -> 750L // 750ms 
            Tier.HIGH -> 500L   // 500ms - responsive
        }
    }
}

/**
 * Composable to remember device performance tier
 */
@Composable
fun rememberDeviceTier(): DevicePerformance.Tier {
    val context = LocalContext.current
    return remember { DevicePerformance.getTier(context) }
}

/**
 * Check if animations should be reduced
 */
@Composable
fun rememberShouldReduceAnimations(): Boolean {
    val context = LocalContext.current
    return remember { DevicePerformance.shouldReduceAnimations(context) }
}
