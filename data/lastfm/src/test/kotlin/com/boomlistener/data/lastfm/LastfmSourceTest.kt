// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.lastfm

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LastfmSourceTest {
    private val key = "0123456789abcdef0123456789abcdef"

    @Test
    fun chaveValidaTem32LetrasENumeros() {
        assertEquals(key, normalizeLastfmKey("  $key \n"))
        assertEquals(key, normalizeLastfmKey(key.uppercase()))
    }

    @Test
    fun chaveComTamanhoOuSimbolosErradosNaoValeNada() {
        assertNull(normalizeLastfmKey(""))
        assertNull(normalizeLastfmKey(key.dropLast(1)))
        assertNull(normalizeLastfmKey(key + "0"))
        assertNull(normalizeLastfmKey(key.dropLast(1) + "!"))
        assertNull(normalizeLastfmKey("minha chave do last.fm aqui"))
    }

    @Test
    fun semChaveVaiPeloServidorDoBoom() {
        val query = lastfmQuery(listOf("method" to "artist.getinfo", "artist" to "Djavan", "lang" to "pt"))
        assertEquals(
            "$PROXY_URL?method=artist.getinfo&artist=Djavan&lang=pt",
            lastfmRequestUrl(null, query),
        )
    }

    @Test
    fun comChaveVaiDiretoNoLastfmComAChave() {
        val query = lastfmQuery(listOf("method" to "artist.getinfo", "artist" to "Djavan"))
        val url = lastfmRequestUrl(key, query)
        assertEquals("$DIRECT_URL?method=artist.getinfo&artist=Djavan&api_key=$key&format=json", url)
        assertFalse(url.startsWith(PROXY_URL))
    }

    @Test
    fun acentosEEComercialSaemCodificados() {
        val query = lastfmQuery(listOf("artist" to "Beyoncé & Jay-Z", "track" to "Crazy in Love (feat. Jay-Z)"))
        assertEquals(
            "artist=Beyonc%C3%A9+%26+Jay-Z&track=Crazy+in+Love+%28feat.+Jay-Z%29",
            query,
        )
    }

    @Test
    fun oCacheNaoDependeDaFonteNemTemAChave() {
        val query = lastfmQuery(listOf("method" to "artist.getinfo", "artist" to "Cher"))
        assertEquals(lastfmCacheId(query), lastfmCacheId(query))
        assertFalse(lastfmCacheId(query).contains(key))
        assertTrue(lastfmCacheId(query).startsWith(PROXY_URL))
        // O cache que já existia (endereço do servidor do Boom) continua valendo.
        assertEquals(lastfmRequestUrl(null, query), lastfmCacheId(query))
    }

    @Test
    fun respostasDoLastfmSaoClassificadas() {
        assertEquals(LastfmReply.Ok, classifyReply(JSONObject("""{"artist":{"name":"Cher"}}""")))
        assertEquals(LastfmReply.NotFound, classifyReply(JSONObject("""{"error":6,"message":"Artist not found"}""")))
        assertEquals(LastfmReply.KeyRejected, classifyReply(JSONObject("""{"error":10,"message":"Invalid API key"}""")))
        assertEquals(LastfmReply.KeyRejected, classifyReply(JSONObject("""{"error":26,"message":"Suspended API key"}""")))
        assertEquals(LastfmReply.OtherError, classifyReply(JSONObject("""{"error":29,"message":"Rate limit exceeded"}""")))
    }
}
