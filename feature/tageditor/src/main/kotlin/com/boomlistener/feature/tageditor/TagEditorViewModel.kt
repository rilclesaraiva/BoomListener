// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import android.content.IntentSender
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.core.model.TrackTags
import com.boomlistener.core.player.PlaybackController
import com.boomlistener.data.deezer.AUTO_PICK_CONFIDENCE
import com.boomlistener.data.deezer.DeezerRepository
import com.boomlistener.data.deezer.LocalTrackInfo
import com.boomlistener.data.deezer.TrackCandidate
import com.boomlistener.data.deezer.TrackSearchResult
import com.boomlistener.data.deezer.TrackTagsResult
import com.boomlistener.data.deezer.artistSearchNames
import com.boomlistener.data.deezer.splitArtistNames
import com.boomlistener.data.lastfm.LastfmRepository
import com.boomlistener.data.lastfm.pickGenre
import com.boomlistener.data.deezer.rankCandidates
import com.boomlistener.data.musicbrainz.MbRecording
import com.boomlistener.data.musicbrainz.MbSearchResult
import com.boomlistener.data.musicbrainz.MusicBrainzRepository
import com.boomlistener.data.musicbrainz.musicBrainzKey
import com.boomlistener.data.musicbrainz.toTagSuggestion
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.tags.CoverEdit
import com.boomlistener.data.tags.TagsReadResult
import com.boomlistener.data.tags.TagsRepository
import com.boomlistener.data.tags.TagsWriteResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A capa como está no formulário. */
internal sealed interface CoverState {
    data object Original : CoverState
    data object Removed : CoverState
    /** Nova capa (escolhida pelo usuário ou vinda das tags automáticas). */
    class New(val data: ByteArray) : CoverState
}

/** Andamento das tags automáticas. */
internal sealed interface AutoTagState {
    data object Idle : AutoTagState
    data object Searching : AutoTagState
    /** Várias opções parecidas: o usuário escolhe. */
    data class Choosing(val candidates: List<TrackCandidate>) : AutoTagState
    /** Preencheu o formulário com [chosen]; [others] são as outras opções. */
    data class Applied(val chosen: TrackCandidate, val others: List<TrackCandidate>) : AutoTagState
    /**
     * Achou [chosen], e as tags do arquivo já estão iguais: nada é trocado.
     * [foundCover] é a capa encontrada, para a pessoa comparar com a dela e
     * escolher se troca ([TagEditorViewModel.useFoundCover]).
     */
    class AlreadyCorrect(
        val chosen: TrackCandidate,
        val others: List<TrackCandidate>,
        val foundCover: ByteArray?,
    ) : AutoTagState
    data object NotFound : AutoTagState
    data object Failed : AutoTagState
}

internal data class TagEditorUiState(
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val original: TrackTags = TrackTags(),
    val tags: TrackTags = TrackTags(),
    val originalCover: ByteArray? = null,
    val cover: CoverState = CoverState.Original,
    val saving: Boolean = false,
    val autoTag: AutoTagState = AutoTagState.Idle,
    val durationMs: Long = 0,
) {
    /** Há algo diferente do que está gravado no arquivo? */
    val hasChanges: Boolean get() = tags != original || cover != CoverState.Original

    /** A capa que aparece na tela agora (null = sem capa). */
    val shownCover: ByteArray?
        get() = when (cover) {
            CoverState.Original -> originalCover
            CoverState.Removed -> null
            is CoverState.New -> cover.data
        }
}

/** Avisos de uma vez só para a tela (pedir permissão, mostrar mensagem). */
internal sealed interface TagEditorEvent {
    class AskWritePermission(val request: IntentSender) : TagEditorEvent
    data object AskStoragePermission : TagEditorEvent
    data object Saved : TagEditorEvent
    data object SaveFailed : TagEditorEvent
}

/**
 * Editor de tags de uma música: carrega do arquivo, deixa editar, preenche
 * sozinho com dados oficiais (Deezer, ou MusicBrainz quando o Deezer não
 * acha; gênero do Last.fm) e grava. Nada é
 * gravado sem o usuário tocar em "Salvar".
 */
internal class TagEditorViewModel(
    private val uri: String,
    private val tagsRepository: TagsRepository,
    private val library: LibraryRepository,
    private val deezer: DeezerRepository,
    private val lastfm: LastfmRepository,
    private val player: PlaybackController,
    /** Reserva quando o Deezer não acha a música (ou está fora do ar). */
    private val musicBrainz: MusicBrainzRepository? = null,
) : ViewModel() {

    // Opções que vieram do MusicBrainz (pelo trackId da opção).
    private val fromMusicBrainz = mutableMapOf<Long, MbRecording>()

    private val _state = MutableStateFlow(TagEditorUiState())
    val state: StateFlow<TagEditorUiState> = _state.asStateFlow()

    private val _events = Channel<TagEditorEvent>(Channel.BUFFERED)
    val events: Flow<TagEditorEvent> = _events.receiveAsFlow()

    // Como o formulário estava antes das tags automáticas (para "Desfazer").
    private var beforeAuto: Pair<TrackTags, CoverState>? = null
    private var autoJob: Job? = null

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, loadFailed = false) }
        viewModelScope.launch {
            when (val result = tagsRepository.read(uri)) {
                is TagsReadResult.Found -> _state.update {
                    it.copy(
                        loading = false,
                        original = result.tags,
                        tags = result.tags,
                        originalCover = result.cover,
                        cover = CoverState.Original,
                        durationMs = result.durationMs,
                    )
                }
                TagsReadResult.Failed -> _state.update { it.copy(loading = false, loadFailed = true) }
            }
        }
    }

    fun edit(change: (TrackTags) -> TrackTags) {
        _state.update { it.copy(tags = change(it.tags)) }
    }

    fun setCover(data: ByteArray) {
        _state.update { it.copy(cover = CoverState.New(data)) }
    }

    fun removeCover() {
        _state.update { it.copy(cover = if (it.originalCover == null) CoverState.Original else CoverState.Removed) }
    }

    /** Volta tudo para o que está gravado no arquivo. */
    fun resetAll() {
        beforeAuto = null
        _state.update { it.copy(tags = it.original, cover = CoverState.Original, autoTag = AutoTagState.Idle) }
    }

    // ---- Tags automáticas -------------------------------------------------

    /**
     * Procura a música no Deezer. Se um resultado bate com folga, já preenche
     * o formulário (o usuário revisa e salva); se houver dúvida, mostra as opções.
     */
    fun autoTag() {
        autoJob?.cancel()
        val current = _state.value
        val local = LocalTrackInfo(
            title = current.tags.title.ifBlank { current.original.title },
            artist = current.tags.artist.ifBlank { current.original.artist }.ifBlank { null },
            album = current.tags.album.ifBlank { null },
            durationMs = current.durationMs,
        )
        if (local.title.isBlank()) {
            _state.update { it.copy(autoTag = AutoTagState.NotFound) }
            return
        }
        _state.update { it.copy(autoTag = AutoTagState.Searching) }
        autoJob = viewModelScope.launch {
            when (val result = deezer.searchTrack(local)) {
                is TrackSearchResult.Found -> offer(result.candidates)
                TrackSearchResult.NotFound -> searchMusicBrainz(local, deezerFailed = false)
                TrackSearchResult.Failed -> searchMusicBrainz(local, deezerFailed = true)
            }
        }
    }

    /** Resultado claro preenche sozinho; com dúvida, mostra as opções. */
    private suspend fun offer(candidates: List<TrackCandidate>) {
        val best = candidates.first()
        val runnerUp = candidates.getOrNull(1)
        val clearWinner = best.confidence >= AUTO_PICK_CONFIDENCE &&
            (runnerUp == null || best.confidence - runnerUp.confidence >= 0.1f)
        if (clearWinner) {
            applyCandidate(best, candidates)
        } else {
            _state.update { it.copy(autoTag = AutoTagState.Choosing(candidates)) }
        }
    }

    /**
     * O Deezer não achou: tenta no MusicBrainz, com as mesmas regras de
     * comparação. Só é "sem conexão" se nenhum dos dois respondeu.
     */
    private suspend fun searchMusicBrainz(local: LocalTrackInfo, deezerFailed: Boolean) {
        // Com vários artistas, o nome inteiro primeiro e, sem nada, cada um deles.
        val names = local.artist?.let(::artistSearchNames).orEmpty().ifEmpty { listOf(null) }
        var result: MbSearchResult? = null
        for (name in names) {
            result = musicBrainz?.searchRecordings(local.title, name) ?: break
            if (result is MbSearchResult.Found) break
        }
        val candidates = (result as? MbSearchResult.Found)?.recordings.orEmpty().map { recording ->
            val candidate = TrackCandidate(
                trackId = musicBrainzKey(recording.recordingId),
                albumId = musicBrainzKey(recording.releaseGroupId ?: recording.releaseId ?: recording.recordingId),
                title = recording.title,
                artist = recording.artist,
                album = recording.album,
                coverUrl = recording.thumbnailUrl,
                durationMs = recording.durationMs,
                link = recording.link,
            )
            fromMusicBrainz[candidate.trackId] = recording
            candidate
        }.let { rankCandidates(local, it) }
        when {
            candidates.isNotEmpty() -> offer(candidates)
            deezerFailed && (result == null || result == MbSearchResult.Failed) ->
                _state.update { it.copy(autoTag = AutoTagState.Failed) }
            else -> _state.update { it.copy(autoTag = AutoTagState.NotFound) }
        }
    }

    /** As tags já estavam certas, mas a pessoa quer a capa encontrada no lugar da dela. */
    fun useFoundCover() {
        val auto = _state.value.autoTag as? AutoTagState.AlreadyCorrect ?: return
        val cover = auto.foundCover ?: return
        val current = _state.value
        beforeAuto = current.tags to current.cover
        _state.update { it.copy(cover = CoverState.New(cover), autoTag = AutoTagState.Applied(auto.chosen, auto.others)) }
    }

    /** Mostra de novo as opções encontradas (para trocar a escolhida). */
    fun showOtherMatches() {
        val (chosen, others) = when (val auto = _state.value.autoTag) {
            is AutoTagState.Applied -> auto.chosen to auto.others
            is AutoTagState.AlreadyCorrect -> auto.chosen to auto.others
            else -> return
        }
        _state.update { it.copy(autoTag = AutoTagState.Choosing(listOf(chosen) + others)) }
    }

    /** Fechou a lista de opções sem escolher. */
    fun dismissChoices() {
        _state.update {
            it.copy(autoTag = if (beforeAuto != null) it.autoTag.toAppliedOrIdle() else AutoTagState.Idle)
        }
    }

    fun choose(candidate: TrackCandidate) {
        val all = (_state.value.autoTag as? AutoTagState.Choosing)?.candidates ?: listOf(candidate)
        autoJob?.cancel()
        _state.update { it.copy(autoTag = AutoTagState.Searching) }
        autoJob = viewModelScope.launch { applyCandidate(candidate, all) }
    }

    /** Desfaz as tags automáticas (volta ao que estava no formulário antes). */
    fun undoAutoTag() {
        val (tags, cover) = beforeAuto ?: return
        beforeAuto = null
        _state.update { it.copy(tags = tags, cover = cover, autoTag = AutoTagState.Idle) }
    }

    private suspend fun applyCandidate(candidate: TrackCandidate, all: List<TrackCandidate>) {
        val fromMb = fromMusicBrainz[candidate.trackId]
        val suggestion = if (fromMb != null) {
            fromMb.toTagSuggestion(candidate.confidence)
        } else {
            when (val result = deezer.trackTags(candidate)) {
                is TrackTagsResult.Found -> result.suggestion
                TrackTagsResult.Failed -> {
                    _state.update { it.copy(autoTag = AutoTagState.Failed) }
                    return
                }
            }
        }
        val official = suggestion.tags
        // Gênero e capa ao mesmo tempo (mais rápido que um depois do outro).
        val (lastfmGenre, coverBytes) = coroutineScope {
            // Gênero: o do Last.fm é mais preciso ("Math Rock" em vez de "Rock"),
            // procurado pelo primeiro artista (com vários, o Last.fm não acha).
            val genre = async {
                val firstArtist = splitArtistNames(official.artist).firstOrNull() ?: official.artist
                pickGenre(lastfm.trackTags(firstArtist, official.title), firstArtist)
            }
            // Capa: a do Cover Art Archive (lançamento, ou o álbum em qualquer edição) ou a do Deezer.
            val cover = async {
                if (fromMb != null) musicBrainz?.coverBytes(fromMb) else suggestion.coverUrl?.let { deezer.imageBytes(it) }
            }
            genre.await() to cover.await()
        }

        val current = _state.value
        val others = all.filter { other -> other.trackId != candidate.trackId }
        val merged = mergeSuggestion(
            beforeAuto?.first ?: current.tags,
            official.copy(genre = lastfmGenre ?: official.genre),
        )
        // O arquivo já tem exatamente essas tags (e já tem capa): não troca nada,
        // só avisa e mostra a capa encontrada ao lado da dela, para comparar.
        if (beforeAuto == null && current.tags == current.original && merged == current.original &&
            current.cover == CoverState.Original && current.originalCover != null
        ) {
            _state.update { it.copy(autoTag = AutoTagState.AlreadyCorrect(candidate, others, coverBytes)) }
            return
        }
        if (beforeAuto == null) beforeAuto = current.tags to current.cover
        val base = beforeAuto!!.first
        _state.update {
            it.copy(
                tags = mergeSuggestion(base, official.copy(genre = lastfmGenre ?: official.genre)),
                cover = coverBytes?.let { bytes -> CoverState.New(bytes) } ?: beforeAuto!!.second,
                autoTag = AutoTagState.Applied(candidate, others),
            )
        }
    }

    private fun AutoTagState.toAppliedOrIdle(): AutoTagState = when (this) {
        is AutoTagState.Choosing -> {
            val chosen = candidates.firstOrNull()
            if (chosen != null) AutoTagState.Applied(chosen, candidates.drop(1)) else AutoTagState.Idle
        }
        else -> this
    }

    // ---- Gravar -----------------------------------------------------------

    fun save() {
        val current = _state.value
        if (current.saving || current.loading || current.loadFailed) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val cover = when (val c = current.cover) {
                CoverState.Original -> CoverEdit.Keep
                CoverState.Removed -> CoverEdit.Remove
                is CoverState.New -> CoverEdit.Replace(c.data)
            }
            when (val result = tagsRepository.write(uri, current.tags, cover)) {
                TagsWriteResult.Success -> {
                    library.notifyChanged(uri)
                    player.updateTrackInfo(
                        uri = uri,
                        title = current.tags.title.ifBlank { current.original.title },
                        artist = current.tags.artist.ifBlank { null },
                        album = current.tags.album.ifBlank { null },
                    )
                    beforeAuto = null
                    _state.update {
                        it.copy(
                            saving = false,
                            original = current.tags,
                            tags = current.tags,
                            originalCover = current.shownCover,
                            cover = CoverState.Original,
                            autoTag = AutoTagState.Idle,
                        )
                    }
                    _events.send(TagEditorEvent.Saved)
                }
                is TagsWriteResult.NeedsPermission -> {
                    _state.update { it.copy(saving = false) }
                    _events.send(TagEditorEvent.AskWritePermission(result.request))
                }
                TagsWriteResult.NeedsStoragePermission -> {
                    _state.update { it.copy(saving = false) }
                    _events.send(TagEditorEvent.AskStoragePermission)
                }
                TagsWriteResult.Failed -> {
                    _state.update { it.copy(saving = false) }
                    _events.send(TagEditorEvent.SaveFailed)
                }
            }
        }
    }

    suspend fun remoteCover(url: String, sizePx: Int): Bitmap? = deezer.image(url, sizePx)
}

/**
 * Junta as tags oficiais com o que o usuário já tem: o que veio da fonte
 * oficial substitui; o que a fonte não tem (letra, compositor…) fica como estava.
 */
internal fun mergeSuggestion(current: TrackTags, official: TrackTags): TrackTags {
    fun pick(officialValue: String, currentValue: String) = officialValue.ifBlank { currentValue }
    return TrackTags(
        title = pick(official.title, current.title),
        artist = pick(official.artist, current.artist),
        album = pick(official.album, current.album),
        albumArtist = pick(official.albumArtist, current.albumArtist),
        year = pick(official.year, current.year),
        genre = pick(official.genre, current.genre),
        trackNumber = pick(official.trackNumber, current.trackNumber),
        trackTotal = if (official.trackNumber.isNotBlank()) official.trackTotal else current.trackTotal,
        discNumber = pick(official.discNumber, current.discNumber),
        discTotal = if (official.discNumber.isNotBlank()) official.discTotal else current.discTotal,
        composer = pick(official.composer, current.composer),
        lyrics = pick(official.lyrics, current.lyrics),
    )
}
