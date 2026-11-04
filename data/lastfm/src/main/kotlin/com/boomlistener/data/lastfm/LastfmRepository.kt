// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

import android.content.Context

/** O que o Last.fm sabe de um artista. */
data class ArtistBio(
    /** Resumo (um ou dois parágrafos). */
    val summary: String,
    /** Texto completo, quando houver mais que o resumo. */
    val content: String?,
    val tags: List<String>,
    val listeners: Long,
    val url: String?,
)

sealed interface BioResult {
    data class Found(val bio: ArtistBio) : BioResult
    data object NotFound : BioResult
    data object Failed : BioResult
}

interface LastfmRepository {
    /** Biografia no idioma [language] ("pt", "en"…), caindo para inglês se não houver. */
    suspend fun artistBio(artistName: String, language: String): BioResult

    /**
     * Tags dos ouvintes para uma música, da mais usada para a menos (vazio se
     * não achar ou sem internet). Sem tags na música, usa as do artista.
     */
    suspend fun trackTags(artistName: String, title: String): List<String> = emptyList()

    /** Testa uma chave própria no Last.fm (uma consulta). */
    suspend fun checkKey(key: String): KeyCheck = KeyCheck.Failed
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object LastfmData {
    @Volatile
    private var instance: LastfmRepository? = null

    @Volatile
    private var store: LastfmKeyStore? = null

    /** Onde fica a chave própria do Last.fm (vazia = servidor do Boom). */
    fun keyStore(context: Context): LastfmKeyStore =
        store ?: synchronized(this) {
            store ?: FileLastfmKeyStore(context.applicationContext).also { store = it }
        }

    fun repository(context: Context): LastfmRepository =
        instance ?: synchronized(this) {
            instance ?: ProxyLastfmRepository(context.applicationContext, keyStore(context)).also { instance = it }
        }
}
