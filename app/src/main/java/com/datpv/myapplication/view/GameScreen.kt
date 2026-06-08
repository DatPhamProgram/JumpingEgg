package com.datpv.myapplication.view

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.datpv.myapplication.R
import com.datpv.myapplication.admobManager.InterstitialAdManager
import com.datpv.myapplication.util.AdFrequencyStore
import com.datpv.myapplication.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GameScreen(
    onGameOver: (finalScore: Int) -> Unit
) {
    // =========================
    // ✅ ADS: Interstitial
    // =========================
    val context = LocalContext.current
    val activity = context as? Activity

    val interstitialUnitId = stringResource(R.string.admob_interstitial_unit_id)
    val interstitialManager = remember(interstitialUnitId) { InterstitialAdManager(interstitialUnitId) }

    val scope = rememberCoroutineScope()
    val vm = remember { GameViewModel() }

    // preload sẵn
    LaunchedEffect(Unit) {
        interstitialManager.preload(context)
    }

    // Wire up ad display for the score milestones the ViewModel reports.
    // Kept here (not in the VM) because it needs the Activity-bound ad manager.
    SideEffect {
        vm.onScoreMilestone = {
            val act = activity
            if (act != null) {
                scope.launch {
                    // Tôn trọng cooldown chung giữa các lần hiện ads (mọi màn hình),
                    // tránh việc vừa rời màn khác có ads xong lại gặp ads ngay trong game.
                    if (!AdFrequencyStore.canShowAdNow(context)) return@launch
                    AdFrequencyStore.markAdShown(context)

                    vm.isAdShowing = true
                    vm.showAdLoading = true

                    // showOrQueue: nếu chưa load kịp -> loading -> load xong -> show
                    interstitialManager.showOrQueue(
                        activity = act,
                        timeoutMs = 10_000L,
                        onState = { state ->
                            vm.showAdLoading = (state == InterstitialAdManager.State.Loading)
                        },
                        onClosedOrFailed = {
                            // đóng ads / fail -> resume game
                            vm.showAdLoading = false
                            vm.isAdShowing = false
                            // preload lại cho lần sau
                            scope.launch { interstitialManager.preload(context) }
                        },
                        onLoadFailedOrTimeout = {
                            // timeout/no-fill -> resume game luôn
                            vm.showAdLoading = false
                            vm.isAdShowing = false
                            scope.launch { interstitialManager.preload(context) }
                        }
                    )
                }
            }
        }
    }

    // ✅ Loading dialog (chỉ ads)
    LoadingDialog(show = vm.showAdLoading)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value

        LaunchedEffect(w, h) {
            vm.initialize(w, h)
        }

        // =========================
        // ✅ GAME LOOP
        // =========================
        LaunchedEffect(Unit) {
            while (true) {
                vm.tick(scope)
                delay(16L) // 60fps
            }
        }

        // =========================
        // UI
        // =========================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { vm.jump() }
        ) {
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Draw baskets (SCREEN = world - camera)
            vm.baskets.forEachIndexed { idx, b ->
                val screenY = (b.y - vm.cameraY.value).dp
                if (vm.basketWithEgg == idx) {
                    Image(
                        painter = painterResource(id = R.drawable.grow_purple_egg),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = b.x.dp, y = screenY)
                            .size(vm.growW.dp)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.basket01),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = b.x.dp, y = screenY)
                            .size(vm.basketW.dp)
                    )
                }
            }

            // Egg (only when outside)
            if (vm.basketWithEgg < 0) {
                Image(
                    painter = painterResource(id = R.drawable.egg_purple),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = vm.eggX.dp, y = (vm.eggY - vm.cameraY.value).dp)
                        .size(vm.eggW.dp)
                )
            }

            vm.onGameOverDetected(onGameOver)

            // Banner score (✅ totalScore không reset khi scroll)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bannerscore),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Text(
                    text = vm.totalScore.toString(),
                    color = Color.Red,
                    fontSize = 32.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 118.dp, bottom = 2.dp)
                )
            }
        }
    }
}

/**
 * ✅ Chỉ phục vụ phần Ads loading (không ảnh hưởng game logic)
 */
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
