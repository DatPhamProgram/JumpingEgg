package com.datpv.myapplication.admobManager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback

class RewardedInterstitialAdManager(
    private val adUnitId: String
) {
    private var ad: RewardedInterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (ad != null || isLoading) return
        isLoading = true

        RewardedInterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedInterstitialAd) {
                    isLoading = false
                    ad = loadedAd
                    Log.d("AdMob", "RewardedInterstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    ad = null
                    Log.e("AdMob", "RewardedInterstitial failed: ${error.code} ${error.message}")
                }
            }
        )
    }

    fun show(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onClosedOrFailed: () -> Unit
    ) {
        val currentAd = ad
        if (currentAd == null) {
            onClosedOrFailed()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload(activity) // load lại cho lần sau
                onClosedOrFailed()
            }

            override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                ad = null
                preload(activity)
                onClosedOrFailed()
            }
        }

        currentAd.show(activity) { rewardItem ->
            // rewardItem.amount / rewardItem.type nếu bạn cần
            onRewardEarned()
        }
    }
}

