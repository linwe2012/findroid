package dev.jdtech.jellyfin.player.local.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaItemQueueTest {
    @Test
    fun insertsNextBeforePreviousWithoutMovingTheCurrentItem() {
        val queue = mutableListOf("current")

        val insertions =
            queue.insertNeighborsAroundCurrent(
                currentIndex = 0,
                previous = "previous",
                next = "next",
                keyOf = { it },
            )

        assertEquals(listOf("previous", "current", "next"), queue)
        assertEquals(listOf(QueueInsertion(1, "next"), QueueInsertion(0, "previous")), insertions)
    }

    @Test
    fun addsANextItemAfterCurrentWhenCurrentIsAlreadyInTheMiddleOfTheQueue() {
        val queue = mutableListOf("previous", "current", "already-next")

        val insertions =
            queue.insertNeighborsAroundCurrent(
                currentIndex = 1,
                previous = null,
                next = "next",
                keyOf = { it },
            )

        assertEquals(listOf("previous", "current", "next", "already-next"), queue)
        assertEquals(listOf(QueueInsertion(2, "next")), insertions)
    }

    @Test
    fun doesNotDuplicateNeighborsOrInsertAroundAnInvalidCurrentIndex() {
        val queue = mutableListOf("previous", "current", "next")

        assertTrue(
            queue
                .insertNeighborsAroundCurrent(
                    currentIndex = 1,
                    previous = "previous",
                    next = "next",
                    keyOf = { it },
                )
                .isEmpty()
        )
        assertTrue(
            queue
                .insertNeighborsAroundCurrent(
                    currentIndex = -1,
                    previous = "older",
                    next = "newer",
                    keyOf = { it },
                )
                .isEmpty()
        )
        assertEquals(listOf("previous", "current", "next"), queue)
    }

    @Test
    fun nextCommandAvailabilityTracksTheFinalQueueIndexAndAdState() {
        assertFalse(
            canSeekToNextMediaItem(
                mediaItemCount = 1,
                currentMediaItemIndex = 0,
                isPlayingAd = false,
            )
        )
        assertTrue(
            canSeekToNextMediaItem(
                mediaItemCount = 2,
                currentMediaItemIndex = 0,
                isPlayingAd = false,
            )
        )
        assertFalse(
            canSeekToNextMediaItem(
                mediaItemCount = 2,
                currentMediaItemIndex = 1,
                isPlayingAd = false,
            )
        )
        assertFalse(
            canSeekToNextMediaItem(
                mediaItemCount = 2,
                currentMediaItemIndex = 0,
                isPlayingAd = true,
            )
        )
    }
}
