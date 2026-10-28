// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Capa de playlist montada com as capas de álbuns diferentes, como as
 * mixtapes: deixa claro que é uma coleção, e não um álbum ou um artista.
 * A forma depende de quantas capas vêm (nunca repete uma capa):
 *
 * - 2: as duas lado a lado, cortadas na vertical.
 * - 3: em "Y", uma em cima (entre os braços do Y) e duas dos lados.
 * - 4: 2 × 2.
 *
 * Capa que ainda não carregou fica no tom do tema.
 *
 * @param size null = ocupa o espaço que o [modifier] der (quadrada por fora).
 * @param shape null = os cantos do [BoomArtwork] do mesmo tamanho; use
 *   [RectangleShape] para a capa grande do topo da playlist.
 */
@Composable
fun BoomMosaicArtwork(
    images: List<ImageBitmap?>,
    modifier: Modifier = Modifier,
    size: Dp? = BoomListArtworkSize,
    shape: Shape? = null,
) {
    val sized = if (size != null) modifier.size(size) else modifier
    val corners = shape ?: if (size == null || size > 96.dp) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.medium
    val box = sized.clip(corners).background(MaterialTheme.colorScheme.secondaryContainer)
    when (images.size) {
        2 -> Row(box) {
            Cell(images[0])
            Cell(images[1])
        }
        3 -> Box(box) {
            // Cada capa ocupa um pedaço do "Y": o centro fica a 45% da altura,
            // os braços vão aos cantos de cima e a perna desce até o meio de baixo.
            YPiece(images[0], YPart.Top)
            YPiece(images[1], YPart.Left)
            YPiece(images[2], YPart.Right)
        }
        else -> Column(box) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Cell(images.getOrNull(0))
                Cell(images.getOrNull(1))
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Cell(images.getOrNull(2))
                Cell(images.getOrNull(3))
            }
        }
    }
}

@Composable
private fun RowScope.Cell(image: ImageBitmap?) {
    Box(Modifier.weight(1f).fillMaxSize()) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

private enum class YPart { Top, Left, Right }

/** Altura do centro do "Y" (fração da altura). */
private const val Y_CENTER = 0.45f

/** Um pedaço do "Y", com a capa centrada na parte que aparece. */
@Composable
private fun YPiece(image: ImageBitmap?, part: YPart) {
    if (image == null) return
    val shape = GenericShape { size, _ ->
        val w = size.width
        val h = size.height
        val cy = h * Y_CENTER
        when (part) {
            YPart.Top -> {
                moveTo(0f, 0f); lineTo(w, 0f); lineTo(w / 2, cy); close()
            }
            YPart.Left -> {
                moveTo(0f, 0f); lineTo(w / 2, cy); lineTo(w / 2, h); lineTo(0f, h); close()
            }
            YPart.Right -> {
                moveTo(w, 0f); lineTo(w / 2, cy); lineTo(w / 2, h); lineTo(w, h); close()
            }
        }
    }
    // Alinha a capa ao pedaço dela, para o meio da arte cair na parte visível.
    val alignment = when (part) {
        YPart.Top -> Alignment.TopCenter
        YPart.Left -> Alignment.CenterStart
        YPart.Right -> Alignment.CenterEnd
    }
    val fill = when (part) {
        YPart.Top -> Modifier.fillMaxWidth().fillMaxHeight(Y_CENTER)
        YPart.Left, YPart.Right -> Modifier.fillMaxWidth(0.5f).fillMaxHeight()
    }
    Box(Modifier.fillMaxSize().clip(shape)) {
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = fill.align(alignment),
        )
    }
}
