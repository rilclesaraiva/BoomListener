// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

/** Altura máxima do painel, em fração da tela. */
private const val MAX_SHEET_FRACTION = 0.88f

/**
 * Painel que sobe de baixo, usado no app inteiro no lugar do
 * `ModalBottomSheet` direto.
 *
 * A diferença é o "voltar": o do M3 encolhe o painel com a animação do
 * sistema enquanto o gesto acontece. Aqui o voltar é nosso: o painel fecha
 * descendo, com a mesma animação de fechar de sempre.
 *
 * O estado do painel (experimental no M3) fica só aqui dentro: quem usa não
 * precisa de `@OptIn`. Para fechar descendo a partir de um botão do
 * conteúdo, use [BoomSheetScope.dismiss].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoomModalSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetGesturesEnabled: Boolean = true,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    /** Falso enquanto o painel não pode fechar (ex.: salvando). */
    dismissEnabled: Boolean = true,
    /**
     * Só a faixa de cima (a do puxador) arrasta o painel: rolar o conteúdo até
     * o topo e continuar puxando não o fecha. Para painéis com coisa a perder
     * (ex.: Editar perfil), que fechavam fácil demais.
     */
    dragOnlyFromTop: Boolean = false,
    content: @Composable BoomSheetScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss by rememberUpdatedState(onDismissRequest)
    // Desce o painel e só então fecha.
    val hideThenDismiss: () -> Unit = { scope.launch { sheetState.hide() }.invokeOnCompletion { dismiss() } }
    ModalBottomSheet(
        onDismissRequest = { if (dismissEnabled) dismiss() },
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        containerColor = containerColor,
        contentColor = contentColor,
        // Sem o voltar do M3 (e a animação de encolher do sistema).
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false),
    ) {
        // Voltar (botão ou gesto concluído): desce o painel e só então fecha.
        BackHandler(enabled = dismissEnabled, onBack = hideThenDismiss)
        // Nunca chega ao topo da tela: a faixa de cima que sobra mostra que o
        // painel é de puxar (para baixo, fecha).
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * MAX_SHEET_FRACTION
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .then(if (dragOnlyFromTop) Modifier.nestedScroll(KeepScrollInside) else Modifier),
        ) {
            val sheetScope = remember(this) { BoomSheetScopeImpl(this, hideThenDismiss) }
            sheetScope.content()
        }
    }
}

/** O conteúdo do [BoomModalSheet]: uma coluna que também sabe fechar o painel descendo. */
interface BoomSheetScope : ColumnScope {
    /** Fecha o painel com a animação de descer (e chama o `onDismissRequest` no fim). */
    fun dismiss()
}

private class BoomSheetScopeImpl(column: ColumnScope, private val close: () -> Unit) :
    BoomSheetScope, ColumnScope by column {
    override fun dismiss() = close()
}

/**
 * Fica com toda a rolagem que o conteúdo não usou (puxar além do topo), para
 * ela não chegar ao painel e arrastá-lo. O puxador, fora do conteúdo, continua arrastando.
 */
private object KeepScrollInside : NestedScrollConnection {
    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset = available
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
}
