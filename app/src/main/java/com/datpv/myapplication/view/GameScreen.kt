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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

private data class Basket(
    val x: Float,        // world X (dp)
    val y: Float,        // world Y (dp)
    val dir: Float,      // +1 / -1
    val speed: Float     // dp per frame
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GameScreen(
    onGameOver: (finalScore: Int) -> Unit
) {

    // ===== SIZES =====
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
    val aimAssist = 0.08f
    val assistOnlyWhenNear = 140f

    // mouth hitbox (balanced)
    val mouthOffsetY = 14f
    val mouthHeight = 34f
    val mouthPaddingX = 14f

    // UI
    val bannerHeight = 72f
    val bottomSafePadding = 18f
    val basketBottomGap = 10f

    val scope = rememberCoroutineScope()

    // ✅ CameraY: scroll thật
    val cameraY = remember { Animatable(0f) }
    var isScrolling by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val w = maxWidth.value
        val h = maxHeight.value

        val leftLimit = 0f
        val rightLimit = w - basketW

        // base y theo SCREEN (dp)
        val baseY1 = h * 0.66f
        val baseY2 = h * 0.44f
        val baseY3 = h * 0.22f
        val stepY = (baseY1 - baseY2).coerceAtLeast(140f)

        // ✅ luôn 4 rỗ
        val platformCount = 4

        // ===== WORLD baskets (start 4) =====
        val baskets = remember {
            mutableStateListOf(
                Basket(x = w * 0.08f, y = baseY1, dir = 1f,  speed = 3.4f),
                Basket(x = w * 0.58f, y = baseY2, dir = -1f, speed = 3.8f),
                Basket(x = w * 0.18f, y = baseY3, dir = 1f,  speed = 4.2f),
                Basket(x = w * 0.70f, y = baseY3 - stepY, dir = -1f, speed = 4.0f),
            )
        }

        // ===== STATES =====
        var isEggFlying by remember { mutableStateOf(false) }
        var basketWithEgg by remember { mutableStateOf(-1) } // index in baskets, -1 = outside

        // ✅ tách 2 score:
        // roundStep: tiến độ trong vòng hiện tại (0..platformCount). Dùng để target rỗ
        var roundStep by remember { mutableStateOf(0) }
        // totalScore: hiển thị banner, cộng dồn, KHÔNG reset khi scroll
        var totalScore by remember { mutableStateOf(0) }

        // ✅ Egg dùng WORLD Y để camera kéo cùng nhau
        var eggX by remember { mutableStateOf(w * 0.5f - eggW / 2f) }
        var eggY by remember { mutableStateOf(h - bannerHeight - eggH - bottomSafePadding) }
        var velY by remember { mutableStateOf(0f) }
        var prevEggBottomY by remember { mutableStateOf(eggY + eggH) }

        // Game over
        var gameOver by remember { mutableStateOf(false) }
        var finalScore by remember { mutableStateOf(0) }
        var hasNavigatedEndGame by remember { mutableStateOf(false) }

        // ===== helpers =====
        fun orderBottomToTop(): List<Int> =
            baskets.indices.sortedByDescending { baskets[it].y } // world y lớn = thấp

        fun desiredBottomScreenY(): Float =
            h - bannerHeight - basketH - basketBottomGap

        fun computeJumpVelocityTo(targetWorldY: Float): Float {
            val desiredPeak = targetWorldY - 80f
            val dy = max(0f, eggY - desiredPeak)
            val v = -sqrt(2f * gravity * dy)
            return v.coerceIn(-36f, -18f)
        }

        fun resetGame() {
            gameOver = false
            finalScore = 0
            isScrolling = false
            scope.launch { cameraY.snapTo(0f) }

            roundStep = 0
            totalScore = 0

            isEggFlying = false
            basketWithEgg = -1
            velY = 0f
            hasNavigatedEndGame = false


            baskets.clear()
            baskets.addAll(
                listOf(
                    Basket(x = w * 0.08f, y = baseY1, dir = 1f,  speed = 3.4f),
                    Basket(x = w * 0.58f, y = baseY2, dir = -1f, speed = 3.8f),
                    Basket(x = w * 0.18f, y = baseY3, dir = 1f,  speed = 4.2f),
                    Basket(x = w * 0.70f, y = baseY3 - stepY, dir = -1f, speed = 4.0f),
                )
            )

            eggX = w * 0.5f - eggW / 2f
            eggY = h - bannerHeight - eggH - bottomSafePadding
            prevEggBottomY = eggY + eggH
        }

        fun spawnOneBasketAboveTop() {
            val topY = baskets.minOf { it.y }
            val newY = topY - stepY

            val x0 = (w * 0.10f).coerceIn(leftLimit, rightLimit)
            val dir = if ((0..1).random() == 0) -1f else 1f
            val speed = listOf(3.5f, 3.8f, 4.1f).random()

            baskets.add(Basket(x = x0, y = newY, dir = dir, speed = speed))
        }

        /**
         * ✅ Scroll bền vững (không kẹt vòng 3+):
         * - Camera kéo xuống để rỗ đang chứa trứng (trên cùng) về vị trí bottom (ngay trên banner)
         * - Sau khi animate xong: SNAP lại 4 rỗ về đúng 4 tầng chuẩn (bottom -> top)
         * - Giữ 4 rỗ mãi mãi, vòng sau chơi dễ như vòng đầu
         * - totalScore KHÔNG reset
         */
        fun startScrollDownAnimation() {
            if (isScrolling) return
            isScrolling = true

            // tạo cảm giác có rỗ sẵn ở trên
            spawnOneBasketAboveTop()

            scope.launch {
                try {
                    val eggIdx = basketWithEgg
                    if (eggIdx < 0 || eggIdx > baskets.lastIndex) return@launch

                    // kéo camera để rỗ đang chứa trứng về đúng bottomScreenY
                    val eggBasketWorldY = baskets[eggIdx].y
                    val targetCamera = eggBasketWorldY - desiredBottomScreenY()

                    cameraY.animateTo(
                        targetValue = targetCamera,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    )

                    // đảm bảo có đủ candidate
                    while (baskets.size < platformCount) {
                        spawnOneBasketAboveTop()
                    }

                    // bottomWorldY tương ứng vị trí bottom trên màn
                    val bottomWorldY = cameraY.value + desiredBottomScreenY()

                    // chọn 4 rỗ gần khu vực chơi nhất (gần bottomWorldY)
                    val chosenIndices = baskets
                        .mapIndexed { idx, b -> idx to abs(b.y - bottomWorldY) }
                        .sortedBy { it.second }
                        .take(platformCount)
                        .map { it.first }

                    val chosen = chosenIndices.map { baskets[it] }
                        .sortedByDescending { it.y } // bottom -> top

                    // SNAP lại đúng 4 tầng
                    val snapped = chosen.mapIndexed { i, b ->
                        b.copy(y = bottomWorldY - (i * stepY))
                    }

                    baskets.clear()
                    baskets.addAll(snapped)

                    // đặt trứng vào rỗ dưới cùng
                    val bottomIdx = baskets.indices.maxBy { baskets[it].y }
                    basketWithEgg = bottomIdx

                    // vòng mới: đã đứng ở rỗ dưới => roundStep=1
                    roundStep = 1
                    isEggFlying = false
                    velY = 0f
                } finally {
                    isScrolling = false
                }
            }
        }

        // ===== GAME LOOP =====
        LaunchedEffect(Unit) {
            while (true) {

                // move baskets horizontally (world)
                for (i in baskets.indices) {
                    val b = baskets[i]
                    var nx = b.x + b.dir * b.speed
                    var nd = b.dir

                    if (nx <= leftLimit) { nx = leftLimit; nd = 1f }
                    if (nx >= rightLimit) { nx = rightLimit; nd = -1f }

                    baskets[i] = b.copy(x = nx, dir = nd)
                }

                if (!gameOver) {
                    if (basketWithEgg >= 0) {
                        val b = baskets[basketWithEgg]
                        eggX = b.x + (growW * 0.5f - eggW * 0.5f)
                        eggY = b.y + (growH * 0.35f)
                        velY = 0f
                        isEggFlying = false
                        prevEggBottomY = eggY + eggH
                    } else if (isEggFlying) {

                        val order = orderBottomToTop()

                        // target theo roundStep (0..3)
                        val step = min(roundStep, platformCount - 1)
                        val targetIndex = order[min(step, order.lastIndex)]
                        val t = baskets[targetIndex]

                        // aim assist theo X (WORLD)
                        val targetCenterX = t.x + basketW / 2f
                        val eggCenterX = eggX + eggW / 2f

                        // assist theo Y (SCREEN)
                        val mouthTopScreen = (t.y - cameraY.value) + mouthOffsetY
                        val eggBottomScreenBefore = (eggY - cameraY.value) + eggH
                        val nearInY = (mouthTopScreen - eggBottomScreenBefore) in 0f..assistOnlyWhenNear

                        val assistFactor = if (velY > 0f && nearInY) aimAssist else 0f
                        val newCenterX = eggCenterX + (targetCenterX - eggCenterX) * assistFactor
                        eggX = newCenterX - eggW / 2f

                        // gravity (WORLD)
                        prevEggBottomY = eggY + eggH
                        velY += gravity
                        eggY += velY

                        // mouth hitbox (WORLD)
                        val mouthLeft = t.x + mouthPaddingX
                        val mouthRight = t.x + basketW - mouthPaddingX
                        val mouthTopWorld = t.y + mouthOffsetY
                        val mouthBottomWorld = mouthTopWorld + mouthHeight

                        val falling = velY > 0f
                        val inX = (newCenterX in mouthLeft..mouthRight)

                        val eggBottomY = eggY + eggH
                        val crossedMouth =
                            (prevEggBottomY <= mouthTopWorld && eggBottomY >= mouthTopWorld) ||
                                    (eggBottomY in mouthTopWorld..mouthBottomWorld)

                        if (falling && inX && crossedMouth) {
                            basketWithEgg = targetIndex
                            isEggFlying = false
                            velY = 0f

                            // update round + total
                            roundStep = min(platformCount, step + 1)
                            totalScore += 1

                            // bắt rỗ top => scroll
                            if (roundStep == platformCount) {
                                startScrollDownAnimation()
                            }
                        }

                        // miss => game over (SCREEN bottom)
                        val bottomLimitScreen = h - bannerHeight - 6f
                        val eggScreenY = eggY - cameraY.value
                        if (eggScreenY > bottomLimitScreen) {
                            finalScore = totalScore
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

            if (basketWithEgg >= 0) {
                val pos = order.indexOf(basketWithEgg)
                val nextPos = min(platformCount - 1, pos + 1)
                val nextIndex = order[nextPos]

                val b = baskets[basketWithEgg]
                basketWithEgg = -1 // rỗ cũ về empty

                eggX = b.x + basketW / 2f - eggW / 2f
                eggY = b.y - eggH * 0.3f

                velY = computeJumpVelocityTo(baskets[nextIndex].y)
                isEggFlying = true
                return
            }

            // từ dưới: nhảy lên rỗ thấp nhất
            val bottomIndex = order[0]
            velY = computeJumpVelocityTo(baskets[bottomIndex].y)
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

            // Draw baskets (SCREEN = world - camera)
            baskets.forEachIndexed { idx, b ->
                val screenY = (b.y - cameraY.value).dp
                if (basketWithEgg == idx) {
                    Image(
                        painter = painterResource(id = R.drawable.grow_purple_egg),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = b.x.dp, y = screenY)
                            .size(growBasketSize)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.basket01),
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = b.x.dp, y = screenY)
                            .size(basketSize)
                    )
                }
            }

            // Egg (only when outside)
            if (basketWithEgg < 0) {
                Image(
                    painter = painterResource(id = R.drawable.egg_purple),
                    contentDescription = null,
                    modifier = Modifier
                        .offset(x = eggX.dp, y = (eggY - cameraY.value).dp)
                        .size(eggSize)
                )
            }

           if (gameOver) {
               finalScore = totalScore
               gameOver = true
               isEggFlying = false
               velY = 0f
               if (!hasNavigatedEndGame) {
                   hasNavigatedEndGame = true
                   onGameOver(totalScore)
               }
           }

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
                    text = totalScore.toString(),
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
