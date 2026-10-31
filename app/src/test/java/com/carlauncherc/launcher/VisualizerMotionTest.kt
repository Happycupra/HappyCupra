package com.carlauncherc.launcher

import com.carlauncherc.launcher.ui.StickFigurePose
import com.carlauncherc.launcher.ui.VisualizerMotion
import org.junit.Assert.assertEquals
import org.junit.Test

class VisualizerMotionTest {
    @Test fun defaultsToEqualizerForUnexpectedSetting() {
        assertEquals(VisualizerMotion.EQUALIZER, VisualizerMotion.normalizeMode(null))
        assertEquals(VisualizerMotion.EQUALIZER, VisualizerMotion.normalizeMode("other"))
        assertEquals(VisualizerMotion.DANCER, VisualizerMotion.normalizeMode("dancer"))
    }

    @Test fun dancesWheneverPlaying() {
        assertEquals(StickFigurePose.DANCE, VisualizerMotion.pose(true, 0L))
        assertEquals(StickFigurePose.DANCE, VisualizerMotion.pose(true, 20_000L))
    }

    @Test fun alternatesIdlePoses() {
        assertEquals(StickFigurePose.STAND, VisualizerMotion.pose(false, 0L))
        assertEquals(StickFigurePose.SIT, VisualizerMotion.pose(false, 12_000L))
        assertEquals(StickFigurePose.LIE, VisualizerMotion.pose(false, 20_000L))
        assertEquals(StickFigurePose.STAND, VisualizerMotion.pose(false, 26_000L))
        assertEquals(StickFigurePose.STAND, VisualizerMotion.pose(false, 31_000L))
    }
}
