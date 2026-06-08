package com.datpv.myapplication.util

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "ad_frequency_prefs")

object AdFrequencyStore {

    private val ENDGAME_COUNT = intPreferencesKey("playagain_count")
    private val DOA_COUNT = intPreferencesKey("doa_back_count")

    private val RANKING_COUNT = intPreferencesKey("ranking_count")

    private val HOME_COUNT = intPreferencesKey("home_count")

    private val GAME_OVER_COUNT = intPreferencesKey("game_over_count")

    private val RANKING_OPEN_COUNT = intPreferencesKey("ranking_open_count")
    private val DOA_OPEN_COUNT = intPreferencesKey("doa_open_count")

    private val LAST_AD_SHOWN_AT = longPreferencesKey("last_ad_shown_at")

    val NUMBER_DISPLAY_ADS = 8
    val NUMBER_HOME_DISPLAY_ADS = 10

    /** Số lần mở màn hình trước khi hiện Interstitial (Ranking/Instruction/DOA Game). */
    val NUMBER_SCREEN_OPEN_DISPLAY_ADS = 3

    val NUMBER_RESET = 1000

    /** Khoảng cách tối thiểu giữa 2 lần hiện Interstitial/Rewarded bất kỳ (mọi màn hình),
     *  để ads không dồn dập khi người dùng chuyển màn liên tục. */
    private const val MIN_INTERVAL_BETWEEN_ADS_MS = 45_000L

    /** Nơi (và tần suất "mỗi N lượt") có thể hiện Interstitial/Rewarded trong app. */
    enum class AdSurface(val key: Preferences.Key<Int>, val every: Int) {
        HOME(HOME_COUNT, NUMBER_HOME_DISPLAY_ADS),
        RANKING(RANKING_COUNT, NUMBER_DISPLAY_ADS),
        END_GAME(ENDGAME_COUNT, NUMBER_DISPLAY_ADS),
        DOA_BACK(DOA_COUNT, NUMBER_DISPLAY_ADS),
        GAME_OVER(GAME_OVER_COUNT, NUMBER_DISPLAY_ADS),
        RANKING_OPEN(RANKING_OPEN_COUNT, NUMBER_SCREEN_OPEN_DISPLAY_ADS),
        DOA_OPEN(DOA_OPEN_COUNT, NUMBER_SCREEN_OPEN_DISPLAY_ADS),
    }

    /**
     * Quyết định tập trung: có nên hiện Interstitial/Rewarded cho [surface] ngay bây giờ không.
     * - Không hiện ở lượt đầu tiên (trải nghiệm sạch cho người dùng mới).
     * - Sau đó chỉ hiện mỗi [AdSurface.every] lượt.
     * - Và chỉ khi đã cách lần hiện ads gần nhất (ở bất kỳ đâu trong app) ít nhất
     *   [MIN_INTERVAL_BETWEEN_ADS_MS], tránh dồn dập khi chuyển màn nhanh.
     *
     * Khi trả về true, hàm cũng ghi nhận luôn "vừa hiện ads" để tính cooldown cho lần kế tiếp —
     * caller chỉ cần show ad ngay sau khi nhận true.
     */
    suspend fun decideShowAd(context: Context, surface: AdSurface): Boolean {
        var count = 0
        context.dataStore.edit { prefs ->
            count = (prefs[surface.key] ?: 0) + 1
            prefs[surface.key] = if (count >= NUMBER_RESET) 0 else count
        }

        if (count <= 1) return false
        if (count % surface.every != 0) return false
        if (!canShowAdNow(context)) return false

        markAdShown(context)
        return true
    }

    /** Đã đủ [MIN_INTERVAL_BETWEEN_ADS_MS] kể từ lần hiện Interstitial/Rewarded gần nhất chưa. */
    suspend fun canShowAdNow(context: Context): Boolean {
        val lastShown = context.dataStore.data.first()[LAST_AD_SHOWN_AT] ?: 0L
        return System.currentTimeMillis() - lastShown >= MIN_INTERVAL_BETWEEN_ADS_MS
    }

    /** Ghi nhận thời điểm vừa hiện Interstitial/Rewarded, dùng để tính cooldown lần sau. */
    suspend fun markAdShown(context: Context) {
        context.dataStore.edit { prefs -> prefs[LAST_AD_SHOWN_AT] = System.currentTimeMillis() }
    }


    suspend fun resetEndGameCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[ENDGAME_COUNT] = 0 }
    }

    suspend fun resetDoaBackCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[DOA_COUNT] = 0 }
    }

    suspend fun resetRankingCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[RANKING_COUNT] = 0 }
    }

    suspend fun resetHomeCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[HOME_COUNT] = 0 }
    }

    suspend fun resetGameOverCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[GAME_OVER_COUNT] = 0 }
    }

    suspend fun resetRankingOpenCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[RANKING_OPEN_COUNT] = 0 }
    }

    suspend fun resetDoaOpenCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[DOA_OPEN_COUNT] = 0 }
    }

    suspend fun resetAllCounts(context: Context) {
        resetHomeCount(context)
        resetRankingCount(context)
        resetDoaBackCount(context)
        resetEndGameCount(context)
        resetGameOverCount(context)
        resetRankingOpenCount(context)
        resetDoaOpenCount(context)
    }

}