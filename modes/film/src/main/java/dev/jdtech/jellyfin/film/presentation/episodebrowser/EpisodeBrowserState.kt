package dev.jdtech.jellyfin.film.presentation.episodebrowser

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSeason
import java.util.UUID

data class EpisodeBrowserState(
    val seriesId: UUID? = null,
    val seasons: List<FindroidSeason> = emptyList(),
    val selectedSeasonId: UUID? = null,
    val episodes: List<FindroidEpisode> = emptyList(),
    val currentEpisodeId: UUID? = null,
    val continueEpisode: FindroidEpisode? = null,
    val loadingSeasons: Boolean = false,
    val loadingEpisodes: Boolean = false,
    val seriesError: Exception? = null,
    val episodesError: Exception? = null,
)
