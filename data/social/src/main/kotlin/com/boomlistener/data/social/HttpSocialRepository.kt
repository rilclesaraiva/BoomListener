// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.unifiedpush.android.connector.UnifiedPush
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
private const val SOCKET = "wss://boomlistener-social.saraivarilcle.workers.dev/v1/ws?v=2"

/** Músicas ouvidas sem conexão, guardadas para mandar quando conectar. */
private const val MAX_QUEUED_LISTENS = 20

/** Sem recibo depois de ir "ao vivo": manda de novo (o outro lado ignora repetidas). */
private const val RECEIPT_TIMEOUT_MS = 60_000L

/** 24 h presa no celular sem entregar: vai para a caixa postal do servidor. */
private const val MAILBOX_AFTER_MS = 24 * 60 * 60_000L

/** Sem conexão ao vivo, o HTTP só leva para a caixa postal o que espera há mais que isto. */
private const val HTTP_FALLBACK_AFTER_MS = 10 * 60_000L

/** Um push acorda o app por este tempo (para entregar ou buscar mensagens). */
private const val PUSH_WAKE_MS = 45_000L

internal class HttpSocialRepository(private val context: Context) : SocialRepository {

    private val dir = File(context.filesDir, "social").apply { mkdirs() }
    private val accountFile = File(dir, "account.json")
    private val chatsDir = File(dir, "chats").apply { mkdirs() }
    private val imagesDir = File(dir, "images").apply { mkdirs() }
    private val profilesDir = File(dir, "profiles").apply { mkdirs() }

    // Versão do perfil de cada pessoa conhecida (chega ao conectar).
    private val knownVersions = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val mutex = Mutex()

    private val _account = MutableStateFlow(runCatching { readAccount() }.getOrNull())
    override val account: StateFlow<SocialAccount?> = _account.asStateFlow()

    private val store = MutableStateFlow<Map<String, StoredConversation>?>(null)

    @Volatile
    private var openConversation: String? = null

    // ---- Conexão ao vivo (WebSocket) -------------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val people = MutableStateFlow<List<FriendProfile>?>(null)

    @Volatile
    private var lastNow: TrackInfo? = null
    private val queuedListens = ArrayDeque<Pair<TrackInfo, Long>>()

    // Até onde já lemos a caixa postal (anel) e o anel de cada grupo.
    private val cursorsFile = File(dir, "cursors.json")
    @Volatile
    private var cursors = runCatching { readCursors() }.getOrDefault(Cursors())

    // Mensagens que foram "ao vivo" e esperam o recibo de entrega (id → quando).
    private val awaitingReceipt = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private val _incoming = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    override val incoming: SharedFlow<ChatMessage> = _incoming.asSharedFlow()

    // Push (UnifiedPush): o endereço que o distribuidor (ex.: ntfy) deu.
    private val pushFile = File(dir, "push.txt")
    private val _pushEnabled = MutableStateFlow(pushFile.exists() && UnifiedPush.getAckDistributor(context) != null)
    override val pushEnabled: StateFlow<Boolean> = _pushEnabled.asStateFlow()
    private var wakeJob: Job? = null

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
        scope.launch { tickWhileOnline() }
        // O UnifiedPush pede para registrar de novo de tempos em tempos (o endereço pode mudar).
        if (_pushEnabled.value) runCatching { UnifiedPush.register(context, messageForDistributor = "Boom, listener!") }
    }

    /**
     * Conectou: diz até onde já leu (o servidor manda só o que falta e cutuca
     * quem tem mensagens para você), e manda o que ficou esperando: mensagens,
     * "ouvindo agora" e músicas ouvidas.
     */
    private suspend fun onLinkOpen() {
        ensureLoaded()
        link.send(
            JSONObject()
                .put("t", "hello")
                .put("v", 2)
                .put("since", cursors.since)
                .put("groups", JSONObject(cursors.groups))
                .put("push", currentPushUrl().orEmpty()),
        )
        resendWaiting { true }
        if (_account.value?.sharing == true) {
            link.send(JSONObject().put("t", "now").put("track", lastNow?.let(::trackToJson) ?: JSONObject.NULL))
            val listens = synchronized(queuedListens) { queuedListens.toList().also { queuedListens.clear() } }
            listens.forEach { (track, at) ->
                link.send(JSONObject().put("t", "listened").put("track", trackToJson(track)).put("at", at))
            }
        }
    }

    /** Manda de novo as mensagens suas que ainda esperam ([filter] escolhe quais). */
    private fun resendWaiting(filter: (ChatMessage) -> Boolean) {
        val now = System.currentTimeMillis()
        store.value.orEmpty().values.flatMap { it.messages }
            .filter { it.sender == null && it.delivery == Delivery.Waiting && filter(it) }
            .forEach { sendOverLink(it, mailbox = now - it.sentMs > MAILBOX_AFTER_MS) }
    }

    /** Enquanto conectado: reenvia o que ficou sem recibo e manda para a caixa postal o que passou de 24 h. */
    private suspend fun tickWhileOnline() {
        while (true) {
            delay(RECEIPT_TIMEOUT_MS)
            if (!link.isOnline) continue
            val now = System.currentTimeMillis()
            val stale = awaitingReceipt.filterValues { now - it > RECEIPT_TIMEOUT_MS }.keys
            resendWaiting { it.id in stale || now - it.sentMs > MAILBOX_AFTER_MS }
        }
    }

    private suspend fun onLinkEvent(event: JSONObject) {
        when (event.optString("t")) {
            "hello" -> event.optJSONObject("me")?.let { me ->
                val current = _account.value ?: return
                val updated = current.withServerFields(me)
                if (updated != current) saveAccount(updated)
            }
            "inbox" -> {
                val array = event.optJSONArray("messages") ?: JSONArray()
                receive(array) { ids -> link.send(JSONObject().put("t", "ack").put("ids", JSONArray(ids))) }
                // Recibo "entregue" para quem mandou ao vivo (conversas particulares).
                val receipts = JSONArray()
                for (index in 0 until array.length()) {
                    val json = array.getJSONObject(index)
                    val senderId = json.optJSONObject("sender")?.optString("id").orEmpty()
                    if (json.optBoolean("live") && senderId.isNotEmpty() && json.optString("conv").startsWith(DM_PREFIX)) {
                        receipts.put(JSONObject().put("id", json.getString("id")).put("to", senderId))
                    }
                }
                if (receipts.length() > 0) link.send(JSONObject().put("t", "delivered").put("items", receipts))
            }
            "versions" -> {
                val users = event.optJSONObject("users") ?: JSONObject()
                users.keys().forEach { id -> knownVersions[id] = users.optLong(id) }
            }
            "activity" -> {
                val list = event.optJSONArray("people") ?: JSONArray()
                people.value = (0 until list.length()).map { parseProfile(list.getJSONObject(it)) }
            }
            "person" -> event.optJSONObject("profile")?.let { json ->
                people.value = withPerson(people.value.orEmpty(), parseProfile(json))
            }
            "sent" -> {
                val cid = event.optString("cid")
                if (event.optInt("status") != 200 || cid.isEmpty()) return
                val delivery = deliveryAfterSend(event.optString("how"))
                if (delivery == null) {
                    // Foi ao vivo: espera o recibo (e manda de novo se não vier).
                    awaitingReceipt[cid] = System.currentTimeMillis()
                } else {
                    setDelivery(listOf(cid), delivery)
                }
            }
            "delivered" -> {
                val list = event.optJSONArray("ids") ?: JSONArray()
                setDelivery((0 until list.length()).map { list.getString(it) }, Delivery.Delivered)
            }
            // A pessoa apareceu: manda o que estava esperando por ela.
            "poke" -> {
                val conversation = DM_PREFIX + event.optString("to")
                resendWaiting { it.conversationId == conversation }
            }
        }
    }

    private fun sendOverLink(message: ChatMessage, mailbox: Boolean = false): Boolean = link.send(
        JSONObject()
            .put("t", "send")
            .put("v", 2)
            .put("cid", message.id)
            .put("at", message.sentMs)
            .put("mailbox", mailbox)
            .put("conv", message.conversationId)
            .put("body", bodyToJson(message.body)),
    )

    /** Muda o estado das mensagens suas [ids] (só para a frente: entregue não volta a "aguardando"). */
    private suspend fun setDelivery(ids: List<String>, delivery: Delivery) {
        if (ids.isEmpty()) return
        ids.forEach { awaitingReceipt.remove(it) }
        val wanted = ids.toHashSet()
        update { map ->
            map.mapValues { (_, stored) ->
                if (stored.messages.none { it.id in wanted }) {
                    stored
                } else {
                    val messages = stored.messages.map { message ->
                        if (message.id in wanted && advances(message.delivery, delivery)) message.copy(delivery = delivery) else message
                    }
                    stored.copy(conversation = stored.conversation.copy(lastMessage = messages.lastOrNull()), messages = messages)
                }
            }
        }
    }

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
                chatsDir.deleteRecursively()
                chatsDir.mkdirs()
                profilesDir.deleteRecursively()
                profilesDir.mkdirs()
                accountFile.delete()
                store.value = emptyMap()
                _account.value = null
            }
        }
        people.value = null
        cursors = Cursors()
        cursorsFile.delete()
        disablePush()
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

    override suspend fun activity(): SocialResult<List<FriendProfile>> {
        // Conectado: a lista já chega ao vivo.
        live.value.people?.let { return SocialResult.Ok(it) }
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
        // O id nasce aqui e vai junto: reenviar nunca duplica do outro lado.
        val local = ChatMessage(
            id = UUID.randomUUID().toString().replace("-", ""),
            conversationId = conversationId,
            sender = null,
            body = body,
            sentMs = System.currentTimeMillis(),
            delivery = Delivery.Waiting,
        )
        update { map -> map.withMessage(local) }
        // Fica no seu celular até a outra pessoa aparecer. Conectado, já tenta
        // agora; sem conexão, vai quando conectar.
        sendOverLink(local)
        return SocialResult.Ok(Unit)
    }

    /** Sem conexão ao vivo há muito tempo: manda pelo HTTP direto para a caixa postal. */
    private suspend fun deliver(message: ChatMessage): SocialResult<Unit> {
        val response = call(
            "POST",
            "/messages",
            JSONObject()
                .put("v", 2)
                .put("cid", message.id)
                .put("at", message.sentMs)
                .put("conv", message.conversationId)
                .put("body", bodyToJson(message.body)),
        ) ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        setDelivery(listOf(message.id), deliveryAfterSend(response.body.optString("how")) ?: Delivery.Sent)
        return SocialResult.Ok(Unit)
    }

    override suspend fun sync(): SocialResult<Int> {
        if (_account.value == null) return SocialResult.Invalid
        ensureLoaded()
        // Conectado: tudo chega sozinho; só pergunta de novo o que falta.
        if (link.isOnline) {
            onLinkOpen()
            return SocialResult.Ok(0)
        }
        // Sem conexão ao vivo: o que espera há muito tempo vai para a caixa postal pelo HTTP.
        val now = System.currentTimeMillis()
        store.value.orEmpty().values.flatMap { it.messages }
            .filter { it.sender == null && it.pending && now - it.sentMs > HTTP_FALLBACK_AFTER_MS }
            .forEach { deliver(it) }

        val response = call("GET", "/inbox") ?: return SocialResult.Offline
        if (response.code != 200) return SocialResult.Invalid
        val count = receive(response.body.optJSONArray("messages") ?: JSONArray()) { ids ->
            call("POST", "/inbox/ack", JSONObject().put("ids", JSONArray(ids)))
        }
        return SocialResult.Ok(count)
    }

    /** Guarda as mensagens que chegaram e confirma ao servidor ([ack]), que então apaga. */
    private suspend fun receive(array: JSONArray, ack: suspend (List<String>) -> Unit): Int {
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
        // Só a caixa de entrada antiga precisa de "recebi" (os anéis não apagam nada).
        val ids = (0 until array.length()).map { array.getJSONObject(it) }
            .filter { it.optBoolean("legacy") || !it.has("seq") && !it.optBoolean("live") }
            .map { it.getString("id") }
        advanceCursors(array)
        if (incoming.isNotEmpty()) {
            val knownAll = store.value.orEmpty().values.flatMapTo(HashSet()) { stored -> stored.messages.map { it.id } }
            update { map -> mergeIncoming(map, incoming, openConversation) }
            incoming.filter { it.id !in knownAll }.forEach { _incoming.tryEmit(it) }
            // Grupo que ainda não conhecemos (ex.: reinstalou o app): busca o nome.
            store.value.orEmpty().values
                .filter { it.conversation.isGroup && it.conversation.title.isBlank() }
                .forEach { refreshGroup(it.conversation.id) }
        }
        if (ids.isNotEmpty()) ack(ids)
        return incoming.size
    }

    /** Guarda até onde já leu a caixa postal e cada grupo (o servidor manda só o que falta). */
    private fun advanceCursors(array: JSONArray) {
        var since = cursors.since
        val groups = cursors.groups.toMutableMap()
        for (index in 0 until array.length()) {
            val json = array.getJSONObject(index)
            val seq = json.optLong("seq")
            if (seq <= 0) continue
            val group = json.optString("group")
            if (group.isNotEmpty()) {
                groups[group] = maxOf(groups[group] ?: 0L, seq)
            } else if (json.optBoolean("ring")) {
                since = maxOf(since, seq)
            }
        }
        val next = Cursors(since, groups)
        if (next != cursors) {
            cursors = next
            runCatching { writeCursors(next) }
        }
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

    // ---- Push (UnifiedPush, sem Google) -----------------------------------

    override fun pushDistributors(): List<String> = runCatching { UnifiedPush.getDistributors(context) }.getOrDefault(emptyList())

    override fun enablePush(distributor: String) {
        runCatching {
            UnifiedPush.saveDistributor(context, distributor)
            UnifiedPush.register(context, messageForDistributor = "Boom, listener!")
        }
    }

    override fun disablePush() {
        runCatching { UnifiedPush.unregister(context) }
        onPushEndpoint(null)
    }

    private fun currentPushUrl(): String? = runCatching { pushFile.readText().trim() }.getOrNull()?.takeIf { it.isNotEmpty() }

    /** O distribuidor deu (ou tirou) o endereço de push: guarda e avisa o servidor. */
    internal fun onPushEndpoint(url: String?) {
        if (url == null) pushFile.delete() else runCatching { pushFile.writeText(url) }
        _pushEnabled.value = url != null
        link.send(JSONObject().put("t", "push").put("url", url.orEmpty()))
    }

    /** Chegou um push ("acorde"): conecta por alguns segundos para entregar e buscar mensagens. */
    internal fun wakeFromPush() {
        link.want(REASON_PUSH, true)
        wakeJob?.cancel()
        wakeJob = scope.launch {
            delay(PUSH_WAKE_MS)
            link.want(REASON_PUSH, false)
        }
    }

    // ---- Guardar no aparelho --------------------------------------------

    private fun readCursors(): Cursors {
        if (!cursorsFile.exists()) return Cursors()
        val json = JSONObject(cursorsFile.readText())
        val groups = json.optJSONObject("groups") ?: JSONObject()
        return Cursors(json.optLong("since"), groups.keys().asSequence().associateWith { groups.optLong(it) })
    }

    private fun writeCursors(value: Cursors) {
        val json = JSONObject().put("since", value.since).put("groups", JSONObject(value.groups))
        val temp = File(dir, "cursors.json.tmp")
        temp.writeText(json.toString())
        temp.renameTo(cursorsFile)
    }

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
                    .put("delivery", message.delivery.name)
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
            delivery = json.optString("delivery").let { name -> Delivery.entries.firstOrNull { it.name == name } }
                ?: if (json.optBoolean("pending")) Delivery.Waiting else Delivery.Sent,
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
            color = json.optString("color"),
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

/** Nome, compartilhamento, descrição e versões das imagens, como o servidor respondeu. */
private const val REASON_PLAYING = "playing"
private const val REASON_PUSH = "push"

/** Até onde já lemos a caixa postal ([since]) e o anel de cada grupo. */
private data class Cursors(val since: Long = 0, val groups: Map<String, Long> = emptyMap())

private fun SocialAccount.withServerFields(json: JSONObject) = copy(
    name = json.optString("name", name),
    sharing = json.optBoolean("sharing", sharing),
    bio = if (json.has("bio")) json.optString("bio") else bio,
    avatarVersion = if (json.has("avatar")) json.optLong("avatar") else avatarVersion,
    bannerVersion = if (json.has("banner")) json.optLong("banner") else bannerVersion,
    color = if (json.has("color")) json.optString("color") else color,
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
