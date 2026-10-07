package dev.jdtech.jellyfin.film.presentation.episodebrowser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeNumberLookupTest {
    private data class Candidate(
        val id: String,
        val positionTicks: Long = 0,
        val played: Boolean = false,
        val canPlay: Boolean = true,
        val missing: Boolean = false,
    )

    private val rows =
        listOf(
            EpisodeNumberRange(0, 0),
            EpisodeNumberRange(1, 1),
            EpisodeNumberRange(46, 47),
            EpisodeNumberRange(100, 100),
        )

    @Test
    fun findsSpecialAndZeroNumbers() {
        assertEquals(0, findEpisodeIndex("0", rows))
        assertEquals(1, findEpisodeIndex("1", rows))
    }

    @Test
    fun matchesBothNumbersInCombinedEpisodeRange() {
        assertEquals(2, findEpisodeIndex("46", rows))
        assertEquals(2, findEpisodeIndex("47", rows))
    }

    @Test
    fun returnsExactRowIndexForNonContiguousNumbering() {
        assertEquals(3, findEpisodeIndex("100", rows))
    }

    @Test
    fun rejectsMissingNonNumericAndOverflowInput() {
        assertNull(findEpisodeIndex("48", rows))
        assertNull(findEpisodeIndex("episode", rows))
        assertNull(findEpisodeIndex("2147483648", rows))
        assertNull(findEpisodeIndex("", rows))
    }

    @Test
    fun choosesNextUpWhenThereIsNoResumableEpisode() {
        val completed = Candidate(id = "S1E1", played = true)
        val nextUp = Candidate(id = "S18E46")

        assertEquals(nextUp, chooseContinueTarget(listOf(completed, nextUp)))
    }

    @Test
    fun inProgressEpisodeTakesPriorityOverNextUp() {
        val nextUp = Candidate(id = "S18E46")
        val inProgress = Candidate(id = "S3E12", positionTicks = 500L)

        assertEquals(inProgress, chooseContinueTarget(listOf(nextUp, inProgress)))
    }

    @Test
    fun returnsNullWhenNoContinueCandidateExists() {
        assertNull(chooseContinueTarget(emptyList()))
        assertNull(chooseContinueTarget(listOf(Candidate(id = "played", played = true))))
        assertNull(
            chooseContinueTarget(
                listOf(Candidate(id = "unplayable", canPlay = false, missing = true))
            )
        )
    }

    private fun chooseContinueTarget(candidates: List<Candidate>): Candidate? =
        selectContinueTarget(
            candidates,
            playbackPositionTicks = { it.positionTicks },
            played = { it.played },
            canPlay = { it.canPlay },
            missing = { it.missing },
        )
}
