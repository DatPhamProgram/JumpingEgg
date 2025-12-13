package com.datpv.myapplication.view

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.datpv.myapplication.R
import com.datpv.myapplication.admobManager.InterstitialAdManager
import com.datpv.myapplication.unit.AdFrequencyStore
import kotlinx.coroutines.launch

@Composable
fun RankingScreen(
    onBack: () -> Unit,
    viewModel: RankingViewModel = viewModel(),
) {
    val top5 by viewModel.topScores.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val activity = context as? Activity

    val interstitialUnitId = stringResource(R.string.admob_interstitial_unit_id)
    val adManager = remember(interstitialUnitId) { InterstitialAdManager(interstitialUnitId) }

    val scope = rememberCoroutineScope()

    // preload sẵn để giảm khả năng phải loading
    LaunchedEffect(Unit) {
        adManager.preload(context)
    }

    var isBackProcessing by remember { mutableStateOf(false) } // chặn spam click
    var showLoading by remember { mutableStateOf(false) }      // hiển thị dialog

    LoadingDialog(show = showLoading)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight

        val contentLeft = w * 0.14f
        val contentTop = h * 0.05f
        val contentWidth = w * 0.72f
        val contentHeight = h * 0.48f

        // Background
        Image(
            painter = painterResource(id = R.drawable.ranking_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Banner top
        BannerAdTop(
            adUnitId = stringResource(R.string.admob_unit_id),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 12.dp)
        )

        // Bảng điểm
        Box(
            modifier = Modifier
                .offset(x = contentLeft, y = h * 0.26f)
                .size(contentWidth, h * 0.40f)
                .padding(horizontal = 10.dp)
                .clipToBounds()
        ) {
            val rows = List(5) { idx -> top5.getOrNull(idx) ?: 0 }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = contentTop, bottom = contentHeight),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(rows) { index, score ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}.",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF8D2C1F),
                            modifier = Modifier.width(48.dp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = score.toString(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Point",
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFFB300),
                            modifier = Modifier.width(120.dp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // Overlay để tránh AdView “ăn touch”
        Box(
            modifier = Modifier
                .matchParentSize()
                .zIndex(999999f)
        ) {
            BackButton(
                enabled = !isBackProcessing,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 90.dp)
            ) {
                if (isBackProcessing) return@BackButton
                isBackProcessing = true

                val act = activity
                if (act == null) {
                    isBackProcessing = false
                    onBack()
                    return@BackButton
                }

                scope.launch {
                    val count = AdFrequencyStore.incrementRankingCount(context)
                    val shouldShowAd =
                        (count == 1) || (count % AdFrequencyStore.NUMBER_DISPLAY_ADS == 0)

                    if (count >= AdFrequencyStore.NUMBER_RESET) {
                        AdFrequencyStore.resetRankingCount(context)
                    }

                    if (!shouldShowAd) {
                        isBackProcessing = false
                        onBack()
                        return@launch
                    }

                    // ✅ NEW: show interstitial + loading nếu chưa load kịp
                    adManager.showOrQueue(
                        activity = act,
                        timeoutMs = 12_000L,
                        onState = { state ->
                            showLoading = (state == InterstitialAdManager.State.Loading)
                        },
                        onClosedOrFailed = {
                            showLoading = false
                            isBackProcessing = false
                            // preload lại cho lần sau (optional vì manager cũng có thể preload)
                            scope.launch { adManager.preload(context) }
                            onBack()
                        },
                        onLoadFailedOrTimeout = {
                            // fallback để không kẹt UX
                            showLoading = false
                            isBackProcessing = false
                            scope.launch { adManager.preload(context) }
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BackButton(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(120.dp) // hitbox lớn
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ranking_btn_01),
            contentDescription = "Back",
            modifier = Modifier.size(64.dp)
        )
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
