// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Picture
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val WRAPPED_FILE_PREFIX = "boomlistener_wrapped_"

/** Guarda o desenho do cartão que está na tela (o que vira a imagem de compartilhar). */
internal class WrappedCapture {
    var picture: Picture? = null
}

internal fun Picture.toBitmap(): Bitmap =
    Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
        android.graphics.Canvas(it).drawPicture(this)
    }

/**
 * Grava o cartão como PNG no cache, na pasta "share" que o FileProvider do app já
 * libera (a mesma do banner da música), e abre o "compartilhar com…". Só imagem:
 * o Wrapped nunca manda áudio.
 */
internal suspend fun Context.shareWrappedCard(card: Bitmap, chooserTitle: String) {
    val file = withContext(Dispatchers.IO) {
        // Só apaga as imagens do Wrapped anteriores: os arquivos do banner da música (a mesma pasta)
        // podem estar sendo lidos por outro app e não são daqui.
        val dir = File(cacheDir, "share").apply { mkdirs() }
        dir.listFiles { file -> file.name.startsWith(WRAPPED_FILE_PREFIX) }?.forEach { it.delete() }
        File(dir, "$WRAPPED_FILE_PREFIX${System.currentTimeMillis()}.png").also { file ->
            file.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    val uri = FileProvider.getUriForFile(this, "$packageName.share", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
