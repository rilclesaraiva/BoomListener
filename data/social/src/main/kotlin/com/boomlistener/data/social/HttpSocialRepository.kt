// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Servidor social na Cloudflare (social/worker.js). Não precisa de chave: o
 * perfil é criado aqui e o segredo fica guardado no aparelho. Sem chat: só
 * perfis, amizades, "ouvindo agora", recomendações e reações.
 */
private const val SERVER = "https://boomlistener-social.saraivarilcle.workers.dev/v1"
private const val SOCKET = "wss://boomlistener-social.saraivarilcle.workers.dev/v1/ws?v=3"

/** Músicas ouvidas sem conexão, guardadas para mandar quando conectar. */
private const val MAX_QUEUED_LISTENS = 20

private const val REASON_PLAYING = "playing"

internal class HttpSocialRepository(private val context: Context) : SocialRepository {

    private val dir = File(context.filesDir, "social").apply { mkdirs() }
    private val accountFile = File(dir, "account.json")
    private val imagesDir = File(dir, "images").apply { mkdirs() }
    private val profilesDir = File(dir, "profiles").apply { mkdirs() }
    private val recommendationsFile = File(dir, "recommendations.json")
    private val mutex = Mutex()

    private val _account = MutableStateFlow(runCatching { readAccount() }.getOrNull())
    override val account: StateFlow<SocialAccount?> = _account.asStateFlow()

    // Versão do perfil de cada amigo (chega com a lista de amigos).
    private val knownVersions = java.util.concurrent.ConcurrentHashMap<String, Long>()

    // ---- Conexão ao vivo (WebSocket) -------------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val people = MutableStateFlow<List<FriendProfile>?>(null)

    @Volatile
    private var lastNow: TrackInfo? = null
    private val queuedListens = ArrayDeque<Pair<TrackInfo, Long>>()

    private val _friends = MutableStateFlow(FriendsState())
    override val friends: StateFlow<FriendsState> = _friends.asStateFlow()

    private val _recommendations = MutableStateFlow(runCatching { readRecommendations() }.getOrDefault(emptyList()))
    override val recommendations: StateFlow<List<Recommendation>> = _recommendations.asStateFlow()

    private val _reactions = MutableSharedFlow<Reaction>(extraBufferCapacity = 16)
    override val reactions: SharedFlow<Reaction> = _reactions.asSharedFlow()

    private val link = SocialLink(
        scope = scope,
        url = SOCKET,
        authorization = { _account.value?.let { "Bearer ${it.id}.${it.token}" } },
        onOpen = ::onLinkOpen,
        onEvent = ::onLinkEvent,
    )

    override val live: StateFlow<SocialLive> = combine(link.status, people) { status, list ->
        SocialLive(status, list.takeIf { status == LinkStatus.Online })
    }.stateIn(scope, SharingStarted.Eagerly, SocialLive())

    override fun keepConnected(reason: String, wanted: Boolean) = link.want(reason, wanted)

    init {
        // O chat saiu (0.30): conversas, cursores e push antigos são apagados do aparelho.
        listOf("chats", "cursors.json", "cursors.json.tmp", "push.txt").forEach { File(dir, it).deleteRecursively() }
    }

    /** Conectou: pede as recomendações que faltam e manda o "ouvindo agora" e as músicas ouvidas. */
    private suspend fun onLinkOpen() {
        link.send(JSONObject().put("t", "hello").put("since", recommendationsCursor()))
        if (_account.value?.sharing == true) {
            link.send(JSONObject().put("t", "now").put("track", lastNow?.let(::trackToJson) ?: JSONObject.NULL))
            val listens = synchronized(queuedListens) { queuedListens.toList().also { queuedListens.clear() } }
            listens.forEach { (track, at) ->
                link.send(JSONObject().put("t", "listened").put("track", trackToJson(track)).put("at", at))
            }
        }
    }

    private suspend fun onLinkEvent(event: JSONObject) {
        when (event.optString("t")) {
            "hello" -> event.optJSONObject("me")?.let { me ->
                val current = _account.value ?: return
                val updated = current.withServerFields(me)
                if (updated != current) saveAccount(updated)
            }
            "activity" -> {
                val list = event.optJSONArray("people") ?: JSONArray()
                val profiles = (0 until list.length()).map { parseProfile(list.getJSONObject(it)) }
                profiles.forEach { knownVersions[it.user.id] = it.version }
                people.value = profiles
                _friends.value = _friends.value.copy(friends = profiles)
            }
            "person" -> event.optJSONObject("profile")?.let { json ->
                val profile = parseProfile(json)
                knownVersions[profile.user.id] = profile.version
                val updated = withPerson(people.value.orEmpty(), profile)
                people.value = updated
                _friends.value = _friends.value.copy(friends = updated)
            }
            // A lista de amigos ou os pedidos mudaram: busca de novo.
            "friends" -> refreshFriends()
            "rec" -> receiveRecommendations(event.optJSONArray("items") ?: JSONArray())
            "reaction" -> {
                val from = event.optJSONObject("from")?.let(::parseUser) ?: return
                _reactions.tryEmit(
                    Reaction(from, event.optString("emoji"), event.str("title"), event.optLong("at").takeIf { it > 0 } ?: System.currentTimeMillis()),
                )
            }
        }
    }

    // ---- Perfil ----------------------------------------------------------

    override suspend fun checkHandle(handle: String): HandleCheck {
        if (!isValidHandle(handle)) return HandleCheck.Invalid
        val response = call("GET", "/handles/" + encode(normalizeHandle(handle)), auth = false) ?: return HandleCheck.Offline
        // Resposta que não veio do nosso servidor (fora do ar, ainda não publicado): não dá para saber.
        if (response.code != 200 || !response.body.has("available")) return HandleCheck.Offline
        return when {
            !response.body.optBoolean("valid", true) -> HandleCheck.Invalid
            response.body.optBoolean("available") -> HandleCheck.Available
            else -> HandleCheck.Taken
        }
    }

    override suspend fun register(name: String, handle: String): SocialResult<SocialAccount> {
        val response = call(
            "POST",
            "/register",
            JSONObject().put("name", name.trim()).put("handle", normalizeHandle(handle)),
            auth = false,
        ) ?: return SocialResult.Offline
        return when (response.code) {
            200 -> {
                val json = response.body
                val created = SocialAccount(
                    id = json.getString("id"),
                    handle = json.getString("handle"),
                    name = json.getString("name"),
                    token = json.getString("token"),
                    sharing = false,
                )
                saveAccount(created)
                link.accountChanged()
                SocialResult.Ok(created)
            }
            409 -> SocialResult.HandleTaken
            400 -> SocialResult.Invalid
            else -> SocialResult.Offline
        }
    }

    override suspend fun updateProfile(name: String?, sharing: Boolean?): SocialResult<SocialAccount> {
        val current = _account.value ?: return SocialResult.Invalid
        val body = JSONObject()
        name?.let { body.put("name", it.trim()) }
        sharing?.let { body.put("sharing", it) }
        val response = call("PATCH", "/me", body) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        val updated = current.withServerFields(response.body)
        saveAccount(updated)
        // Ligou agora com música tocando: já avisa os amigos.
        if (updated.sharing && !current.sharing) {
            lastNow?.let { link.send(JSONObject().put("t", "now").put("track", trackToJson(it))) }
        }
        link.want(REASON_PLAYING, updated.sharing && lastNow != null)
        return SocialResult.Ok(updated)
    }

    override suspend fun updateBio(bio: String): SocialResult<SocialAccount> {
        val current = _account.value ?: return SocialResult.Invalid
        val response = call("PATCH", "/me", JSONObject().put("bio", bio.trim())) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        val updated = current.withServerFields(response.body)
        saveAccount(updated)
        return SocialResult.Ok(updated)
    }

    override suspend fun updateColor(color: String): SocialResult<SocialAccount> {
        val current = _account.value ?: return SocialResult.Invalid
        val clean = cleanProfileColor(color) ?: return SocialResult.Invalid
        val response = call("PATCH", "/me", JSONObject().put("color", clean)) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        val updated = current.withServerFields(response.body)
        saveAccount(updated)
        return SocialResult.Ok(updated)
    }

    override suspend fun setProfileImage(kind: ProfileImage, uri: String?): SocialResult<SocialAccount> {
        val current = _account.value ?: return SocialResult.Invalid
        val response = if (uri == null) {
            call("DELETE", "/me/image/" + kind.path)
        } else {
            val jpeg = withContext(Dispatchers.Default) { prepareProfileImage(context, uri, kind) }
                ?: return SocialResult.Invalid
            raw("PUT", "/me/image/" + kind.path, jpeg)?.let { (code, bytes) ->
                if (code == 200) {
                    // A própria imagem já fica guardada: aparece na hora, sem baixar de volta.
                    val body = JSONObject(String(bytes))
                    val version = body.optLong(kind.path)
                    if (version > 0) cacheImage(current.id, kind, version, jpeg)
                    Response(code, body)
                } else {
                    Response(code, JSONObject())
                }
            }
        } ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        val updated = current.withServerFields(response.body)
        saveAccount(updated)
        return SocialResult.Ok(updated)
    }

    override suspend fun profileImage(userId: String, kind: ProfileImage, version: Long): ByteArray? {
        if (version <= 0) return null
        val file = imageFile(userId, kind, version)
        if (file.exists()) return withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull() }
        val (code, bytes) = raw("GET", "/users/${encode(userId)}/image/${kind.path}", null) ?: return null
        if (code != 200 || bytes.isEmpty()) return null
        cacheImage(userId, kind, version, bytes)
        return bytes
    }

    private fun imageFile(userId: String, kind: ProfileImage, version: Long) =
        File(imagesDir, "${userId}_${kind.path}_$version.jpg")

    /** Guarda a versão nova e apaga as antigas da mesma pessoa. */
    private suspend fun cacheImage(userId: String, kind: ProfileImage, version: Long, bytes: ByteArray) =
        withContext(Dispatchers.IO) {
            runCatching {
                imagesDir.listFiles { file -> file.name.startsWith("${userId}_${kind.path}_") }?.forEach { it.delete() }
                imageFile(userId, kind, version).writeBytes(bytes)
            }
        }
    override suspend fun deleteAccount(): SocialResult<Unit> {
        val response = call("DELETE", "/me") ?: return SocialResult.Offline
        if (response.code != 200 && response.code != 401) return SocialResult.Invalid
        withContext(Dispatchers.IO) {
            mutex.withLock {
                profilesDir.deleteRecursively()
                profilesDir.mkdirs()
                recommendationsFile.delete()
                accountFile.delete()
                _account.value = null
            }
        }
        people.value = null
        _friends.value = FriendsState()
        _recommendations.value = emptyList()
        recsCursor = 0
        link.accountChanged()
        return SocialResult.Ok(Unit)
    }

    override suspend fun findByHandle(handle: String): SocialResult<FriendProfile> =
        profileAt("/users/by-handle/" + encode(normalizeHandle(handle)))

    override suspend fun profile(userId: String): SocialResult<FriendProfile> = profileAt("/users/" + encode(userId))

    override suspend fun profileCard(userId: String): FriendProfile? {
        val cached = withContext(Dispatchers.IO) { runCatching { readProfileCard(userId) }.getOrNull() }
        val known = knownVersions[userId]
        // Guardado e na versão que o servidor disse (ou sem saber a versão ainda): usa o guardado.
        if (cached != null && (known == null || known == cached.version)) return cached
        return (profile(userId) as? SocialResult.Ok)?.value ?: cached
    }

    private fun profileCardFile(userId: String) = File(profilesDir, userId.filter { it.isLetterOrDigit() } + ".json")

    private fun readProfileCard(userId: String): FriendProfile? {
        val file = profileCardFile(userId)
        if (!file.exists()) return null
        return parseProfile(JSONObject(file.readText())).copy(sharing = false, now = null, recent = emptyList())
    }

    /** Guarda só a parte do perfil que não muda a toda hora (nome, foto, capa, cor, descrição). */
    private suspend fun saveProfileCard(profile: FriendProfile) = withContext(Dispatchers.IO) {
        runCatching {
            val json = userToJson(profile.user)
                .put("bio", profile.bio)
                .put("avatar", profile.avatarVersion)
                .put("banner", profile.bannerVersion)
                .put("color", profile.color)
                .put("v", profile.version)
                .put("links", linksToJson(profile.links))
            profileCardFile(profile.user.id).writeText(json.toString())
        }
    }

    private suspend fun profileAt(path: String): SocialResult<FriendProfile> {
        val response = call("GET", path) ?: return SocialResult.Offline
        return when (response.code) {
            200 -> SocialResult.Ok(parseProfile(response.body).also { saveProfileCard(it) })
            404 -> SocialResult.NotFound
            else -> SocialResult.Invalid
        }
    }


    // ---- Amizades --------------------------------------------------------

    override suspend fun refreshFriends(): SocialResult<FriendsState> {
        val response = call("GET", "/friends") ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        fun users(name: String): List<SocialUser> {
            val array = response.body.optJSONArray(name) ?: JSONArray()
            return (0 until array.length()).map { parseUser(array.getJSONObject(it)) }
        }
        val list = response.body.optJSONArray("friends") ?: JSONArray()
        val profiles = (0 until list.length()).map { parseProfile(list.getJSONObject(it)) }
        profiles.forEach { knownVersions[it.user.id] = it.version }
        val state = FriendsState(profiles, users("incoming"), users("outgoing"))
        _friends.value = state
        if (link.isOnline) people.value = profiles
        return SocialResult.Ok(state)
    }

    override suspend fun addFriend(userId: String): SocialResult<Relation> = changeFriend("POST", userId)

    override suspend fun removeFriend(userId: String): SocialResult<Relation> = changeFriend("DELETE", userId)

    private suspend fun changeFriend(method: String, userId: String): SocialResult<Relation> {
        val response = call(method, "/friends/" + encode(userId)) ?: return SocialResult.Offline
        if (response.code != 200) return if (response.code == 404) SocialResult.NotFound else SocialResult.Invalid
        refreshFriends()
        return SocialResult.Ok(
            when (response.body.optString("relation")) {
                "friend" -> Relation.Friend
                "sent" -> Relation.Sent
                else -> Relation.None
            },
        )
    }

    override suspend fun activity(): SocialResult<List<FriendProfile>> {
        // Conectado: a lista já chega ao vivo.
        live.value.people?.let { return SocialResult.Ok(it) }
        return when (val result = refreshFriends()) {
            is SocialResult.Ok -> SocialResult.Ok(result.value.friends)
            SocialResult.Offline -> SocialResult.Offline
            else -> SocialResult.Invalid
        }
    }

    // ---- Redes no perfil -------------------------------------------------

    override suspend fun updateLinks(
        links: Map<SocialNetwork, String>,
        numbers: Map<NumberNetwork, String>,
        shareNumbers: Boolean,
    ): SocialResult<SocialAccount> {
        val current = _account.value ?: return SocialResult.Invalid
        val body = JSONObject()
            .put("links", linksToJson(links))
            .put("shareNumbers", shareNumbers)
            .put("numbers", if (shareNumbers) numbersToJson(numbers) else JSONObject())
        val response = call("PATCH", "/me", body) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        // O número fica guardado no aparelho mesmo desligado (para não precisar digitar de novo).
        val updated = current.withServerFields(response.body).copy(numbers = numbers, shareNumbers = shareNumbers)
        saveAccount(updated)
        return SocialResult.Ok(updated)
    }

    // ---- Recomendações e reações ----------------------------------------

    override suspend fun recommend(to: List<String>, card: MusicCard): SocialResult<Unit> {
        if (to.isEmpty()) return SocialResult.Invalid
        val cid = UUID.randomUUID().toString().replace("-", "")
        val event = JSONObject().put("t", "recommend").put("cid", cid).put("to", JSONArray(to)).put("card", cardToJson(card))
        if (link.send(event)) return SocialResult.Ok(Unit)
        // Sem conexão ao vivo: pelo HTTP.
        val response = call(
            "POST",
            "/recommendations",
            JSONObject().put("cid", cid).put("to", JSONArray(to)).put("card", cardToJson(card)),
        ) ?: return SocialResult.Offline
        return if (response.code == 200) SocialResult.Ok(Unit) else SocialResult.Invalid
    }

    /** Guarda as recomendações que chegaram (sem repetir) e até onde já leu. */
    private suspend fun receiveRecommendations(items: JSONArray) {
        val incoming = (0 until items.length()).mapNotNull { index ->
            val json = items.getJSONObject(index)
            val card = parseCard(json.optJSONObject("card")) ?: return@mapNotNull null
            Recommendation(
                id = json.getString("id"),
                from = json.optJSONObject("sender")?.let(::parseUser),
                card = card,
                atMs = json.optLong("at"),
            ) to json.optLong("seq")
        }
        if (incoming.isEmpty()) return
        val cursor = maxOf(recommendationsCursor(), incoming.maxOf { it.second })
        updateRecommendations(cursor) { stored ->
            mergeRecommendations(stored, incoming.map { it.first }, System.currentTimeMillis())
        }
    }

    override suspend fun markRecommendationsSeen() {
        if (_recommendations.value.all { it.seen }) return
        updateRecommendations(recommendationsCursor()) { list -> list.map { it.copy(seen = true) } }
    }

    override suspend fun removeRecommendation(id: String) {
        updateRecommendations(recommendationsCursor()) { list -> list.filterNot { it.id == id } }
    }

    override suspend fun react(toUserId: String, emoji: String, title: String?): Boolean {
        if (emoji !in REACTION_EMOJIS) return false
        return link.send(
            JSONObject().put("t", "react").put("to", toUserId).put("emoji", emoji).put("title", title ?: JSONObject.NULL),
        )
    }

    @Volatile
    private var recsCursor: Long = -1

    private fun recommendationsCursor(): Long {
        if (recsCursor < 0) {
            recsCursor = runCatching { JSONObject(recommendationsFile.readText()).optLong("since") }.getOrDefault(0L)
        }
        return recsCursor
    }

    private suspend fun updateRecommendations(cursor: Long, change: (List<Recommendation>) -> List<Recommendation>) =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val next = change(_recommendations.value)
                _recommendations.value = next
                recsCursor = cursor
                runCatching {
                    val array = JSONArray()
                    next.forEach { rec ->
                        array.put(
                            JSONObject()
                                .put("id", rec.id)
                                .put("at", rec.atMs)
                                .put("seen", rec.seen)
                                .put("card", cardToJson(rec.card))
                                .apply { rec.from?.let { put("sender", userToJson(it)) } },
                        )
                    }
                    val temp = File(dir, "recommendations.json.tmp")
                    temp.writeText(JSONObject().put("since", cursor).put("items", array).toString())
                    temp.renameTo(recommendationsFile)
                }
            }
        }

    private fun readRecommendations(): List<Recommendation> {
        if (!recommendationsFile.exists()) return emptyList()
        val array = JSONObject(recommendationsFile.readText()).optJSONArray("items") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val json = array.getJSONObject(index)
            val card = parseCard(json.optJSONObject("card")) ?: return@mapNotNull null
            Recommendation(
                id = json.getString("id"),
                from = json.optJSONObject("sender")?.let(::parseUser),
                card = card,
                atMs = json.optLong("at"),
                seen = json.optBoolean("seen"),
            )
        }
    }

    override suspend fun block(userId: String): SocialResult<Unit> {
        val response = call("POST", "/blocks", JSONObject().put("id", userId)) ?: return SocialResult.Offline
        return if (response.code == 200) SocialResult.Ok(Unit) else SocialResult.Invalid
    }

    override suspend fun unblock(userId: String): SocialResult<Unit> {
        val response = call("DELETE", "/blocks/" + encode(userId)) ?: return SocialResult.Offline
        return if (response.code == 200) SocialResult.Ok(Unit) else SocialResult.Invalid
    }


    override suspend fun publishNowPlaying(track: TrackInfo?) {
        lastNow = track
        val sharing = _account.value?.sharing == true
        // Tocando com o "ouvindo agora" ligado: fica conectado mesmo com o app em segundo plano.
        link.want(REASON_PLAYING, sharing && track != null)
        if (!sharing) return
        // Sem conexão agora: vai quando conectar (onLinkOpen). Nunca grava no banco do servidor.
        link.send(JSONObject().put("t", "now").put("track", track?.let(::trackToJson) ?: JSONObject.NULL))
    }

    override suspend fun publishListened(track: TrackInfo) {
        if (_account.value?.sharing != true) return
        val at = System.currentTimeMillis()
        val event = JSONObject().put("t", "listened").put("track", trackToJson(track)).put("at", at)
        if (!link.send(event)) {
            synchronized(queuedListens) {
                queuedListens.addLast(track to at)
                while (queuedListens.size > MAX_QUEUED_LISTENS) queuedListens.removeFirst()
            }
        }
    }

    // ---- Guardar no aparelho --------------------------------------------

    private fun readAccount(): SocialAccount? {
        if (!accountFile.exists()) return null
        val json = JSONObject(accountFile.readText())
        return SocialAccount(
            id = json.getString("id"),
            handle = json.getString("handle"),
            name = json.getString("name"),
            token = json.getString("token"),
            sharing = json.optBoolean("sharing"),
            bio = json.optString("bio"),
            avatarVersion = json.optLong("avatar"),
            bannerVersion = json.optLong("banner"),
            color = json.optString("color"),
            links = parseLinks(json.optJSONObject("links")),
            numbers = parseNumbers(json.optJSONObject("numbers")),
            shareNumbers = json.optBoolean("shareNumbers"),
        )
    }

    private suspend fun saveAccount(account: SocialAccount) {
        withContext(Dispatchers.IO) {
            val json = JSONObject()
                .put("id", account.id)
                .put("handle", account.handle)
                .put("name", account.name)
                .put("token", account.token)
                .put("sharing", account.sharing)
                .put("bio", account.bio)
                .put("avatar", account.avatarVersion)
                .put("banner", account.bannerVersion)
                .put("color", account.color)
                .put("links", linksToJson(account.links))
                .put("numbers", numbersToJson(account.numbers))
                .put("shareNumbers", account.shareNumbers)
            val temp = File(dir, "account.json.tmp")
            temp.writeText(json.toString())
            temp.renameTo(accountFile)
        }
        _account.value = account
    }

    // ---- Rede ------------------------------------------------------------

    private class Response(val code: Int, val body: JSONObject)

    /** null = sem internet (ou servidor fora do ar). */
    private suspend fun call(method: String, path: String, body: JSONObject? = null, auth: Boolean = true): Response? =
        withContext(Dispatchers.IO) {
            val connection = try {
                URL(SERVER + path).openConnection() as HttpURLConnection
            } catch (_: IOException) {
                return@withContext null
            }
            try {
                connection.requestMethod = method
                connection.connectTimeout = 10_000
                connection.readTimeout = 15_000
                connection.setRequestProperty("Accept", "application/json")
                if (auth) {
                    val account = _account.value ?: return@withContext Response(401, JSONObject())
                    connection.setRequestProperty("Authorization", "Bearer ${account.id}.${account.token}")
                }
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray()) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                restingUntil(
                    code,
                    json?.optString("error")?.takeIf { it.isNotEmpty() },
                    json?.optLong("until") ?: 0L,
                    System.currentTimeMillis(),
                )?.let(link::markResting)
                Response(code, json ?: JSONObject())
            } catch (_: IOException) {
                null
            } finally {
                connection.disconnect()
            }
        }

    /** Envia ou recebe bytes (imagens). null = sem internet. */
    private suspend fun raw(method: String, path: String, payload: ByteArray?): Pair<Int, ByteArray>? =
        withContext(Dispatchers.IO) {
            val account = _account.value ?: return@withContext null
            val connection = try {
                URL(SERVER + path).openConnection() as HttpURLConnection
            } catch (_: IOException) {
                return@withContext null
            }
            try {
                connection.requestMethod = method
                connection.connectTimeout = 10_000
                connection.readTimeout = 20_000
                connection.setRequestProperty("Authorization", "Bearer ${account.id}.${account.token}")
                if (payload != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", when {
                            isGif(payload) -> "image/gif"
                            isWebp(payload) -> "image/webp"
                            else -> "image/jpeg"
                        },
                    )
                    connection.setFixedLengthStreamingMode(payload.size)
                    connection.outputStream.use { it.write(payload) }
                }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                code to (stream?.use { it.readBytes() } ?: ByteArray(0))
            } catch (_: IOException) {
                null
            } finally {
                connection.disconnect()
            }
        }

    private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}

private fun SocialAccount.withServerFields(json: JSONObject) = copy(
    name = json.optString("name", name),
    sharing = json.optBoolean("sharing", sharing),
    bio = if (json.has("bio")) json.optString("bio") else bio,
    avatarVersion = if (json.has("avatar")) json.optLong("avatar") else avatarVersion,
    bannerVersion = if (json.has("banner")) json.optLong("banner") else bannerVersion,
    color = if (json.has("color")) json.optString("color") else color,
    links = if (json.has("links")) parseLinks(json.optJSONObject("links")) else links,
    shareNumbers = if (json.has("shareNumbers")) json.optBoolean("shareNumbers") else shareNumbers,
)

// ---- JSON (formato combinado com social/worker.js) ---------------------

private fun parseUser(json: JSONObject) =
    SocialUser(json.optString("id"), json.optString("handle"), json.optString("name"))

private fun userToJson(user: SocialUser) =
    JSONObject().put("id", user.id).put("handle", user.handle).put("name", user.name)

private fun JSONObject.str(name: String): String? = optString(name).takeIf { it.isNotBlank() && it != "null" }

private fun parseTrack(json: JSONObject?): TrackInfo? {
    val title = json?.str("title") ?: return null
    return TrackInfo(title, json.str("artist"), json.str("album"))
}

private fun trackToJson(track: TrackInfo) = JSONObject()
    .put("title", track.title)
    .put("artist", track.artist ?: JSONObject.NULL)
    .put("album", track.album ?: JSONObject.NULL)

private fun parseProfile(json: JSONObject): FriendProfile {
    val recent = json.optJSONArray("recent") ?: JSONArray()
    return FriendProfile(
        user = parseUser(json),
        sharing = json.optBoolean("sharing"),
        now = parseTrack(json.optJSONObject("now")),
        updatedMs = json.optLong("updated").takeIf { it > 0 },
        recent = (0 until recent.length()).mapNotNull { index ->
            val item = recent.getJSONObject(index)
            parseTrack(item)?.let { ListenedTrack(it, item.optLong("at")) }
        },
        blockedByMe = json.optBoolean("blockedByMe"),
        bio = json.optString("bio").takeIf { it != "null" }.orEmpty(),
        avatarVersion = json.optLong("avatar"),
        bannerVersion = json.optLong("banner"),
        color = json.optString("color").takeIf { it != "null" }.orEmpty(),
        version = json.optLong("v"),
        links = parseLinks(json.optJSONObject("links")),
        numbers = parseNumbers(json.optJSONObject("numbers")),
        relation = when (json.optString("relation")) {
            "self" -> Relation.Self
            "friend" -> Relation.Friend
            "sent" -> Relation.Sent
            "received" -> Relation.Received
            else -> Relation.None
        },
    )
}


private fun parseLinks(json: JSONObject?): Map<SocialNetwork, String> {
    if (json == null) return emptyMap()
    return json.keys().asSequence().mapNotNull { key ->
        val network = SocialNetwork.fromKey(key) ?: return@mapNotNull null
        json.str(key)?.let { network to it }
    }.toMap()
}

private fun linksToJson(links: Map<SocialNetwork, String>) =
    JSONObject().apply { links.forEach { (network, handle) -> if (handle.isNotBlank()) put(network.key, handle) } }

private fun parseNumbers(json: JSONObject?): Map<NumberNetwork, String> {
    if (json == null) return emptyMap()
    return json.keys().asSequence().mapNotNull { key ->
        val network = NumberNetwork.fromKey(key) ?: return@mapNotNull null
        json.str(key)?.let { network to it }
    }.toMap()
}

private fun numbersToJson(numbers: Map<NumberNetwork, String>) =
    JSONObject().apply { numbers.forEach { (network, digits) -> if (digits.isNotBlank()) put(network.key, digits) } }

private fun cardToJson(card: MusicCard) = JSONObject()
    .put("kind", card.kind.name.lowercase())
    .put("title", card.title)
    .put("artist", card.artist ?: JSONObject.NULL)
    .put("album", card.album ?: JSONObject.NULL)
    .put("coverUrl", card.coverUrl ?: JSONObject.NULL)
    .put("link", card.link ?: JSONObject.NULL)

private fun parseCard(json: JSONObject?): MusicCard? {
    val title = json?.str("title") ?: return null
    return MusicCard(
        kind = when (json.optString("kind")) {
            "album" -> MusicKind.Album
            "artist" -> MusicKind.Artist
            else -> MusicKind.Track
        },
        title = title,
        artist = json.str("artist"),
        album = json.str("album"),
        coverUrl = json.str("coverUrl"),
        link = json.str("link"),
    )
}
