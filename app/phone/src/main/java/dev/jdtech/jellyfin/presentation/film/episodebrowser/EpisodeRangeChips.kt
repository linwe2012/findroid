package dev.jdtech.jellyfin.presentation.film.episodebrowser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.film.presentation.episodebrowser.EpisodeNumberBand

@Composable
internal fun EpisodeRangeChips(
    bands: List<EpisodeNumberBand>,
    selectedBand: Int?,
    onSelect: (EpisodeNumberBand) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (bands.isEmpty()) return
    val listState = rememberLazyListState()
    val selectedIndex = bands.indexOfFirst { selectedBand == (it.start - 1) / 100 }
    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) listState.animateScrollToItem(selectedIndex)
    }
    LazyRow(
        state = listState,
        modifier = modifier.padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(bands, key = { _, band -> band.start }) { _, band ->
            FilterChip(
                selected = selectedBand == (band.start - 1) / 100,
                onClick = { onSelect(band) },
                label = { Text(band.label, style = MaterialTheme.typography.labelLarge) },
            )
        }
    }
}
