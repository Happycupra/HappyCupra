package com.carlauncherc.launcher

import com.carlauncherc.launcher.location.GpsDiagnostics
import com.carlauncherc.launcher.location.GpsQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class GpsQualityTest {
    private fun reading(
        enabled: Boolean = true,
        permission: Boolean = true,
        age: Long? = 500,
        accuracy: Float? = 5f,
        speedAccuracy: Float? = 0.5f
    ) = GpsDiagnostics(enabled, permission, age, accuracy, speedAccuracy, 10f, 10f, 90f)

    @Test fun permissionsAndGpsStatusAreNotMistakenForAValidFix() {
        assertEquals(GpsQuality.NO_PERMISSION, reading(permission = false).quality)
        assertEquals(GpsQuality.DISABLED, reading(enabled = false).quality)
        assertEquals(GpsQuality.NO_FIX, reading(age = null).quality)
        assertEquals(GpsQuality.STALE, reading(age = 8000).quality)
    }

    @Test fun accuracyBandsReflectActualSensorQuality() {
        assertEquals(GpsQuality.GOOD, reading().quality)
        assertEquals(GpsQuality.FAIR, reading(accuracy = 18f).quality)
        assertEquals(GpsQuality.POOR, reading(accuracy = 60f).quality)
        assertEquals(GpsQuality.POOR, reading(speedAccuracy = 9f).quality)
        assertEquals(GpsQuality.FAIR, reading(speedAccuracy = 3f).quality)
        assertEquals(GpsQuality.POOR, reading(accuracy = Float.NaN).quality)
    }
}
