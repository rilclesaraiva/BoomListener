// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.social

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.data.social.ProfileImage
import com.boomlistener.data.social.SocialAccount
import com.boomlistener.data.social.SocialRepository
import com.boomlistener.data.social.SocialResult
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.ListeningHistoryRepository
import com.boomlistener.data.wellbeing.ListeningStatsRepository
import com.boomlistener.data.wellbeing.SongStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Foto de um artista no Deezer pelo id (o Deezer redireciona para a imagem). */
internal fun deezerArtistImageUrl(deezerId: Long) = "https://api.deezer.com/artist/$deezerId/image?size=medium"

private val FEAT = Regex("""\s+(feat\.?|ft\.?|featuring)\s+.*$""", RegexOption.IGNORE_CASE)

/** Artista principal: "Djavan feat. Caetano" → "Djavan". */
internal fun artistOf(raw: String): String = raw.replace(FEAT, "").trim()

/** Tamanho máximo da descrição (o mesmo do servidor). */
internal const val MAX_BIO = 160

internal data class MyProfileUiState(
    val account: SocialAccount?,
    val stats: ProfileStats,
    val topSongs: List<SongStat>,
    val recent: List<HistoryEntry>,
    val following: List<FollowedArtist>,
    val today: LocalDate,
)

/** O seu perfil: números de escuta, mais ouvidas, histórico e quem você segue. Tudo do aparelho. */
internal class MyProfileViewModel(
    private val social: SocialRepository,
    history: ListeningHistoryRepository,
    stats: ListeningStatsRepository,
    private val library: LibraryRepository,
    follows: FollowRepository,
    private val player: PlaybackController,
    /** Foto do artista no Deezer (pela URL); null nos testes. */
    private val remoteImage: suspend (url: String, sizePx: Int) -> Bitmap? = { _, _ -> null },
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val today: () -> LocalDate = { LocalDate.now(zone) },
) : ViewModel() {

    private val tracks = MutableStateFlow<List<Track>>(emptyList())

    val state: StateFlow<MyProfileUiState?> = combine(
        social.account,
        history.history,
        stats.topSongs(TOP_SONGS),
        follows.followed,
        tracks,
    ) { account, entries, top, following, library ->
        val now = today()
        MyProfileUiState(
            account = account,
            stats = computeProfileStats(entries, library.associate { it.uri to it.durationMs }, now, zone),
            topSongs = top,
            recent = entries.take(RECENT),
            following = following,
            today = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saveFailed = MutableStateFlow(false)
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    init {
        viewModelScope.launch { tracks.value = library.getTracks() }
    }

    fun rename(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch { _saveFailed.value = social.updateProfile(name = clean) !is SocialResult.Ok }
    }

    /** Descrição do perfil (até 160 letras). */
    fun setBio(bio: String) {
        viewModelScope.launch { _saveFailed.value = social.updateBio(bio.take(MAX_BIO)) !is SocialResult.Ok }
    }

    private val _uploading = MutableStateFlow<ProfileImage?>(null)

    /** A imagem que está sendo enviada agora (para mostrar o carregando). */
    val uploading: StateFlow<ProfileImage?> = _uploading.asStateFlow()

    /** Troca a foto ou a capa pela imagem [uri] do seletor; null tira a imagem. */
    fun setImage(kind: ProfileImage, uri: String?) {
        if (_uploading.value != null) return
        _uploading.value = kind
        viewModelScope.launch {
            _saveFailed.value = social.setProfileImage(kind, uri) !is SocialResult.Ok
            _uploading.value = null
        }
    }

    fun setSharing(enabled: Boolean) {
        viewModelScope.launch {
            _saveFailed.value = social.updateProfile(sharing = enabled) !is SocialResult.Ok
            if (!enabled) social.publishNowPlaying(null)
        }
    }

    /** Toca as mais ouvidas a partir da escolhida (as que ainda estão no aparelho). */
    fun playTop(index: Int) {
        val current = state.value ?: return
        playUris(current.topSongs.map { it.uri }, current.topSongs.getOrNull(index)?.uri)
    }

    /** Toca o histórico recente a partir da escolhida. */
    fun playRecent(entry: HistoryEntry) {
        val current = state.value ?: return
        playUris(current.recent.map { it.uri }.distinct(), entry.uri)
    }

    private fun playUris(uris: List<String>, start: String?) {
        val byUri = tracks.value.associateBy { it.uri }
        val queue = uris.mapNotNull { byUri[it] }
        val index = queue.indexOfFirst { it.uri == start }
        if (index >= 0) player.play(queue, index)
    }

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)

    /**
     * Foto de um artista: a do Deezer, se você o segue; senão a capa de uma
     * música dele que está no aparelho.
     */
    suspend fun artistImage(name: String, sizePx: Int): Bitmap? {
        val key = name.normalizedForSearch()
        val followed = state.value?.following?.firstOrNull { it.name.normalizedForSearch() == key }
        if (followed != null) {
            remoteImage(deezerArtistImageUrl(followed.deezerId), sizePx)?.let { return it }
        }
        val track = tracks.value.firstOrNull { track ->
            track.artist?.let { artistOf(it).normalizedForSearch() == key } == true
        } ?: return null
        return library.loadArtwork(track.uri, sizePx)
    }

    private companion object {
        const val TOP_SONGS = 12
        const val RECENT = 15
    }
}
