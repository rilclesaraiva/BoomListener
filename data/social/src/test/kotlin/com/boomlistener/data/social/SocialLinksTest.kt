// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SocialLinksTest {
    @Test
    fun arrobaDeCadaRede() {
        assertEquals("ana.clara", cleanNetworkHandle(SocialNetwork.Instagram, " @ana.clara "))
        assertEquals("ana", cleanNetworkHandle(SocialNetwork.Instagram, "https://instagram.com/ana/"))
        assertEquals("ana", cleanNetworkHandle(SocialNetwork.TikTok, "https://www.tiktok.com/@ana?lang=pt"))
        assertEquals("ana", cleanNetworkHandle(SocialNetwork.LastFm, "https://www.last.fm/user/ana"))
        assertEquals("ana", cleanNetworkHandle(SocialNetwork.Bandcamp, "https://ana.bandcamp.com/album/disco"))
        assertEquals("ana.bsky.social", cleanNetworkHandle(SocialNetwork.Bluesky, "@ana"))
        assertEquals("ana.com.br", cleanNetworkHandle(SocialNetwork.Bluesky, "ana.com.br"))
        assertEquals("", cleanNetworkHandle(SocialNetwork.X, "  "))
        assertNull(cleanNetworkHandle(SocialNetwork.X, "ana clara"))
        assertNull(cleanNetworkHandle(SocialNetwork.X, "<script>"))
    }

    @Test
    fun linkMontadoPeloApp() {
        // Mesmo colando um link de outro site, o link final é sempre o da rede.
        val handle = cleanNetworkHandle(SocialNetwork.Instagram, "https://golpe.example/ana")!!
        assertEquals("https://instagram.com/ana", networkUrl(SocialNetwork.Instagram, handle))
        assertEquals("https://www.tiktok.com/@ana", networkUrl(SocialNetwork.TikTok, "ana"))
        assertEquals("https://ana.bandcamp.com", networkUrl(SocialNetwork.Bandcamp, "ana"))
        assertEquals("https://t.me/ana", networkUrl(SocialNetwork.Telegram, "ana"))
        assertEquals("https://wa.me/5591999999999", numberUrl(NumberNetwork.WhatsApp, "5591999999999"))
        assertEquals("https://t.me/+5591999999999", numberUrl(NumberNetwork.Telegram, "5591999999999"))
    }

    @Test
    fun numeroComCodigoDoPais() {
        assertEquals("5591999999999", normalizePhone("(91) 99999-9999", "55"))
        assertEquals("5591999999999", normalizePhone("091 99999 9999", "+55")) // 0 de discagem
        assertEquals("5591999999999", normalizePhone("+55 91 99999-9999", "1")) // já tem código
        assertEquals("351912345678", normalizePhone("00351 912 345 678", "55"))
        assertEquals("", normalizePhone("  ", "55"))
        assertNull(normalizePhone("123", "55"))
        assertNull(normalizePhone("meu número", "55"))
        assertNull(normalizePhone("91999999999", ""))
    }

    @Test
    fun codigoDoPaisDoCelular() {
        assertEquals("55", callingCodeFor("BR"))
        assertEquals("351", callingCodeFor("pt"))
        assertEquals("1", callingCodeFor("US"))
        assertNull(callingCodeFor("ZZ"))
    }
}
