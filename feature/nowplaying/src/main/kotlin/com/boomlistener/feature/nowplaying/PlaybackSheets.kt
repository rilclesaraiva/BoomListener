// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import com.boomlistener.core.designsystem.component.BoomModalSheet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.graphics.Color
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

/** Timer para dormir: 15 min a 1 h 30, ou no fim desta música. */
@Composable
internal fun SleepTimerSheet(settings: PlaybackSettings, onDismiss: () -> Unit) {
    val timer by settings.sleepTimer.collectAsStateWithLifecycle()
    BoomModalSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.feature_nowplaying_sleep),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            listOf(15, 30, 45, 60, 90).forEach { minutes ->
                ListItem(
                    modifier = Modifier.clickable {
                        settings.startSleepTimer(minutes)
                        onDismiss()
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = { Text(pluralStringResource(R.plurals.feature_nowplaying_sleep_minutes, minutes, minutes)) },
                )
            }
            ListItem(
                modifier = Modifier.clickable {
                    settings.sleepAtEndOfTrack()
                    onDismiss()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                headlineContent = { Text(stringResource(R.string.feature_nowplaying_sleep_end_of_track)) },
            )
            if (timer != null) {
                ListItem(
                    modifier = Modifier.clickable {
                        settings.cancelSleepTimer()
                        onDismiss()
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    headlineContent = {
                        Text(stringResource(R.string.feature_nowplaying_sleep_cancel), color = MaterialTheme.colorScheme.error)
                    },
                )
            }
        }
    }
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
