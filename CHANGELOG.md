# Changelog

## 0.19 — 2026-10-31

- Added four persisted dashboard layouts: Classic, Navigation-priority, Music-priority and Focus (gauges only); settings apply without Activity restart.
- Navigation and Music quick cards can be independently hidden or shown outside Focus mode; remaining cards expand to fill unused width.
- Added live GPS diagnostics opened via long-pressing the speed gauge or from Launcher Settings. Shows Android GPS_PROVIDER raw and filtered speed, fix age, position/speed accuracy, course and available GNSS satellites.
- GPS callback runs **only while the diagnostic dialog is visible**, with no coordinate display, history or transmission.
- New pure Kotlin JVM tests cover layout presets and GPS quality flags.
- Added docs/ROADMAP.md with phased plans for v0.20–v0.25 and signing/hardware requirements.

## 0.18 — 2026-10-31

- Added **opt-in real-time FFT equalizer** via Android audiofx.Visualizer on global output-mix audio session 0. Unlike the previous synthetic animation, bands respond to frequency energy in the playing audio.
- Requires explicit RECORD_AUDIO runtime consent and MODIFY_AUDIO_SETTINGS manifest permission. CarLauncher **does not record microphone PCM**, save samples, or transmit audio.
- Settings → Music visualization → Real audio sync / bass beat (beta). **Off by default**. Used only for the Equalizer mode while YMusic is playing and CarLauncher is foreground.
- Many head-unit ROMs restrict or omit global playback capture. Unsupported effects or silent FFT automatically fall back to the existing synthetic display; reliable real sync cannot be guaranteed on every device.
- Added pure Kotlin logarithmic FFT frequency-band analysis and unit tests, plus capture lifecycle cleanup.

## 0.17 — 2026-10-31

- Added twelve articulated stick-figure dance movements with randomized sequencing, eased transitions and no immediate move repetitions.
- Four persistent animation choices: full-width equalizer, solo dancer, synchronized duo and independently choreographed random freestyle duo.
- The stage now extends across the full music-card width beneath the album cover as well as track metadata.
- Figures wander sideways while staying in their allotted space; when no music plays they occasionally stand, sit, lie down, stretch or wave.
- Backward compatible with the previous solo preference and includes all Full-HD and GPS-only improvements prepared in 0.15 and 0.16.
- MediaSession offers playback state rather than audio PCM, so simulated rhythmic dancing is not actual beat detection. Animations stop repainting while the launcher is in the background.

## 0.16 — 2026-10-31

- Enlarged equalizer into a full-width lane in the player.
- Added launcher music-visualization setting: **Equalizer** or **Stick figure**.
- Stick figure dances with a synthetic rhythmic motion while YMusic plays. When stopped, it alternates between standing, sitting and lying down.
- Audio spectrum and real tempo are not available via YMusic's MediaSession, so this is not a real audio beat detector.
- Animation scheduling stops when the launcher moves to the background.
- All previously prepared 0.15 Full-HD layout and GPS-only improvements are included.

## 0.15 — 2026-10-31

- Responsive dashboard for 1024×600, 1280×720 and Full-HD-density Android head units; measured gauge text resizes down to avoid overlaps on vendor ROMs.
- Compact YMusic player card with scalable controls and animated 5-bar playback-state equalizer (not live audio-frequency sampling).
- GPS speed comes **only** from `LocationManager.GPS_PROVIDER` / `Location.getSpeed()` (m/s), with speed-accuracy screening, monotonic timestamps, lightweight filtering and a spike gate. No coordinate-distance fallback. Unknown speed is `--`, not a fabricated `0`.
- GPS speed indication is separate from presence of a GPS position fix; stale fixes disappear automatically.
- No attempt to alter the physical display resolution; Android device controls screen mode and DPI.

## 0.14 — 2026-10-31

- Added Android Share target for supported YouTube / YouTube Music track and playlist URLs.
- Added startup selection: resume last YMusic queue, open the saved link once then resume, or always open it.
- Song links are sent to YMusic v4 for playback; playlist-only links open the playlist screen because v4 does not yet auto-play them from an external VIEW link.
- Share receiver saves the link and enables YMusic v4 autostart when v4 is installed on the same Android device.
- The launcher returns to the dashboard after starting a linked track when supported by the head-unit ROM.

## 0.13 — 2026-10-31

- Added YMusic v4 package (`io.ymusic.android`) and debug package support, retaining legacy YMusic.
- The launcher recognises v4 media sessions for song title, artist, playback state, progress and transport controls.
- Media-button commands target the active or configured YMusic package rather than hard-coded legacy YMusic.
- Migrates legacy music shortcuts to v4 if installed when the old app is removed.
- Declares v4 packages for visibility on Android 11+.

## 0.12 — 2026-09-26

- Strengthened launcher startup on head units with a best-effort boot receiver when CarLauncher C already owns the HOME role.
- Reworked YMusic autostart into a 30-second watchdog.
- Autostart is only marked successful after actual YMusic playback is observed.
- Direct MediaSession PLAY, YMusic-targeted media-button broadcasts and periodic system PLAY fallback are combined.
- YMusic stays in the background; CarLauncher C remains visible.
- If startup times out, a later launcher resume can retry instead of being permanently marked done.
- Updated in-app version log.

## 0.11 — 2026-09-26

- YMusic autostart changed to background playback so CarLauncher C remains visible.
- YMusic-specific media control via MediaSession with media-button fallback.
- Now-playing panel with album cover, song title, artist, live progress, elapsed time and duration.
- Larger Previous, Play/Pause and Next controls.
- Phone-Link quick card removed; music area enlarged.
- Clock is now a configurable app shortcut, suitable for CarLink/ZLink or any installed app.
- Added Red / Carbon cockpit theme.
- Hold the compass for 1.5 seconds to cycle through Dark, Red / Carbon, Light and System themes.
- Guided first-run setup for location permission, YMusic media access and default HOME launcher.
- One-time in-app “What’s new” dialog after app updates.
- CI now publishes the latest APK to `apk/CarLauncher-C.apk` on `main` and handles concurrent updates safely.

## 0.10

- Upstream Minimal Car Launcher base imported and rebranded as CarLauncher C.
- Application id changed to `com.carlauncherc.launcher`.
- Configurable music-app autostart and automatic playback added.
- YMusic-specific launch/playback support introduced.
