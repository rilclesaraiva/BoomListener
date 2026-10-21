// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
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

    init {
        audioManager?.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = devicesChanged()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = devicesChanged()
            },
            handler,
        )
        refresh()
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
        list.value = buildOutputs(devices, chosen ?: preferred.value, systemRoute)
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

// Constantes de versões novas do Android, escritas aqui para valerem em qualquer versão.
private const val TYPE_BLE_HEADSET = 26
private const val TYPE_BLE_SPEAKER = 27
private const val TYPE_HDMI_EARC = 29

/**
 * Junta repetidos (o mesmo fone aparece mais de uma vez) e marca a ativa:
 * a escolhida na tela; senão a que o sistema informa (Android 13+); senão a
 * mais provável (Bluetooth > USB > fio > HDMI > celular).
 */
internal fun buildOutputs(devices: List<Candidate>, preferredId: Int?, systemRouteId: Int?): List<AudioOutput> {
    val unique = devices.distinctBy { it.kind to (if (it.kind == AudioOutputKind.Phone) "" else it.name.orEmpty()) }
    val activeId = preferredId?.takeIf { id -> unique.any { it.id == id } }
        ?: systemRouteId?.let { route ->
            // O id do sistema pode ser de um repetido que saiu na limpeza: vale o do mesmo aparelho.
            val routed = devices.firstOrNull { it.id == route }
            routed?.let { r -> unique.firstOrNull { it.kind == r.kind && (r.kind == AudioOutputKind.Phone || it.name == r.name) }?.id }
        }
        ?: unique.minByOrNull { priority(it.kind) }?.id
    return unique
        .sortedBy { priority(it.kind) }
        .map { AudioOutput(it.id, it.name, it.kind, active = it.id == activeId) }
}

internal fun priorityOf(kind: AudioOutputKind) = priority(kind)

private fun priority(kind: AudioOutputKind) = when (kind) {
    AudioOutputKind.Bluetooth -> 0
    AudioOutputKind.Usb -> 1
    AudioOutputKind.Wired -> 2
    AudioOutputKind.Hdmi -> 3
    AudioOutputKind.Phone -> 4
}
