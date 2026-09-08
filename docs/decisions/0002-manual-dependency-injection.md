# ADR 0002: Manuelle Dependency Injection in Phase 1

**Status:** Angenommen

## Entscheidung

DriveDeck verwendet constructor injection und einen anwendungsweiten Composition Root. Hilt wird erst eingeführt, wenn mehrere austauschbare Integrationen oder Scopes den zusätzlichen Compiler- und Laufzeitaufwand rechtfertigen.

## Folgen

Repositories und ViewModels bleiben testbar. Die Abhängigkeitsmenge, Buildzeit und Launcher-Startarbeit bleiben klein; der Composition Root muss bewusst übersichtlich gehalten werden.
