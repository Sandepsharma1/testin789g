package com.orignal.buddylynk.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AdMob Manager - Handles all ad operations for BuddyLynk
 * 
 * Usage:
 *   AdMobManager.initialize(context)
 *   AdMobManager.showInterstitial(activity)
 *   AdMobManager.showRewarded(activity) { rewardAmount -> ... }
 */
object AdMobManager {
    private const val TAG = "AdMobManager"
    
    // Production Ad Unit IDs
    private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6164570294408123/5141538580"
    private const val BANNER_AD_UNIT_ID = "ca-app-pub-6164570294408123/5361046345"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917" // Test ID until you create one
    
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false
    
    /**
     * Initialize Mobile Ads SDK - Call once at app startup
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MobileAds.initialize(context) { initStatus ->
                    Log.d(TAG, "AdMob initialized: $initStatus")
                    isInitialized = true
                    
                    // Pre-load ads
                    loadInterstitialAd(context)
                    loadRewardedAd(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AdMob: ${e.message}")
            }
        }
    }
    
    /**
     * Load Interstitial Ad
     */
    fun loadInterstitialAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(context, INTERSTITIAL_AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial ad loaded")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                }
            })
    }
    
    /**
     * Show Interstitial Ad
     * @return true if ad was shown, false if not available
     */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}): Boolean {
        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "Interstitial not ready")
            onDismissed()
            return false
        }
        
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitialAd(activity) // Reload for next time
                onDismissed()
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitialAd(activity)
                onDismissed()
            }
        }
        
        ad.show(activity)
        return true
    }
    
    /**
     * Load Rewarded Ad
     */
    fun loadRewardedAd(context: Context) {
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(context, REWARDED_AD_UNIT_ID, adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Rewarded ad loaded")
                }
                
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    Log.e(TAG, "Rewarded failed to load: ${error.message}")
                }
            })
    }
    
    /**
     * Show Rewarded Ad
     * @param onRewarded Callback with reward amount when user earns reward
     * @return true if ad was shown, false if not available
     */
    fun showRewarded(activity: Activity, onRewarded: (Int) -> Unit = {}): Boolean {
        val ad = rewardedAd
        if (ad == null) {
            Log.d(TAG, "Rewarded ad not ready")
            return false
        }
        
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewardedAd(activity) // Reload for next time
            }
            
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewardedAd(activity)
            }
        }
        
        ad.show(activity) { reward ->
            Log.d(TAG, "User earned reward: ${reward.amount} ${reward.type}")
            onRewarded(reward.amount)
        }
        return true
    }
    
    /**
     * Check if interstitial is ready
     */
    fun isInterstitialReady(): Boolean = interstitialAd != null
    
    /**
     * Check if rewarded is ready
     */
    fun isRewardedReady(): Boolean = rewardedAd != null
    
    /**
     * Get Banner Ad Unit ID for Compose BannerAd
     */
    fun getBannerAdUnitId(): String = BANNER_AD_UNIT_ID
}
