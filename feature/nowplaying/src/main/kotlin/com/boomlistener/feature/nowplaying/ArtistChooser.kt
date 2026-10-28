// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.designsystem.component.BoomModalSheet
import com.boomlistener.data.deezer.splitArtistNames

/**
 * Abrir o artista de uma música com vários ("Lilbubblegum, 1nonly", "A feat.
 * B"): em vez de abrir uma página que não existe com o nome junto, sobe um
 * painel com cada artista separado. O nome inteiro fica por último, para
 * bandas com vírgula ou "&" no nome ("Earth, Wind & Fire"). Com um artista
 * só, abre direto.
 */
@Composable
internal fun rememberArtistOpener(onOpenArtist: (String) -> Unit): (String) -> Unit {
    var choosing by remember { mutableStateOf<String?>(null) }
    choosing?.let { full ->
        ArtistChooserSheet(
            fullName = full,
            artists = splitArtistNames(full),
            onPick = { name ->
                choosing = null
                onOpenArtist(name)
            },
            onDismiss = { choosing = null },
        )
    }
    return remember(onOpenArtist) {
        { name -> if (splitArtistNames(name).size > 1) choosing = name else onOpenArtist(name) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtistChooserSheet(
    fullName: String,
    artists: List<String>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BoomModalSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.feature_nowplaying_artists_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            artists.forEach { name ->
                ListItem(
                    modifier = Modifier.clickable { onPick(name) },
                    leadingContent = { Icon(BoomIcons.Person, contentDescription = null) },
                    headlineContent = { Text(name) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            ListItem(
                modifier = Modifier.clickable { onPick(fullName) },
                leadingContent = { Icon(BoomIcons.Collection, contentDescription = null) },
                headlineContent = { Text(fullName) },
                supportingContent = { Text(stringResource(R.string.feature_nowplaying_artist_full_name)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}
