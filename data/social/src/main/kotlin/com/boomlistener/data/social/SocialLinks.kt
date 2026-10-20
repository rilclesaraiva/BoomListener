// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.social

/*
 * Redes no perfil (testado). A pessoa digita só o @ ou o número; o app monta
 * o link. Nunca usa um link colado como está (evita golpe): se colarem um
 * endereço, fica só o @ dele e o link é montado de novo.
 */

/** Redes públicas: visíveis para quem abrir o perfil, como uma bio. [key] é o nome no servidor. */
enum class SocialNetwork(val key: String) {
    Instagram("instagram"),
    X("x"),
    TikTok("tiktok"),
    Threads("threads"),
    Bluesky("bluesky"),
    YouTube("youtube"),
    LastFm("lastfm"),
    SoundCloud("soundcloud"),
    Bandcamp("bandcamp"),
    Spotify("spotify"),
    Telegram("telegram"),
    ;

    /** Nome da rede (nome próprio: igual em todos os idiomas). */
    val label: String
        get() = when (this) {
            Instagram -> "Instagram"
            X -> "X"
            TikTok -> "TikTok"
            Threads -> "Threads"
            Bluesky -> "Bluesky"
            YouTube -> "YouTube"
            LastFm -> "Last.fm"
            SoundCloud -> "SoundCloud"
            Bandcamp -> "Bandcamp"
            Spotify -> "Spotify"
            Telegram -> "Telegram"
        }

    companion object {
        fun fromKey(key: String): SocialNetwork? = entries.firstOrNull { it.key == key }
    }
}

/** Redes que expõem o número: só para amigos. */
enum class NumberNetwork(val key: String) {
    WhatsApp("whatsapp"),
    Telegram("telegram"),
    ;

    val label: String
        get() = when (this) {
            WhatsApp -> "WhatsApp"
            Telegram -> "Telegram"
        }

    companion object {
        fun fromKey(key: String): NumberNetwork? = entries.firstOrNull { it.key == key }
    }
}

private val HANDLE_CHARS = Regex("^[A-Za-z0-9._-]{1,60}$")

/**
 * O @ limpo para [network], ou null se não der. "@ana", "ana" e
 * "https://instagram.com/ana/" viram "ana". Vazio vira "" (tirar a rede).
 */
fun cleanNetworkHandle(network: SocialNetwork, raw: String): String? {
    var text = raw.trim()
    if (text.isEmpty()) return ""
    if ("/" in text || text.startsWith("http", ignoreCase = true)) {
        val path = text.substringAfter("://").substringAfter('/', "").trim('/')
        // Bandcamp: o @ é o começo do endereço (usuario.bandcamp.com).
        text = if (network == SocialNetwork.Bandcamp && ".bandcamp.com" in text) {
            text.substringAfter("://").substringBefore(".bandcamp.com")
        } else {
            path.split('/').lastOrNull { it.isNotBlank() && it != "user" && it != "profile" }.orEmpty()
        }
        text = text.substringBefore('?')
    }
    text = text.removePrefix("@").trim()
    if (network == SocialNetwork.Bluesky && text.isNotEmpty() && '.' !in text) text += ".bsky.social"
    return text.takeIf { HANDLE_CHARS.matches(it) }
}

/** O link do perfil em [network] para o @ [handle]. */
fun networkUrl(network: SocialNetwork, handle: String): String = when (network) {
    SocialNetwork.Instagram -> "https://instagram.com/$handle"
    SocialNetwork.X -> "https://x.com/$handle"
    SocialNetwork.TikTok -> "https://www.tiktok.com/@$handle"
    SocialNetwork.Threads -> "https://www.threads.com/@$handle"
    SocialNetwork.Bluesky -> "https://bsky.app/profile/$handle"
    SocialNetwork.YouTube -> "https://www.youtube.com/@$handle"
    SocialNetwork.LastFm -> "https://www.last.fm/user/$handle"
    SocialNetwork.SoundCloud -> "https://soundcloud.com/$handle"
    SocialNetwork.Bandcamp -> "https://$handle.bandcamp.com"
    SocialNetwork.Spotify -> "https://open.spotify.com/user/$handle"
    SocialNetwork.Telegram -> "https://t.me/$handle"
}

/** O link para conversar pelo número ([digits] com código do país, sem "+"). */
fun numberUrl(network: NumberNetwork, digits: String): String = when (network) {
    NumberNetwork.WhatsApp -> "https://wa.me/$digits"
    NumberNetwork.Telegram -> "https://t.me/+$digits"
}

/**
 * Número como o WhatsApp entende: só dígitos, com código do país. Tira
 * espaços, traços e parênteses; "+" ou "00" no começo = já tem o código;
 * sem código, usa [countryCode] (o do país do celular, que a pessoa pode
 * trocar) e tira o 0 de discagem. null se não parecer um número.
 * "(91) 99999-9999" com 55 → "5591999999999".
 */
fun normalizePhone(raw: String, countryCode: String): String? {
    val text = raw.trim()
    if (text.isEmpty()) return ""
    if (text.any { it.isLetter() }) return null
    val international = text.startsWith("+") || text.startsWith("00")
    var digits = text.filter { it.isDigit() }
    if (international) {
        if (text.startsWith("00")) digits = digits.removePrefix("00")
    } else {
        val code = countryCode.filter { it.isDigit() }
        if (code.isEmpty()) return null
        digits = code + digits.trimStart('0')
    }
    return digits.takeIf { it.length in 8..15 && it.first() != '0' }
}

/** Código de discagem do país [region] ("BR" → "55"), ou null se não souber. */
fun callingCodeFor(region: String): String? = CALLING_CODES[region.uppercase()]

private val CALLING_CODES: Map<String, String> = """
AD 376 AE 971 AF 93 AG 1 AI 1 AL 355 AM 374 AO 244 AR 54 AS 1 AT 43 AU 61 AW 297 AZ 994
BA 387 BB 1 BD 880 BE 32 BF 226 BG 359 BH 973 BI 257 BJ 229 BM 1 BN 673 BO 591 BR 55 BS 1
BT 975 BW 267 BY 375 BZ 501 CA 1 CD 243 CF 236 CG 242 CH 41 CI 225 CL 56 CM 237 CN 86 CO 57
CR 506 CU 53 CV 238 CW 599 CY 357 CZ 420 DE 49 DJ 253 DK 45 DM 1 DO 1 DZ 213 EC 593 EE 372
EG 20 ER 291 ES 34 ET 251 FI 358 FJ 679 FM 691 FO 298 FR 33 GA 241 GB 44 GD 1 GE 995 GF 594
GH 233 GI 350 GL 299 GM 220 GN 224 GP 590 GQ 240 GR 30 GT 502 GU 1 GW 245 GY 592 HK 852
HN 504 HR 385 HT 509 HU 36 ID 62 IE 353 IL 972 IN 91 IQ 964 IR 98 IS 354 IT 39 JM 1 JO 962
JP 81 KE 254 KG 996 KH 855 KI 686 KM 269 KN 1 KP 850 KR 82 KW 965 KY 1 KZ 7 LA 856 LB 961
LC 1 LI 423 LK 94 LR 231 LS 266 LT 370 LU 352 LV 371 LY 218 MA 212 MC 377 MD 373 ME 382
MG 261 MH 692 MK 389 ML 223 MM 95 MN 976 MO 853 MQ 596 MR 222 MS 1 MT 356 MU 230 MV 960
MW 265 MX 52 MY 60 MZ 258 NA 264 NC 687 NE 227 NG 234 NI 505 NL 31 NO 47 NP 977 NR 674
NZ 64 OM 968 PA 507 PE 51 PF 689 PG 675 PH 63 PK 92 PL 48 PR 1 PS 970 PT 351 PW 680 PY 595
QA 974 RE 262 RO 40 RS 381 RU 7 RW 250 SA 966 SB 677 SC 248 SD 249 SE 46 SG 65 SI 386
SK 421 SL 232 SM 378 SN 221 SO 252 SR 597 SS 211 ST 239 SV 503 SX 1 SY 963 SZ 268 TC 1
TD 235 TG 228 TH 66 TJ 992 TL 670 TM 993 TN 216 TO 676 TR 90 TT 1 TV 688 TW 886 TZ 255
UA 380 UG 256 US 1 UY 598 UZ 998 VA 39 VC 1 VE 58 VG 1 VI 1 VN 84 VU 678 WS 685 XK 383
YE 967 ZA 27 ZM 260 ZW 263
""".trim().split(Regex("\\s+")).chunked(2).associate { (region, code) -> region to code }
