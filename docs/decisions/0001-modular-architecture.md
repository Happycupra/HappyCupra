# ADR 0001: Feature-orientierte modulare Architektur

**Status:** Angenommen

## Entscheidung

Stabile Grundlagen liegen in `core:*`, Nutzerbereiche in `feature:*`, Android-Einstieg und Verdrahtung in `app`. Herstellerintegrationen werden später in `integration:*` ergänzt und implementieren ausschließlich Core-Verträge.

## Gründe

Dies verhindert UI-/Hardware-Kopplung, ermöglicht isolierte Tests und hält optionale Gerätefunktionen austauschbar. Es vermeidet zugleich eine verfrühte Aufteilung in viele leere Module.
