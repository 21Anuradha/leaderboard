package com.example.leaderboard.domain

import com.example.leaderboard.data.Player
import com.example.leaderboard.data.ScoreUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RankingEngineTest {

    private lateinit var engine: RankingEngine

    private val players = listOf(
        Player("p1", "Alice", 0, 1),
        Player("p2", "Bob", 0, 2),
        Player("p3", "Carol", 0, 3),
        Player("p4", "Dave", 0, 4),
    )

    @Before
    fun setUp() {
        engine = RankingEngine()
    }

    @Test
    fun `initial ranking ties all players at rank 1`() {
        val ranked = engine.initialRanking(players)

        assertEquals(4, ranked.size)
        assertTrue(ranked.all { it.rank == 1 })
        assertTrue(ranked.all { it.previousRank == null })
    }

    @Test
    fun `competition ranking assigns same rank for tied scores and skips next`() {
        val sorted = listOf(
            Player("p1", "Alice", 500, 1),
            Player("p2", "Bob", 500, 2),
            Player("p3", "Carol", 300, 3),
            Player("p4", "Dave", 100, 4),
        )
        val update = ScoreUpdate("p1", 0)

        val ranked = engine.assignRanks(sorted, emptyMap(), update, emptySet())

        assertEquals(listOf(1, 1, 3, 4), ranked.map { it.rank })
    }

    @Test
    fun `applyUpdateAndRank only increases score`() {
        val scores = players.associateBy { it.id }
        val update = ScoreUpdate("p2", 150)

        val ranked = engine.applyUpdateAndRank(scores, emptyMap(), update, setOf("p2"))
        val bob = ranked.first { it.player.id == "p2" }

        assertEquals(150L, bob.player.score)
        assertEquals(150L, bob.lastScoreDelta)
        assertTrue(bob.isRecentlyUpdated)
    }

    @Test
    fun `applyUpdateAndRank sorts by score descending`() {
        val scores = mapOf(
            "p1" to Player("p1", "Alice", 100, 1),
            "p2" to Player("p2", "Bob", 50, 2),
            "p3" to Player("p3", "Carol", 200, 3),
        )
        val update = ScoreUpdate("p2", 10)

        val ranked = engine.applyUpdateAndRank(scores, emptyMap(), update, emptySet())

        assertEquals(listOf("p3", "p1", "p2"), ranked.map { it.player.id })
        assertEquals(listOf(1, 2, 3), ranked.map { it.rank })
    }

    @Test
    fun `rank change reflects movement after score update`() {
        val scores = mapOf(
            "p1" to Player("p1", "Alice", 100, 1),
            "p2" to Player("p2", "Bob", 90, 2),
        )
        val previousRanks = mapOf("p1" to 1, "p2" to 2)
        val update = ScoreUpdate("p2", 50)

        val ranked = engine.applyUpdateAndRank(scores, previousRanks, update, setOf("p2"))
        val bob = ranked.first { it.player.id == "p2" }

        assertEquals(1, bob.rank)
        assertEquals(RankChange.UP, bob.rankChange)
    }
}
