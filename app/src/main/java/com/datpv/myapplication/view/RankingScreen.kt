package com.datpv.myapplication.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.datpv.myapplication.R


@Composable
fun RankingScreen(
    onBack: () -> Unit,
    viewModel: RankingViewModel = viewModel() ,
) {
    val top5 by viewModel.topScores.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight

        // ✅ các “tỉ lệ” để canh nội dung vào khung trắng phía trên (ăn theo mọi màn hình)
        val contentLeft = w * 0.14f
        val contentTop = h * 0.20f       // bắt đầu dưới chữ Ranking
        val contentWidth = w * 0.72f
        val contentHeight = h * 0.34f    // nằm gọn trong khung trắng trên

        // Background
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

        // ✅ LIST nằm đúng “khung trắng trên”
        Box(
            modifier = Modifier
                .offset(x = contentLeft, y = contentTop)
                .size(contentWidth, contentHeight)
                .padding(horizontal = 10.dp, vertical = 100.dp)
        ) {
            if (top5.isEmpty()) {
                Text(
                    text = "No scores yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6D4C41)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(top5) { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}:",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF8D2C1F), // đỏ nâu giống hình
                                modifier = Modifier.width(44.dp)
                            )
                            Text(
                                text = item.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF1B2A8A) // xanh giống hình
                            )
                        }
                    }
                }
            }
        }

        // ✅ Back button: góc trên phải
        Image(
            painter = painterResource(id = R.drawable.ranking_btn_01),
            contentDescription = "Back",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .size(64.dp)
                .zIndex(999f)
                .clickable { onBack() }
        )

    }
}
