// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.boomlistener.app.R
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.feature.library.COLLECTIONS_ROUTE
import com.boomlistener.feature.social.SOCIAL_ROUTE
import com.boomlistener.feature.library.LIBRARY_ROUTE
import com.boomlistener.feature.releases.RELEASES_ROUTE

/**
 * As abas da barra de navegação inferior (máximo de 5, regra do M3).
 * Ajustes e histórico não são abas: ficam no canto superior direito de cada
 * aba. Bem-estar fica dentro de Ajustes.
 */
enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    Home(LIBRARY_ROUTE, BoomIcons.Library, R.string.nav_home),
    Releases(RELEASES_ROUTE, BoomIcons.Releases, R.string.nav_releases),
    Collections(COLLECTIONS_ROUTE, BoomIcons.Collection, R.string.nav_collections),
    Social(SOCIAL_ROUTE, BoomIcons.Chat, R.string.nav_social),
}
