package com.carlauncherc.launcher.ui

internal enum class StickFigurePose { DANCE, STAND, SIT, LIE, STRETCH, WAVE }

/** Legacy preference values remain supported, existing "dancer" becomes Solo. */
internal object VisualizerMotion {
    const val EQUALIZER = "equalizer"
    const val DANCER = "dancer"
    const val DUO_SYNC = "duo_sync"
    const val DUO_RANDOM = "duo_random"

    fun normalizeMode(raw: String?): String = when (raw) {
        DANCER, DUO_SYNC, DUO_RANDOM -> raw
        else -> EQUALIZER
    }

    fun isDancer(mode: String): Boolean = normalizeMode(mode) != EQUALIZER

    fun numberOfDancers(mode: String): Int =
        if (mode == DUO_SYNC || mode == DUO_RANDOM) 2 else 1

    /** Legacy helper retained for existing tests. New animation uses DanceDirector.rest. */
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
