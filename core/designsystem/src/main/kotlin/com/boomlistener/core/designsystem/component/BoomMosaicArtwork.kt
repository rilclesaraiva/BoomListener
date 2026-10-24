// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Capa de playlist montada com quatro capas (2 × 2), como as mixtapes:
 * deixa claro que é uma coleção de várias músicas, e não um álbum ou um
 * artista. Capa que ainda não carregou fica no tom do tema.
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
    Column(sized.clip(corners).background(MaterialTheme.colorScheme.secondaryContainer)) {
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

@Composable
private fun RowScope.Cell(image: ImageBitmap?) {
    Box(Modifier.weight(1f).fillMaxSize()) {
        if (image != null) {
            Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
