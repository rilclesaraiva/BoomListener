// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.tageditor

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val TAGEDITOR_ROUTE = "tageditor"

fun NavController.navigateToTagEditor() = navigate(TAGEDITOR_ROUTE)

fun NavGraphBuilder.tageditorScreen(onBack: () -> Unit) {
    composable(TAGEDITOR_ROUTE) { TagEditorScreen(onBack = onBack) }
}
