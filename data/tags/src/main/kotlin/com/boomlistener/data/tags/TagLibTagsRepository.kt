// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.tags

import android.app.RecoverableSecurityException
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.boomlistener.core.model.TrackTags
import com.kyant.taglib.Picture
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume

private const val FRONT_COVER = "Front Cover"
// Capas maiores que isso são reduzidas antes de gravar (o arquivo não
// precisa de uma foto de 4000 px; 1200 px já fica nítido em qualquer tela).
private const val MAX_COVER_PX = 1200

/**
 * Implementação com o TagLib (biblioteca nativa, a mesma de vários players
 * de desktop). Trabalha com o descritor do arquivo aberto pelo MediaStore,
 * que é o único jeito de gravar em arquivos de música no Android 10+.
 */
internal class TagLibTagsRepository(private val context: Context) : TagsRepository {

    private val resolver = context.contentResolver

    override suspend fun read(uri: String): TagsReadResult = withContext(Dispatchers.IO) {
        try {
            resolver.openFileDescriptor(Uri.parse(uri), "r")?.use { file ->
                val metadata = TagLib.getMetadata(file.dupFd(), readPictures = true)
                    ?: return@withContext TagsReadResult.Failed
                val cover = metadata.pictures.firstOrNull { it.pictureType == FRONT_COVER }
                    ?: metadata.pictures.firstOrNull()
                val durationMs = runCatching { TagLib.getAudioProperties(file.dupFd())?.length?.toLong() }
                    .getOrNull() ?: 0L
                TagsReadResult.Found(tagsFromProperties(metadata.propertyMap.toListMap()), cover?.data, durationMs)
            } ?: TagsReadResult.Failed
        } catch (_: Exception) {
            TagsReadResult.Failed
        } catch (_: LinkageError) {
            // Biblioteca nativa não carregou (aparelho com processador incomum).
            TagsReadResult.Failed
        }
    }

    override suspend fun readLyrics(uri: String): String? = withContext(Dispatchers.IO) {
        try {
            resolver.openFileDescriptor(Uri.parse(uri), "r")?.use { file ->
                TagLib.getMetadataPropertyValues(file.dupFd(), KEY_LYRICS)
                    ?.firstOrNull { it.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    override suspend fun write(uri: String, tags: TrackTags, cover: CoverEdit): TagsWriteResult =
        withContext(Dispatchers.IO) {
            val parsed = Uri.parse(uri)
            val opened: ParcelFileDescriptor? = try {
                resolver.openFileDescriptor(parsed, "rw")
            } catch (denied: SecurityException) {
                return@withContext permissionFor(parsed, denied)
            } catch (_: Exception) {
                null
            }
            val file = opened ?: return@withContext TagsWriteResult.Failed

            val saved = try {
                file.use { writeInto(it, tags, cover) }
            } catch (_: Exception) {
                false
            } catch (_: LinkageError) {
                false
            }
            if (!saved) return@withContext TagsWriteResult.Failed
            rescan(parsed)
            TagsWriteResult.Success
        }

    private fun writeInto(file: ParcelFileDescriptor, tags: TrackTags, cover: CoverEdit): Boolean {
        // Lê tudo antes, para manter as tags que o editor não mostra.
        val metadata = TagLib.getMetadata(file.dupFd(), readPictures = false) ?: return false
        val updated = applyTags(metadata.propertyMap.toListMap(), tags)
        val properties = HashMap<String, Array<String>>().apply {
            updated.forEach { (key, values) -> put(key, values.toTypedArray()) }
        }
        if (!TagLib.savePropertyMap(file.dupFd(), properties)) return false
        return when (cover) {
            CoverEdit.Keep -> true
            CoverEdit.Remove -> TagLib.savePictures(file.dupFd(), emptyArray())
            is CoverEdit.Replace -> {
                val image = shrinkCover(cover.data)
                TagLib.savePictures(
                    file.dupFd(),
                    arrayOf(Picture(data = image.first, description = "", pictureType = FRONT_COVER, mimeType = image.second)),
                )
            }
        }
    }

    /** Pede ao Android o acesso de gravação do jeito de cada versão. */
    private fun permissionFor(uri: Uri, denied: SecurityException): TagsWriteResult = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            TagsWriteResult.NeedsPermission(MediaStore.createWriteRequest(resolver, listOf(uri)).intentSender)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && denied is RecoverableSecurityException ->
            TagsWriteResult.NeedsPermission(denied.userAction.actionIntent.intentSender)
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> TagsWriteResult.NeedsStoragePermission
        else -> TagsWriteResult.Failed
    }

    /**
     * Avisa o Android que o arquivo mudou, para a biblioteca (e as capas)
     * mostrarem as tags novas. Espera no máximo alguns segundos.
     */
    @Suppress("DEPRECATION") // DATA ainda é o caminho aceito pelo MediaScanner.
    private suspend fun rescan(uri: Uri) {
        val path = runCatching {
            resolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: return
        withTimeoutOrNull(5_000) {
            suspendCancellableCoroutine { continuation ->
                MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ ->
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }

    /** Reduz e converte para JPEG quando a imagem é grande demais. */
    private fun shrinkCover(data: ByteArray): Pair<ByteArray, String> {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        val isPng = bounds.outMimeType == "image/png"
        val small = bounds.outWidth in 1..MAX_COVER_PX && bounds.outHeight in 1..MAX_COVER_PX
        val mime = bounds.outMimeType
        if (small && mime != null && (mime == "image/jpeg" || isPng)) return data to mime
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_COVER_PX && bounds.outHeight / (sample * 2) >= MAX_COVER_PX) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return data to (mime ?: "image/jpeg")
        val scale = minOf(1f, MAX_COVER_PX.toFloat() / maxOf(decoded.width, decoded.height))
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(decoded, (decoded.width * scale).toInt(), (decoded.height * scale).toInt(), true)
        } else {
            decoded
        }
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        return out.toByteArray() to "image/jpeg"
    }
}

/**
 * Cada chamada ao TagLib recebe uma cópia do descritor, que ele fecha ao
 * terminar; o original continua aberto para a próxima chamada.
 */
private fun ParcelFileDescriptor.dupFd(): Int = dup().detachFd()

private fun Map<String, Array<String>>.toListMap(): Map<String, List<String>> =
    mapValues { (_, values) -> values.toList() }
