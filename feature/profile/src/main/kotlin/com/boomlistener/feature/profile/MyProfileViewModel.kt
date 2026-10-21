// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.data.profile.MAX_BIO
import com.boomlistener.data.profile.MyProfile
import com.boomlistener.data.profile.ProfileImage
import com.boomlistener.data.profile.ProfileRepository
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.ListeningHistoryRepository
import com.boomlistener.data.wellbeing.ListeningStatsRepository
import com.boomlistener.data.wellbeing.SongStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Foto de um artista no Deezer pelo id (o Deezer redireciona para a imagem). */
internal fun deezerArtistImageUrl(deezerId: Long) = "https://api.deezer.com/artist/$deezerId/image?size=medium"

private val FEAT = Regex("""\s+(feat\.?|ft\.?|featuring)\s+.*$""", RegexOption.IGNORE_CASE)

/** Artista principal: "Djavan feat. Caetano" → "Djavan". */
internal fun artistOf(raw: String): String = raw.replace(FEAT, "").trim()

/** O que fazer com a foto ou a capa ao salvar o perfil. */
internal sealed interface ImageChange {
    data object Keep : ImageChange
    data object Remove : ImageChange
    /** A imagem escolhida no seletor ([uri] content://). */
    data class Set(val uri: String) : ImageChange
}

internal data class MyProfileUiState(
    val profile: MyProfile,
    val stats: ProfileStats,
    val topSongs: List<SongStat>,
    val recent: List<HistoryEntry>,
    val following: List<FollowedArtist>,
    val today: LocalDate,
)

/** O seu perfil: números de escuta, mais ouvidas, histórico e quem você segue. Tudo do aparelho. */
internal class MyProfileViewModel(
    private val profile: ProfileRepository,
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
        profile.profile,
        history.history,
        stats.topSongs(TOP_SONGS),
        follows.followed,
        tracks,
    ) { me, entries, top, following, library ->
        val now = today()
        MyProfileUiState(
            profile = me,
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

    private val _saving = MutableStateFlow(false)

    /** "Salvar" do editar perfil em andamento (preparar as imagens demora um pouco). */
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /**
     * Salva o que mudou no "Editar perfil": nome, descrição, capa e foto. Nada
     * muda no perfil antes disso. [onDone] só é chamado se tudo deu certo.
     */
    fun saveProfile(
        name: String,
        bio: String,
        banner: ImageChange,
        avatar: ImageChange,
        color: String = profile.profile.value.color,
        onDone: () -> Unit,
    ) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            var ok = profile.save(name, bio.take(MAX_BIO), color)
            for ((kind, change) in listOf(ProfileImage.Banner to banner, ProfileImage.Avatar to avatar)) {
                when (change) {
                    ImageChange.Keep -> Unit
                    ImageChange.Remove -> ok = profile.setImage(kind, null) && ok
                    is ImageChange.Set -> ok = profile.setImage(kind, change.uri) && ok
                }
            }
            _saveFailed.value = !ok
            _saving.value = false
            if (ok) onDone()
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
