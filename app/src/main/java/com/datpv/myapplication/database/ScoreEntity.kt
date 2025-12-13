package com.datpv.myapplication.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scores")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val score: Int,
    val createdAt: Long = System.currentTimeMillis()
)