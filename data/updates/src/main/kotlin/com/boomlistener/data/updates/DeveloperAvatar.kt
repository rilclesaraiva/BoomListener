// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.updates

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.boomlistener.core.common.OfflineMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * A foto do desenvolvedor (a do GitHub dele), para Ajustes → Sobre. Baixada
 * uma vez e guardada no aparelho; depois só confere se mudou de vez em quando
 * ([shouldRefreshAvatar]), e "não mudou" (304) nem traz a imagem de novo.
 */
interface DeveloperAvatarRepository {
    /** A foto guardada no aparelho, ou null se ainda não foi baixada. Lê o disco: chame fora da thread principal. */
    fun cached(): Bitmap?

    /** Confere no GitHub, se já está na hora: a foto nova, ou null (nada mudou, sem internet, ainda não é hora). */
    suspend fun refresh(): Bitmap?
}

/** Com a foto guardada, confere uma vez por semana; sem ela (falhou), tenta de novo depois de 10 min. */
internal const val AVATAR_REFRESH_MS = 7L * 24 * 60 * 60 * 1000
internal const val AVATAR_RETRY_MS = 10L * 60 * 1000

/** Já é hora de perguntar ao GitHub pela foto? [lastAttemptMs] = 0 se nunca perguntou. */
fun shouldRefreshAvatar(nowMs: Long, lastAttemptMs: Long, hasFile: Boolean): Boolean {
    // Relógio voltou no tempo: confere de novo.
    if (lastAttemptMs <= 0L || nowMs < lastAttemptMs) return true
    return nowMs - lastAttemptMs >= if (hasFile) AVATAR_REFRESH_MS else AVATAR_RETRY_MS
}

private const val AVATAR_URL = "https://github.com/rilclesaraiva.png?size=256"
private const val KEY_LAST_ATTEMPT = "last_attempt"
private const val KEY_ETAG = "etag"

internal class GitHubDeveloperAvatar(private val context: Context) : DeveloperAvatarRepository {
    private val file = File(File(context.filesDir, "github"), "developer_avatar")
    private val prefs = context.getSharedPreferences(DEVELOPER_AVATAR_PREFS, Context.MODE_PRIVATE)

    override fun cached(): Bitmap? =
        if (file.exists()) runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull() else null

    override suspend fun refresh(): Bitmap? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val hasFile = file.exists()
        if (!shouldRefreshAvatar(now, prefs.getLong(KEY_LAST_ATTEMPT, 0L), hasFile)) return@withContext null
        // Marca antes de perguntar: falhando, não insiste a cada vez que a tela abre.
        prefs.edit().putLong(KEY_LAST_ATTEMPT, now).apply()
        runCatching {
            OfflineMode.requireOnline()
            val connection = (URL(AVATAR_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "BoomListener")
                // "Mudou desde a última?": se não, o GitHub responde 304 sem a imagem.
                if (hasFile) prefs.getString(KEY_ETAG, null)?.let { setRequestProperty("If-None-Match", it) }
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@runCatching null
                val bytes = connection.inputStream.use { it.readBytes() }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return@runCatching null
                file.parentFile?.mkdirs()
                val partial = File(file.parentFile, file.name + ".part")
                partial.writeBytes(bytes)
                if (!partial.renameTo(file)) {
                    file.delete()
                    partial.renameTo(file)
                }
                prefs.edit().putString(KEY_ETAG, connection.getHeaderField("ETag")).apply()
                bitmap
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}

/** Nome dos ajustes da foto (fora do backup: não são da pessoa). */
const val DEVELOPER_AVATAR_PREFS = "developer_avatar"

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object DeveloperAvatarData {
    @Volatile
    private var instance: DeveloperAvatarRepository? = null

    fun repository(context: Context): DeveloperAvatarRepository =
        instance ?: synchronized(this) {
            instance ?: GitHubDeveloperAvatar(context.applicationContext).also { instance = it }
        }
}
