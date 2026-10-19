// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import android.content.Context
import android.content.IntentSender
import android.graphics.Bitmap
import com.boomlistener.core.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Acesso às músicas do aparelho. O resto do app só enxerga esta interface. */
interface LibraryRepository {

    /** Todas as músicas do aparelho, em ordem alfabética de título. */
    suspend fun getTracks(): List<Track>

    /**
     * A capa de uma música em tamanho próximo de [sizePx], ou null se ela não
     * tiver capa. Guarda em cache na memória.
     */
    suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap?

    /**
     * Muda (soma 1) toda vez que as músicas do aparelho mudam: tags editadas,
     * música nova baixada ou apagada, ou o filtro da biblioteca trocado.
     * Quem mostra músicas ou capas recarrega ao mudar.
     */
    val version: StateFlow<Int> get() = NoChanges

    /** Avisa que [trackUri] mudou: esquece a capa guardada e sobe [version]. */
    fun notifyChanged(trackUri: String) {}

    /** O que entra na biblioteca: duração mínima e pastas. */
    val filter: StateFlow<LibraryFilter> get() = NoFilter

    /** Salva o filtro e recarrega quem mostra músicas. */
    fun setFilter(filter: LibraryFilter) {}

    /** Todas as pastas com música no aparelho (sem aplicar o filtro). */
    suspend fun folders(): List<MusicFolder> = emptyList()

    /**
     * Apaga [uris] do aparelho. No Android 10+ o próprio sistema pede
     * confirmação ([DeleteResult.NeedsConfirmation]: abrir o [IntentSender] e,
     * se a pessoa aceitar, chamar [delete] de novo no Android 10). Sem permissão
     * nenhuma no manifesto, como na gravação de tags.
     */
    suspend fun delete(uris: List<String>): DeleteResult = DeleteResult.Failed
}

sealed interface DeleteResult {
    data object Done : DeleteResult
    /** O sistema precisa perguntar à pessoa (Android 10+). */
    data class NeedsConfirmation(val intentSender: IntentSender, val retryAfter: Boolean) : DeleteResult
    /** Android 8 e 9 sem a permissão de gravar no armazenamento. */
    data object NeedsPermission : DeleteResult
    data object Failed : DeleteResult
}

private val NoChanges: StateFlow<Int> = MutableStateFlow(0).asStateFlow()
private val NoFilter: StateFlow<LibraryFilter> = MutableStateFlow(LibraryFilter()).asStateFlow()

/**
 * Ponto de acesso provisório enquanto o app não tem injeção de dependências
 * (Hilt). Sempre devolve a mesma instância, para o cache de capas valer no app todo.
 */
object LibraryData {
    @Volatile
    private var instance: LibraryRepository? = null

    fun repository(context: Context): LibraryRepository =
        instance ?: synchronized(this) {
            instance ?: MediaStoreLibraryRepository(context.applicationContext).also { instance = it }
        }
}
