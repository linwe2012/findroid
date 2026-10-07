package dev.jdtech.jellyfin.film.presentation.episodebrowser

data class EpisodeNumberRange(val start: Int, val end: Int)

fun findEpisodeIndex(numberInput: String, episodes: List<EpisodeNumberRange>): Int? {
    val requestedNumber = numberInput.toIntOrNull() ?: return null
    return episodes
        .indexOfFirst { range -> requestedNumber >= range.start && requestedNumber <= range.end }
        .takeIf { it >= 0 }
}

fun <T> selectContinueTarget(
    candidates: List<T>,
    playbackPositionTicks: (T) -> Long,
    played: (T) -> Boolean,
    canPlay: (T) -> Boolean,
    missing: (T) -> Boolean,
): T? =
    candidates.firstOrNull { !played(it) && playbackPositionTicks(it) > 0 }
        ?: candidates.firstOrNull { !played(it) && canPlay(it) && !missing(it) }
