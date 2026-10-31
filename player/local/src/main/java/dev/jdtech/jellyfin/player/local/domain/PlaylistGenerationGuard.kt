package dev.jdtech.jellyfin.player.local.domain

/** Prevents an async result from an old playlist from mutating the replacement playlist. */
internal class PlaylistGenerationGuard {
    private var generation = 0

    fun current(): Int = generation

    fun advance(): Int = ++generation

    fun isCurrent(candidate: Int): Boolean = candidate == generation
}
