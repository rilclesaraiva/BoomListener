// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.R

/**
 * Painel "Artistas desta música": cada artista de uma música com vários, com
 * a foto num círculo (a inicial enquanto carrega ou se não houver foto), e o
 * nome inteiro por último (para bandas com vírgula ou "&" no nome).
 * [photo] busca a foto de um artista pelo nome.
 */
@Composable
fun BoomArtistChooserSheet(
    fullName: String,
    artists: List<String>,
    photo: suspend (name: String, sizePx: Int) -> ImageBitmap?,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    BoomModalSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Text(
                stringResource(R.string.core_designsystem_artists_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            artists.forEach { name ->
                ListItem(
                    modifier = Modifier.clickable { onPick(name) },
                    leadingContent = { ArtistPhoto(name, photo) },
                    headlineContent = { Text(name) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }
            ListItem(
                modifier = Modifier.clickable { onPick(fullName) },
                leadingContent = { Icon(BoomIcons.Collection, contentDescription = null) },
                headlineContent = { Text(fullName) },
                supportingContent = { Text(stringResource(R.string.core_designsystem_artist_full_name)) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        }
    }
}

@Composable
private fun ArtistPhoto(name: String, photo: suspend (String, Int) -> ImageBitmap?) {
    val sizePx = with(LocalDensity.current) { ArtistPhotoSize.roundToPx() }
    val image by produceState<ImageBitmap?>(initialValue = null, name) { value = photo(name, sizePx) }
    BoomArtwork(image = image, fallbackText = name, size = ArtistPhotoSize, modifier = Modifier.clip(CircleShape))
}

private val ArtistPhotoSize = 48.dp
