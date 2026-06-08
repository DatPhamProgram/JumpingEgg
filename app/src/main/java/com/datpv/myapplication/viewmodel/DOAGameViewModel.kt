package com.datpv.myapplication.viewmodel

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.datpv.myapplication.admobManager.RewardedInterstitialAdManager
import com.datpv.myapplication.util.AdFrequencyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Holds the ad-gating state for the DOA screen's back action: whether a rewarded
 * interstitial must be shown before leaving, and whether that flow is currently blocking
 * the UI. Plain state-holder (not androidx.lifecycle.ViewModel) since the ad manager and
 * Activity it depends on are owned by the screen and must not be retained across rotations.
 */
class DOAGameViewModel {

    var shouldShowAd by mutableStateOf(false)
        private set

    // chặn double click + hiện loading
    var isBlocking by mutableStateOf(false)
        private set

    suspend fun loadShouldShowAd(context: Context) {
        shouldShowAd = AdFrequencyStore.decideShowAd(context, AdFrequencyStore.AdSurface.DOA_BACK)
    }

    fun requireAdThen(
        scope: CoroutineScope,
        activity: Activity?,
        adManager: RewardedInterstitialAdManager,
        context: Context,
        action: () -> Unit
    ) {
        // Nếu không cần show ad hoặc không có activity => chạy luôn
        if (!shouldShowAd || activity == null) {
            action()
            return
        }

        // Nếu đang chạy flow ads rồi thì bỏ qua click spam
        if (isBlocking) return

        scope.launch {
            isBlocking = true

            // Đợi load xong rồi show (timeout tùy bạn)
            adManager.showOrQueue(
                activity = activity,
                timeoutMs = 12_000L,
                onState = { state ->
                    isBlocking = (state == RewardedInterstitialAdManager.State.Loading)
                },
                onRewardEarned = {
                    isBlocking = false
                    action()
                },
                onClosedOrFailed = {
                    // Ad đã hiển thị và user đóng (hoặc show fail) -> cho đi tiếp
                    isBlocking = false
                    action()
                },
                onLoadFailedOrTimeout = {
                    // Bạn nói "bắt buộc": nếu muốn cứng hơn, bạn có thể retry ở đây.
                    // Nhưng nếu no-fill/mất mạng mà hard-block vô hạn sẽ kẹt app.
                    // Mình chọn fallback để không kẹt:
                    isBlocking = false
                    action()
                }
            )
        }
    }
}
