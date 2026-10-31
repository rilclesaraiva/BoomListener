// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.material3.TopAppBarState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec

/**
 * Estrutura padrão de toda tela: barra superior + conteúdo.
 *
 * A barra inferior de navegação fica no :app, então aqui o Scaffold não
 * reserva espaço para as barras do sistema embaixo (evita espaço em dobro).
 *
 * @param onBack mostra a seta de voltar quando não é null.
 * @param largeTitle título grande que encolhe ao rolar (telas principais, como Ajustes).
 *   Sem ele, a barra some ao rolar para baixo e volta ao rolar para cima.
 * @param topBar barra própria, que substitui a barra padrão (ex.: modo busca).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoomScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    largeTitle: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    topBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    /**
     * Fundo próprio da página inteira, por trás da barra de cima também (ex.:
     * a cor do perfil). A barra usa [topBarColor], a cor do topo desse fundo.
     */
    pageBackground: (Modifier.() -> Modifier)? = null,
    /** Cor da barra de cima, parada ou rolada; null = a do tema. */
    topBarColor: Color? = null,
    /** Barra fixa: não some ao rolar (ex.: o perfil). */
    pinnedTopBar: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val effects = boomEffectsSpec<Float>()
    val spatial = boomSpatialSpec<IntOffset>()
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = if (largeTitle) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    } else if (pinnedTopBar) {
        TopAppBarDefaults.pinnedScrollBehavior(topBarState)
    } else {
        // Rolar para baixo esconde a barra; qualquer subida a traz de volta
        // (e no topo da página ela sempre aparece).
        TopAppBarDefaults.enterAlwaysScrollBehavior(topBarState)
    }
    val barColor = topBarColor ?: MaterialTheme.colorScheme.surface
    val barScrolledColor = topBarColor ?: MaterialTheme.colorScheme.surfaceContainer
    val barInsets = TopAppBarDefaults.windowInsets
    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            BoomBackButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp))
        }
    }
    val pageColor = MaterialTheme.colorScheme.background
    val pageModifier = if (pageBackground != null) {
        modifier.background(pageColor).pageBackground()
    } else {
        modifier
    }
    Scaffold(
        // Barra própria (ou nenhuma, como no perfil): sem ligar a rolagem à barra
        // padrão. Ela não foi desenhada, então não sabe a própria altura, e o
        // "esconder ao rolar" engolia toda a rolagem da lista (a tela travava).
        modifier = if (topBar == null) pageModifier.nestedScroll(scrollBehavior.nestedScrollConnection) else pageModifier,
        containerColor = if (pageBackground != null) Color.Transparent else pageColor,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        // Avisos sobem acima do mini player (que flutua por cima da tela) e da
        // barra de gestos do sistema; antes ficavam escondidos atrás dele.
        snackbarHost = {
            Box(
                Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(bottom = LocalBottomOverlay.current),
            ) { snackbarHost() }
        },
        topBar = {
            // A troca entre a barra normal e uma barra própria (ex.: busca)
            // é animada, nunca um corte seco.
            AnimatedContent(
                targetState = topBar,
                transitionSpec = {
                    (fadeIn(effects) + slideInVertically(spatial) { -it / 3 }) togetherWith
                        (fadeOut(effects) + slideOutVertically(spatial) { -it / 3 })
                },
                contentKey = { it == null },
                label = "topBar",
            ) { customBar ->
              if (customBar != null) {
                customBar()
              } else if (largeTitle) {
                LargeTopAppBar(
                    title = { Text(title) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = barInsets,
                    colors = TopAppBarDefaults.largeTopAppBarColors(),
                    scrollBehavior = scrollBehavior,
                )
            } else {
                // Recolhendo, a barra vai ficando transparente: recolhida, sobra
                // só a faixa das notificações, que mostra a página por trás.
                val visible = 1f - topBarState.collapsedFraction
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    windowInsets = barInsets,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = barColor.copy(alpha = barColor.alpha * visible),
                        scrolledContainerColor = barScrolledColor.copy(alpha = barScrolledColor.alpha * visible),
                    ),
                    scrollBehavior = scrollBehavior,
                )
              }
            }
        },
        content = { padding ->
            // As listas da tela enxergam o estado da barra (para zerar a cor de
            // "rolado" quando sobem até o topo por código).
            CompositionLocalProvider(LocalTopBarState provides topBarState) {
                Box(Modifier.fillMaxSize()) {
                    // O mini player flutua por cima da página: a lista ganha
                    // folga embaixo para o último item não ficar atrás dele.
                    val overlay = LocalBottomOverlay.current
                    content(
                        PaddingValues(
                            top = padding.calculateTopPadding(),
                            bottom = padding.calculateBottomPadding() + overlay,
                        ),
                    )
                    // Tela sem barra presa (título na página): o voltar fica parado
                    // no canto, por cima da página, e não sobe junto ao rolar.
                    if (topBar != null && onBack != null) BoomFloatingBackButton(onClick = onBack)
                }
            }
        },
    )
}

/**
 * Altura do que flutua por cima da parte de baixo das telas (o mini player).
 * Ele não tem faixa de fundo própria: a página continua por trás dele, e as
 * listas usam isto como folga no fim.
 */
val LocalBottomOverlay = compositionLocalOf { 0.dp }

/**
 * Estado da barra superior da tela atual. Rolar a lista por código (tocar de
 * novo na aba) não passa pela barra, que ficaria com a cor de "rolado";
 * [ScrollToTopOnReselect] usa isto para devolver a cor normal.
 */
@OptIn(ExperimentalMaterial3Api::class)
val LocalTopBarState = staticCompositionLocalOf<TopAppBarState?> { null }

