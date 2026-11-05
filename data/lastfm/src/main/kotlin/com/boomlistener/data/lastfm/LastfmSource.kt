// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder

/** Proxy na Cloudflare (proxy/worker.js): acrescenta a chave do projeto, que nunca fica no APK. */
internal const val PROXY_URL = "https://boomlistener-lastfm.saraivarilcle.workers.dev/lastfm"

/** API do Last.fm, usada direto só quando a pessoa cadastrou a própria chave. */
internal const val DIRECT_URL = "https://ws.audioscrobbler.com/2.0/"

/** Códigos de erro do Last.fm: chave inválida (10) e chave suspensa (26). */
private val KEY_ERRORS = setOf(10, 26)

/** Last.fm responde `error 6` quando não conhece o artista ou a música. */
private const val NOT_FOUND_ERROR = 6

/**
 * De onde vêm as consultas ao Last.fm. Sem chave própria (padrão), passam pelo
 * servidor do Boom. Com ela, o aparelho fala direto com o Last.fm, e o servidor
 * do Boom não vê nada.
 *
 * A chave fica só no aparelho, fora do backup (do app e do Android).
 */
interface LastfmKeyStore {
    /** A chave própria, ou `null` quando o app usa o servidor do Boom. */
    val ownKey: StateFlow<String?>

    /** Verdadeiro quando o Last.fm recusou a chave salva (suspensa ou revogada depois). */
    val rejected: StateFlow<Boolean>

    /** Guarda (ou, com `null`, esquece) a chave. Falso se não deu para gravar; nada muda nesse caso. */
    fun setOwnKey(key: String?): Boolean

    /** O Last.fm recusou a chave salva: a tela de Ajustes avisa. */
    fun markRejected()
}

/** Guarda a chave num arquivo de `noBackupFilesDir` (o Android não o copia para a nuvem). */
internal class FileLastfmKeyStore(context: Context) : LastfmKeyStore {
    private val file = File(context.noBackupFilesDir, "lastfm_own_key")
    private val state = MutableStateFlow(read())

    private val rejectedState = MutableStateFlow(false)

    override val ownKey: StateFlow<String?> = state.asStateFlow()
    override val rejected: StateFlow<Boolean> = rejectedState.asStateFlow()

    override fun setOwnKey(key: String?): Boolean {
        val clean = key?.let(::normalizeLastfmKey)
        val saved = runCatching {
            if (clean == null) {
                if (file.exists()) check(file.delete()) { "Não deu para apagar a chave" }
            } else {
                file.writeText(clean)
            }
        }.isSuccess
        if (saved) {
            state.value = clean
            rejectedState.value = false
        }
        return saved
    }

    override fun markRejected() {
        rejectedState.value = true
    }

    private fun read(): String? = runCatching { file.readText() }.getOrNull()?.let(::normalizeLastfmKey)
}

/** A chave do Last.fm tem 32 letras e números; devolve-a limpa, ou `null` se não parece uma. */
fun normalizeLastfmKey(raw: String): String? =
    raw.trim().lowercase().takeIf { KEY_FORMAT.matches(it) }

private val KEY_FORMAT = Regex("[0-9a-z]{32}")

/** Parâmetros já codificados, sem a chave: serve de base para o endereço e para o cache. */
internal fun lastfmQuery(params: List<Pair<String, String>>): String =
    params.joinToString("&") { (name, value) -> name + "=" + URLEncoder.encode(value, "UTF-8") }

/** Endereço do pedido: pelo servidor do Boom, ou direto no Last.fm com a chave da pessoa. */
internal fun lastfmRequestUrl(ownKey: String?, query: String): String =
    if (ownKey == null) "$PROXY_URL?$query" else "$DIRECT_URL?$query&api_key=$ownKey&format=json"

/** Identifica o pedido no cache: igual nas duas fontes, e nunca contém a chave. */
internal fun lastfmCacheId(query: String): String = "$PROXY_URL?$query"

/** O Last.fm recusou a chave (inválida ou suspensa). */
internal class LastfmKeyException(message: String) : Exception(message)

/** Como a resposta deve ser tratada. */
internal enum class LastfmReply { Ok, NotFound, KeyRejected, OtherError }

internal fun classifyReply(json: JSONObject): LastfmReply {
    if (!json.has("error")) return LastfmReply.Ok
    val code = json.optInt("error", -1)
    return when {
        code == NOT_FOUND_ERROR -> LastfmReply.NotFound
        code in KEY_ERRORS -> LastfmReply.KeyRejected
        else -> LastfmReply.OtherError
    }
}

/** Resultado de testar uma chave. */
sealed interface KeyCheck {
    data object Valid : KeyCheck
    data object Invalid : KeyCheck

    /** Sem internet, modo offline ou o Last.fm fora do ar: não deu para saber. */
    data object Failed : KeyCheck
}
