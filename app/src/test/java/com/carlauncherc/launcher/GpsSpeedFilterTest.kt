package com.carlauncherc.launcher

import com.carlauncherc.launcher.location.GpsSpeedFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GpsSpeedFilterTest {
    @Test fun directGpsSpeedIsAlreadyMetresPerSecond() {
        val filter = GpsSpeedFilter()
        assertEquals(20f, filter.accept(20f, 1_000_000_000L, 0.3f)!!, 0.001f)
    }

    @Test fun absenceOfSpeedDoesNotBecomeZero() {
        val filter = GpsSpeedFilter()
        assertNull(filter.accept(null, 1_000_000_000L, null))
        assertEquals(0f, filter.accept(0.2f, 2_000_000_000L, 0.2f)!!, 0.001f)
    }

    @Test fun rejectsUnreliableAccuracyAndOutOfOrderFixes() {
        val filter = GpsSpeedFilter()
        assertNull(filter.accept(20f, 1_000_000_000L, 12f))
        assertEquals(14f, filter.accept(14f, 2_000_000_000L, 0.2f)!!, 0.001f)
        assertNull(filter.accept(100f, 1_000_000_000L, null))
        assertNull(filter.accept(150f, 3_000_000_000L, null))
    }

    @Test fun ignoresSuddenImpossibleSpeedSpikes() {
        val filter = GpsSpeedFilter()
        assertEquals(15f, filter.accept(15f, 1_000_000_000L, null)!!, 0.001f)
        val held = filter.accept(70f, 2_000_000_000L, null)
        assertEquals(15f, held!!, 0.001f)
        val next = filter.accept(16f, 3_000_000_000L, null)
        assertTrue(next!! in 15f..16f)
    }

    @Test fun startsFreshAfterAResetAndNeverHoldsAfterLongGap() {
        val filter = GpsSpeedFilter()
        filter.accept(20f, 1_000_000_000L, null)
        assertEquals(0f, filter.accept(0f, 10_000_000_000L, null)!!, 0.001f)
        filter.reset()
        assertEquals(30f, filter.accept(30f, 1_000_000_000L, null)!!, 0.001f)
    }
}
