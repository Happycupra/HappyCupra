# Changelog

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
