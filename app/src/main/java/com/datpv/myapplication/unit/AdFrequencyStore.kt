package com.datpv.myapplication.unit

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore(name = "ad_frequency_prefs")

object AdFrequencyStore {

    private val ENDGAME_COUNT = intPreferencesKey("playagain_count")
    private val DOA_COUNT = intPreferencesKey("doa_back_count")

    private val RANKING_COUNT = intPreferencesKey("ranking_count")

    val NUMBER_DISPLAY_ADS = 5

    val NUMBER_RESET = 1000


    suspend fun incrementEndGameCount(context: Context) : Int {
        context.dataStore.edit { prefs->
            val current = prefs[ENDGAME_COUNT] ?: 0
            prefs[ENDGAME_COUNT] = current + 1
        }

        return getEndGameCount(context)
    }

    suspend fun getEndGameCount(context: Context) : Int {
        val prefs = context.dataStore.data.first()
        return prefs[ENDGAME_COUNT] ?: 0
    }

    suspend fun resetEndGameCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[ENDGAME_COUNT] = 0 }
    }

    //DOA BACK COUNT
    suspend fun incrementDoaBackCount(context: Context) : Int {
        context.dataStore.edit { prefs->
            val current = prefs[DOA_COUNT] ?: 0
            prefs[DOA_COUNT] = current + 1
        }

        return getDoaBackCount(context)
    }

    suspend fun getDoaBackCount(context: Context) : Int {
        val prefs = context.dataStore.data.first()
        return prefs[DOA_COUNT] ?: 0
    }

    suspend fun resetDoaBackCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[DOA_COUNT] = 0 }
    }

    //RANKING COUNT
    suspend fun incrementRankingCount(context: Context) : Int {
        context.dataStore.edit { prefs->
            val current = prefs[RANKING_COUNT] ?: 0
            prefs[RANKING_COUNT] = current + 1
        }

        return getRankingCount(context)
    }

    suspend fun getRankingCount(context: Context) : Int {
        val prefs = context.dataStore.data.first()
        return prefs[RANKING_COUNT] ?: 0
    }

    suspend fun resetRankingCount(context: Context) {
        context.dataStore.edit { prefs -> prefs[RANKING_COUNT] = 0 }
    }
}