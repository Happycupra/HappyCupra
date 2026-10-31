package com.carlauncherc.launcher

import com.carlauncherc.launcher.media.AudioSpectrumAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioSpectrumAnalyzerTest {
    @Test fun silenceDoesNotAppearAsMusic() {
        assertNull(AudioSpectrumAnalyzer().analyze(ByteArray(512)))
    }

    @Test fun shortAndInvalidFramesFailSafely() {
        val analyzer = AudioSpectrumAnalyzer()
        assertNull(analyzer.analyze(byteArrayOf(1, 2, 3, 4)))
        assertNull(analyzer.analyze(byteArrayOf()))
    }

    @Test fun detectsRealFftLowFrequencyBins() {
        val fft = ByteArray(512)
        fft[2] = 90
        fft[3] = 70
        fft[4] = 80
        fft[5] = 20
        val frame = AudioSpectrumAnalyzer().analyze(fft)
        assertNotNull(frame)
        assertEquals(32, frame!!.bands.size)
        assertTrue(frame.bass > 0f)
        assertTrue(frame.bands.first() > frame.bands.last())
        assertTrue(frame.bands.all { it in 0f..1f })
    }

    @Test fun capturesHighFrequenciesSeparately() {
        val fft = ByteArray(512)
        fft[2 * 190] = 100
        fft[2 * 190 + 1] = 50
        val frame = AudioSpectrumAnalyzer().analyze(fft)
        assertNotNull(frame)
        assertTrue(frame!!.bands.last() > frame.bands.first())
    }

    @Test fun resetKeepsValidFramesResponsive() {
        val analyzer = AudioSpectrumAnalyzer()
        val loud = ByteArray(256)
        loud[2] = 90
        assertNotNull(analyzer.analyze(loud))
        analyzer.reset()
        val soft = ByteArray(256)
        soft[2] = 30
        val frame = analyzer.analyze(soft)
        assertNotNull(frame)
        assertTrue(frame!!.bass > 0.2f)
    }
}
