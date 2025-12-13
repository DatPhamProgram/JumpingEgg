package com.datpv.myapplication.admobManager

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class RewardedInterstitialAdManager(
    private val adUnitId: String
) {
    private var ad: RewardedInterstitialAd? = null
    private var isLoading = false

    fun isLoaded(): Boolean = ad != null
    fun isLoading(): Boolean = isLoading

    /**
     * Fire-and-forget preload (không suspend). Giữ như bạn đang dùng.
     */
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
                    Log.d(TAG, "RewardedInterstitial loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    ad = null
                    Log.e(TAG, "RewardedInterstitial failed: ${error.code} ${error.message}")
                }
            }
        )
    }

    /**
     * Suspend: đảm bảo ad loaded trước khi show.
     * - Nếu đã loaded -> true ngay
     * - Nếu đang loading -> đợi đến khi loaded/timeout
     * - Nếu chưa loading -> bắt đầu load rồi đợi
     */
    suspend fun awaitLoaded(
        context: Context,
        timeoutMs: Long = 8_000L,
        pollIntervalMs: Long = 60L
    ): Boolean = withContext(Dispatchers.Main) {
        if (ad != null) return@withContext true

        // Nếu chưa loading thì kick load
        if (!isLoading) preload(context)

        val start = SystemClock.elapsedRealtime()
        withTimeoutOrNull(timeoutMs) {
            while (ad == null && isLoading) {
                delay(pollIntervalMs)
            }
            // Nếu load xong mà ad != null => true; còn lại false
            ad != null
        } ?: run {
            // timeout
            val spent = SystemClock.elapsedRealtime() - start
            Log.w(TAG, "awaitLoaded() timeout after ${spent}ms")
            false
        }
    }

    /**
     * Show ngay nếu có ad.
     * Nếu không có ad -> trả false (để caller tự quyết: chờ/load rồi show)
     */
    fun showIfLoaded(
        activity: Activity,
        onRewardEarned: () -> Unit,
        onClosedOrFailed: () -> Unit
    ): Boolean {
        val currentAd = ad ?: return false

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

        currentAd.show(activity) {
            onRewardEarned()
        }
        return true
    }

    /**
     * Convenience: Nếu chưa loaded -> chờ load xong rồi show.
     * Dùng cái này để implement "bấm back -> loading -> show ad".
     *
     * onState: optional callback cho UI (Loading/Idle)
     */
    suspend fun showOrQueue(
        activity: Activity,
        timeoutMs: Long = 8_000L,
        onState: ((State) -> Unit)? = null,
        onRewardEarned: () -> Unit,
        onClosedOrFailed: () -> Unit,
        onLoadFailedOrTimeout: () -> Unit
    ) {
        // Nếu đã loaded thì show luôn
        if (showIfLoaded(activity, onRewardEarned, onClosedOrFailed)) return

        onState?.invoke(State.Loading)

        val ok = awaitLoaded(activity, timeoutMs = timeoutMs)

        onState?.invoke(State.Idle)

        if (!ok) {
            onLoadFailedOrTimeout()
            return
        }

        // Lúc này chắc chắn có ad -> show
        val shown = showIfLoaded(activity, onRewardEarned, onClosedOrFailed)
        if (!shown) {
            // hiếm nhưng vẫn possible nếu ad bị null giữa chừng
            onLoadFailedOrTimeout()
        }
    }

    enum class State { Loading, Idle }

    companion object {
        private const val TAG = "AdMob"
    }
}
