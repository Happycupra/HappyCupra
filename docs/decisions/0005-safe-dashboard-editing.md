# ADR 0005: Sicherer Dashboard-Edit-Modus

**Status:** Angenommen

## Entscheidung

Dashboard-Elemente sind eine geordnete Liste stabiler Typen mit genau einer von vier Größen. Auswahl, Reihenfolge und Größe werden kompakt und versionsrobust in DataStore gespeichert. Verschieben erfordert einen langen Druck und ist ausschließlich im deutlich markierten Edit-Modus aktiv. Der normale Fahrmodus enthält keinerlei Drag-Gesten.

## UX-Folgen

Der Ablauf bleibt linear: Edit-Modus öffnen, Elemente an-/abwählen, lange drücken und verschieben, Größe direkt an der Kachel ändern, mit „Fertig“ verlassen. Mindestens eine Kachel bleibt sichtbar. Unbekannte oder beschädigte gespeicherte Werte werden ignoriert; ein vollständig ungültiges Layout fällt auf das sichere Standardlayout zurück.
