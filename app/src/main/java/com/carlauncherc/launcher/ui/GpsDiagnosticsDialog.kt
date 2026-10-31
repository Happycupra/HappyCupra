package com.carlauncherc.launcher.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.carlauncherc.launcher.R
import com.carlauncherc.launcher.location.GpsQuality
import java.util.Locale

/**
 * A foreground-only diagnostic window. The GNSS satellite listener is unregistered
 * immediately when the dialog closes. No location coordinates are stored or displayed.
 */
object GpsDiagnosticsDialog {
    fun show(activity: AppCompatActivity, viewModel: HomeViewModel) {
        val manager = activity.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val density = activity.resources.displayMetrics.density
        val content = TextView(activity).apply {
            val pad = (22f * density).toInt()
            setPadding(pad, pad, pad, pad)
            textSize = 16f
            setTextIsSelectable(true)
        }
        var visibleSatellites: Int? = null
        var usedSatellites: Int? = null

        fun render() {
            val d = viewModel.gpsDiagnostics()
            val quality = when (d.quality) {
                GpsQuality.NO_PERMISSION -> activity.getString(R.string.gps_diagnostics_quality_permission)
                GpsQuality.DISABLED -> activity.getString(R.string.gps_diagnostics_quality_off)
                GpsQuality.NO_FIX -> activity.getString(R.string.gps_diagnostics_quality_no_fix)
                GpsQuality.STALE -> activity.getString(R.string.gps_diagnostics_quality_stale)
                GpsQuality.POOR -> activity.getString(R.string.gps_diagnostics_quality_poor)
                GpsQuality.FAIR -> activity.getString(R.string.gps_diagnostics_quality_fair)
                GpsQuality.GOOD -> activity.getString(R.string.gps_diagnostics_quality_good)
            }
            val loc = Locale.getDefault()
            fun meters(v: Float?) = v?.takeIf { it.isFinite() && it >= 0f }
                ?.let { String.format(loc, "%.1f m", it) } ?: "—"
            fun velocity(v: Float?) = v?.takeIf { it.isFinite() && it >= 0f }
                ?.let { String.format(loc, "%.1f km/h", it * 3.6f) } ?: "—"
            val age = d.fixAgeMs?.takeIf { it >= 0L }
                ?.let { String.format(loc, "%.1f s", it / 1000.0) } ?: "—"
            val accuracy = d.speedAccuracyMps?.takeIf { it.isFinite() && it >= 0f }
                ?.let { String.format(loc, "± %.1f km/h", it * 3.6f) } ?: "—"
            val course = d.bearingDegrees?.takeIf { it.isFinite() }
                ?.let { String.format(loc, "%.0f°", it) } ?: "—"
            val sats = if (visibleSatellites != null)
                "$usedSatellites used / $visibleSatellites visible"
            else "— (not reported by GNSS)"

            content.text = """
                GPS quality: $quality

                GNSS satellites: $sats
                Last position fix: $age ago
                Horizontal accuracy: ${meters(d.horizontalAccuracyMeters)}
                GPS speed accuracy: $accuracy

                Raw GPS velocity: ${velocity(d.rawSpeedMps)}
                Filtered dashboard velocity: ${velocity(d.filteredSpeedMps)}
                GPS course: $course

                Source: Android GPS_PROVIDER
                Speed is read directly from GNSS, not coordinate changes.

                • Stale or missing speed shows — on the dashboard.
                • Weak GPS accuracy makes speed less reliable.
                • A difference between raw and filtered speed may indicate an outlier.
                • Satellite reporting depends on head-unit firmware.
                • This diagnostic data is not saved or transmitted.
            """.trimIndent()
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.gps_diagnostics_title)
            .setView(content)
            .setPositiveButton(R.string.gps_diagnostics_close, null)
            .create()
        val handler = Handler(Looper.getMainLooper())
        val gnssCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                visibleSatellites = status.satelliteCount
                usedSatellites = (0 until status.satelliteCount).count { status.usedInFix(it) }
                render()
            }
            override fun onStopped() {
                visibleSatellites = null
                usedSatellites = null
                render()
            }
        }
        var registered = false
        val ticker = object : Runnable {
            override fun run() {
                if (dialog.isShowing) {
                    render()
                    handler.postDelayed(this, 1_000L)
                }
            }
        }
        dialog.setOnShowListener {
            if (manager != null &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                registered = runCatching {
                    manager.registerGnssStatusCallback(gnssCallback, handler)
                }.getOrDefault(false)
            }
            render()
            handler.postDelayed(ticker, 1_000L)
        }
        dialog.setOnDismissListener {
            handler.removeCallbacks(ticker)
            if (registered) runCatching { manager?.unregisterGnssStatusCallback(gnssCallback) }
        }
        dialog.show()
    }
}
