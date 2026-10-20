// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * A conexão ao vivo com o servidor social (WebSocket com hibernação no
 * servidor). Fica aberta só enquanto alguém precisa dela ([want]): o app na
 * tela ou uma música tocando com o "ouvindo agora" ligado. Caiu: tenta de novo
 * com espera crescente ([reconnectDelayMs]). Cota do dia esgotada: espera a
 * meia-noite UTC, sem insistir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SocialLink(
    private val scope: CoroutineScope,
    private val url: String,
    /** "Bearer id.token", ou null sem perfil. */
    private val authorization: () -> String?,
    private val onOpen: suspend () -> Unit,
    private val onEvent: suspend (JSONObject) -> Unit,
) {
    private val client = OkHttpClient.Builder()
        // Ping do próprio protocolo: o servidor responde sem acordar.
        .pingInterval(45, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private val _status = MutableStateFlow<LinkStatus>(LinkStatus.Offline)
    val status: StateFlow<LinkStatus> = _status.asStateFlow()

    private val reasons = MutableStateFlow<Set<String>>(emptySet())
    private val accountReady = MutableStateFlow(authorization() != null)
    private val kick = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    @Volatile
    private var socket: WebSocket? = null

    @Volatile
    private var restingUntilMs = 0L

    init {
        scope.launch {
            combine(reasons, accountReady) { wanted, ready -> wanted.isNotEmpty() && ready }
                // Uma folga antes de fechar: trocar de tela ou de app rapidinho não derruba a conexão.
                .transformLatest { wanted ->
                    if (!wanted) delay(CLOSE_GRACE_MS)
                    emit(wanted)
                }
                .distinctUntilChanged()
                .collectLatest { wanted ->
                    if (wanted) {
                        run()
                    } else if (_status.value !is LinkStatus.Resting) {
                        _status.value = LinkStatus.Offline
                    }
                }
        }
    }

    /** Liga ou desliga um motivo para ficar conectado ("ui", "playing"…). */
    fun want(reason: String, wanted: Boolean) {
        reasons.value = if (wanted) reasons.value + reason else reasons.value - reason
        if (wanted) kick.tryEmit(Unit)
    }

    /** O perfil foi criado ou apagado. */
    fun accountChanged() {
        accountReady.value = authorization() != null
        if (accountReady.value) kick.tryEmit(Unit) else socket?.close(1000, "bye")
    }

    /** Uma resposta HTTP disse que o social está descansando. */
    fun markResting(untilMs: Long) {
        restingUntilMs = untilMs
        _status.value = LinkStatus.Resting(untilMs)
    }

    val isOnline: Boolean get() = _status.value == LinkStatus.Online && socket != null

    /** Manda um evento; false se não está conectado (quem chama usa o HTTP). */
    fun send(event: JSONObject): Boolean = isOnline && socket?.send(event.toString()) == true

    private suspend fun run() {
        var attempt = 0
        try {
            while (true) {
                val auth = authorization() ?: return
                val now = System.currentTimeMillis()
                if (restingUntilMs > now) {
                    _status.value = LinkStatus.Resting(restingUntilMs)
                    // Volta um pouco depois da meia-noite UTC, cada celular num minuto diferente.
                    delay(restingUntilMs - now + Random.nextLong(0, 10 * 60_000L))
                    continue
                }
                _status.value = LinkStatus.Connecting
                val stayedOnline = connectOnce(auth)
                if (stayedOnline) attempt = 0
                attempt++
                if (restingUntilMs > System.currentTimeMillis()) continue
                _status.value = LinkStatus.Offline
                // Espera, mas volta antes se alguém pedir (app voltou para a tela).
                withTimeoutOrNull(reconnectDelayMs(attempt, Random.nextDouble())) { kick.first() }
            }
        } finally {
            socket?.close(1000, "bye")
            socket = null
            if (_status.value !is LinkStatus.Resting) _status.value = LinkStatus.Offline
        }
    }

    /** Conecta e fica recebendo até a conexão cair. true = ficou conectado por um bom tempo. */
    private suspend fun connectOnce(auth: String): Boolean {
        val events = Channel<LinkEvent>(Channel.UNLIMITED)
        val request = Request.Builder().url(url).header("Authorization", auth).build()
        val ws = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    events.trySend(LinkEvent.Opened)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    events.trySend(LinkEvent.Message(text))
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                    events.trySend(LinkEvent.Closed(code))
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    events.trySend(LinkEvent.Closed(code))
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    val code = response?.code ?: 0
                    val body = runCatching { response?.peekBody(4096)?.string() }.getOrNull()
                    val json = runCatching { JSONObject(body.orEmpty()) }.getOrNull()
                    restingUntil(
                        code,
                        json?.optString("error")?.takeIf { it.isNotEmpty() },
                        json?.optLong("until") ?: 0L,
                        System.currentTimeMillis(),
                    )?.let(::markResting)
                    events.trySend(LinkEvent.Closed(code))
                }
            },
        )
        socket = ws
        var openedAt = 0L
        try {
            for (event in events) {
                when (event) {
                    LinkEvent.Opened -> {
                        openedAt = System.currentTimeMillis()
                        _status.value = LinkStatus.Online
                        runCatching { onOpen() }
                    }
                    is LinkEvent.Message -> runCatching { onEvent(JSONObject(event.text)) }
                    is LinkEvent.Closed -> {
                        // 4001: o perfil não vale mais (apagado em outro aparelho).
                        if (event.code == 4001) accountReady.value = false
                        break
                    }
                }
            }
        } finally {
            if (socket === ws) socket = null
            ws.cancel()
        }
        return openedAt > 0 && System.currentTimeMillis() - openedAt > STABLE_MS
    }

    private sealed interface LinkEvent {
        data object Opened : LinkEvent
        data class Message(val text: String) : LinkEvent
        data class Closed(val code: Int) : LinkEvent
    }

    private companion object {
        const val CLOSE_GRACE_MS = 20_000L
        const val STABLE_MS = 60_000L
    }
}
