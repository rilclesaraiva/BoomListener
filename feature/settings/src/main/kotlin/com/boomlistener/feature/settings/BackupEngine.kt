// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/*
 * Backup dos dados da pessoa: um .zip com os arquivos do app (playlists,
 * favoritas, estatísticas, histórico, perfil), os ajustes (SharedPreferences
 * e DataStore) e um manifest.json com a descrição de cada música citada
 * (BackupTracks.kt), para as músicas serem achadas em outro celular.
 *
 * As músicas em si não vão: o backup só leva os dados do app, e só vai para
 * onde a pessoa escolher salvar.
 *
 * Arquivo novo de dados da pessoa precisa entrar em [USER_DIRS] (ou já estar
 * nos ajustes), senão fica fora do backup.
 */

/** Pastas de `filesDir` com dados da pessoa. */
private val USER_DIRS = listOf("favorites", "playlists", "profile", "stats")

/** Ajustes que não são da pessoa (avisos de atualização) ou nem são do app. */
private val SKIPPED_PREFS = setOf("update_prompt")
private val SKIPPED_PREF_PREFIXES = listOf("androidx", "WebView", "com.google")

private const val MANIFEST = "manifest.json"
private const val FILES = "files/"
private const val PREFS = "prefs/"
private const val DATASTORE = "datastore/"
private const val APP_ID = "boomlistener"
private const val FORMAT = 1

internal sealed interface RestoreResult {
    data object Done : RestoreResult
    data object NotABackup : RestoreResult
    data object Newer : RestoreResult
}

/** Grava o backup em [target] (o arquivo que a pessoa escolheu). */
internal suspend fun exportBackup(context: Context, target: Uri, appVersion: String) = withContext(Dispatchers.IO) {
    val files = File(context.filesDir.path)
    val userFiles = USER_DIRS.flatMap { dir -> File(files, dir).walkTopDown().filter { it.isFile }.toList() }
    val uris = userFiles.filter { it.extension == "json" }.flatMap { mediaUrisIn(it.readText()) }.toSet()
    val tracks = describeTracks(context, uris)

    val manifest = JSONObject()
        .put("app", APP_ID)
        .put("format", FORMAT)
        .put("version", appVersion)
        .put("createdAt", System.currentTimeMillis())
        .put("tracks", JSONArray().apply { tracks.forEach { put(it.toJson()) } })

    val out = context.contentResolver.openOutputStream(target, "wt") ?: error("no output")
    ZipOutputStream(out.buffered()).use { zip ->
        fun add(name: String, bytes: ByteArray) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
        add(MANIFEST, manifest.toString().toByteArray())
        userFiles.forEach { add(FILES + it.relativeTo(files).invariantSeparatorsPath, it.readBytes()) }
        prefsDir(context).listFiles().orEmpty().filter { it.isFile && keepPrefs(it.name) }.forEach { add(PREFS + it.name, it.readBytes()) }
        dataStoreDir(context).listFiles().orEmpty().filter { it.isFile }.forEach { add(DATASTORE + it.name, it.readBytes()) }
    }
}

/**
 * Troca os dados do app pelos do backup em [source], achando cada música no
 * aparelho de agora. Depois disso o app precisa reiniciar ([restartApp]):
 * as telas e os repositórios ainda guardam os dados antigos na memória.
 */
internal suspend fun restoreBackup(context: Context, source: Uri): RestoreResult = withContext(Dispatchers.IO) {
    val entries = LinkedHashMap<String, ByteArray>()
    val input = context.contentResolver.openInputStream(source) ?: return@withContext RestoreResult.NotABackup
    runCatching {
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory && safeName(entry.name)) {
                    val bytes = ByteArrayOutputStream().also { zip.copyTo(it) }.toByteArray()
                    entries[entry.name] = bytes
                }
            }
        }
    }.onFailure { return@withContext RestoreResult.NotABackup }

    val manifest = entries[MANIFEST]?.let { runCatching { JSONObject(String(it)) }.getOrNull() }
    if (manifest == null || manifest.optString("app") != APP_ID) return@withContext RestoreResult.NotABackup
    if (manifest.optInt("format") > FORMAT) return@withContext RestoreResult.Newer

    val saved = manifest.optJSONArray("tracks")?.let { array -> (0 until array.length()).map { array.getJSONObject(it).toTrack() } }.orEmpty()
    val map = matchBackupTracks(saved, currentTracks(context))

    // Os dados da pessoa são trocados por inteiro pelos do backup.
    val files = context.filesDir
    USER_DIRS.forEach { File(files, it).deleteRecursively() }
    for ((name, bytes) in entries) {
        when {
            name.startsWith(FILES) -> {
                val file = File(files, name.removePrefix(FILES))
                file.parentFile?.mkdirs()
                if (file.extension == "json") file.writeText(remapMediaUris(String(bytes), map)) else file.writeBytes(bytes)
            }
            name.startsWith(PREFS) -> File(prefsDir(context).apply { mkdirs() }, name.removePrefix(PREFS)).writeBytes(bytes)
            name.startsWith(DATASTORE) -> File(dataStoreDir(context).apply { mkdirs() }, name.removePrefix(DATASTORE)).writeBytes(bytes)
        }
    }
    RestoreResult.Done
}

/** Fecha e abre o app de novo, para tudo ler os dados restaurados. */
internal fun restartApp(context: Context) {
    val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
    context.startActivity(Intent.makeRestartActivityTask(launch.component))
    Runtime.getRuntime().exit(0)
}

/** Só nomes conhecidos e sem "..": um backup não pode escrever fora das pastas do app. */
private fun safeName(name: String): Boolean {
    if (name.contains("..") || name.startsWith("/") || name.contains('\\')) return false
    return when {
        name == MANIFEST -> true
        name.startsWith(FILES) -> USER_DIRS.any { name.removePrefix(FILES).startsWith("$it/") }
        name.startsWith(PREFS) -> name.count { it == '/' } == 1 && name.endsWith(".xml") && keepPrefs(name.removePrefix(PREFS))
        name.startsWith(DATASTORE) -> name.count { it == '/' } == 1 && name.endsWith(".preferences_pb")
        else -> false
    }
}

private fun keepPrefs(fileName: String): Boolean {
    val name = fileName.removeSuffix(".xml")
    return name !in SKIPPED_PREFS && SKIPPED_PREF_PREFIXES.none { name.startsWith(it) }
}

private fun prefsDir(context: Context) = File(context.applicationInfo.dataDir, "shared_prefs")
private fun dataStoreDir(context: Context) = File(context.filesDir, "datastore")

/** A descrição de cada música citada (as que não existem mais ficam de fora). */
private fun describeTracks(context: Context, uris: Set<String>): List<BackupTrack> {
    if (uris.isEmpty()) return emptyList()
    val byId = uris.mapNotNull { uri -> runCatching { ContentUris.parseId(Uri.parse(uri)) }.getOrNull()?.let { it to uri } }.toMap()
    return queryAudio(context).filter { it.first in byId }.map { (id, track) -> track.copy(uri = byId.getValue(id)) }
}

/** Todas as músicas do aparelho agora. */
private fun currentTracks(context: Context): List<BackupTrack> = queryAudio(context).map { it.second }

private fun queryAudio(context: Context): List<Pair<Long, BackupTrack>> {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val modernPath = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    @Suppress("DEPRECATION")
    val pathColumn = if (modernPath) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DISPLAY_NAME,
        pathColumn,
    )
    val result = ArrayList<Pair<Long, BackupTrack>>()
    runCatching {
        context.contentResolver.query(collection, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val path = cursor.string(6)
                val folder = path.trimEnd('/').let { if (modernPath) it else it.substringBeforeLast('/', "") }.substringAfterLast('/')
                result += id to BackupTrack(
                    uri = ContentUris.withAppendedId(collection, id).toString(),
                    title = cursor.string(1),
                    artist = cursor.string(2).takeUnless { it == MediaStore.UNKNOWN_STRING }.orEmpty(),
                    album = cursor.string(3),
                    durationMs = cursor.getLong(4),
                    fileName = cursor.string(5),
                    folder = folder,
                )
            }
        }
    }
    return result
}

private fun Cursor.string(index: Int): String = if (isNull(index)) "" else getString(index).orEmpty()

private fun BackupTrack.toJson() = JSONObject()
    .put("uri", uri)
    .put("title", title)
    .put("artist", artist)
    .put("album", album)
    .put("duration", durationMs)
    .put("file", fileName)
    .put("folder", folder)

private fun JSONObject.toTrack() = BackupTrack(
    uri = optString("uri"),
    title = optString("title"),
    artist = optString("artist"),
    album = optString("album"),
    durationMs = optLong("duration"),
    fileName = optString("file"),
    folder = optString("folder"),
)
