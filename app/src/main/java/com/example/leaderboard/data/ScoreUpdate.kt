package com.example.leaderboard.data

data class ScoreUpdate(
    val playerId: String,
    val scoreDelta: Long,
    val timestamp: Long = System.currentTimeMillis()
)