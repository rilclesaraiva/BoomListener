// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.designsystem.component.BoomPageTitle
import com.boomlistener.core.designsystem.component.BoomScreenScaffold
import com.boomlistener.core.designsystem.component.BoomSettingsItem
import com.boomlistener.core.designsystem.component.BoomSettingsSection
import com.boomlistener.core.player.PlaybackPrefs
import com.boomlistener.core.player.PlaybackSettingsData
import java.util.Locale
import kotlin.math.roundToInt

/** Ajustes → Reprodução: transição entre músicas, fade ao tocar/pausar e velocidade. */
@Composable
internal fun PlaybackSettingsScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val settings = remember { PlaybackSettingsData.get(context) }
    val prefs by settings.prefs.collectAsStateWithLifecycle()

    BoomScreenScaffold(
        title = stringResource(R.string.feature_settings_playback),
        onBack = onBack,
        modifier = modifier,
        // Sem barra presa na tela: o título é o topo da página.
        topBar = {},
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item(key = "pageTitle") {
                BoomPageTitle(stringResource(R.string.feature_settings_playback), onBack = onBack, large = true)
            }
            item(key = "transition") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_playback_transition)) {
                    // Arrastar mexe só na tela; salva ao soltar.
                    var dragging by remember { mutableFloatStateOf(-1f) }
                    val seconds = (if (dragging >= 0f) dragging else prefs.transitionSeconds.toFloat()).roundToInt()
                    SliderCard(
                        title = if (seconds == 0) {
                            stringResource(R.string.feature_settings_playback_transition_off)
                        } else {
                            pluralStringResource(R.plurals.feature_settings_playback_transition_on, seconds, seconds)
                        },
                        summary = stringResource(R.string.feature_settings_playback_transition_summary),
                    ) {
                        Slider(
                            value = seconds.toFloat(),
                            onValueChange = { dragging = it },
                            onValueChangeFinished = {
                                settings.setTransitionSeconds(dragging.roundToInt())
                                dragging = -1f
                            },
                            valueRange = 0f..PlaybackPrefs.MAX_TRANSITION_SECONDS.toFloat(),
                            steps = PlaybackPrefs.MAX_TRANSITION_SECONDS - 1,
                        )
                    }
                }
            }
            item(key = "fade") {
                BoomSettingsSection(title = null) {
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_playback_fade),
                        subtitle = stringResource(R.string.feature_settings_playback_fade_summary),
                        onClick = { settings.setFadeOnPlayPause(!prefs.fadeOnPlayPause) },
                        trailing = { Switch(checked = prefs.fadeOnPlayPause, onCheckedChange = settings::setFadeOnPlayPause) },
                    )
                    // Gapless: o ExoPlayer já faz sozinho (lê os atrasos do codificador do arquivo).
                    BoomSettingsItem(
                        icon = null,
                        title = stringResource(R.string.feature_settings_playback_gapless),
                        subtitle = stringResource(R.string.feature_settings_playback_gapless_summary),
                    )
                }
            }
            item(key = "speed") {
                BoomSettingsSection(title = stringResource(R.string.feature_settings_playback_speed)) {
                    var dragging by remember { mutableFloatStateOf(-1f) }
                    val speed = if (dragging >= 0f) dragging else prefs.speed
                    SliderCard(
                        title = formatSpeed(speed),
                        summary = stringResource(R.string.feature_settings_playback_speed_summary),
                        action = {
                            if (prefs.speed != 1f) {
                                TextButton(onClick = { settings.setSpeed(1f) }) {
                                    Text(stringResource(R.string.feature_settings_playback_speed_reset))
                                }
                            }
                        },
                    ) {
                        Slider(
                            value = speed,
                            onValueChange = { dragging = it },
                            onValueChangeFinished = {
                                settings.setSpeed(dragging)
                                dragging = -1f
                            },
                            valueRange = PlaybackPrefs.MIN_SPEED..PlaybackPrefs.MAX_SPEED,
                            steps = 29,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** "1,25×" no formato do idioma. */
internal fun formatSpeed(speed: Float): String = String.format(Locale.getDefault(), "%.2f×", speed)

@Composable
private fun SliderCard(
    title: String,
    summary: String,
    action: @Composable () -> Unit = {},
    slider: @Composable () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                action()
            }
            Spacer(Modifier.height(8.dp))
            slider()
        }
    }
}
