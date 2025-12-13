package com.datpv.myapplication.admobManager

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialAdManager(
    private val adUnitId: String
) {
    private var ad: InterstitialAd? = null
    private var isLoading = false

    fun preload(context: Context) {
        if (ad != null || isLoading) return
        isLoading = true

        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loadedAd: InterstitialAd) {
                    isLoading = false
                    ad = loadedAd
                    Log.d("AdMob", "Interstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    ad = null
                    Log.e("AdMob", "Interstitial failed: ${error.code} ${error.message}")
                }
            }
        )
    }

    fun show(activity: Activity, onClosedOrFailed: () -> Unit) {
        val currentAd = ad
        if (currentAd == null) {
            onClosedOrFailed()
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                preload(activity)
                onClosedOrFailed()
            }

            override fun onAdFailedToShowFullScreenContent(p0: com.google.android.gms.ads.AdError) {
                ad = null
                preload(activity)
                onClosedOrFailed()
            }
        }

        currentAd.show(activity)
    }
}
