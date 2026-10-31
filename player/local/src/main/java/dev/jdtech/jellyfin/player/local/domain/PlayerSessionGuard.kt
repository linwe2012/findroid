package dev.jdtech.jellyfin.player.local.domain

import kotlinx.coroutines.Job

/** Identifies a playback activity session and rejects callbacks from an earlier reused activity. */
internal class PlayerSessionGuard {
    private var generation = 0

    fun begin(): Int = ++generation

    fun isCurrent(session: Int): Boolean = session == generation

    fun isCurrentTransition(session: Int, mediaId: String, currentMediaId: String?): Boolean =
        isCurrent(session) && mediaId == currentMediaId
}

internal enum class PlaybackEndDecision {
    ADVANCE,
    FINISH,
    IGNORE,
}

internal fun decidePlaybackEnd(
    isCurrentSession: Boolean,
    isCurrentItem: Boolean,
    hasNextItem: Boolean,
): PlaybackEndDecision =
    when {
        !isCurrentSession || !isCurrentItem -> PlaybackEndDecision.IGNORE
        hasNextItem -> PlaybackEndDecision.ADVANCE
        else -> PlaybackEndDecision.FINISH
    }

internal suspend fun awaitNeighborPreloadAndDecide(
    preloadJob: Job?,
    decide: () -> PlaybackEndDecision,
): PlaybackEndDecision {
    preloadJob?.join()
    return decide()
}

internal fun isAuthoritativeEndOfItemPause(
    callbackPlayWhenReady: Boolean,
    callbackReasonIsEndOfItem: Boolean,
    currentPlayWhenReady: Boolean,
    currentPlaybackStateIsReady: Boolean,
): Boolean =
    !callbackPlayWhenReady &&
        callbackReasonIsEndOfItem &&
        !currentPlayWhenReady &&
        currentPlaybackStateIsReady

internal fun isAuthoritativePlaybackEnded(
    reportedStateIsEnded: Boolean,
    currentStateIsEnded: Boolean,
): Boolean = reportedStateIsEnded && currentStateIsEnded

internal class PlaybackEndTracker {
    private val handledOccurrences = mutableSetOf<Pair<Int, Int>>()

    fun markHandled(session: Int, occurrence: Int): Boolean =
        handledOccurrences.add(session to occurrence)

    fun reset() = handledOccurrences.clear()
}
