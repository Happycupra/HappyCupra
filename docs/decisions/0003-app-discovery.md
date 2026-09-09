# ADR 0003: App-Erkennung hinter einem Repository

**Status:** Angenommen

## Entscheidung

Nur `InstalledAppsRepository` greift auf `PackageManager` zu. Es sucht startbare MAIN/LAUNCHER-Aktivitäten, entfernt DriveDeck selbst, sortiert nach lokalisiertem Namen und fängt nicht startbare oder inzwischen entfernte Pakete ab.

## Folgen

UI und ViewModels bleiben frei von Android-Abfragen. OEM-Abweichungen führen zu einem leeren/fehlertoleranten Ergebnis statt zu einem Launcher-Absturz.
