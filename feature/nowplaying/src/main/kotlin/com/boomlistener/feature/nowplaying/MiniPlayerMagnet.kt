// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.nowplaying

/** Dentro do ímã o mini player acompanha o dedo só um pouco (esta fração do movimento). */
private const val MAGNET_PULL = 0.25f

/**
 * "Ímã" da posição de baixo do mini player: enquanto o dedo desceu menos que [stickPx], o mini player
 * cede só uma fração (parece grudado); passando disso ele desgruda e volta a seguir o dedo, sem salto.
 * Assim, descer um pouquinho sem querer (depois de subir e voltar devagar) não encerra a música.
 */
internal fun magnetOffset(pullPx: Float, stickPx: Float): Float {
    val pull = pullPx.coerceAtLeast(0f)
    val stick = stickPx.coerceAtLeast(0f)
    return if (pull <= stick) pull * MAGNET_PULL else stick * MAGNET_PULL + (pull - stick)
}
