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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.datpv.myapplication.R
import com.datpv.myapplication.admobManager.InterstitialAdManager
import com.datpv.myapplication.admobManager.RewardedInterstitialAdManager
import com.datpv.myapplication.util.AdFrequencyStore
import com.datpv.myapplication.viewmodel.DOAGameViewModel
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

    val scope = rememberCoroutineScope()
    val vm = remember { DOAGameViewModel() }

    // =========================
    // ✅ ADS: Interstitial khi mở màn hình (mỗi 3 lần mở)
    // =========================
    val interstitialUnitId = stringResource(R.string.admob_interstitial_unit_id)
    val interstitialManager = remember(interstitialUnitId) {
        InterstitialAdManager(interstitialUnitId)
    }
    var isOpenAdShowing by remember { mutableStateOf(false) }
    var isOpenAdLoading by remember { mutableStateOf(false) }

    LoadingDialog(show = vm.isBlocking || isOpenAdLoading)

    LaunchedEffect(Unit) {
        vm.loadShouldShowAd(context)

        interstitialManager.preload(context)

        val act = activity
        if (act != null &&
            AdFrequencyStore.decideShowAd(context, AdFrequencyStore.AdSurface.DOA_OPEN)
        ) {
            isOpenAdShowing = true
            isOpenAdLoading = true

            interstitialManager.showOrQueue(
                activity = act,
                timeoutMs = 10_000L,
                onState = { state ->
                    isOpenAdLoading = (state == InterstitialAdManager.State.Loading)
                },
                onClosedOrFailed = {
                    isOpenAdLoading = false
                    isOpenAdShowing = false
                    scope.launch { interstitialManager.preload(context) }
                },
                onLoadFailedOrTimeout = {
                    isOpenAdLoading = false
                    isOpenAdShowing = false
                    scope.launch { interstitialManager.preload(context) }
                }
            )
        }
    }

    // Chặn system back khi cần bắt xem ad hoặc đang loading
    BackHandler(enabled = vm.shouldShowAd || vm.isBlocking || isOpenAdShowing) {
        // Không làm gì -> user không thoát lách bằng back hệ thống
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
                vm.requireAdThen(scope, activity, adManager, context) { onBack() }
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
