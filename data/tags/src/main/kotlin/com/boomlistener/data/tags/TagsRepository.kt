// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.tags

import android.content.Context
import android.content.IntentSender
import com.boomlistener.core.model.TrackTags

sealed interface TagsReadResult {
    /**
     * [cover] é a capa embutida no arquivo (bytes da imagem), se houver.
     * [durationMs] é 0 quando não deu para ler.
     */
    class Found(val tags: TrackTags, val cover: ByteArray?, val durationMs: Long = 0) : TagsReadResult
    data object Failed : TagsReadResult
}

/** O que fazer com a capa embutida ao gravar. */
sealed interface CoverEdit {
    data object Keep : CoverEdit
    data object Remove : CoverEdit
    class Replace(val data: ByteArray) : CoverEdit
}

sealed interface TagsWriteResult {
    data object Success : TagsWriteResult

    /**
     * Android 10+: o arquivo é de outro app (ou foi copiado pelo computador) e
     * o Android precisa perguntar ao usuário. Lançar [request] e, se ele
     * permitir, chamar a gravação de novo.
     */
    class NeedsPermission(val request: IntentSender) : TagsWriteResult

    /** Android 8 e 9: falta a permissão de gravar no armazenamento. */
    data object NeedsStoragePermission : TagsWriteResult

    data object Failed : TagsWriteResult
}

/**
 * Lê e grava as tags dentro dos arquivos de música (MP3, FLAC, M4A, OGG,
 * OPUS, WAV…). O resto do app só vê esta interface.
 */
interface TagsRepository {
    suspend fun read(uri: String): TagsReadResult

    /** Só a letra guardada no arquivo (para o player), ou null. */
    suspend fun readLyrics(uri: String): String?

    suspend fun write(uri: String, tags: TrackTags, cover: CoverEdit): TagsWriteResult
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object TagsData {
    @Volatile
    private var instance: TagsRepository? = null

    fun repository(context: Context): TagsRepository =
        instance ?: synchronized(this) {
            instance ?: TagLibTagsRepository(context.applicationContext).also { instance = it }
        }
}
