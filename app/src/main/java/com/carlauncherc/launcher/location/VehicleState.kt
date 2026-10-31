package com.carlauncherc.launcher.location

enum class HeadingSource { NONE, GPS, SENSOR }

/** Speed hasFix only when a fresh GPS_PROVIDER fix has a trustworthy speed field. */
data class VehicleState(
    val hasFix: Boolean = false,
    val speedMps: Float = 0f,
    val headingDeg: Float? = null,
    val headingSource: HeadingSource = HeadingSource.NONE,
    val gpsEnabled: Boolean = true,
    val permissionGranted: Boolean = false,
    val preciseLocation: Boolean = false,
    val gpsFixPresent: Boolean = false
)
