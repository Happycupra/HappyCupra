package com.carlauncherc.launcher.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.app.role.RoleManager
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.content.res.ColorStateList
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.carlauncherc.launcher.CarLauncherApp
import com.carlauncherc.launcher.R
import com.carlauncherc.launcher.core.Format
import com.carlauncherc.launcher.core.Prefs
import com.carlauncherc.launcher.databinding.ActivityHomeBinding
import com.carlauncherc.launcher.location.HeadingSource
import com.carlauncherc.launcher.location.VehicleState
import com.carlauncherc.launcher.media.YMusicMediaBridge
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

        bindDashboardClicks()
        bindClockShortcut()
        bindDockActions()
        bindMusicControls()
        bindBackBehaviour()
        observe()

        if (!Prefs.firstRunDone) {
            binding.root.post { startFirstRunSetup() }
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
        binding.musicProgress.progressTintList = ColorStateList.valueOf(red)
        binding.musicProgress.progressBackgroundTintList = ColorStateList.valueOf(
            getColor(R.color.carbon_red_dark)
        )
    }

    // ------------------------------------------------------------------ wiring

    private fun bindDashboardClicks() {
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
        val isYMusic = selectedPackage == YMUSIC_PACKAGE
        binding.musicControls.visibility = if (isYMusic) View.VISIBLE else View.GONE

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

    private fun formatMediaTime(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSeconds = ms / 1000L
        val minutes = totalSeconds / 60L
        val seconds = totalSeconds % 60L
        return "%d:%02d".format(minutes, seconds)
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
                !state.hasFix -> R.string.gps_acquiring
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
            onPrefsChanged = {
                viewModel.refreshPrefs()
                lastCardinal = null
                render(viewModel.vehicle.value)
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
        firstRunSetupActive = false
        firstRunLocationRequested = false
        firstRunMediaAccessRequested = false
        toast(getString(R.string.first_run_done))
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
        viewModel.start()
        viewModel.refreshPrefs()
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
        if (app.musicAutoStartDone || !Prefs.musicAutoStart) return
        val stored = Prefs.musicPackage ?: return
        app.musicAutoStartDone = true

        binding.root.postDelayed({
            val packageName = IntentUtil.packageOf(stored)
            val launched = if (packageName == YMUSIC_PACKAGE) {
                launchYMusic()
            } else {
                IntentUtil.launchStored(this, stored, null)
            }

            if (!launched) {
                Prefs.musicPackage = null
                toast(getString(R.string.app_not_installed))
                return@postDelayed
            }

            if (Prefs.musicAutoPlay) {
                scheduleMediaPlayRetries(packageName)
            }
        }, MUSIC_START_DELAY_MS)
    }

    /**
     * Media apps often create their MediaSession asynchronously after their activity appears.
     * A single PLAY shortly after launch is therefore unreliable on slower head units.
     *
     * PLAY is intentionally retried instead of PLAY_PAUSE: repeated PLAY commands are harmless
     * once playback has already started, while PLAY_PAUSE could toggle it back off.
     */
    private fun launchYMusic(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(YMUSIC_PACKAGE, YMUSIC_MAIN_ACTIVITY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        }
        return try {
            startActivity(intent)
            true
        } catch (_: Exception) {
            IntentUtil.launchPackage(this, YMUSIC_PACKAGE)
        }
    }

    private fun scheduleMediaPlayRetries(packageName: String) {
        val handler = Handler(Looper.getMainLooper())
        MUSIC_PLAY_RETRY_DELAYS_MS.forEach { delayMs ->
            handler.postDelayed({
                if (packageName == YMUSIC_PACKAGE) {
                    dispatchYMusicPlay()
                }
                dispatchMediaPlay()
            }, delayMs)
        }
    }

    private fun dispatchYMusicPlay() {
        dispatchYMusicMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    }

    private fun dispatchYMusicMediaKey(keyCode: Int) {
        val eventTime = SystemClock.uptimeMillis()
        val down = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0)
        val up = KeyEvent(eventTime, eventTime, KeyEvent.ACTION_UP, keyCode, 0)

        runCatching {
            sendBroadcast(
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setPackage(YMUSIC_PACKAGE)
                    .addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                    .putExtra(Intent.EXTRA_KEY_EVENT, down)
            )
            sendBroadcast(
                Intent(Intent.ACTION_MEDIA_BUTTON)
                    .setPackage(YMUSIC_PACKAGE)
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
        const val MUSIC_START_DELAY_MS = 5_000L
        const val YMUSIC_PACKAGE = "com.kapp.youtube.final"
        const val YMUSIC_MAIN_ACTIVITY = "com.kapp.youtube.ui.MainActivity"
        val MUSIC_PLAY_RETRY_DELAYS_MS = longArrayOf(1_500L, 4_000L, 7_000L, 10_000L)
    }
}
