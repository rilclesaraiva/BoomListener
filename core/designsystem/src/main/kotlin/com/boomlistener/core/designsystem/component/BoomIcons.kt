// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Todos os ícones do app passam por aqui, para trocar o conjunto (por exemplo,
 * pelos Material Symbols) mexendo num arquivo só.
 */
object BoomIcons {
    val Library: ImageVector = Icons.Filled.Home
    val Releases: ImageVector = Icons.Filled.Notifications
    val Wellbeing: ImageVector = Icons.Filled.Favorite
    val Settings: ImageVector = Icons.Filled.Settings
    val Search: ImageVector = Icons.Filled.Search
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Close: ImageVector = Icons.Filled.Close
    val More: ImageVector = Icons.Filled.MoreVert
}
