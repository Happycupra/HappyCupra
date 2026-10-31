package com.carlauncherc.launcher

import com.carlauncherc.launcher.media.MusicStartLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicStartLinkTest {
    @Test fun findsSharedYoutubeSong() {
        val parsed = MusicStartLink.parse("Listen to this https://youtu.be/abcdefghijk?si=abc")
        assertEquals(MusicStartLink.Kind.TRACK, parsed?.kind)
        assertEquals("https://youtu.be/abcdefghijk?si=abc", parsed?.url)
    }

    @Test fun videoWithPlaylistContextIsStillPlayableTrack() {
        assertEquals(
            MusicStartLink.Kind.TRACK,
            MusicStartLink.parse("https://music.youtube.com/watch?v=abc&list=PLxyz")?.kind
        )
    }

    @Test fun recognisesPlaylistOnlyLink() {
        assertEquals(
            MusicStartLink.Kind.PLAYLIST,
            MusicStartLink.parse("https://www.youtube.com/playlist?list=PLxyz")?.kind
        )
    }

    @Test fun rejectsUnrelatedAndSpoofedLinks() {
        assertNull(MusicStartLink.parse("https://youtube.com.example.org/watch?v=abc"))
        assertNull(MusicStartLink.parse("https://open.spotify.com/playlist/123"))
        assertNull(MusicStartLink.parse("javascript:alert(1)"))
        assertNull(MusicStartLink.parse("https://youtube.com/channel/example"))
    }

    @Test fun onceAndAlwaysModesBehaveAsAdvertised() {
        assertTrue(MusicStartLink.shouldOpenLink("once", false))
        assertFalse(MusicStartLink.shouldOpenLink("once", true))
        assertTrue(MusicStartLink.shouldOpenLink("always", true))
        assertFalse(MusicStartLink.shouldOpenLink("resume", false))
    }
}
