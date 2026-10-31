package com.carlauncherc.launcher.location

import kotlin.math.abs

/**
 * Filters the SPEED FIELD of consecutive GPS provider fixes, not distance between coordinates.
 *
 * Android Location.speed is defined in metres per second (usually GNSS Doppler velocity).
 * Position deltas are intentionally never used: their noise created the old random spikes.
 * GPS speed absent/unreliable => null, NEVER a fabricated zero.
 *
 * Pure Kotlin so time, out-of-order fixes and spikes can be exercised as JVM unit tests.
 */
class GpsSpeedFilter {
    private var lastSeenNanos = Long.MIN_VALUE
    private var lastAcceptedNanos = Long.MIN_VALUE
    private var lastAcceptedRaw: Float? = null
    private var lastOutput: Float? = null

    fun reset() {
        lastSeenNanos = Long.MIN_VALUE
        lastAcceptedNanos = Long.MIN_VALUE
        lastAcceptedRaw = null
        lastOutput = null
    }

    fun accept(speedMps: Float?, fixElapsedNanos: Long, accuracyMps: Float?): Float? {
        if (fixElapsedNanos <= 0L || fixElapsedNanos < lastSeenNanos) return null
        if (fixElapsedNanos == lastSeenNanos) return lastOutput
        lastSeenNanos = fixElapsedNanos

        val reported = speedMps?.takeIf { it.isFinite() && it in 0f..MAX_SPEED_MPS }
        if (reported == null ||
            (accuracyMps != null && (!accuracyMps.isFinite() || accuracyMps > MAX_SPEED_ACCURACY_MPS))
        ) {
            lastOutput = null
            return null
        }

        // Outlier protection on adjacent trusted Doppler fixes. A 1Hz jump of 30m/s
        // cannot be real vehicle acceleration. Do not let one bogus vendor fix change
        // the gauge; do allow aggressive real braking and rapid acceleration.
        val dtSeconds = if (lastAcceptedNanos > 0L) {
            (fixElapsedNanos - lastAcceptedNanos) / 1_000_000_000f
        } else Float.POSITIVE_INFINITY
        val previous = lastAcceptedRaw
        if (previous != null && dtSeconds in 0.05f..MAX_FILTER_GAP_SECONDS &&
            abs(reported - previous) > MAX_ACCELERATION_MPS2 * dtSeconds + SPIKE_MARGIN_MPS
        ) {
            // Never hold an obsolete speed indefinitely.
            return lastOutput.takeIf { it != null && dtSeconds <= MAX_HOLD_SECONDS }
        }

        val corrected = if (reported < STATIONARY_GATE_MPS) 0f else reported
        val value = if (corrected == 0f || lastOutput == null ||
            dtSeconds > MAX_FILTER_GAP_SECONDS || !dtSeconds.isFinite()
        ) corrected else {
            // Mild smoothing at 1Hz; Doppler remains the source, not position deltas.
            SMOOTHING_ALPHA * corrected + (1f - SMOOTHING_ALPHA) * lastOutput!!
        }

        lastAcceptedRaw = reported
        lastAcceptedNanos = fixElapsedNanos
        lastOutput = value
        return value
    }

    private companion object {
        const val MAX_SPEED_MPS = 90f       // 324km/h; rejects absurd firmware readings
        const val MAX_SPEED_ACCURACY_MPS = 6f
        const val STATIONARY_GATE_MPS = 0.6f
        const val MAX_ACCELERATION_MPS2 = 13f
        const val SPIKE_MARGIN_MPS = 2.5f
        const val MAX_FILTER_GAP_SECONDS = 3f
        const val MAX_HOLD_SECONDS = 1.5f
        const val SMOOTHING_ALPHA = 0.72f
    }
}
