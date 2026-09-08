# DriveDeck

DriveDeck ist ein eigenständiger, für Android-Autoradios entwickelter HOME-Launcher. Das Projekt folgt dem Leitbild **„Maximale Funktion mit minimaler Bedienkomplexität“**: große Ziele, hoher Kontrast und die wichtigsten Aktionen in höchstens zwei Berührungen.

## Entwicklungsstand

Phase 1 liefert die belastbare Basis: responsives Landscape-Dashboard, HOME-Registrierung, App-Übersicht und -Start, persistente Favoriten und Design-Einstellungen sowie einen abgesicherten Dashboard-Editiermodus. MediaSession, GPS und Hardware-Integrationen folgen bewusst in späteren Phasen.

> Screenshots werden nach dem ersten Emulator-Release ergänzt.

## Voraussetzungen

- Android Studio Ladybug oder neuer
- Android SDK 35 (minSdk 29)
- JDK 17
- Gradle 8.10.2 (der CI-Build installiert diese Version automatisch)

## Bauen und testen

```bash
gradle test
gradle lint
gradle assembleDebug
```

Die APK liegt danach unter `app/build/outputs/apk/debug/app-debug.apk`.

## Installation und HOME-Auswahl

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Nach dem Druck auf die Home-Taste DriveDeck auswählen und **Immer** bestätigen. Alternativ führt der Hinweis unter **Einstellungen → System** zu den HOME-Einstellungen. Die Anwendung ist primär für Landscape konfiguriert.

## Architektur

Die Module sind nach Verantwortlichkeit getrennt: `core:model`, `core:preferences` und `core:design` enthalten stabile Grundlagen; `feature:home`, `feature:apps` und `feature:settings` enthalten UI und ViewModels; `app` bildet den Composition Root. Details stehen in [ARCHITECTURE.md](ARCHITECTURE.md), Entscheidungen in [`docs/decisions`](docs/decisions) und der Plan in [ROADMAP.md](ROADMAP.md).

## Bekannte Einschränkungen

- Phase 1 steuert noch keine MediaSession; die Medienkarte nutzt klar bezeichnete Demo-/Preview-Daten.
- Navigation, Telefon und Radio öffnen in Phase 1 eine passende installierte beziehungsweise vom Nutzer favorisierte App, sofern vorhanden.
- Dashboard Drag & Drop, GPS, Day/Night-Automatik und Headunit-Adapter sind für spätere Phasen geplant.
- Manche Hersteller-ROMs behandeln die HOME-Auswahl anders als AOSP.

## Mitarbeit

Entwicklung erfolgt über kurze Feature-Branches und Pull Requests gegen `main`. Änderungen müssen Tests, Lint und den Debug-Build bestehen.
