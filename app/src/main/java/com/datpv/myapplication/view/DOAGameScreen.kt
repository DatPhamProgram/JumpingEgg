package com.datpv.myapplication.view

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.datpv.myapplication.R
import com.datpv.myapplication.admobManager.RewardedInterstitialAdManager
import com.datpv.myapplication.unit.AdFrequencyStore
import kotlinx.coroutines.launch

@Composable
fun DOAGameScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val rewardedInterstitialUnitId = stringResource(R.string.admob_rewarded_unit_id)

    val adManager = remember(rewardedInterstitialUnitId) {
        RewardedInterstitialAdManager(rewardedInterstitialUnitId)
    }

    var shouldShowAd by remember { mutableStateOf(false) }

    // ✅ chặn double click (NHƯNG sẽ reset khi quay lại screen)
    var isBlocking by remember { mutableStateOf(false) } // chặn thao tác + hiện loading
    val scope = rememberCoroutineScope()

    LoadingDialog(show = isBlocking)

    LaunchedEffect(Unit) {
        val count = AdFrequencyStore.incrementDoaBackCount(context)
        shouldShowAd = (count % AdFrequencyStore.NUMBER_DISPLAY_ADS == 0)

        if (count == 1) shouldShowAd = true

        if (count == AdFrequencyStore.NUMBER_RESET) {
            AdFrequencyStore.resetDoaBackCount(context)
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

        // Background
        Image(
            painter = painterResource(id = R.drawable.doa),
            contentDescription = "DOA Game Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Banner
        BannerAdTop(
            adUnitId = stringResource(R.string.admob_unit_id),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(60.dp)
        )

        // ✅ Back Button (GIỐNG ban đầu)
        Button(
            onClick = {
              requireAdThen { onBack.invoke() }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
                .zIndex(9999F)
        ) {
            Text("Back")
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