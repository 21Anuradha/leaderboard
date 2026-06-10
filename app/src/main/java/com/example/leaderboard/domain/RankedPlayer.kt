package com.example.leaderboard.domain

import com.example.leaderboard.data.Player

data class RankedPlayer(
    val player: Player,
    val rank: Int,
    val previousRank: Int?,
    val lastScoreDelta: Long,
    val isRecentlyUpdated: Boolean
) {
    val rankChange: RankChange
        get() = when {
            previousRank == null -> RankChange.NONE
            rank < previousRank  -> RankChange.UP
            rank > previousRank  -> RankChange.DOWN
            else                 -> RankChange.SAME
        }
}

enum class RankChange { UP, DOWN, SAME, NONE }