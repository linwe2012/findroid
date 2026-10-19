package dev.jdtech.jellyfin.film.presentation.episodebrowser

data class EpisodeNumberRange(val start: Int, val end: Int)

data class EpisodeNumberBand(val start: Int, val end: Int, val episodeIndex: Int) {
    val label: String
        get() = "$start–$end"
}

/** Builds 100-number quick-jump bands from the numbers that actually occur in a season. */
fun episodeNumberBands(episodes: List<EpisodeNumberRange>): List<EpisodeNumberBand> {
    if (episodes.size <= 100) return emptyList()
    val numberedEpisodes = episodes.mapIndexedNotNull { index, range ->
        val start = range.start.coerceAtLeast(1)
        val end = range.end.coerceAtLeast(start)
        if (range.end < 1) null else Triple(index, start, end)
    }
    if (numberedEpisodes.isEmpty()) return emptyList()
    val firstBand = (numberedEpisodes.minOf { it.second } - 1) / 100
    val lastBand = (numberedEpisodes.maxOf { it.third } - 1) / 100
    return (firstBand..lastBand).mapNotNull { band ->
        val start = band * 100 + 1
        val end = start + 99
        val firstIndex =
            numberedEpisodes
                .firstOrNull { (_, episodeStart, episodeEnd) ->
                    episodeStart <= end && episodeEnd >= start
                }
                ?.first ?: return@mapNotNull null
        EpisodeNumberBand(start, end, firstIndex)
    }
}

fun episodeNumberBandIndex(episode: EpisodeNumberRange): Int? =
    episode.start.takeIf { it > 0 }?.let { (it - 1) / 100 }

fun visibleEpisodeBand(
    episode: EpisodeNumberRange,
    bands: List<EpisodeNumberBand>,
    preferredBand: Int?,
): Int? {
    val start = episode.start.coerceAtLeast(1)
    val end = episode.end.coerceAtLeast(start)
    return bands
        .firstOrNull { (it.start - 1) / 100 == preferredBand && start <= it.end && end >= it.start }
        ?.let { (it.start - 1) / 100 } ?: episodeNumberBandIndex(episode)
}

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
