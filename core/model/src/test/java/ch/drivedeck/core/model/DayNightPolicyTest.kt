package ch.drivedeck.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DayNightPolicyTest {
    @Test fun switchesAtDocumentedBoundaries() {
        assertTrue(DayNightPolicy.isNight(6))
        assertFalse(DayNightPolicy.isNight(7))
        assertFalse(DayNightPolicy.isNight(18))
        assertTrue(DayNightPolicy.isNight(19))
    }
}
