// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.boomlistener.core.designsystem.R

/**
 * Atalho para o seu perfil no canto superior direito. O :app fornece (ele
 * sabe o seu nome); [initial] null mostra o ícone de pessoa.
 */
class ProfileShortcut(val initial: String?, val onClick: () -> Unit)

val LocalProfileShortcut = staticCompositionLocalOf<ProfileShortcut?> { null }

/**
 * Os botões do canto superior direito das abas principais: histórico de
 * escuta, ajustes e o seu perfil. Iguais em todas as abas.
 */
@Composable
fun BoomTopActions(onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    IconButton(onClick = onOpenHistory) {
        Icon(BoomIcons.History, contentDescription = stringResource(R.string.core_designsystem_history))
    }
    IconButton(onClick = onOpenSettings) {
        Icon(BoomIcons.Settings, contentDescription = stringResource(R.string.core_designsystem_settings))
    }
    LocalProfileShortcut.current?.let { profile ->
        val label = stringResource(R.string.core_designsystem_profile)
        IconButton(onClick = profile.onClick, modifier = Modifier.semantics { contentDescription = label }) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(32.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initial = profile.initial
                    if (initial != null) {
                        Text(initial, style = MaterialTheme.typography.titleSmall)
                    } else {
                        Icon(
                            BoomIcons.Person,
                            contentDescription = stringResource(R.string.core_designsystem_profile),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}
