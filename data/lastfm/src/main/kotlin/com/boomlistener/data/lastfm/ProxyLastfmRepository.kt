// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

import com.boomlistener.core.common.OfflineMode
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000

/** Frase usada para testar uma chave: um artista que o Last.fm conhece. */
private val KEY_TEST_QUERY = lastfmQuery(listOf("method" to "artist.getinfo", "artist" to "Cher"))

/**
 * Consulta o Last.fm pelo servidor do Boom (proxy/worker.js, que acrescenta a
 * chave do projeto) ou, se a pessoa cadastrou a própria chave, direto nele.
 * A chave do projeto nunca fica no APK (ver CLAUDE.md, Segurança).
 */
internal class ProxyLastfmRepository(
    context: Context,
    private val keyStore: LastfmKeyStore,
) : LastfmRepository {
    private val cacheDir = File(context.cacheDir, "lastfm").apply { mkdirs() }

    override suspend fun artistBio(artistName: String, language: String): BioResult = withContext(Dispatchers.IO) {
        try {
            val localized = fetchBio(artistName, language)
            when {
                localized != null && localized.summary.isNotBlank() -> BioResult.Found(localized)
                language != "en" -> fetchBio(artistName, "en")
                    ?.takeIf { it.summary.isNotBlank() }
                    ?.let { BioResult.Found(it) }
                    ?: BioResult.NotFound
                else -> BioResult.NotFound
            }
        } catch (_: Exception) {
            BioResult.Failed
        }
    }

    override suspend fun trackTags(artistName: String, title: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val track = getJson(
                lastfmQuery(
                    listOf("method" to "track.getinfo", "autocorrect" to "1", "artist" to artistName, "track" to title),
                ),
            )
            val fromTrack = tagNames(track.optJSONObject("track")?.optJSONObject("toptags"))
            if (fromTrack.isNotEmpty()) return@withContext fromTrack
            val artistTags = getJson(
                lastfmQuery(listOf("method" to "artist.gettoptags", "autocorrect" to "1", "artist" to artistName)),
            )
            tagNames(artistTags.optJSONObject("toptags"))
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun checkKey(key: String): KeyCheck = withContext(Dispatchers.IO) {
        val clean = normalizeLastfmKey(key) ?: return@withContext KeyCheck.Invalid
        try {
            when (classifyReply(fetch(lastfmRequestUrl(clean, KEY_TEST_QUERY)).second)) {
                LastfmReply.Ok, LastfmReply.NotFound -> KeyCheck.Valid
                LastfmReply.KeyRejected -> KeyCheck.Invalid
                LastfmReply.OtherError -> KeyCheck.Failed
            }
        } catch (_: Exception) {
            KeyCheck.Failed
        }
    }

    private fun tagNames(container: JSONObject?): List<String> {
        val array = container?.optJSONArray("tag") ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("name") }
            .filter { it.isNotBlank() }
    }

    private fun fetchBio(artistName: String, language: String): ArtistBio? {
        val json = getJson(
            lastfmQuery(
                listOf("method" to "artist.getinfo", "autocorrect" to "1", "artist" to artistName, "lang" to language),
            ),
        )
        // Artista desconhecido: o Last.fm responde {"error": 6, ...}.
        if (json.has("error")) return null
        return parseArtistInfo(json)
    }

    /**
     * Resposta do Last.fm para [query] (sem a chave), do cache se ainda valer.
     * Erro de chave ou outro erro vira exceção (e não vai para o cache); só
     * "não achei" (erro 6) volta como JSON.
     */
    private fun getJson(query: String): JSONObject {
        val file = File(cacheDir, sha1(lastfmCacheId(query)) + ".json")
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < CACHE_TTL_MS) {
            runCatching { return JSONObject(file.readText()) }
        }
        val (code, json, body) = fetch(lastfmRequestUrl(keyStore.ownKey.value, query))
        when (classifyReply(json)) {
            LastfmReply.Ok -> if (code in 200..299) runCatching { file.writeText(body) } else error("Last.fm respondeu $code")
            LastfmReply.NotFound -> Unit
            LastfmReply.KeyRejected -> {
                // Só avisa quando a chave é da pessoa; a do projeto é problema do mantenedor.
                if (keyStore.ownKey.value != null) keyStore.markRejected()
                throw LastfmKeyException("Last.fm recusou a chave")
            }
            LastfmReply.OtherError -> error("Last.fm respondeu erro ${json.optInt("error")}")
        }
        return json
    }

    private fun fetch(url: String): Triple<Int, JSONObject, String> {
        OfflineMode.requireOnline()
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: error("Last.fm sem resposta ($code)")
            return Triple(code, JSONObject(body), body)
        } finally {
            connection.disconnect()
        }
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

/** Lê a resposta de artist.getinfo. */
internal fun parseArtistInfo(json: JSONObject): ArtistBio? {
    val artist = json.optJSONObject("artist") ?: return null
    val bio = artist.optJSONObject("bio")
    val summary = cleanBio(bio?.optString("summary").orEmpty())
    val content = cleanBio(bio?.optString("content").orEmpty()).takeIf { it.length > summary.length + 20 }
    val tagsArray = artist.optJSONObject("tags")?.optJSONArray("tag")
    val tags = (0 until (tagsArray?.length() ?: 0)).mapNotNull { tagsArray?.optJSONObject(it)?.optString("name") }
        .filter { it.isNotBlank() }
    return ArtistBio(
        summary = summary,
        content = content,
        tags = tags,
        listeners = artist.optJSONObject("stats")?.optString("listeners")?.toLongOrNull() ?: 0,
        url = artist.optString("url").takeIf { it.isNotBlank() },
    )
}

private val READ_MORE = Regex("""<a href="https?://www\.last\.fm[^"]*">.*?</a>\.?""", RegexOption.DOT_MATCHES_ALL)
private val TAG = Regex("<[^>]+>")
private val LICENSE_NOTE = Regex("""User-contributed text is available under the Creative Commons.*$""", RegexOption.DOT_MATCHES_ALL)

/**
 * Texto da bio sem HTML, sem o link "Read more on Last.fm" e sem a nota de
 * licença do fim (o crédito ao Last.fm aparece na tela).
 */
internal fun cleanBio(raw: String): String =
    raw.replace(READ_MORE, "")
        .replace(LICENSE_NOTE, "")
        .replace(TAG, "")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .trim()
