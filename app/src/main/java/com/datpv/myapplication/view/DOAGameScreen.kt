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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.datpv.myapplication.R
import com.datpv.myapplication.admobManager.RewardedInterstitialAdManager

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

    Box(modifier = Modifier.fillMaxSize()) {

        // Background image
        Image(
            painter = painterResource(id = R.drawable.doa),
            contentDescription = "DOA Game Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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
                if (activity == null) {
                    onBack()
                    return@Button
                }

                adManager.show(
                    activity = activity,
                    onRewardEarned = {
                        onBack()
                    },
                    onClosedOrFailed = {
                        onBack()
                    }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text("Back")
        }
    }
}
