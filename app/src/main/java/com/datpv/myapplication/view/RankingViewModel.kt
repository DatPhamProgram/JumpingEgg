package com.datpv.myapplication.view

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.datpv.myapplication.database.AppDatabase
import com.datpv.myapplication.repository.ScoreRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RankingViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = ScoreRepository(AppDatabase.get(app).scoreDao())

    val topScores: StateFlow<List<Int>> =
        repo.observeTopScores(limit = 5)
            .map { list -> list.map { it.score } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveScore(score: Int) {
        viewModelScope.launch { repo.addScore(score) }
    }

    fun clearAll() {
        viewModelScope.launch { repo.clearAll() }
    }
}