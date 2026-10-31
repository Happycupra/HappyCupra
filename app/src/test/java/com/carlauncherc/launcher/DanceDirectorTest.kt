package com.carlauncherc.launcher

import com.carlauncherc.launcher.ui.DanceDirector
import com.carlauncherc.launcher.ui.DanceMove
import com.carlauncherc.launcher.ui.StickFigurePose
import com.carlauncherc.launcher.ui.VisualizerMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DanceDirectorTest {
    @Test fun allFourModesAreAcceptedAndLegacyDancerRemainsSolo() {
        assertEquals(VisualizerMotion.EQUALIZER, VisualizerMotion.normalizeMode(null))
        assertEquals(VisualizerMotion.DANCER, VisualizerMotion.normalizeMode("dancer"))
        assertEquals(VisualizerMotion.DUO_SYNC, VisualizerMotion.normalizeMode("duo_sync"))
        assertEquals(VisualizerMotion.DUO_RANDOM, VisualizerMotion.normalizeMode("duo_random"))
        assertEquals(VisualizerMotion.EQUALIZER, VisualizerMotion.normalizeMode("broken"))
        assertEquals(1, VisualizerMotion.numberOfDancers(VisualizerMotion.DANCER))
        assertEquals(2, VisualizerMotion.numberOfDancers(VisualizerMotion.DUO_SYNC))
        assertEquals(2, VisualizerMotion.numberOfDancers(VisualizerMotion.DUO_RANDOM))
    }

    @Test fun movementChangesRandomlyWithoutImmediateRepeats() {
        val director = DanceDirector(1234)
        val moves = mutableSetOf<DanceMove>()
        var previous = director.dance(100_000L).move
        moves.add(previous)
        for (t in 104_000L..184_000L step 4_000L) {
            val current = director.dance(t).move
            assertFalse("Repeated consecutive move: $current", current == previous)
            moves.add(current)
            previous = current
        }
        assertTrue("Too little choreography variety", moves.size >= 6)
    }

    @Test fun sameDirectorProducesIdenticalSyncFramesAtTheSameTime() {
        val director = DanceDirector(55)
        val a = director.dance(300_000L, true)
        val b = director.dance(300_000L, true)
        assertEquals(a, b)
        assertEquals(0f, a.blend, 0.001f)
        assertEquals(1f, director.dance(300_430L, true).blend, 0.001f)
    }

    @Test fun freestyleUsesIndependentMoveSchedules() {
        val a = DanceDirector(33)
        val b = DanceDirector(18882)
        var different = false
        for (t in 0L..45_000L step 3_500L) {
            if (a.dance(t).move != b.dance(t).move) different = true
        }
        assertTrue(different)
    }

    @Test fun idleIncludesSittingLyingAndRestMotion() {
        val d = DanceDirector(7)
        val seen = mutableSetOf<StickFigurePose>()
        for (t in 0L..180_000L step 1_000L) seen.add(d.rest(t).pose)
        assertTrue(StickFigurePose.STAND in seen)
        assertTrue(StickFigurePose.SIT in seen)
        assertTrue(StickFigurePose.LIE in seen)
        assertTrue(seen.size >= 4)
    }

    @Test fun easedBlendingHasSmoothEndpoints() {
        assertEquals(0f, DanceDirector.ease(-1f), 0.001f)
        assertEquals(0.5f, DanceDirector.ease(0.5f), 0.001f)
        assertEquals(1f, DanceDirector.ease(2f), 0.001f)
    }
}
