package com.datpv.myapplication.admobManager

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

class InterstitialAdManager(
    private val adUnitId: String
) {
    private var interstitialAd: InterstitialAd? = null
    private val isLoading = AtomicBoolean(false)

    fun isReady(): Boolean = interstitialAd != null
    fun isLoading(): Boolean = isLoading.get()

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

    private suspend fun awaitLoaded(
        context: Context,
        timeoutMs: Long = 8_000L,
        pollIntervalMs: Long = 60L
    ): Boolean = withContext(Dispatchers.Main) {
        if (interstitialAd != null) return@withContext true

        // kick load nếu chưa load
        if (!isLoading.get()) {
            preload(context)
        }

        withTimeoutOrNull(timeoutMs) {
            while (interstitialAd == null && isLoading.get()) {
                delay(pollIntervalMs)
            }
            interstitialAd != null
        } ?: false
    }

    fun showIfReady(
        activity: Activity,
        onClosedOrFailed: () -> Unit
    ): Boolean {
        val ad = interstitialAd ?: return false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                // preload cho lần sau
                preload(activity.applicationContext)
                onClosedOrFailed()
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                interstitialAd = null
                preload(activity.applicationContext)
                onClosedOrFailed()
            }

            override fun onAdShowedFullScreenContent() {
                // clear reference để lần sau load mới
                interstitialAd = null
            }
        }

        ad.show(activity)
        return true
    }

    enum class State { Loading, Idle }

    /**
     * ✅ Nếu chưa ready -> hiện loading (UI làm ở caller), await load xong rồi show.
     */
    suspend fun showOrQueue(
        activity: Activity,
        timeoutMs: Long = 8_000L,
        onState: ((State) -> Unit)? = null,
        onClosedOrFailed: () -> Unit,
        onLoadFailedOrTimeout: () -> Unit
    ) {
        // ready -> show luôn
        if (showIfReady(activity, onClosedOrFailed)) return

        onState?.invoke(State.Loading)

        val ok = awaitLoaded(activity.applicationContext, timeoutMs = timeoutMs)

        onState?.invoke(State.Idle)

        if (!ok) {
            onLoadFailedOrTimeout()
            return
        }

        val shown = showIfReady(activity, onClosedOrFailed)
        if (!shown) onLoadFailedOrTimeout()
    }
}
