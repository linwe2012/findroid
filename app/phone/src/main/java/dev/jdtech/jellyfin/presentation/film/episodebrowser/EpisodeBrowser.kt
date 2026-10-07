package dev.jdtech.jellyfin.presentation.film.episodebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.episodebrowser.EpisodeBrowserViewModel
import dev.jdtech.jellyfin.film.presentation.episodebrowser.EpisodeNumberRange
import dev.jdtech.jellyfin.film.presentation.episodebrowser.findEpisodeIndex
import dev.jdtech.jellyfin.models.FindroidEpisode
import kotlinx.coroutines.launch

@Composable
fun EpisodeBrowser(
    viewModel: EpisodeBrowserViewModel,
    onSelect: (FindroidEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var seasonMenuExpanded by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }
    var numberError by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val performJump = {
        val index =
            findEpisodeIndex(
                numberInput,
                state.episodes.map {
                    EpisodeNumberRange(it.indexNumber, it.indexNumberEnd ?: it.indexNumber)
                },
            )
        numberError = index == null
        if (index != null) {
            scope.launch { listState.animateScrollToItem(index) }
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    LaunchedEffect(state.episodes, state.currentEpisodeId, state.continueEpisode?.id) {
        val targetId = state.currentEpisodeId ?: state.continueEpisode?.id
        val targetIndex = state.episodes.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0) listState.scrollToItem(targetIndex)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(CoreR.string.episodes),
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                onClick = { seasonMenuExpanded = true },
                enabled = state.seasons.isNotEmpty(),
            ) {
                val season = state.seasons.firstOrNull { it.id == state.selectedSeasonId }
                Text(season?.name ?: stringResource(CoreR.string.season))
            }
            DropdownMenu(
                expanded = seasonMenuExpanded,
                onDismissRequest = { seasonMenuExpanded = false },
            ) {
                state.seasons.forEach { season ->
                    DropdownMenuItem(
                        text = { Text(season.name) },
                        onClick = {
                            seasonMenuExpanded = false
                            viewModel.selectSeason(season.id)
                            numberError = false
                        },
                    )
                }
            }
            state.continueEpisode
                ?.takeIf { it.id != state.currentEpisodeId }
                ?.let { continueEpisode ->
                    TextButton(
                        onClick = { onSelect(continueEpisode) },
                        enabled = !continueEpisode.missing && continueEpisode.canPlay,
                    ) {
                        Text(
                            stringResource(
                                if (continueEpisode.playbackPositionTicks > 0) {
                                    CoreR.string.resume
                                } else {
                                    CoreR.string.next_up
                                }
                            )
                        )
                    }
                }
        }

        if (state.loadingSeasons || state.loadingEpisodes) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        }
        if (state.seriesError != null) {
            TextButton(onClick = viewModel::retrySeries) {
                Text(stringResource(CoreR.string.retry))
            }
        }
        if (state.episodesError != null) {
            TextButton(onClick = viewModel::retrySeason) {
                Text(stringResource(CoreR.string.retry))
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = numberInput,
                onValueChange = { input ->
                    numberInput = input.filter(Char::isDigit)
                    numberError = false
                },
                label = { Text(stringResource(CoreR.string.episode_number_input)) },
                singleLine = true,
                keyboardOptions =
                    KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { performJump() }),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { performJump() }) { Text(stringResource(CoreR.string.jump)) }
        }
        if (numberError)
            Text(
                stringResource(CoreR.string.episode_number_not_found),
                color = MaterialTheme.colorScheme.error,
            )

        if (
            !state.loadingEpisodes &&
                state.episodes.isEmpty() &&
                state.episodesError == null &&
                state.seriesError == null
        ) {
            Text(stringResource(CoreR.string.no_episodes))
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp)) {
            items(state.episodes, key = { it.id }) { episode ->
                val current = episode.id == state.currentEpisodeId
                val continueTarget = episode.id == state.continueEpisode?.id
                val enabled = !episode.missing && episode.canPlay
                val number =
                    episode.indexNumberEnd
                        ?.takeIf { it > episode.indexNumber }
                        ?.let { "${episode.indexNumber}–$it" } ?: episode.indexNumber.toString()
                val markers = buildString {
                    if (current) append("▶ ${stringResource(CoreR.string.now_playing)} · ")
                    else if (continueTarget) {
                        val label =
                            if (episode.playbackPositionTicks > 0) CoreR.string.resume
                            else CoreR.string.next_up
                        append("● ${stringResource(label)} · ")
                    }
                }
                Text(
                    text = "$markers$number. ${episode.name}",
                    color =
                        when {
                            current -> MaterialTheme.colorScheme.primary
                            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable(enabled = enabled) { onSelect(episode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                )
            }
        }
    }
}
