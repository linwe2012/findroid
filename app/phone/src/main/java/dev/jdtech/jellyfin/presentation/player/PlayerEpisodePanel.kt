package dev.jdtech.jellyfin.presentation.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import dev.jdtech.jellyfin.film.presentation.episodebrowser.EpisodeBrowserViewModel
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.presentation.film.episodebrowser.EpisodeBrowser
import java.util.UUID

@Composable
fun PlayerEpisodePanel(
    currentEpisodeId: UUID?,
    onDismiss: () -> Unit,
    onSelect: (FindroidEpisode) -> Unit,
    viewModel: EpisodeBrowserViewModel = hiltViewModel(),
) {
    LaunchedEffect(currentEpisodeId) { currentEpisodeId?.let(viewModel::loadFromEpisode) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Row(
            Modifier.fillMaxSize().clickable(onClick = onDismiss),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                Modifier.fillMaxHeight().width(380.dp).clickable {},
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    EpisodeBrowser(
                        viewModel = viewModel,
                        onSelect = onSelect,
                        modifier = Modifier.weight(1f),
                        onClose = onDismiss,
                    )
                }
            }
        }
    }
}
