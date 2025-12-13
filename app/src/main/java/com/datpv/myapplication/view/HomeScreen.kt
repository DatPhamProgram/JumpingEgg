package com.datpv.myapplication.view

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import com.datpv.myapplication.R
import com.datpv.myapplication.admobManager.InterstitialAdManager
import com.datpv.myapplication.unit.AdFrequencyStore
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onStartGameClick: () -> Unit,
    onRankingClick: () -> Unit,
    onInstructionClick: () -> Unit,
    onDoaGameClick: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val interstitialUnitId = stringResource(R.string.admob_interstitial_unit_id)
    val adManager = remember(interstitialUnitId) { InterstitialAdManager(interstitialUnitId) }

    val scope = rememberCoroutineScope()

    var shouldShowAd by remember { mutableStateOf(false) }
    var isStartProcessing by remember { mutableStateOf(false) } // chặn double click
    var showLoading by remember { mutableStateOf(false) }       // dialog loading

    LoadingDialog(show = showLoading)

    // Preload + tính frequency
    LaunchedEffect(Unit) {
        adManager.preload(context)

        val count = AdFrequencyStore.incrementHomeCount(context)
        shouldShowAd = (count % AdFrequencyStore.NUMBER_HOME_DISPLAY_ADS == 0)

        if (count >= AdFrequencyStore.NUMBER_RESET) {
            AdFrequencyStore.resetHomeCount(context)
        }
    }

    fun requireInterstitialThen(action: () -> Unit) {
        val act = activity
        if (!shouldShowAd || act == null) {
            action()
            return
        }
        if (isStartProcessing) return

        scope.launch {
            isStartProcessing = true
            showLoading = true

            adManager.showOrQueue(
                activity = act,
                timeoutMs = 10_000L,
                onState = { state ->
                    showLoading = (state == InterstitialAdManager.State.Loading)
                },
                onClosedOrFailed = {
                    showLoading = false
                    isStartProcessing = false
                    // preload lại cho lần sau
                    scope.launch { adManager.preload(context) }
                    action()
                },
                onLoadFailedOrTimeout = {
                    // fallback để không kẹt
                    showLoading = false
                    isStartProcessing = false
                    scope.launch { adManager.preload(context) }
                    action()
                }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.background),
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

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.top_title),
                contentDescription = "Jumping Egg Title",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(bottom = 26.dp),
                contentScale = ContentScale.Fit
            )

            // ✅ Start Game: show interstitial (nếu cần) rồi mới vào game
            MenuButton(
                resId = R.drawable.top_btn_01,
                contentDescription = "Start Game",
                onClick = { requireInterstitialThen { onStartGameClick() } }
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                resId = R.drawable.top_btn_02,
                contentDescription = "Ranking",
                onClick = onRankingClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                resId = R.drawable.top_btn_03,
                contentDescription = "Instruction",
                onClick = onInstructionClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            MenuButton(
                resId = R.drawable.top_btn_04,
                contentDescription = "D.O.A Game",
                onClick = onDoaGameClick
            )
        }
    }
}

@Composable
private fun MenuButton(
    resId: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Image(
        painter = painterResource(id = resId),
        contentDescription = contentDescription,
        modifier = Modifier
            .width(260.dp)
            .height(64.dp)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun LoadingDialog(show: Boolean) {
    if (!show) return
    androidx.compose.ui.window.Dialog(onDismissRequest = { /* block dismiss */ }) {
        androidx.compose.material3.Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
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
