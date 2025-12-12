package com.datpv.myapplication.view

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.datpv.myapplication.R
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private data class Basket(
    val xAbs: Float,     // top-left X (dp)
    val yAbs: Float,     // top-left Y (dp)
    val dir: Float,      // +1 / -1
    val speed: Float     // dp per frame
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GameScreen(onBack: () -> Unit) {

    // ===== SIZES (to hơn) =====
    val basketSize = 92.dp
    val growBasketSize = 108.dp
    val eggSize = 56.dp

    val basketW = 92f
    val basketH = 92f
    val growW = 108f
    val growH = 108f
    val eggW = 56f
    val eggH = 56f

    // ===== PHYSICS =====
    val gravity = 1.2f

    // ===== EASY MODE SETTINGS =====
    val aimAssist = 0.05f
    val assistOnlyWhenNear = 10f

    val mouthOffsetY = 1f
    val mouthHeight = 1f
    val mouthPaddingX = 1f

    // UI
    val bannerHeight = 72f
    val bottomSafePadding = 18f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value

        val leftLimit = 0f
        val rightLimit = w - basketW

        // ===== 3 baskets (đẩy lên để thấy banner) =====
        val baskets = remember {
            val y1 = h * 0.66f
            val y2 = h * 0.44f
            val y3 = h * 0.22f
            mutableStateListOf(
                Basket(xAbs = w * 0.08f, yAbs = y1, dir = 1f,  speed = 3.4f),
                Basket(xAbs = w * 0.58f, yAbs = y2, dir = -1f, speed = 3.8f),
                Basket(xAbs = w * 0.18f, yAbs = y3, dir = 1f,  speed = 4.2f),
            )
        }

        // ===== STATES =====
        var isEggFlying by remember { mutableStateOf(false) }
        var basketWithEgg by remember { mutableStateOf(-1) } // -1 = outside
        var score by remember { mutableStateOf(0) }          // 0..3 (0 = chưa bắt cái nào)

        var eggX by remember { mutableStateOf(w * 0.5f - eggW / 2f) } // top-left
        var eggY by remember { mutableStateOf(h - bannerHeight - eggH - bottomSafePadding) }
        var velY by remember { mutableStateOf(0f) }

        // để bắt theo “crossing” (rơi qua vùng miệng rổ)
        var prevEggBottomY by remember { mutableStateOf(eggY + eggH) }

        fun resetGame() {
            isEggFlying = false
            basketWithEgg = -1
            score = 0
            velY = 0f
            eggX = w * 0.5f - eggW / 2f
            eggY = h - bannerHeight - eggH - bottomSafePadding
            prevEggBottomY = eggY + eggH
        }

        // jump velocity để lên đến target
        fun computeJumpVelocityTo(targetY: Float): Float {
            // muốn peak cao hơn miệng rổ một chút
            val desiredPeak = targetY - 80f
            val dy = max(0f, eggY - desiredPeak)
            val v = -sqrt(2f * gravity * dy)
            return v.coerceIn(-42f, -18f) // cho lên cao hơn để bắt rổ 2/3 chắc chắn
        }

        LaunchedEffect(Unit) {
            while (true) {
                // 1) move baskets
                for (i in baskets.indices) {
                    val b = baskets[i]
                    var nx = b.xAbs + b.dir * b.speed
                    var nd = b.dir

                    if (nx <= leftLimit) { nx = leftLimit; nd = 1f }
                    if (nx >= rightLimit) { nx = rightLimit; nd = -1f }

                    baskets[i] = b.copy(xAbs = nx, dir = nd)
                }

                // 2) egg update
                if (basketWithEgg >= 0) {
                    val b = baskets[basketWithEgg]
                    // egg nằm trong rổ -> đi theo rổ đó (vẽ bằng grow image)
                    eggX = b.xAbs + (growW * 0.5f - eggW * 0.5f)
                    eggY = b.yAbs + (growH * 0.35f)
                    velY = 0f
                    isEggFlying = false
                    prevEggBottomY = eggY + eggH
                } else {
                    if (isEggFlying) {
                        // target basket index: score=0 -> target0, score=1 -> target1, score=2 -> target2
                        val targetIndex = min(score, 2)
                        val t = baskets[targetIndex]

                        // ===== AIM ASSIST: kéo eggX về tâm rổ target =====
                        val targetCenterX = t.xAbs + basketW / 2f
                        val eggCenterX = eggX + eggW / 2f

                        // chỉ assist khi đang rơi và gần rổ theo trục Y
                        val mouthTop1 = t.yAbs + mouthOffsetY
                        val eggBottomYBefore = eggY + eggH
                        val nearInY = (mouthTop1 - eggBottomYBefore) in 0f..assistOnlyWhenNear

                        val assistFactor = if (velY > 0f && nearInY) aimAssist else 0f
                        val newCenterX = eggCenterX + (targetCenterX - eggCenterX) * assistFactor

                        eggX = newCenterX - eggW / 2f

                        // gravity
                        prevEggBottomY = eggY + eggH
                        velY += gravity
                        eggY += velY
                        val eggBottomY = eggY + eggH

                        // hitbox mouth
                        val mouthLeft = t.xAbs + mouthPaddingX
                        val mouthRight = t.xAbs + basketW - mouthPaddingX
                        val mouthTop = t.yAbs + mouthOffsetY
                        val mouthBottom = mouthTop + mouthHeight

                        val falling = velY > 0f
                        val inX = (newCenterX in mouthLeft..mouthRight)

                        // ===== CROSSING CHECK: nếu trứng đi từ trên xuống qua vùng mouth => catch =====
                        val crossedMouth =
                            (prevEggBottomY <= mouthTop && eggBottomY >= mouthTop) ||
                                    (eggBottomY in mouthTop..mouthBottom)

                        if (falling && inX && crossedMouth) {
                            basketWithEgg = targetIndex
                            score = min(3, targetIndex + 1) // update số đỏ dưới banner
                            isEggFlying = false
                            velY = 0f
                        }

                        // miss -> reset
                        val bottomLimit = h - bannerHeight - 6f
                        if (eggY > bottomLimit) resetGame()
                    } else {
                        velY = 0f
                        prevEggBottomY = eggY + eggH
                    }
                }

                delay(16L)
            }
        }

        fun jump() {
            if (isEggFlying) return

            // Nếu đang nằm trong rổ nào đó: tap để nhảy lên rổ kế tiếp
            if (basketWithEgg >= 0) {
                val from = basketWithEgg
                basketWithEgg = -1 // rổ cũ trở lại empty ngay
                val next = min(2, from + 1)
                val target = baskets[next]

                // đặt trứng xuất phát từ giữa rổ hiện tại
                val b = baskets[from]
                eggX = b.xAbs + basketW / 2f - eggW / 2f
                eggY = b.yAbs - eggH * 0.3f

                velY = computeJumpVelocityTo(target.yAbs)
                isEggFlying = true
                return
            }

            // nếu ở dưới cùng: nhảy lên rổ 0
            velY = computeJumpVelocityTo(baskets[0].yAbs)
            isEggFlying = true
        }

        // ===== UI =====
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { jump() }
        ) {
            Image(
                painter = painterResource(id = R.drawable.background),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            baskets.forEachIndexed { idx, b ->
                if (basketWithEgg == idx) {
                    Image(
                        painter = painterResource(id = R.drawable.grow_purple_egg),
                        contentDescription = "Egg In Basket",
                        modifier = Modifier
                            .offset(x = b.xAbs.dp, y = b.yAbs.dp)
                            .size(growBasketSize)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.basket01),
                        contentDescription = "Basket",
                        modifier = Modifier
                            .offset(x = b.xAbs.dp, y = b.yAbs.dp)
                            .size(basketSize)
                    )
                }
            }

            if (basketWithEgg < 0) {
                Image(
                    painter = painterResource(id = R.drawable.egg_purple),
                    contentDescription = "Egg",
                    modifier = Modifier
                        .offset(x = eggX.dp, y = eggY.dp)
                        .size(eggSize)
                )
            }

            Button(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("Back", fontSize = 14.sp)
            }

            // Banner score: chỉ số đỏ
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(72.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.bannerscore),
                    contentDescription = "Score Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Text(
                    text = score.toString(),
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
