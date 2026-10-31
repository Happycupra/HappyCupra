package com.carlauncherc.launcher.media

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.carlauncherc.launcher.R
import com.carlauncherc.launcher.core.Prefs

/** Separate activity so a share never resets the HOME task or launches the music player. */
class MusicShareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = if (intent?.action == Intent.ACTION_SEND) {
            MusicStartLink.parse(intent.getStringExtra(Intent.EXTRA_TEXT))
        } else null
        if (shared == null) {
            Toast.makeText(this, R.string.music_share_no_link, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.music_share_title)
            .setMessage(
                if (shared.kind == MusicStartLink.Kind.TRACK) R.string.music_share_track
                else R.string.music_share_playlist
            )
            .setPositiveButton(R.string.music_share_save) { _, _ ->
                Prefs.musicStartLink = shared.url
                Prefs.musicStartLinkUsed = false
                Prefs.musicStartMode = Prefs.MUSIC_START_ONCE
                Prefs.musicAutoStart = true
                Prefs.musicAutoPlay = true

                val installedV4 = listOf(YMusicPackages.V4, YMusicPackages.V4_DEBUG)
                    .firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }
                if (installedV4 != null) Prefs.musicPackage = installedV4

                Toast.makeText(
                    this,
                    if (installedV4 != null) R.string.music_share_saved
                    else R.string.music_share_no_ymusic,
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }
}
