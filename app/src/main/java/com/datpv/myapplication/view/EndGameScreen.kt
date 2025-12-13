package com.datpv.myapplication.view

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.datpv.myapplication.R
import com.datpv.myapplication.admobManager.RewardedInterstitialAdManager
import com.datpv.myapplication.unit.AdFrequencyStore
import kotlinx.coroutines.launch

@Composable
fun EndGameScreen(
    score: Int,
    onPlayAgain: () -> Unit,
    onBackHome: () -> Unit,
    vm: RankingViewModel = viewModel()
) {
    LaunchedEffect(score) { vm.saveScore(score) }

    val context = LocalContext.current
    val activity = context as? Activity

    val rewardedInterstitialUnitId = stringResource(R.string.admob_rewarded_unit_id)
    val adManager = remember(rewardedInterstitialUnitId) {
        RewardedInterstitialAdManager(rewardedInterstitialUnitId)
    }

    // preload 1 lần khi vào screen
    LaunchedEffect(Unit) { adManager.preload(context) }

    var shouldShowAd by remember { mutableStateOf(false) }
    var isBlocking by remember { mutableStateOf(false) } // chặn thao tác + hiện loading
    val scope = rememberCoroutineScope()

    LoadingDialog(show = isBlocking)

    // Tính frequency
    LaunchedEffect(Unit) {
        val count = AdFrequencyStore.incrementEndGameCount(context)
        shouldShowAd = (count % AdFrequencyStore.NUMBER_DISPLAY_ADS == 0)

        if (count == 1) shouldShowAd = true

        if (count == AdFrequencyStore.NUMBER_RESET) {
            AdFrequencyStore.resetEndGameCount(context)
        }
    }

    // Chặn system back khi cần bắt xem ad hoặc đang loading
    BackHandler(enabled = shouldShowAd || isBlocking) {
        // Không làm gì -> user không thoát lách bằng back hệ thống
    }

    fun requireAdThen(action: () -> Unit) {
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

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.total_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        BannerAdTop(
            adUnitId = stringResource(R.string.admob_unit_id),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 32.dp,
                    end = 32.dp,
                    top = 140.dp,
                    bottom = 200.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = score.toString(),
                fontSize = 72.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFE91E63)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play Again
            Image(
                painter = painterResource(id = R.drawable.result_btn_01),
                contentDescription = "Play Again",
                modifier = Modifier
                    .size(width = 200.dp, height = 64.dp)
                    .clickable(enabled = !isBlocking) {
                        requireAdThen { onPlayAgain() }
                    }
            )

            // Back Home
            Image(
                painter = painterResource(id = R.drawable.ranking_btn_01),
                contentDescription = "Back Home",
                modifier = Modifier
                    .size(64.dp)
                    .clickable(enabled = !isBlocking) {
                        requireAdThen { onBackHome() }
                    }
            )
        }
    }
}

@Composable
private fun LoadingDialog(show: Boolean) {
    if (!show) return
    androidx.compose.ui.window.Dialog(onDismissRequest = { /* block dismiss */ }) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material3.CircularProgressIndicator()
                Text(
                    text = "Loading ad...",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
