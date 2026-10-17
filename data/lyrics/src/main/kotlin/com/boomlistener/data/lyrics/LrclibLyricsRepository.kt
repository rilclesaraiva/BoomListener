// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lyrics

import android.content.Context
import com.boomlistener.core.model.Lyrics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import kotlin.math.abs

private const val BASE_URL = "https://lrclib.net/api"
private const val SOURCE = "LRCLIB"
// "Não encontrada" fica guardado por uma semana; depois tenta de novo
// (alguém pode ter publicado a letra no LRCLIB nesse meio-tempo).
private const val NOT_FOUND_TTL_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Letras do LRCLIB (gratuito, sem chave, com letras sincronizadas).
 * Cada resultado vira um arquivo em cache no aparelho, então a mesma letra
 * nunca é buscada duas vezes.
 */
internal class LrclibLyricsRepository(context: Context) : LyricsRepository {

    private val cacheDir = File(context.cacheDir, "lyrics").apply { mkdirs() }
    private val userAgent = "BoomListener (https://github.com/rilclesaraiva/BoomListener)"

    override suspend fun getLyrics(query: LyricsQuery): LyricsResult = withContext(Dispatchers.IO) {
        val cacheFile = File(cacheDir, cacheKey(query) + ".json")
        readCache(cacheFile)?.let { return@withContext it }

        // Cada etapa pode falhar sozinha (o LRCLIB às vezes demora numa delas).
        // Só é "sem conexão" se NENHUMA etapa respondeu; se alguma respondeu e
        // nenhuma achou, a música simplesmente não tem letra lá.
        var answered = false
        fun attempt(step: () -> Lyrics?): Lyrics? =
            try {
                step().also { answered = true }
            } catch (_: Exception) {
                null
            }

        val canSearchExact = query.artist != null && query.album != null && query.durationMs > 0
        val result = (if (canSearchExact) attempt { fetchExact(query) } else null)
            ?: attempt { searchBest(query, byFields = true) }
            ?: attempt { searchBest(query, byFields = false) }
        if (result == null && !answered) return@withContext LyricsResult.Failed

        val final = result?.let { LyricsResult.Found(it) } ?: LyricsResult.NotFound
        writeCache(cacheFile, final)
        final
    }

    /**
     * Busca exata (título + artista + álbum + duração), só no banco do LRCLIB
     * ("get-cached"): responde na hora, sem ir procurar em outros sites.
     */
    private fun fetchExact(query: LyricsQuery): Lyrics? {
        val artist = query.artist ?: return null
        val album = query.album ?: return null
        if (query.durationMs <= 0) return null
        val url = "$BASE_URL/get-cached?" + params(
            "track_name" to query.title,
            "artist_name" to artist,
            "album_name" to album,
            "duration" to (query.durationMs / 1000).toString(),
        )
        val body = request(url) ?: return null
        return toLyrics(JSONObject(body))
    }

    /**
     * Busca aberta: entre os resultados, o de duração mais parecida, preferindo
     * os sincronizados. [byFields] procura por título e artista separados; sem
     * ele, procura o texto livre "artista título" (pega grafias diferentes).
     */
    private fun searchBest(query: LyricsQuery, byFields: Boolean): Lyrics? {
        val url = "$BASE_URL/search?" + if (byFields) {
            params("track_name" to query.title, "artist_name" to query.artist)
        } else {
            params("q" to listOfNotNull(query.artist, query.title).joinToString(" "))
        }
        val body = request(url) ?: return null
        val results = JSONArray(body)
        val candidates = (0 until results.length()).map { results.getJSONObject(it) }
        val best = candidates
            .filter { it.optString("syncedLyrics").isNotBlank() || it.optString("plainLyrics").isNotBlank() }
            .minByOrNull { candidate ->
                val durationGap = if (query.durationMs > 0) {
                    abs(candidate.optDouble("duration", 0.0) * 1000 - query.durationMs)
                } else {
                    0.0
                }
                val syncedBonus = if (candidate.optString("syncedLyrics").isNotBlank()) 0.0 else 5_000.0
                durationGap + syncedBonus
            } ?: return null
        // Duração muito diferente (mais de 10 s): provavelmente outra versão.
        if (query.durationMs > 0 && abs(best.optDouble("duration", 0.0) * 1000 - query.durationMs) > 10_000) {
            return null
        }
        return toLyrics(best)
    }

    private fun toLyrics(json: JSONObject): Lyrics? {
        val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() && it != "null" }
        if (synced != null) {
            val lines = parseLrc(synced)
            if (lines.isNotEmpty()) return Lyrics(lines, synced = true, source = SOURCE)
        }
        val plain = json.optString("plainLyrics").takeIf { it.isNotBlank() && it != "null" } ?: return null
        return Lyrics(parsePlain(plain), synced = false, source = SOURCE)
    }

    /** null = 404 (não encontrada). Outros erros viram exceção. */
    private fun request(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8_000
            readTimeout = 12_000
            setRequestProperty("User-Agent", userAgent)
        }
        try {
            return when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader().use { it.readText() }
                HttpURLConnection.HTTP_NOT_FOUND -> null
                else -> error("LRCLIB respondeu $code")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun params(vararg pairs: Pair<String, String?>): String =
        pairs.filter { it.second != null }.joinToString("&") { (key, value) ->
            key + "=" + URLEncoder.encode(value, "UTF-8")
        }

    private fun cacheKey(query: LyricsQuery): String {
        val raw = listOf(query.title, query.artist, query.album, (query.durationMs / 1000).toString())
            .joinToString("|") { it.orEmpty().lowercase() }
        return MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }

    private fun readCache(file: File): LyricsResult? {
        if (!file.exists()) return null
        return runCatching {
            val json = JSONObject(file.readText())
            if (!json.optBoolean("found")) {
                val age = System.currentTimeMillis() - file.lastModified()
                if (age > NOT_FOUND_TTL_MS) null else LyricsResult.NotFound
            } else {
                val synced = json.getBoolean("synced")
                val text = json.getString("text")
                LyricsResult.Found(
                    Lyrics(
                        lines = if (synced) parseLrc(text) else parsePlain(text),
                        synced = synced,
                        source = json.optString("source", SOURCE),
                    ),
                )
            }
        }.getOrNull()
    }

    private fun writeCache(file: File, result: LyricsResult) {
        runCatching {
            val json = JSONObject()
            when (result) {
                is LyricsResult.Found -> {
                    val lyrics = result.lyrics
                    json.put("found", true)
                    json.put("synced", lyrics.synced)
                    json.put("source", lyrics.source)
                    json.put("text", toCacheText(lyrics))
                }
                LyricsResult.NotFound -> json.put("found", false)
                LyricsResult.Failed -> return
            }
            file.writeText(json.toString())
        }
    }
}

/** Letra de volta para texto (LRC se sincronizada) para guardar em cache. */
internal fun toCacheText(lyrics: Lyrics): String =
    if (lyrics.synced) {
        lyrics.lines.joinToString("\n") { line ->
            val minutes = line.timeMs / 60_000
            val seconds = (line.timeMs % 60_000) / 1000
            val millis = line.timeMs % 1000
            "[%02d:%02d.%03d]%s".format(minutes, seconds, millis, line.text)
        }
    } else {
        lyrics.lines.joinToString("\n") { it.text }
    }
