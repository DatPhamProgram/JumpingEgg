package com.datpv.myapplication.view

import android.app.Activity
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
import androidx.compose.runtime.remember
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


@Composable
fun EndGameScreen(
    score: Int,
    onPlayAgain: () -> Unit,
    onBackHome: () ->Unit,
    vm: RankingViewModel = viewModel()
){
    LaunchedEffect(score) {
        vm.saveScore(score)
    }

    val context = LocalContext.current
    val activity = context as? Activity

    val rewardedInterstitialUnitId = stringResource(R.string.admob_rewarded_unit_id)

    val adManager = remember(rewardedInterstitialUnitId) {
        RewardedInterstitialAdManager(rewardedInterstitialUnitId)
    }

    LaunchedEffect(Unit) {
        adManager.preload(context)
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // ===== Background =====
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

        // ===== SCORE CENTER (giữa khung trắng) =====
        Box(
            modifier = Modifier
                .fillMaxSize()
                // chỉnh lại nếu sau này đổi background
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
                color = Color(0xFFE91E63) // pink giống thiết kế
            )
        }

        // ===== BUTTON AREA =====
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
                    .clickable {

                        if(activity == null) {
                            onPlayAgain()
                            return@clickable
                        }
                        adManager.show(
                            activity = activity,
                            onRewardEarned = {
                                // ✅ Xem xong + được reward -> vào game
                                onPlayAgain()
                            },
                            onClosedOrFailed = {
                                // ✅ User đóng/ads fail -> vẫn vào game (đỡ kẹt UX)
                                onPlayAgain()
                            }
                        )
                    }
            )

            // Back Home
            Image(
                painter = painterResource(id = R.drawable.ranking_btn_01),
                contentDescription = "Back Home",
                modifier = Modifier
                    .size(64.dp)
                    .clickable { onBackHome() }
            )
        }
    }
}