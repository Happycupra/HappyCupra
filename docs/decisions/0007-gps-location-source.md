# ADR 0007: GPS als optionale lokale Datenquelle

**Status:** Angenommen

## Entscheidung

`LocationRepository` kapselt Androids `LocationManager` vollständig. Die generische Implementierung verwendet ausschließlich `GPS_PROVIDER`, fordert Updates nur an, solange die Launcher-Activity sichtbar ist, und führt keine Netzwerkabfragen durch. Geschwindigkeit wird aus Metern pro Sekunde in km/h umgerechnet; Status, Richtung und Genauigkeit bleiben explizite optionale Werte.

## Fehlertoleranz und Datenschutz

Fehlende Berechtigung, deaktiviertes GPS, ausstehender Fix und nicht verfügbare Hardware sind normale `GpsStatus`-Zustände. Keine Variante wirft einen Fehler bis in die UI. Standortkoordinaten werden weder persistiert noch übertragen. Ohne GPS bleibt das Dashboard bedienbar und zeigt statt einer erfundenen Geschwindigkeit `-- km/h`.
