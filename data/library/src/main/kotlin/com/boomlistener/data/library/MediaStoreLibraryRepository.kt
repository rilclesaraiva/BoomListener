// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.library

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import com.boomlistener.core.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal class MediaStoreLibraryRepository(
    private val context: Context,
) : LibraryRepository {

    private val collator = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply {
        strength = Collator.PRIMARY // ignora acento e maiúscula ao ordenar
    }

    // Cache de capas: até ~1/8 da memória do app, contado em KB.
    private val artworkCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
    ) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    // Músicas já conferidas que não têm capa, para não tentar de novo.
    private val noArtwork: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    private val changes = MutableStateFlow(0)
    override val version: StateFlow<Int> = changes.asStateFlow()

    override fun notifyChanged(trackUri: String) {
        artworkCache.snapshot().keys.filter { it.startsWith("$trackUri@") }.forEach { artworkCache.remove(it) }
        noArtwork.removeAll { it.startsWith("$trackUri@") }
        changes.update { it + 1 }
    }

    override suspend fun getTracks(): List<Track> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        // Só músicas: deixa de fora toques, notificações, alarmes e gravações.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val tracks = mutableListOf<Track>()
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null,
            )?.use { cursor -> while (cursor.moveToNext()) tracks += cursor.toTrack() }
        } catch (_: SecurityException) {
            // Sem permissão: a tela já trata esse caso antes de chegar aqui.
        }
        tracks.sortedWith { a, b -> collator.compare(a.title, b.title) }
    }

    override suspend fun loadArtwork(trackUri: String, sizePx: Int): Bitmap? {
        val key = "$trackUri@$sizePx"
        artworkCache.get(key)?.let { return it }
        if (key in noArtwork) return null

        val bitmap = withContext(Dispatchers.IO) {
            val uri = Uri.parse(trackUri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
                }.getOrNull()
            } else {
                loadEmbeddedArtwork(uri, sizePx)
            }
        }
        if (bitmap != null) artworkCache.put(key, bitmap) else noArtwork += key
        return bitmap
    }

    /** Android 9 ou anterior: lê a capa embutida no próprio arquivo. */
    private fun loadEmbeddedArtwork(uri: Uri, sizePx: Int): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val bytes = retriever.embeddedPicture ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) {
                sample *= 2
            }
            BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        } catch (_: Exception) {
            null
        } finally {
            retriever.release()
        }
    }

    private fun Cursor.toTrack(): Track {
        val id = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        val rawTrack = getIntOrNull(MediaStore.Audio.Media.TRACK)
        return Track(
            id = id,
            uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id).toString(),
            // Sem tag de título: usa o nome do arquivo (sem extensão), que vale em qualquer idioma.
            title = getStringOrNull(MediaStore.Audio.Media.TITLE)?.takeIf { it.isNotBlank() }
                ?: getStringOrNull(MediaStore.Audio.Media.DISPLAY_NAME)?.substringBeforeLast('.')
                ?: "—",
            artist = getStringOrNull(MediaStore.Audio.Media.ARTIST)?.cleanUnknown(),
            album = getStringOrNull(MediaStore.Audio.Media.ALBUM)?.cleanUnknown(),
            albumId = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)),
            durationMs = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)),
            trackNumber = parseTrackNumber(rawTrack),
            year = getIntOrNull(MediaStore.Audio.Media.YEAR)?.takeIf { it > 0 },
            dateAddedSeconds = getLong(getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)),
        )
    }

    private fun Cursor.getStringOrNull(column: String): String? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getIntOrNull(column: String): Int? {
        val index = getColumnIndexOrThrow(column)
        return if (isNull(index)) null else getInt(index)
    }

    private fun String.cleanUnknown(): String? = cleanUnknownTag(this)
}

/** O MediaStore guarda o disco junto com a faixa: 1005 = disco 1, faixa 5. */
internal fun parseTrackNumber(raw: Int?): Int? = raw?.let { it % 1000 }?.takeIf { it > 0 }

/** O Android usa "<unknown>" quando o arquivo não tem a tag. */
internal fun cleanUnknownTag(value: String): String? =
    value.takeIf { it.isNotBlank() && it != "<unknown>" }
