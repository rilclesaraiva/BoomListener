// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.releases

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.releases.FollowedArtist
import com.boomlistener.data.releases.ReleasesData

private val FollowedAvatar = 40.dp

/** Os artistas que você segue, em ordem alfabética (para a barra lateral). */
@Composable
fun rememberFollowedArtists(): List<FollowedArtist> {
    val context = LocalContext.current.applicationContext
    val follows = remember { ReleasesData.follows(context) }
    val artists by follows.followed.collectAsStateWithLifecycle(emptyList())
    return remember(artists) { artists.sortedBy { it.name.lowercase() } }
}

/**
 * Os artistas que você segue, um embaixo do outro, dentro da lista da barra
 * lateral (ela rola inteira, não só esta parte). [collapsed] mostra só as
 * fotos. Tocar abre a página do artista.
 */
fun LazyListScope.followedArtistItems(
    artists: List<FollowedArtist>,
    collapsed: Boolean,
    onOpenArtist: (String) -> Unit,
) {
    if (!collapsed && artists.isNotEmpty()) {
        item(key = "followed-title") {
            Text(
                stringResource(R.string.feature_releases_following),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
    items(artists, key = { "followed-" + it.deezerId }) { artist ->
        FollowedArtistRow(artist, collapsed, onClick = { onOpenArtist(artist.name) })
    }
}

@Composable
private fun FollowedArtistRow(artist: FollowedArtist, collapsed: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val px = with(LocalDensity.current) { FollowedAvatar.roundToPx() }
    val photo by produceState<ImageBitmap?>(null, artist.deezerId) {
        value = DeezerData.repository(context)
            .image("https://api.deezer.com/artist/${artist.deezerId}/image?size=medium", px)
            ?.asImageBitmap()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(FollowedAvatar),
        ) {
            Box(contentAlignment = Alignment.Center) {
                val image = photo
                if (image != null) {
                    Image(image, contentDescription = artist.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(artist.name.trim().firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        if (!collapsed) {
            Spacer(Modifier.width(12.dp))
            Text(artist.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
