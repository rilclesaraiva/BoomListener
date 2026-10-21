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
 */
@Composable
fun BoomFastScroller(
    state: LazyListState,
    label: (index: Int) -> String?,
    modifier: Modifier = Modifier,
) {
    val metrics = remember(state) { ListScrollMetrics(state) }
    FastScrollerImpl(
        total = { state.layoutInfo.totalItemsCount },
        visible = { state.layoutInfo.visibleItemsInfo.size },
        fraction = metrics::fraction,
        topIndex = metrics::topIndex,
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
private class ListScrollMetrics(private val state: LazyListState) {
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
        return info.totalItemsCount * row - viewport
    }

    fun fraction(): Float {
        val row = row()
        val max = maxScroll(row)
        if (max <= 0f) return 0f
        if (!state.canScrollForward) return 1f
        val scrolled = state.firstVisibleItemIndex * row + state.firstVisibleItemScrollOffset
        return (scrolled / max).coerceIn(0f, 1f)
    }

    suspend fun scrollTo(fraction: Float) {
        val row = row()
        val target = (fraction * maxScroll(row)).coerceAtLeast(0f)
        val index = (target / row).toInt().coerceIn(0, (state.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
        state.scrollToItem(index, (target - index * row).roundToInt())
    }

    /**
     * O item que está mesmo no topo: o primeiro com mais da metade à mostra
     * (ignora o cabeçalho preso, que tem índice menor). É o do balão.
     */
    fun topIndex(): Int {
        val first = state.firstVisibleItemIndex
        return state.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index >= first && it.offset + it.size / 2 >= 0 }
            ?.index ?: first
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
        topIndex = { state.firstVisibleItemIndex },
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
    total: () -> Int,
    visible: () -> Int,
    fraction: () -> Float,
    topIndex: () -> Int,
    scrolling: () -> Boolean,
    scrollToFraction: suspend (Float) -> Unit,
    label: (Int) -> String?,
    modifier: Modifier,
) {
    val enoughItems by remember { derivedStateOf { total() >= MIN_ITEMS && total() > visible() } }
    if (!enoughItems) return

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    // Só a última posição pedida importa: rola até ela e descarta as que
    // ficaram para trás (senão a lista vai aos trancos atrás do dedo).
    var target by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(Unit) {
        snapshotFlow { target }.filterNotNull().collectLatest { scrollToFraction(it) }
    }
    val isScrolling by remember { derivedStateOf(scrolling) }
    // O balão mostra o rótulo do item que está de fato no topo da lista.
    val shownIndex by remember { derivedStateOf(topIndex) }

    // Aparece ao rolar ou arrastar; some um pouco depois de parar.
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(isScrolling, dragging) {
        if (isScrolling || dragging) {
            shown = true
        } else {
            delay(HIDE_AFTER_MS)
            shown = false
        }
    }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, boomEffectsSpec(), label = "fastScrollAlpha")
    val thumbWidth by animateDpAsState(if (dragging) ThumbWidthDragging else ThumbWidth, boomFastSpatialSpec(), label = "thumbWidth")

    val listFraction by remember { derivedStateOf(fraction) }
    val currentFraction = if (dragging) dragFraction else listFraction

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
