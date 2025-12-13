package com.datpv.myapplication.admobManager

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicBoolean

class InterstitialAdManager(
    private val adUnitId: String
) {
    private var interstitialAd: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)

    fun isReady(): Boolean = interstitialAd != null

    fun preload(
        context: Context,
        onLoaded: (() -> Unit)? = null,
        onFailed: ((LoadAdError) -> Unit)? = null
    ) {
        if (isLoading.get()) return
        if (interstitialAd != null) {
            onLoaded?.invoke()
            return
        }

        isLoading.set(true)

        val request = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            request,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading.set(false)
                    onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isLoading.set(false)
                    onFailed?.invoke(error)
                }
            }
        )
    }

    /**
     * Show interstitial if ready, else fallback and try preload again.
     */
    fun show(
        activity: Activity,
        onClosedOrFailed: () -> Unit
    ) {
        val ad = interstitialAd
        if (ad == null) {
            // Chưa load xong -> coi như fail, không block UX
            onClosedOrFailed()
            // cố gắng load cho lần sau
            preload(activity.applicationContext)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                onClosedOrFailed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                onClosedOrFailed()
            }

            override fun onAdShowedFullScreenContent() {
                // đã show -> clear reference để lần sau load mới
                interstitialAd = null
            }
        }

        ad.show(activity)
    }
}
