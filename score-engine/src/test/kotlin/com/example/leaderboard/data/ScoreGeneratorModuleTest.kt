package com.example.leaderboard.data

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoreGeneratorModuleTest {

    private val generator = ScoreGeneratorModule()

    @Test
    fun `getInitialPlayers returns stable roster`() {
        val players = generator.getInitialPlayers()

        assertEquals(20, players.size)
        assertTrue(players.all { it.score == 0L })
        assertEquals(players.size, players.map { it.id }.toSet().size)
    }

    @Test
    fun `scoreUpdates is deterministic for the same session seed`() = runTest {
        val seed = 42L

        generator.scoreUpdates(seed).test {
            val first = awaitItem()
            val second = awaitItem()

            generator.scoreUpdates(seed).test {
                val replayFirst = awaitItem()
                val replaySecond = awaitItem()

                assertEquals(first.playerId, replayFirst.playerId)
                assertEquals(first.scoreDelta, replayFirst.scoreDelta)
                assertEquals(second.playerId, replaySecond.playerId)
                assertEquals(second.scoreDelta, replaySecond.scoreDelta)
            }

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `scoreUpdates only emits positive deltas`() = runTest {
        generator.scoreUpdates(99L).test {
            repeat(5) {
                val update = awaitItem()
                assertTrue(update.scoreDelta > 0)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
