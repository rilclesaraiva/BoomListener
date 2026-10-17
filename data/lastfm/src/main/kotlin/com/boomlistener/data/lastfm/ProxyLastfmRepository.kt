// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

/**
 * Proxy na Cloudflare (proxy/worker.js) que acrescenta a chave do Last.fm.
 * A chave nunca fica no APK (ver CLAUDE.md, Segurança).
 */
private const val PROXY_URL = "https://boomlistener-lastfm.saraivarilcle.workers.dev/lastfm"
private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000

internal class ProxyLastfmRepository(context: Context) : LastfmRepository {
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
            val artist = URLEncoder.encode(artistName, "UTF-8")
            val track = getJson(
                "$PROXY_URL?method=track.getinfo&autocorrect=1&artist=$artist&track=" + URLEncoder.encode(title, "UTF-8"),
            )
            val fromTrack = tagNames(track.optJSONObject("track")?.optJSONObject("toptags"))
            if (fromTrack.isNotEmpty()) return@withContext fromTrack
            val artistTags = getJson("$PROXY_URL?method=artist.gettoptags&autocorrect=1&artist=$artist")
            tagNames(artistTags.optJSONObject("toptags"))
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun tagNames(container: JSONObject?): List<String> {
        val array = container?.optJSONArray("tag") ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("name") }
            .filter { it.isNotBlank() }
    }

    private fun fetchBio(artistName: String, language: String): ArtistBio? {
        val url = PROXY_URL + "?method=artist.getinfo&autocorrect=1" +
            "&artist=" + URLEncoder.encode(artistName, "UTF-8") +
            "&lang=" + URLEncoder.encode(language, "UTF-8")
        val json = getJson(url)
        // Artista desconhecido: o Last.fm responde {"error": 6, ...}.
        if (json.has("error")) return null
        return parseArtistInfo(json)
    }

    private fun getJson(url: String): JSONObject {
        val file = File(cacheDir, sha1(url) + ".json")
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < CACHE_TTL_MS) {
            runCatching { return JSONObject(file.readText()) }
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: error("Last.fm sem resposta ($code)")
            val json = JSONObject(body)
            if (code in 200..299) runCatching { file.writeText(body) }
            else if (!json.has("error")) error("Last.fm respondeu $code")
            return json
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
