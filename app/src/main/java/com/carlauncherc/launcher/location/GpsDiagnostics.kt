package com.carlauncherc.launcher.location

/**
 * Pure assessment of fix freshness and accuracy. The diagnostic panel deliberately does not
 * infer speed from positional changes: velocity is always direct GNSS getSpeed().
 */
enum class GpsQuality { NO_PERMISSION, DISABLED, NO_FIX, STALE, POOR, FAIR, GOOD }

data class GpsDiagnostics(
    val providerEnabled: Boolean,
    val permissionGranted: Boolean,
    val fixAgeMs: Long?,
    val horizontalAccuracyMeters: Float?,
    val speedAccuracyMps: Float?,
    val rawSpeedMps: Float?,
    val filteredSpeedMps: Float?,
    val bearingDegrees: Float?
) {
    val quality: GpsQuality
        get() = GpsQualityAssessment.classify(this)
}

object GpsQualityAssessment {
    const val STALE_MS = 5_000L

    fun classify(d: GpsDiagnostics): GpsQuality = when {
        !d.permissionGranted -> GpsQuality.NO_PERMISSION
        !d.providerEnabled -> GpsQuality.DISABLED
        d.fixAgeMs == null -> GpsQuality.NO_FIX
        d.fixAgeMs !in 0L..STALE_MS -> GpsQuality.STALE
        d.horizontalAccuracyMeters == null ||
            !d.horizontalAccuracyMeters.isFinite() ||
            d.horizontalAccuracyMeters > 30f ||
            (d.speedAccuracyMps != null &&
                (!d.speedAccuracyMps.isFinite() || d.speedAccuracyMps > 6f)) ->
            GpsQuality.POOR
        d.horizontalAccuracyMeters > 10f ||
            (d.speedAccuracyMps != null && d.speedAccuracyMps > 2f) ->
            GpsQuality.FAIR
        else -> GpsQuality.GOOD
    }
}
