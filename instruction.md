# Projektanweisung: artnet2view (Interactive 3D Lighting Rig & Stage Visualizer)

> **An den ausführenden Entwickler-Agenten:**
> Dieses Dokument ist deine verbindliche Spezifikation und Leitplanke.
> Arbeite die Roadmap strikt Phase für Phase nach TDD-Prinzipien ab.
> Weiche nicht eigenmächtig von Stack oder Architektur ab.

---

## 1. Projektübersicht & Scope

### Problem & Ziel
In der professionellen und semiprofessionellen Lichttechnik fehlt ein schlanker, kostenloser und plattformunabhängiger 3D-Bühnen-Visualizer, der Art-Net-Streams empfängt und Traversen sowie Scheinwerfer mit echten 3D-Lichtstrahlen in Echtzeit visualisiert. Bestehende Programme sind extrem teuer, an proprietäre Pulte gekoppelt oder instabil.

**`artnet2view`** schließt diese Lücke als vollwertiger, leichtgewichtiger **3D Lighting Rig & Stage Visualizer mit interaktivem Rig-Editor**.

### In-Scope (MVP v1.0)
1. **Art-Net 4 UDP Ingestion:**
   - Empfang von standardisiertem Art-Net (Port `6454` UDP, OpDmx / OpOutput).
   - Unterstützung von Universen 0 bis 15 (Net/Sub-Net konfigurierbar).
   - Jitter-freier Puffer & Frame-Rate Telemetrie (`DMX Frames/s`).
2. **Hardware-beschleunigtes 3D-Rendering (JavaFX 3D):**
   - Interaktive 3D-SubScene mit nativer Hardwarebeschleunigung (Direct3D unter Windows, OpenGL unter Linux).
   - Orbit-Kamera mit Maussteuerung (Drehen, Pannen, Zoomen, Preset-Views: Top, Front, Side, Perspective).
   - Bühnenboden mit Gitter (Grid), Traversen-Rigging und Rückwand.
3. **Interaktiver 3D Rig-Editor:**
   - Platzierung, Verschieben (X, Y, Z) und Rotation von Traversen und Scheinwerfern im 3D-Raum.
   - Fixture-Patching: Zuweisung von Universe, DMX-Startadresse und Modus.
   - Visuelle Selektion von Elementen mit 3D-Bounding-Box und Inspector-Dock.
4. **3D Fixture- & Rig-Katalog:**
   - **Moving Head Spot:** 16-Bit Pan/Tilt (separate Rotation von Base und Yoke/Head), Dimmer, Strobe, RGBW-Farbmischung, fokussierter Lichtstrahl (Cone).
   - **LED Par 64:** Statischer Spot/Flood mit RGBW-Farbe, Dimmer und Strobe.
   - **LED Bar / Batten:** Fluter mit segmentierter oder homogener Farbabstrahlung.
   - **Traversen (Trusses):** F34 4-Punkt Traversen (1m, 2m, 3m), 90°-Ecken, T-Stücke und Bodenplatten.
5. **Echte volumetrische Lichtstrahlen (Beams & Haze):**
   - Transparente Lichtkonen mit sanftem Falloff.
   - Echtzeit-Farbübernahme, Dimmer und Strobe-Flimmern.
6. **Persistenz & Speicherung (H2 Datenbank & JSON):**
   - Embedded H2-Datenbank mit HikariCP (`~/.artnet2view/rigs.mv.db`) für Projekte, Fixture-Bibliotheken und Presets.
   - Export/Import als saubere `.rig.json`-Projektdatei.
7. **Modern Studio Dark UI (Gray Accent):**
   - Material Design 2 Dark Theme mit Akzentfarbe Studio Gray (`#CFD8DC` / `#E0E0E0`).
   - Lucide-Vektor-Icons (keine Emojis!).

### Out-of-Scope (Ausdrücklich verboten / Nicht implementieren)
- Keine integrierten DMX-Fader-Pulte, Cues oder Show-Timeline (dafür existiert das Schwesterprojekt `jdmx`).
- Keine externen C++-Engines oder unsichere native JNI-Wrapper (alles über OpenJFX 3D).
- Keine Cloud-Accounts oder Online-Zwang.

---

## 2. Tech-Stack & Umgebungsleitplanken

- **Laufzeit & Sprache:** Java 21 LTS (OpenJDK 21).
- **Build-Tool:** Apache Maven (`pom.xml`).
- **GroupId / Package:** `de.exit.artnet2view`.
- **Frameworks:**
  - OpenJFX 21 LTS (`javafx-controls`, `javafx-graphics`, `javafx-base`).
  - H2 Database `2.3.232` mit HikariCP `6.2.0`.
  - Jackson `2.18.2` für JSON.
  - SLF4J `2.0.16` für Logging.
- **Testing:** JUnit 5 Jupiter `5.11.3` + AssertJ `3.26.3`.
- **Cross-Platform:** Windows (Direct3D) & Linux (OpenGL / Mesa, Fat-JAR, Linux AppImage via `build-appimage.sh`).

---

## 3. Architektur & Modul-Struktur (Reine MVC-Trennung)

```text
de.exit.artnet2view
├── Main.java                          // JavaFX Launcher
├── Artnet2viewApplication.java          // JavaFX Application Lifecycle
├── core/                              // 100% reine Geschäftslogik (Kein JavaFX UI-Code!)
│   ├── artnet/                        // Art-Net UDP Receiver & OpDmx Parser
│   │   ├── ArtNetPacket.java
│   │   ├── ArtNetReceiver.java
│   │   └── DmxUniverseBuffer.java
│   ├── model/                         // 3D-Domänenmodell
│   │   ├── Fixture.java
│   │   ├── FixtureType.java           // MOVING_HEAD_SPOT, LED_PAR, LED_BAR
│   │   ├── FixturePatch.java          // Universe (0-15), StartAddress (1-512)
│   │   ├── TrussElement.java          // F34 Straight, Corner, Base
│   │   ├── Transform3D.java           // Position (x,y,z), Rotation (pitch,yaw,roll)
│   │   └── RigProject.java            // Gesamt-Setup
│   └── dmx/                           // DMX-Kanal-Mapper
│       ├── ChannelMapping.java
│       └── FixtureProfile.java
├── service/                           // Services & Persistenz
│   ├── ArtNetService.java             // Start/Stop des UDP-Sockets, Event-Dispatch
│   ├── RigService.java                // Verwalten von Fixtures & Traversen
│   ├── DatabaseService.java           // H2 DataSource (HikariCP) & Schema-Setup
│   └── RigRepository.java             // CRUD-Operationen für Rigs
└── ui/                                // Reine View-Schicht
    ├── MainView.java                  // Hauptlayout (Header, 3D Canvas, Inspector)
    ├── canvas/                        // 3D Visualizer Canvas
    │   ├── Stage3DCanvas.java         // JavaFX SubScene & 3D Group
    │   ├── OrbitCameraController.java // 3D-Kamerasteuerung
    │   ├── MeshFactory.java           // Generierung von 3D-Traversen, Lampen & Beams
    │   └── BeamRenderer.java          // Volumetrische Lichtkegel
    ├── components/                    // UI-Widgets & Inspector
    │   ├── HeaderBar.java             // Universum, FPS, Connect/Disconnect, Projekt-Menü
    │   ├── FixtureInspector.java      // Bearbeiten von Position, Rotation & DMX-Patch
    │   ├── LibraryDrawer.java         // Drag & Drop / Klick zum Hinzufügen von Elementen
    │   └── ModalOverlay.java          // In-App Modale Dialoge
    └── theme/                         // Design & Styling
        ├── ThemeConstants.java        // Studio Gray (#CFD8DC), Surfaces (#121212)
        └── LucideIcon.java            // SVGPath Vektor-Icons
```

---

## 4. Phasenweise Implementierungs-Roadmap

### Phase 1: Projekt-Setup & Art-Net Core-Engine (TDD)
- Maven-Setup, JUnit 5 Tests für UDP Art-Net Paket-Parsing (`OpDmx` Paket-Header `Art-Net\0`, OpCode `0x5000`, Universe-Extraktion, 512-Byte Payload).
- `DmxUniverseBuffer` für thread-sicheren Lese-/Schreibzugriff (40Hz+ Durchsatz).

### Phase 2: H2-Persistenz & Datenmodell (TDD)
- H2 embedded Schema für Rigs, Fixtures, Traversen und Profile.
- Repositories mit HikariCP-Verbindungspool.
- JSON Export/Import-Logik für `.rig.json`.

### Phase 3: JavaFX 3D-SubScene & Orbit-Kamera
- 3D-SubScene mit `PerspectiveCamera`, Bühnen-Gitter (Floor Grid), Koordinatenachsen (X=Rot, Y=Grün, Z=Blau).
- Interaktiver `OrbitCameraController`:
  - Rechte Maustaste / Ziehen: Drehen um Zielpunkt.
  - Mittlere Maustaste / Umschalt + Ziehen: Pannen.
  - Scrollrad: Stufenloser Zoom.
  - Preset-Buttons: Top, Front, Side, Reset.

### Phase 4: 3D-Mesh-Factory & Traversen-System
- Parametrische 3D-Geometrie für F34 Traversen (Hauptrohre + Diagonale Bracings), Corners und Baseplates.
- Einfaches Einrasten (Snapping) von Traversen-Segmenten.

### Phase 5: Scheinwerfer-Modelle & Beam-Rendering
- 3D-Meshes für Moving Heads (Base + Yoke + Head separat drehbar) und Pars.
- Volumetrische Lichtkegel (`Cylinder` / `MeshView` Cones mit additivem/transparentem Material).
- Verknüpfung der Art-Net-DMX-Puffer mit den 3D-Scheinwerfern:
  - DMX-Kanäle Pan/Tilt $\rightarrow$ Rotation von Base und Head.
  - DMX-Kanal Dimmer $\rightarrow$ Deckkraft / Intensität des Beams.
  - DMX-Kanäle R/G/B/W $\rightarrow$ Materialfarbe des Lichtkegels und Linsenglanz.
  - DMX-Kanal Strobe $\rightarrow$ Zeitgesteuertes Ein-/Ausblenden.

### Phase 6: Rig-Editor UI & Inspector
- Selektion von Elementen im 3D-Raum (PickResult).
- Inspector-Dock zur numerischen Anpassung von Koordinaten (X, Y, Z), Rotation und DMX-Startadresse.
- Bibliothek zum Hinzufügen neuer Lampen und Traversen.
- Header-Bar mit Status-Telemetrie (Pakete/Sekunde, aktive Universen).

### Phase 7: Packaging & Cross-Platform-Deployment
- `mvn test` zu 100% grün.
- Fat-JAR Erstellung (`artnet2view-1.0.0-all.jar`).
- `build-appimage.sh` für Linux, `.bat` / `.sh` Starter für Windows und Linux.
