package com.carlauncherc.launcher.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.content.Intent
import android.app.role.RoleManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.content.res.ColorStateList
import android.widget.Toast
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Space
import android.util.TypedValue
import kotlin.math.abs
import androidx.core.app.NotificationManagerCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.carlauncherc.launcher.BuildConfig
import com.carlauncherc.launcher.CarLauncherApp
import com.carlauncherc.launcher.R
import com.carlauncherc.launcher.core.Format
import com.carlauncherc.launcher.core.DashboardLayouts
import com.carlauncherc.launcher.core.Prefs
import com.carlauncherc.launcher.databinding.ActivityHomeBinding
import com.carlauncherc.launcher.location.HeadingSource
import com.carlauncherc.launcher.location.VehicleState
import com.carlauncherc.launcher.media.MusicStartLink
import com.carlauncherc.launcher.media.RealAudioVisualizer
import com.carlauncherc.launcher.media.YMusicMediaBridge
import com.carlauncherc.launcher.media.YMusicPackages
import com.carlauncherc.launcher.update.ApkInstaller
import com.carlauncherc.launcher.update.UpdateState
import com.carlauncherc.launcher.util.IntentUtil
import kotlinx.coroutines.launch

/**
 * The HOME activity.
 *
 * Nothing heavy or fallible happens in onCreate: the app list loads asynchronously and every
 * pref decode falls back to defaults. A launcher that throws here on a head unit with no
 * second home app leaves the device recoverable only over adb.
 */
class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var dock: DockController
    private lateinit var drawer: DrawerController
    private lateinit var cards: QuickCardsController

    private val app: CarLauncherApp get() = application as CarLauncherApp

    private val liveAudio = RealAudioVisualizer { spectrum ->
        // Listener callbacks may run on a framework audio thread. All View state lives on UI.
        if (::binding.isInitialized) binding.playbackEqualizer.post {
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                Prefs.musicRealAudioSync) {
                binding.playbackEqualizer.setAudioSpectrum(spectrum)
            }
        }
    }
    private var audioCaptureUnsupported = false
    private var lastCardinal: String? = null
    private var leftAtElapsedMs = 0L
    private var firstRunSetupActive = false
    private var firstRunLocationRequested = false
    private var firstRunMediaAccessRequested = false

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.onPermissionResult()
        if (firstRunSetupActive) continueFirstRunSetup()
    }

    private val realAudioPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Prefs.musicRealAudioSync = false
            toast(getString(R.string.music_audio_sync_permission_denied))
        }
        audioCaptureUnsupported = false
        YMusicMediaBridge.nowPlaying.value.let { info ->
            renderNowPlaying(info.title, info.artist, info.isPlaying,
                info.artwork, info.durationMs, info.positionMs)
        }
    }

    private val mediaAccessSetup = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (firstRunSetupActive) continueFirstRunSetup()
    }

    private val homeRoleSetup = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        finishFirstRunSetup()
    }

    private val unknownSources = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // The user may have declined; re-check rather than assuming.
        val state = app.updateRepository.state.value
        if (state is UpdateState.ReadyToInstall && ApkInstaller.canInstall(this)) {
            ApkInstaller.install(this, state.file)?.let { toast(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.themeMode == Prefs.THEME_RED_CARBON) {
            setTheme(R.style.Theme_CarLauncher_RedCarbon_Home)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyThemeVisuals()

        val repository = app.appRepository

        dock = DockController(this, binding.dockStrip, repository, lifecycleScope)
        drawer = DrawerController(this, binding, repository, lifecycleScope) { entry ->
            dock.pin(entry)
        }
        cards = QuickCardsController(this, binding, repository, lifecycleScope)

        dock.bind()
        drawer.bind()
        cards.bind()
        applyDashboardLayout()

        bindDashboardClicks()
        bindCompassThemeCycle()
        bindClockShortcut()
        bindDockActions()
        bindMusicControls()
        configureResponsiveGauges()
        bindBackBehaviour()
        observe()

        if (!Prefs.firstRunDone) {
            binding.root.post { startFirstRunSetup() }
        } else {
            binding.root.post { maybeShowVersionLog() }
        }
    }

    // ------------------------------------------------------------------ theme

    private fun applyThemeVisuals() {
        if (Prefs.themeMode != Prefs.THEME_RED_CARBON) return

        binding.root.setBackgroundResource(R.drawable.bg_carbon)
        binding.dashboard.setBackgroundResource(R.drawable.bg_carbon)

        binding.gaugeSpeed.setBackgroundResource(R.drawable.bg_gauge_red_carbon)
        binding.gaugeClock.setBackgroundResource(R.drawable.bg_gauge_red_carbon)
        binding.gaugeCompass.setBackgroundResource(R.drawable.bg_gauge_red_carbon)

        binding.cardNav.setBackgroundResource(R.drawable.bg_card_red_carbon)
        binding.cardMusic.setBackgroundResource(R.drawable.bg_card_red_carbon)
        binding.dockStrip.root.setBackgroundResource(R.drawable.bg_dock_red_carbon)

        val red = getColor(R.color.carbon_red_bright)
        val softRed = getColor(R.color.carbon_red)
        binding.textSpeed.setTextColor(red)
        binding.clockTime.setTextColor(red)
        binding.clockDay.setTextColor(softRed)
        binding.titleNav.setTextColor(red)
        binding.titleMusic.setTextColor(red)
        binding.playbackEqualizer.setBarColor(red)
        binding.musicProgress.progressTintList = ColorStateList.valueOf(red)
        binding.musicProgress.progressBackgroundTintList = ColorStateList.valueOf(
            getColor(R.color.carbon_red_dark)
        )
    }

    // ------------------------------------------------------------------ wiring

    private fun bindDashboardClicks() {
        binding.gaugeSpeed.setOnLongClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            GpsDiagnosticsDialog.show(this, viewModel)
            true
        }
        binding.gaugeSpeed.setOnClickListener { view ->
            val state = viewModel.vehicle.value
            when {
                !state.permissionGranted -> requestLocation()
                !state.gpsEnabled -> IntentUtil.openLocationSettings(this)
                else -> {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                    viewModel.toggleSpeedUnit()
                    render(viewModel.vehicle.value)
                }
            }
        }
    }

    private fun bindCompassThemeCycle() {
        binding.gaugeCompass.isClickable = true
        binding.gaugeCompass.isFocusable = true

        var switchTriggered = false
        val switchTheme = Runnable {
            switchTriggered = true
            binding.gaugeCompass.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            cycleToNextTheme()
        }

        binding.gaugeCompass.setOnClickListener {
            toast(getString(R.string.compass_theme_hold_hint))
        }

        binding.gaugeCompass.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    switchTriggered = false
                    view.postDelayed(switchTheme, THEME_HOLD_MS)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(switchTheme)
                    if (!switchTriggered) view.performClick()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    view.removeCallbacks(switchTheme)
                    true
                }
                else -> true
            }
        }
    }

    private fun cycleToNextTheme() {
        val next = when (Prefs.themeMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> Prefs.THEME_RED_CARBON
            Prefs.THEME_RED_CARBON -> AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            else -> AppCompatDelegate.MODE_NIGHT_YES
        }

        Prefs.themeMode = next
        toast(
            getString(
                R.string.theme_switched,
                getString(
                    when (next) {
                        Prefs.THEME_RED_CARBON -> R.string.theme_red_carbon
                        AppCompatDelegate.MODE_NIGHT_NO -> R.string.theme_light
                        AppCompatDelegate.MODE_NIGHT_YES -> R.string.theme_dark
                        else -> R.string.theme_system
                    }
                )
            )
        )

        val effective = if (next == Prefs.THEME_RED_CARBON) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            next
        }

        if (AppCompatDelegate.getDefaultNightMode() == effective) {
            recreate()
        } else {
            AppCompatDelegate.setDefaultNightMode(effective)
        }
    }

    private fun bindClockShortcut() {
        binding.gaugeClock.isClickable = true
        binding.gaugeClock.isFocusable = true

        binding.gaugeClock.setOnClickListener { view ->
            val stored = Prefs.clockPackage
            if (stored.isNullOrBlank()) {
                pickClockShortcut()
                return@setOnClickListener
            }
            if (!IntentUtil.launchStored(this, stored, view)) {
                Prefs.clockPackage = null
                toast(getString(R.string.app_not_installed))
                pickClockShortcut()
            }
        }

        binding.gaugeClock.setOnLongClickListener {
            pickClockShortcut()
            true
        }
    }

    private fun pickClockShortcut() {
        AppPicker.show(this, app.appRepository, lifecycleScope, R.string.pick_clock_app) { entry ->
            Prefs.clockPackage = entry.component.flattenToShortString()
            toast(getString(R.string.clock_shortcut_saved, entry.label))
        }
    }

    private fun bindDockActions() {
        binding.dockStrip.btnAllApps.setOnClickListener { drawer.open() }
        // Tap goes to Android's own settings, long-press to this launcher's own settings -
        // so the common case stays one tap and the dock keeps its three fixed actions.
        binding.dockStrip.btnSettings.setOnClickListener {
            if (!IntentUtil.openSystemSettings(this)) toast(getString(R.string.could_not_launch))
        }
        binding.dockStrip.btnSettings.setOnLongClickListener {
            openSettings()
            true
        }
        binding.dockStrip.btnAbout.setOnClickListener { openAbout() }
    }

    private fun bindMusicControls() {
        binding.btnMusicPrevious.setOnClickListener {
            if (!YMusicMediaBridge.previous()) {
                dispatchYMusicMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }
        }
        binding.btnMusicPlayPause.setOnClickListener {
            if (!YMusicMediaBridge.togglePlayPause()) {
                dispatchYMusicMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
        }
        binding.btnMusicNext.setOnClickListener {
            if (!YMusicMediaBridge.next()) {
                dispatchYMusicMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            }
        }
    }

    private fun renderNowPlaying(
        title: String,
        artist: String,
        isPlaying: Boolean,
        artwork: android.graphics.Bitmap?,
        durationMs: Long,
        positionMs: Long
    ) {
        val selectedPackage = Prefs.musicPackage?.let(IntentUtil::packageOf)
        // Show an active v4 session even when uninstalling legacy YMusic cleared the old shortcut.
        val isYMusic = YMusicPackages.isYMusic(selectedPackage) ||
            (selectedPackage == null && YMusicPackages.isYMusic(YMusicMediaBridge.sessionPackageName))
        binding.musicControls.visibility = if (isYMusic) View.VISIBLE else View.GONE
        // Hide the redundant "Music / app name" heading when a live player is shown:
        // this recovers two text rows, avoiding collisions with the transport buttons.
        binding.titleMusic.visibility = if (isYMusic) View.GONE else View.VISIBLE
        binding.subtitleMusic.visibility = if (isYMusic) View.GONE else View.VISIBLE
        binding.albumArtContainer.visibility = if (isYMusic) View.VISIBLE else View.GONE
        val visualizerMode = Prefs.musicVisualizerMode
        binding.playbackEqualizer.setMode(visualizerMode)
        // Dancing characters still rest on the stage without an active music session.
        binding.playbackEqualizer.visibility = if (
            isYMusic || visualizerMode != Prefs.VISUALIZER_EQUALIZER
        ) View.VISIBLE else View.GONE
        binding.playbackEqualizer.setPlaying(isYMusic && isPlaying)
        syncRealAudioVisualizer(isYMusic && isPlaying)

        if (!isYMusic) {
            binding.textNowPlayingTitle.visibility = View.GONE
            binding.textNowPlayingArtist.visibility = View.GONE
            binding.imageAlbumArt.visibility = View.GONE
            binding.musicProgress.visibility = View.GONE
            binding.textElapsed.visibility = View.GONE
            binding.textDuration.visibility = View.GONE
            return
        }

        binding.textNowPlayingTitle.visibility = View.VISIBLE
        binding.textNowPlayingArtist.visibility = View.VISIBLE
        binding.imageAlbumArt.visibility = View.VISIBLE
        binding.musicProgress.visibility = View.VISIBLE
        binding.textElapsed.visibility = View.VISIBLE
        binding.textDuration.visibility = View.VISIBLE

        val accessEnabled =
            NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

        binding.textNowPlayingTitle.text = when {
            title.isNotBlank() -> title
            !accessEnabled -> getString(R.string.media_access_required)
            else -> getString(R.string.media_title_unavailable)
        }
        binding.textNowPlayingArtist.text = artist

        if (artwork != null) {
            binding.imageAlbumArt.setPadding(0, 0, 0, 0)
            binding.imageAlbumArt.setImageBitmap(artwork)
        } else {
            binding.imageAlbumArt.setPadding(24, 24, 24, 24)
            binding.imageAlbumArt.setImageResource(R.drawable.ic_music)
        }

        val safeDuration = durationMs.coerceAtLeast(0L)
        val safePosition = positionMs.coerceAtLeast(0L).let {
            if (safeDuration > 0L) it.coerceAtMost(safeDuration) else it
        }
        binding.musicProgress.progress =
            if (safeDuration > 0L) ((safePosition * 1000L) / safeDuration).toInt() else 0
        binding.textElapsed.text = formatMediaTime(safePosition)
        binding.textDuration.text = formatMediaTime(safeDuration)

        binding.btnMusicPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
        )
    }

    private fun hasRealAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Use device output-mix FFT only while the launcher is RESUMED, YMusic is PLAYING,
     * Equalizer is selected and the user opted in. No microphone capture is constructed.
     * A broken/unsupported head unit falls back to the existing synthetic animation.
     */
    private fun syncRealAudioVisualizer(ymusicPlaying: Boolean) {
        val shouldCapture = Prefs.musicRealAudioSync &&
            Prefs.musicVisualizerMode == Prefs.VISUALIZER_EQUALIZER &&
            ymusicPlaying && hasRealAudioPermission() &&
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)

        if (!shouldCapture) {
            liveAudio.stop()
            binding.playbackEqualizer.clearAudioSpectrum()
            audioCaptureUnsupported = false
            return
        }
        if (liveAudio.isRunning || audioCaptureUnsupported) return
        if (!liveAudio.start()) {
            audioCaptureUnsupported = true
            toast(getString(R.string.music_audio_sync_unsupported))
        }
    }

    private fun formatMediaTime(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSeconds = ms / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
    }

    /**
     * Android layouts use dp/sp, not physical video pixels. Resize the two largest gauge
     * strings from the AVAILABLE measured card dimensions so 1024x600, 1280x720 and
     * 1920x1080 panels (and vendor density/font scaling) cannot draw across cards.
     * The baseline dimen is never exceeded, so fonts stay consistent on large screens.
     */
    /**
     * Rebuild only the container order; the original View instances are retained, so
     * compass sensors, TextClock, music callbacks and click listeners keep working.
     * Quick cards may be independently hidden, or both hidden in Focus preset.
     */
    private fun applyDashboardLayout() {
        val arrangement = DashboardLayouts.arrange(
            Prefs.dashboardPreset, Prefs.dashboardShowNav, Prefs.dashboardShowMusic
        )
        val gauges = mapOf(
            "speed" to binding.gaugeSpeed,
            "clock" to binding.gaugeClock,
            "compass" to binding.gaugeCompass
        )
        val gap = resources.getDimensionPixelSize(R.dimen.gap)
        val row = binding.dashboardGaugeRow
        row.removeAllViews()
        arrangement.gaugeOrder.forEachIndexed { index, id ->
            if (index > 0) row.addView(
                Space(this), LinearLayout.LayoutParams(gap, LinearLayout.LayoutParams.MATCH_PARENT)
            )
            row.addView(
                gauges.getValue(id),
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT,
                    arrangement.gaugeWeights[index])
            )
        }

        val quick = binding.quickCardsRow
        quick.removeAllViews()
        val items = listOfNotNull(
            binding.cardNav.takeIf { Prefs.dashboardShowNav && arrangement.quickCardsVisible },
            binding.cardMusic.takeIf { Prefs.dashboardShowMusic && arrangement.quickCardsVisible }
        )
        items.forEachIndexed { index, card ->
            if (index > 0) quick.addView(
                Space(this), LinearLayout.LayoutParams(gap, LinearLayout.LayoutParams.MATCH_PARENT)
            )
            val weight = if (items.size == 1) 1f
                else if (card === binding.cardNav) arrangement.navWeight
                else arrangement.musicWeight
            quick.addView(card, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, weight
            ))
        }
        val visible = arrangement.quickCardsVisible
        quick.visibility = if (visible) View.VISIBLE else View.GONE
        binding.quickCardsSpacer.visibility = if (visible) View.VISIBLE else View.GONE
        row.requestLayout()
    }

    private fun configureResponsiveGauges() {
        binding.dashboard.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // A 1080-pixel head unit may advertise only ~360dp height at 3x
            // vendor density. Keep enough space for the gauges instead of letting
            // a fixed-height quick-card row push their labels on top of each other.
            val available = binding.dashboard.height -
                binding.dashboard.paddingTop - binding.dashboard.paddingBottom
            val density = resources.displayMetrics.density
            val maxCard = (available -
                (110f * density).toInt() -
                resources.getDimensionPixelSize(R.dimen.gap))
                .coerceAtLeast((132f * density).toInt())
            val desired = minOf(resources.getDimensionPixelSize(R.dimen.card_height), maxCard)
            if (binding.quickCardsRow.layoutParams.height != desired) {
                binding.quickCardsRow.layoutParams =
                    binding.quickCardsRow.layoutParams.apply { height = desired }
            }
        }
        binding.gaugeSpeed.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            fitGaugeText(
                binding.textSpeed, binding.gaugeSpeed, R.dimen.speed_text,
                "888", 0.58f
            )
        }
        binding.gaugeClock.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            fitGaugeText(
                binding.clockTime, binding.gaugeClock, R.dimen.clock_text,
                "23:59", 0.46f
            )
        }
        binding.musicInfoColumn.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            // Text may be enlarged in system font settings. Keep both lines inside
            // the remaining space above progress, timer and playback controls.
            val room = binding.musicInfoColumn.height -
                binding.musicControls.height -
                binding.musicProgress.height -
                binding.textElapsed.height -
                (18f * resources.displayMetrics.density).toInt()
            if (room > 0) {
                val titleBase = resources.getDimension(R.dimen.music_title_text)
                val artistBase = resources.getDimension(R.dimen.music_artist_text)
                val ratio = (room.toFloat() / (titleBase + artistBase))
                    .coerceIn(0.52f, 1f)
                setTextPxIfNeeded(binding.textNowPlayingTitle, titleBase * ratio)
                setTextPxIfNeeded(binding.textNowPlayingArtist, artistBase * ratio)
            }
        }
    }

    private fun fitGaugeText(
        text: TextView,
        container: View,
        baseRes: Int,
        widestExample: String,
        heightRatio: Float
    ) {
        if (container.width <= 0 || container.height <= 0) return
        val baselinePx = resources.getDimension(baseRes)
        val samplePaint = android.text.TextPaint(text.paint).apply { textSize = baselinePx }
        val textWidth = samplePaint.measureText(widestExample).coerceAtLeast(1f)
        val availWidth = (container.width - container.paddingLeft - container.paddingRight -
            (8f * resources.displayMetrics.density)).coerceAtLeast(1f)
        val desiredPx = minOf(
            baselinePx,
            baselinePx * availWidth / textWidth,
            container.height * heightRatio
        ).coerceAtLeast(20f * resources.displayMetrics.density)
        setTextPxIfNeeded(text, desiredPx)
    }

    private fun setTextPxIfNeeded(text: TextView, px: Float) {
        if (abs(text.textSize - px) > 0.75f) {
            text.setTextSize(TypedValue.COMPLEX_UNIT_PX, px)
        }
    }

    /** A launcher must never finish itself - on some ROMs that leaves a blank screen. */
    private fun bindBackBehaviour() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawer.isOpen) drawer.close()
            }
        })
    }

    private fun observe() {
        val repository = app.appRepository

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.apps.collect { apps ->
                    drawer.submitApps(apps)
                    dock.bind()
                    cards.refreshLabels()
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.packageRemoved.collect { packageName ->
                    dock.onPackageRemoved(packageName)
                    cards.onPackageRemoved(packageName)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.vehicle.collect { render(it) }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                app.updateRepository.badgeVisible.collect { visible ->
                    binding.dockStrip.updateBadge.visibility =
                        if (visible) View.VISIBLE else View.GONE
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                YMusicMediaBridge.nowPlaying.collect { info ->
                    renderNowPlaying(
                        info.title,
                        info.artist,
                        info.isPlaying,
                        info.artwork,
                        info.durationMs,
                        info.positionMs
                    )
                }
            }
        }
    }

    // ------------------------------------------------------------- dashboard render

    private fun render(state: VehicleState) {
        val unit = viewModel.speedUnit.value

        binding.textSpeed.text = Format.speedText(unit, state.speedMps, state.hasFix)
        binding.textSpeedUnit.text = Format.unitLabel(unit)
        binding.textSpeedStatus.setText(
            when {
                !state.permissionGranted -> R.string.gps_permission_needed
                !state.gpsEnabled -> R.string.gps_disabled
                !state.gpsFixPresent -> R.string.gps_acquiring
                !state.hasFix -> R.string.gps_speed_unavailable
                else -> R.string.gps_ready
            }
        )

        val heading = state.headingDeg
        if (heading == null) {
            lastCardinal = null
            binding.textCardinal.setText(R.string.speed_placeholder)
            binding.textDegrees.text = ""
            binding.textHeadingSource.setText(
                if (viewModel.compassAvailable) R.string.heading else R.string.no_compass
            )
        } else {
            val cardinal = Format.cardinalWithHysteresis(
                heading, lastCardinal, Prefs.compass16Point
            )
            lastCardinal = cardinal
            binding.textCardinal.text = cardinal
            binding.textDegrees.text = Format.degreesText(heading)
            // Showing the source keeps a wrong stationary magnetometer reading from being
            // mistaken for the truth.
            binding.textHeadingSource.setText(
                if (state.headingSource == HeadingSource.GPS) R.string.heading_source_gps
                else R.string.heading_source_mag
            )
        }
    }

    // -------------------------------------------------------------------- dialogs

    private fun openAbout() {
        AboutDialog.show(this, app.updateRepository) { state ->
            // Never put an update prompt in front of a driver who is moving.
            if (viewModel.vehicle.value.speedMps > MOVING_MPS) {
                toast(getString(R.string.update_deferred_while_driving))
                return@show
            }
            val release = when (state) {
                is UpdateState.Available -> state.release
                is UpdateState.ReadyToInstall -> state.release
                else -> return@show
            }
            UpdateDialog.show(this, app.updateRepository, release) {
                toast(getString(R.string.update_unknown_sources))
                unknownSources.launch(ApkInstaller.unknownSourcesIntent(this))
            }
        }
    }

    private fun openSettings() {
        SettingsDialog.show(
            activity = this,
            repository = app.appRepository,
            scope = lifecycleScope,
            onDockReset = { dock.reset() },
            onGpsDiagnostics = { GpsDiagnosticsDialog.show(this, viewModel) },
            onPrefsChanged = {
                applyDashboardLayout()
                viewModel.refreshPrefs()
                lastCardinal = null
                render(viewModel.vehicle.value)
                binding.playbackEqualizer.setMode(Prefs.musicVisualizerMode)
                YMusicMediaBridge.nowPlaying.value.let { info ->
                    renderNowPlaying(info.title, info.artist, info.isPlaying,
                        info.artwork, info.durationMs, info.positionMs)
                }
                // Ask only when the person explicitly enabled real FFT in settings.
                if (Prefs.musicRealAudioSync && !hasRealAudioPermission()) {
                    realAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )
    }

    private fun requestLocation() {
        locationPermission.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // ------------------------------------------------------------ first-run setup

    private fun startFirstRunSetup() {
        if (Prefs.firstRunDone || firstRunSetupActive) return
        firstRunSetupActive = true
        continueFirstRunSetup()
    }

    /**
     * Runs the Android-owned setup prompts one after another:
     * 1) location for speed/heading
     * 2) notification-listener access for YMusic title/transport controls
     * 3) HOME role so CarLauncher C becomes the default launcher
     *
     * Storage is intentionally not requested: this app uses its private app storage and
     * FileProvider for APK updates, so Android 10+ does not require broad storage access.
     */
    private fun continueFirstRunSetup() {
        if (!firstRunSetupActive) return

        val hasFine = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCoarse = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

        if ((!hasFine || !hasCoarse) && !firstRunLocationRequested) {
            firstRunLocationRequested = true
            requestLocation()
            return
        }

        val mediaAccessEnabled =
            NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (!mediaAccessEnabled && !firstRunMediaAccessRequested) {
            firstRunMediaAccessRequested = true
            toast(getString(R.string.first_run_media_access))
            mediaAccessSetup.launch(IntentUtil.notificationListenerSettingsIntent())
            return
        }

        requestHomeRoleOrFinish()
    }

    private fun requestHomeRoleOrFinish() {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        ) {
            toast(getString(R.string.first_run_home_role))
            homeRoleSetup.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
            return
        }

        // Some head-unit ROMs do not expose ROLE_HOME correctly. Fall back to the system's
        // default-home screen, then consider first-run setup complete.
        if (roleManager == null || !roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
            IntentUtil.openHomeSettings(this)
        }
        finishFirstRunSetup()
    }

    private fun finishFirstRunSetup() {
        Prefs.firstRunDone = true
        Prefs.lastShownVersionCode = BuildConfig.VERSION_CODE
        firstRunSetupActive = false
        firstRunLocationRequested = false
        firstRunMediaAccessRequested = false
        toast(getString(R.string.first_run_done))
    }

    private fun maybeShowVersionLog() {
        val currentVersion = BuildConfig.VERSION_CODE
        if (Prefs.lastShownVersionCode >= currentVersion) return

        // Mark before showing so a configuration change cannot display the dialog twice.
        Prefs.lastShownVersionCode = currentVersion

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.changelog_title, BuildConfig.VERSION_NAME))
            .setMessage(R.string.changelog_current)
            .setPositiveButton(R.string.action_close, null)
            .show()
    }

    // ------------------------------------------------------------------ lifecycle

    override fun onStart() {
        super.onStart()
        // Coming back from another app after a while: a driver should never find a stale
        // search box waiting for them.
        if (leftAtElapsedMs > 0L &&
            SystemClock.elapsedRealtime() - leftAtElapsedMs > STALE_RETURN_MS
        ) {
            resetToDashboard()
        }
        app.updateRepository.maybeCheck()
    }

    override fun onResume() {
        super.onResume()
        binding.playbackEqualizer.setActive(true)
        viewModel.start()
        viewModel.refreshPrefs()
        applyDashboardLayout()
        migrateLegacyYMusicPreference()
        cards.refreshLabels()
        render(viewModel.vehicle.value)
        YMusicMediaBridge.nowPlaying.value.let { info ->
            renderNowPlaying(
                info.title,
                info.artist,
                info.isPlaying,
                info.artwork,
                info.durationMs,
                info.positionMs
            )
        }
        maybeAutoStartDashcam()
        maybeAutoStartMusic()
    }

    /** Redirect a saved legacy shortcut to v4 when the old app is no longer installed. */
    private fun migrateLegacyYMusicPreference() {
        val stored = Prefs.musicPackage ?: return
        if (IntentUtil.packageOf(stored) != YMusicPackages.LEGACY ||
            app.appRepository.isInstalled(YMusicPackages.LEGACY)
        ) return
        val replacement = listOf(YMusicPackages.V4, YMusicPackages.V4_DEBUG)
            .firstOrNull(app.appRepository::isInstalled)
        if (replacement != null) Prefs.musicPackage = replacement
    }

    /**
     * Starts the DVR app once per launcher process so its floating overlay comes up on the
     * dashboard. Deliberately delayed: launching anything before the home screen has drawn
     * makes a head unit look like it is booting into the wrong app.
     */
    private fun maybeAutoStartDashcam() {
        if (app.dashcamAutoStartDone || !Prefs.dashcamAutoStart) return
        val stored = Prefs.dashcamPackage ?: return
        app.dashcamAutoStartDone = true

        binding.root.postDelayed({
            if (!IntentUtil.launchStored(this, stored, null)) {
                Prefs.dashcamPackage = null
                toast(getString(R.string.app_not_installed))
                return@postDelayed
            }
            if (Prefs.dashcamReturnHome) {
                // Best effort. Android 10+ restricts background activity starts, so some ROMs
                // will drop this and the user presses HOME once instead.
                binding.root.postDelayed({ returnToDashboard() }, DASHCAM_RETURN_DELAY_MS)
            }
        }, DASHCAM_START_DELAY_MS)
    }


    /**
     * Opens the selected music app once per launcher process and, optionally, sends an explicit
     * MEDIA_PLAY key shortly afterwards. Using PLAY rather than PLAY_PAUSE avoids accidentally
     * pausing a player that already resumed by itself.
     */
    private fun maybeAutoStartMusic() {
        if (app.musicAutoStartDone || app.musicAutoStartInProgress || !Prefs.musicAutoStart) return
        val stored = Prefs.musicPackage ?: return
        val packageName = IntentUtil.packageOf(stored)

        app.musicAutoStartInProgress = true

        if (YMusicPackages.isYMusic(packageName)) {
            if (!app.appRepository.isInstalled(packageName)) {
                app.musicAutoStartInProgress = false
                Prefs.musicPackage = null
                toast(getString(R.string.app_not_installed))
                return
            }
            val saved = MusicStartLink.parse(Prefs.musicStartLink)
            val mode = Prefs.musicStartMode
            val useLink = YMusicPackages.isV4(packageName) && Prefs.musicAutoPlay &&
                saved != null && MusicStartLink.shouldOpenLink(mode, Prefs.musicStartLinkUsed)
            if (useLink) {
                binding.root.postDelayed({ startSavedMusicLink(packageName, saved!!) }, MUSIC_START_DELAY_MS)
            } else if (Prefs.musicAutoPlay) {
                binding.root.postDelayed({ startYMusicWatchdog() }, MUSIC_START_DELAY_MS)
            } else {
                app.musicAutoStartDone = true
                app.musicAutoStartInProgress = false
            }
            return
        }

        binding.root.postDelayed({
            val launched = IntentUtil.launchStored(this, stored, null)
            if (!launched) {
                app.musicAutoStartInProgress = false
                Prefs.musicPackage = null
                toast(getString(R.string.app_not_installed))
                return@postDelayed
            }

            if (Prefs.musicAutoPlay) {
                scheduleMediaPlayRetries(packageName)
            }
            app.musicAutoStartDone = true
            app.musicAutoStartInProgress = false
        }, MUSIC_START_DELAY_MS)
    }

    /** Explicit v4 link support: a video auto-plays; playlist-only links open their list page. */
    private fun startSavedMusicLink(packageName: String, link: MusicStartLink) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link.url))
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val started = runCatching { startActivity(intent); true }.getOrDefault(false)
        app.musicAutoStartDone = started
        app.musicAutoStartInProgress = false
        if (!started) {
            toast(getString(R.string.music_start_link_could_not_open))
            return
        }
        if (Prefs.musicStartMode == Prefs.MUSIC_START_ONCE) Prefs.musicStartLinkUsed = true
        // Link playback starts inside YMusic, not through a premature generic PLAY press:
        // doing both can resurrect an old queue before the new video is resolved.
        // A playlist-only link needs the user to tap Play in YMusic v4.
        if (link.kind == MusicStartLink.Kind.TRACK) {
            binding.root.postDelayed({ returnToDashboard() }, MUSIC_LINK_RETURN_DELAY_MS)
        }
    }

    private fun startYMusicWatchdog() {
        val handler = Handler(Looper.getMainLooper())
        var attempt = 0

        fun finish(success: Boolean) {
            handler.removeCallbacksAndMessages(YMUSIC_WATCHDOG_TOKEN)
            app.musicAutoStartDone = success
            app.musicAutoStartInProgress = false
        }

        val watchdog = object : Runnable {
            override fun run() {
                val info = YMusicMediaBridge.nowPlaying.value
                if (info.isPlaying) {
                    finish(true)
                    return
                }

                if (attempt >= YMUSIC_WATCHDOG_MAX_ATTEMPTS) {
                    finish(false)
                    return
                }

                attempt++

                // Prefer the direct MediaSession when Android has exposed it.
                val sessionCommandSent = YMusicMediaBridge.play()

                // Also wake YMusic's manifest media-button path. This works before the
                // MediaSession exists on many head units and does not bring YMusic to front.
                dispatchYMusicPlay()

                // Every third pass add a generic system PLAY as a second fallback.
                if (!sessionCommandSent && attempt % 3 == 0) {
                    dispatchMediaPlay()
                }

                handler.postAtTime(
                    this,
                    YMUSIC_WATCHDOG_TOKEN,
                    SystemClock.uptimeMillis() + YMUSIC_WATCHDOG_INTERVAL_MS
                )
            }
        }

        handler.postAtTime(watchdog, YMUSIC_WATCHDOG_TOKEN, SystemClock.uptimeMillis())
    }

    /**
     * Media apps often create their MediaSession asynchronously after their activity appears.
     * A single PLAY shortly after launch is therefore unreliable on slower head units.
     *
     * PLAY is intentionally retried instead of PLAY_PAUSE: repeated PLAY commands are harmless
     * once playback has already started, while PLAY_PAUSE could toggle it back off.
     */
    private fun scheduleMediaPlayRetries(packageName: String) {
        val handler = Handler(Looper.getMainLooper())
        MUSIC_PLAY_RETRY_DELAYS_MS.forEach { delayMs ->
            handler.postDelayed({
                if (YMusicPackages.isYMusic(packageName)) {
                    if (!YMusicMediaBridge.play()) {
                        dispatchYMusicPlay()
                    }
                } else {
                    dispatchMediaPlay()
                }
            }, delayMs)
        }
    }

    private fun dispatchYMusicPlay() {
        dispatchYMusicMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    private fun dispatchYMusicMediaKey(keyCode: Int) {
        val targetPackage = YMusicMediaBridge.sessionPackageName
            ?: Prefs.musicPackage?.let(IntentUtil::packageOf)?.takeIf(YMusicPackages::isYMusic)
            ?: YMusicPackages.V4
        val eventTime = SystemClock.uptimeMillis()
        val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

        runCatching {
            sendBroadcast(
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setPackage(targetPackage)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(Intent.EXTRA_KEY_EVENT, down)
            )
            sendBroadcast(
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setPackage(targetPackage)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(Intent.EXTRA_KEY_EVENT, up)
            )
        }
    }

    private fun dispatchMediaPlay() {
        val audio = getSystemService(AudioManager::class.java) ?: return
        val eventTime = SystemClock.uptimeMillis()
        runCatching {
            audio.dispatchMediaKeyEvent(
                KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY, 0)
            )
            audio.dispatchMediaKeyEvent(
                KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY, 0)
            )
        }
    }

    private fun returnToDashboard() {
        val intent = Intent(this, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            // Blocked by the background-activity-start policy; nothing useful to do.
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister the moment anything else comes to the foreground - this process is alive
        // for as long as the head unit is powered.
        viewModel.stop()
        liveAudio.stop()
        binding.playbackEqualizer.clearAudioSpectrum()
        audioCaptureUnsupported = false
        binding.playbackEqualizer.setActive(false)
        leftAtElapsedMs = SystemClock.elapsedRealtime()
    }

    /** HOME pressed while already home: singleTask re-delivers the intent here. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) resetToDashboard()
    }

    private fun resetToDashboard() {
        drawer.reset()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private companion object {
        /** ~5 km/h. */
        const val MOVING_MPS = 1.4f
        const val STALE_RETURN_MS = 30_000L
        const val DASHCAM_START_DELAY_MS = 1_500L
        const val DASHCAM_RETURN_DELAY_MS = 3_000L
        const val MUSIC_START_DELAY_MS = 3_000L
        const val MUSIC_LINK_RETURN_DELAY_MS = 5_000L
        const val THEME_HOLD_MS = 1_500L
        const val YMUSIC_WATCHDOG_INTERVAL_MS = 2_000L
        const val YMUSIC_WATCHDOG_MAX_ATTEMPTS = 15
        val YMUSIC_WATCHDOG_TOKEN = Any()
        val MUSIC_PLAY_RETRY_DELAYS_MS = longArrayOf(1_000L, 2_500L, 5_000L, 8_000L)
    }
}
