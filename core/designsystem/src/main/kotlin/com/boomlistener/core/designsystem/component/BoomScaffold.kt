// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

/**
 * Estrutura padrão de toda tela: barra superior + conteúdo.
 *
 * A barra inferior de navegação fica no :app, então aqui o Scaffold não
 * reserva espaço para as barras do sistema embaixo (evita espaço em dobro).
 *
 * @param onBack mostra a seta de voltar quando não é null.
 * @param largeTitle título grande que encolhe ao rolar (telas principais, como Ajustes).
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
    content: @Composable (PaddingValues) -> Unit,
) {
    val topBarState = rememberTopAppBarState()
    val scrollBehavior = if (largeTitle) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topBarState)
    } else {
        TopAppBarDefaults.pinnedScrollBehavior(topBarState)
    }
    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) { Icon(BoomIcons.Back, contentDescription = "Voltar") }
        }
    }
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = topBar ?: {
            if (largeTitle) {
                LargeTopAppBar(
                    title = { Text(title) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            } else {
                TopAppBar(
                    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                )
            }
        },
        content = content,
    )
}

/**
 * Barra superior em modo de busca: campo de texto no lugar do título, que já
 * abre com o teclado. [onClose] sai da busca.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoomSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    placeholder: String,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onClose) { Icon(BoomIcons.Back, contentDescription = "Fechar busca") }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(placeholder) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) { Icon(BoomIcons.Close, contentDescription = "Limpar") }
            }
        },
    )
}
