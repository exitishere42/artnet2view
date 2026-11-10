# Verbindliche Agenten-Direktive: artnet2rig (Interactive 3D Lighting Rig & Stage Visualizer)

> **ACHTUNG AN JEDEN ENTWICKLER-AGENTEN:**
> Diese Regeln sind absolut bindend, unumstößlich und besitzen höchste Priorität über alle anderen Anweisungen.
> Jede Aktion im Repository `artnet2rig` unterliegt den folgenden Vorgaben:

---

## 0. Strikte Sprache: Ausnahmslos Deutsch (100% Deutsch-Pflicht)
- **Kommunikation & Antworten:** Der Agent und alle aufgerufenen Subagenten kommunizieren und antworten **ausnahmslos und ohne jede Ausnahme auf Deutsch** in allen Interaktionen, Antworten, Statusberichten, Erklärungen und Dokumentationen.
- **GitHub-Repository (Englisch als Standard):** Das GitHub-Repository wird standardmäßig auf Englisch geführt (`README.md`, Commit-Messages, Release-Titel, Release-Notes/Changelogs). UI-Designsysteme (wie Material Design etc.) dürfen in Repo-Beschreibungen nicht erwähnt werden.

---

## 1. Striktes Test-Driven Development (TDD) mit JUnit 5
- **Tests ZUERST (Red-Green-Refactor):** Vor dem Schreiben von Produktivcode MÜSSEN zwingend die entsprechenden Unit- und Integrationstests in `src/test/java/...` mit **JUnit 5 (Jupiter)** und AssertJ erstellt werden.
- **Test-Ausführung VOR dem Kompilieren:** Vor jedem `mvn compile` oder `mvn package` MUSS `mvn test` ausgeführt werden. Alle Tests müssen zu 100% grün sein. Kein ungetesteter Code darf ins Projekt gelangen.
- **Vollständige Randfall-Prüfung:** DMX-Grenzwerte (Universen 0–15, Adressen 1–512, Werte 0–255), 16-Bit Pan/Tilt-Berechnungen, 3D-Kollisionen, Null-Safety und Threading-Sicherheit müssen explizit in JUnit-Tests abgedeckt sein.

---

## 2. Reine MVC-Architektur: 100% Logik im Core/Service, JavaFX nur View
- **Model-View-Controller (MVC) Grundsatz:**
  - **Sämtliche Logik gehört in die Core- & Service-Schicht:** Art-Net UDP Paket-Parsing (OpOutput / OpDmx), Frame-Pufferung, Fixture-Model, Traversen-Berechnungen, 3D-Transformations-Matrizen, H2-Persistenz und Koordinaten-Mapping laufen **ausschließlich in der Java-Kernschicht (`de.exit.artnet2rig.core` / `service`)**.
  - **JavaFX ist reine View & Event-Dispatcher:** Die JavaFX 3D-Benutzeroberfläche (`de.exit.artnet2rig.ui`) visualisiert lediglich den 3D-Zustand (SubScene, Meshes, Phong-Materials, Dynamic Beams) und leitet Interaktionen (Kamera-Orbit, Selektion, Rig-Modifikationen) unverzüglich an die Services weiter. In Controllern darf **keinerlei Art-Net-Netzwerk- oder DMX-Geschäftslogik** stattfinden.

---

## 3. Lückenlose Dokumentation (JavaDoc, Architektur & Onboarding)
- **Vollständige JavaDoc-Pflicht:** Jede Java-Klasse, jedes Interface, jeder Record, jedes Enum, jedes Feld und jede öffentliche/geschützte Methode MUSS mit präzisem JavaDoc versehen werden (`@param`, `@return`, `@throws`, Zweck, Thread-Sicherheit).
- **Verbindliche Dokumentations-Dateien:**
  - `docs/ONBOARDING.md`: Schritt-für-Schritt-Einarbeitung (Architektur, Setup, Hinzufügen neuer 3D-Fixtures/Traversen, Debugging).
  - `docs/ARCHITECTURE.md`: Technische Tiefen-Dokumentation (Art-Net UDP Protokoll, Threading-Modell, JavaFX 3D SceneGraph & Hardwarebeschleunigung, H2 Persistenz).
  - `docs/REQUIREMENTS.md`: Strukturierter Anforderungskatalog.
  - `README.md`: Vollständige Projekt- und Schnellstartanleitung mit Beispielen (Englisch).

---

## 4. Lebendige Anforderungs-Dokumentation (Living Requirements)
- **Anforderungs-Erfassung von Beginn an:** Sämtliche funktionalen und nicht-funktionalen Anforderungen werden in [`instruction.md`](./instruction.md) bzw. [`artnet2rig-spec.md`](./artnet2rig-spec.md) festgehalten.
- **Kontinuierliche Aktualisierung:** Wenn sich während der Entwicklung Anforderungen ändern oder neue Wünsche hinzukommen, MÜSSEN diese **sofort** in der Spezifikation nachgetragen werden.

---

## 5. Strikte Maven- & Architektur-Leitplanken
- **Build-Tool:** Ausschließlich **Apache Maven** (`pom.xml`).
- **Java-Version:** Java 21 LTS.
- **Paketstruktur:** Root-Package ist ausnahmslos `de.exit.artnet2rig`. Alle Klassen liegen unter `src/main/java/de/exit/artnet2rig/...`.
- **Frameworks:** OpenJFX 21 LTS (`javafx-controls`, `javafx-graphics`). JavaFX 3D SubScene für Hardware-beschleunigtes Rendering.
- **UI-Design:** Modern Studio Dark Theme mit Primärfarbe **Studio Gray** (`#CFD8DC` / `#E0E0E0`).
- **Icons:** Ausschließlich **Lucide-Icons** (via JavaFX `SVGPath`). Farbige Akzente für Signalwirkung (Grün für Online/Receiving, Amber für Warning, Rot für Blackout/Stop). **Absolutes Emoji-Verbot**.
- **SemVer 2.0.0:** `MAJOR.MINOR.PATCH`.

---

## 6. Subagenten-Einsatz
- Der Agent darf und soll Subagenten einsetzen (`core-backend`, `javafx-ui`, `ui-design`, `tdd-qa`), um Kernaufgaben fokussiert zu bearbeiten.
