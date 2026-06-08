package com.datpv.myapplication.viewmodel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.datpv.myapplication.model.Basket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Holds all "Jumping Egg" game state and rules (physics, collisions, scoring,
 * scrolling, ad milestones). Plain state-holder class (not androidx.lifecycle.ViewModel):
 * the game session is transient and the camera animation needs a CoroutineScope that
 * the screen already owns via rememberCoroutineScope().
 *
 * Sizes that depend on the screen (width/height from BoxWithConstraints) are resolved
 * once via [initialize].
 */
class GameViewModel {

    // ===== SIZES (world units, dp-equivalent floats) =====
    val basketW = 92f
    val basketH = 92f
    val growW = 108f
    val growH = 108f
    val eggW = 56f
    val eggH = 56f

    // ===== PHYSICS =====
    private val gravity = 1.2f

    // ===== Difficulty (balanced) =====
    private val aimAssist = 0.08f
    private val assistOnlyWhenNear = 140f

    // mouth hitbox (balanced)
    private val mouthOffsetY = 14f
    private val mouthHeight = 34f
    private val mouthPaddingX = 14f

    // UI
    val bannerHeight = 72f
    private val bottomSafePadding = 18f
    private val basketBottomGap = 10f

    val platformCount = 4

    // ===== camera: scroll thật =====
    val cameraY = Animatable(0f)
    var isScrolling by mutableStateOf(false)
        private set

    // ===== world geometry, resolved once we know the screen size =====
    private var w = 0f
    private var h = 0f
    private var leftLimit = 0f
    private var rightLimit = 0f
    private var baseY1 = 0f
    private var baseY2 = 0f
    private var baseY3 = 0f
    private var stepY = 0f
    private var initialized = false

    // ===== STATES =====
    val baskets = mutableStateListOf<Basket>()

    var isEggFlying by mutableStateOf(false)
        private set
    var basketWithEgg by mutableStateOf(-1) // index in baskets, -1 = outside
        private set

    // tách 2 score:
    var roundStep by mutableStateOf(0)
        private set
    var totalScore by mutableStateOf(0)
        private set

    // Egg dùng WORLD Y để camera kéo cùng nhau
    var eggX by mutableStateOf(0f)
        private set
    var eggY by mutableStateOf(0f)
        private set
    private var velY = 0f
    private var prevEggBottomY = 0f

    // Game over
    var gameOver by mutableStateOf(false)
        private set
    var finalScore by mutableStateOf(0)
        private set
    private var hasNavigatedEndGame = false

    // ===== ADS =====
    private var totalScoreDisplayInterstitial = 30
    private val scoreOffSet = 30
    var hasShownScore10Ad by mutableStateOf(false)
        private set

    // Set/cleared by the screen, which owns the Activity-bound ad manager.
    var isAdShowing by mutableStateOf(false)
    var showAdLoading by mutableStateOf(false)

    /** Invoked from [tick] when a score milestone is reached; the screen wires up ad display. */
    var onScoreMilestone: (() -> Unit)? = null

    fun initialize(width: Float, height: Float) {
        if (initialized) return
        initialized = true

        w = width
        h = height

        leftLimit = 0f
        rightLimit = w - basketW

        // base y theo SCREEN (dp)
        baseY1 = h * 0.66f
        baseY2 = h * 0.44f
        baseY3 = h * 0.22f
        stepY = (baseY1 - baseY2).coerceAtLeast(140f)

        baskets.addAll(initialBaskets())

        eggX = w * 0.5f - eggW / 2f
        eggY = h - bannerHeight - eggH - bottomSafePadding
        prevEggBottomY = eggY + eggH
    }

    private fun initialBaskets(): List<Basket> = listOf(
        Basket(x = w * 0.08f, y = baseY1, dir = 1f,  speed = 3.4f),
        Basket(x = w * 0.58f, y = baseY2, dir = -1f, speed = 3.8f),
        Basket(x = w * 0.18f, y = baseY3, dir = 1f,  speed = 4.2f),
        Basket(x = w * 0.70f, y = baseY3 - stepY, dir = -1f, speed = 4.0f),
    )

    // ===== helpers =====
    private fun orderBottomToTop(): List<Int> =
        baskets.indices.sortedByDescending { baskets[it].y } // world y lớn = thấp

    private fun desiredBottomScreenY(): Float =
        h - bannerHeight - basketH - basketBottomGap

    private fun computeJumpVelocityTo(targetWorldY: Float): Float {
        val desiredPeak = targetWorldY - 80f
        val dy = max(0f, eggY - desiredPeak)
        val v = -sqrt(2f * gravity * dy)
        return v.coerceIn(-36f, -18f)
    }

    fun resetGame(scope: CoroutineScope) {
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

        // reset ads state cho game mới
        hasShownScore10Ad = false
        isAdShowing = false
        showAdLoading = false
        totalScoreDisplayInterstitial = 30

        baskets.clear()
        baskets.addAll(initialBaskets())

        eggX = w * 0.5f - eggW / 2f
        eggY = h - bannerHeight - eggH - bottomSafePadding
        prevEggBottomY = eggY + eggH
    }

    private fun spawnOneBasketAboveTop() {
        val topY = baskets.minOf { it.y }
        val newY = topY - stepY

        val x0 = (w * 0.10f).coerceIn(leftLimit, rightLimit)
        val dir = if ((0..1).random() == 0) -1f else 1f
        val speed = listOf(3.5f, 3.8f, 4.1f).random()

        baskets.add(Basket(x = x0, y = newY, dir = dir, speed = speed))
    }

    private fun startScrollDownAnimation(scope: CoroutineScope) {
        if (isScrolling) return
        isScrolling = true

        scope.launch {
            try {
                val eggIdx = basketWithEgg
                if (eggIdx < 0 || eggIdx > baskets.lastIndex) return@launch

                val eggBasketWorldY = baskets[eggIdx].y
                val targetCamera = eggBasketWorldY - desiredBottomScreenY()

                // Spawn đủ rổ phía trên TRƯỚC khi cuộn, sao cho cả chồng rổ phủ kín
                // toàn bộ khoảng màn hình sẽ đi qua trong lúc cuộn (từ vị trí camera hiện tại
                // tới targetCamera). Nếu không, các rổ cũ trôi khuất phía dưới mà chưa có rổ
                // mới trôi vào từ phía trên -> chỉ thấy 1-2 rổ di chuyển giữa chừng.
                while (baskets.minOf { it.y } > targetCamera) {
                    spawnOneBasketAboveTop()
                }

                cameraY.animateTo(
                    targetValue = targetCamera,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )

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

    /** One frame of the ~60fps game loop. Call repeatedly with delay(16L) from a LaunchedEffect. */
    fun tick(scope: CoroutineScope) {
        // move baskets horizontally (world)
        for (i in baskets.indices) {
            val b = baskets[i]
            var nx = b.x + b.dir * b.speed
            var nd = b.dir

            if (nx <= leftLimit) { nx = leftLimit; nd = 1f }
            if (nx >= rightLimit) { nx = rightLimit; nd = -1f }

            baskets[i] = b.copy(x = nx, dir = nd)
        }

        if (gameOver || isAdShowing) return

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

                // ✅ ADS TRIGGER
                if (totalScore == totalScoreDisplayInterstitial) {
                    totalScoreDisplayInterstitial += scoreOffSet
                    hasShownScore10Ad = true
                    onScoreMilestone?.invoke()
                }

                if (roundStep == platformCount) {
                    startScrollDownAnimation(scope)
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

    fun jump() {
        if (gameOver) return
        if (isScrolling) return
        if (isEggFlying) return
        if (isAdShowing) return // chặn input khi ads đang show/loading

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

    /** Mirrors the original "detect game over during composition" navigation trigger. */
    fun onGameOverDetected(onGameOver: (finalScore: Int) -> Unit) {
        if (!gameOver) return
        finalScore = totalScore
        isEggFlying = false
        velY = 0f
        if (!hasNavigatedEndGame) {
            hasNavigatedEndGame = true
            onGameOver(totalScore)
        }
    }
}
