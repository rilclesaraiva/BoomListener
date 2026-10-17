// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.deezer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.boomlistener.core.model.AlbumDetail
import com.boomlistener.core.model.ArtistProfile
import com.boomlistener.core.model.ExternalArtist
import com.boomlistener.core.model.PopularTrack
import com.boomlistener.core.model.Release
import com.boomlistener.core.model.ReleaseType
import com.boomlistener.core.model.TagSuggestion
import com.boomlistener.core.model.TrackTags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale

private const val API = "https://api.deezer.com"
// Respostas da API ficam guardadas por um dia (discografia muda pouco).
// song.link (Odesli): gratuito, sem chave, até 10 consultas por minuto.
private const val SONG_LINK_API = "https://api.song-link.io/v1-alpha.1/links"
private const val SHARE_CONFIDENCE = 0.6f
private const val JSON_TTL_MS = 24L * 60 * 60 * 1000

internal class HttpDeezerRepository(context: Context) : DeezerRepository {

    private val jsonCache = File(context.cacheDir, "deezer/json").apply { mkdirs() }
    private val imageCache = File(context.cacheDir, "deezer/images").apply { mkdirs() }
    private val memoryImages = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 16).toInt(),
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    override suspend fun artistProfile(artistName: String): ProfileResult = withContext(Dispatchers.IO) {
        try {
            val query = URLEncoder.encode(primaryArtistName(artistName), "UTF-8")
            val search = getJson("$API/search/artist?q=$query&limit=10")
            val candidates = search.optJSONArray("data").objects().map { it.toArtist() }
            val artist = pickArtist(artistName, candidates) ?: return@withContext ProfileResult.NotFound

            coroutineScope {
                val details = async { getJson("$API/artist/${artist.id}").toArtist() }
                val top = async { getJson("$API/artist/${artist.id}/top?limit=10") }
                val albums = async { getJson("$API/artist/${artist.id}/albums?limit=200") }
                val related = async { getJson("$API/artist/${artist.id}/related?limit=12") }
                ProfileResult.Found(
                    ArtistProfile(
                        artist = details.await(),
                        topTracks = top.await().optJSONArray("data").objects().map { it.toPopularTrack() },
                        releases = albums.await().optJSONArray("data").objects()
                            .map { it.toRelease() }
                            .sortedByDescending { it.releaseDate.orEmpty() },
                        related = related.await().optJSONArray("data").objects().map { it.toArtist() },
                    ),
                )
            }
        } catch (_: Exception) {
            ProfileResult.Failed
        }
    }

    override suspend fun artistReleases(artistId: Long): ArtistReleasesResult = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val artist = async { getJson("$API/artist/$artistId").toArtist() }
                val albums = async { getJson("$API/artist/$artistId/albums?limit=200") }
                ArtistReleasesResult.Found(
                    artist = artist.await(),
                    releases = albums.await().optJSONArray("data").objects()
                        .map { it.toRelease() }
                        .sortedByDescending { it.releaseDate.orEmpty() },
                )
            }
        } catch (_: Exception) {
            ArtistReleasesResult.Failed
        }
    }

    override suspend fun album(albumId: Long): AlbumResult = withContext(Dispatchers.IO) {
        try {
            val json = getJson("$API/album/$albumId")
            val release = json.toRelease()
            val tracks = json.optJSONObject("tracks")?.optJSONArray("data").objects().map { track ->
                // As músicas do álbum vêm sem a capa: usam a do próprio álbum.
                track.toPopularTrack().copy(albumTitle = release.title, coverUrl = release.coverUrl)
            }
            AlbumResult.Found(
                AlbumDetail(
                    release = release,
                    artistName = json.optJSONObject("artist")?.str("name").orEmpty(),
                    tracks = tracks,
                ),
            )
        } catch (_: Exception) {
            AlbumResult.Failed
        }
    }

    override suspend fun searchTrack(local: LocalTrackInfo): TrackSearchResult = withContext(Dispatchers.IO) {
        try {
            val title = searchableSongTitle(local.title)
            val artist = local.artist?.let(::primaryArtistName)?.takeIf { it.isNotBlank() }
            // Busca precisa (artista + título); se não vier nada, busca livre.
            val queries = listOfNotNull(
                artist?.let { "artist:\"$it\" track:\"$title\"" },
                listOfNotNull(artist, title).joinToString(" "),
            )
            var ranked = emptyList<TrackCandidate>()
            for (query in queries) {
                val json = getJson("$API/search?q=" + URLEncoder.encode(query, "UTF-8") + "&limit=25")
                val candidates = json.optJSONArray("data").objects().map { it.toCandidate() }
                ranked = rankCandidates(local, candidates)
                if (ranked.isNotEmpty()) break
            }
            if (ranked.isEmpty()) TrackSearchResult.NotFound else TrackSearchResult.Found(ranked)
        } catch (_: Exception) {
            TrackSearchResult.Failed
        }
    }

    override suspend fun trackTags(candidate: TrackCandidate): TrackTagsResult = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val trackJob = async { getJson("$API/track/${candidate.trackId}") }
                val albumJob = async { getJson("$API/album/${candidate.albumId}") }
                val track = trackJob.await()
                val album = albumJob.await()
                TrackTagsResult.Found(tagSuggestionOf(track, album, candidate))
            }
        } catch (_: Exception) {
            TrackTagsResult.Failed
        }
    }

    override suspend fun imageBytes(url: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(imageCache, sha1(url))
            if (!file.exists()) {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                    connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                } finally {
                    connection.disconnect()
                }
            }
            file.readBytes()
        }.getOrNull()
    }

    override suspend fun youtubeMusicLink(deezerUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val country = Locale.getDefault().country.ifBlank { "US" }
            val json = getJson(
                "$SONG_LINK_API?userCountry=$country&url=" + URLEncoder.encode(deezerUrl, "UTF-8"),
            )
            val links = json.optJSONObject("linksByPlatform")
            val raw = links?.optJSONObject("youtubeMusic")?.str("url") ?: links?.optJSONObject("youtube")?.str("url")
            raw?.let(::youtubeMusicShareUrl)
        }.getOrNull()
    }

    override suspend fun youtubeMusicLinkForSong(title: String, artist: String?, durationMs: Long): String? {
        val found = searchTrack(LocalTrackInfo(title, artist, album = null, durationMs)) as? TrackSearchResult.Found
            ?: return null
        // Só com boa certeza: melhor o link de busca do que a música errada.
        val best = found.candidates.first().takeIf { it.confidence >= SHARE_CONFIDENCE } ?: return null
        return best.link?.let { youtubeMusicLink(it) }
    }

    override suspend fun image(url: String, sizePx: Int): Bitmap? {
        val key = "$url@$sizePx"
        memoryImages.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val file = File(imageCache, sha1(url))
                if (!file.exists()) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 15_000
                    try {
                        if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                        connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                    } finally {
                        connection.disconnect()
                    }
                }
                decodeSampled(file, sizePx)?.also { memoryImages.put(key, it) }
            }.getOrNull()
        }
    }

    private fun decodeSampled(file: File, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) sample *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
    }

    /** GET com cache em arquivo. Erros de rede viram exceção. */
    private fun getJson(url: String): JSONObject {
        val file = File(jsonCache, sha1(url) + ".json")
        if (file.exists() && System.currentTimeMillis() - file.lastModified() < JSON_TTL_MS) {
            runCatching { return JSONObject(file.readText()) }
        }
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        try {
            check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Deezer ${connection.responseCode}" }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            // O Deezer responde 200 com {"error": …} em alguns erros.
            check(!json.has("error")) { "Deezer: ${json.opt("error")}" }
            runCatching { file.writeText(body) }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

private fun JSONObject.str(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun JSONObject.toArtist() = ExternalArtist(
    id = optLong("id"),
    name = optString("name"),
    pictureUrl = str("picture_xl") ?: str("picture_big"),
    fans = optLong("nb_fan"),
    albumCount = optInt("nb_album"),
    link = str("link"),
)

private fun JSONObject.toRelease() = Release(
    id = optLong("id"),
    title = optString("title"),
    type = when (optString("record_type").lowercase()) {
        "single" -> ReleaseType.Single
        "ep" -> ReleaseType.EP
        "compile", "compilation" -> ReleaseType.Compilation
        else -> ReleaseType.Album
    },
    releaseDate = str("release_date"),
    coverUrl = str("cover_big") ?: str("cover_xl") ?: str("cover_medium"),
    link = str("link"),
)

private fun JSONObject.toPopularTrack() = PopularTrack(
    id = optLong("id"),
    title = optString("title"),
    albumTitle = optJSONObject("album")?.str("title"),
    coverUrl = optJSONObject("album")?.let { it.str("cover_medium") ?: it.str("cover_big") },
    durationMs = optLong("duration") * 1000,
    link = str("link"),
)

private fun JSONObject.toCandidate(): TrackCandidate {
    val album = optJSONObject("album")
    return TrackCandidate(
        trackId = optLong("id"),
        albumId = album?.optLong("id") ?: 0,
        title = optString("title"),
        artist = optJSONObject("artist")?.str("name").orEmpty(),
        album = album?.str("title").orEmpty(),
        coverUrl = album?.let { it.str("cover_xl") ?: it.str("cover_big") ?: it.str("cover_medium") },
        durationMs = optLong("duration") * 1000,
        link = str("link"),
    )
}

/**
 * Junta a faixa (/track) e o álbum (/album) do Deezer nas tags do arquivo.
 * O gênero do Deezer é amplo ("Pop", "Rock"); quem chama pode trocar por um
 * mais preciso do Last.fm.
 */
internal fun tagSuggestionOf(track: JSONObject, album: JSONObject, candidate: TrackCandidate): TagSuggestion {
    val mainArtists = track.optJSONArray("contributors").objects()
        .filter { it.optString("role").equals("Main", ignoreCase = true) }
        .mapNotNull { it.str("name") }
        .distinct()
    val artist = mainArtists.takeIf { it.size > 1 }?.joinToString(", ")
        ?: track.optJSONObject("artist")?.str("name")
        ?: candidate.artist
    val releaseDate = album.str("release_date") ?: track.str("release_date")
    val genre = album.optJSONObject("genres")?.optJSONArray("data").objects()
        .firstNotNullOfOrNull { it.str("name") }
        .orEmpty()
    val trackNumber = track.optInt("track_position").takeIf { it > 0 }?.toString().orEmpty()
    val discNumber = track.optInt("disk_number").takeIf { it > 0 }?.toString().orEmpty()
    val totalTracks = album.optInt("nb_tracks").takeIf { it > 0 }?.toString().orEmpty()
    return TagSuggestion(
        tags = TrackTags(
            title = track.str("title") ?: candidate.title,
            artist = artist,
            album = album.str("title") ?: candidate.album,
            albumArtist = album.optJSONObject("artist")?.str("name").orEmpty(),
            year = releaseDate?.take(4).orEmpty(),
            genre = genre,
            trackNumber = trackNumber,
            trackTotal = if (trackNumber.isEmpty()) "" else totalTracks,
            discNumber = discNumber,
        ),
        coverUrl = album.str("cover_xl") ?: album.str("cover_big") ?: candidate.coverUrl,
        durationMs = track.optLong("duration") * 1000,
        confidence = candidate.confidence,
        source = "Deezer",
        link = track.str("link") ?: candidate.link,
    )
}
