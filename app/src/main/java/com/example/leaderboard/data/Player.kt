package com.example.leaderboard.data

data class Player(
    val id: String,
    val username: String,
    val score: Long,
    val avatarSeed: Int
)