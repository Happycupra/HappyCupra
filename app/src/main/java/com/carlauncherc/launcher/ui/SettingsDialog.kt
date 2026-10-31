package com.carlauncherc.launcher.ui

import android.app.Activity
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import com.carlauncherc.launcher.R
import com.carlauncherc.launcher.core.Format
import com.carlauncherc.launcher.core.DashboardPreset
import com.carlauncherc.launcher.core.Prefs
import com.carlauncherc.launcher.data.AppRepository
import com.carlauncherc.launcher.media.MusicStartLink
import com.carlauncherc.launcher.util.IntentUtil
import kotlinx.coroutines.CoroutineScope

object SettingsDialog {

    fun show(
        activity: Activity,
        repository: AppRepository,
        scope: CoroutineScope,
        onDockReset: () -> Unit,
        onPrefsChanged: () -> Unit,
        onGpsDiagnostics: () -> Unit
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_settings, null, false)

        val presetGroup = view.findViewById<RadioGroup>(R.id.groupDashboardPreset)
        val showNav = view.findViewById<CheckBox>(R.id.checkShowNavCard)
        val showMusic = view.findViewById<CheckBox>(R.id.checkShowMusicCard)
        presetGroup.check(when (Prefs.dashboardPreset) {
            DashboardPreset.CLASSIC -> R.id.dashboardClassic
            DashboardPreset.NAVIGATION -> R.id.dashboardNavigation
            DashboardPreset.MUSIC -> R.id.dashboardMusic
            DashboardPreset.MINIMAL -> R.id.dashboardMinimal
        })
        showNav.isChecked = Prefs.dashboardShowNav
        showMusic.isChecked = Prefs.dashboardShowMusic
        fun updateCardSettings() {
            val enabled = Prefs.dashboardPreset != DashboardPreset.MINIMAL
            showNav.isEnabled = enabled
            showMusic.isEnabled = enabled
        }
        updateCardSettings()
        presetGroup.setOnCheckedChangeListener { _, checkedId ->
            Prefs.dashboardPreset = when (checkedId) {
                R.id.dashboardNavigation -> DashboardPreset.NAVIGATION
                R.id.dashboardMusic -> DashboardPreset.MUSIC
                R.id.dashboardMinimal -> DashboardPreset.MINIMAL
                else -> DashboardPreset.CLASSIC
            }
            updateCardSettings()
            onPrefsChanged()
        }
        showNav.setOnCheckedChangeListener { _, checked ->
            Prefs.dashboardShowNav = checked
            onPrefsChanged()
        }
        showMusic.setOnCheckedChangeListener { _, checked ->
            Prefs.dashboardShowMusic = checked
            onPrefsChanged()
        }
        view.findViewById<Button>(R.id.btnGpsDiagnostics).setOnClickListener {
            onGpsDiagnostics()
        }

        val themeGroup = view.findViewById<RadioGroup>(R.id.groupTheme)
        val unitGroup = view.findViewById<RadioGroup>(R.id.groupUnits)
        val compassGroup = view.findViewById<RadioGroup>(R.id.groupCompass)

        themeGroup.check(
            when (Prefs.themeMode) {
                Prefs.THEME_RED_CARBON -> R.id.themeRedCarbon
                AppCompatDelegate.MODE_NIGHT_NO -> R.id.themeLight
                AppCompatDelegate.MODE_NIGHT_YES -> R.id.themeDark
                else -> R.id.themeSystem
            }
        )
        unitGroup.check(
            if (Prefs.speedUnit == Format.UNIT_MPH) R.id.unitMph else R.id.unitKmh
        )
        compassGroup.check(if (Prefs.compass16Point) R.id.compass16 else R.id.compass8)

        val dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.settings_title)
            .setView(view)
            .setPositiveButton(R.string.action_close, null)
            .create()

        unitGroup.setOnCheckedChangeListener { _, checkedId ->
            Prefs.speedUnit =
                if (checkedId == R.id.unitMph) Format.UNIT_MPH else Format.UNIT_KMH
            onPrefsChanged()
        }

        compassGroup.setOnCheckedChangeListener { _, checkedId ->
            Prefs.compass16Point = checkedId == R.id.compass16
            onPrefsChanged()
        }

        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.themeRedCarbon -> Prefs.THEME_RED_CARBON
                R.id.themeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.themeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            if (mode == Prefs.themeMode) return@setOnCheckedChangeListener
            Prefs.themeMode = mode
            dialog.dismiss()

            if (mode == Prefs.THEME_RED_CARBON) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                activity.recreate()
            } else {
                AppCompatDelegate.setDefaultNightMode(mode)
            }
        }

        // --- clock shortcut ---
        val clockButton = view.findViewById<Button>(R.id.btnClockApp)

        fun renderClockLabel() {
            val stored = Prefs.clockPackage
            val label = if (stored == null) {
                activity.getString(R.string.settings_clock_none)
            } else {
                val pkg = IntentUtil.packageOf(stored)
                repository.findByPackage(pkg)?.label ?: pkg
            }
            clockButton.text = activity.getString(R.string.settings_clock_app, label)
        }
        renderClockLabel()

        clockButton.setOnClickListener {
            AppPicker.show(activity, repository, scope, R.string.pick_clock_app) { entry ->
                Prefs.clockPackage = entry.component.flattenToShortString()
                renderClockLabel()
                onPrefsChanged()
            }
        }

        // --- music at startup ---
        val musicButton = view.findViewById<Button>(R.id.btnMusicApp)
        val musicAutostart = view.findViewById<CheckBox>(R.id.checkMusicAutostart)
        val musicAutoplay = view.findViewById<CheckBox>(R.id.checkMusicAutoplay)

        fun renderMusicLabel() {
            val stored = Prefs.musicPackage
            val label = if (stored == null) {
                activity.getString(R.string.settings_music_none)
            } else {
                val pkg = IntentUtil.packageOf(stored)
                repository.findByPackage(pkg)?.label ?: pkg
            }
            musicButton.text = activity.getString(R.string.settings_music_app, label)
        }
        renderMusicLabel()

        musicAutostart.isChecked = Prefs.musicAutoStart
        musicAutoplay.isChecked = Prefs.musicAutoPlay
        musicAutoplay.isEnabled = Prefs.musicAutoStart

        musicButton.setOnClickListener {
            AppPicker.show(activity, repository, scope, R.string.pick_music_app) { entry ->
                Prefs.musicPackage = entry.component.flattenToShortString()
                renderMusicLabel()
                onPrefsChanged()
            }
        }
        musicAutostart.setOnCheckedChangeListener { _, checked ->
            Prefs.musicAutoStart = checked
            musicAutoplay.isEnabled = checked
        }
        musicAutoplay.setOnCheckedChangeListener { _, checked ->
            Prefs.musicAutoPlay = checked
        }

        // Music visualization selection is saved and applies to the dashboard instantly.
        val visualizerGroup = view.findViewById<RadioGroup>(R.id.groupMusicVisualizer)
        visualizerGroup.check(
            when (Prefs.musicVisualizerMode) {
                Prefs.VISUALIZER_DANCER -> R.id.visualizerDancer
                Prefs.VISUALIZER_DUO_SYNC -> R.id.visualizerDuoSync
                Prefs.VISUALIZER_DUO_RANDOM -> R.id.visualizerDuoRandom
                else -> R.id.visualizerEqualizer
            }
        )
        visualizerGroup.setOnCheckedChangeListener { _, selected ->
            Prefs.musicVisualizerMode = when (selected) {
                R.id.visualizerDancer -> Prefs.VISUALIZER_DANCER
                R.id.visualizerDuoSync -> Prefs.VISUALIZER_DUO_SYNC
                R.id.visualizerDuoRandom -> Prefs.VISUALIZER_DUO_RANDOM
                else -> Prefs.VISUALIZER_EQUALIZER
            }
            onPrefsChanged()
        }

        val realAudioSync = view.findViewById<CheckBox>(R.id.checkRealAudioSync)
        realAudioSync.isChecked = Prefs.musicRealAudioSync
        realAudioSync.setOnCheckedChangeListener { _, enabled ->
            Prefs.musicRealAudioSync = enabled
            onPrefsChanged() // HomeActivity requests RECORD_AUDIO only on explicit opt-in.
        }

        // Starting a saved link is a YMusic v4 feature; resume uses its own Play Again history.
        val startGroup = view.findViewById<RadioGroup>(R.id.groupMusicStartMode)
        val savedLink = view.findViewById<TextView>(R.id.textMusicStartLink)
        val clearLink = view.findViewById<Button>(R.id.btnClearMusicStartLink)
        fun renderStartLink() {
            val link = MusicStartLink.parse(Prefs.musicStartLink)
            savedLink.text = if (link == null) activity.getString(R.string.music_start_link_none)
                else activity.getString(R.string.music_start_link_saved, link.url)
            clearLink.isEnabled = link != null
        }
        renderStartLink()
        startGroup.check(when (Prefs.musicStartMode) {
            Prefs.MUSIC_START_ONCE -> R.id.musicStartOnce
            Prefs.MUSIC_START_ALWAYS -> R.id.musicStartAlways
            else -> R.id.musicResumeLast
        })
        startGroup.setOnCheckedChangeListener { _, checked ->
            val mode = when (checked) {
                R.id.musicStartOnce -> Prefs.MUSIC_START_ONCE
                R.id.musicStartAlways -> Prefs.MUSIC_START_ALWAYS
                else -> Prefs.MUSIC_START_RESUME
            }
            if (mode != Prefs.MUSIC_START_RESUME && MusicStartLink.parse(Prefs.musicStartLink) == null) {
                Toast.makeText(activity, R.string.music_start_link_none, Toast.LENGTH_SHORT).show()
                startGroup.check(R.id.musicResumeLast)
                return@setOnCheckedChangeListener
            }
            if (mode == Prefs.MUSIC_START_ONCE && Prefs.musicStartMode != mode) {
                Prefs.musicStartLinkUsed = false
            }
            Prefs.musicStartMode = mode
        }
        clearLink.setOnClickListener {
            Prefs.musicStartLink = null
            Prefs.musicStartLinkUsed = false
            Prefs.musicStartMode = Prefs.MUSIC_START_RESUME
            startGroup.check(R.id.musicResumeLast)
            renderStartLink()
        }

        val mediaAccessButton = view.findViewById<Button>(R.id.btnMediaAccess)
        val mediaAccessEnabled =
            NotificationManagerCompat.getEnabledListenerPackages(activity)
                .contains(activity.packageName)
        mediaAccessButton.text = activity.getString(
            if (mediaAccessEnabled) R.string.settings_media_access_enabled
            else R.string.settings_media_access_enable
        )
        mediaAccessButton.setOnClickListener {
            IntentUtil.openNotificationListenerSettings(activity)
        }

        // --- dashcam / DVR ---
        val dashcamButton = view.findViewById<Button>(R.id.btnDashcamApp)
        val autostart = view.findViewById<CheckBox>(R.id.checkDashcamAutostart)
        val returnHome = view.findViewById<CheckBox>(R.id.checkDashcamReturn)

        fun renderDashcamLabel() {
            val stored = Prefs.dashcamPackage
            val label = if (stored == null) {
                activity.getString(R.string.settings_dashcam_none)
            } else {
                val pkg = IntentUtil.packageOf(stored)
                repository.findByPackage(pkg)?.label ?: pkg
            }
            dashcamButton.text = activity.getString(R.string.settings_dashcam_app, label)
        }
        renderDashcamLabel()

        autostart.isChecked = Prefs.dashcamAutoStart
        returnHome.isChecked = Prefs.dashcamReturnHome
        returnHome.isEnabled = Prefs.dashcamAutoStart

        dashcamButton.setOnClickListener {
            AppPicker.show(activity, repository, scope, R.string.pick_dashcam_app) { entry ->
                Prefs.dashcamPackage = entry.component.flattenToShortString()
                renderDashcamLabel()
            }
        }
        autostart.setOnCheckedChangeListener { _, checked ->
            Prefs.dashcamAutoStart = checked
            returnHome.isEnabled = checked
        }
        returnHome.setOnCheckedChangeListener { _, checked ->
            Prefs.dashcamReturnHome = checked
        }

        view.findViewById<Button>(R.id.btnResetDock).setOnClickListener {
            onDockReset()
            Toast.makeText(activity, R.string.dock_reset, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.btnSetHomeApp).setOnClickListener {
            IntentUtil.openHomeSettings(activity)
        }

        dialog.show()
    }
}
