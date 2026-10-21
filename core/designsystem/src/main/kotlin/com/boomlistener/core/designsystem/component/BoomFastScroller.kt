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
import androidx.compose.runtime.mutableIntStateOf
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
    FastScrollerImpl(
        total = { state.layoutInfo.totalItemsCount },
        visible = { state.layoutInfo.visibleItemsInfo.size },
        position = {
            // Posição contínua (item + quanto dele já passou): a bolinha anda
            // junto com a lista, sem degraus a cada linha.
            val firstItem = state.layoutInfo.visibleItemsInfo.firstOrNull()
            val size = firstItem?.size?.takeIf { it > 0 }
            state.firstVisibleItemIndex + if (size == null) 0f else (state.firstVisibleItemScrollOffset.toFloat() / size).coerceIn(0f, 1f)
        },
        scrolling = { state.isScrollInProgress },
        scrollTo = { position ->
            val index = position.toInt()
            val items = state.layoutInfo.visibleItemsInfo
            val size = items.firstOrNull { it.index == index }?.size
                ?: items.takeIf { it.isNotEmpty() }?.let { list -> list.sumOf { it.size } / list.size }
                ?: 0
            state.scrollToItem(index, ((position - index) * size).roundToInt())
        },
        label = label,
        modifier = modifier,
    )
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
        position = {
            val firstItem = state.layoutInfo.visibleItemsInfo.firstOrNull()
            val height = firstItem?.size?.height?.takeIf { it > 0 }
            state.firstVisibleItemIndex + if (height == null) 0f else (state.firstVisibleItemScrollOffset.toFloat() / height).coerceIn(0f, 1f)
        },
        scrolling = { state.isScrollInProgress },
        scrollTo = { position -> state.scrollToItem(position.toInt()) },
        label = label,
        modifier = modifier,
    )
}

@Composable
private fun FastScrollerImpl(
    total: () -> Int,
    visible: () -> Int,
    position: () -> Float,
    scrolling: () -> Boolean,
    scrollTo: suspend (Float) -> Unit,
    label: (Int) -> String?,
    modifier: Modifier,
) {
    val enoughItems by remember { derivedStateOf { total() >= MIN_ITEMS && total() > visible() } }
    if (!enoughItems) return

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableStateOf(0f) }
    var targetIndex by remember { mutableIntStateOf(0) }
    // Só a última posição pedida importa: rola até ela e descarta as que
    // ficaram para trás (senão a lista vai aos trancos atrás do dedo).
    var target by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(Unit) {
        snapshotFlow { target }.filterNotNull().collectLatest { scrollTo(it) }
    }
    val isScrolling by remember { derivedStateOf(scrolling) }
    // Quantas linhas cabem na tela: o maior valor visto. Contar a cada quadro
    // mudaria ao passar por seções altas (o topo do Início) e a bolinha pularia.
    var screenItems by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        snapshotFlow { visible() }.collect { if (it > screenItems) screenItems = it }
    }
    val range = { (total() - maxOf(screenItems, visible())).coerceAtLeast(1) }

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

    val listFraction by remember {
        derivedStateOf {
            (position() / range()).coerceIn(0f, 1f)
        }
    }
    val fraction = if (dragging) dragFraction else listFraction

    BoxWithConstraints(modifier.fillMaxHeight().padding(vertical = 8.dp)) {
        val density = LocalDensity.current
        val trackPx = with(density) { maxHeight.toPx() }
        val thumbPx = with(density) { ThumbHeight.toPx() }
        val thumbY = ((trackPx - thumbPx) * fraction).roundToInt()

        fun jump(y: Float) {
            dragFraction = (y / trackPx).coerceIn(0f, 1f)
            // A mesma conta da bolinha ao contrário: o dedo e a lista andam juntos.
            val position = dragFraction * range()
            targetIndex = position.toInt().coerceIn(0, (total() - 1).coerceAtLeast(0))
            target = position
        }

        // Balão com a letra, ao lado da bolinha, enquanto arrasta.
        if (dragging) {
            label(targetIndex)?.let { text ->
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
