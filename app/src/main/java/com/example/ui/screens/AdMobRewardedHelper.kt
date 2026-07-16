package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object AdMobRewardedHelper {
    private const val TAG = "AdMobRewarded"
    
    // Official test unit ID for Rewarded Ads
    private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    
    private var mRewardedAd: RewardedAd? = null
    private var isAdLoading = false

    fun loadAd(context: Context) {
        if (mRewardedAd != null || isAdLoading) {
            return
        }

        isAdLoading = true
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, "Rewarded Ad failed to load: ${adError.message}")
                    mRewardedAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d(TAG, "Rewarded Ad was loaded.")
                    mRewardedAd = rewardedAd
                    isAdLoading = false
                }
            }
        )
    }

    fun showAd(activity: Activity, onRewardEarned: (Int) -> Unit = {}, onAdClosed: () -> Unit = {}) {
        val ad = mRewardedAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad dismissed full screen content.")
                    mRewardedAd = null
                    onAdClosed()
                    // Preload the next rewarded ad
                    loadAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d(TAG, "Rewarded Ad failed to show: ${adError.message}")
                    mRewardedAd = null
                    onAdClosed()
                    // Try preloading again
                    loadAd(activity)
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Rewarded Ad showed full screen content.")
                }
            }
            ad.show(activity) { rewardItem ->
                val amount = rewardItem.amount
                val type = rewardItem.type
                Log.d(TAG, "User earned reward: $amount $type")
                onRewardEarned(amount)
            }
        } else {
            Log.d(TAG, "Rewarded Ad wasn't ready yet.")
            onAdClosed()
            loadAd(activity)
        }
    }
}
