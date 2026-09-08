# DriveDeck Roadmap

## Phase 1 — stabile Launcher-Basis

- [x] Gradle-Kotlin-DSL, Versionskatalog und modulare Struktur
- [x] Automotive Compose Theme und responsives Dashboard
- [x] HOME-Intent und Launcher-Status
- [x] installierte startbare Apps anzeigen und öffnen
- [x] Favoriten und Einstellungen per DataStore speichern
- [x] expliziter Dashboard-Edit-Mode als sichere Vorbereitung
- [x] CI für Tests, Lint und Debug-APK

## Phase 2 — Medien und Personalisierung

- [x] MediaSession-Integration und Metadaten/Artwork
- [x] universelle Transportsteuerung
- [ ] Dashboard-Layouts, Größen und Drag & Drop nur im Edit-Mode (nächster Punkt)
- [ ] konfigurierbare Favoritenleiste (nächster Punkt)

## Phase 3 — Geräteservices

- [ ] GPS-Abstraktion, Berechtigungsfluss und Geschwindigkeit
- [ ] automatische Day/Night-Umschaltung
- [ ] generische Radio- und Telefon-Provider

## Phase 4 — Headunit und Fahrzeug

- [ ] `HeadUnitAdapter` mit Capability-Modell
- [ ] optionale OBD2-, CAN-/MCU- und Lenkradtasten-Integrationen
- [ ] dokumentierte Herstelleradapter ohne UI-Kopplung
