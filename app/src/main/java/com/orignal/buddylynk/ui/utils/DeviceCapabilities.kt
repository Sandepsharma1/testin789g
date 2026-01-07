package com.orignal.buddylynk.ui.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.File
import java.io.RandomAccessFile

/**
 * Instagram-Style Device Capability Detector
 * 
 * Automatically detects device capabilities and adjusts UI performance:
 * - CPU cores and max frequency
 * - Available and total RAM
 * - GPU capabilities
 * - Screen refresh rate
 * 
 * Like Instagram, this allows the app to run smoothly on ALL devices
 * by automatically scaling down effects on low-end hardware.
 */
object DeviceCapabilities {
    
    // Cached device info
    private var cachedInfo: DeviceInfo? = null
    private var cachedPerformanceProfile: PerformanceProfile? = null
    
    /**
     * Complete device information
     */
    data class DeviceInfo(
        val cpuCores: Int,
        val cpuMaxFreqMHz: Int,
        val totalRamMB: Int,
        val availableRamMB: Int,
        val isLowRamDevice: Boolean,
        val sdkVersion: Int,
        val deviceModel: String,
        val manufacturer: String
    ) {
        val cpuScore: Int get() = (cpuCores * cpuMaxFreqMHz) / 1000
        val ramScore: Int get() = totalRamMB / 512
        
        // Overall device score (0-100)
        val overallScore: Int get() {
            val cpuWeight = 0.6
            val ramWeight = 0.4
            val normalizedCpu = (cpuScore.coerceIn(0, 20) / 20.0 * 100).toInt()
            val normalizedRam = (ramScore.coerceIn(0, 16) / 16.0 * 100).toInt()
            return ((normalizedCpu * cpuWeight) + (normalizedRam * ramWeight)).toInt()
        }
    }
    
    /**
     * Performance profile based on device capabilities
     * Like Instagram's adaptive rendering
     */
    data class PerformanceProfile(
        val tier: Tier,
        val imageQuality: ImageQuality,
        val animationLevel: AnimationLevel,
        val videoPreloadCount: Int,
        val imageCacheSize: Int, // MB
        val useHardwareLayers: Boolean,
        val enableBlurEffects: Boolean,
        val enableGradients: Boolean,
        val enableShadows: Boolean,
        val maxConcurrentDownloads: Int,
        val scrollFlingMultiplier: Float
    ) {
        enum class Tier { LOW, MEDIUM, HIGH, ULTRA }
        enum class ImageQuality { LOW, MEDIUM, HIGH }
        enum class AnimationLevel { NONE, MINIMAL, STANDARD, FULL }
    }
    
    /**
     * Get device information
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        cachedInfo?.let { return it }
        
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val info = DeviceInfo(
            cpuCores = getCpuCores(),
            cpuMaxFreqMHz = getCpuMaxFrequency(),
            totalRamMB = (memoryInfo.totalMem / (1024 * 1024)).toInt(),
            availableRamMB = (memoryInfo.availMem / (1024 * 1024)).toInt(),
            isLowRamDevice = activityManager.isLowRamDevice,
            sdkVersion = Build.VERSION.SDK_INT,
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER
        )
        
        cachedInfo = info
        
        android.util.Log.d("DeviceCapabilities", """
            |Device Info:
            |  Model: ${info.manufacturer} ${info.deviceModel}
            |  CPU: ${info.cpuCores} cores @ ${info.cpuMaxFreqMHz}MHz
            |  RAM: ${info.totalRamMB}MB total, ${info.availableRamMB}MB available
            |  Low RAM: ${info.isLowRamDevice}
            |  Score: ${info.overallScore}/100
        """.trimMargin())
        
        return info
    }
    
    /**
     * Get performance profile based on device capabilities
     * This is the Instagram-style adaptive rendering
     */
    fun getPerformanceProfile(context: Context): PerformanceProfile {
        cachedPerformanceProfile?.let { return it }
        
        val info = getDeviceInfo(context)
        val score = info.overallScore
        
        val profile = when {
            // ULTRA: Flagship devices (Score 80+)
            score >= 80 -> PerformanceProfile(
                tier = PerformanceProfile.Tier.ULTRA,
                imageQuality = PerformanceProfile.ImageQuality.HIGH,
                animationLevel = PerformanceProfile.AnimationLevel.FULL,
                videoPreloadCount = 3,
                imageCacheSize = 256,
                useHardwareLayers = true,
                enableBlurEffects = true,
                enableGradients = true,
                enableShadows = true,
                maxConcurrentDownloads = 6,
                scrollFlingMultiplier = 1.0f
            )
            
            // HIGH: Good mid-range devices (Score 60-79)
            score >= 60 -> PerformanceProfile(
                tier = PerformanceProfile.Tier.HIGH,
                imageQuality = PerformanceProfile.ImageQuality.HIGH,
                animationLevel = PerformanceProfile.AnimationLevel.STANDARD,
                videoPreloadCount = 2,
                imageCacheSize = 192,
                useHardwareLayers = true,
                enableBlurEffects = true,
                enableGradients = true,
                enableShadows = true,
                maxConcurrentDownloads = 4,
                scrollFlingMultiplier = 1.0f
            )
            
            // MEDIUM: Budget mid-range devices (Score 40-59)
            score >= 40 -> PerformanceProfile(
                tier = PerformanceProfile.Tier.MEDIUM,
                imageQuality = PerformanceProfile.ImageQuality.MEDIUM,
                animationLevel = PerformanceProfile.AnimationLevel.MINIMAL,
                videoPreloadCount = 1,
                imageCacheSize = 128,
                useHardwareLayers = true,
                enableBlurEffects = false, // Blur is expensive
                enableGradients = true,
                enableShadows = false, // Shadows are expensive
                maxConcurrentDownloads = 3,
                scrollFlingMultiplier = 1.2f // Faster fling for perceived speed
            )
            
            // LOW: Entry-level devices (Score <40)
            else -> PerformanceProfile(
                tier = PerformanceProfile.Tier.LOW,
                imageQuality = PerformanceProfile.ImageQuality.LOW,
                animationLevel = PerformanceProfile.AnimationLevel.NONE,
                videoPreloadCount = 0,
                imageCacheSize = 64,
                useHardwareLayers = false, // Can cause issues on very low devices
                enableBlurEffects = false,
                enableGradients = false, // Solid colors only
                enableShadows = false,
                maxConcurrentDownloads = 2,
                scrollFlingMultiplier = 1.5f // Fast fling compensation
            )
        }
        
        cachedPerformanceProfile = profile
        
        android.util.Log.d("DeviceCapabilities", """
            |Performance Profile: ${profile.tier}
            |  Image Quality: ${profile.imageQuality}
            |  Animations: ${profile.animationLevel}
            |  Video Preload: ${profile.videoPreloadCount}
            |  Blur Effects: ${profile.enableBlurEffects}
            |  Gradients: ${profile.enableGradients}
        """.trimMargin())
        
        return profile
    }
    
    /**
     * Get recommended image size in pixels based on device
     */
    fun getImageSize(context: Context): Int {
        return when (getPerformanceProfile(context).imageQuality) {
            PerformanceProfile.ImageQuality.LOW -> 256
            PerformanceProfile.ImageQuality.MEDIUM -> 384
            PerformanceProfile.ImageQuality.HIGH -> 512
        }
    }
    
    /**
     * Get video polling interval for state updates
     */
    fun getVideoPollInterval(context: Context): Long {
        return when (getPerformanceProfile(context).tier) {
            PerformanceProfile.Tier.LOW -> 1500L
            PerformanceProfile.Tier.MEDIUM -> 1000L
            PerformanceProfile.Tier.HIGH -> 750L
            PerformanceProfile.Tier.ULTRA -> 500L
        }
    }
    
    /**
     * Check if animations should be used
     */
    fun shouldAnimate(context: Context): Boolean {
        return getPerformanceProfile(context).animationLevel != PerformanceProfile.AnimationLevel.NONE
    }
    
    /**
     * Check if blur effects should be used
     */
    fun shouldUseBlur(context: Context): Boolean {
        return getPerformanceProfile(context).enableBlurEffects
    }
    
    /**
     * Check if gradient backgrounds should be used
     */
    fun shouldUseGradients(context: Context): Boolean {
        return getPerformanceProfile(context).enableGradients
    }
    
    // =========================================================================
    // Private helper functions
    // =========================================================================
    
    private fun getCpuCores(): Int {
        return try {
            Runtime.getRuntime().availableProcessors()
        } catch (e: Exception) {
            4 // Default assumption
        }
    }
    
    private fun getCpuMaxFrequency(): Int {
        return try {
            // Try to read from sysfs
            val cpuFreqFile = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
            if (cpuFreqFile.exists()) {
                val freqKHz = cpuFreqFile.readText().trim().toIntOrNull() ?: 0
                freqKHz / 1000 // Convert to MHz
            } else {
                // Fallback: estimate based on device age
                estimateCpuFrequency()
            }
        } catch (e: Exception) {
            estimateCpuFrequency()
        }
    }
    
    private fun estimateCpuFrequency(): Int {
        // Estimate CPU frequency based on Android version and cores
        val cores = getCpuCores()
        return when {
            Build.VERSION.SDK_INT >= 33 && cores >= 8 -> 2800 // Modern flagship
            Build.VERSION.SDK_INT >= 30 && cores >= 8 -> 2400 // Recent high-end
            Build.VERSION.SDK_INT >= 28 && cores >= 6 -> 2000 // Mid-range
            Build.VERSION.SDK_INT >= 26 && cores >= 4 -> 1800 // Budget mid
            else -> 1400 // Entry level
        }
    }
}

/**
 * Composable to remember device performance profile
 */
@Composable
fun rememberPerformanceProfile(): DeviceCapabilities.PerformanceProfile {
    val context = LocalContext.current
    return remember { DeviceCapabilities.getPerformanceProfile(context) }
}

/**
 * Composable to remember if animations should be used
 */
@Composable
fun rememberShouldAnimate(): Boolean {
    val context = LocalContext.current
    return remember { DeviceCapabilities.shouldAnimate(context) }
}

/**
 * Composable to remember device info
 */
@Composable
fun rememberDeviceInfo(): DeviceCapabilities.DeviceInfo {
    val context = LocalContext.current
    return remember { DeviceCapabilities.getDeviceInfo(context) }
}
