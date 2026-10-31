package com.carlauncherc.launcher.ui

import kotlin.random.Random

/** Small, deterministic choreography engine: no Android APIs or frame allocations from random decisions. */
internal enum class DanceMove {
    BOUNCE, SIDE_STEP, ARM_WAVE, KICK, CLAP, TWIST,
    DISCO, ROBOT, HOP, SLIDE, POINT, GROOVE
}

internal data class DanceFrame(
    val move: DanceMove,
    val from: DanceMove,
    val blend: Float,
    val phase: Double
)

internal data class RestFrame(
    val pose: StickFigurePose,
    val from: StickFigurePose,
    val blend: Float
)

/**
 * Each dancer gets an independent seed in freestyle mode; sync mode reuses ONE director.
 * A move lasts 1.4–3.4 seconds and never repeats immediately. Transitions ease over 420ms.
 * Beat phase remains continuous at synthetic 120 BPM when a new move is selected.
 *
 * This is intentionally not an audio/tempo detector: remote MediaSession has no PCM/FFT.
 */
internal class DanceDirector(seed: Int) {
    private val random = Random(seed)
    private var started = false
    private var move = DanceMove.BOUNCE
    private var from = move
    private var changedAt = 0L
    private var nextAt = 0L
    private var phaseOffset = random.nextDouble(0.0, 6.283185307179586)
    private var rest = StickFigurePose.STAND
    private var previousRest = rest
    private var restChangedAt = 0L
    private var nextRestAt = 0L

    fun reset(nowMs: Long) {
        started = true
        move = DanceMove.entries[random.nextInt(DanceMove.entries.size)]
        from = move
        changedAt = nowMs
        nextAt = nowMs + random.nextLong(1_500L, 2_700L)
        phaseOffset = random.nextDouble(0.0, 6.283185307179586)
        rest = StickFigurePose.STAND
        previousRest = rest
        restChangedAt = nowMs
        nextRestAt = nowMs + random.nextLong(5_000L, 9_000L)
    }

    fun dance(nowMs: Long, synchronizedPhase: Boolean = false): DanceFrame {
        if (!started || nowMs < changedAt) reset(nowMs)
        if (nowMs >= nextAt) {
            from = move
            val choices = DanceMove.entries
            var next = choices[random.nextInt(choices.size - 1)]
            if (next.ordinal >= move.ordinal) next = choices[next.ordinal + 1]
            move = next
            changedAt = nowMs
            nextAt = nowMs + random.nextLong(1_400L, 3_400L)
        }
        val blend = ease(((nowMs - changedAt) / 420f).coerceIn(0f, 1f))
        val phase = nowMs / 500.0 * (Math.PI * 2.0) +
            if (synchronizedPhase) 0.0 else phaseOffset
        return DanceFrame(move, from, blend, phase)
    }

    fun rest(nowMs: Long): RestFrame {
        if (!started || nowMs < restChangedAt) reset(nowMs)
        if (nowMs >= nextRestAt) {
            previousRest = rest
            val options = arrayOf(
                StickFigurePose.STAND, StickFigurePose.SIT, StickFigurePose.LIE,
                StickFigurePose.STRETCH, StickFigurePose.WAVE
            )
            var next = options[random.nextInt(options.size)]
            if (next == rest) next = options[(options.indexOf(next) + 1 + random.nextInt(4)) % options.size]
            rest = next
            restChangedAt = nowMs
            nextRestAt = nowMs + when (next) {
                StickFigurePose.STAND -> random.nextLong(5_000L, 10_000L)
                StickFigurePose.SIT, StickFigurePose.LIE -> random.nextLong(4_000L, 9_000L)
                else -> random.nextLong(2_500L, 5_000L)
            }
        }
        val blend = ease(((nowMs - restChangedAt) / 650f).coerceIn(0f, 1f))
        return RestFrame(rest, previousRest, blend)
    }

    companion object {
        fun ease(x: Float): Float {
            val t = x.coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }
    }
}
