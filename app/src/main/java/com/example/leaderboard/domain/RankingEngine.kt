package com.example.leaderboard.domain

import com.example.leaderboard.data.Player
import com.example.leaderboard.data.ScoreUpdate
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class RankingEngine @Inject constructor() {

    fun applyUpdateAndRank(
        currentScores: Map<String, Player>,
        previousRanks: Map<String, Int>,
        update: ScoreUpdate,
        recentlyUpdatedIds: Set<String>
    ): List<RankedPlayer> {
        val updatedScores = currentScores.toMutableMap()
        val existingPlayer = updatedScores[update.playerId] ?: return emptyList()
        updatedScores[update.playerId] = existingPlayer.copy(
            score = existingPlayer.score + update.scoreDelta
        )
        val sorted = updatedScores.values.sortedByDescending { it.score }

        return assignRanks(sorted, previousRanks, update, recentlyUpdatedIds)
    }
    fun assignRanks(
        sortedPlayers: List<Player>,
        previousRanks: Map<String, Int>,
        lastUpdate: ScoreUpdate,
        recentlyUpdatedIds: Set<String>
    ): List<RankedPlayer> {
        var currentRank = 1
        var lastScore = Long.MAX_VALUE

        return sortedPlayers.mapIndexed { index, player ->
            if (player.score < lastScore) {
                currentRank = index + 1
                lastScore = player.score
            }

            val isUpdated = player.id in recentlyUpdatedIds
            val delta = if (player.id == lastUpdate.playerId) lastUpdate.scoreDelta else 0L

            RankedPlayer(
                player = player,
                rank = currentRank,
                previousRank = previousRanks[player.id],
                lastScoreDelta = delta,
                isRecentlyUpdated = isUpdated
            )
        }
    }

    fun initialRanking(players: List<Player>): List<RankedPlayer> {
        return players.mapIndexed { index, player ->
            RankedPlayer(
                player = player,
                rank = 1,
                previousRank = null,
                lastScoreDelta = 0L,
                isRecentlyUpdated = false
            )
        }
    }
}