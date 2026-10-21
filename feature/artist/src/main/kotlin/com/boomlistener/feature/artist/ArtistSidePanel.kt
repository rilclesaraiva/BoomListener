// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.core.player.PlayerConnection
import com.boomlistener.data.deezer.DeezerData
import com.boomlistener.data.lastfm.LastfmData
import com.boomlistener.data.library.LibraryData
import com.boomlistener.data.musicbrainz.MusicBrainzData
import com.boomlistener.data.releases.ReleasesData
import kotlinx.coroutines.cancel
import java.text.NumberFormat

/** Quantas populares o painel mostra (a lista inteira fica na página do artista). */
private const val PANEL_POPULAR = 5

/**
 * Painel do artista que está tocando, à direita em telas largas (tablet,
 * celular deitado): foto, nome, seguir, biografia e as populares. Tocar no
 * nome ou em "Ver página do artista" abre a página completa.
 */
@Composable
fun ArtistSidePanel(
    artistName: String,
    onOpenArtist: (String) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    // Um por artista, e descartado quando a música troca de artista (o painel
    // não é uma tela da navegação, então não tem um dono de ViewModel próprio).
    val viewModel = remember(artistName) {
        ArtistViewModel(
            artistName = artistName,
            library = LibraryData.repository(context),
            deezer = DeezerData.repository(context),
            follows = ReleasesData.follows(context),
            player = PlayerConnection.controller(context),
            lastfm = LastfmData.repository(context),
            musicBrainz = MusicBrainzData.repository(context),
            language = java.util.Locale.getDefault().language,
        )
    }
    DisposableEffect(viewModel) { onDispose { viewModel.viewModelScope.cancel() } }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ready = state.profile as? ProfileState.Ready
    val profile = ready?.profile
    val photoPx = with(LocalDensity.current) { 320.dp.roundToPx() }
    val photo by produceState<ImageBitmap?>(null, profile?.artist?.pictureUrl) {
        value = profile?.artist?.pictureUrl?.let { viewModel.remoteImage(it, photoPx)?.asImageBitmap() }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = "title") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.feature_artist_panel_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onCollapse) {
                        Icon(BoomIcons.ChevronRight, contentDescription = stringResource(R.string.feature_artist_panel_hide))
                    }
                }
            }
            item(key = "photo") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(MaterialTheme.shapes.extraLarge)
                        .clickable { onOpenArtist(artistName) },
                ) {
                    val image = photo
                    if (image != null) {
                        Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(BoomIcons.Person, contentDescription = null, modifier = Modifier.size(64.dp))
                            }
                        }
                    }
                }
            }
            item(key = "name") {
                Column {
                    Text(
                        state.name,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { onOpenArtist(artistName) },
                    )
                    if (profile != null && profile.artist.fans > 0) {
                        Text(
                            stringResource(R.string.feature_artist_fans, NumberFormat.getIntegerInstance().format(profile.artist.fans)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            // Seguir só com o artista do Deezer (o radar de lançamentos usa o Deezer).
            if (ready != null && !ready.fromMusicBrainz) {
                item(key = "follow") {
                    if (state.following) {
                        FilledTonalButton(onClick = viewModel::toggleFollow) {
                            Icon(BoomIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.feature_artist_following))
                        }
                    } else {
                        Button(onClick = viewModel::toggleFollow) {
                            Icon(BoomIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.feature_artist_follow))
                        }
                    }
                }
            }
            (state.bio as? BioState.Ready)?.bio?.summary?.takeIf { it.isNotBlank() }?.let { summary ->
                item(key = "bio") {
                    Column {
                        Text(
                            stringResource(R.string.feature_artist_biography),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(summary, style = MaterialTheme.typography.bodyMedium, maxLines = 8, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            val popular = profile?.topTracks.orEmpty().take(PANEL_POPULAR)
            if (popular.isNotEmpty() && ready?.fromMusicBrainz != true) {
                item(key = "popularTitle") {
                    Text(
                        stringResource(R.string.feature_artist_popular),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                items(popular, key = { "p" + it.id }) { track ->
                    val coverPx = with(LocalDensity.current) { 48.dp.roundToPx() }
                    val cover by produceState<ImageBitmap?>(null, track.coverUrl) {
                        value = track.coverUrl?.let { viewModel.remoteImage(it, coverPx)?.asImageBitmap() }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(MaterialTheme.shapes.small)) {
                            cover?.let { Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                                ?: Surface(color = MaterialTheme.colorScheme.surfaceContainerHighest, modifier = Modifier.fillMaxSize()) {}
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            track.albumTitle?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (track.id in state.ownedPopular) {
                            Icon(
                                BoomIcons.Owned,
                                contentDescription = stringResource(R.string.feature_artist_owned),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
            item(key = "open") {
                TextButton(onClick = { onOpenArtist(artistName) }) {
                    Text(stringResource(R.string.feature_artist_panel_open))
                    Spacer(Modifier.width(4.dp))
                    Icon(BoomIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
