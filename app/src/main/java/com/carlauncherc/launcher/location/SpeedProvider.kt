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

/**
 * Uses GPS_PROVIDER exclusively and Location.getSpeed() in Android's standard m/s.
 * No position-delta calculations, NetworkProvider, or inferred km/h conversion.
 * Bad/missing speed is represented as unavailable instead of displaying a wrong number.
 */
class SpeedProvider(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val filter = GpsSpeedFilter()

    val hasPermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val hasCoarsePermission: Boolean
        get() = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val isGpsEnabled: Boolean
        get() = runCatching {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        }.getOrDefault(false)

    fun reset() = filter.reset()

    @SuppressLint("MissingPermission")
    fun locations(): Flow<Location> = callbackFlow {
        val manager = locationManager
        if (!hasPermission || manager == null) {
            close()
            return@callbackFlow
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.provider == LocationManager.GPS_PROVIDER) trySend(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        // A cached fix is acceptable only when its MONOTONIC timestamp is still fresh.
        lastFreshLocation()?.let { trySend(it) }
        try {
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 500L, 0f, listener, Looper.getMainLooper()
            )
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        awaitClose { runCatching { manager.removeUpdates(listener) } }
    }

    @SuppressLint("MissingPermission")
    fun lastFreshLocation(): Location? {
        if (!hasPermission) return null
        val fix = runCatching {
            locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }.getOrNull() ?: return null
        return fix.takeIf { ageMs(it) in 0..Constants.FIX_STALE_MS }
    }

    fun ageMs(location: Location): Long {
        val nanos = location.elapsedRealtimeNanos
        if (nanos <= 0L) return Long.MAX_VALUE
        return (SystemClock.elapsedRealtimeNanos() - nanos) / 1_000_000L
    }

    /** Null means the GPS fix has no trustworthy speed; do not show "0". */
    fun speedOf(location: Location): Float? {
        if (location.provider != LocationManager.GPS_PROVIDER ||
            ageMs(location) !in 0..Constants.FIX_STALE_MS
        ) return null
        val accuracy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            location.hasSpeedAccuracy()
        ) location.speedAccuracyMetersPerSecond else null
        return filter.accept(
            speedMps = if (location.hasSpeed()) location.speed else null,
            fixElapsedNanos = location.elapsedRealtimeNanos,
            accuracyMps = accuracy
        )
    }
}
