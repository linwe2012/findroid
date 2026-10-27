package dev.jdtech.jellyfin.presentation.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.settings.R
import dev.jdtech.jellyfin.settings.domain.PlaybackSpeeds
import dev.jdtech.jellyfin.settings.domain.models.Preference as BackendPreference
import dev.jdtech.jellyfin.settings.presentation.models.PreferencePlaybackSpeed

@Composable
fun SettingsPlaybackSpeedCard(
    preference: PreferencePlaybackSpeed,
    onUpdate: (PreferencePlaybackSpeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val parsedSpeeds = PlaybackSpeeds.parse(preference.speeds)
    val summary = parsedSpeeds?.joinToString(", ") { "${PlaybackSpeeds.format(it)}x" }.orEmpty()

    SettingsBaseCard(
        preference = preference,
        onClick = { showDialog = true },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(MaterialTheme.spacings.medium)) {
            Text(
                stringResource(preference.nameStringResource),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text =
                    "${stringResource(R.string.playback_default_speed)}: ${preference.defaultSpeed}x  ·  $summary",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (showDialog) {
        var speeds by remember(preference.speeds) { mutableStateOf(preference.speeds) }
        var defaultSpeed by
            remember(preference.defaultSpeed) { mutableStateOf(preference.defaultSpeed) }
        val parsed = PlaybackSpeeds.parse(speeds)
        val defaultNumber = defaultSpeed.toFloatOrNull()
        val isValid = parsed != null && defaultNumber != null && defaultNumber in parsed

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(preference.nameStringResource)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = speeds,
                        onValueChange = { speeds = it },
                        label = { Text(stringResource(R.string.playback_speeds)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = defaultSpeed,
                        onValueChange = { defaultSpeed = it },
                        label = { Text(stringResource(R.string.playback_default_speed)) },
                        suffix = { Text("x") },
                        singleLine = true,
                        modifier =
                            Modifier.fillMaxWidth().padding(top = MaterialTheme.spacings.small),
                        isError =
                            defaultNumber != null && parsed != null && defaultNumber !in parsed,
                    )
                    if (!isValid) {
                        Text(
                            text = stringResource(R.string.playback_speed_invalid),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = MaterialTheme.spacings.small),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = isValid,
                    onClick = {
                        onUpdate(
                            preference.copy(
                                speeds = speeds,
                                defaultSpeed = PlaybackSpeeds.format(defaultNumber!!),
                            )
                        )
                        showDialog = false
                    },
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Preview
@Composable
private fun SettingsPlaybackSpeedCardPreview() {
    FindroidTheme {
        SettingsPlaybackSpeedCard(
            preference =
                PreferencePlaybackSpeed(
                    nameStringResource = R.string.playback_speed,
                    speedsPreference = BackendPreference("speeds", PlaybackSpeeds.DEFAULT),
                    defaultSpeedPreference = BackendPreference("default", "1"),
                    speeds = PlaybackSpeeds.DEFAULT,
                    defaultSpeed = "1",
                ),
            onUpdate = {},
        )
    }
}
