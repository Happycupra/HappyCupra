package com.carlauncherc.launcher.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.carlauncherc.launcher.core.Constants
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.abs

/**
 * GPS speed and course, via the framework LocationManager.
 *
 * Deliberately NOT FusedLocationProviderClient: many aftermarket head units ship without
 * Google Play Services (or with a stub), where the fused client silently never fires. The
 * framework API is always present and gives us the raw GNSS fixes.
 *
 * Some head-unit firmware has imperfect Location implementations: speed can be missing,
 * carry a very poor accuracy value, or (on a few vendor stacks) be exposed in km/h even
 * though Android's contract requires m/s. To keep the dashboard useful we cross-check the
 * reported speed against distance/time between consecutive GNSS fixes and only use the
 * derived value when the framework value is clearly unusable.
 */
class SpeedProvider(context: Context) {

    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    private var previousFix: Location? = null
    private var lastEvaluatedFixNanos = Long.MIN_VALUE
    private var lastEvaluatedFixTimeMs = Long.MIN_VALUE
    private var lastSpeedMps = 0f
    private var lastDerivedSpeedMps: Float? = null

    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val hasCoarsePermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val isGpsEnabled: Boolean
        get() = try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        } catch (e: Exception) {
            false
        }

    /**
     * Cold flow of GPS fixes. Collection registers the listener; cancelling it calls
     * removeUpdates, so the receiver is only running while the launcher is visible.
     */
    @SuppressLint("MissingPermission")
    fun locations(): Flow<Location> = callbackFlow {
        val manager = locationManager
        if (!hasPermission || manager == null) {
            close()
            return@callbackFlow
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                trySend(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit

            override fun onProviderDisabled(provider: String) = Unit
        }

        // Seed from the last known fix, but only if it is fresh - otherwise the gauge would
        // open at yesterday's speed.
        lastFreshLocation()?.let { trySend(it) }

        try {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                500L,     // accept faster vendor GNSS rates; 1 Hz receivers still behave normally
                0f,       // distance filter must stay 0 or the readout can freeze at a stop
                listener,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }

        awaitClose { runCatching { manager.removeUpdates(listener) } }
    }

    @SuppressLint("MissingPermission")
    fun lastFreshLocation(): Location? {
        val manager = locationManager
        if (!hasPermission || manager == null) return null
        val last = try {
            manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            null
        } ?: return null
        val ageMs = (SystemClock.elapsedRealtimeNanos() - last.elapsedRealtimeNanos) / 1_000_000L
        return if (ageMs in 0..10_000L) last else null
    }

    /**
     * Returns speed in m/s.
     *
     * Android specifies [Location.getSpeed] in m/s. We prefer it whenever it is sane because
     * GNSS Doppler speed is normally more accurate than position-delta speed. The fallback
     * catches missing/broken vendor values and obvious unit mistakes without changing good
     * Android implementations.
     */
    fun speedOf(location: Location): Float {
        // publish() is also called by a one-second stale-fix ticker. Do not re-derive speed from
        // the same fix because that would compare a location with itself and collapse to zero.
        val fixNanos = location.elapsedRealtimeNanos
        val fixTimeMs = location.time
        val isSameFix = if (fixNanos > 0L) {
            fixNanos == lastEvaluatedFixNanos
        } else {
            fixTimeMs == lastEvaluatedFixTimeMs
        }
        if (isSameFix) return lastSpeedMps

        val direct = reportedSpeed(location)
        val derived = derivedSpeed(previousFix, location)
        val poorDirectAccuracy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            location.hasSpeedAccuracy() &&
            location.speedAccuracyMetersPerSecond > DIRECT_ACCURACY_POOR_MPS

        val chosen = when {
            direct != null && derived != null && looksLikeVendorUnitBug(direct, derived) -> derived
            direct != null && derived != null && poorDirectAccuracy &&
                abs(direct - derived) > DIRECT_DERIVED_DISAGREEMENT_MPS -> derived
            direct != null -> direct
            derived != null -> derived
            else -> 0f
        }

        // Derived position-delta speed is naturally noisier, so only smooth that fallback.
        val output = if (derived != null && chosen === derived) {
            val previous = lastDerivedSpeedMps
            val filtered = if (previous == null || chosen < Constants.SPEED_NOISE_GATE_MPS) {
                chosen
            } else {
                DERIVED_ALPHA * chosen + (1f - DERIVED_ALPHA) * previous
            }
            lastDerivedSpeedMps = filtered
            filtered
        } else {
            lastDerivedSpeedMps = null
            chosen
        }.let { if (it < Constants.SPEED_NOISE_GATE_MPS) 0f else it }

        previousFix = Location(location)
        lastEvaluatedFixNanos = fixNanos
        lastEvaluatedFixTimeMs = fixTimeMs
        lastSpeedMps = output.coerceIn(0f, MAX_REASONABLE_SPEED_MPS)
        return lastSpeedMps
    }

    private fun reportedSpeed(location: Location): Float? {
        if (!location.hasSpeed()) return null
        val value = location.speed
        if (!value.isFinite() || value < 0f || value > MAX_VENDOR_REPORTED_MPS) return null
        return value
    }

    private fun derivedSpeed(previous: Location?, current: Location): Float? {
        previous ?: return null

        val dtSeconds = when {
            current.elapsedRealtimeNanos > 0L && previous.elapsedRealtimeNanos > 0L ->
                (current.elapsedRealtimeNanos - previous.elapsedRealtimeNanos) / 1_000_000_000f
            else -> (current.time - previous.time) / 1_000f
        }
        if (dtSeconds !in MIN_DERIVE_INTERVAL_S..MAX_DERIVE_INTERVAL_S) return null

        // Position-delta speed is useless when the location radius is huge. Direct GNSS speed,
        // if present, remains usable and will be selected instead.
        if ((current.hasAccuracy() && current.accuracy > MAX_DERIVE_ACCURACY_M) ||
            (previous.hasAccuracy() && previous.accuracy > MAX_DERIVE_ACCURACY_M)
        ) return null

        val speed = previous.distanceTo(current) / dtSeconds
        return speed.takeIf { it.isFinite() && it in 0f..MAX_REASONABLE_SPEED_MPS }
    }

    private fun looksLikeVendorUnitBug(direct: Float, derived: Float): Boolean {
        // A km/h value incorrectly exposed as m/s is ~3.6x the real value. Require both a
        // strong ratio and a meaningful absolute gap so normal acceleration does not trip it.
        return derived > 1f &&
            direct > derived * 2.5f &&
            direct - derived > 8f
    }

    private companion object {
        const val MIN_DERIVE_INTERVAL_S = 0.25f
        const val MAX_DERIVE_INTERVAL_S = 5f
        const val MAX_DERIVE_ACCURACY_M = 50f
        const val DIRECT_ACCURACY_POOR_MPS = 4f
        const val DIRECT_DERIVED_DISAGREEMENT_MPS = 5f
        const val DERIVED_ALPHA = 0.65f
        const val MAX_REASONABLE_SPEED_MPS = 90f      // 324 km/h
        const val MAX_VENDOR_REPORTED_MPS = 140f     // lets us detect km/h-as-m/s before rejecting
    }
}
