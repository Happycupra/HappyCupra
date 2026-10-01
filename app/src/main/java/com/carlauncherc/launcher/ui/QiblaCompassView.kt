package com.carlauncherc.launcher.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.carlauncherc.launcher.R
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Round dashboard compass with a Qibla pointer.
 *
 * The Qibla bearing is calculated locally from the current GPS position; no network service is
 * needed. While the car is moving, GPS course-over-ground is preferred because a magnetometer
 * mounted inside a metal dashboard is often distorted. At a stop, the rotation-vector sensor is
 * used when the head unit has one.
 */
class QiblaCompassView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), SensorEventListener, LocationListener {

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPath = Path()

    private val sensorManager = context.applicationContext
        .getSystemService(SensorManager::class.java)
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val locationManager = context.applicationContext
        .getSystemService(LocationManager::class.java)

    private val rotationMatrix = FloatArray(9)
    private val remappedMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var sensorHeadingDeg: Float? = null
    private var gpsHeadingDeg: Float? = null
    private var gpsHeadingElapsedMs = 0L
    private var qiblaBearingDeg: Float? = null

    private var sinHeading = 0.0
    private var cosHeading = 0.0
    private var sensorSeeded = false

    private val accentColor: Int
        get() = themeColor(
            com.google.android.material.R.attr.colorPrimary,
            R.color.cockpit_accent
        )
    private val primaryTextColor: Int
        get() = themeColor(
            com.google.android.material.R.attr.colorOnSurface,
            R.color.cockpit_text_primary
        )
    private val secondaryTextColor: Int
        get() = themeColor(
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            R.color.cockpit_text_secondary
        )

    init {
        isClickable = false
        isFocusable = false
        contentDescription = "Qibla compass"
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startSensors()
        startLocation()
    }

    override fun onDetachedFromWindow() {
        runCatching { sensorManager?.unregisterListener(this) }
        runCatching { locationManager?.removeUpdates(this) }
        super.onDetachedFromWindow()
    }

    private fun startSensors() {
        val sensor = rotationSensor ?: return
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        val manager = locationManager ?: return
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return

        val providers = buildList {
            if (fine && runCatching {
                    manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                }.getOrDefault(false)
            ) add(LocationManager.GPS_PROVIDER)
            if (runCatching {
                    manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                }.getOrDefault(false)
            ) add(LocationManager.NETWORK_PROVIDER)
        }

        // Use the freshest cached position immediately so Qibla does not stay blank while the
        // GNSS receiver is obtaining a new fix after boot.
        providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { it.time }?.let(::onLocationChanged)

        providers.forEach { provider ->
            runCatching {
                manager.requestLocationUpdates(provider, LOCATION_INTERVAL_MS, 0f, this)
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        // The display is mounted upright in the dashboard rather than lying flat on a table.
        SensorManager.remapCoordinateSystem(
            rotationMatrix,
            SensorManager.AXIS_X,
            SensorManager.AXIS_Z,
            remappedMatrix
        )
        SensorManager.getOrientation(remappedMatrix, orientation)

        val radians = orientation[0].toDouble()
        if (!sensorSeeded) {
            sinHeading = sin(radians)
            cosHeading = cos(radians)
            sensorSeeded = true
        } else {
            sinHeading += SENSOR_ALPHA * (sin(radians) - sinHeading)
            cosHeading += SENSOR_ALPHA * (cos(radians) - cosHeading)
        }

        sensorHeadingDeg = normalize(Math.toDegrees(atan2(sinHeading, cosHeading)).toFloat())
        invalidate()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onLocationChanged(location: Location) {
        qiblaBearingDeg = qiblaBearing(location.latitude, location.longitude)

        if (location.hasBearing() && location.hasSpeed() && location.speed >= GPS_HEADING_MIN_MPS) {
            gpsHeadingDeg = normalize(location.bearing)
            gpsHeadingElapsedMs = SystemClock.elapsedRealtime()
        } else if (SystemClock.elapsedRealtime() - gpsHeadingElapsedMs > GPS_HEADING_STALE_MS) {
            gpsHeadingDeg = null
        }
        invalidate()
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val cx = width / 2f
        val cy = height * 0.45f
        val radius = min(width * 0.43f, height * 0.37f).coerceAtLeast(24f * density)
        val heading = currentHeading()

        drawOuterRing(canvas, cx, cy, radius)
        drawCompassRose(canvas, cx, cy, radius, heading ?: 0f)
        drawVehicleMarker(canvas, cx, cy, radius)

        val qibla = qiblaBearingDeg
        if (qibla != null) {
            drawQiblaPointer(canvas, cx, cy, radius, normalize(qibla - (heading ?: 0f)))
        }

        drawStatus(canvas, cx, cy, radius, qibla, heading)
    }

    private fun drawOuterRing(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.color = secondaryTextColor
        paint.alpha = 150
        canvas.drawCircle(cx, cy, radius, paint)
        paint.alpha = 255
    }

    private fun drawCompassRose(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        headingDeg: Float
    ) {
        paint.strokeCap = Paint.Cap.ROUND
        for (absolute in 0 until 360 step 15) {
            val relative = normalize(absolute.toFloat() - headingDeg)
            val major = absolute % 45 == 0
            val outer = polar(cx, cy, radius * 0.93f, relative)
            val inner = polar(cx, cy, radius * if (major) 0.82f else 0.87f, relative)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (major) 2f * density else 1f * density
            paint.color = if (absolute == 0) accentColor else secondaryTextColor
            paint.alpha = if (major) 230 else 110
            canvas.drawLine(inner.first, inner.second, outer.first, outer.second, paint)
        }
        paint.alpha = 255

        val cardinals = arrayOf(0f to "N", 90f to "E", 180f to "S", 270f to "W")
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 12f * density
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        cardinals.forEach { (absolute, label) ->
            val relative = normalize(absolute - headingDeg)
            val point = polar(cx, cy, radius * 0.69f, relative)
            paint.color = if (absolute == 0f) accentColor else primaryTextColor
            val y = point.second - (paint.ascent() + paint.descent()) / 2f
            canvas.drawText(label, point.first, y, paint)
        }
        paint.typeface = android.graphics.Typeface.DEFAULT
    }

    private fun drawVehicleMarker(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        paint.style = Paint.Style.FILL
        paint.color = primaryTextColor
        val top = cy - radius * 0.96f
        arrowPath.reset()
        arrowPath.moveTo(cx, top)
        arrowPath.lineTo(cx - 5f * density, top + 9f * density)
        arrowPath.lineTo(cx + 5f * density, top + 9f * density)
        arrowPath.close()
        canvas.drawPath(arrowPath, paint)
    }

    private fun drawQiblaPointer(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        relativeBearing: Float
    ) {
        val tip = polar(cx, cy, radius * 0.72f, relativeBearing)
        paint.color = accentColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f * density
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx, cy, tip.first, tip.second, paint)

        // Arrow head pointing toward Makkah.
        val left = polar(tip.first, tip.second, 10f * density, normalize(relativeBearing + 150f))
        val right = polar(tip.first, tip.second, 10f * density, normalize(relativeBearing - 150f))
        paint.style = Paint.Style.FILL
        arrowPath.reset()
        arrowPath.moveTo(tip.first, tip.second)
        arrowPath.lineTo(left.first, left.second)
        arrowPath.lineTo(right.first, right.second)
        arrowPath.close()
        canvas.drawPath(arrowPath, paint)

        // Small Kaaba marker on the same radial direction.
        val marker = polar(cx, cy, radius * 0.88f, relativeBearing)
        val half = 5f * density
        canvas.drawRect(
            marker.first - half,
            marker.second - half,
            marker.first + half,
            marker.second + half,
            paint
        )

        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, 4f * density, paint)
    }

    private fun drawStatus(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        qibla: Float?,
        heading: Float?
    ) {
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        paint.textSize = 10f * density
        paint.color = accentColor
        val label = if (qibla != null) "QIBLA ${qibla.toInt()}°" else "QIBLA • GPS"
        canvas.drawText(label, cx, cy + radius + 17f * density, paint)

        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.textSize = 8f * density
        paint.color = secondaryTextColor
        val source = when {
            heading == null -> "WAITING FOR HEADING"
            gpsHeadingDeg != null &&
                SystemClock.elapsedRealtime() - gpsHeadingElapsedMs <= GPS_HEADING_STALE_MS -> "GPS HEADING"
            else -> "COMPASS HEADING"
        }
        canvas.drawText(source, cx, cy + radius + 29f * density, paint)
    }

    private fun currentHeading(): Float? {
        val gpsFresh = gpsHeadingDeg != null &&
            SystemClock.elapsedRealtime() - gpsHeadingElapsedMs <= GPS_HEADING_STALE_MS
        return if (gpsFresh) gpsHeadingDeg else sensorHeadingDeg
    }

    private fun polar(
        cx: Float,
        cy: Float,
        radius: Float,
        degreesClockwiseFromTop: Float
    ): Pair<Float, Float> {
        val radians = Math.toRadians(degreesClockwiseFromTop.toDouble())
        return (cx + sin(radians).toFloat() * radius) to
            (cy - cos(radians).toFloat() * radius)
    }

    private fun qiblaBearing(latitude: Double, longitude: Double): Float {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val deltaLon = Math.toRadians(KAABA_LONGITUDE - longitude)

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return normalize(Math.toDegrees(atan2(y, x)).toFloat())
    }

    private fun normalize(value: Float): Float = ((value % 360f) + 360f) % 360f

    private fun themeColor(attr: Int, fallback: Int): Int {
        val value = TypedValue()
        if (context.theme.resolveAttribute(attr, value, true)) {
            if (value.resourceId != 0) {
                return runCatching { ContextCompat.getColor(context, value.resourceId) }
                    .getOrDefault(value.data)
            }
            return value.data
        }
        return ContextCompat.getColor(context, fallback)
    }

    private companion object {
        const val KAABA_LATITUDE = 21.422487
        const val KAABA_LONGITUDE = 39.826206
        const val LOCATION_INTERVAL_MS = 1_000L
        const val GPS_HEADING_MIN_MPS = 1.5f
        const val GPS_HEADING_STALE_MS = 5_000L
        const val SENSOR_ALPHA = 0.12
    }
}
