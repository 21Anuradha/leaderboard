package com.example.leaderboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.leaderboard.domain.LeaderboardModule
import com.example.leaderboard.domain.RankedPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val leaderboardModule: LeaderboardModule
) : ViewModel() {

    init {
        leaderboardModule.start()
    }
    val leaderboard: StateFlow<List<RankedPlayer>> = leaderboardModule.leaderboard
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}