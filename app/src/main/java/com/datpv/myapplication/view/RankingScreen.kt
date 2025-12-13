package com.datpv.myapplication.view

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
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

    // ✅ Interstitial Unit ID
    val interstitialUnitId = stringResource(R.string.admob_interstitial_unit_id)

    // ✅ Interstitial manager
    val adManager = remember(interstitialUnitId) {
        InterstitialAdManager(interstitialUnitId)
    }

    val scope = rememberCoroutineScope()

    // ✅ preload sẵn để khi bấm Back dễ show hơn
    LaunchedEffect(Unit) {
        adManager.preload(context)
    }

    // ✅ chặn double click Back
    var isBackProcessing by remember { mutableStateOf(false) }

    // ================= UI =================

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight

        val contentLeft = w * 0.14f
        val contentTop = h * 0.05f
        val contentWidth = w * 0.72f
        val contentHeight = h * 0.48f

        Image(
            painter = painterResource(id = R.drawable.ranking_background),
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

        // ✅ GIỮ NGUYÊN bảng điểm 3 cột (KHÔNG ĐỔI)
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

        // ✅ FIX: Back image không click được
        // - Tăng hitbox
        // - Đặt zIndex lớn
        // - Click trên Box (không click trực tiếp Image)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .size(120.dp)
                .zIndex(9999f)
                .clickable(enabled = !isBackProcessing) {

                    if (isBackProcessing) return@clickable
                    isBackProcessing = true

                    val act = activity
                    if (act == null) {
                        isBackProcessing = false
                        onBack()
                        return@clickable
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

                        adManager.show(
                            activity = act,
                            onClosedOrFailed = {
                                scope.launch { adManager.preload(context) }
                                isBackProcessing = false
                                onBack()
                            }
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ranking_btn_01),
                contentDescription = "Back",
                modifier = Modifier.size(64.dp)
            )
        }
    }
}
