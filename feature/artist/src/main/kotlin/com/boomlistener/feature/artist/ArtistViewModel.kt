// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.artist

import com.boomlistener.core.common.ExternalLinks
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.ArtistProfile
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.deezer.ProfileResult
import com.boomlistener.data.deezer.primaryArtistName
import com.boomlistener.data.lastfm.ArtistBio
import com.boomlistener.data.lastfm.BioResult
import com.boomlistener.data.lastfm.LastfmRepository
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.musicbrainz.MbArtistResult
import com.boomlistener.data.musicbrainz.MusicBrainzRepository
import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal sealed interface ProfileState {
    data object Loading : ProfileState
    /** [fromMusicBrainz]: o Deezer não achou; sem seguir, sem populares, álbuns abrem no site. */
    data class Ready(val profile: ArtistProfile, val fromMusicBrainz: Boolean = false) : ProfileState
    data object NotFound : ProfileState
    data object Failed : ProfileState
}

internal sealed interface BioState {
    data object Loading : BioState
    data class Ready(val bio: ArtistBio) : BioState
    data object NotFound : BioState
    data object Failed : BioState
}

internal data class ArtistUiState(
    val name: String,
    val localTracks: List<Track> = emptyList(),
    val localAlbums: Int = 0,
    val profile: ProfileState = ProfileState.Loading,
    val following: Boolean = false,
    val ownedReleases: Set<Long> = emptySet(),
    val ownedPopular: Set<Long> = emptySet(),
    val bio: BioState = BioState.Loading,
)

internal class ArtistViewModel(
    private val artistName: String,
    private val library: LibraryRepository,
    private val deezer: DeezerRepository,
    private val follows: FollowRepository,
    private val player: PlaybackController,
    private val lastfm: LastfmRepository,
    private val language: String,
    /** Reserva quando o Deezer não acha o artista. */
    private val musicBrainz: MusicBrainzRepository? = null,
) : ViewModel() {

    private val localTracks = MutableStateFlow<List<Track>>(emptyList())
    private val profile = MutableStateFlow<ProfileState>(ProfileState.Loading)
    private val bio = MutableStateFlow<BioState>(BioState.Loading)

    val uiState: StateFlow<ArtistUiState> =
        combine(localTracks, profile, follows.followed, bio) { tracks, profileState, followed, bioState ->
            val ready = (profileState as? ProfileState.Ready)?.profile
            ArtistUiState(
                name = ready?.artist?.name ?: artistName,
                localTracks = tracks,
                localAlbums = localAlbumCount(tracks),
                profile = profileState,
                following = ready != null && (profileState as? ProfileState.Ready)?.fromMusicBrainz != true && followed.any { it.deezerId == ready.artist.id },
                ownedReleases = ready?.let { ownedReleaseIds(it.releases, tracks) }.orEmpty(),
                ownedPopular = ready?.let { ownedPopularIds(it.topTracks, tracks) }.orEmpty(),
                bio = bioState,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArtistUiState(name = artistName))

    init {
        viewModelScope.launch { localTracks.value = tracksOfArtist(library.getTracks(), artistName) }
        loadProfile()
        loadBio()
    }

    fun loadBio() {
        bio.value = BioState.Loading
        viewModelScope.launch {
            bio.value = when (val result = lastfm.artistBio(primaryArtistName(artistName), language)) {
                is BioResult.Found -> BioState.Ready(result.bio)
                BioResult.NotFound -> BioState.NotFound
                BioResult.Failed -> BioState.Failed
            }
        }
    }

    fun loadProfile() {
        profile.value = ProfileState.Loading
        viewModelScope.launch {
            profile.value = when (val result = deezer.artistProfile(artistName)) {
                is ProfileResult.Found -> ProfileState.Ready(result.profile)
                ProfileResult.NotFound -> fromMusicBrainz(deezerFailed = false)
                ProfileResult.Failed -> fromMusicBrainz(deezerFailed = true)
            }
        }
    }

    /**
     * O Deezer não achou (ou não respondeu): discografia do MusicBrainz, com
     * as capas do Cover Art Archive. Os álbuns do aparelho ajudam a escolher
     * entre artistas com o mesmo nome.
     */
    private suspend fun fromMusicBrainz(deezerFailed: Boolean): ProfileState {
        val fallback = musicBrainz ?: return if (deezerFailed) ProfileState.Failed else ProfileState.NotFound
        val albums = tracksOfArtist(library.getTracks(), artistName).mapNotNull { it.album }.distinct()
        return when (val result = fallback.artistProfile(artistName, albums)) {
            is MbArtistResult.Found -> ProfileState.Ready(result.profile, fromMusicBrainz = true)
            MbArtistResult.NotFound -> ProfileState.NotFound
            MbArtistResult.Failed -> if (deezerFailed) ProfileState.Failed else ProfileState.NotFound
        }
    }

    fun toggleFollow() {
        val current = profile.value as? ProfileState.Ready ?: return
        if (current.fromMusicBrainz) return // seguir usa o Deezer (radar de lançamentos)
        val ready = current.profile
        viewModelScope.launch {
            if (uiState.value.following) {
                follows.unfollow(ready.artist.id)
            } else {
                follows.follow(FollowedArtist(ready.artist.id, ready.artist.name))
            }
        }
    }

    fun play(index: Int = 0) {
        val tracks = localTracks.value
        if (tracks.isNotEmpty()) player.play(tracks, index.coerceIn(tracks.indices))
    }

    fun shuffle() {
        val tracks = localTracks.value
        if (tracks.isNotEmpty()) player.playShuffled(tracks)
    }

    suspend fun remoteImage(url: String, sizePx: Int): Bitmap? = deezer.image(url, sizePx)

    suspend fun localArtwork(track: Track, sizePx: Int): Bitmap? = library.loadArtwork(track.uri, sizePx)

    /**
     * Link para compartilhar uma música ou álbum do Deezer: o mesmo no YouTube
     * Music (pelo song.link) ou, se não achar, a busca por ele lá.
     */
    suspend fun shareUrl(deezerLink: String?, title: String, artist: String?): String =
        deezerLink?.let { deezer.youtubeMusicLink(it) } ?: ExternalLinks.youtubeMusicSearch(title, artist)
}
