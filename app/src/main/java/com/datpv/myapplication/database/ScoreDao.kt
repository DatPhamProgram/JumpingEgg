package com.datpv.myapplication.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScoreDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(score: ScoreEntity)

    @Query("SELECT * FROM scores ORDER BY score DESC, createdAt DESC LIMIT :limit")
    fun observeTopScores(limit: Int = 5): Flow<List<ScoreEntity>>

    @Query("DELETE FROM scores")
    suspend fun clearAll()
}