package ch.drivedeck.integration.gps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AndroidLocationRepository(private val context: Context) : LocationRepository, LocationListener {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val mutableReading = MutableStateFlow(LocationReading())
    override val reading: StateFlow<LocationReading> = mutableReading
    private var listening = false

    override fun start() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mutableReading.value = LocationReading(GpsStatus.PERMISSION_REQUIRED)
            return
        }
        val enabled = runCatching { manager.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        if (!enabled) {
            mutableReading.value = LocationReading(GpsStatus.DISABLED)
            return
        }
        if (listening) return
        mutableReading.value = LocationReading(GpsStatus.SEARCHING)
        listening = runCatching {
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1_000L, 0.5f, this)
            true
        }.getOrElse {
            mutableReading.value = LocationReading(GpsStatus.UNAVAILABLE)
            false
        }
    }

    override fun stop() {
        if (!listening) return
        runCatching { manager.removeUpdates(this) }
        listening = false
    }

    override fun onLocationChanged(location: Location) {
        mutableReading.value = LocationReading(
            status = GpsStatus.FIXED,
            speedKmh = location.takeIf(Location::hasSpeed)?.speed?.let(::metersPerSecondToKmh),
            bearingDegrees = location.takeIf(Location::hasBearing)?.bearing,
            accuracyMeters = location.takeIf(Location::hasAccuracy)?.accuracy,
            timestampMillis = location.time,
        )
    }

    override fun onProviderDisabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) mutableReading.value = LocationReading(GpsStatus.DISABLED)
    }
    override fun onProviderEnabled(provider: String) {
        if (provider == LocationManager.GPS_PROVIDER) mutableReading.value = LocationReading(GpsStatus.SEARCHING)
    }
    @Deprecated("Legacy callback required through API 29")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
}
