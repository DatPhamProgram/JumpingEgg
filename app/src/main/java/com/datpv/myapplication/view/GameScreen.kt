package com.datpv.myapplication.view

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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

    // =========================
    // ✅ ADS: Interstitial (UPDATED)
    // =========================
    val context = LocalContext.current
    val activity = context as? Activity

    var totalScoreDisplayInterstitial = 30
    val scoreOffSet = 30

    val interstitialUnitId = stringResource(R.string.admob_interstitial_unit_id)
    val interstitialManager = remember(interstitialUnitId) { InterstitialAdManager(interstitialUnitId) }

    // state cũ của bạn (giữ nguyên)
    var hasShownScore10Ad by remember { mutableStateOf(false) } // bạn đang set true nhưng không dùng tiếp, mình giữ nguyên
    var isAdShowing by remember { mutableStateOf(false) }

    // ✅ NEW: loading dialog state cho interstitial
    var showAdLoading by remember { mutableStateOf(false) }

    // preload sẵn
    LaunchedEffect(Unit) {
        interstitialManager.preload(context)
    }

    // ✅ NEW: Loading dialog (chỉ ads)
    LoadingDialog(show = showAdLoading)

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
        var roundStep by remember { mutableStateOf(0) }
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

            // ✅ reset ads state cho game mới (giữ nguyên + reset loading)
            hasShownScore10Ad = false
            isAdShowing = false
            showAdLoading = false

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

        fun startScrollDownAnimation() {
            if (isScrolling) return
            isScrolling = true

            spawnOneBasketAboveTop()

            scope.launch {
                try {
                    val eggIdx = basketWithEgg
                    if (eggIdx < 0 || eggIdx > baskets.lastIndex) return@launch

                    val eggBasketWorldY = baskets[eggIdx].y
                    val targetCamera = eggBasketWorldY - desiredBottomScreenY()

                    cameraY.animateTo(
                        targetValue = targetCamera,
                        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                    )

                    while (baskets.size < platformCount) {
                        spawnOneBasketAboveTop()
                    }

                    val bottomWorldY = cameraY.value + desiredBottomScreenY()

                    val chosenIndices = baskets
                        .mapIndexed { idx, b -> idx to abs(b.y - bottomWorldY) }
                        .sortedBy { it.second }
                        .take(platformCount)
                        .map { it.first }

                    val chosen = chosenIndices.map { baskets[it] }
                        .sortedByDescending { it.y } // bottom -> top

                    val snapped = chosen.mapIndexed { i, b ->
                        b.copy(y = bottomWorldY - (i * stepY))
                    }

                    baskets.clear()
                    baskets.addAll(snapped)

                    val bottomIdx = baskets.indices.maxBy { baskets[it].y }
                    basketWithEgg = bottomIdx

                    roundStep = 1
                    isEggFlying = false
                    velY = 0f
                } finally {
                    isScrolling = false
                }
            }
        }

        // =========================
        // ✅ GAME LOOP
        // =========================
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

                if (!gameOver && !isAdShowing) {
                    if (basketWithEgg >= 0) {
                        val b = baskets[basketWithEgg]
                        eggX = b.x + (growW * 0.5f - eggW * 0.5f)
                        eggY = b.y + (growH * 0.35f)
                        velY = 0f
                        isEggFlying = false
                        prevEggBottomY = eggY + eggH
                    } else if (isEggFlying) {

                        val order = orderBottomToTop()

                        val step = min(roundStep, platformCount - 1)
                        val targetIndex = order[min(step, order.lastIndex)]
                        val t = baskets[targetIndex]

                        val targetCenterX = t.x + basketW / 2f
                        val eggCenterX = eggX + eggW / 2f

                        val mouthTopScreen = (t.y - cameraY.value) + mouthOffsetY
                        val eggBottomScreenBefore = (eggY - cameraY.value) + eggH
                        val nearInY = (mouthTopScreen - eggBottomScreenBefore) in 0f..assistOnlyWhenNear

                        val assistFactor = if (velY > 0f && nearInY) aimAssist else 0f
                        val newCenterX = eggCenterX + (targetCenterX - eggCenterX) * assistFactor
                        eggX = newCenterX - eggW / 2f

                        prevEggBottomY = eggY + eggH
                        velY += gravity
                        eggY += velY

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

                            roundStep = min(platformCount, step + 1)
                            totalScore += 1

                            // =========================
                            // ✅ ADS TRIGGER (UPDATED)
                            // =========================
                            if (totalScore == totalScoreDisplayInterstitial) {
                                totalScoreDisplayInterstitial += scoreOffSet
                                hasShownScore10Ad = true

                                val act = activity
                                if (act != null) {
                                    isAdShowing = true
                                    showAdLoading = true

                                    // ✅ showOrQueue: nếu chưa load kịp -> loading -> load xong -> show
                                    scope.launch {
                                        interstitialManager.showOrQueue(
                                            activity = act,
                                            timeoutMs = 10_000L,
                                            onState = { state ->
                                                showAdLoading = (state == InterstitialAdManager.State.Loading)
                                            },
                                            onClosedOrFailed = {
                                                // đóng ads / fail -> resume game
                                                showAdLoading = false
                                                isAdShowing = false
                                                // preload lại cho lần sau
                                                scope.launch { interstitialManager.preload(context) }
                                            },
                                            onLoadFailedOrTimeout = {
                                                // timeout/no-fill -> resume game luôn
                                                showAdLoading = false
                                                isAdShowing = false
                                                scope.launch { interstitialManager.preload(context) }
                                            }
                                        )
                                    }
                                }
                            }
                            // =========================

                            if (roundStep == platformCount) {
                                startScrollDownAnimation()
                            }
                        }

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

                delay(16L) // 60fps
            }
        }

        fun jump() {
            if (gameOver) return
            if (isScrolling) return
            if (isEggFlying) return
            if (isAdShowing) return // ✅ chặn input khi ads đang show/loading

            val order = orderBottomToTop()

            if (basketWithEgg >= 0) {
                val pos = order.indexOf(basketWithEgg)
                val nextPos = min(platformCount - 1, pos + 1)
                val nextIndex = order[nextPos]

                val b = baskets[basketWithEgg]
                basketWithEgg = -1

                eggX = b.x + basketW / 2f - eggW / 2f
                eggY = b.y - eggH * 0.3f

                velY = computeJumpVelocityTo(baskets[nextIndex].y)
                isEggFlying = true
                return
            }

            val bottomIndex = order[0]
            velY = computeJumpVelocityTo(baskets[bottomIndex].y)
            isEggFlying = true
        }

        // =========================
        // UI
        // =========================
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
