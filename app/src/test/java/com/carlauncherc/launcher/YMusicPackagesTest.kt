package com.carlauncherc.launcher

import com.carlauncherc.launcher.media.YMusicPackages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YMusicPackagesTest {
    @Test fun recognisesV4AndLegacyPackages() {
        assertTrue(YMusicPackages.isYMusic(YMusicPackages.V4))
        assertTrue(YMusicPackages.isYMusic(YMusicPackages.V4_DEBUG))
        assertTrue(YMusicPackages.isYMusic(YMusicPackages.LEGACY))
        assertFalse(YMusicPackages.isYMusic("com.spotify.music"))
        assertFalse(YMusicPackages.isYMusic(null))
    }

    @Test fun v4HelperDoesNotMistakeLegacyForV4() {
        assertTrue(YMusicPackages.isV4(YMusicPackages.V4))
        assertTrue(YMusicPackages.isV4(YMusicPackages.V4_DEBUG))
        assertFalse(YMusicPackages.isV4(YMusicPackages.LEGACY))
        assertFalse(YMusicPackages.isV4(null))
    }

    @Test fun followsSelectedSessionWhenActive() {
        assertEquals(
            YMusicPackages.LEGACY,
            YMusicPackages.selectSessionPackage(
                listOf(YMusicPackages.V4, YMusicPackages.LEGACY), YMusicPackages.LEGACY
            )
        )
    }

    @Test fun fallsBackToV4IfLegacyIsNotActive() {
        assertEquals(
            YMusicPackages.V4,
            YMusicPackages.selectSessionPackage(listOf(YMusicPackages.V4), YMusicPackages.LEGACY)
        )
    }

    @Test fun ignoresUnrelatedMediaSessions() {
        assertNull(
            YMusicPackages.selectSessionPackage(listOf("com.spotify.music"), "com.spotify.music")
        )
    }
}
