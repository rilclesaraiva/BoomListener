// Boom, listener! — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.settings

/** Um bloco das notas da versão (o Markdown do CHANGELOG, simplificado). */
internal sealed interface NoteBlock {
    data class Heading(val level: Int, val text: String) : NoteBlock
    data class Bullet(val depth: Int, val text: String) : NoteBlock
    data class Paragraph(val text: String) : NoteBlock
}

private val HEADING = Regex("""^(#{1,6})\s+(.*)$""")
private val BULLET = Regex("""^(\s*)[-*+]\s+(.*)$""")
private val LINK = Regex("""\[([^\]]+)]\([^)]*\)""")

/**
 * Lê as notas da Release: títulos (#), itens (- ou *, com recuo para subitens)
 * e parágrafos. Links viram só o texto e `código` perde as crases; o negrito
 * (**…**) fica para quem desenha.
 */
internal fun parseReleaseNotes(markdown: String): List<NoteBlock> {
    val blocks = mutableListOf<NoteBlock>()
    val paragraph = StringBuilder()
    fun flush() {
        if (paragraph.isNotBlank()) blocks += NoteBlock.Paragraph(cleanInline(paragraph.toString().trim()))
        paragraph.clear()
    }
    for (raw in markdown.replace("\r\n", "\n").lines()) {
        val line = raw.trimEnd()
        val heading = HEADING.find(line.trimStart())
        val bullet = BULLET.find(line)
        when {
            line.isBlank() -> flush()
            heading != null -> {
                flush()
                blocks += NoteBlock.Heading(heading.groupValues[1].length, cleanInline(heading.groupValues[2]))
            }
            bullet != null -> {
                flush()
                val indent = bullet.groupValues[1].replace("\t", "  ").length
                blocks += NoteBlock.Bullet(depth = indent / 2, text = cleanInline(bullet.groupValues[2]))
            }
            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(line.trim())
            }
        }
    }
    flush()
    return blocks
}

private fun cleanInline(text: String): String =
    text.replace(LINK, "$1").replace("`", "").trim()

/** Pedaços de um texto com **negrito**: (texto, negrito?). */
internal fun boldRuns(text: String): List<Pair<String, Boolean>> {
    val parts = text.split("**")
    // Número par de "**": o último ficou sem fechar; trata como texto comum.
    if (parts.size % 2 == 0) return listOf(text.replace("**", "") to false)
    return parts.mapIndexedNotNull { i, part -> part.takeIf { it.isNotEmpty() }?.let { it to (i % 2 == 1) } }
}
