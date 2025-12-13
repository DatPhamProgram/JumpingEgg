package com.datpv.myapplication.view

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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

    LaunchedEffect(Unit) {
        adManager.preload(context)
    }

    var shouldShowAd by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val count = AdFrequencyStore.incrementDoaBackCount(context)

        shouldShowAd =
            count == 1 || count % AdFrequencyStore.NUMBER_DISPLAY_ADS == 0

        if (count >= AdFrequencyStore.NUMBER_RESET) {
            AdFrequencyStore.resetDoaBackCount(context)
        }
    }


    Box(modifier = Modifier.fillMaxSize()) {

        // Background image
        Image(
            painter = painterResource(id = R.drawable.doa),
            contentDescription = "DOA Game Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        BannerAdTop(
            adUnitId = stringResource(R.string.admob_unit_id),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )

        // Back button at bottom
        Button(
            onClick = {
                if (!shouldShowAd || activity == null) {
                    onBack()
                    return@Button
                }

                adManager.show(
                    activity = activity,
                    onRewardEarned = {
                        scope.launch { adManager.preload(context) }
                        onBack()
                    },
                    onClosedOrFailed = {
                        scope.launch { adManager.preload(context) }
                        onBack()
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 120.dp)
        ) {
            Text("Back")
        }
    }
}
