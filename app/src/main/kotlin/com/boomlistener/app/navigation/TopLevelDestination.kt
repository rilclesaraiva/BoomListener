// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.app.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.boomlistener.app.R
import com.boomlistener.core.designsystem.component.BoomIcons
import com.boomlistener.feature.library.COLLECTIONS_ROUTE
import com.boomlistener.feature.profile.MY_PROFILE_ROUTE
import com.boomlistener.feature.library.LIBRARY_ROUTE

/**
 * As abas da barra de navegação inferior (máximo de 5, regra do M3): Início,
 * Biblioteca e Perfil. Lançamentos (o sino) e Ajustes
 * não são abas: ficam no canto superior direito de cada aba. O histórico fica
 * dentro do Perfil; Bem-estar, dentro de Ajustes.
 */
enum class TopLevelDestination(
    val route: String,
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    Home(LIBRARY_ROUTE, BoomIcons.Library, R.string.nav_home),
    Collections(COLLECTIONS_ROUTE, BoomIcons.Collection, R.string.nav_collections),
    Profile(MY_PROFILE_ROUTE, BoomIcons.Person, R.string.nav_profile),
}
