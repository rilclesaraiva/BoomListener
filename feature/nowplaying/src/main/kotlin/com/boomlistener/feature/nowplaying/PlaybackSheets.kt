// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import com.boomlistener.core.designsystem.component.BoomModalSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.core.player.PlaybackPrefs
import com.boomlistener.core.player.PlaybackSettings
import com.boomlistener.core.player.SleepTimer
import kotlinx.coroutines.delay
import java.util.Locale

/** Rótulo do item "Timer para dormir" no menu: mostra quanto falta quando está ligado. */
@Composable
internal fun sleepTimerLabel(timer: SleepTimer?): String {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (timer?.endsAtMs != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }
    return when {
        timer == null -> stringResource(R.string.feature_nowplaying_sleep)
        timer.endOfTrack -> stringResource(R.string.feature_nowplaying_sleep_active_end)
        else -> {
            val minutes = (((timer.endsAtMs ?: now) - now + 59_999) / 60_000).toInt().coerceAtLeast(1)
            pluralStringResource(R.plurals.feature_nowplaying_sleep_active, minutes, minutes)
        }
    }
}

/** "1:05:09" ou "05:09": o tempo que falta, arredondado para cima em segundos. */
internal fun formatCountdown(remainingMs: Long): String {
    val total = ((remainingMs.coerceAtLeast(0) + 999) / 1000)
    val h = total / 3600
    val m = (total % 3600) / 60
    val sec = total % 60
    return if (h > 0) String.format(Locale.ROOT, "%d:%02d:%02d", h, m, sec)
    else String.format(Locale.ROOT, "%02d:%02d", m, sec)
}

/** Minutos do "Personalizar": só dígitos; vazio vale 0; no máximo 23 h 59 min. */
internal fun customSleepMinutes(hours: String, minutes: String): Int {
    val h = hours.filter(Char::isDigit).take(2).toIntOrNull() ?: 0
    val m = minutes.filter(Char::isDigit).take(2).toIntOrNull() ?: 0
    return (h.coerceAtMost(23) * 60 + m.coerceAtMost(59))
}

/**
 * Timer para dormir, como o do relógio do Android: o tempo no topo (correndo
 * quando ligado), atalhos prontos para só tocar e um botão grande para
 * personalizar.
 */
@Composable
internal fun SleepTimerSheet(settings: PlaybackSettings, onDismiss: () -> Unit) {
    val timer by settings.sleepTimer.collectAsStateWithLifecycle()
    var custom by rememberSaveable { mutableStateOf(false) }
    var hours by rememberSaveable { mutableStateOf("") }
    var minutes by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(timer) {
        while (timer?.endsAtMs != null) {
            now = System.currentTimeMillis()
            delay(250)
        }
    }
    val endsAt = timer?.endsAtMs
    BoomModalSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.feature_nowplaying_sleep),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            when {
                timer?.endOfTrack == true -> Text(
                    stringResource(R.string.feature_nowplaying_sleep_end_of_track),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
                endsAt != null -> Text(
                    formatCountdown(endsAt - now),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                else -> Text(
                    formatCountdown(0L),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            if (timer != null) {
                TextButton(onClick = { settings.cancelSleepTimer() }) {
                    Text(stringResource(R.string.feature_nowplaying_sleep_cancel), color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (!custom) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60).forEach { min -> SleepPreset(min, Modifier.weight(1f)) { settings.startSleepTimer(min) } }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SleepPreset(90, Modifier.weight(1f)) { settings.startSleepTimer(90) }
                    FilledTonalButton(
                        onClick = { settings.sleepAtEndOfTrack() },
                        modifier = Modifier.weight(3f).height(48.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                    ) {
                        Text(stringResource(R.string.feature_nowplaying_sleep_end_of_track), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { custom = true }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                    Text(stringResource(R.string.feature_nowplaying_sleep_custom), style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SleepField(hours, R.string.feature_nowplaying_sleep_hours, Modifier.weight(1f)) { hours = it.filter(Char::isDigit).take(2) }
                    SleepField(minutes, R.string.feature_nowplaying_sleep_minutes_label, Modifier.weight(1f)) { minutes = it.filter(Char::isDigit).take(2) }
                }
                Spacer(Modifier.height(16.dp))
                val total = customSleepMinutes(hours, minutes)
                Button(
                    onClick = {
                        settings.startSleepTimer(total)
                        custom = false
                    },
                    enabled = total > 0,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    Text(stringResource(R.string.feature_nowplaying_sleep_start), style = MaterialTheme.typography.titleMedium)
                }
                TextButton(onClick = { custom = false }) { Text(stringResource(R.string.feature_nowplaying_sleep_back)) }
            }
        }
    }
}

@Composable
private fun SleepPreset(minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(48.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Text(stringResource(R.string.feature_nowplaying_sleep_preset, minutes), maxLines = 1)
    }
}

@Composable
private fun SleepField(value: String, label: Int, modifier: Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        placeholder = { Text(0.toString()) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}

/** "1,25×" no formato do idioma. */
internal fun formatSpeed(speed: Float): String = String.format(Locale.getDefault(), "%.2f×", speed)

/** Velocidade de reprodução (a mesma dos Ajustes → Reprodução). */
@Composable
internal fun SpeedSheet(settings: PlaybackSettings, onDismiss: () -> Unit) {
    val prefs by settings.prefs.collectAsStateWithLifecycle()
    var dragging by remember { mutableFloatStateOf(-1f) }
    val speed = if (dragging >= 0f) dragging else prefs.speed
    BoomModalSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.feature_nowplaying_speed), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text(formatSpeed(speed), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(16.dp))
            Slider(
                value = speed,
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    settings.setSpeed(dragging)
                    dragging = -1f
                },
                valueRange = PlaybackPrefs.MIN_SPEED..PlaybackPrefs.MAX_SPEED,
                steps = 29,
                modifier = Modifier.fillMaxWidth(),
            )
            if (prefs.speed != 1f) {
                TextButton(onClick = { settings.setSpeed(1f) }) { Text(stringResource(R.string.feature_nowplaying_speed_reset)) }
            }
        }
    }
}
