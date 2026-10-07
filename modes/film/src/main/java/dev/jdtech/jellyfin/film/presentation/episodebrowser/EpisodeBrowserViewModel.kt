package dev.jdtech.jellyfin.film.presentation.episodebrowser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeBrowserViewModel @Inject constructor(private val repository: JellyfinRepository) :
    ViewModel() {
    private val _state = MutableStateFlow(EpisodeBrowserState())
    val state = _state.asStateFlow()
    private var seriesJob: Job? = null
    private var episodeJob: Job? = null
    private var generation = 0

    fun loadShow(seriesId: UUID) =
        loadSeries(seriesId, currentEpisodeId = null, includeResume = true)

    fun loadFromEpisode(episodeId: UUID) {
        seriesJob?.cancel()
        episodeJob?.cancel()
        val request = ++generation
        seriesJob = viewModelScope.launch {
            _state.value = EpisodeBrowserState(loadingSeasons = true)
            try {
                val current = repository.getEpisode(episodeId)
                loadSeriesContents(current.seriesId, current.id, includeResume = true, request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == generation)
                    _state.value =
                        EpisodeBrowserState(currentEpisodeId = episodeId, seriesError = e)
            }
        }
    }

    fun retrySeries() {
        val episodeId = _state.value.currentEpisodeId
        val seriesId = _state.value.seriesId
        if (episodeId != null && seriesId == null) loadFromEpisode(episodeId)
        else if (seriesId != null) loadSeries(seriesId, episodeId, includeResume = true)
    }

    private fun loadSeries(seriesId: UUID, currentEpisodeId: UUID?, includeResume: Boolean) {
        seriesJob?.cancel()
        episodeJob?.cancel()
        val request = ++generation
        seriesJob = viewModelScope.launch {
            _state.value =
                EpisodeBrowserState(
                    seriesId = seriesId,
                    currentEpisodeId = currentEpisodeId,
                    loadingSeasons = true,
                )
            try {
                loadSeriesContents(seriesId, currentEpisodeId, includeResume, request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == generation)
                    _state.value = _state.value.copy(loadingSeasons = false, seriesError = e)
            }
        }
    }

    private suspend fun loadSeriesContents(
        seriesId: UUID,
        currentEpisodeId: UUID?,
        includeResume: Boolean,
        request: Int,
    ) {
        val seasons = repository.getSeasons(seriesId)
        val nextUpCandidates =
            if (includeResume) {
                repository.getNextUp(seriesId, enableResumable = true)
            } else emptyList()
        val continueEpisode =
            selectContinueTarget(
                nextUpCandidates,
                playbackPositionTicks = { it.playbackPositionTicks },
                played = { it.played },
                canPlay = { it.canPlay },
                missing = { it.missing },
            )
        if (request != generation) return
        val current = currentEpisodeId?.let { repository.getEpisode(it) }
        if (request != generation) return
        val seasonId = current?.seasonId ?: continueEpisode?.seasonId ?: seasons.firstOrNull()?.id
        _state.value =
            EpisodeBrowserState(
                seriesId = seriesId,
                seasons = seasons,
                selectedSeasonId = seasonId,
                currentEpisodeId = currentEpisodeId,
                continueEpisode = continueEpisode,
            )
        if (seasonId != null) loadSeason(seasonId)
    }

    fun selectSeason(seasonId: UUID) {
        if (
            seasonId == _state.value.selectedSeasonId &&
                !_state.value.loadingEpisodes &&
                _state.value.episodesError == null
        )
            return
        _state.value =
            _state.value.copy(
                selectedSeasonId = seasonId,
                episodes = emptyList(),
                loadingEpisodes = true,
                episodesError = null,
            )
        loadSeason(seasonId)
    }

    fun retrySeason() {
        _state.value.selectedSeasonId?.let { seasonId ->
            _state.value =
                _state.value.copy(
                    episodes = emptyList(),
                    loadingEpisodes = true,
                    episodesError = null,
                )
            loadSeason(seasonId)
        }
    }

    private fun loadSeason(seasonId: UUID) {
        episodeJob?.cancel()
        val request = ++generation
        val seriesId = _state.value.seriesId ?: return
        _state.value =
            _state.value.copy(episodes = emptyList(), loadingEpisodes = true, episodesError = null)
        episodeJob = viewModelScope.launch {
            try {
                val episodes = repository.getEpisodes(seriesId, seasonId)
                if (request == generation && _state.value.selectedSeasonId == seasonId) {
                    _state.value = _state.value.copy(episodes = episodes, loadingEpisodes = false)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (request == generation && _state.value.selectedSeasonId == seasonId) {
                    _state.value = _state.value.copy(loadingEpisodes = false, episodesError = e)
                }
            }
        }
    }
}
