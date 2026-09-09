# ADR 0008: Offline Day/Night und fortlaufende Debug-Versionen

**Status:** Angenommen

## Entscheidung

Der automatische Designmodus verwendet zunächst einen transparenten Offline-Zeitplan: Tag ab 07:00 Uhr, Nacht ab 19:00 Uhr lokaler Gerätezeit. Manuelle Modi Tag und Nacht haben stets Vorrang. Die UI-Helligkeit wird getrennt zwischen 20 und 100 Prozent gespeichert und auf das aktuelle Activity-Fenster angewendet.

Debug-Builds verwenden lokal `0.3.0`. In GitHub Actions werden `versionCode` und `versionName` aus der fortlaufenden Workflow-Nummer erzeugt (`0.3.<run>`). Die resultierende APK und ihre SHA-256-Prüfsumme werden unter einem versionshaltigen Dateinamen als Artifact bereitgestellt.
