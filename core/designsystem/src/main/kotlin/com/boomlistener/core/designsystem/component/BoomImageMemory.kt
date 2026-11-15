// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.core.designsystem.component

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.boomlistener.core.common.DiagnosticLog

/**
 * Capas e fotos já carregadas, à mão **na hora** (sem esperar uma carga assíncrona):
 * um item que volta à tela já nasce com a imagem em vez de passar pela inicial ou
 * pela capa de reserva. A chave tem de dizer tudo o que muda a imagem (de onde
 * vem, tamanho e, nas capas, a versão da biblioteca).
 *
 * Duas camadas: a memória comum, que descarta o que foi menos usado (1/8 da
 * memória que o Android dá ao app), e os **fixados** por grupo, que não saem:
 * os primeiros itens de cada lista, para onde o toque na aba "sobe ao topo" cai.
 */
object BoomImageMemory {
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = (value.width * value.height * 4 / 1024).coerceAtLeast(1)

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: ImageBitmap, newValue: ImageBitmap?) {
            if (evicted) DiagnosticLog.log("imagem", "saiu da memória por falta de espaço (${size() / 1024} de ${maxSize() / 1024} MB)")
        }
    }
    private val pins = HashMap<String, HashMap<String, ImageBitmap>>()

    @Synchronized
    fun get(key: String): ImageBitmap? {
        for (group in pins.values) group[key]?.let { return it }
        return cache.get(key)
    }

    @Synchronized
    fun put(key: String, image: ImageBitmap) {
        cache.put(key, image)
    }

    @Synchronized
    internal fun pin(group: String, key: String, image: ImageBitmap) {
        pins.getOrPut(group) { HashMap() }[key] = image
    }

    /** Solta do [group] o que não está em [keys] (outra lista, outro tamanho, outra versão). */
    @Synchronized
    internal fun keepOnly(group: String, keys: Set<String>) {
        pins[group]?.keys?.retainAll(keys)
    }
}

/**
 * A imagem de [memoryKey]: já na memória, aparece de imediato; senão, é carregada
 * por [load] e guardada. [memoryKey] nulo = sem imagem.
 */
@Composable
fun rememberMemoryImage(memoryKey: String?, load: suspend () -> Bitmap?): ImageBitmap? {
    val image by produceState(initialValue = memoryKey?.let { BoomImageMemory.get(it) }, memoryKey) {
        if (memoryKey == null) {
            value = null
            return@produceState
        }
        BoomImageMemory.get(memoryKey)?.let {
            value = it
            return@produceState
        }
        value = load()?.asImageBitmap()?.also { BoomImageMemory.put(memoryKey, it) }
    }
    return image
}

/** Uma imagem para deixar fixada: a chave da memória e como carregá-la. */
class PinItem(val key: String, val load: suspend () -> Bitmap?)

/**
 * Carrega, uma de cada vez e em segundo plano, as [items] (os primeiros da lista)
 * e as deixa fixadas no [group]. Para ao passar de [budgetMb] de memória, para
 * não pesar quando as imagens são grandes.
 */
@Composable
fun PinImages(group: String, items: List<PinItem>, budgetMb: Int = 32) {
    LaunchedEffect(group, items.map { it.key }) {
        BoomImageMemory.keepOnly(group, items.mapTo(HashSet()) { it.key })
        var bytes = 0L
        for (item in items) {
            if (bytes > budgetMb * 1024L * 1024L) break
            val image = BoomImageMemory.get(item.key) ?: item.load()?.asImageBitmap()?.also { BoomImageMemory.put(item.key, it) }
            if (image != null) {
                BoomImageMemory.pin(group, item.key, image)
                bytes += image.width.toLong() * image.height * 4
            }
        }
    }
}
