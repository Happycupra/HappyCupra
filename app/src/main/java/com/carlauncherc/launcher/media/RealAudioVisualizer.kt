package com.carlauncherc.launcher.media

import android.media.audiofx.Visualizer
import android.util.Log

/**
 * Opt-in audio capture via Android's built-in Visualizer on output-mix session 0.
 *
 * The host Activity must obtain RECORD_AUDIO and check it again before calling start().
 * MODIFY_AUDIO_SETTINGS is also required for global mix (manifest normal permission).
 * We do not create a microphone AudioRecord, store PCM or transmit audio.
 *
 * Some head-unit ROMs block third-party playback capture, so start may fail or FFT may
 * stay zero. The UI automatically uses its previous synthetic effect in that case.
 */
class RealAudioVisualizer(
    private val onSpectrum: (AudioSpectrumFrame) -> Unit
) {
    private var visualizer: Visualizer? = null
    private val analyzer = AudioSpectrumAnalyzer()

    val isRunning: Boolean get() = visualizer != null

    fun start(): Boolean {
        if (visualizer != null) return true
        analyzer.reset()
        var candidate: Visualizer? = null
        return try {
            candidate = Visualizer(0)
            val sizes = Visualizer.getCaptureSizeRange()
            val target = 512.coerceIn(sizes[0], sizes[1])
            candidate.setCaptureSize(target)
            val listener = object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(
                    visualizer: Visualizer?,
                    waveform: ByteArray?,
                    samplingRate: Int
                ) = Unit

                override fun onFftDataCapture(
                    visualizer: Visualizer?,
                    fft: ByteArray?,
                    samplingRate: Int
                ) {
                    if (fft != null) {
                        analyzer.analyze(fft)?.let(onSpectrum)
                    }
                }
            }
            val rate = Visualizer.getMaxCaptureRate().coerceAtMost(20_000)
            check(candidate.setDataCaptureListener(listener, rate, false, true)
                == Visualizer.SUCCESS)
            candidate.enabled = true
            visualizer = candidate
            true
        } catch (e: Exception) {
            Log.w(TAG, "Audio visualizer unavailable; using synthetic animation", e)
            runCatching { candidate?.enabled = false }
            runCatching { candidate?.release() }
            visualizer = null
            false
        } catch (e: LinkageError) {
            Log.w(TAG, "No compatible audio effects implementation", e)
            runCatching { candidate?.release() }
            visualizer = null
            false
        }
    }

    fun stop() {
        val old = visualizer ?: return
        visualizer = null
        runCatching { old.enabled = false }
        runCatching { old.release() }
        analyzer.reset()
    }

    private companion object {
        const val TAG = "CarLauncherAudioSync"
    }
}
