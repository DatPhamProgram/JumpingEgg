package com.datpv.myapplication.view

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import kotlinx.coroutines.launch
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

    // ===== Difficulty (balanced) =====
    // Không dễ quá (nhưng không khó như bug sau scroll)
    val aimAssist = 0.08f
    val assistOnlyWhenNear = 140f

    // Hitbox miệng rổ (balanced)
    val mouthOffsetY = 14f
    val mouthHeight = 34f
    val mouthPaddingX = 14f

    // UI
    val bannerHeight = 72f
    val bottomSafePadding = 18f
    val basketBottomGap = 10f

    val scope = rememberCoroutineScope()
    val scrollOffset = remember { Animatable(0f) }
    var isScrolling by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value

        val leftLimit = 0f
        val rightLimit = w - basketW

        // ===== 3 baskets base positions =====
        val baseY1 = h * 0.66f
        val baseY2 = h * 0.44f
        val baseY3 = h * 0.22f
        val stepY = (baseY1 - baseY2).coerceAtLeast(140f)

        // ✅ Lần đầu 3 rổ, sau scroll đầu tiên -> 4 rổ và giữ 4 mãi
        var platformCount by remember { mutableStateOf(3) }

        // ===== baskets state =====
        val baskets = remember {
            mutableStateListOf(
                Basket(xAbs = w * 0.08f, yAbs = baseY1, dir = 1f,  speed = 3.4f),
                Basket(xAbs = w * 0.58f, yAbs = baseY2, dir = -1f, speed = 3.8f),
                Basket(xAbs = w * 0.18f, yAbs = baseY3, dir = 1f,  speed = 4.2f),
            )
        }

        // ===== STATES =====
        var isEggFlying by remember { mutableStateOf(false) }
        var basketWithEgg by remember { mutableStateOf(-1) } // -1 = outside
        var score by remember { mutableStateOf(0) }          // 0..platformCount

        var eggX by remember { mutableStateOf(w * 0.5f - eggW / 2f) } // top-left
        var eggY by remember { mutableStateOf(h - bannerHeight - eggH - bottomSafePadding) }
        var velY by remember { mutableStateOf(0f) }
        var prevEggBottomY by remember { mutableStateOf(eggY + eggH) }

        // Game over
        var gameOver by remember { mutableStateOf(false) }
        var finalScore by remember { mutableStateOf(0) }

        // ===== helpers =====
        fun orderBottomToTop(): List<Int> =
            baskets.indices.sortedByDescending { baskets[it].yAbs } // y lớn = thấp

        fun resetEggBottom() {
            isEggFlying = false
            basketWithEgg = -1
            velY = 0f
            eggX = w * 0.5f - eggW / 2f
            eggY = h - bannerHeight - eggH - bottomSafePadding
            prevEggBottomY = eggY + eggH
        }

        fun resetGame() {
            gameOver = false
            finalScore = 0
            isScrolling = false
            scope.launch { scrollOffset.snapTo(0f) }

            score = 0
            platformCount = 3
            resetEggBottom()

            baskets.clear()
            baskets.addAll(
                listOf(
                    Basket(xAbs = w * 0.08f, yAbs = baseY1, dir = 1f,  speed = 3.4f),
                    Basket(xAbs = w * 0.58f, yAbs = baseY2, dir = -1f, speed = 3.8f),
                    Basket(xAbs = w * 0.18f, yAbs = baseY3, dir = 1f,  speed = 4.2f),
                )
            )
        }

        fun computeJumpVelocityTo(targetY: Float): Float {
            val desiredPeak = targetY - 80f
            val dy = max(0f, eggY - desiredPeak)
            val v = -sqrt(2f * gravity * dy)
            return v.coerceIn(-36f, -18f)
        }

        fun spawnOneBasketAboveTop() {
            val topY = baskets.minOf { it.yAbs }
            val newY = topY - stepY

            val x0 = (w * 0.10f).coerceIn(leftLimit, rightLimit)
            val dir = if ((0..1).random() == 0) -1f else 1f
            val speed = listOf(3.5f, 3.8f, 4.1f).random()

            baskets.add(Basket(xAbs = x0, yAbs = newY, dir = dir, speed = speed))
        }

        /**
         * ✅ Scroll FIX:
         * - Tính scrollDistance để "top basket" đi xuống tận vị trí bottom (ngay trên banner)
         * - Animate đúng scrollDistance -> cảm giác kéo xuống liên tục, không giật reset
         * - Commit yAbs += scrollDistance, sau đó scrollOffset=0
         * - Sau scroll: giữ đúng platformCount rổ và đặt trứng vào rổ dưới cùng
         */
        fun startScrollDownAnimation() {
            if (isScrolling) return
            isScrolling = true

            // lần đầu: 3 -> 4, các lần sau giữ 4
            if (platformCount == 3) platformCount = 4

            // mỗi lần scroll mọc thêm 1 rổ ở top
            spawnOneBasketAboveTop()

            scope.launch {
                // 1) tìm top basket hiện tại
                val topIndex = baskets.indices.minBy { baskets[it].yAbs }
                val topY = baskets[topIndex].yAbs

                // 2) đích bottom
                val desiredBottomY = h - bannerHeight - basketH - basketBottomGap

                // 3) scrollDistance để top đi xuống tận bottom (mượt)
                val scrollDistance = (desiredBottomY - topY).coerceAtLeast(stepY)

                scrollOffset.snapTo(0f)
                scrollOffset.animateTo(
                    targetValue = scrollDistance,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )

                // 4) commit yAbs (tránh giật)
                val committed = baskets.map { it.copy(yAbs = it.yAbs + scrollDistance) }
                baskets.clear()
                baskets.addAll(committed)

                // 5) giữ đúng platformCount rổ: giữ rổ trong vùng nhìn (bottom->top)
                baskets.sortByDescending { it.yAbs }
                while (baskets.size > platformCount) {
                    baskets.removeLast() // bỏ rổ thấp nhất / ngoài vùng
                }

                // 6) clamp để rổ dưới cùng không đè banner
                val bottomY = baskets.maxOf { it.yAbs }
                val shift = desiredBottomY - bottomY
                if (shift != 0f) {
                    val shifted = baskets.map { it.copy(yAbs = it.yAbs + shift) }
                    baskets.clear()
                    baskets.addAll(shifted)
                }

                // ✅ sau scroll: trứng nằm trong rổ dưới cùng, bắt đầu vòng mới dễ như vòng 1
                val bottomIndex = baskets.indices.maxBy { baskets[it].yAbs }
                basketWithEgg = bottomIndex
                score = 1
                isEggFlying = false
                velY = 0f

                scrollOffset.snapTo(0f)
                isScrolling = false
            }
        }

        // ===== GAME LOOP =====
        LaunchedEffect(Unit) {
            while (true) {
                // 1) move baskets (đến sát mép)
                for (i in baskets.indices) {
                    val b = baskets[i]
                    var nx = b.xAbs + b.dir * b.speed
                    var nd = b.dir

                    if (nx <= leftLimit) { nx = leftLimit; nd = 1f }
                    if (nx >= rightLimit) { nx = rightLimit; nd = -1f }

                    baskets[i] = b.copy(xAbs = nx, dir = nd)
                }

                // 2) egg update
                if (!gameOver) {
                    if (basketWithEgg >= 0) {
                        val b = baskets[basketWithEgg]
                        eggX = b.xAbs + (growW * 0.5f - eggW * 0.5f)
                        eggY = b.yAbs + (growH * 0.35f)
                        velY = 0f
                        isEggFlying = false
                        prevEggBottomY = eggY + eggH
                    } else if (isEggFlying) {
                        val order = orderBottomToTop()
                        val step = min(score, platformCount - 1)
                        val targetIndex = order[step]
                        val t = baskets[targetIndex]

                        // aim assist nhẹ
                        val targetCenterX = t.xAbs + basketW / 2f
                        val eggCenterX = eggX + eggW / 2f

                        val mouthTopAssist = t.yAbs + mouthOffsetY
                        val eggBottomBefore = eggY + eggH
                        val nearInY = (mouthTopAssist - eggBottomBefore) in 0f..assistOnlyWhenNear

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

                        val crossedMouth =
                            (prevEggBottomY <= mouthTop && eggBottomY >= mouthTop) ||
                                    (eggBottomY in mouthTop..mouthBottom)

                        if (falling && inX && crossedMouth) {
                            basketWithEgg = targetIndex
                            score = min(platformCount, step + 1)
                            isEggFlying = false
                            velY = 0f

                            // ✅ bắt rổ trên cùng (step == platformCount-1) -> scroll
                            if (score == platformCount) {
                                startScrollDownAnimation()
                            }
                        }

                        // miss => game over
                        val bottomLimit = h - bannerHeight - 6f
                        if (eggY > bottomLimit) {
                            finalScore = score
                            gameOver = true
                            isEggFlying = false
                            velY = 0f
                        }
                    } else {
                        velY = 0f
                        prevEggBottomY = eggY + eggH
                    }
                }

                delay(16L)
            }
        }

        fun jump() {
            if (gameOver) return
            if (isScrolling) return
            if (isEggFlying) return

            val order = orderBottomToTop()

            // Nếu đang nằm trong rổ: tap để nhảy lên rổ kế tiếp theo "tầng"
            if (basketWithEgg >= 0) {
                val pos = order.indexOf(basketWithEgg)
                val nextPos = min(platformCount - 1, pos + 1)
                val nextIndex = order[nextPos]

                val b = baskets[basketWithEgg]
                basketWithEgg = -1 // rổ cũ trở lại empty ngay

                eggX = b.xAbs + basketW / 2f - eggW / 2f
                eggY = b.yAbs - eggH * 0.3f

                velY = computeJumpVelocityTo(baskets[nextIndex].yAbs)
                isEggFlying = true
                return
            }

            // Ở dưới cùng: nhảy lên rổ thấp nhất (bottom)
            val bottomIndex = order[0]
            velY = computeJumpVelocityTo(baskets[bottomIndex].yAbs)
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

            val yAnim = scrollOffset.value

            // Baskets
            baskets.forEachIndexed { idx, b ->
                val drawY = (b.yAbs + yAnim).dp
                if (basketWithEgg == idx) {
                    Image(
                        painter = painterResource(id = R.drawable.grow_purple_egg),
                        contentDescription = "Egg In Basket",
                        modifier = Modifier
                            .offset(x = b.xAbs.dp, y = drawY)
                            .size(growBasketSize)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.basket01),
                        contentDescription = "Basket",
                        modifier = Modifier
                            .offset(x = b.xAbs.dp, y = drawY)
                            .size(basketSize)
                    )
                }
            }

            // Egg (only when outside baskets)
            if (basketWithEgg < 0) {
                Image(
                    painter = painterResource(id = R.drawable.egg_purple),
                    contentDescription = "Egg",
                    modifier = Modifier
                        .offset(x = eggX.dp, y = (eggY + yAnim).dp)
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

            // Game Over dialog
            if (gameOver) {
                AlertDialog(
                    onDismissRequest = { /* locked */ },
                    title = { Text("Game Over") },
                    text = { Text("Your score: $finalScore") },
                    confirmButton = {
                        TextButton(onClick = { resetGame() }) {
                            Text("Play Again")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onBack) {
                            Text("Back")
                        }
                    }
                )
            }

            // Score banner: chỉ số đỏ
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
