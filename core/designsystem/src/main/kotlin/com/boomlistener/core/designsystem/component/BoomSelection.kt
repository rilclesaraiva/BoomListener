// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.R
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomSpatialSpec

/*
 * Seleção de várias músicas numa lista (no estilo do Gmail): tocar na capa
 * marca a música; com alguma marcada, tocar na linha também marca, e uma barra
 * no topo mostra quantas são, "Selecionar tudo" e as ações. "Voltar" fecha a
 * seleção antes de sair da tela.
 */

/** As músicas marcadas numa lista, pelo endereço (uri) de cada uma. */
@Stable
class BoomSelection internal constructor(initial: Set<String> = emptySet()) {
    var selected: Set<String> by mutableStateOf(initial)
        private set

    val isActive: Boolean get() = selected.isNotEmpty()
    val count: Int get() = selected.size

    fun isSelected(id: String): Boolean = id in selected

    fun toggle(id: String) {
        selected = if (id in selected) selected - id else selected + id
    }

    fun selectAll(ids: Collection<String>) {
        selected = ids.toSet()
    }

    fun clear() {
        selected = emptySet()
    }

    /** Tira da seleção o que saiu da lista (uma busca nova, uma música removida). */
    fun retain(ids: Set<String>) {
        if (selected.isNotEmpty() && !ids.containsAll(selected)) selected = selected.intersect(ids)
    }

    /** As marcadas, na ordem em que aparecem na lista [all]. */
    fun inOrder(all: List<String>): List<String> = all.filter { it in selected }

    companion object {
        val Saver = listSaver<BoomSelection, String>(
            save = { it.selected.toList() },
            restore = { BoomSelection(it.toSet()) },
        )
    }
}

@Composable
fun rememberBoomSelection(): BoomSelection = rememberSaveable(saver = BoomSelection.Saver) { BoomSelection() }

/**
 * Capa que também é o marcador de seleção: tocar nela marca a música; marcada,
 * vira um quadrado com o visto.
 */
@Composable
fun BoomSelectableArtwork(
    image: ImageBitmap?,
    fallbackText: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = selected,
        animationSpec = boomEffectsSpec(),
        label = "selectableArtwork",
        modifier = modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onToggle() }),
    ) { marked ->
        if (marked) {
            Box(
                modifier = Modifier
                    .size(BoomListArtworkSize)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(BoomIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        } else {
            BoomArtwork(image = image, fallbackText = fallbackText)
        }
    }
}

/**
 * Barra no topo enquanto há músicas marcadas: fechar, quantas são, as [actions]
 * e "Selecionar tudo" ([allIds] é a lista inteira da tela). Fica por cima da
 * página, que continua por baixo.
 */
@Composable
fun BoomSelectionBar(
    selection: BoomSelection,
    allIds: List<String>,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    // "Voltar" fecha a seleção antes de sair da tela.
    BackHandler(enabled = selection.isActive) { selection.clear() }
    // Durante a saída da barra o número não pode zerar antes de ela sumir.
    var shownCount by remember { mutableIntStateOf(0) }
    if (selection.count > 0) shownCount = selection.count
    val allSelected = allIds.isNotEmpty() && selection.count == allIds.size && selection.selected.containsAll(allIds)

    AnimatedVisibility(
        visible = selection.isActive,
        modifier = modifier,
        enter = slideInVertically(boomSpatialSpec()) { -it } + fadeIn(boomEffectsSpec()),
        exit = slideOutVertically(boomSpatialSpec()) { -it } + fadeOut(boomEffectsSpec()),
    ) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = selection::clear) {
                        Icon(BoomIcons.Close, contentDescription = stringResource(R.string.core_designsystem_selection_close))
                    }
                    Text(
                        text = shownCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 12.dp).weight(1f),
                    )
                    actions()
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = allSelected,
                            role = Role.Checkbox,
                            onValueChange = { checked -> if (checked) selection.selectAll(allIds) else selection.clear() },
                        )
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = allSelected, onCheckedChange = null)
                    Text(
                        text = stringResource(R.string.core_designsystem_selection_all),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}
