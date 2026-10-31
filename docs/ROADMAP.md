# CarLauncher C — staged cockpit expansion

**Status:** technical backlog and release sequence. Only features explicitly marked
"implemented" are in the app; future versions are planned, not delivered.
**Primary design goal:** a reliable HOME screen that never prevents access to system settings,
navigation or another installed launcher if optional features fail.

## Cross-cutting requirements

1. Stay offline-first. Weather, map traffic, and optional cloud integrations should not
   break start-up, and no network permission should be needed for ordinary dashboards.
2. Support 1024×600, 1280×720 and 1920×1080-class screens at differing vendor DPI and
   font-scale settings. Never change physical screen resolution.
3. Avoid blocking the UI thread and never start GPS/audio/OBD services in the background
   without an explicit user-facing reason. Manage lifecycle and timeouts.
4. Prefer data directly reported by supported sensor APIs. Show **unavailable** for absent
   data; do not fabricate speed, vehicle telemetry, battery health, fuel efficiency or beats.
5. Preserve existing preferences. Each staged release must pass Android unit tests, APK
   compilation and an on-device acceptance checklist before it is shipped.
6. Motion-sensitive controls: defer text entry, complex reconfiguration and update prompts
   while driving, using reliable speed availability where possible.
7. Keep a second launcher on the device and preserve a recovery path. Separate signing and
   in-app update repair from cosmetic feature work.

## Roadmap

| Stage | Scope | Acceptance criteria | Risks / prerequisites |
|---|---|---|---|
| **0.19 — Cockpit layouts + GNSS diagnostics** | Four persistent dashboard presets; navigation/music quick-card switches; layout reflows without Activity recreation. Long-press speed gauge or open Settings to inspect raw GPS velocity, filtered velocity, fix age, accuracy, heading and available GNSS satellite counts. | Unit tests for presets/quality; no ghost gaps when both cards hidden; no loss of other click handlers; zero active GNSS callbacks after closing dialog. | Vendor GNSS firmware may not expose satellite status. **Implemented in PR associated with this roadmap.** |
| **0.20 — Dashboard editor** | Safe drag-to-reorder of gauge and quick-card components; visibility and sizing presets, per-profile undo/reset; single setting for default view. Layout serialization and migration. | Persist/reload exact order; restore defaults if stored state corrupt; large-font overflow tests; edit only while parked. | Touch accessibility, screen density. **Planned.** |
| **0.21 — Universal media bridge** | Generalize now-playing panel from YMusic to the active compatible MediaSession (Spotify, local players, etc.), user-priority selection, last-used player fallback; preserve YMusic start-link behaviour. | No control command sent to an unrelated session; blank media metadata handled; playback state and current package explicit. | Notification listener permission, app-specific session policies. **Planned.** |
| **0.22 — Profiles / smart start** | Profiles (Driver, Night, Navigation, Music), optional home/favorite destination shortcuts, robust per-profile startup sequencing with timeouts and manual override. | Manual override always available; never permanently hijack HOME; app unavailable => skip; no accidental repeated launches. | ROM background activity restrictions. **Planned.** |
| **0.23 — Trip log (offline)** | Opt-in trip tracking with distance, duration, averages, per-trip local storage, export/delete; pause/resume/restart handling and explicit consent. | Never infer distance from a single invalid fix, report sampling gaps and estimates clearly, make deletion and export work offline. | Location retention/privacy; minimum movement thresholds. **Planned.** |
| **0.24 — Vehicle telemetry (OBD2)** | Optional ELM327-compatible Bluetooth/Wi-Fi/USB adapter pairing, supported live PIDs (RPM, coolant temperature, voltage where available), reconnect logic, no write/control PIDs. | Only display values vehicle reports, connector not present => no crash, no unapproved ECU commands, safe reconnection. | Need representative hardware/car testing; CAN-specific support varies. **Planned.** |
| **0.25 — Ecosystem** | Opt-in weather, traffic shortcuts (external nav), supported camera integrations and Bluetooth media-control remapping. | Feature gracefully degrades offline or without firmware integration. | Internet, privacy permissions, vendor firmware interfaces. **Planned.** |

## Parallel reliability track (not tied to visual feature versions)

- **Critical: release signing.** The repository currently documents debug-signed
  builds and TODO owner/repo BuildConfig fields. Pin the update identity, provide a
  permanent signing key via secure CI secrets (never commit it), and test upgrades
  without uninstall/reinstall before promising a one-click in-app update.
- **GPS verification.** Compare raw getSpeed(), filtered speed, position accuracy and
  GNSS satellite counts on the specific head unit and reference GNSS receiver.
  Test start, stationary, urban canyon, tunnel and return-from-background.
- **Audio compatibility.** Real-time output-mix FFT remains optional and may be
  blocked by Android ROM policy; never imply real beat detection when fallback active.
- **Performance.** Limit visualizer redraw and satellite callback lifetimes and
  profile on a low-memory head unit. Avoid background work without user choice.
- **Device test matrix.** 1024×600 / 160 dpi, 1280×720, 1920×1080 at 1× and 2×,
  Android 10–15 where representative, font scaling, dark/light, missing GPS,
  missing compass, no network, app uninstalled, permissions denied.

## v0.19 on-device acceptance checklist

- Start launcher in Classic, Navigation, Music, Focus; close/open settings and reboot.
- Hide only Navigation, only Music, both; re-enable and check click/long-click behaviour.
- Verify speed, clock and compass never overlap at large font scale.
- Long-press GPS speed gauge: quality, age, horizontal accuracy, raw speed, filtered
  speed and satellites update live; close and confirm no lingering GNSS callback.
- Deny location permission and disable GPS; see appropriate explanations, no crash.
- Confirm YMusic playback, equalizer/solo/duo modes, audio beta and apps drawer remain intact.
- Confirm a second HOME app exists before changing system defaults or uninstalling.
