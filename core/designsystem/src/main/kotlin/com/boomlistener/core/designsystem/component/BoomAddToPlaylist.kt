// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** O conteúdo da página "Adicionar à playlist" para as músicas de [trackUris]. */
typealias BoomAddToPlaylistContent = @Composable BoomMenuPageScope.(trackUris: List<String>) -> Unit

/**
 * Quem sabe adicionar a uma playlist é a feature de biblioteca, e uma feature não importa outra:
 * o `:app` fornece o conteúdo aqui e qualquer menu abre a página com [BoomAddToPlaylistPage].
 */
val LocalAddToPlaylistPage = staticCompositionLocalOf<BoomAddToPlaylistContent> { {} }

/** A página "Adicionar à playlist" dentro do painel do menu ([BoomMenuAction.page]). */
@Composable
fun BoomMenuPageScope.BoomAddToPlaylistPage(trackUris: List<String>) {
    LocalAddToPlaylistPage.current(this, trackUris)
}
