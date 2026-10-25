// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import android.content.Context
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
import java.io.File

/**
 * O seu perfil, só seu: nome, descrição, cor, foto e capa. Fica só neste
 * aparelho (nada vai para servidor nenhum); é para deixar o app do seu jeito.
 */
data class MyProfile(
    val name: String = "",
    val bio: String = "",
    /** "#RRGGBB" ou "" (nenhuma). */
    val color: String = "",
    /** Cor sólida no perfil inteiro; falso = degradê do topo para baixo (o padrão). */
    val solidColor: Boolean = false,
    /** Muda a cada troca de imagem (0 = sem imagem): a tela relê quando muda. */
    val avatarVersion: Long = 0,
    val bannerVersion: Long = 0,
)

/** Foto de perfil (quadrada) ou capa (faixa larga no topo do perfil). */
enum class ProfileImage(val path: String, val width: Int, val height: Int) {
    Avatar("avatar", 512, 512),
    Banner("banner", 1080, 405),
}

/** Tamanho máximo da descrição. */
const val MAX_BIO = 160

interface ProfileRepository {
    val profile: StateFlow<MyProfile>

    /** Nome, descrição, cor e se a cor é sólida; false se a cor for inválida. */
    suspend fun save(name: String, bio: String, color: String, solidColor: Boolean): Boolean

    /**
     * Troca a foto ou a capa por [uri] (o content:// do seletor de fotos do
     * Android); null tira. A imagem é recortada e reduzida. false se não deu para ler.
     */
    suspend fun setImage(kind: ProfileImage, uri: String?): Boolean

    /** A foto ou a capa guardada; null se não houver. */
    suspend fun image(kind: ProfileImage): ByteArray?
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object ProfileData {
    @Volatile
    private var instance: ProfileRepository? = null

    fun repository(context: Context): ProfileRepository =
        instance ?: synchronized(this) {
            instance ?: LocalProfileRepository(context.applicationContext).also { instance = it }
        }
}

private val PROFILE_COLOR = Regex("^#?([0-9a-fA-F]{6})$")

/** "#ff5500", "FF5500" → "#FF5500"; vazio → "" (sem cor); inválido → null. */
fun cleanProfileColor(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    return PROFILE_COLOR.find(trimmed)?.let { "#" + it.groupValues[1].uppercase() }
}

internal fun profileToJson(profile: MyProfile): String = JSONObject()
    .put("name", profile.name)
    .put("bio", profile.bio)
    .put("color", profile.color)
    .put("solid", profile.solidColor)
    .put("avatar", profile.avatarVersion)
    .put("banner", profile.bannerVersion)
    .toString()

internal fun profileFromJson(text: String): MyProfile {
    val json = JSONObject(text)
    return MyProfile(
        name = json.optString("name"),
        bio = json.optString("bio").take(MAX_BIO),
        color = cleanProfileColor(json.optString("color")).orEmpty(),
        // Arquivos de antes desta opção não têm "solid": continuam com o degradê.
        solidColor = json.optBoolean("solid", false),
        avatarVersion = json.optLong("avatar"),
        bannerVersion = json.optLong("banner"),
    )
}

/**
 * O perfil das versões com amigos (até a 0.32): nome, descrição e cor
 * continuam; o @, o segredo, as redes e o resto do social ficam para trás.
 */
internal fun profileFromOldSocialAccount(text: String): MyProfile? = runCatching {
    val json = JSONObject(text)
    MyProfile(
        name = json.optString("name"),
        bio = json.optString("bio").take(MAX_BIO),
        color = cleanProfileColor(json.optString("color")).orEmpty(),
    )
}.getOrNull()

internal class LocalProfileRepository(private val context: Context) : ProfileRepository {
    private val dir = File(context.filesDir, "profile")
    private val file = File(dir, "profile.json")
    private val mutex = Mutex()
    private val _profile = MutableStateFlow(MyProfile())
    override val profile: StateFlow<MyProfile> = _profile.asStateFlow()

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            mutex.withLock {
                dir.mkdirs()
                migrateFromSocial()
                if (file.exists()) runCatching { _profile.value = profileFromJson(file.readText()) }
            }
        }
    }

    private fun imageFile(kind: ProfileImage) = File(dir, kind.path)

    /**
     * Uma vez: traz nome, descrição, cor, foto e capa do perfil antigo (que
     * ficava no servidor do social) e apaga tudo o que era do social.
     */
    private fun migrateFromSocial() {
        val old = File(context.filesDir, "social")
        if (!old.exists()) return
        if (!file.exists()) {
            val account = File(old, "account.json")
            val imported = account.takeIf { it.exists() }?.let { runCatching { profileFromOldSocialAccount(it.readText()) }.getOrNull() }
            if (imported != null) {
                val id = runCatching { JSONObject(account.readText()).optString("id") }.getOrNull().orEmpty()
                var profile: MyProfile = imported
                // A sua foto e a sua capa já estavam guardadas no aparelho.
                if (id.isNotEmpty()) {
                    ProfileImage.entries.forEach { kind ->
                        val saved = File(old, "images").listFiles { f -> f.name.startsWith("${id}_${kind.path}_") }
                            ?.maxByOrNull { it.lastModified() }
                        if (saved != null && runCatching { saved.copyTo(imageFile(kind), overwrite = true) }.isSuccess) {
                            val version = System.currentTimeMillis()
                            profile = when (kind) {
                                ProfileImage.Avatar -> profile.copy(avatarVersion = version)
                                ProfileImage.Banner -> profile.copy(bannerVersion = version)
                            }
                        }
                    }
                }
                runCatching { file.writeText(profileToJson(profile)) }
            }
        }
        old.deleteRecursively()
    }

    private fun write(profile: MyProfile) {
        dir.mkdirs()
        val temp = File(dir, "profile.json.tmp")
        temp.writeText(profileToJson(profile))
        if (!temp.renameTo(file)) {
            file.writeText(temp.readText())
            temp.delete()
        }
        _profile.value = profile
    }

    override suspend fun save(name: String, bio: String, color: String, solidColor: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cleanColor = cleanProfileColor(color) ?: return@withContext false
        mutex.withLock {
            runCatching {
                write(_profile.value.copy(name = name.trim().take(40), bio = bio.trim().take(MAX_BIO), color = cleanColor, solidColor = solidColor))
            }.isSuccess
        }
    }

    override suspend fun setImage(kind: ProfileImage, uri: String?): Boolean {
        val bytes = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.Default) { prepareProfileImage(context, uri, kind) } ?: return false
        }
        return withContext(Dispatchers.IO) {
            mutex.withLock {
                runCatching {
                    val target = imageFile(kind)
                    if (bytes == null) target.delete() else target.writeBytes(bytes)
                    val version = if (bytes == null) 0L else System.currentTimeMillis()
                    write(
                        when (kind) {
                            ProfileImage.Avatar -> _profile.value.copy(avatarVersion = version)
                            ProfileImage.Banner -> _profile.value.copy(bannerVersion = version)
                        },
                    )
                }.isSuccess
            }
        }
    }

    override suspend fun image(kind: ProfileImage): ByteArray? = withContext(Dispatchers.IO) {
        imageFile(kind).takeIf { it.exists() }?.let { runCatching { it.readBytes() }.getOrNull() }
    }
}
