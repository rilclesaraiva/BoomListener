// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.profile.PetFrame
import com.boomlistener.data.profile.PetPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PetEngineTest {
    private fun engine(seed: Int = 1) = PetEngine(Random(seed)).apply { layout(300f, 160f, 80f, 90f) }

    private fun PetEngine.run(totalMs: Long, playing: Boolean = false, stepMs: Long = 16) {
        var left = totalMs
        while (left > 0) {
            update(minOf(stepMs, left), playing)
            left -= stepMs
        }
    }

    @Test
    fun `starts on the ground in the middle of the box`() {
        val pet = engine()
        assertEquals(110f, pet.x, 0.01f)
        assertEquals(70f, pet.y, 0.01f)
        assertEquals(PetMode.Idle, pet.mode)
    }

    @Test
    fun `never leaves the box while wandering`() {
        for (seed in 1..20) {
            val pet = engine(seed)
            repeat(600) {
                pet.run(100)
                assertTrue("x=${pet.x}", pet.x in 0f..220f)
                assertEquals(70f, pet.y, 0.01f)
            }
        }
    }

    @Test
    fun `eventually walks and eventually sits`() {
        val seen = mutableSetOf<PetMode>()
        val pet = engine(3)
        repeat(3_000) {
            pet.update(100, false)
            seen += pet.mode
        }
        assertTrue(PetMode.Walk in seen)
        assertTrue(PetMode.Sit in seen)
    }

    @Test
    fun `dances while music plays and goes back to normal when it stops`() {
        val pet = engine()
        pet.update(16, true)
        assertEquals(PetMode.Dance, pet.mode)
        val xWhileDancing = pet.x
        pet.run(30_000, playing = true)
        assertEquals(PetMode.Dance, pet.mode)
        assertEquals(xWhileDancing, pet.x, 0.01f)
        pet.update(16, false)
        assertEquals(PetMode.Idle, pet.mode)
    }

    @Test
    fun `being held follows the finger and does not move on its own`() {
        val pet = engine()
        pet.grab()
        pet.drag(40f, 30f)
        assertEquals(PetMode.Held, pet.mode)
        assertEquals(0f, pet.x, 0.01f)
        assertEquals(0f, pet.y, 0.01f)
        pet.run(10_000)
        assertEquals(PetMode.Held, pet.mode)
        assertEquals(0f, pet.x, 0.01f)
        pet.drag(150f, 20f)
        assertEquals(110f, pet.x, 0.01f)
        assertEquals(0f, pet.y, 0.01f)
    }

    @Test
    fun `dragging is limited to the box`() {
        val pet = engine()
        pet.grab()
        pet.drag(9_999f, 9_999f)
        assertEquals(220f, pet.x, 0.01f)
        assertEquals(70f, pet.y, 0.01f)
    }

    @Test
    fun `after being let go it falls and lands on the ground`() {
        val pet = engine()
        pet.grab()
        pet.drag(150f, 10f)
        pet.release()
        assertEquals(PetMode.Fall, pet.mode)
        pet.run(200)
        assertTrue(pet.y > 0f)
        pet.run(5_000)
        assertEquals(70f, pet.y, 0.01f)
        assertTrue(pet.mode != PetMode.Fall && pet.mode != PetMode.Held)
    }

    @Test
    fun `a poke makes it jump and land again`() {
        val pet = engine()
        pet.poke()
        pet.run(150)
        assertTrue(pet.y < 70f)
        pet.run(5_000)
        assertEquals(70f, pet.y, 0.01f)
        assertTrue(pet.mode != PetMode.Fall)
    }

    @Test
    fun `a poke wakes a sleeping pet and is ignored while it is held`() {
        val pet = engine()
        pet.grab()
        pet.poke()
        assertEquals(PetMode.Held, pet.mode)
        pet.release()
        pet.run(5_000)
        pet.poke()
        assertEquals(PetMode.Fall, pet.mode)
    }

    @Test
    fun `a long pause between frames does not teleport it`() {
        val pet = engine()
        pet.poke()
        pet.update(60_000, false)
        assertTrue(pet.y in 0f..70f)
    }

    @Test
    fun `resizing the box keeps the pet inside`() {
        val pet = engine()
        pet.grab()
        pet.drag(290f, 30f)
        pet.release()
        pet.layout(150f, 120f, 80f, 90f)
        assertTrue(pet.x in 0f..70f)
        assertTrue(pet.y in 0f..30f)
    }

    @Test
    fun `modes pick the matching pose`() {
        assertEquals(PetPose.Stand, PetMode.Idle.pose())
        assertEquals(PetPose.Drag, PetMode.Held.pose())
        assertEquals(PetPose.Dance, PetMode.Dance.pose())
    }

    @Test
    fun `animation frames play in order for their time and loop`() {
        val frames = listOf(PetFrame("a.png", 100), PetFrame("b.png", 200))
        assertEquals("a.png", frameAt(frames, 0)?.file)
        assertEquals("a.png", frameAt(frames, 99)?.file)
        assertEquals("b.png", frameAt(frames, 100)?.file)
        assertEquals("b.png", frameAt(frames, 299)?.file)
        assertEquals("a.png", frameAt(frames, 300)?.file)
        assertEquals("a.png", frameAt(frames, -5)?.file)
        assertNull(frameAt(emptyList(), 10))
    }
}
