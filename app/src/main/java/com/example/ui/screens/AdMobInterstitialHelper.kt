package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobInterstitialHelper {
    private const val TAG = "AdMobInterstitial"
    
    // Official test unit ID for Interstitials
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    
    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false

    fun loadAd(context: Context) {
        if (mInterstitialAd != null || isAdLoading) {
            return
        }

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Ad failed to load: ${adError.message}")
                    mInterstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    isAdLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity, onAdClosed: () -> Unit = {}) {
        val ad = mInterstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad dismissed full screen content.")
                    mInterstitialAd = null
                    onAdClosed()
                    // Preload the next interstitial
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d(TAG, "Ad failed to show: ${adError.message}")
                    mInterstitialAd = null
                    onAdClosed()
                    // Try preloading again
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed full screen content.")
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Ad wasn't ready yet.")
            onAdClosed()
            loadAd(activity)
        }
    }
}
