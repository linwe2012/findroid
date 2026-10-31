package dev.jdtech.jellyfin.player.local.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerSessionGuardTest {
    @Test
    fun aReusedPlayerSessionRejectsCallbacksFromItsPreviousItem() {
        val guard = PlayerSessionGuard()
        val previousSession = guard.begin()
        val activeSession = guard.begin()

        assertFalse(guard.isCurrent(previousSession))
        assertTrue(guard.isCurrent(activeSession))
        assertFalse(
            guard.isCurrentTransition(
                previousSession,
                mediaId = "previous-episode",
                currentMediaId = "new-episode",
            )
        )
        assertTrue(
            guard.isCurrentTransition(
                activeSession,
                mediaId = "new-episode",
                currentMediaId = "new-episode",
            )
        )
        assertFalse(
            guard.isCurrentTransition(
                activeSession,
                mediaId = "previous-episode",
                currentMediaId = "new-episode",
            )
        )
    }

    @Test
    fun endIsHandledOnceAndAdvancesOnlyForTheCurrentItemWithASuccessor() {
        val guard = PlayerSessionGuard()
        val endTracker = PlaybackEndTracker()
        val session = guard.begin()

        assertTrue(endTracker.markHandled(session, occurrence = 1))
        assertFalse(endTracker.markHandled(session, occurrence = 1))
        assertEquals(
            PlaybackEndDecision.ADVANCE,
            decidePlaybackEnd(
                isCurrentSession = guard.isCurrent(session),
                isCurrentItem = true,
                hasNextItem = true,
            ),
        )
        assertEquals(
            PlaybackEndDecision.FINISH,
            decidePlaybackEnd(
                isCurrentSession = guard.isCurrent(session),
                isCurrentItem = true,
                hasNextItem = false,
            ),
        )

        val nextSession = guard.begin()
        assertEquals(
            PlaybackEndDecision.IGNORE,
            decidePlaybackEnd(
                isCurrentSession = guard.isCurrent(session),
                isCurrentItem = false,
                hasNextItem = true,
            ),
        )
        assertTrue(endTracker.markHandled(nextSession, occurrence = 1))
        assertTrue(endTracker.markHandled(nextSession, occurrence = 2))
        assertFalse(endTracker.markHandled(nextSession, occurrence = 2))
    }

    @Test
    fun endDecisionWaitsForNeighborPreloadAndUsesTheCommittedQueue() = runBlocking {
        val preload = CompletableDeferred<Unit>()
        var hasNext = false
        val decision = async {
            awaitNeighborPreloadAndDecide(preload) {
                decidePlaybackEnd(
                    isCurrentSession = true,
                    isCurrentItem = true,
                    hasNextItem = hasNext,
                )
            }
        }

        yield()
        assertFalse(decision.isCompleted)
        hasNext = true
        preload.complete(Unit)

        assertEquals(PlaybackEndDecision.ADVANCE, decision.await())
    }

    @Test
    fun queuedEndCallbacksAreIgnoredAfterThePlayerHasAdvanced() {
        assertTrue(
            isAuthoritativeEndOfItemPause(
                callbackPlayWhenReady = false,
                callbackReasonIsEndOfItem = true,
                currentPlayWhenReady = false,
                currentPlaybackStateIsReady = true,
            )
        )
        assertFalse(
            isAuthoritativeEndOfItemPause(
                callbackPlayWhenReady = false,
                callbackReasonIsEndOfItem = true,
                currentPlayWhenReady = true,
                currentPlaybackStateIsReady = true,
            )
        )
        assertFalse(
            isAuthoritativePlaybackEnded(
                reportedStateIsEnded = true,
                currentStateIsEnded = false,
            )
        )
        assertTrue(
            isAuthoritativePlaybackEnded(
                reportedStateIsEnded = true,
                currentStateIsEnded = true,
            )
        )
    }
}
