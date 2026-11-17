// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.deezer.latestTrackOf
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.Track
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.core.player.AreaPlayer
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.library.PlaylistRepository
import com.boomlistener.data.releases.FollowRepository
import com.boomlistener.data.releases.FollowedArtist
import com.boomlistener.core.common.normalizedForSearch
import com.boomlistener.data.profile.MAX_BIO
import com.boomlistener.data.profile.MyProfile
import com.boomlistener.data.profile.ProfileImage
import com.boomlistener.data.profile.ProfileRepository
import com.boomlistener.data.wellbeing.DailyStatsRepository
import com.boomlistener.data.wellbeing.DailySummary
import com.boomlistener.data.wellbeing.HistoryEntry
import com.boomlistener.data.wellbeing.ListeningHistoryRepository
import com.boomlistener.data.wellbeing.ListeningStatsRepository
import com.boomlistener.data.wellbeing.SongRank
import com.boomlistener.data.wellbeing.SongStat
import com.boomlistener.data.wellbeing.StatsPeriod
import com.boomlistener.data.wellbeing.StatsReport
import com.boomlistener.data.wellbeing.computeStatsReport
import com.boomlistener.data.wellbeing.WrappedAlertsRepository
import com.boomlistener.data.wellbeing.firstDay
import com.boomlistener.data.wellbeing.wrappedCardVisible
import com.boomlistener.data.wellbeing.wrappedSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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
    /** A música mais ouvida (dá o tom do cabeçalho do perfil). */
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
    /** Foto do artista no Deezer pelo nome (o mesmo que a página dele acha); null nos testes. */
    private val artistPhoto: suspend (name: String, sizePx: Int) -> Bitmap? = { _, _ -> null },
    /** A capa do lançamento mais novo do artista no Deezer (a que a página dele usa sem foto). */
    private val artistLatestCover: suspend (name: String, sizePx: Int) -> Bitmap? = { _, _ -> null },
    /** Resumo diário da escuta (tela de Estatísticas); null nos testes que não o usam. */
    private val daily: DailyStatsRepository? = null,
    /** Para a playlist do Top 100 do Wrapped; null nos testes que não a usam. */
    private val playlists: PlaylistRepository? = null,
    /** Aviso de fim de ano do Wrapped (cartão no Perfil); null nos testes que não o usam. */
    private val wrappedAlerts: WrappedAlertsRepository? = null,
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

    private val _period = MutableStateFlow(StatsPeriod.ThisMonth)

    /** Período escolhido na tela de Estatísticas. */
    val period: StateFlow<StatsPeriod> = _period.asStateFlow()

    fun selectPeriod(period: StatsPeriod) {
        _period.value = period
    }

    /** As contas da tela de Estatísticas para o período escolhido (null até carregar). */
    val report: StateFlow<StatsReport?> = combine(daily?.summary ?: emptyFlow<DailySummary>(), _period, tracks) { summary, period, library ->
        val durations = library.associate { it.uri to it.durationMs }
        computeStatsReport(summary, period, today(), { uri -> durations[uri] ?: 0L })
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** A música que toca no Wrapped: a mais ouvida do período que ainda está no aparelho (senão a seguinte). */
    val wrappedSong: StateFlow<SongRank?> = combine(report, tracks) { current, library ->
        val existing = library.mapTo(HashSet()) { it.uri }
        current?.let { wrappedSong(it.songs) { uri -> uri in existing } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val dismissedYear = MutableStateFlow(wrappedAlerts?.dismissedYear ?: 0)

    /** Mostra o cartão "Seu Wrapped está pronto" no Perfil (dezembro, sem a notificação ter saído). */
    val wrappedCard: StateFlow<Boolean> = combine(
        daily?.summary ?: emptyFlow<DailySummary>(),
        wrappedAlerts?.enabled ?: flowOf(false),
        dismissedYear,
    ) { summary, enabled, dismissed ->
        wrappedCardVisible(today(), enabled, wrappedAlerts?.notifiedYear ?: 0, dismissed, firstDay(summary))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun dismissWrappedCard() {
        val year = today().year
        wrappedAlerts?.dismissedYear = year
        dismissedYear.value = year
    }

    /** O player só do Wrapped (pausa o principal ao abrir e o devolve ao fechar). */
    fun areaPlayer(context: android.content.Context) = AreaPlayer(context, player)

    /** Cria a playlist com as músicas do Top 100 do período que ainda estão no aparelho; devolve quantas entraram (0 = não criou). */
    fun createTopPlaylist(name: String, onDone: (Int) -> Unit) {
        val repository = playlists ?: return onDone(0)
        val existing = tracks.value.mapTo(HashSet()) { it.uri }
        val uris = report.value?.songs.orEmpty().map { it.uri }.filter { it in existing }
        if (uris.isEmpty()) return onDone(0)
        viewModelScope.launch {
            repository.create(name, uris)
            onDone(uris.size)
        }
    }

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
        solidColor: Boolean = profile.profile.value.solidColor,
        onDone: () -> Unit,
    ) {
        if (_saving.value) return
        _saving.value = true
        viewModelScope.launch {
            var ok = profile.save(name, bio.take(MAX_BIO), color, solidColor)
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

    /** Toca as músicas do ranking das Estatísticas a partir da escolhida (as que ainda estão no aparelho). */
    fun playReportSongs(index: Int) {
        val songs = report.value?.songs ?: return
        playUris(songs.map { it.uri }, songs.getOrNull(index)?.uri)
    }

    /** Toca o histórico recente a partir da escolhida. */
    fun playRecent(entry: HistoryEntry) {
        val current = state.value ?: return
        playUris(current.recent.map { it.uri }.distinct(), entry.uri)
    }

    /** Toca as músicas de um artista que estão no aparelho (do menu do artista). */
    fun playArtist(name: String, shuffled: Boolean) {
        val key = name.normalizedForSearch()
        val songs = tracks.value.filter { track -> track.artist?.let { artistOf(it).normalizedForSearch() == key } == true }
        if (songs.isEmpty()) return
        if (shuffled) player.playShuffled(songs) else player.play(songs, 0)
    }

    private fun playUris(uris: List<String>, start: String?) {
        val byUri = tracks.value.associateBy { it.uri }
        val queue = uris.mapNotNull { byUri[it] }
        val index = queue.indexOfFirst { it.uri == start }
        if (index >= 0) player.play(queue, index)
    }

    /** Sobe quando as tags mudam: as capas guardadas na memória de imagens deixam de valer. */
    val libraryVersion: kotlinx.coroutines.flow.StateFlow<Int> get() = library.version

    suspend fun artwork(uri: String, sizePx: Int): Bitmap? = library.loadArtwork(uri, sizePx)

    /**
     * Foto de um artista: a do Deezer (pelo id, se você o segue; senão
     * procurando pelo nome, como a página dele); sem ela (sem internet, artista
     * que o Deezer não conhece), a capa do último lançamento dele que está no aparelho.
     */
    suspend fun artistImage(name: String, sizePx: Int, coverFallback: Boolean = true): Bitmap? {
        val key = name.normalizedForSearch()
        val followed = state.value?.following?.firstOrNull { it.name.normalizedForSearch() == key }
        if (followed != null) {
            remoteImage(deezerArtistImageUrl(followed.deezerId), sizePx)?.let { return it }
        } else {
            artistPhoto(name, sizePx)?.let { return it }
        }
        if (!coverFallback) return null
        runCatching { artistLatestCover.invoke(name, sizePx) }.getOrNull()?.let { return it }
        val track = latestTrackOf(tracks.value, name) ?: return null
        return library.loadArtwork(track.uri, sizePx)
    }

    private companion object {
        const val TOP_SONGS = 1
        const val RECENT = 15
    }
}
