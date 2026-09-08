# DriveDeck Architektur

## Ziele

DriveDeck optimiert Startzeit, Fehlertoleranz und Bedienbarkeit im Fahrzeug. UI liest ausschließlich unveränderliche Zustände und sendet Benutzeraktionen an ViewModels. Android- und Herstellerzugriffe bleiben hinter Repository- beziehungsweise Adapter-Schnittstellen.

## Module und Abhängigkeiten

```text
app ─┬─ feature:home ─┬─ core:design
     ├─ feature:apps ─┼─ core:model
     └─ feature:settings ─ core:preferences
```

- **app**: Manifest, Activity, Navigation und manueller Composition Root.
- **core:model**: plattformarme Domänenmodelle.
- **core:preferences**: DataStore-Vertrag und Implementierung.
- **core:design**: Automotive-Farben, Typografie, Maße und wiederverwendbare Komponenten.
- **feature:home**: Dashboard-Zustand, Uhr, Demo-Medienkarte und Edit-Mode.
- **feature:apps**: gekapselte PackageManager-Abfrage, Start und Favoriten.
- **feature:settings**: fahrgeeignete Basis-Einstellungen.

Abhängigkeiten zeigen nach innen. Feature-Module kennen sich nicht gegenseitig; die App koordiniert Ziele. Phase-1-Navigation bleibt absichtlich typisiert und klein, statt eine zusätzliche Navigation-Library einzuführen.

## Zustandsfluss

Repositories stellen `Flow` bereit. ViewModels kombinieren diese zu einem `StateFlow<UiState>`. Composables sind zustandslos, soweit praktikabel, und senden Events zurück. Langlaufende oder optionale Funktionen dürfen den ersten Frame nicht blockieren.

## Erweiterungspunkte

Spätere `integration:*`-Module implementieren Verträge wie `HeadUnitAdapter`, `RadioRepository`, `VehicleRepository` oder `LocationRepository`. Fähigkeiten werden explizit gemeldet; `Unsupported` ist ein gültiger Zustand. Die UI bindet nie herstellerspezifische Klassen ein.

## Responsive UI

Layouts reagieren auf verfügbare `Dp`-Breite und -Höhe, nicht auf konkrete Displays. Kompakte Displays nutzen zwei Dashboard-Spalten, breite drei. Touch-Ziele sind mindestens 64 dp groß. Scrollen ist auf Apps und Einstellungen beschränkt.

## Dependency Injection

Phase 1 nutzt constructor injection plus einen kleinen Composition Root in `app`. Damit bleiben Klassen testbar, ohne Hilt/KSP und zusätzliche Startkosten einzuführen. Bei mehreren austauschbaren Integrationen wird Hilt erneut bewertet.

## Build-Werkzeuge

Das Repository enthält bewusst keine binäre Gradle-Wrapper-JAR, da der verwendete Pull-Request-Workflow keine Binärdateien akzeptiert. Lokal wird Gradle 8.10.2 verwendet; GitHub Actions installiert exakt dieselbe Version deklarativ über `gradle/actions/setup-gradle`. Dadurch bleibt der Build reproduzierbar, ohne Binärartefakte im Repository abzulegen.
