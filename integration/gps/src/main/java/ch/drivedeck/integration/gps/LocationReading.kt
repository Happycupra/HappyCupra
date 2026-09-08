package ch.drivedeck.integration.gps

enum class GpsStatus { PERMISSION_REQUIRED, DISABLED, SEARCHING, FIXED, UNAVAILABLE }

data class LocationReading(
    val status: GpsStatus = GpsStatus.PERMISSION_REQUIRED,
    val speedKmh: Int? = null,
    val bearingDegrees: Float? = null,
    val accuracyMeters: Float? = null,
    val timestampMillis: Long? = null,
) {
    val hasFix: Boolean get() = status == GpsStatus.FIXED
}

internal fun metersPerSecondToKmh(value: Float): Int = (value.coerceAtLeast(0f) * 3.6f).toInt()
