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
    val x: Float,        // world X (dp)
    val y: Float,        // world Y (dp)
    val dir: Float,      // +1 / -1
    val speed: Float     // dp per frame
)

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun GameScreen(onBack: () -> Unit) {

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

    // ✅ CameraY: scroll thật (không reset)
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

        // ✅ lần đầu 3 rỗ, sau đó thành 4 và giữ 4
        var platformCount by remember { mutableStateOf(3) }

        // ===== WORLD baskets =====
        val baskets = remember {
            mutableStateListOf(
                Basket(x = w * 0.08f, y = baseY1, dir = 1f,  speed = 3.4f),
                Basket(x = w * 0.58f, y = baseY2, dir = -1f, speed = 3.8f),
                Basket(x = w * 0.18f, y = baseY3, dir = 1f,  speed = 4.2f),
            )
        }

        // ===== STATES =====
        var isEggFlying by remember { mutableStateOf(false) }
        var basketWithEgg by remember { mutableStateOf(-1) } // index in baskets, -1 = outside
        var score by remember { mutableStateOf(0) }          // dùng cho banner + target step

        // ✅ Egg dùng WORLD Y để camera kéo cùng nhau
        var eggX by remember { mutableStateOf(w * 0.5f - eggW / 2f) } // world x
        var eggY by remember { mutableStateOf(h - bannerHeight - eggH - bottomSafePadding) } // world y
        var velY by remember { mutableStateOf(0f) }
        var prevEggBottomY by remember { mutableStateOf(eggY + eggH) }

        // Game over
        var gameOver by remember { mutableStateOf(false) }
        var finalScore by remember { mutableStateOf(0) }

        // ===== helpers =====
        fun orderBottomToTop(): List<Int> =
            baskets.indices.sortedByDescending { baskets[it].y } // world y lớn = thấp

        fun desiredBottomScreenY(): Float =
            h - bannerHeight - basketH - basketBottomGap

        fun resetEggBottom() {
            isEggFlying = false
            basketWithEgg = -1
            velY = 0f
            eggX = w * 0.5f - eggW / 2f
            // egg world y = cameraY + (screen bottom)
            eggY = cameraY.value + (h - bannerHeight - eggH - bottomSafePadding)
            prevEggBottomY = eggY + eggH
        }

        fun resetGame() {
            gameOver = false
            finalScore = 0
            isScrolling = false
            scope.launch { cameraY.snapTo(0f) }

            score = 0
            platformCount = 3
            isEggFlying = false
            basketWithEgg = -1
            velY = 0f

            baskets.clear()
            baskets.addAll(
                listOf(
                    Basket(x = w * 0.08f, y = baseY1, dir = 1f,  speed = 3.4f),
                    Basket(x = w * 0.58f, y = baseY2, dir = -1f, speed = 3.8f),
                    Basket(x = w * 0.18f, y = baseY3, dir = 1f,  speed = 4.2f),
                )
            )

            eggX = w * 0.5f - eggW / 2f
            eggY = h - bannerHeight - eggH - bottomSafePadding
            prevEggBottomY = eggY + eggH
        }

        fun computeJumpVelocityTo(targetWorldY: Float): Float {
            // peak hơi cao hơn
            val desiredPeak = targetWorldY - 80f
            val dy = max(0f, eggY - desiredPeak)
            val v = -sqrt(2f * gravity * dy)
            return v.coerceIn(-36f, -18f)
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
         * ✅ Camera scroll:
         * - Không reset baskets.
         * - Animate cameraY để basketWithEgg (top-most) đi xuống đúng vị trí bottom (ngay trên banner)
         * - Trong lúc scroll, vì baskets đã có cái nằm trên top (worldY nhỏ), nên sẽ “lộ ra” dần.
         * - Sau scroll xong: giữ đúng platformCount baskets visible (cắt bớt basket quá thấp).
         */
        fun startScrollDownAnimation() {
            if (isScrolling) return
            isScrolling = true

            // lần đầu: 3 -> 4
            if (platformCount == 3) platformCount = 4

            // ✅ đảm bảo có basket ở phía trên để lộ ra khi camera kéo
            while (baskets.size < platformCount) {
                spawnOneBasketAboveTop()
            }
            // mỗi vòng scroll: thêm 1 cái ở trên cho “cảm giác vô tận”
            spawnOneBasketAboveTop()

            scope.launch {
                // basket đang chứa trứng là top-most vừa bắt được
                val eggBasketIdx = basketWithEgg
                if (eggBasketIdx < 0) {
                    isScrolling = false
                    return@launch
                }

                val eggBasketWorldY = baskets[eggBasketIdx].y

                // ta muốn sau scroll: eggBasket nằm ở bottomScreenY
                val targetCamera = eggBasketWorldY - desiredBottomScreenY()

                cameraY.animateTo(
                    targetValue = targetCamera,
                    animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                )

                // Sau scroll: cắt bớt baskets quá thấp (screenY > bottom giới hạn)
                // screenY = worldY - cameraY
                val bottomLimitScreen = desiredBottomScreenY() + 30f
                val keep = baskets
                    .mapIndexed { idx, b -> idx to (b.y - cameraY.value) }
                    .filter { (_, screenY) -> screenY <= bottomLimitScreen }
                    .sortedBy { it.second } // top->bottom
                    .takeLast(platformCount) // giữ đúng N cái gần dưới (đủ chơi)

                val keepIndices = keep.map { it.first }.toSet()
                val newList = baskets.filterIndexed { idx, _ -> idx in keepIndices }

                baskets.clear()
                baskets.addAll(newList)

                // re-map basketWithEgg (vì list đã đổi)
                val bottomIdx = baskets.indices.maxBy { baskets[it].y } // world y lớn nhất
                basketWithEgg = bottomIdx

                // bắt đầu vòng mới dễ như vòng 1: egg đang ở rỗ dưới cùng
                score = 1
                isEggFlying = false
                velY = 0f

                isScrolling = false
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
                        val step = min(score, platformCount - 1)
                        val targetIndex = order[min(step, order.lastIndex)]
                        val t = baskets[targetIndex]

                        // aim assist nhẹ theo X (WORLD)
                        val targetCenterX = t.x + basketW / 2f
                        val eggCenterX = eggX + eggW / 2f

                        // chỉ assist khi gần theo Y (so sánh SCREEN để giống người chơi thấy)
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

                        // hitbox mouth (SCREEN for Y, WORLD for X)
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
                            score = min(platformCount, step + 1)
                            isEggFlying = false
                            velY = 0f

                            // bắt rỗ top => scroll camera
                            if (score == platformCount) {
                                startScrollDownAnimation()
                            }
                        }

                        // miss => game over (SCREEN bottom)
                        val bottomLimitScreen = h - bannerHeight - 6f
                        val eggScreenY = eggY - cameraY.value
                        if (eggScreenY > bottomLimitScreen) {
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

            Button(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) { Text("Back", fontSize = 14.sp) }

            // Game Over dialog
            if (gameOver) {
                AlertDialog(
                    onDismissRequest = { },
                    title = { Text("Game Over") },
                    text = { Text("Your score: $finalScore") },
                    confirmButton = {
                        TextButton(onClick = { resetGame() }) { Text("Play Again") }
                    },
                    dismissButton = {
                        TextButton(onClick = onBack) { Text("Back") }
                    }
                )
            }

            // Banner score
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
