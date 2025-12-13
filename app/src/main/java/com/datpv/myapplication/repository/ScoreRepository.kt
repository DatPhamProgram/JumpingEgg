package com.datpv.myapplication.repository

import com.datpv.myapplication.database.ScoreDao
import com.datpv.myapplication.database.ScoreEntity
import kotlinx.coroutines.flow.Flow

class ScoreRepository(private val dao: ScoreDao) {

    fun observeTopScores(limit: Int = 10) : Flow<List<ScoreEntity>>
        = dao.observeTopScores(limit)

    suspend fun addScore(score: Int) {
        if(score <= 0) return
        dao.insert(ScoreEntity(score = score))
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}