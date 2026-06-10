package com.example.leaderboard.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

@Singleton
class ScoreGeneratorModule @Inject constructor() {

    companion object {
        private val DEFAULT_PLAYERS = listOf(
            Player(id = "p1",  username = "ArjunXPro",    score = 0, avatarSeed = 1),
            Player(id = "p2",  username = "Rahul_K",      score = 0, avatarSeed = 2),
            Player(id = "p3",  username = "Sneha_S",      score = 0, avatarSeed = 3),
            Player(id = "p4",  username = "Vikram_T",     score = 0, avatarSeed = 4),
            Player(id = "p5",  username = "Priya_G",      score = 0, avatarSeed = 5),
            Player(id = "p6",  username = "Mohit_D",      score = 0, avatarSeed = 6),
            Player(id = "p7",  username = "Nikhil_K",     score = 0, avatarSeed = 7),
            Player(id = "p8",  username = "Aisha_R",      score = 0, avatarSeed = 8),
            Player(id = "p9",  username = "Karan_Playz",  score = 0, avatarSeed = 9),
            Player(id = "p10", username = "Divya_Gamer",  score = 0, avatarSeed = 10),
            Player(id = "p11", username = "Rohan_FF",     score = 0, avatarSeed = 11),
            Player(id = "p12", username = "Ananya_Pro",   score = 0, avatarSeed = 12),
            Player(id = "p13", username = "Sameer_BGMI",  score = 0, avatarSeed = 13),
            Player(id = "p14", username = "Isha_Queen",   score = 0, avatarSeed = 14),
            Player(id = "p15", username = "Dev_Sniper",   score = 0, avatarSeed = 15),
            Player(id = "p16", username = "Neha_Storm",   score = 0, avatarSeed = 16),
            Player(id = "p17", username = "Aditya_King",  score = 0, avatarSeed = 17),
            Player(id = "p18", username = "Pooja_Warrior",score = 0, avatarSeed = 18),
            Player(id = "p19", username = "Harsh_Blaze",  score = 0, avatarSeed = 19),
            Player(id = "p20", username = "Tanvi_Legend",   score = 0, avatarSeed = 20),
        )

        private const val MIN_DELAY_MS = 500L
        private const val MAX_DELAY_MS = 2000L
        private const val MIN_SCORE_DELTA = 10L
        private const val MAX_SCORE_DELTA = 300L
    }


    fun getInitialPlayers(): List<Player> = DEFAULT_PLAYERS
    fun scoreUpdates(sessionSeed: Long = System.currentTimeMillis()): Flow<ScoreUpdate> = flow {
        val rng = Random(sessionSeed)
        val playerIds = DEFAULT_PLAYERS.map { it.id }

        while (true) {
            val delayMs = rng.nextLong(MIN_DELAY_MS, MAX_DELAY_MS)
            delay(delayMs)

            val playerId = playerIds[rng.nextInt(playerIds.size)]
            val delta = rng.nextLong(MIN_SCORE_DELTA, MAX_SCORE_DELTA)

            emit(
                ScoreUpdate(
                    playerId = playerId,
                    scoreDelta = delta,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
}