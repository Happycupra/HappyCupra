package com.carlauncherc.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import com.carlauncherc.launcher.R
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos

/**
 * Full-width visualizer. Animations mimic a rhythm but DO NOT sample the music:
 * the remote YMusic MediaSession cannot expose PCM or frequency data.
 * Supports quiet equalizer or an idle figure which occasionally sits / lies down.
 */
class PlaybackEqualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val bars = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.cockpit_accent)
        style = Paint.Style.FILL
    }
    private val lines = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = bars.color
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private var playing = false
    private var active = true
    private var mode = VisualizerMotion.EQUALIZER
    private var idleStartedAt = SystemClock.uptimeMillis()
    private var scheduled = false

    private val ticker = object : Runnable {
        override fun run() {
            scheduled = false
            if (shouldAnimate()) {
                invalidate()
                scheduleNext()
            }
        }
    }

    fun setBarColor(color: Int) {
        bars.color = color
        lines.color = color
        invalidate()
    }

    fun setMode(raw: String) {
        val next = VisualizerMotion.normalizeMode(raw)
        if (mode == next) return
        mode = next
        if (!playing) idleStartedAt = SystemClock.uptimeMillis()
        updateAnimation()
    }

    fun setPlaying(value: Boolean) {
        if (playing == value) return
        playing = value
        if (!value) idleStartedAt = SystemClock.uptimeMillis()
        updateAnimation()
    }

    fun setActive(value: Boolean) {
        if (active == value) return
        active = value
        updateAnimation()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimation()
    }

    override fun onDetachedFromWindow() {
        stopTicker()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimation()
    }

    private fun shouldAnimate() =
        active && isAttachedToWindow && isShown && windowVisibility == VISIBLE &&
            (playing || mode == VisualizerMotion.DANCER)

    private fun updateAnimation() {
        if (shouldAnimate()) scheduleNext() else stopTicker()
        invalidate()
    }

    private fun scheduleNext() {
        if (scheduled || !shouldAnimate()) return
        scheduled = true
        postDelayed(ticker, if (playing) 33L else 155L)
    }

    private fun stopTicker() {
        removeCallbacks(ticker)
        scheduled = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        if (mode == VisualizerMotion.DANCER) drawFigure(canvas) else drawBars(canvas)
    }

    private fun drawBars(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val preferredPitch = 5.9f * density
        val count = (w / preferredPitch).toInt().coerceIn(1, 140)
        val pitch = w / count
        val barW = pitch * 0.57f
        val radius = barW * 0.33f
        val time = SystemClock.uptimeMillis() / 1000.0
        for (i in 0 until count) {
            val waveA = sin(i * 0.69 + time * 7.8)
            val waveB = sin(i * 0.24 - time * 11.5)
            val fauxBeat = abs(sin(time * PI * 2))
            val strength = if (playing) {
                (0.12 + 0.70 * abs(0.62 * waveA + 0.38 * waveB) + 0.12 * fauxBeat)
                    .coerceIn(0.13, 0.96)
            } else 0.14
            val barHeight = (h * strength).toFloat().coerceAtLeast(2f * density)
            val x = i * pitch + (pitch - barW) / 2f
            canvas.drawRoundRect(x, h - barHeight, x + barW, h, radius, radius, bars)
        }
    }

    private fun drawFigure(canvas: Canvas) {
        val now = SystemClock.uptimeMillis()
        val phase = now / 500.0 * PI * 2.0 // synthetic 120 BPM visual tempo
        val pose = VisualizerMotion.pose(playing, now - idleStartedAt)
        val w = width.toFloat()
        val h = height.toFloat()
        val s = min(h / 110f, w / 180f)
        val sway = if (playing) sin(phase * 0.5) * min(w * 0.20, 70f * s) else 0.0
        val bottom = h - 2f * density

        lines.strokeWidth = 1f * density
        lines.alpha = 70
        canvas.drawLine(4f * density, bottom, w - 4f * density, bottom, lines)
        lines.alpha = 255

        canvas.save()
        canvas.translate(w * 0.5f + sway.toFloat(), bottom)
        canvas.scale(s, s)
        lines.strokeWidth = (2.5f * density / s).coerceIn(2.5f, 5.5f)
        when (pose) {
            StickFigurePose.DANCE -> {
                val kick = sin(phase).toFloat()
                val bounce = abs(cos(phase)).toFloat() * 7f
                val lean = kick * 5f
                circle(canvas, lean, -77f - bounce, 9f)
                line(canvas, lean, -66f - bounce, lean - 2f, -34f - bounce)
                line(canvas, lean, -61f - bounce, -23f, -49f - bounce - kick * 20f)
                line(canvas, lean, -61f - bounce, 23f, -49f - bounce + kick * 20f)
                line(canvas, lean - 2f, -34f - bounce, -16f - kick * 6f, -16f + kick * 7f)
                line(canvas, -16f - kick * 6f, -16f + kick * 7f, -24f - kick * 13f, -2f)
                line(canvas, lean - 2f, -34f - bounce, 14f + kick * 6f, -19f - kick * 5f)
                line(canvas, 14f + kick * 6f, -19f - kick * 5f, 23f + kick * 12f, -2f)
            }
            StickFigurePose.STAND -> {
                val swayIdle = sin((now - idleStartedAt) / 1900.0).toFloat() * 1.5f
                circle(canvas, swayIdle, -76f, 9f)
                line(canvas, swayIdle, -66f, swayIdle, -33f)
                line(canvas, swayIdle, -61f, -15f, -38f)
                line(canvas, swayIdle, -61f, 15f, -38f)
                line(canvas, swayIdle, -33f, -16f, -2f)
                line(canvas, swayIdle, -33f, 16f, -2f)
            }
            StickFigurePose.SIT -> {
                lines.alpha = 120
                line(canvas, -30f, -25f, 19f, -25f)
                line(canvas, -26f, -25f, -26f, -2f)
                lines.alpha = 255
                circle(canvas, -8f, -73f, 9f)
                line(canvas, -8f, -63f, -13f, -28f)
                line(canvas, -8f, -57f, 10f, -40f)
                line(canvas, -8f, -57f, -25f, -38f)
                line(canvas, -13f, -28f, 24f, -26f)
                line(canvas, 24f, -26f, 24f, -2f)
                line(canvas, -13f, -28f, 10f, -24f)
                line(canvas, 10f, -24f, 9f, -2f)
            }
            StickFigurePose.LIE -> {
                circle(canvas, -47f, -19f, 9f)
                line(canvas, -37f, -18f, 15f, -19f)
                line(canvas, -28f, -18f, -8f, -5f)
                line(canvas, -28f, -18f, -8f, -28f)
                line(canvas, 15f, -19f, 48f, -5f)
                line(canvas, 15f, -19f, 47f, -19f)
            }
        }
        canvas.restore()
    }

    private fun line(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float) =
        c.drawLine(x1, y1, x2, y2, lines)

    private fun circle(c: Canvas, x: Float, y: Float, radius: Float) =
        c.drawCircle(x, y, radius, lines)
}
