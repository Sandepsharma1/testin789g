package com.orignal.buddylynk.ui.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Permission Manager for handling runtime permissions
 * 
 * Handles:
 * - Camera permission (for QR scanner, video calls)
 * - Notification permission (Android 13+)
 * - Media access (for photos/videos)
 * - Audio permission (for voice/video calls)
 */
object PermissionManager {
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 13
        }
    }
    
    /**
     * Check if audio/microphone permission is granted
     */
    fun hasAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if media (photos/videos) permission is granted
     */
    fun hasMediaPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Get all required permissions for the app
     */
    fun getAllRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        return permissions.toTypedArray()
    }
    
    /**
     * Get camera-related permissions
     */
    fun getCameraPermissions(): Array<String> {
        return arrayOf(Manifest.permission.CAMERA)
    }
    
    /**
     * Get media-related permissions
     */
    fun getMediaPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    
    /**
     * Get notification permission (Android 13+)
     */
    fun getNotificationPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }
}

/**
 * Composable to request camera permission
 */
@Composable
fun rememberCameraPermissionState(
    onPermissionResult: (Boolean) -> Unit
): PermissionState {
    val context = LocalContext.current
    var hasPermission by remember { 
        mutableStateOf(PermissionManager.hasCameraPermission(context)) 
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        onPermissionResult(granted)
    }
    
    return remember(hasPermission) {
        PermissionState(
            hasPermission = hasPermission,
            requestPermission = {
                if (!hasPermission) {
                    launcher.launch(Manifest.permission.CAMERA)
                }
            }
        )
    }
}

/**
 * Composable to request multiple permissions at once
 */
@Composable
fun rememberMultiplePermissionsState(
    permissions: Array<String>,
    onPermissionsResult: (Map<String, Boolean>) -> Unit
): MultiplePermissionsState {
    val context = LocalContext.current
    var permissionsGranted by remember {
        mutableStateOf(
            permissions.all { 
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
            }
        )
    }
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
        onPermissionsResult(results)
    }
    
    return remember(permissionsGranted) {
        MultiplePermissionsState(
            allPermissionsGranted = permissionsGranted,
            requestPermissions = {
                launcher.launch(permissions)
            }
        )
    }
}

/**
 * State holder for single permission
 */
data class PermissionState(
    val hasPermission: Boolean,
    val requestPermission: () -> Unit
)

/**
 * State holder for multiple permissions
 */
data class MultiplePermissionsState(
    val allPermissionsGranted: Boolean,
    val requestPermissions: () -> Unit
)
