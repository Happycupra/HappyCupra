package com.carlauncherc.launcher.media

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/** Normalized frequency spectrum; bands contain values between 0 and 1. */
data class AudioSpectrumFrame(val bands: FloatArray, val bass: Float)

/**
 * Decodes the packed unsigned/signed 8-bit FFT supplied by Android audiofx.Visualizer.
 *
 * The Android format contains DC at [0], Nyquist at [1], followed by real/imag pairs
 * for bins 1..N/2-1. Bands are logarithmically grouped to make bass clearly visible.
 * This is genuine audio-frequency information, not a time-based simulated waveform.
 */
class AudioSpectrumAnalyzer(private val bandCount: Int = 32) {
    private var envelope = 8f

    init { require(bandCount in 4..64) }

    fun reset() {
        envelope = 8f
    }

    /**
     * Null means no meaningful audio signal. Caller should retain the synthetic animation
     * in that case; vendor head units commonly disable mixed-output visualization.
     */
    fun analyze(fft: ByteArray): AudioSpectrumFrame? {
        val complexBins = fft.size / 2
        if (complexBins < 4) return null

        val sums = FloatArray(bandCount)
        val counts = IntArray(bandCount)
        val denom = ln(complexBins.toDouble()).coerceAtLeast(1.0)
        for (bin in 1 until complexBins) {
            val real = fft[bin * 2].toInt()
            val imaginary = fft[bin * 2 + 1].toInt()
            val magnitude = sqrt((real * real + imaginary * imaginary).toFloat())
            val group = ((ln(bin.toDouble()) / denom) * bandCount)
                .toInt().coerceIn(0, bandCount - 1)
            sums[group] += magnitude
            counts[group]++
        }

        val raw = FloatArray(bandCount)
        var peak = 0f
        for (i in 0 until bandCount) {
            val strength = if (counts[i] > 0) sums[i] / counts[i] else 0f
            raw[i] = strength
            peak = max(peak, strength)
        }
        // Silence / blocked capture must not be confused with valid FFT.
        if (!peak.isFinite() || peak < 2.5f) return null

        envelope = max(peak, envelope * 0.97f)
        val scale = envelope.coerceAtLeast(8f)
        val normalized = FloatArray(bandCount) { index ->
            (raw[index] / scale).coerceIn(0f, 1f).pow(0.68f)
        }
        val lowCount = (bandCount / 4).coerceAtLeast(1)
        val bass = (0 until lowCount).sumOf { normalized[it].toDouble() }
            .div(lowCount).toFloat().coerceIn(0f, 1f)
        return AudioSpectrumFrame(normalized, bass)
    }
}
