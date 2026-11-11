// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boomlistener.data.library.LibraryRepository
import com.boomlistener.data.tags.TagGap
import com.boomlistener.data.tags.TagsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Quantos arquivos são lidos ao mesmo tempo no escaneamento. */
private const val PARALLEL_READS = 8

/** De quantos em quantos lotes a lista de resultados é refeita durante o escaneamento. */
private const val PUBLISH_EVERY_BATCHES = 20

/** Uma música do resultado do escaneamento. */
internal data class AuditSong(val uri: String, val title: String, val artist: String?)

internal enum class ScanPhase { Idle, Scanning, Done }

internal data class TagAuditState(
    val phase: ScanPhase = ScanPhase.Idle,
    val scanned: Int = 0,
    val total: Int = 0,
    /** Arquivos que o TagLib não conseguiu abrir (ficam fora da conta). */
    val unreadable: Int = 0,
    /** Para cada coisa que falta, as músicas a que ela falta (só as que têm alguma). */
    val found: Map<TagGap, List<AuditSong>> = emptyMap(),
)

/** Agrupa as músicas pelo que falta a cada uma (na ordem de [TagGap]); o que não falta a ninguém fica de fora. */
internal fun buildAudit(songs: List<AuditSong>, scans: Map<String, Set<TagGap>>): Map<TagGap, List<AuditSong>> =
    TagGap.entries
        .associateWith { gap -> songs.filter { gap in scans[it.uri].orEmpty() } }
        .filterValues { it.isNotEmpty() }

/**
 * Escaneia a biblioteca lendo as tags de cada arquivo e junta o que falta
 * (artista, álbum, ano, gênero, faixa, capa, letra…). Fica só na memória: sair
 * da tela descarta o resultado, e escanear de novo leva alguns segundos.
 */
internal class TagAuditViewModel(
    private val tags: TagsRepository,
    private val library: LibraryRepository,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _state = MutableStateFlow(TagAuditState())
    val state: StateFlow<TagAuditState> = _state.asStateFlow()

    private var songs: List<AuditSong> = emptyList()
    private val scans = HashMap<String, Set<TagGap>>()
    private val edited = HashSet<String>()
    private var job: Job? = null

    fun scan() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val all = library.getTracks().map { AuditSong(it.uri, it.title, it.artist) }
            songs = all
            scans.clear()
            edited.clear()
            _state.value = TagAuditState(phase = ScanPhase.Scanning, total = all.size)
            var unreadable = 0
            var batches = 0
            all.chunked(PARALLEL_READS).forEach { batch ->
                val results = batch.map { song -> async(io) { song.uri to tags.scan(song.uri) } }.awaitAll()
                results.forEach { (uri, gaps) -> if (gaps == null) unreadable++ else scans[uri] = gaps }
                batches++
                val scanned = _state.value.scanned + batch.size
                _state.value = if (batches % PUBLISH_EVERY_BATCHES == 0) {
                    _state.value.copy(scanned = scanned, unreadable = unreadable, found = buildAudit(all, scans))
                } else {
                    _state.value.copy(scanned = scanned, unreadable = unreadable)
                }
            }
            _state.value = _state.value.copy(phase = ScanPhase.Done, found = buildAudit(all, scans))
        }
    }

    /** Guarda que estas músicas foram abertas no editor: ao voltar, só elas são lidas de novo. */
    fun markEdited(uris: Collection<String>) {
        edited += uris
    }

    /** Lê de novo as músicas abertas no editor e atualiza o resultado. */
    fun refreshEdited() {
        if (_state.value.phase != ScanPhase.Done || edited.isEmpty() || job?.isActive == true) return
        val targets = edited.toList()
        edited.clear()
        job = viewModelScope.launch {
            targets.chunked(PARALLEL_READS).forEach { batch ->
                batch.map { uri -> async(io) { uri to tags.scan(uri) } }.awaitAll()
                    .forEach { (uri, gaps) -> if (gaps != null) scans[uri] = gaps }
            }
            _state.value = _state.value.copy(found = buildAudit(songs, scans))
        }
    }
}
