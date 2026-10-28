// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.player

import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioOutputsTest {

    private val phone = Candidate(2, "Pixel 9", AudioOutputKind.Phone)
    private val buds = Candidate(10, "X-LY4", AudioOutputKind.Bluetooth)
    private val budsAgain = Candidate(11, "X-LY4", AudioOutputKind.Bluetooth)
    private val wired = Candidate(5, null, AudioOutputKind.Wired)

    @Test
    fun tiposDoAndroid() {
        assertEquals(AudioOutputKind.Phone, outputKind(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertEquals(AudioOutputKind.Bluetooth, outputKind(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertEquals(AudioOutputKind.Bluetooth, outputKind(26)) // fone Bluetooth LE
        assertEquals(AudioOutputKind.Wired, outputKind(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
        assertEquals(AudioOutputKind.Usb, outputKind(AudioDeviceInfo.TYPE_USB_HEADSET))
        assertEquals(AudioOutputKind.Hdmi, outputKind(AudioDeviceInfo.TYPE_HDMI))
    }

    @Test
    fun foneDeLigacaoNaoAparece() {
        assertNull(outputKind(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE))
        assertNull(outputKind(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
        assertNull(outputKind(AudioDeviceInfo.TYPE_TELEPHONY))
    }

    @Test
    fun repetidosViramUm() {
        val outputs = buildOutputs(listOf(phone, buds, budsAgain, Candidate(3, "Pixel 9", AudioOutputKind.Phone)), null, null)
        assertEquals(listOf(10, 2), outputs.map { it.id })
    }

    @Test
    fun semInformacaoBluetoothGanha() {
        val outputs = buildOutputs(listOf(phone, wired, buds), null, null)
        assertEquals(10, outputs.single { it.active }.id)
    }

    @Test
    fun rotaDoSistemaValeMesmoQueSejaORepetido() {
        val outputs = buildOutputs(listOf(phone, buds, budsAgain), null, systemRouteId = 11)
        assertEquals(10, outputs.single { it.active }.id)
        val toPhone = buildOutputs(listOf(phone, buds), null, systemRouteId = 2)
        assertEquals(2, toPhone.single { it.active }.id)
    }

    @Test
    fun escolhaDaTelaVemPrimeiro() {
        val outputs = buildOutputs(listOf(phone, buds), preferredId = 2, systemRouteId = 10)
        assertEquals(2, outputs.single { it.active }.id)
    }

    @Test
    fun escolhaQueSumiuNaoMarcaNada() {
        val outputs = buildOutputs(listOf(phone), preferredId = 10, systemRouteId = null)
        assertEquals(2, outputs.single { it.active }.id)
    }

    @Test
    fun voltarAoFoneMarcaOFoneMesmoComARotaAtrasada() {
        // Estava no celular; tocou no fone (a saída automática). O sistema ainda
        // informa o celular por um instante: vale o que foi tocado.
        val outputs = buildOutputs(listOf(phone, wired), preferredId = 5, systemRouteId = 2)
        assertEquals(listOf(5), outputs.filter { it.active }.map { it.id })
    }

    @Test
    fun foneRenomeadoMostraONomeDaPessoa() {
        val outputs = buildOutputs(listOf(phone, buds), null, null, bluetoothName = "TANGZU Wan'er")
        assertEquals("TANGZU Wan'er", outputs.first { it.id == 10 }.name)
        assertEquals("Pixel 9", outputs.first { it.id == 2 }.name)
    }

    @Test
    fun doisFonesSoOQueTocaGanhaONome() {
        val other = Candidate(20, "AZ09", AudioOutputKind.Bluetooth)
        val outputs = buildOutputs(listOf(phone, buds, other), null, systemRouteId = 20, bluetoothName = "Meu fone")
        assertEquals("Meu fone", outputs.first { it.id == 20 }.name)
        assertEquals("X-LY4", outputs.first { it.id == 10 }.name)
    }
}
