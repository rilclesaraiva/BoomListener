// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Único lugar do app onde cores aparecem escritas à mão (ver CLAUDE.md).
// Este esquema só é usado quando não há cor dinâmica (Android 11 ou anterior,
// ou o usuário desligou nos Ajustes). Cor-semente: laranja "boom".

internal val BoomLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF9A4600),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBC8),
    onPrimaryContainer = Color(0xFF321200),
    secondary = Color(0xFF755846),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBC8),
    onSecondaryContainer = Color(0xFF2B1709),
    tertiary = Color(0xFF606134),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE6E6AD),
    onTertiaryContainer = Color(0xFF1C1D00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A15),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A15),
    surfaceVariant = Color(0xFFF4DED3),
    onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFF85746B),
)

internal val BoomDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB68A),
    onPrimary = Color(0xFF532200),
    primaryContainer = Color(0xFF763300),
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = Color(0xFFE5BFA9),
    onSecondary = Color(0xFF432B1C),
    secondaryContainer = Color(0xFF5B4130),
    onSecondaryContainer = Color(0xFFFFDBC8),
    tertiary = Color(0xFFCACA93),
    onTertiary = Color(0xFF32320A),
    tertiaryContainer = Color(0xFF48491F),
    onTertiaryContainer = Color(0xFFE6E6AD),
    background = Color(0xFF1A120D),
    onBackground = Color(0xFFF0DFD7),
    surface = Color(0xFF1A120D),
    onSurface = Color(0xFFF0DFD7),
    surfaceVariant = Color(0xFF52443C),
    onSurfaceVariant = Color(0xFFD7C3B8),
    outline = Color(0xFFA08D84),
)

/** Versão AMOLED de qualquer esquema escuro: fundos em preto puro. */
internal fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF080808),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF202020),
)
