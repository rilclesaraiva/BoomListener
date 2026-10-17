// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.boomlistener.core.designsystem.component.BoomIcons
import kotlinx.coroutines.delay

/** Bolinha com a inicial do nome (ou o ícone de grupo). */
@Composable
internal fun Avatar(name: String, isGroup: Boolean = false, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    Surface(
        color = if (isGroup) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (isGroup) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        shape = CircleShape,
        modifier = modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isGroup) {
                Icon(BoomIcons.Person, contentDescription = null)
            } else {
                Text(
                    text = name.trim().firstOrNull()?.uppercase() ?: "?",
                    style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

/** "agora", "há 5 min", "ontem"… no idioma do celular. */
internal fun relativeTime(timestampMs: Long, nowMs: Long = System.currentTimeMillis()): String =
    DateUtils.getRelativeTimeSpanString(timestampMs, nowMs, DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE)
        .toString()

/**
 * Repete [block] a cada [intervalMs] só enquanto a tela está visível (o app
 * em segundo plano não fica consultando o servidor).
 */
@Composable
internal fun PollWhileVisible(intervalMs: Long, block: suspend () -> Unit) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                block()
                delay(intervalMs)
            }
        }
    }
}
