package com.carlauncherc.launcher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.carlauncherc.launcher.R
import kotlin.math.sin

/**
 * Lightweight equalizer-style playback indicator. The wave is an animation of YMusic's
 * PLAYING state, not sampled audio frequencies (Android's remote MediaSession does not
 * expose the waveform). No microphone / RECORD_AUDIO permissions required.
 *
 * Animates ONLY while attached, visible and actually playing.
 */
class PlaybackEqualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.cockpit_accent)
        style = Paint.Style.FILL
    }
    private var playing = false
    private var phase = 0f
    private var animator: ValueAnimator? = null

    fun setBarColor(color: Int) {
        paint.color = color
        invalidate()
    }

    fun setPlaying(value: Boolean) {
        if (playing == value) return
        playing = value
        updateAnimator()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateAnimator()
    }

    override fun onDetachedFromWindow() {
        stopAnimator()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimator()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimator()
    }

    private fun updateAnimator() {
        val active = playing && isAttachedToWindow &&
            visibility == VISIBLE && windowVisibility == VISIBLE
        if (!active) {
            stopAnimator()
            return
        }
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1150L
            repeatCount = ValueAnimator.INFINITE
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimator() {
        animator?.cancel()
        animator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val count = 5
        val gap = w * 0.065f
        val barWidth = ((w - gap * (count - 1)) / count).coerceAtLeast(1f)
        val radius = (barWidth * 0.42f).coerceAtLeast(1f)
        for (i in 0 until count) {
            val intensity = if (playing) {
                val wave = sin((phase * 6.2831853f + i * 1.73f).toDouble()).toFloat()
                0.18f + 0.70f * ((wave + 1f) / 2f)
            } else 0.17f
            val barHeight = h * intensity
            val x = i * (barWidth + gap)
            canvas.drawRoundRect(
                x, h - barHeight, x + barWidth, h, radius, radius, paint
            )
        }
    }
}
