// BoomListener — Copyright (C) 2026 rilclesaraiva — licenciado sob GNU GPLv3.
// Texto completo: https://github.com/rilclesaraiva/BoomListener/blob/main/LICENSE

package com.boomlistener.feature.profile

import com.boomlistener.data.profile.PetFrame
import com.boomlistener.data.profile.PetPose
import kotlin.random.Random

/** O que o bichinho está fazendo. */
internal enum class PetMode { Idle, Walk, Sit, Sleep, Dance, Held, Fall, Run, Crawl, WallHold, WallClimb, CeilingClimb }

/** A pose do pacote que desenha cada modo. */
internal fun PetMode.pose(): PetPose = when (this) {
    PetMode.Idle -> PetPose.Stand
    PetMode.Walk -> PetPose.Walk
    PetMode.Sit -> PetPose.Sit
    PetMode.Sleep -> PetPose.Sleep
    PetMode.Dance -> PetPose.Dance
    PetMode.Held -> PetPose.Drag
    PetMode.Fall -> PetPose.Fall
    PetMode.Run -> PetPose.Run
    PetMode.Crawl -> PetPose.Crawl
    PetMode.WallHold -> PetPose.GrabWall
    PetMode.WallClimb -> PetPose.ClimbWall
    PetMode.CeilingClimb -> PetPose.ClimbCeiling
}

/**
 * O quadro que está na tela [elapsedMs] depois de a animação começar: os quadros passam na ordem,
 * cada um pelo tempo dele, e a animação recomeça no fim. Lista vazia: null.
 */
internal fun frameAt(frames: List<PetFrame>, elapsedMs: Long): PetFrame? {
    if (frames.isEmpty()) return null
    val total = frames.sumOf { it.durationMs.toLong() }.coerceAtLeast(1)
    var rest = elapsedMs.coerceAtLeast(0) % total
    for (frame in frames) {
        if (rest < frame.durationMs) return frame
        rest -= frame.durationMs
    }
    return frames.last()
}

/**
 * O "cérebro" do bichinho: onde ele está dentro da caixa e o que faz. Tudo em dp, sem tela nem
 * relógio de verdade (quem usa passa o tempo que se passou), por isso dá para testar.
 * O desenho dos pacotes de shimeji olha para a esquerda; [facingRight] diz quando é preciso espelhar.
 */
internal class PetEngine(
    private val random: Random = Random.Default,
    /** As poses que o pacote tem: ele só corre, rasteja ou sobe na parede e no teto se houver a pose. */
    private val abilities: Set<PetPose> = emptySet(),
) {
    private var boxWidth = 0f
    private var boxHeight = 0f
    private var spriteWidth = 0f
    private var spriteHeight = 0f

    /** Canto de cima à esquerda do bichinho, dentro da caixa. */
    var x = 0f
        private set
    var y = 0f
        private set
    var facingRight = false
        private set
    var mode = PetMode.Idle
        private set

    /** Há quanto tempo ele está neste modo (para escolher o quadro da animação). */
    var modeTimeMs = 0L
        private set

    private var timeLeftMs = 1_500L
    private var velocityY = 0f
    private var walkDirection = -1
    private var seekWall = false
    private var climbTargetY = 0f

    private val maxX get() = (boxWidth - spriteWidth).coerceAtLeast(0f)
    private val groundY get() = (boxHeight - spriteHeight).coerceAtLeast(0f)

    /** Tamanhos novos (a caixa ou o bichinho mudaram): ele continua dentro da caixa. */
    fun layout(boxWidth: Float, boxHeight: Float, spriteWidth: Float, spriteHeight: Float) {
        val first = this.boxWidth == 0f
        this.boxWidth = boxWidth
        this.boxHeight = boxHeight
        this.spriteWidth = spriteWidth
        this.spriteHeight = spriteHeight
        x = if (first) maxX / 2 else x.coerceIn(0f, maxX)
        y = if (mode in GROUND_MODES) groundY else y.coerceIn(0f, groundY)
        if (mode == PetMode.WallHold || mode == PetMode.WallClimb) x = if (facingRight) maxX else 0f
    }

    /** Anda [elapsedMs] no tempo. [playing]: há música tocando (ele dança; sem música, volta ao normal). */
    fun update(elapsedMs: Long, playing: Boolean) {
        if (boxWidth == 0f) return
        val step = elapsedMs.coerceIn(0, MAX_STEP_MS)
        val seconds = step / 1000f
        modeTimeMs += step
        when (mode) {
            PetMode.Held -> Unit
            PetMode.Fall -> fall(seconds)
            PetMode.WallClimb -> {
                y -= CLIMB_SPEED * seconds
                if (y <= climbTargetY) {
                    y = climbTargetY
                    if (climbTargetY <= 0f && PetPose.ClimbCeiling in abilities) {
                        val fromLeft = !facingRight
                        enter(PetMode.CeilingClimb)
                        walkDirection = if (fromLeft) 1 else -1
                        facingRight = walkDirection > 0
                    } else {
                        dropFromWall()
                    }
                }
            }
            PetMode.CeilingClimb -> {
                x += walkDirection * CLIMB_SPEED * seconds
                facingRight = walkDirection > 0
                timeLeftMs -= step
                if (x <= 0f || x >= maxX || timeLeftMs <= 0) {
                    x = x.coerceIn(0f, maxX)
                    dropFromWall()
                }
            }
            else -> {
                val ground = mode in GROUND_MODES
                if (playing && ground && mode != PetMode.Dance) enter(PetMode.Dance)
                if (!playing && mode == PetMode.Dance) enter(PetMode.Idle)
                if (mode == PetMode.Walk || mode == PetMode.Run || mode == PetMode.Crawl) walk(seconds)
                timeLeftMs -= step
                if (timeLeftMs <= 0 && mode != PetMode.Dance) chooseNext()
            }
        }
    }

    private fun fall(seconds: Float) {
        velocityY += GRAVITY * seconds
        y += velocityY * seconds
        if (y >= groundY) {
            y = groundY
            if (velocityY > BOUNCE_MIN_SPEED) {
                velocityY = -velocityY * BOUNCE_KEEP
            } else {
                velocityY = 0f
                enter(PetMode.Idle)
            }
        }
    }

    /** Solta-se da parede ou do teto: cai até o chão, como o FallFromWall do shimeji. */
    private fun dropFromWall() {
        mode = PetMode.Fall
        modeTimeMs = 0
        velocityY = 0f
        seekWall = false
    }

    private fun walk(seconds: Float) {
        val speed = when (mode) {
            PetMode.Run -> RUN_SPEED
            PetMode.Crawl -> CRAWL_SPEED
            else -> WALK_SPEED
        }
        x += walkDirection * speed * seconds
        if (x <= 0f) {
            x = 0f
            if (seekWall) return attachToWall(right = false)
            walkDirection = 1
        } else if (x >= maxX) {
            x = maxX
            if (seekWall) return attachToWall(right = true)
            walkDirection = -1
        }
        facingRight = walkDirection > 0
    }

    /** Chegou à beira da caixa: agarra na parede (o desenho olha para a esquerda: espelhado, é a parede da direita). */
    private fun attachToWall(right: Boolean) {
        enter(PetMode.WallHold)
        facingRight = right
        x = if (right) maxX else 0f
    }

    private fun enter(next: PetMode) {
        mode = next
        modeTimeMs = 0
        timeLeftMs = when (next) {
            PetMode.Idle -> random.nextLong(1_500, 3_500)
            PetMode.Walk -> random.nextLong(2_500, 6_000)
            PetMode.Sit -> random.nextLong(3_000, 6_000)
            PetMode.Sleep -> random.nextLong(6_000, 12_000)
            PetMode.Run -> random.nextLong(1_500, 3_500)
            PetMode.Crawl -> random.nextLong(2_500, 5_000)
            PetMode.WallHold -> random.nextLong(800, 2_000)
            PetMode.CeilingClimb -> random.nextLong(3_000, 6_000)
            else -> Long.MAX_VALUE / 2
        }
        seekWall = false
        if (next == PetMode.Walk || next == PetMode.Run || next == PetMode.Crawl) {
            walkDirection = if (random.nextBoolean()) 1 else -1
            facingRight = walkDirection > 0
        }
    }

    private fun chooseNext() {
        if (mode == PetMode.WallHold) return startClimb()
        val roll = random.nextFloat()
        // Caminhar vira, às vezes, correr, rastejar ou ir até a parede (se o pacote tiver essas poses).
        val next = when (mode) {
            PetMode.Idle -> when {
                roll < 0.40f -> PetMode.Walk
                roll < 0.55f -> PetMode.Sit
                roll < 0.63f -> PetMode.Sleep
                roll < 0.73f && PetPose.Run in abilities -> PetMode.Run
                roll < 0.81f && PetPose.Crawl in abilities -> PetMode.Crawl
                roll < 0.93f && PetPose.ClimbWall in abilities -> PetMode.Walk.also { wallNext = true }
                else -> PetMode.Idle
            }
            PetMode.Sit -> if (roll < 0.3f) PetMode.Sleep else PetMode.Idle
            else -> PetMode.Idle
        }
        enter(next)
        if (wallNext) {
            wallNext = false
            seekWall = true
            timeLeftMs = Long.MAX_VALUE / 2
        }
    }

    private var wallNext = false

    /** Agarrado na parede: sobe até uma altura (ou até o teto, se o pacote tiver o teto). */
    private fun startClimb() {
        val toCeiling = PetPose.ClimbCeiling in abilities && random.nextFloat() < 0.5f
        climbTargetY = if (toCeiling) 0f else groundY * random.nextFloat() * 0.6f
        mode = PetMode.WallClimb
        modeTimeMs = 0
        timeLeftMs = Long.MAX_VALUE / 2
    }

    /** O dedo pegou o bichinho: ele para de fazer o que fazia e vai junto. */
    fun grab() {
        mode = PetMode.Held
        modeTimeMs = 0
        velocityY = 0f
    }

    /** O dedo está em ([pointerX], [pointerY]) dentro da caixa: o bichinho fica com o centro ali. */
    fun drag(pointerX: Float, pointerY: Float) {
        if (mode != PetMode.Held) return
        val newX = (pointerX - spriteWidth / 2).coerceIn(0f, maxX)
        facingRight = newX > x
        x = newX
        y = (pointerY - spriteHeight / 2).coerceIn(0f, groundY)
    }

    /** Soltou: ele cai (e quica um pouco) até o chão. */
    fun release() {
        if (mode != PetMode.Held) return
        mode = PetMode.Fall
        modeTimeMs = 0
        velocityY = 0f
    }

    /** Um toque: ele dá um pulinho (e acorda, se dormia). */
    fun poke() {
        if (mode == PetMode.Held) return
        mode = PetMode.Fall
        modeTimeMs = 0
        velocityY = -POKE_SPEED
    }

    private companion object {
        val GROUND_MODES = setOf(PetMode.Idle, PetMode.Walk, PetMode.Sit, PetMode.Sleep, PetMode.Dance, PetMode.Run, PetMode.Crawl)
        const val MAX_STEP_MS = 64L
        const val RUN_SPEED = 90f
        const val CRAWL_SPEED = 20f
        const val CLIMB_SPEED = 30f
        const val GRAVITY = 900f
        const val WALK_SPEED = 36f
        const val POKE_SPEED = 260f
        const val BOUNCE_MIN_SPEED = 120f
        const val BOUNCE_KEEP = 0.35f
    }
}
