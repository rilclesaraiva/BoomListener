// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import com.boomlistener.core.common.DiagnosticLog
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.R

/** Área (na raiz) do campo de busca que está com o foco agora; só um campo tem o foco por vez. */
private object FocusedSearchField {
    private var owner: Any? = null
    var bounds: Rect? = null
        private set

    fun set(token: Any, rect: Rect?) {
        owner = token
        bounds = rect
    }

    fun clear(token: Any) {
        if (owner === token) {
            owner = null
            bounds = null
        }
    }
}

/**
 * Toque (sem deslizar) fora do campo de busca focado tira o foco dele e fecha o teclado.
 * Vai na raiz do app e dentro do [BoomModalSheet] (que é outra janela): vale para toda [BoomSearchField]. Olha o toque antes dos filhos
 * e não o consome, então rolar a lista (deslizar) não faz o campo perder o foco.
 */
@Composable
fun Modifier.clearSearchFocusOnTapOutside(): Modifier {
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val slop = LocalViewConfiguration.current.touchSlop
    // Onde este elemento está na raiz da janela: o toque vem relativo a ele, a área do campo vem da raiz.
    val origin = remember { arrayOf(Offset.Zero) }
    return onGloballyPositioned { origin[0] = it.positionInRoot() }.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val field = FocusedSearchField.bounds ?: return@awaitEachGesture
            if (field.contains(down.position + origin[0])) return@awaitEachGesture
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.size > 1) return@awaitEachGesture
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                if ((change.position - down.position).getDistance() > slop) return@awaitEachGesture
                if (change.changedToUp()) {
                    focus.clearFocus()
                    keyboard?.hide()
                    return@awaitEachGesture
                }
            }
        }
    }
}

/**
 * Barra de busca em pílula, para ficar dentro da tela (e não no topo):
 * lupa, o texto e um "x" para limpar.
 */
@Composable
fun BoomSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    /** Chamado quando o campo ganha o foco (para a tela rolar até ele). */
    onFocused: () -> Unit = {},
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    var focused by remember { mutableStateOf(false) }
    val token = remember { Any() }
    val bounds = remember { arrayOfNulls<Rect>(1) }
    DisposableEffect(token) { onDispose { FocusedSearchField.clear(token) } }
    // Teclado fechado (botão de voltar, gesto, esconder): o campo perde o foco, em vez de ficar
    // com o cursor piscando como se o teclado ainda estivesse aberto. Vale para toda busca do app.
    LaunchedEffect(focused) {
        if (!focused) return@LaunchedEffect
        var wasVisible = false
        snapshotFlow { imeInsets.getBottom(density) > 0 }.collect { visible ->
            if (wasVisible && !visible) focus.clearFocus()
            wasVisible = visible
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = CircleShape,
        modifier = modifier.height(48.dp).onGloballyPositioned {
            bounds[0] = it.boundsInRoot()
            if (focused) FocusedSearchField.set(token, bounds[0])
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 14.dp)) {
            Icon(BoomIcons.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    modifier = Modifier.fillMaxWidth().onFocusChanged {
                        DiagnosticLog.log("teclado", "campo de busca "+ if (it.isFocused) "ganhou o foco" else "perdeu o foco")
                        focused = it.isFocused
                        if (it.isFocused) FocusedSearchField.set(token, bounds[0]) else FocusedSearchField.clear(token)
                        if (it.isFocused) onFocused()
                    },
                )
            }
            if (query.isNotEmpty()) {
                // Só apaga o texto: o foco (e o teclado) ficam como estão.
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(BoomIcons.Close, contentDescription = stringResource(R.string.core_designsystem_clear))
                }
            } else {
                Spacer(Modifier.width(14.dp))
            }
        }
    }
}
