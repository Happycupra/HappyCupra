package ch.drivedeck.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DemoDriveDataTest {
    @Test fun previewDataMatchesDocumentedScenario() {
        assertEquals("Midnight Drive", DemoDriveData.Preview.song)
        assertEquals("Demo Artist", DemoDriveData.Preview.artist)
        assertEquals(83, DemoDriveData.Preview.speedKmh)
        assertEquals("Bern", DemoDriveData.Preview.destination)
        assertEquals("18°C", DemoDriveData.Preview.weather)
    }
}
