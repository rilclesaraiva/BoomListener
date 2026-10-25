// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileStoreTest {
    @Test
    fun corDoPerfil() {
        assertEquals("#FF5500", cleanProfileColor("#ff5500"))
        assertEquals("#FF5500", cleanProfileColor(" FF5500 "))
        assertEquals("", cleanProfileColor(""))
        assertEquals(null, cleanProfileColor("red"))
        assertEquals(null, cleanProfileColor("#FFF"))
    }

    @Test
    fun perfilVaiEVoltaDoArquivo() {
        val profile = MyProfile("Rilcle", "I'm the dev", "#AA0000", avatarVersion = 5, bannerVersion = 0)
        assertEquals(profile, profileFromJson(profileToJson(profile)))
    }

    @Test
    fun perfilAntigoDoSocialFicaSoComONomeDescricaoECor() {
        val old = """{"id":"u1","handle":"rilcle","name":"Rilcle","token":"segredo","sharing":true,
            "bio":"Olá","color":"#00ff00","links":{"instagram":"rilcle"},"numbers":{"whatsapp":"5591999999999"}}"""
        assertEquals(MyProfile(name = "Rilcle", bio = "Olá", color = "#00FF00"), profileFromOldSocialAccount(old))
        assertNull(profileFromOldSocialAccount("não é json"))
    }

    @Test
    fun `solid color round trips and old files keep the gradient`() {
        val solid = MyProfile(name = "R", color = "#7A0000", solidColor = true)
        assertEquals(solid, profileFromJson(profileToJson(solid)))
        val old = profileFromJson("""{"name":"R","bio":"","color":"#7A0000","avatar":0,"banner":0}""")
        assertEquals(false, old.solidColor)
        assertEquals("#7A0000", old.color)
    }
}
