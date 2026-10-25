// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.boomlistener.core.designsystem.theme.boomEffectsSpec
import com.boomlistener.core.designsystem.theme.boomFastSpatialSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import java.text.Normalizer
import kotlin.math.roundToInt

private val TrackWidth = 36.dp
private val ThumbHeight = 52.dp
private val ThumbWidth = 6.dp
private val ThumbWidthDragging = 10.dp
private val BubbleSize = 64.dp
private const val HIDE_AFTER_MS = 1_500L

/** Abaixo disso a lista é curta e a barra só atrapalharia. */
private const val MIN_ITEMS = 25

/**
 * Barra de rolagem rápida da lista: aparece ao rolar, e arrastar a bolinha
 * pula pela lista mostrando num balão a letra (ou o rótulo) de onde está.
 * Fica por cima da lista, alinhada à direita:
 * `Box { LazyColumn(...); BoomFastScroller(state, label, Modifier.align(Alignment.CenterEnd)) }`.
 *
 * @param label rótulo do item de posição (na lista inteira, contando
 *   cabeçalhos) — normalmente [boomIndexLabel] do título; null não mostra balão.
 * @param startIndex a barra só cobre a lista a partir deste item (ex.: a seção
 *   "Músicas" do Início): só aparece com ele no topo, e o começo do trilho é ele.
 */
@Composable
fun BoomFastScroller(
    state: LazyListState,
    label: (index: Int) -> String?,
    modifier: Modifier = Modifier,
    startIndex: Int = 0,
    /** A partir de quantos itens a barrinha aparece (listas curtas que ainda rolam podem pedir menos). */
    minItems: Int = MIN_ITEMS,
) {
    val metrics = remember(state, startIndex) { ListScrollMetrics(state, startIndex) }
    FastScrollerImpl(
        minItems = minItems,
        total = { state.layoutInfo.totalItemsCount },
        startIndex = startIndex,
        inSection = { state.firstVisibleItemIndex >= startIndex },
        visible = { state.layoutInfo.visibleItemsInfo.size },
        fraction = metrics::fraction,
        scrolling = { state.isScrollInProgress },
        scrollToFraction = metrics::scrollTo,
        label = label,
        modifier = modifier,
    )
}

/**
 * Onde a lista está, em pixels: a altura de uma linha comum (a do meio das
 * que estão na tela) vezes o número de itens. Assim a bolinha anda contínua,
 * chega ao fim junto com a lista, e arrastar até embaixo leva ao último item.
 */
private class ListScrollMetrics(private val state: LazyListState, private val start: Int) {
    /** Altura de uma linha comum (com o espaço entre linhas); 0 = ainda não mediu. */
    private var rowPx = 0f

    private fun row(): Float {
        val info = state.layoutInfo
        val sizes = info.visibleItemsInfo.map { it.size }.sorted()
        // Com poucos itens na tela (seções altas do topo), fica a medida de antes.
        if (sizes.size >= 3 || rowPx == 0f) {
            sizes.getOrNull(sizes.size / 2)?.let { rowPx = (it + info.mainAxisItemSpacing).toFloat() }
        }
        return rowPx.coerceAtLeast(1f)
    }

    private fun maxScroll(row: Float): Float {
        val info = state.layoutInfo
        val viewport = info.viewportSize.height - info.beforeContentPadding - info.afterContentPadding
        return (info.totalItemsCount - start) * row - viewport
    }

    fun fraction(): Float {
        val row = row()
        val max = maxScroll(row)
        if (max <= 0f) return 0f
        if (!state.canScrollForward) return 1f
        if (state.firstVisibleItemIndex < start) return 0f
        val scrolled = (state.firstVisibleItemIndex - start) * row + state.firstVisibleItemScrollOffset
        return (scrolled / max).coerceIn(0f, 1f)
    }

    suspend fun scrollTo(fraction: Float) {
        val row = row()
        val target = (fraction * maxScroll(row)).coerceAtLeast(0f)
        val last = (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(start)
        val index = (start + (target / row).toInt()).coerceIn(start, last)
        state.scrollToItem(index, (target - (index - start) * row).roundToInt().coerceAtLeast(0))
    }
}

/** O mesmo, para grades (álbuns, artistas). */
@Composable
fun BoomFastScroller(
    state: LazyGridState,
    label: (index: Int) -> String?,
    modifier: Modifier = Modifier,
) {
    FastScrollerImpl(
        total = { state.layoutInfo.totalItemsCount },
        visible = { state.layoutInfo.visibleItemsInfo.size },
        fraction = {
            val range = (state.layoutInfo.totalItemsCount - state.layoutInfo.visibleItemsInfo.size).coerceAtLeast(1)
            if (!state.canScrollForward) 1f else (state.firstVisibleItemIndex.toFloat() / range).coerceIn(0f, 1f)
        },
        scrolling = { state.isScrollInProgress },
        scrollToFraction = { fraction ->
            val range = (state.layoutInfo.totalItemsCount - state.layoutInfo.visibleItemsInfo.size).coerceAtLeast(0)
            state.scrollToItem((fraction * range).roundToInt())
        },
        label = label,
        modifier = modifier,
    )
}

@Composable
private fun FastScrollerImpl(
    minItems: Int = MIN_ITEMS,
    total: () -> Int,
    startIndex: Int = 0,
    inSection: () -> Boolean = { true },
    visible: () -> Int,
    fraction: () -> Float,
    scrolling: () -> Boolean,
    scrollToFraction: suspend (Float) -> Unit,
    label: (Int) -> String?,
    modifier: Modifier,
) {
    val enoughItems by remember { derivedStateOf { total() - startIndex >= minItems && total() > visible() } }
    if (!enoughItems) return
    // Fora da seção (ex.: o topo do Início, acima de "Músicas"), a barra some.
    val active by remember { derivedStateOf(inSection) }

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    // Só a última posição pedida importa: rola até ela e descarta as que
    // ficaram para trás (senão a lista vai aos trancos atrás do dedo).
    var target by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(Unit) {
        snapshotFlow { target }.filterNotNull().collectLatest { scrollToFraction(it) }
    }
    val isScrolling by remember { derivedStateOf(scrolling) }

    // Aparece ao rolar ou arrastar; some um pouco depois de parar.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(isScrolling, dragging, active) {
        if (dragging || (isScrolling && active)) {
            shown = true
        } else if (!active) {
            shown = false
        } else {
            delay(HIDE_AFTER_MS)
            shown = false
        }
    }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, boomEffectsSpec(), label = "fastScrollAlpha")
    val thumbWidth by animateDpAsState(if (dragging) ThumbWidthDragging else ThumbWidth, boomFastSpatialSpec(), label = "thumbWidth")

    val listFraction by remember { derivedStateOf(fraction) }
    val currentFraction = if (dragging) dragFraction else listFraction
    // O balão segue a bolinha: no topo do trilho, o primeiro item; no fim, o
    // último; no meio, o item proporcional (o que está perto da bolinha na tela).
    // Antes era o item do topo da tela, e com a bolinha lá embaixo o balão
    // mostrava uma letra lá de cima.
    val shownIndex = startIndex + (currentFraction * (total() - 1 - startIndex).coerceAtLeast(0)).roundToInt()

    BoxWithConstraints(modifier.fillMaxHeight().padding(vertical = 8.dp)) {
        val density = LocalDensity.current
        val trackPx = with(density) { maxHeight.toPx() }
        val thumbPx = with(density) { ThumbHeight.toPx() }
        val thumbY = ((trackPx - thumbPx) * currentFraction).roundToInt()

        fun jump(y: Float) {
            // O centro da bolinha fica sob o dedo; o topo e o fim do trilho são o começo e o fim da lista.
            dragFraction = ((y - thumbPx / 2) / (trackPx - thumbPx)).coerceIn(0f, 1f)
            target = dragFraction
        }

        // Balão com a letra, ao lado da bolinha, enquanto arrasta.
        if (dragging) {
            label(shownIndex)?.let { text ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset {
                            val bubble = BubbleSize.roundToPx()
                            IntOffset(
                                x = -(TrackWidth.roundToPx() + 8.dp.roundToPx()),
                                y = (thumbY + thumbPx.roundToInt() / 2 - bubble / 2).coerceAtLeast(0),
                            )
                        }
                        .height(BubbleSize)
                        .widthIn(min = BubbleSize),
                ) {
                    // Uma letra fica num círculo; um rótulo maior ("Ontem") vira pílula.
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text,
                            style = if (text.length <= 2) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.TopEnd)
                .width(TrackWidth)
                .fillMaxHeight()
                .alpha(alpha)
                // Só pega o toque quando está à mostra: escondida, a lista rola normal.
                .then(
                    if (shown) {
                        Modifier.pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { offset ->
                                    dragging = true
                                    jump(offset.y)
                                },
                                onDragEnd = { dragging = false },
                                onDragCancel = { dragging = false },
                                onVerticalDrag = { change, _ ->
                                    change.consume()
                                    jump(change.position.y)
                                },
                            )
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, thumbY) }
                    .size(width = thumbWidth, height = ThumbHeight)
                    // Na cor do tema (o cinza destoava no AMOLED).
                    .background(
                        if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        CircleShape,
                    ),
            )
        }
    }
}

/**
 * Letra do índice para um título: sem acento ("Ávila" → "A"), ignorando
 * aspas e símbolos do começo; número ou outro alfabeto viram "#".
 */
fun boomIndexLabel(title: String?): String {
    val first = title?.trim()?.firstOrNull { it.isLetterOrDigit() } ?: return "#"
    val plain = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
        .firstOrNull()?.uppercaseChar() ?: return "#"
    return if (plain in 'A'..'Z') plain.toString() else "#"
}
