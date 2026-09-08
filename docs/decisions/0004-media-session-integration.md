# ADR 0004: Mediensteuerung über Android MediaSession

**Status:** Angenommen

## Entscheidung

Die Medienintegration verwendet ausschließlich Androids `MediaSessionManager` und `MediaController`. Ein schlanker `NotificationListenerService` verschafft nach ausdrücklicher Freigabe Zugriff auf aktive Sessions. UI und Home-ViewModel sehen nur `MediaRepository` und `MediaPlayback`; app-spezifische SDKs werden nicht verwendet.

## Fehlertoleranz

Ohne Freigabe oder aktive Session liefert das Repository `MediaPlayback.Unavailable`. Das Dashboard zeigt dann klar bezeichnete Preview-Daten, deaktiviert die Transporttasten und bleibt vollständig bedienbar. Session-Wechsel und entfernte Apps werden ohne Exception an die UI übertragen.
