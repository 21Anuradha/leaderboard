package com.example.leaderboard.domain

import com.example.leaderboard.data.ScoreGeneratorModule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LeaderboardModuleTest {

    @Test
    fun `start initializes all players at rank 1`() {
        val module = LeaderboardModule(
            scoreGenerator = ScoreGeneratorModule(),
            rankingEngine = RankingEngine()
        )

        module.start()

        val initial = module.leaderboard.value
        assertEquals(20, initial.size)
        assertTrue(initial.all { it.rank == 1 })
        assertTrue(initial.all { it.player.score == 0L })

        module.stop()
    }

    @Test
    fun `start is idempotent and does not duplicate collectors`() {
        val module = LeaderboardModule(
            scoreGenerator = ScoreGeneratorModule(),
            rankingEngine = RankingEngine()
        )

        module.start()
        module.start()

        assertEquals(20, module.leaderboard.value.size)

        module.stop()
    }
}
