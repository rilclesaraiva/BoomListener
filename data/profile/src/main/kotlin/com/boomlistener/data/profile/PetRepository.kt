// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.boomlistener.core.common.DiagnosticLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/** O que fica atrás do bichinho na caixa do perfil. */
enum class PetBackgroundKind { Profile, Color, Image, None }

/**
 * O bichinho do perfil: um pacote (de shimeji ou uma imagem) que passeia numa caixa do Perfil.
 * Fica só neste aparelho. Só existe se a pessoa importar um.
 */
data class PetState(
    val loaded: Boolean = false,
    val enabled: Boolean = true,
    val pack: PetPack? = null,
    /** Muda a cada troca de pacote: a tela relê as imagens quando muda. */
    val packVersion: Long = 0,
    val backgroundKind: PetBackgroundKind = PetBackgroundKind.Profile,
    /** "#RRGGBB" quando o fundo é [PetBackgroundKind.Color]. */
    val backgroundColor: String = "",
    val backgroundVersion: Long = 0,
)

interface PetRepository {
    val state: StateFlow<PetState>

    /**
     * Importa o bichinho de [uri] (o content:// do seletor de arquivos): um .zip de shimeji
     * ou uma imagem PNG/JPEG/WebP. Troca o que havia. false se o arquivo não serve.
     */
    suspend fun importPack(uri: String): Boolean

    /** Tira o bichinho (e as imagens dele). O fundo escolhido fica. */
    suspend fun removePack()

    suspend fun setEnabled(enabled: Boolean)

    /** O fundo da caixa; false se for cor e a cor for inválida, ou imagem sem imagem salva. */
    suspend fun setBackground(kind: PetBackgroundKind, color: String = ""): Boolean

    /** Salva (ou, com null, tira) a imagem de fundo e passa a usá-la. false se não deu para ler. */
    suspend fun setBackgroundImage(uri: String?): Boolean

    /** Os bytes de um quadro do bichinho; null se não existir. */
    suspend fun frame(file: String): ByteArray?

    /** A imagem de fundo guardada; null se não houver. */
    suspend fun backgroundImage(): ByteArray?
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object PetData {
    @Volatile
    private var instance: PetRepository? = null

    fun repository(context: Context): PetRepository =
        instance ?: synchronized(this) {
            instance ?: LocalPetRepository(context.applicationContext).also { instance = it }
        }
}

internal data class PetSettings(
    val enabled: Boolean = true,
    val packVersion: Long = 0,
    val backgroundKind: PetBackgroundKind = PetBackgroundKind.Profile,
    val backgroundColor: String = "",
    val backgroundVersion: Long = 0,
)

internal fun petSettingsToJson(settings: PetSettings): String = JSONObject()
    .put("enabled", settings.enabled)
    .put("pack", settings.packVersion)
    .put("bg", settings.backgroundKind.name)
    .put("bgColor", settings.backgroundColor)
    .put("bgVersion", settings.backgroundVersion)
    .toString()

internal fun petSettingsFromJson(text: String): PetSettings {
    val json = JSONObject(text)
    return PetSettings(
        enabled = json.optBoolean("enabled", true),
        packVersion = json.optLong("pack"),
        backgroundKind = PetBackgroundKind.entries.firstOrNull { it.name == json.optString("bg") } ?: PetBackgroundKind.Profile,
        backgroundColor = cleanProfileColor(json.optString("bgColor")).orEmpty(),
        backgroundVersion = json.optLong("bgVersion"),
    )
}

/** Limites do pacote: protegem o aparelho de um .zip gigante ou cheio de arquivos. */
private const val MAX_ENTRIES = 400
private const val MAX_ENTRY_BYTES = 3 * 1024 * 1024
private const val MAX_TOTAL_BYTES = 30 * 1024 * 1024
/** O .zip em si pode ter programas e bibliotecas dentro (só PNG e XML são usados): aceita até este tamanho. */
private const val MAX_ZIP_BYTES = 60 * 1024 * 1024
private const val MAX_SINGLE_IMAGE_SIDE = 512

private val PNG_HEADER = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())

/** Lê até [limit] bytes; null se passar disso. */
private fun readLimited(input: InputStream, limit: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > limit) return null
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}

/** Copia até [limit] bytes; passando disso, lança erro (o arquivo é grande demais). */
private fun copyLimited(input: InputStream, out: java.io.OutputStream, limit: Int) {
    val buffer = ByteArray(16 * 1024)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        require(total <= limit) { "arquivo grande demais" }
        out.write(buffer, 0, read)
    }
}

internal class LocalPetRepository(private val context: Context) : PetRepository {
    private val dir = File(context.filesDir, "profile/pet")
    private val imagesDir = File(dir, "img")
    private val settingsFile = File(dir, "settings.json")
    private val packFile = File(dir, "pack.json")
    private val backgroundFile = File(dir, "background")
    private val mutex = Mutex()
    private var settings = PetSettings()
    private val _state = MutableStateFlow(PetState())
    override val state: StateFlow<PetState> = _state.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            mutex.withLock {
                runCatching { if (settingsFile.exists()) settings = petSettingsFromJson(settingsFile.readText()) }
                publish(readPack())
            }
        }
    }

    private fun readPack(): PetPack? =
        packFile.takeIf { it.exists() }?.let { runCatching { petFromJson(it.readText()) }.getOrNull() }
            ?.takeIf { pack -> pack.files.all { File(imagesDir, it).exists() } }

    private fun publish(pack: PetPack?) {
        _state.value = PetState(
            loaded = true,
            enabled = settings.enabled,
            pack = pack,
            packVersion = settings.packVersion,
            backgroundKind = settings.backgroundKind,
            backgroundColor = settings.backgroundColor,
            backgroundVersion = settings.backgroundVersion,
        )
    }

    private fun saveSettings(new: PetSettings) {
        dir.mkdirs()
        val temp = File(dir, "settings.json.tmp")
        temp.writeText(petSettingsToJson(new))
        if (!temp.renameTo(settingsFile)) {
            settingsFile.writeText(temp.readText())
            temp.delete()
        }
        settings = new
        publish(_state.value.pack)
    }

    override suspend fun importPack(uri: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val parsed = Uri.parse(uri)
                dir.mkdirs()
                val staging = File(dir, "img.new")
                staging.deleteRecursively()
                staging.mkdirs()
                val name = parsed.lastPathSegment.orEmpty().substringAfterLast('/').substringBeforeLast('.')
                var xml: String? = null
                DiagnosticLog.log("bichinho", "importando")
                val opened = context.contentResolver.openInputStream(parsed)
                if (opened == null) {
                    DiagnosticLog.log("bichinho", "o sistema não abriu o arquivo (openInputStream = null)")
                    return@runCatching false
                }
                opened.use { raw ->
                    val input = BufferedInputStream(raw)
                    input.mark(8)
                    val head = ByteArray(4).also { input.read(it) }
                    input.reset()
                    DiagnosticLog.log("bichinho", "início do arquivo: ${head.joinToString(" ") { "%02X".format(it) }}")
                    if (head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) {
                        // Zip lido pelo índice (ZipFile): o que vem de programas como o Shimeji-ee às vezes tem
                        // arquivos sem compressão com "descritor", que a leitura em sequência recusa.
                        val temp = File(dir, "import.zip")
                        try {
                            temp.outputStream().use { out -> copyLimited(input, out, MAX_ZIP_BYTES) }
                            DiagnosticLog.log("bichinho", "zip copiado: ${temp.length()} bytes")
                            xml = unzipPack(temp, staging)
                        } finally {
                            temp.delete()
                        }
                    } else {
                        saveSingleImage(input, staging)
                    }
                }
                val available = staging.list()?.filter { it.endsWith(".png") }?.toSet().orEmpty()
                DiagnosticLog.log("bichinho", "imagens guardadas: ${available.size}, actions.xml: ${if (xml != null) "achado (${xml!!.length} caracteres)" else "não achado"}")
                val pack = buildPetPack(name, xml, available)
                if (pack == null) {
                    DiagnosticLog.log("bichinho", "pacote recusado: nenhuma pose utilizável (parado/andando)")
                    staging.deleteRecursively()
                    return@runCatching false
                }
                // Só ficam as imagens que o bichinho usa.
                staging.listFiles()?.forEach { if (it.name !in pack.files) it.delete() }
                imagesDir.deleteRecursively()
                if (!staging.renameTo(imagesDir)) {
                    staging.copyRecursively(imagesDir, overwrite = true)
                    staging.deleteRecursively()
                }
                packFile.writeText(petToJson(pack))
                saveSettings(settings.copy(packVersion = System.currentTimeMillis()))
                publish(pack)
                DiagnosticLog.log("bichinho", "importado: ${pack.files.size} imagens, ${pack.poses.size} ações")
                true
            }.onFailure {
                DiagnosticLog.log("bichinho", "erro ao importar: ${it::class.qualifiedName}: ${it.message}")
                DiagnosticLog.log("bichinho", it.stackTrace.take(4).joinToString(" | "))
            }.getOrDefault(false)
        }
    }

    /** Guarda as imagens PNG do zip em [staging] e devolve o texto do `actions.xml`, se vier um. */
    private fun unzipPack(file: File, staging: File): String? {
        var xml: String? = null
        var entries = 0
        var total = 0
        var pngs = 0
        var skipped = 0
        ZipArchive(file).use { zip ->
            DiagnosticLog.log("bichinho", "zip aberto: ${zip.entries.size} entradas")
            for (entry in zip.entries) {
                if (entry.isDirectory) continue
                if (++entries > MAX_ENTRIES) break
                val name = petEntryName(entry.name) ?: continue
                val bytes = zip.read(entry, MAX_ENTRY_BYTES)
                if (bytes == null) { skipped++; continue }
                total += bytes.size
                if (total > MAX_TOTAL_BYTES) break
                if (name.endsWith(".xml")) {
                    val text = bytes.toString(Charsets.UTF_8).removePrefix("\uFEFF")
                    if (xml == null && text.contains("<Action")) xml = text
                } else if (bytes.size > PNG_HEADER.size && bytes.copyOfRange(0, PNG_HEADER.size).contentEquals(PNG_HEADER)) {
                    val target = File(staging, name)
                    if (!target.exists()) target.writeBytes(bytes)
                    pngs++
                } else {
                    skipped++
                }
            }
            DiagnosticLog.log("bichinho", "lidas $entries entradas: $pngs PNG, $skipped ignoradas (grandes ou inválidas)")
        }
        return xml
    }

    /** Uma imagem solta vira o bichinho parado (reduzida se for grande). */
    private fun saveSingleImage(input: InputStream, staging: File) {
        val bytes = readLimited(input, MAX_TOTAL_BYTES) ?: return
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_SINGLE_IMAGE_SIDE && bounds.outHeight / (sample * 2) >= MAX_SINGLE_IMAGE_SIDE) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return
        File(staging, "shime1.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    override suspend fun removePack() {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    imagesDir.deleteRecursively()
                    packFile.delete()
                    saveSettings(settings.copy(packVersion = 0))
                    publish(null)
                }
            }
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) { mutex.withLock { runCatching { saveSettings(settings.copy(enabled = enabled)) } } }
    }

    override suspend fun setBackground(kind: PetBackgroundKind, color: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                val clean = when (kind) {
                    PetBackgroundKind.Color -> cleanProfileColor(color)?.takeIf { it.isNotEmpty() } ?: return@runCatching false
                    PetBackgroundKind.Image -> if (backgroundFile.exists()) settings.backgroundColor else return@runCatching false
                    else -> settings.backgroundColor
                }
                saveSettings(settings.copy(backgroundKind = kind, backgroundColor = clean))
                true
            }.getOrDefault(false)
        }
    }

    override suspend fun setBackgroundImage(uri: String?): Boolean {
        val bytes = if (uri == null) {
            null
        } else {
            // A caixa é larga como a capa do perfil: o mesmo preparo (recorte, redução e GIF animado).
            withContext(Dispatchers.Default) { prepareProfileImage(context, uri, ProfileImage.Banner) } ?: return false
        }
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    dir.mkdirs()
                    if (bytes == null) {
                        backgroundFile.delete()
                        val kind = if (settings.backgroundKind == PetBackgroundKind.Image) PetBackgroundKind.Profile else settings.backgroundKind
                        saveSettings(settings.copy(backgroundKind = kind, backgroundVersion = 0))
                    } else {
                        backgroundFile.writeBytes(bytes)
                        saveSettings(settings.copy(backgroundKind = PetBackgroundKind.Image, backgroundVersion = System.currentTimeMillis()))
                    }
                    true
                }.getOrDefault(false)
            }
        }
    }

    override suspend fun frame(file: String): ByteArray? = withContext(Dispatchers.IO) {
        // Só nomes que o pacote guardou (nunca um caminho).
        if (file != petBaseName(file)) return@withContext null
        File(imagesDir, file).takeIf { it.isFile }?.let { runCatching { it.readBytes() }.getOrNull() }
    }

    override suspend fun backgroundImage(): ByteArray? = withContext(Dispatchers.IO) {
        backgroundFile.takeIf { it.isFile }?.let { runCatching { it.readBytes() }.getOrNull() }
    }
}
