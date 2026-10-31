package com.carlauncherc.launcher.ui

internal enum class StickFigurePose { DANCE, STAND, SIT, LIE }

/** Deterministic state machine for playback vs idle poses; no Android dependencies. */
internal object VisualizerMotion {
    const val EQUALIZER = "equalizer"
    const val DANCER = "dancer"

    fun normalizeMode(raw: String?): String = if (raw == DANCER) DANCER else EQUALIZER

    fun pose(isPlaying: Boolean, idleElapsedMs: Long): StickFigurePose {
        if (isPlaying) return StickFigurePose.DANCE
        val t = idleElapsedMs.coerceAtLeast(0L) % 30_000L
        return when {
            t < 12_000L -> StickFigurePose.STAND
            t < 18_000L -> StickFigurePose.SIT
            t < 24_000L -> StickFigurePose.LIE
            else -> StickFigurePose.STAND
        }
    }
}
