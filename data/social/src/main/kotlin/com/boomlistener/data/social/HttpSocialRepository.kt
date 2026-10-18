// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
 * perfil é criado aqui e o segredo fica guardado no aparelho.
 */
private const val SERVER = "https://boomlistener-social.saraivarilcle.workers.dev/v1"

internal class HttpSocialRepository(private val context: Context) : SocialRepository {

    private val dir = File(context.filesDir, "social").apply { mkdirs() }
    private val accountFile = File(dir, "account.json")
    private val chatsDir = File(dir, "chats").apply { mkdirs() }
    private val imagesDir = File(dir, "images").apply { mkdirs() }
    private val mutex = Mutex()

    private val _account = MutableStateFlow(runCatching { readAccount() }.getOrNull())
    override val account: StateFlow<SocialAccount?> = _account.asStateFlow()

    private val store = MutableStateFlow<Map<String, StoredConversation>?>(null)

    @Volatile
    private var openConversation: String? = null

    override val conversations: Flow<List<Conversation>> = store
        .onStart { ensureLoaded() }
        .map { map -> sortConversations(map.orEmpty().values.map { it.conversation }) }
        .distinctUntilChanged()

    override fun messages(conversationId: String): Flow<List<ChatMessage>> = store
        .onStart { ensureLoaded() }
        .map { map -> map?.get(conversationId)?.messages.orEmpty() }
        .distinctUntilChanged()

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
                chatsDir.deleteRecursively()
                chatsDir.mkdirs()
                accountFile.delete()
                store.value = emptyMap()
                _account.value = null
            }
        }
        return SocialResult.Ok(Unit)
    }

    override suspend fun findByHandle(handle: String): SocialResult<FriendProfile> =
        profileAt("/users/by-handle/" + encode(normalizeHandle(handle)))

    override suspend fun profile(userId: String): SocialResult<FriendProfile> = profileAt("/users/" + encode(userId))

    private suspend fun profileAt(path: String): SocialResult<FriendProfile> {
        val response = call("GET", path) ?: return SocialResult.Offline
        return when (response.code) {
            200 -> SocialResult.Ok(parseProfile(response.body))
            404 -> SocialResult.NotFound
            else -> SocialResult.Invalid
        }
    }

    override suspend fun activity(): SocialResult<List<FriendProfile>> {
        val response = call("GET", "/activity") ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        val people = response.body.optJSONArray("people") ?: JSONArray()
        return SocialResult.Ok((0 until people.length()).map { parseProfile(people.getJSONObject(it)) })
    }

    // ---- Conversas -------------------------------------------------------

    override suspend fun openDirect(user: SocialUser): String {
        val id = DM_PREFIX + user.id
        update { map ->
            val current = map[id]
            val conversation = current?.conversation?.copy(title = user.name)
                ?: Conversation(id, user.name, isGroup = false, code = null, lastMessage = null, unread = 0, updatedMs = System.currentTimeMillis())
            map + (id to StoredConversation(conversation, current?.messages.orEmpty()))
        }
        return id
    }

    override suspend fun send(conversationId: String, body: MessageBody): SocialResult<Unit> {
        val local = ChatMessage(
            id = "local-" + UUID.randomUUID(),
            conversationId = conversationId,
            sender = null,
            body = body,
            sentMs = System.currentTimeMillis(),
            pending = true,
        )
        update { map -> map.withMessage(local) }
        return deliver(local)
    }

    /** Manda ao servidor; se der certo, tira o "pendente". */
    private suspend fun deliver(message: ChatMessage): SocialResult<Unit> {
        val response = call(
            "POST",
            "/messages",
            JSONObject().put("conv", message.conversationId).put("body", bodyToJson(message.body)),
        ) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        update { map -> map.withMessage(message.copy(pending = false)) }
        return SocialResult.Ok(Unit)
    }

    override suspend fun sync(): SocialResult<Int> {
        if (_account.value == null) return SocialResult.Invalid
        ensureLoaded()
        // Primeiro reenvia o que ficou sem internet.
        store.value.orEmpty().values.flatMap { it.messages }.filter { it.pending }.forEach { deliver(it) }

        val response = call("GET", "/inbox") ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        val array = response.body.optJSONArray("messages") ?: JSONArray()
        val incoming = (0 until array.length()).mapNotNull { index ->
            val json = array.getJSONObject(index)
            val body = jsonToBody(json.optJSONObject("body")) ?: return@mapNotNull null
            ChatMessage(
                id = json.getString("id"),
                conversationId = json.getString("conv"),
                sender = json.optJSONObject("sender")?.let(::parseUser),
                body = body,
                sentMs = json.optLong("sent"),
            )
        }
        val ids = (0 until array.length()).map { array.getJSONObject(it).getString("id") }
        if (incoming.isNotEmpty()) {
            update { map -> mergeIncoming(map, incoming, openConversation) }
            // Grupo que ainda não conhecemos (ex.: reinstalou o app): busca o nome.
            store.value.orEmpty().values
                .filter { it.conversation.isGroup && it.conversation.title.isBlank() }
                .forEach { refreshGroup(it.conversation.id) }
        }
        if (ids.isNotEmpty()) call("POST", "/inbox/ack", JSONObject().put("ids", JSONArray(ids)))
        return SocialResult.Ok(incoming.size)
    }

    override suspend fun createGroup(name: String): SocialResult<Conversation> {
        val response = call("POST", "/groups", JSONObject().put("name", name.trim())) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        return SocialResult.Ok(saveGroup(response.body))
    }

    override suspend fun joinGroup(code: String): SocialResult<Conversation> {
        val response = call("POST", "/groups/join", JSONObject().put("code", code.trim())) ?: return SocialResult.Offline
        return when (response.code) {
            200 -> SocialResult.Ok(saveGroup(response.body))
            404 -> SocialResult.NotFound
            else -> SocialResult.Invalid
        }
    }

    override suspend fun groupMembers(conversationId: String): SocialResult<List<SocialUser>> {
        val groupId = conversationId.removePrefix(GROUP_PREFIX)
        val response = call("GET", "/groups/" + encode(groupId)) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.NotFound
        saveGroup(response.body)
        val members = response.body.optJSONArray("members") ?: JSONArray()
        return SocialResult.Ok((0 until members.length()).map { parseUser(members.getJSONObject(it)) })
    }

    private suspend fun refreshGroup(conversationId: String) {
        groupMembers(conversationId)
    }

    override suspend fun leaveGroup(conversationId: String): SocialResult<Unit> {
        val groupId = conversationId.removePrefix(GROUP_PREFIX)
        val response = call("POST", "/groups/" + encode(groupId) + "/leave") ?: return SocialResult.Offline
        if (response.code != 200 && response.code != 404) return SocialResult.Invalid
        deleteConversation(conversationId)
        return SocialResult.Ok(Unit)
    }

    private suspend fun saveGroup(json: JSONObject): Conversation {
        val id = GROUP_PREFIX + json.getString("id")
        var saved: Conversation? = null
        update { map ->
            val current = map[id]
            val conversation = (current?.conversation ?: Conversation(
                id = id,
                title = "",
                isGroup = true,
                code = null,
                lastMessage = null,
                unread = 0,
                updatedMs = System.currentTimeMillis(),
            )).copy(title = json.optString("name"), code = json.optString("code").ifEmpty { null })
            saved = conversation
            map + (id to StoredConversation(conversation, current?.messages.orEmpty()))
        }
        return saved!!
    }

    override suspend fun block(userId: String): SocialResult<Unit> {
        val response = call("POST", "/blocks", JSONObject().put("id", userId)) ?: return SocialResult.Offline
        return if (response.code == 200) SocialResult.Ok(Unit) else SocialResult.Invalid
    }

    override suspend fun unblock(userId: String): SocialResult<Unit> {
        val response = call("DELETE", "/blocks/" + encode(userId)) ?: return SocialResult.Offline
        return if (response.code == 200) SocialResult.Ok(Unit) else SocialResult.Invalid
    }

    override fun setOpenConversation(conversationId: String?) {
        openConversation = conversationId
    }

    override suspend fun markRead(conversationId: String) {
        val current = store.value?.get(conversationId) ?: return
        if (current.conversation.unread == 0) return
        update { map -> map + (conversationId to current.copy(conversation = current.conversation.copy(unread = 0))) }
    }

    override suspend fun deleteConversation(conversationId: String) {
        update { map -> map - conversationId }
    }

    override suspend fun publishNowPlaying(track: TrackInfo?) {
        if (_account.value?.sharing != true) return
        call("PUT", "/me/now", JSONObject().put("track", track?.let(::trackToJson) ?: JSONObject.NULL))
    }

    override suspend fun publishListened(track: TrackInfo) {
        if (_account.value?.sharing != true) return
        call("POST", "/me/recent", JSONObject().put("track", trackToJson(track)))
    }

    // ---- Guardar no aparelho --------------------------------------------

    private suspend fun ensureLoaded() {
        if (store.value != null) return
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (store.value == null) store.value = runCatching { readChats() }.getOrDefault(emptyMap())
            }
        }
    }

    /** Aplica [change] e grava as conversas que mudaram. */
    private suspend fun update(change: (Map<String, StoredConversation>) -> Map<String, StoredConversation>) {
        ensureLoaded()
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val before = store.value.orEmpty()
                val after = change(before)
                store.value = after
                (before.keys - after.keys).forEach { fileFor(it).delete() }
                after.forEach { (id, stored) -> if (before[id] != stored) runCatching { writeChat(stored) } }
            }
        }
    }

    private fun Map<String, StoredConversation>.withMessage(message: ChatMessage): Map<String, StoredConversation> {
        val current = this[message.conversationId] ?: return this
        val messages = (current.messages.filterNot { it.id == message.id } + message)
            .sortedBy { it.sentMs }
            .takeLast(MAX_STORED_MESSAGES)
        val conversation = current.conversation.copy(
            lastMessage = messages.lastOrNull(),
            updatedMs = maxOf(current.conversation.updatedMs, message.sentMs),
        )
        return this + (message.conversationId to StoredConversation(conversation, messages))
    }

    private fun fileFor(conversationId: String) = File(chatsDir, conversationId.replace(':', '_') + ".json")

    private fun readChats(): Map<String, StoredConversation> =
        chatsDir.listFiles { file -> file.extension == "json" }.orEmpty().mapNotNull { file ->
            runCatching {
                val json = JSONObject(file.readText())
                val id = json.getString("id")
                val messages = json.optJSONArray("messages") ?: JSONArray()
                val list = (0 until messages.length()).mapNotNull { index -> parseStoredMessage(id, messages.getJSONObject(index)) }
                id to StoredConversation(
                    Conversation(
                        id = id,
                        title = json.optString("title"),
                        isGroup = id.startsWith(GROUP_PREFIX),
                        code = json.optString("code").ifEmpty { null },
                        lastMessage = list.lastOrNull(),
                        unread = json.optInt("unread"),
                        updatedMs = json.optLong("updated"),
                    ),
                    list,
                )
            }.getOrNull()
        }.toMap()

    private fun writeChat(stored: StoredConversation) {
        val conversation = stored.conversation
        val messages = JSONArray()
        stored.messages.forEach { message ->
            messages.put(
                JSONObject()
                    .put("id", message.id)
                    .put("sent", message.sentMs)
                    .put("pending", message.pending)
                    .put("body", bodyToJson(message.body))
                    .apply { message.sender?.let { put("sender", userToJson(it)) } },
            )
        }
        val json = JSONObject()
            .put("id", conversation.id)
            .put("title", conversation.title)
            .put("code", conversation.code.orEmpty())
            .put("unread", conversation.unread)
            .put("updated", conversation.updatedMs)
            .put("messages", messages)
        val file = fileFor(conversation.id)
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(json.toString())
        temp.renameTo(file)
    }

    private fun parseStoredMessage(conversationId: String, json: JSONObject): ChatMessage? {
        val body = jsonToBody(json.optJSONObject("body")) ?: return null
        return ChatMessage(
            id = json.getString("id"),
            conversationId = conversationId,
            sender = json.optJSONObject("sender")?.let(::parseUser),
            body = body,
            sentMs = json.optLong("sent"),
            pending = json.optBoolean("pending"),
        )
    }

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
                Response(code, runCatching { JSONObject(text) }.getOrDefault(JSONObject()))
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
                    connection.setRequestProperty("Content-Type", "image/jpeg")
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

/** Nome, compartilhamento, descrição e versões das imagens, como o servidor respondeu. */
private fun SocialAccount.withServerFields(json: JSONObject) = copy(
    name = json.optString("name", name),
    sharing = json.optBoolean("sharing", sharing),
    bio = if (json.has("bio")) json.optString("bio") else bio,
    avatarVersion = if (json.has("avatar")) json.optLong("avatar") else avatarVersion,
    bannerVersion = if (json.has("banner")) json.optLong("banner") else bannerVersion,
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
    )
}

private fun bodyToJson(body: MessageBody): JSONObject = when (body) {
    is MessageBody.Text -> JSONObject().put("type", "text").put("text", body.text)
    is MessageBody.Music -> JSONObject()
        .put("type", "music")
        .put("kind", body.kind.name.lowercase())
        .put("title", body.title)
        .put("artist", body.artist ?: JSONObject.NULL)
        .put("album", body.album ?: JSONObject.NULL)
        .put("coverUrl", body.coverUrl ?: JSONObject.NULL)
        .put("link", body.link ?: JSONObject.NULL)
        .put("note", body.note ?: JSONObject.NULL)
}

private fun jsonToBody(json: JSONObject?): MessageBody? = when (json?.optString("type")) {
    "text" -> json.str("text")?.let { MessageBody.Text(it) }
    "music" -> json.str("title")?.let { title ->
        MessageBody.Music(
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
            note = json.str("note"),
        )
    }
    else -> null
}
