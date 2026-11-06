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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
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
 * Controles flutuantes no topo enquanto há músicas marcadas (no estilo do
 * Fotos): à esquerda uma pílula com fechar e quantas são; à direita, outra com
 * "selecionar tudo" e o ⋮ com as [actions]. Sem faixa de fundo: a página
 * continua visível por trás. [allIds] é a lista inteira da tela.
 */
@Composable
fun BoomSelectionBar(
    selection: BoomSelection,
    allIds: List<String>,
    actions: List<BoomMenuAction>,
    modifier: Modifier = Modifier,
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
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 3.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = selection::clear) {
                        Icon(BoomIcons.Close, contentDescription = stringResource(R.string.core_designsystem_selection_close))
                    }
                    Text(
                        text = shownCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 20.dp, start = 4.dp),
                    )
                }
            }
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHighest, shadowElevation = 3.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconToggleButton(
                        checked = allSelected,
                        onCheckedChange = { checked -> if (checked) selection.selectAll(allIds) else selection.clear() },
                    ) {
                        Icon(
                            BoomIcons.SelectAll,
                            contentDescription = stringResource(R.string.core_designsystem_selection_all),
                            tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    BoomMoreMenu(actions = actions)
                }
            }
        }
    }
}
