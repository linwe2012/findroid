package dev.jdtech.jellyfin.player.local.domain

internal data class QueueInsertion<T>(val index: Int, val item: T)

/** Inserts missing neighbors while keeping the current item between the previous and next items. */
internal fun <T, K> MutableList<T>.insertNeighborsAroundCurrent(
    currentIndex: Int,
    previous: T?,
    next: T?,
    keyOf: (T) -> K,
): List<QueueInsertion<T>> {
    if (currentIndex !in indices) return emptyList()

    val insertions = mutableListOf<QueueInsertion<T>>()
    val currentKey = keyOf(this[currentIndex])

    // Insert after the active item first. Inserting before it can shift MPV's current index
    // asynchronously, so using the player's index again in the same callback can misorder items.
    if (next != null && keyOf(next) != currentKey && none { keyOf(it) == keyOf(next) }) {
        val index = currentIndex + 1
        add(index, next)
        insertions += QueueInsertion(index, next)
    }

    if (
        previous != null && keyOf(previous) != currentKey && none { keyOf(it) == keyOf(previous) }
    ) {
        add(currentIndex, previous)
        insertions += QueueInsertion(currentIndex, previous)
    }

    return insertions
}

internal fun canSeekToNextMediaItem(
    mediaItemCount: Int,
    currentMediaItemIndex: Int,
    isPlayingAd: Boolean,
): Boolean =
    !isPlayingAd && currentMediaItemIndex >= 0 && currentMediaItemIndex < mediaItemCount - 1
