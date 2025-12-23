package com.orignal.buddylynk.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import java.io.File
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

/**
 * Security Manager for BuddyLynk
 * Handles root detection, tampering detection, and secure storage
 */
object SecurityManager {
    
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
    
    // Obfuscated key - will be further obfuscated by ProGuard
    private val secretKey = "BuddyLynkSecure2024Key!@#$%^&*"
    
    /**
     * Check if device is rooted
     */
    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3()
    }
    
    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }
    
    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }
    
    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val bufferedReader = process.inputStream.bufferedReader()
            bufferedReader.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }
    
    /**
     * Check if app is debuggable (tampered)
     */
    fun isAppDebuggable(context: Context): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * Encrypt sensitive data
     */
    fun encrypt(data: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val keySpec = SecretKeySpec(getKeyBytes(), ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, keySpec)
            val encrypted = cipher.doFinal(data.toByteArray())
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (e: Exception) {
            data // Fallback to original if encryption fails
        }
    }
    
    /**
     * Decrypt sensitive data
     */
    fun decrypt(encryptedData: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val keySpec = SecretKeySpec(getKeyBytes(), ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decoded = Base64.decode(encryptedData, Base64.DEFAULT)
            val decrypted = cipher.doFinal(decoded)
            String(decrypted)
        } catch (e: Exception) {
            encryptedData // Fallback to original if decryption fails
        }
    }
    
    private fun getKeyBytes(): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(secretKey.toByteArray()).copyOf(16)
    }
    
    /**
     * Generate secure hash for integrity checking
     */
    fun generateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }
}