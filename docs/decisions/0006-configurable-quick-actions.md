# ADR 0006: Konfigurierbare Favoritenleiste

**Status:** Angenommen

## Entscheidung

Die Favoritenleiste enthält höchstens fünf eindeutige `QuickAction`-Werte. Reihenfolge und Belegung werden über einen defensiven Codec in DataStore gespeichert. Die Konfiguration erfolgt in den Einstellungen über große Schaltflächen zum Verschieben und Ersetzen; im normalen Fahrmodus bleibt jeder Eintrag eine einzige große Touchaktion.

## Fehlertoleranz

Unbekannte, doppelte und überzählige gespeicherte Werte werden verworfen. Ein leerer oder vollständig ungültiger Wert fällt auf Home, Navigation, Musik, Telefon und Apps zurück. Dadurch bleibt der HOME-Launcher jederzeit navigierbar.
