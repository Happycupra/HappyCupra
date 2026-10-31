package com.carlauncherc.launcher

import com.carlauncherc.launcher.core.DashboardLayouts
import com.carlauncherc.launcher.core.DashboardPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardLayoutsTest {
    @Test fun unknownModeFallsBackToClassic() {
        assertEquals(DashboardPreset.CLASSIC, DashboardPreset.parse(null))
        assertEquals(DashboardPreset.CLASSIC, DashboardPreset.parse("some-invalid-mode"))
        assertEquals(DashboardPreset.MUSIC, DashboardPreset.parse("music"))
    }

    @Test fun allPresetsContainTheSameThreeGaugesOnce() {
        for (preset in DashboardPreset.entries) {
            val arrangement = DashboardLayouts.arrange(preset, true, true)
            assertEquals(setOf("speed", "clock", "compass"), arrangement.gaugeOrder.toSet())
            assertEquals(3, arrangement.gaugeOrder.size)
            assertEquals(3, arrangement.gaugeWeights.size)
            assertTrue(arrangement.gaugeWeights.all { it > 0f })
        }
    }

    @Test fun navAndMusicHaveDifferentPriorities() {
        val navigation = DashboardLayouts.arrange(DashboardPreset.NAVIGATION, true, true)
        val music = DashboardLayouts.arrange(DashboardPreset.MUSIC, true, true)
        assertTrue(navigation.navWeight > navigation.musicWeight)
        assertTrue(music.musicWeight > music.navWeight)
        assertEquals("clock", navigation.gaugeOrder.first())
    }

    @Test fun emptyOrMinimalCardsNeverReserveScreenSpace() {
        assertFalse(DashboardLayouts.arrange(DashboardPreset.MINIMAL, true, true).quickCardsVisible)
        assertFalse(DashboardLayouts.arrange(DashboardPreset.CLASSIC, false, false).quickCardsVisible)
        assertTrue(DashboardLayouts.arrange(DashboardPreset.CLASSIC, true, false).quickCardsVisible)
    }
}
