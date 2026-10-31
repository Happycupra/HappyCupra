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

/**
 * Full music-card-width stage:
 *  - equalizer: many bars,
 *  - solo: one articulated stick figure roams across the stage,
 *  - duo sync: two mirrored figures share one choreography and beat phase,
 *  - duo freestyle: two independently seeded random directors.
 *
 * Play/pause is read from YMusic MediaSession. Rhythm is SYNTHETIC, not measured audio:
 * remote MediaSession does not provide song PCM, FFT or actual BPM data.
 */
class PlaybackEqualizerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.cockpit_accent)
        style = Paint.Style.FILL
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = paint.color
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val stagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = paint.color
        alpha = 65
        strokeWidth = density
    }

    private var playing = false
    private var active = true
    private var mode = VisualizerMotion.EQUALIZER
    private val startSeed = SystemClock.elapsedRealtime().toInt()
    private val solo = DanceDirector(startSeed xor 0x1a3b3)
    private val partner = DanceDirector(startSeed xor 0x72519)
    private var lastA: DanceSkeleton? = null
    private var lastB: DanceSkeleton? = null
    private var beforeA: DanceSkeleton? = null
    private var beforeB: DanceSkeleton? = null
    private var transitionAt = 0L
    private var scheduled = false

    private val ticker = object : Runnable {
        override fun run() {
            scheduled = false
            if (!shouldAnimate()) return
            invalidate()
            scheduleFrame()
        }
    }

    fun setBarColor(color: Int) {
        paint.color = color
        stroke.color = color
        stagePaint.color = color
        stagePaint.alpha = 65
        invalidate()
    }

    fun setMode(raw: String) {
        val next = VisualizerMotion.normalizeMode(raw)
        if (mode == next) return
        captureTransition()
        mode = next
        updateFrames()
    }

    fun setPlaying(value: Boolean) {
        if (playing == value) return
        captureTransition()
        playing = value
        if (value) {
            val now = SystemClock.uptimeMillis()
            solo.reset(now)
            partner.reset(now)
        }
        updateFrames()
    }

    /** Stop ALL scheduled frames when the HOME activity is not foreground. */
    fun setActive(value: Boolean) {
        if (active == value) return
        active = value
        updateFrames()
    }

    private fun captureTransition() {
        beforeA = lastA
        beforeB = lastB
        transitionAt = SystemClock.uptimeMillis()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateFrames()
    }

    override fun onDetachedFromWindow() {
        stopFrames()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateFrames()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateFrames()
    }

    private fun shouldAnimate(): Boolean =
        active && isAttachedToWindow && isShown && windowVisibility == VISIBLE &&
            (playing || VisualizerMotion.isDancer(mode))

    private fun updateFrames() {
        if (shouldAnimate()) scheduleFrame() else stopFrames()
        invalidate()
    }

    private fun scheduleFrame() {
        if (scheduled || !shouldAnimate()) return
        scheduled = true
        // 30 fps in playback, ~7 fps in idle; no timer while view is hidden.
        postDelayed(ticker, if (playing) 33L else 145L)
    }

    private fun stopFrames() {
        removeCallbacks(ticker)
        scheduled = false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        val now = SystemClock.uptimeMillis()
        if (mode == VisualizerMotion.EQUALIZER) drawBars(canvas, now)
        else drawStage(canvas, now)
    }

    private fun drawBars(canvas: Canvas, now: Long) {
        val w = width.toFloat()
        val h = height.toFloat()
        val count = (w / (5.9f * density)).toInt().coerceIn(1, 160)
        val pitch = w / count
        val bw = pitch * 0.57f
        val radius = bw * 0.33f
        val t = now / 1000.0
        for (i in 0 until count) {
            val a = sin(i * 0.69 + t * 7.8)
            val b = sin(i * 0.24 - t * 11.5)
            val beat = abs(sin(t * PI * 2.0))
            val power = if (playing) {
                (0.12 + 0.7 * abs(0.62 * a + 0.38 * b) + 0.12 * beat).coerceIn(0.13, 0.97)
            } else 0.14
            val bh = (h * power).toFloat().coerceAtLeast(2f * density)
            val x = pitch * i + (pitch - bw) / 2f
            canvas.drawRoundRect(x, h - bh, x + bw, h, radius, radius, paint)
        }
    }

    private fun drawStage(canvas: Canvas, now: Long) {
        val w = width.toFloat()
        val h = height.toFloat()
        val baseline = h - 3f * density
        stagePaint.strokeWidth = density
        canvas.drawLine(3f * density, baseline, w - 3f * density, baseline, stagePaint)

        val duo = VisualizerMotion.numberOfDancers(mode) == 2
        val sync = mode == VisualizerMotion.DUO_SYNC
        val dancerA = figureFor(solo, now, sync)
        val dancerB = if (duo) {
            figureFor(if (sync) solo else partner, now, sync)
        } else null

        val blending = DanceDirector.ease(((now - transitionAt) / 550f).coerceIn(0f, 1f))
        val shownA = beforeA?.takeIf { blending < 1f }?.mixed(dancerA, blending) ?: dancerA
        lastA = shownA
        if (blending >= 1f) beforeA = null

        val scale = min(h / 106f, w / (if (duo) 280f else 145f))
        val slowRoam = sin(now / 3_500.0)
        val centerA = if (duo) w * (0.24 + 0.15 * slowRoam).toFloat()
            else w * (0.50 + 0.37 * slowRoam).toFloat()
        drawSkeleton(canvas, shownA, centerA, baseline, scale, mirrored = false)

        if (dancerB != null) {
            val shownB = beforeB?.takeIf { blending < 1f }?.mixed(dancerB, blending) ?: dancerB
            lastB = shownB
            if (blending >= 1f) beforeB = null
            val partnerRoam = sin(now / 4_100.0 + if (sync) PI else 1.3)
            val centerB = w * (0.76 + 0.15 * partnerRoam).toFloat()
            drawSkeleton(canvas, shownB, centerB, baseline, scale, mirrored = sync)
        } else {
            lastB = null
            beforeB = null
        }
    }

    private fun figureFor(director: DanceDirector, now: Long, sync: Boolean): DanceSkeleton {
        if (playing) {
            val frame = director.dance(now, synchronizedPhase = sync)
            val old = DancePose.dance(frame.from, frame.phase)
            val new = DancePose.dance(frame.move, frame.phase)
            return old.mixed(new, frame.blend)
        }
        val frame = director.rest(now)
        val phase = now / 900.0
        val old = DancePose.rest(frame.from, phase)
        val new = DancePose.rest(frame.pose, phase)
        return old.mixed(new, frame.blend)
    }

    private fun drawSkeleton(
        canvas: Canvas,
        p: DanceSkeleton,
        x: Float,
        bottom: Float,
        size: Float,
        mirrored: Boolean
    ) {
        canvas.save()
        canvas.translate(x, bottom)
        canvas.scale(if (mirrored) -size else size, size)
        stroke.strokeWidth = (2.6f * density / size).coerceIn(2.3f, 5.0f)
        val neck = p.head.shifted(0f, 10f)
        segment(canvas, neck, p.shoulder)
        segment(canvas, p.shoulder, p.hip)
        segment(canvas, p.shoulder, p.leftElbow)
        segment(canvas, p.leftElbow, p.leftHand)
        segment(canvas, p.shoulder, p.rightElbow)
        segment(canvas, p.rightElbow, p.rightHand)
        segment(canvas, p.hip, p.leftKnee)
        segment(canvas, p.leftKnee, p.leftFoot)
        segment(canvas, p.hip, p.rightKnee)
        segment(canvas, p.rightKnee, p.rightFoot)
        canvas.drawCircle(p.head.x, p.head.y, 8.1f, stroke)
        // Tiny hands/feet, especially helpful to read the pose at head-unit sizes.
        for (j in arrayOf(p.leftHand, p.rightHand, p.leftFoot, p.rightFoot)) {
            canvas.drawCircle(j.x, j.y, 2f, paint)
        }
        canvas.restore()
    }

    private fun segment(canvas: Canvas, a: Joint, b: Joint) =
        canvas.drawLine(a.x, a.y, b.x, b.y, stroke)
}
