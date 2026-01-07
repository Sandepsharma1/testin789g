package com.orignal.buddylynk.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform

/**
 * Google Certified CMP (Consent Management Platform)
 * 
 * Handles GDPR/CCPA consent for AdMob ads using Google's UMP SDK
 * 
 * Usage:
 *   ConsentManager.initialize(activity) {
 *       // Consent obtained, load ads
 *       AdMobManager.initialize(context)
 *   }
 */
object ConsentManager {
    private const val TAG = "ConsentManager"
    
    private var consentInformation: ConsentInformation? = null
    private var consentForm: ConsentForm? = null
    
    /**
     * Initialize consent management and request consent if needed
     * Call this BEFORE initializing AdMob
     */
    fun initialize(activity: Activity, onConsentComplete: () -> Unit) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()
        
        consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        
        consentInformation?.requestConsentInfoUpdate(
            activity,
            params,
            {
                // Consent info successfully updated
                Log.d(TAG, "Consent info updated. Status: ${consentInformation?.consentStatus}")
                
                if (isConsentFormAvailable()) {
                    loadAndShowConsentForm(activity, onConsentComplete)
                } else {
                    Log.d(TAG, "Consent form not required or not available")
                    onConsentComplete()
                }
            },
            { error ->
                // Consent info update failed
                Log.e(TAG, "Consent info update failed: ${error.message}")
                onConsentComplete() // Continue anyway
            }
        )
    }
    
    /**
     * Check if consent form is available to show
     */
    fun isConsentFormAvailable(): Boolean {
        return consentInformation?.isConsentFormAvailable == true
    }
    
    /**
     * Check if consent has been obtained
     */
    fun canRequestAds(): Boolean {
        return consentInformation?.canRequestAds() == true
    }
    
    /**
     * Get current consent status
     */
    fun getConsentStatus(): Int {
        return consentInformation?.consentStatus ?: ConsentInformation.ConsentStatus.UNKNOWN
    }
    
    /**
     * Load and show the consent form
     */
    private fun loadAndShowConsentForm(activity: Activity, onComplete: () -> Unit) {
        UserMessagingPlatform.loadConsentForm(
            activity,
            { form ->
                consentForm = form
                Log.d(TAG, "Consent form loaded")
                
                if (consentInformation?.consentStatus == ConsentInformation.ConsentStatus.REQUIRED) {
                    form.show(activity) { formError ->
                        if (formError != null) {
                            Log.e(TAG, "Consent form error: ${formError.message}")
                        } else {
                            Log.d(TAG, "Consent form dismissed. Status: ${consentInformation?.consentStatus}")
                        }
                        onComplete()
                    }
                } else {
                    Log.d(TAG, "Consent not required, form not shown")
                    onComplete()
                }
            },
            { formError ->
                Log.e(TAG, "Failed to load consent form: ${formError.message}")
                onComplete()
            }
        )
    }
    
    /**
     * Reset consent (for testing or user request)
     */
    fun reset(context: Context) {
        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        consentInformation?.reset()
        Log.d(TAG, "Consent information reset")
    }
    
    /**
     * Show privacy options form (for users to change consent)
     */
    fun showPrivacyOptionsForm(activity: Activity, onComplete: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.e(TAG, "Privacy options form error: ${formError.message}")
            }
            onComplete()
        }
    }
    
    /**
     * Check if privacy options are required (for GDPR regions)
     */
    fun isPrivacyOptionsRequired(): Boolean {
        return consentInformation?.privacyOptionsRequirementStatus == 
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }
}
