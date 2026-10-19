package dev.jdtech.jellyfin.presentation.film.episodebrowser

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.film.presentation.episodebrowser.EpisodeBrowserViewModel
import dev.jdtech.jellyfin.film.presentation.episodebrowser.EpisodeNumberRange
import dev.jdtech.jellyfin.film.presentation.episodebrowser.episodeNumberBands
import dev.jdtech.jellyfin.film.presentation.episodebrowser.findEpisodeIndex
import dev.jdtech.jellyfin.film.presentation.episodebrowser.visibleEpisodeBand
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.presentation.film.components.Direction
import dev.jdtech.jellyfin.presentation.film.components.ItemPoster
import dev.jdtech.jellyfin.presentation.film.components.OverviewText
import kotlinx.coroutines.launch

@Composable
fun EpisodeBrowser(
    viewModel: EpisodeBrowserViewModel,
    onSelect: (FindroidEpisode) -> Unit,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var seasonMenuExpanded by remember { mutableStateOf(false) }
    var numberInput by remember { mutableStateOf("") }
    var numberError by remember { mutableStateOf(false) }
    var showEpisodeDetails by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val episodeRanges =
        remember(state.episodes) {
            state.episodes.map {
                EpisodeNumberRange(it.indexNumber, it.indexNumberEnd ?: it.indexNumber)
            }
        }
    val rangeBands = remember(episodeRanges) { episodeNumberBands(episodeRanges) }
    var preferredBand by remember { mutableStateOf<Int?>(null) }
    val selectedBand by remember {
        androidx.compose.runtime.derivedStateOf {
            val visibleIndex = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
            visibleIndex
                ?.let { episodeRanges.getOrNull(it) }
                ?.let { visibleEpisodeBand(it, rangeBands, preferredBand) }
        }
    }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val numberFieldFocusRequester = remember { FocusRequester() }
    val closeDescription = stringResource(CoreR.string.close)
    val performJump = {
        val index =
            findEpisodeIndex(
                numberInput,
                episodeRanges,
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(CoreR.string.episodes),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.weight(1f))
            val detailsToggleDescription =
                stringResource(
                    if (showEpisodeDetails) CoreR.string.hide_episode_details
                    else CoreR.string.show_episode_details
                )
            IconButton(
                onClick = { showEpisodeDetails = !showEpisodeDetails },
                modifier = Modifier.semantics { contentDescription = detailsToggleDescription },
            ) {
                Text(if (showEpisodeDetails) "▣" else "▤")
            }
            onClose?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.semantics { contentDescription = closeDescription },
                ) {
                    Text("×", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { seasonMenuExpanded = true },
                enabled = state.seasons.isNotEmpty(),
                modifier = Modifier.widthIn(max = 96.dp),
            ) {
                val season = state.seasons.firstOrNull { it.id == state.selectedSeasonId }
                Text(
                    season?.name ?: stringResource(CoreR.string.season),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
                        modifier = Modifier.widthIn(max = 76.dp),
                    ) {
                        Text(
                            stringResource(
                                if (continueEpisode.playbackPositionTicks > 0) {
                                    CoreR.string.resume
                                } else {
                                    CoreR.string.next_up
                                }
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            Box(
                modifier =
                    Modifier.weight(1f).heightIn(min = 48.dp).clickable {
                        numberFieldFocusRequester.requestFocus()
                        keyboardController?.show()
                    },
                contentAlignment = Alignment.Center,
            ) {
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                BasicTextField(
                    value = numberInput,
                    onValueChange = { input ->
                        numberInput = input.filter(Char::isDigit)
                        numberError = false
                    },
                    singleLine = true,
                    textStyle =
                        MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions = KeyboardActions(onDone = { performJump() }),
                    interactionSource = interactionSource,
                    modifier =
                        Modifier.fillMaxWidth()
                            .heightIn(min = 40.dp)
                            .focusRequester(numberFieldFocusRequester),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .heightIn(min = 40.dp)
                                    .border(
                                        width = 1.dp,
                                        color =
                                            if (isFocused) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                        shape = MaterialTheme.shapes.small,
                                    )
                                    .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            if (numberInput.isEmpty()) {
                                Text(
                                    text = stringResource(CoreR.string.episode_number_input),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }
            TextButton(
                onClick = { performJump() },
                modifier = Modifier.widthIn(min = 48.dp),
            ) {
                Text(stringResource(CoreR.string.jump))
            }
        }

        EpisodeRangeChips(
            bands = rangeBands,
            selectedBand = selectedBand,
            onSelect = { band ->
                preferredBand = (band.start - 1) / 100
                scope.launch { listState.animateScrollToItem(band.episodeIndex) }
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        )
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier.fillMaxWidth()
                            .clickable(enabled = enabled) { onSelect(episode) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    if (showEpisodeDetails) {
                        ItemPoster(
                            item = episode,
                            direction = Direction.HORIZONTAL,
                            modifier = Modifier.width(112.dp),
                        )
                        Spacer(Modifier.width(12.dp))
                    }
                    Column {
                        Text(
                            text = "$markers$number. ${episode.name}",
                            color =
                                when {
                                    current -> MaterialTheme.colorScheme.primary
                                    !enabled ->
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (showEpisodeDetails && episode.overview.isNotBlank()) {
                            OverviewText(text = episode.overview, maxCollapsedLines = 3)
                        }
                    }
                }
            }
        }
    }
}
