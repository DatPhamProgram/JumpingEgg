package com.datpv.myapplication.view

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    var isStartProcessing by remember { mutableStateOf(false) } // ✅ chặn double click

    // ✅ Chỉ preload + tính shouldShowAd (KHÔNG auto show)
    LaunchedEffect(Unit) {
        adManager.preload(context)

        val count = AdFrequencyStore.incrementHomeCount(context)
        shouldShowAd = (count % AdFrequencyStore.NUMBER_HOME_DISPLAY_ADS == 0)

        if (count >= AdFrequencyStore.NUMBER_RESET) {
            AdFrequencyStore.resetHomeCount(context)
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

            // ✅ Start Game: nếu shouldShowAd -> show ads rồi mới vào game
            MenuButton(
                resId = R.drawable.top_btn_01,
                contentDescription = "Start Game",
                onClick = {
                    if (isStartProcessing) return@MenuButton
                    isStartProcessing = true

                    val act = activity
                    if (!shouldShowAd || act == null) {
                        isStartProcessing = false
                        onStartGameClick()
                        return@MenuButton
                    }

                    adManager.show(
                        activity = act,
                        onClosedOrFailed = {
                            scope.launch { adManager.preload(context) }
                            isStartProcessing = false
                            onStartGameClick()
                        }
                    )
                }
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
