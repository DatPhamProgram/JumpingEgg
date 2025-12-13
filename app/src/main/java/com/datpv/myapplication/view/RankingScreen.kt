package com.datpv.myapplication.view

import android.R.attr.end
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
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow


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
        val contentTop = h * 0.05f       // bắt đầu dưới chữ Ranking
        val contentWidth = w * 0.72f
        val contentHeight = h * 0.48f    // nằm gọn trong khung trắng trên

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
        // ✅ LIST nằm đúng “khung trắng trên”
        Box(
            modifier = Modifier
                .offset(x = contentLeft, y = h * 0.26f)   // 🔥 đẩy xuống thêm (0.24f - 0.30f tuỳ máy)
                .size(contentWidth, h * 0.40f)            // 🔥 chiều cao hợp lý cho 5 dòng
                .padding(horizontal = 10.dp)
                .clipToBounds()                           // 🔥 QUAN TRỌNG: chặn vẽ tràn ra ngoài
        ) {
            // ✅ luôn đủ 5 rows giống hình
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
                        Spacer(modifier = Modifier.width(12.dp))   // khoảng cách giữa col1-col2
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
                        Spacer(modifier = Modifier.width(12.dp))   // khoảng cách giữa col2-col3
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
