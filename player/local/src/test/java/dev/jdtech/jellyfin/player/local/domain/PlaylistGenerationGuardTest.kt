package dev.jdtech.jellyfin.player.local.domain

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistGenerationGuardTest {
    @Test
    fun anOldDelayedNeighborCannotMutateTheReplacementPlaylistCache() = runBlocking {
        val guard = PlaylistGenerationGuard()
        val oldGeneration = guard.current()
        val delayedNeighbor = CompletableDeferred<String>()
        val playerItemCache = mutableListOf<String>()

        val oldPreload = async {
            val item = delayedNeighbor.await()
            if (guard.isCurrent(oldGeneration)) playerItemCache.add(item)
        }
        yield()

        val newGeneration = guard.advance()
        playerItemCache.clear()
        delayedNeighbor.complete("stale-next-episode")
        oldPreload.await()

        assertEquals(newGeneration, guard.current())
        assertEquals(emptyList<String>(), playerItemCache)
    }
}
