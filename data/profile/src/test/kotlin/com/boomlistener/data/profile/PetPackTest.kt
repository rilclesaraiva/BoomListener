// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.data.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PetPackTest {
    private val files = setOf("shime1.png", "shime2.png", "shime3.png", "shime4.png", "shime5.png", "shime11.png")

    private val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <Mascot xmlns="http://www.group-finity.com/Mascot">
          <ActionList>
            <Action Name="Stand" Type="Embedded" Class="com.group_finity.mascot.action.Stay">
              <Animation><Pose Image="/shime1.png" ImageAnchor="64,128" Velocity="0,0" Duration="250"/></Animation>
            </Action>
            <Action Name="Walk" Type="Embedded" Class="com.group_finity.mascot.action.Move">
              <Animation>
                <Pose Image="/shime1.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6"/>
                <Pose Image="/shime2.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6"/>
                <Pose Image="/shime1.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6"/>
                <Pose Image="/shime3.png" ImageAnchor="64,128" Velocity="-2,0" Duration="6"/>
              </Animation>
            </Action>
            <Action Name="Sit" Type="Embedded" Class="com.group_finity.mascot.action.Stay">
              <Animation><Pose Image="/shime11.png" ImageAnchor="64,128" Velocity="0,0" Duration="250"/></Animation>
            </Action>
            <Action Name="Fall" Type="Embedded" Class="com.group_finity.mascot.action.Move">
              <Animation><Pose Image="/shime4.png" ImageAnchor="64,128" Velocity="0,0" Duration="1"/></Animation>
            </Action>
            <Action Name="Pinched" Type="Embedded" Class="com.group_finity.mascot.action.Move">
              <Animation><Pose Image="/shime5.png" ImageAnchor="64,128" Velocity="0,0" Duration="1"/></Animation>
            </Action>
            <Action Name="Falling" Type="Sequence"><ActionReference Name="Fall"/></Action>
          </ActionList>
        </Mascot>
    """.trimIndent()

    @Test
    fun `reads poses from the pack actions file`() {
        val poses = parsePetActions(xml, files)
        assertEquals(listOf("shime1.png"), poses.getValue(PetPose.Stand).map { it.file })
        assertEquals(listOf("shime1.png", "shime2.png", "shime1.png", "shime3.png"), poses.getValue(PetPose.Walk).map { it.file })
        assertEquals(240, poses.getValue(PetPose.Walk).first().durationMs)
        assertEquals(listOf("shime11.png"), poses.getValue(PetPose.Sit).map { it.file })
        assertEquals(listOf("shime4.png"), poses.getValue(PetPose.Fall).map { it.file })
        assertEquals(listOf("shime5.png"), poses.getValue(PetPose.Drag).map { it.file })
    }

    @Test
    fun `frames pointing to images that are not in the pack are skipped`() {
        val poses = parsePetActions(xml, setOf("shime1.png"))
        assertEquals(listOf("shime1.png", "shime1.png"), poses.getValue(PetPose.Walk).map { it.file })
        assertNull(poses[PetPose.Sit])
    }

    @Test
    fun `xml with a doctype or entities is refused`() {
        val bad = "<?xml version=\"1.0\"?><!DOCTYPE a [<!ENTITY x \"y\">]><Action Name=\"Stand\"/>"
        assertTrue(parsePetActions(bad, files).isEmpty())
        assertTrue(parsePetActions("não é xml", files).isEmpty())
    }

    @Test
    fun `image names with capital letters and folders still match`() {
        val named = xml.replace("/shime1.png", "img/Shime1.PNG")
        val poses = parsePetActions(named, files)
        assertEquals("shime1.png", poses.getValue(PetPose.Stand).first().file)
    }

    @Test
    fun `a pack without an actions file falls back to the image numbers`() {
        val pack = buildPetPack("Teste", null, files)!!
        assertEquals(listOf("shime1.png"), pack.poses.getValue(PetPose.Stand).map { it.file })
        assertEquals(listOf("shime1.png", "shime2.png", "shime1.png", "shime3.png"), pack.poses.getValue(PetPose.Walk).map { it.file })
    }

    @Test
    fun `numbers are compared as numbers not as text`() {
        val poses = fallbackPetPoses(setOf("shime10.png", "shime2.png", "shime1.png"))
        assertEquals(listOf("shime1.png", "shime2.png", "shime1.png", "shime10.png"), poses.getValue(PetPose.Walk).map { it.file })
    }

    @Test
    fun `no images means no pet`() {
        assertNull(buildPetPack("Vazio", xml, emptySet()))
        assertNull(buildPetPack("Vazio", null, setOf("leiame.txt")))
    }

    @Test
    fun `missing poses fall back to a similar one and then to standing`() {
        val pack = buildPetPack("Só parado", null, setOf("a.png"))!!
        assertEquals(listOf("a.png"), pack.frames(PetPose.Walk).map { it.file })
        assertEquals(listOf("a.png"), pack.frames(PetPose.Dance).map { it.file })
        val full = buildPetPack("Completo", xml, files)!!
        assertEquals(full.frames(PetPose.Walk), full.frames(PetPose.Dance))
        assertEquals(listOf("shime11.png"), full.frames(PetPose.Sleep).map { it.file })
    }

    @Test
    fun `pack goes to json and back`() {
        val pack = buildPetPack("Teste", xml, files)!!
        assertEquals(pack, petFromJson(petToJson(pack)))
        assertNotNull(pack.files.firstOrNull { it == "shime11.png" })
        assertNull(petFromJson("não é json"))
        assertNull(petFromJson("""{"name":"x","poses":{"Walk":[{"f":"a.png","d":100}]}}"""))
    }

    @Test
    fun `only png and xml entries are kept and only by base name`() {
        assertEquals("shime1.png", petEntryName("img/Shime/shime1.PNG"))
        assertEquals("actions.xml", petEntryName("conf\\actions.xml"))
        assertEquals("shime2.png", petEntryName("../../shime2.png"))
        assertNull(petEntryName("leia-me.txt"))
        assertNull(petEntryName("pasta/"))
        assertNull(petEntryName(".hidden.png"))
        assertNull(petEntryName("a..b.png"))
    }

    @Test
    fun `pet settings round trip and old or broken values fall back to defaults`() {
        val settings = PetSettings(enabled = false, packVersion = 7, backgroundKind = PetBackgroundKind.Color, backgroundColor = "#112233", backgroundVersion = 3)
        assertEquals(settings, petSettingsFromJson(petSettingsToJson(settings)))
        val odd = petSettingsFromJson("""{"bg":"Inexistente","bgColor":"vermelho"}""")
        assertEquals(PetBackgroundKind.Profile, odd.backgroundKind)
        assertEquals("", odd.backgroundColor)
        assertEquals(true, odd.enabled)
    }

    @Test
    fun `reads run crawl wall and ceiling poses, first animation only, exact names only`() {
        val extra = """
            <Mascot><ActionList>
              <Action Name="Stand"><Animation><Pose Image="/a.png" Duration="10"/></Animation></Action>
              <Action Name="Run"><Animation><Pose Image="/r1.png" Duration="2"/><Pose Image="/r2.png" Duration="2"/></Animation></Action>
              <Action Name="Creep"><Animation><Pose Image="/c1.png" Duration="4"/></Animation></Action>
              <Action Name="ClimbWall" BorderType="Wall">
                <Animation Condition="up"><Pose Image="/w1.png" Duration="4"/><Pose Image="/w2.png" Duration="4"/></Animation>
                <Animation Condition="down"><Pose Image="/w2.png" Duration="4"/><Pose Image="/w1.png" Duration="4"/><Pose Image="/w3.png" Duration="4"/></Animation>
              </Action>
              <Action Name="GrabWall"><Animation><Pose Image="/w1.png" Duration="250"/></Animation></Action>
              <Action Name="ClimbCeiling"><Animation><Pose Image="/t1.png" Duration="4"/></Animation></Action>
              <Action Name="RunWithIe"><Animation><Pose Image="/ie.png" Duration="2"/></Animation></Action>
            </ActionList></Mascot>
        """.trimIndent()
        val all = setOf("a.png", "r1.png", "r2.png", "c1.png", "w1.png", "w2.png", "w3.png", "t1.png", "ie.png")
        val poses = parsePetActions(extra, all)
        assertEquals(listOf("r1.png", "r2.png"), poses.getValue(PetPose.Run).map { it.file })
        assertEquals(listOf("c1.png"), poses.getValue(PetPose.Crawl).map { it.file })
        assertEquals(listOf("w1.png", "w2.png"), poses.getValue(PetPose.ClimbWall).map { it.file })
        assertEquals(listOf("w1.png"), poses.getValue(PetPose.GrabWall).map { it.file })
        assertEquals(listOf("t1.png"), poses.getValue(PetPose.ClimbCeiling).map { it.file })
        // Sem "Run" de verdade, "RunWithIe" (carrega uma janela) não serve.
        val noRun = parsePetActions(extra.replace("""<Action Name="Run">""", """<Action Name="Nothing">"""), all)
        assertNull(noRun[PetPose.Run])
    }

    @Test
    fun `pack json round trips the new poses`() {
        val pack = PetPack("x", mapOf(PetPose.Stand to listOf(PetFrame("a.png", 100)), PetPose.ClimbWall to listOf(PetFrame("w.png", 160))))
        assertEquals(pack, petFromJson(petToJson(pack)))
    }

    @Test
    fun `knows which way the drawings face from the walking speed`() {
        fun xml(speed: String) = """<Mascot><ActionList>
            <Action Name="Stand"><Animation><Pose Image="/a.png" Velocity="0,0" Duration="10"/></Animation></Action>
            <Action Name="Walk"><Animation><Pose Image="/a.png" Velocity="0,0" Duration="2"/><Pose Image="/b.png" Velocity="$speed,0" Duration="2"/></Animation></Action>
            </ActionList></Mascot>""".trimIndent()
        val available = setOf("a.png", "b.png")
        assertEquals(true, parsePetFacesRight(xml("2")))
        assertEquals(false, parsePetFacesRight(xml("-2")))
        assertEquals(false, parsePetFacesRight("<Mascot/>"))
        assertEquals(true, buildPetPack("Agnes", xml("2"), available)!!.facesRight)
        assertEquals(false, buildPetPack("Ankha", xml("-2"), available)!!.facesRight)
        val right = buildPetPack("Agnes", xml("2"), available)!!
        assertEquals(right, petFromJson(petToJson(right)))
    }
}
