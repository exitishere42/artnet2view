# Living Requirements Catalog: artnet2view

## Functional Requirements (FR)

- **FR-1 [Art-Net UDP Streaming]:** Receive Art-Net 4 packets on UDP port 6454 for universes 0 to 15.
- **FR-2 [Frame Rate & Telemetry]:** Measure and display packets-per-second (FPS) and active universes in real-time.
- **FR-3 [3D Stage Visualization]:** Render a 3D stage with floor grid, coordinate system, and trussing.
- **FR-4 [3D Orbit Camera]:** Free camera orbit, panning, zooming, and quick preset view angles (Top, Front, Side, Perspective).
- **FR-5 [3D Fixture Catalog]:** Support Moving Head Spots (Pan, Tilt, Dimmer, Color, Strobe), LED Pars, and LED Bars.
- **FR-6 [Real-Time Beams]:** Display volumetric light cones that react dynamically to DMX dimmer, shutter, and RGBW values.
- **FR-7 [3D Rig Editor]:** Place, translate (X, Y, Z), rotate, and patch fixtures and trusses interactively.
- **FR-8 [H2 & JSON Persistence]:** Store projects, stages, and fixture configurations in an embedded H2 database and export/import as `.rig.json`.

## Non-Functional Requirements (NFR)

- **NFR-1 [Performance]:** Sustain 40+ FPS rendering and jitter-free DMX parsing without UI freezing.
- **NFR-2 [Cross-Platform]:** Flawless native execution on Windows 10/11 (Direct3D) and Linux (OpenGL/Mesa).
- **NFR-3 [Zero External JNI]:** Pure Java 21 LTS and OpenJFX 3D without unmanaged third-party C++ dependencies.
- **NFR-4 [Design]:** Clean studio dark theme with Studio Gray accent (`#CFD8DC`) and Lucide vector iconography.
