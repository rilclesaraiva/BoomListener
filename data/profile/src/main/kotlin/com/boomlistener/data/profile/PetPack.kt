// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/** As poses que o bichinho do perfil sabe fazer. Faltando uma no pacote, a tela usa a mais parecida. */
enum class PetPose { Stand, Walk, Sit, Sleep, Fall, Drag, Dance }

/** Um quadro: o arquivo de imagem (nome em minúsculas, sem pasta) e por quanto tempo fica na tela. */
data class PetFrame(val file: String, val durationMs: Int)

/** Um bichinho já lido: o nome e os quadros de cada pose. Sempre tem a pose [PetPose.Stand]. */
data class PetPack(val name: String, val poses: Map<PetPose, List<PetFrame>>) {
    /** Os quadros de [pose]; sem eles, a pose mais parecida e, no fim, o bichinho parado. */
    fun frames(pose: PetPose): List<PetFrame> {
        val chain = when (pose) {
            PetPose.Stand -> listOf(PetPose.Stand)
            PetPose.Walk -> listOf(PetPose.Walk, PetPose.Stand)
            PetPose.Sit -> listOf(PetPose.Sit, PetPose.Stand)
            PetPose.Sleep -> listOf(PetPose.Sleep, PetPose.Sit, PetPose.Stand)
            PetPose.Fall -> listOf(PetPose.Fall, PetPose.Drag, PetPose.Stand)
            PetPose.Drag -> listOf(PetPose.Drag, PetPose.Fall, PetPose.Stand)
            PetPose.Dance -> listOf(PetPose.Dance, PetPose.Walk, PetPose.Stand)
        }
        return chain.firstNotNullOfOrNull { poses[it]?.takeIf { f -> f.isNotEmpty() } }.orEmpty()
    }

    /** Todos os arquivos de imagem que o bichinho usa. */
    val files: Set<String> get() = poses.values.flatten().map { it.file }.toSet()
}

/** Cada passo da animação nos pacotes de shimeji dura 40 ms (25 quadros por segundo). */
private const val TICK_MS = 40
private const val MIN_FRAME_MS = 80
private const val MAX_FRAME_MS = 1_000
private const val DEFAULT_FRAME_MS = 240

/** Nomes das ações do pacote que valem para cada pose, do mais certo para o menos certo (comparados em minúsculas). */
private val POSE_NAMES: Map<PetPose, List<String>> = mapOf(
    PetPose.Stand to listOf("stand"),
    PetPose.Walk to listOf("walk", "run"),
    PetPose.Sit to listOf("sit"),
    PetPose.Sleep to listOf("sleep", "sprawl"),
    PetPose.Fall to listOf("fall", "falling"),
    PetPose.Drag to listOf("pinched", "dragged", "resisting"),
    PetPose.Dance to listOf("dance"),
)

/** O nome do arquivo sem pasta, em minúsculas ("/img/Shime1.PNG" → "shime1.png"). */
fun petBaseName(path: String): String = path.replace('\\', '/').substringAfterLast('/').trim().lowercase()

/**
 * Lê as ações do `actions.xml` de um pacote de shimeji e devolve os quadros de cada pose.
 * [available] são os arquivos de imagem que vieram no pacote (nomes em minúsculas). Quadro que
 * aponta para imagem que não veio é ignorado. XML com declaração de tipo (DOCTYPE/ENTITY) é recusado.
 */
fun parsePetActions(xml: String, available: Set<String>): Map<PetPose, List<PetFrame>> {
    if (xml.contains("<!DOCTYPE", ignoreCase = true) || xml.contains("<!ENTITY", ignoreCase = true)) return emptyMap()
    val document = runCatching {
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)))
    }.getOrNull() ?: return emptyMap()
    val actions = document.getElementsByTagName("Action")
    // Nome da ação (minúsculas) → quadros da primeira ação com esse nome que tenha imagens.
    val byName = LinkedHashMap<String, List<PetFrame>>()
    for (i in 0 until actions.length) {
        val action = actions.item(i) as? org.w3c.dom.Element ?: continue
        val name = action.getAttribute("Name").trim().lowercase()
        if (name.isEmpty() || name in byName) continue
        val poses = action.getElementsByTagName("Pose")
        val frames = buildList {
            for (j in 0 until poses.length) {
                val pose = poses.item(j) as? org.w3c.dom.Element ?: continue
                val file = petBaseName(pose.getAttribute("Image"))
                if (file !in available) continue
                val ticks = pose.getAttribute("Duration").trim().toIntOrNull()
                val ms = if (ticks == null) DEFAULT_FRAME_MS else (ticks * TICK_MS).coerceIn(MIN_FRAME_MS, MAX_FRAME_MS)
                add(PetFrame(file, ms))
            }
        }
        if (frames.isNotEmpty()) byName[name] = frames
    }
    val result = LinkedHashMap<PetPose, List<PetFrame>>()
    for ((pose, names) in POSE_NAMES) {
        val found = names.firstNotNullOfOrNull { byName[it] }
            ?: names.firstNotNullOfOrNull { wanted -> byName.entries.firstOrNull { it.key.contains(wanted) }?.value }
        if (found != null) result[pose] = found
    }
    return result
}

/** Número dentro do nome do arquivo ("shime12.png" → 12); sem número, null. */
private fun fileNumber(name: String): Int? = Regex("(\\d+)").findAll(name.substringBeforeLast('.')).lastOrNull()?.value?.toIntOrNull()

/**
 * Plano B, para pacote sem `actions.xml` (ou que não deu para ler): as imagens por ordem de número;
 * a primeira é o bichinho parado e, havendo três ou mais, a caminhada alterna a primeira com a segunda e a terceira.
 */
fun fallbackPetPoses(available: Set<String>): Map<PetPose, List<PetFrame>> {
    val images = available.filter { it.endsWith(".png") }
        .sortedWith(compareBy<String> { fileNumber(it) ?: Int.MAX_VALUE }.thenBy { it })
    val first = images.firstOrNull() ?: return emptyMap()
    val stand = listOf(PetFrame(first, DEFAULT_FRAME_MS))
    val poses = linkedMapOf(PetPose.Stand to stand)
    if (images.size >= 3) {
        val walk = listOf(first, images[1], first, images[2]).map { PetFrame(it, DEFAULT_FRAME_MS) }
        poses[PetPose.Walk] = walk
    }
    return poses
}

/** Monta o bichinho: usa o `actions.xml` quando ele traz a pose parada; senão o plano B. Null se não há imagem. */
fun buildPetPack(name: String, actionsXml: String?, available: Set<String>): PetPack? {
    val fromXml = actionsXml?.let { parsePetActions(it, available) }.orEmpty()
    val poses = if (fromXml[PetPose.Stand] != null) fromXml else fallbackPetPoses(available)
    if (poses[PetPose.Stand] == null) return null
    return PetPack(name.trim().take(40), poses)
}

/**
 * Nome com que um arquivo do pacote é guardado, ou null se não serve: só imagens PNG e
 * arquivos XML, sempre pelo nome sem pasta (nada de sair da pasta do bichinho).
 */
fun petEntryName(entryName: String): String? {
    val base = petBaseName(entryName)
    if (base.isEmpty() || base.startsWith(".") || base.contains("..")) return null
    return if (base.endsWith(".png") || base.endsWith(".xml")) base else null
}

internal fun petToJson(pack: PetPack): String {
    val poses = JSONObject()
    pack.poses.forEach { (pose, frames) ->
        poses.put(
            pose.name,
            JSONArray().apply { frames.forEach { put(JSONObject().put("f", it.file).put("d", it.durationMs)) } },
        )
    }
    return JSONObject().put("name", pack.name).put("poses", poses).toString()
}

internal fun petFromJson(text: String): PetPack? = runCatching {
    val json = JSONObject(text)
    val poses = LinkedHashMap<PetPose, List<PetFrame>>()
    val node = json.getJSONObject("poses")
    PetPose.entries.forEach { pose ->
        val array = node.optJSONArray(pose.name) ?: return@forEach
        val frames = (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val file = item.optString("f").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            PetFrame(file, item.optInt("d", DEFAULT_FRAME_MS).coerceIn(MIN_FRAME_MS, MAX_FRAME_MS))
        }
        if (frames.isNotEmpty()) poses[pose] = frames
    }
    if (poses[PetPose.Stand] == null) null else PetPack(json.optString("name"), poses)
}.getOrNull()
