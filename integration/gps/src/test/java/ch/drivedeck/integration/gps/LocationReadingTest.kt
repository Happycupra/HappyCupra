package ch.drivedeck.integration.gps

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationReadingTest {
    @Test fun convertsMetersPerSecondToKilometresPerHour() {
        assertEquals(83, metersPerSecondToKmh(23.1f))
        assertEquals(0, metersPerSecondToKmh(-2f))
    }
}
