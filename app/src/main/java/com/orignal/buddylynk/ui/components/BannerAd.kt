package com.orignal.buddylynk.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.orignal.buddylynk.data.ads.AdMobManager

/**
 * AdMob Banner Ad Composable
 * 
 * Usage: BannerAd(modifier = Modifier.fillMaxWidth())
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier
) {
    val adUnitId = AdMobManager.getBannerAdUnitId()
    
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(AdMobManager.getBannerAdUnitId())
                
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("BannerAd", "Banner ad loaded successfully")
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("BannerAd", "Banner ad failed to load: ${error.message}")
                    }
                    
                    override fun onAdClicked() {
                        Log.d("BannerAd", "Banner ad clicked")
                    }
                }
                
                // Load the ad
                loadAd(AdRequest.Builder().build())
            }
        },
        update = { adView ->
            // Reload ad when composition updates (optional)
        }
    )
}

/**
 * Large Banner Ad (320x100)
 */
@Composable
fun LargeBannerAd(
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.LARGE_BANNER)
                setAdUnitId(AdMobManager.getBannerAdUnitId())
                
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        Log.d("BannerAd", "Large banner ad loaded")
                    }
                    
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.e("BannerAd", "Large banner failed: ${error.message}")
                    }
                }
                
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
