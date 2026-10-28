// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRoute2Info
import android.media.MediaRouter
import android.media.MediaRouter2
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Tipo de saída de som, para o ícone e o nome na tela. */
enum class AudioOutputKind { Phone, Wired, Bluetooth, Usb, Hdmi }

/**
 * Um aparelho conectado que pode tocar o som.
 * [name] é o nome que o sistema dá (o do fone Bluetooth, por exemplo); para o
 * alto-falante do celular a tela usa um texto próprio.
 */
data class AudioOutput(
    val id: Int,
    val name: String?,
    val kind: AudioOutputKind,
    val active: Boolean,
)

/**
 * Saídas de som conectadas (alto-falante, fone com fio, Bluetooth, USB, HDMI)
 * e qual está tocando. Escolher uma manda a música para ela, sem mexer no
 * resto do sistema. Não precisa de permissão.
 */
interface AudioOutputs {
    val outputs: StateFlow<List<AudioOutput>>

    /** Manda o som para a saída [id]; null volta a deixar o sistema decidir. */
    fun choose(id: Int?)

    /**
     * Abre o seletor de saída do próprio Android (Android 14+), que também
     * mostra caixas e TVs da rede; nos anteriores, os ajustes de Bluetooth.
     */
    fun openSystemPicker(context: Context)
}

/** Ponto de acesso provisório enquanto o app não tem Hilt. */
object AudioOutputsData {
    @Volatile
    private var instance: SystemAudioOutputs? = null

    fun get(context: Context): AudioOutputs = internalGet(context)

    internal fun internalGet(context: Context): SystemAudioOutputs =
        instance ?: synchronized(this) {
            instance ?: SystemAudioOutputs(context.applicationContext).also { instance = it }
        }
}

internal class SystemAudioOutputs(private val context: Context) : AudioOutputs {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val list = MutableStateFlow<List<AudioOutput>>(emptyList())
    override val outputs: StateFlow<List<AudioOutput>> = list.asStateFlow()

    /** A saída escolhida na tela (o [PlaybackService] aplica no player). */
    val preferred = MutableStateFlow<Int?>(null)

    /**
     * A última saída tocada na lista, mesmo quando ela é a "automática" (e por
     * isso não vai como preferida para o player). É ela que fica marcada: o
     * sistema demora a informar a rota nova e, sem isto, a marca ficava na
     * saída anterior (escolhia o fone e o celular continuava marcado).
     */
    private var chosen: Int? = null

    // Ao abrir o app o sistema avisa várias mudanças de rota seguidas: junta
    // todas numa atualização só, um pouco depois, sem pesar na abertura.
    private val refreshRunnable = Runnable { refresh() }

    private fun refreshSoon() {
        handler.removeCallbacks(refreshRunnable)
        handler.postDelayed(refreshRunnable, ROUTE_REFRESH_DELAY_MS)
    }

    private val router2: MediaRouter2? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) runCatching { MediaRouter2.getInstance(context) }.getOrNull() else null
    }

    init {
        audioManager?.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = devicesChanged()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = devicesChanged()
            },
            handler,
        )
        // O nome do fone muda quando a pessoa o renomeia nos ajustes de Bluetooth.
        handler.post {
            runCatching {
                context.getSystemService(MediaRouter::class.java)?.addCallback(
                    MediaRouter.ROUTE_TYPE_LIVE_AUDIO,
                    object : MediaRouter.SimpleCallback() {
                        override fun onRouteChanged(router: MediaRouter?, info: MediaRouter.RouteInfo?) = refreshSoon()
                        override fun onRouteSelected(router: MediaRouter?, type: Int, info: MediaRouter.RouteInfo?) = refreshSoon()
                    },
                )
            }
        }
        refresh()
    }

    /**
     * O nome do fone Bluetooth tocando como aparece nos ajustes do Android,
     * inclusive o que a pessoa deu a ele ao renomear (o [AudioDeviceInfo] só
     * tem o nome de fábrica). Vem da rota de som do sistema, sem permissão.
     */
    private fun bluetoothRouteName(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            runCatching {
                router2?.systemController?.selectedRoutes.orEmpty()
                    .firstOrNull { it.type in BLUETOOTH_ROUTE_TYPES }
                    ?.name?.toString()?.takeIf { it.isNotBlank() }
            }.getOrNull()?.let { return it }
        }
        return runCatching {
            context.getSystemService(MediaRouter::class.java)
                ?.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_AUDIO)
                ?.takeIf { it.deviceType == MediaRouter.RouteInfo.DEVICE_TYPE_BLUETOOTH }
                ?.getName(context)?.toString()?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    /** Ligou ou desligou algo: o automático volta a valer (o sistema decide de novo). */
    private fun devicesChanged() {
        chosen = preferred.value
        refresh()
    }

    fun refresh() {
        val manager = audioManager ?: return
        val devices = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .mapNotNull { device -> outputKind(device.type)?.let { Candidate(device.id, device.productName?.toString(), it) } }
        // A saída escolhida sumiu (fone desligado): volta ao automático.
        if (preferred.value != null && devices.none { it.id == preferred.value }) preferred.value = null
        if (chosen != null && devices.none { it.id == chosen }) chosen = null
        val systemRoute = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching {
                manager.getAudioDevicesForAttributes(
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build(),
                ).firstOrNull()?.id
            }.getOrNull()
        } else {
            null
        }
        list.value = buildOutputs(devices, chosen ?: preferred.value, systemRoute, bluetoothRouteName())
    }

    override fun choose(id: Int?) {
        // Escolher a saída que o sistema já usaria sozinho (o fone, com fone
        // ligado) volta ao automático: assim o Android segue cuidando da troca.
        val natural = list.value.minByOrNull { priorityOf(it.kind) }?.id
        preferred.value = if (id == natural) null else id
        chosen = id
        refresh()
    }

    override fun openSystemPicker(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val shown = runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)
            if (shown) return
        }
        runCatching {
            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** O dispositivo de verdade, para o player. */
    fun deviceInfo(id: Int): AudioDeviceInfo? =
        audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS)?.firstOrNull { it.id == id }
}

/** Uma saída antes de saber qual está ativa. */
internal data class Candidate(val id: Int, val name: String?, val kind: AudioOutputKind)

/**
 * Tipo do Android → tipo na tela. Fica de fora o que não é para música
 * (fone de ligação, saídas internas, transmissão).
 */
internal fun outputKind(type: Int): AudioOutputKind? = when (type) {
    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputKind.Phone
    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_LINE_ANALOG,
    -> AudioOutputKind.Wired
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    TYPE_BLE_HEADSET,
    TYPE_BLE_SPEAKER,
    -> AudioOutputKind.Bluetooth
    AudioDeviceInfo.TYPE_USB_DEVICE,
    AudioDeviceInfo.TYPE_USB_ACCESSORY,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    -> AudioOutputKind.Usb
    AudioDeviceInfo.TYPE_HDMI,
    AudioDeviceInfo.TYPE_HDMI_ARC,
    TYPE_HDMI_EARC,
    -> AudioOutputKind.Hdmi
    else -> null
}

private const val ROUTE_REFRESH_DELAY_MS = 300L

private val BLUETOOTH_ROUTE_TYPES = setOf(
    MediaRoute2Info.TYPE_BLUETOOTH_A2DP,
    MediaRoute2Info.TYPE_BLE_HEADSET,
    MediaRoute2Info.TYPE_HEARING_AID,
)

// Constantes de versões novas do Android, escritas aqui para valerem em qualquer versão.
private const val TYPE_BLE_HEADSET = 26
private const val TYPE_BLE_SPEAKER = 27
private const val TYPE_HDMI_EARC = 29

/**
 * Junta repetidos (o mesmo fone aparece mais de uma vez) e marca a ativa:
 * a escolhida na tela; senão a que o sistema informa (Android 13+); senão a
 * mais provável (Bluetooth > USB > fio > HDMI > celular). [bluetoothName] é o
 * nome do fone nos ajustes do Android (com o que a pessoa deu ao renomear).
 */
internal fun buildOutputs(
    devices: List<Candidate>,
    preferredId: Int?,
    systemRouteId: Int?,
    bluetoothName: String? = null,
): List<AudioOutput> {
    val unique = devices.distinctBy { it.kind to (if (it.kind == AudioOutputKind.Phone) "" else it.name.orEmpty()) }
    val activeId = preferredId?.takeIf { id -> unique.any { it.id == id } }
        ?: systemRouteId?.let { route ->
            // O id do sistema pode ser de um repetido que saiu na limpeza: vale o do mesmo aparelho.
            val routed = devices.firstOrNull { it.id == route }
            routed?.let { r -> unique.firstOrNull { it.kind == r.kind && (r.kind == AudioOutputKind.Phone || it.name == r.name) }?.id }
        }
        ?: unique.minByOrNull { priority(it.kind) }?.id
    // O nome dado pela pessoa (do fone que o sistema usa): vai no único
    // Bluetooth ligado ou, com mais de um, no que está tocando.
    val bluetooth = unique.filter { it.kind == AudioOutputKind.Bluetooth }
    val renamedId = bluetoothName?.let { bluetooth.singleOrNull()?.id ?: bluetooth.firstOrNull { it.id == activeId }?.id }
    return unique
        .sortedBy { priority(it.kind) }
        .map { AudioOutput(it.id, if (it.id == renamedId) bluetoothName else it.name, it.kind, active = it.id == activeId) }
}

internal fun priorityOf(kind: AudioOutputKind) = priority(kind)

private fun priority(kind: AudioOutputKind) = when (kind) {
    AudioOutputKind.Bluetooth -> 0
    AudioOutputKind.Usb -> 1
    AudioOutputKind.Wired -> 2
    AudioOutputKind.Hdmi -> 3
    AudioOutputKind.Phone -> 4
}
