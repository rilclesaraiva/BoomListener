// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.musicbrainz

import android.content.Context
import android.os.SystemClock
import com.boomlistener.core.model.ArtistProfile
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.Release
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest

private const val API_URL = "https://musicbrainz.org/ws/2"
private const val USER_AGENT = "BoomListener ( https://github.com/rilclesaraiva/BoomListener )"

// O MusicBrainz pede no máximo 1 consulta por segundo; com folga.
private const val MIN_GAP_MS = 1_100L

// Respostas guardadas por uma semana (discografia e buscas mudam pouco).
private const val JSON_TTL_MS = 7L * 24 * 60 * 60 * 1000

internal class HttpMusicBrainzRepository(context: Context) : MusicBrainzRepository {

    private val jsonCache = File(context.cacheDir, "musicbrainz").apply { mkdirs() }
    private val coverCache = File(context.cacheDir, "coverart").apply { mkdirs() }
    private val gate = Mutex()
    private var lastRequestAt = 0L

    override suspend fun searchRecordings(title: String, artist: String?): MbSearchResult = withContext(Dispatchers.IO) {
        val query = buildString {
            append("recording:").append(luceneQuote(title))
            if (!artist.isNullOrBlank()) append(" AND artist:").append(luceneQuote(primaryName(artist)))
        }
        val json = runCatching { getJson("$API_URL/recording?query=${encode(query)}&limit=15&fmt=json") }
            .getOrElse { return@withContext MbSearchResult.Failed }
        val recordings = json.optJSONArray("recordings").objects().mapNotNull(::toRecording)
        if (recordings.isEmpty()) MbSearchResult.NotFound else MbSearchResult.Found(recordings)
    }

    override suspend fun coverBytes(recording: MbRecording): ByteArray? = withContext(Dispatchers.IO) {
        val urls = listOfNotNull(
            recording.releaseId?.let { "$COVER_ART_URL/release/$it/front-500" },
            recording.releaseGroupId?.let { "$COVER_ART_URL/release-group/$it/front-500" },
        )
        urls.firstNotNullOfOrNull { download(it) }
    }

    override suspend fun artistProfile(artistName: String, localAlbums: List<String>): MbArtistResult =
        withContext(Dispatchers.IO) {
            val name = primaryName(artistName)
            if (name.isBlank()) return@withContext MbArtistResult.NotFound
            try {
                val search = getJson("$API_URL/artist?query=${encode("artist:" + luceneQuote(name))}&limit=10&fmt=json")
                val hits = search.optJSONArray("artists").objects().map { artist ->
                    MbArtistHit(
                        id = artist.optString("id"),
                        name = artist.optString("name"),
                        score = artist.optInt("score"),
                        aliases = artist.optJSONArray("aliases").objects().mapNotNull { it.str("name") },
                    )
                }
                val matches = matchingArtists(name, hits)
                if (matches.isEmpty()) return@withContext MbArtistResult.NotFound
                // Homônimos: olha a discografia dos 3 mais prováveis e fica com a que
                // tem mais álbuns do aparelho (empate: o de melhor nota na busca).
                val options = if (matches.size == 1 || localAlbums.isEmpty()) {
                    listOf(matches.first() to releasesOf(matches.first().id))
                } else {
                    matches.take(3).map { it to releasesOf(it.id) }
                }
                val (chosen, releases) = options.maxByOrNull { (_, list) -> albumsInCommon(localAlbums, list.map { it.title }) }
                    ?: options.first()
                MbArtistResult.Found(
                    ArtistProfile(
                        artist = ExternalArtist(
                            id = musicBrainzKey(chosen.id),
                            name = chosen.name,
                            pictureUrl = null,
                            fans = 0,
                            albumCount = releases.size,
                            link = "https://musicbrainz.org/artist/${chosen.id}",
                        ),
                        topTracks = emptyList(),
                        releases = releases,
                        related = emptyList(),
                    ),
                )
            } catch (_: Exception) {
                MbArtistResult.Failed
            }
        }

    /** Discografia (álbuns, EPs, singles e coletâneas), do mais novo para o mais antigo. */
    private suspend fun releasesOf(artistId: String): List<Release> {
        val json = getJson("$API_URL/release-group?artist=$artistId&limit=100&fmt=json")
        return json.optJSONArray("release-groups").objects().mapNotNull { group ->
            val id = group.str("id") ?: return@mapNotNull null
            val type = releaseTypeOf(
                group.str("primary-type"),
                group.optJSONArray("secondary-types").strings(),
            ) ?: return@mapNotNull null
            Release(
                id = musicBrainzKey(id),
                title = group.optString("title"),
                type = type,
                releaseDate = group.str("first-release-date"),
                coverUrl = "$COVER_ART_URL/release-group/$id/front-250",
                link = "https://musicbrainz.org/release-group/$id",
            )
        }.sortedByDescending { it.releaseDate.orEmpty() }
    }

    private fun toRecording(json: JSONObject): MbRecording? {
        val id = json.str("id") ?: return null
        val title = json.str("title") ?: return null
        val artist = json.optJSONArray("artist-credit").objects()
            .joinToString("") { it.optString("name") + it.optString("joinphrase") }
            .trim()
        val options = json.optJSONArray("releases").objects().mapNotNull { release ->
            val releaseId = release.str("id") ?: return@mapNotNull null
            val group = release.optJSONObject("release-group")
            val medium = release.optJSONArray("media").objects().firstOrNull()
            MbReleaseOption(
                id = releaseId,
                groupId = group?.str("id"),
                title = release.optString("title"),
                date = release.optString("date").takeIf { it != "null" }.orEmpty(),
                official = release.optString("status").equals("Official", ignoreCase = true),
                primaryType = group?.str("primary-type"),
                secondaryTypes = group?.optJSONArray("secondary-types").strings(),
                trackNumber = medium?.optJSONArray("track").objects().firstOrNull()?.str("number").orEmpty(),
                trackTotal = medium?.optInt("track-count") ?: 0,
                discNumber = medium?.optInt("position") ?: 0,
                discTotal = release.optJSONArray("media").objects().size.takeIf { it > 1 } ?: 0,
            )
        }
        val release = bestRelease(options)
        return MbRecording(
            recordingId = id,
            releaseId = release?.id,
            releaseGroupId = release?.groupId,
            title = title,
            artist = artist,
            album = release?.title.orEmpty(),
            durationMs = json.optLong("length"),
            date = release?.date?.ifBlank { null } ?: json.optString("first-release-date").takeIf { it != "null" }.orEmpty(),
            trackNumber = release?.trackNumber?.takeIf { n -> n.all(Char::isDigit) }.orEmpty(),
            trackTotal = release?.trackTotal?.takeIf { it > 0 }?.toString().orEmpty(),
            discNumber = release?.takeIf { it.discTotal > 1 }?.discNumber?.takeIf { it > 0 }?.toString().orEmpty(),
            discTotal = release?.discTotal?.takeIf { it > 1 }?.toString().orEmpty(),
        )
    }

    /** GET com cache em arquivo e o ritmo pedido pelo MusicBrainz. Erros viram exceção. */
    private suspend fun getJson(url: String): JSONObject {
        val file = File(jsonCache, sha1(url) + ".json")
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < JSON_TTL_MS) {
            runCatching { return JSONObject(file.readText()) }
        }
        repeat(2) { attempt ->
            val body = gate.withLock {
                val wait = lastRequestAt + MIN_GAP_MS - SystemClock.elapsedRealtime()
                if (wait > 0) delay(wait)
                try {
                    fetch(url)
                } finally {
                    lastRequestAt = SystemClock.elapsedRealtime()
                }
            }
            if (body != null) {
                runCatching { file.writeText(body) }
                return JSONObject(body)
            }
            // 503: passou do ritmo (ou o servidor está cheio); espera e tenta uma vez mais.
            if (attempt == 0) delay(1_500)
        }
        error("MusicBrainz ocupado")
    }

    /** O corpo da resposta; null em 503. */
    private fun fetch(url: String): String? {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        try {
            val code = connection.responseCode
            if (code == 503) return null
            check(code == HttpURLConnection.HTTP_OK) { "MusicBrainz $code" }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Capa do Cover Art Archive (que redireciona para o archive.org); null se não houver. */
    private fun download(url: String): ByteArray? = runCatching {
        val file = File(coverCache, sha1(url))
        if (!file.exists()) {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 20_000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
            } catch (_: FileNotFoundException) {
                return@runCatching null
            } finally {
                connection.disconnect()
            }
        }
        file.readBytes()
    }.getOrNull()

    private fun encode(text: String) = URLEncoder.encode(text, "UTF-8")

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotBlank() } }

private fun JSONObject.str(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }
