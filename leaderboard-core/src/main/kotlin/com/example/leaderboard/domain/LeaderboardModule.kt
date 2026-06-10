package com.example.leaderboard.domain

import com.example.leaderboard.data.Player
import com.example.leaderboard.data.ScoreGeneratorModule
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Singleton
class LeaderboardModule @Inject constructor(
    private val scoreGenerator: ScoreGeneratorModule,
    private val rankingEngine: RankingEngine
) {
    companion object {
        private const val ANIMATION_WINDOW_MS = 1200L
    }

    private val moduleScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _leaderboard = MutableStateFlow<List<RankedPlayer>>(emptyList())
    val leaderboard: StateFlow<List<RankedPlayer>> = _leaderboard.asStateFlow()
    private val playerScores = mutableMapOf<String, Player>()
    private val previousRanks = mutableMapOf<String, Int>()
    private val recentlyUpdatedIds = mutableSetOf<String>()
    private var collectionJob: Job? = null

    fun start() {
        if (playerScores.isEmpty()) {
            val players = scoreGenerator.getInitialPlayers()
            players.forEach { playerScores[it.id] = it }
            _leaderboard.value = rankingEngine.initialRanking(players)
        }

        if (collectionJob?.isActive == true) return
        collectionJob = moduleScope.launch {
            scoreGenerator.scoreUpdates().collect { update ->
                _leaderboard.value.forEach { previousRanks[it.player.id] = it.rank }
                recentlyUpdatedIds.add(update.playerId)
                val newLeaderboard = rankingEngine.applyUpdateAndRank(
                    currentScores = playerScores,
                    previousRanks = previousRanks,
                    update = update,
                    recentlyUpdatedIds = recentlyUpdatedIds.toSet()
                )
                newLeaderboard.forEach { playerScores[it.player.id] = it.player }
                _leaderboard.value = newLeaderboard
                launch {
                    delay(ANIMATION_WINDOW_MS)
                    recentlyUpdatedIds.remove(update.playerId)
                    _leaderboard.value = _leaderboard.value.map {
                        if (it.player.id == update.playerId) {
                            it.copy(isRecentlyUpdated = false)
                        } else it
                    }
                }
            }
        }
    }

    fun stop() {
        collectionJob?.cancel()
        collectionJob = null
    }
}
