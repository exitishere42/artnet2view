# artnet2view System Architecture

## 1. High-Level Architecture Overview

`artnet2view` is structured according to strict Model-View-Controller (MVC) principles, isolating all DMX network ingestion and mathematics in the core layer, while keeping the JavaFX 3D rendering pipeline completely decoupled.

```text
┌────────────────────────────────────────────────────────┐
│                      Core Layer                        │
│  - ArtNetReceiver (UDP socket listening on port 6454)   │
│  - DmxUniverseBuffer (Atomic / Volatile 16x512 array)  │
│  - 3D Geometry Models & Transformation Matrices        │
└──────────────────────────┬─────────────────────────────┘
                           │ Dispatches state changes / 40Hz tick
┌──────────────────────────▼─────────────────────────────┐
│                     Service Layer                      │
│  - ArtNetService (Lifecycle & Telemetry)               │
│  - RigService (Fixtures, Trusses, Patching)            │
│  - DatabaseService (H2 embedded storage via HikariCP)  │
└──────────────────────────┬─────────────────────────────┘
                           │ Binds to Observable properties
┌──────────────────────────▼─────────────────────────────┐
│                       UI Layer                         │
│  - JavaFX 3D SubScene (PerspectiveCamera, MeshView)    │
│  - Dynamic BeamRenderer (Volumetric cone shaders)      │
│  - Modern Studio Dark Theme (Gray Accent #CFD8DC)      │
└────────────────────────────────────────────────────────┘
```

## 2. Art-Net UDP Ingestion

- **Port:** UDP `6454`
- **Packet Structure:** Standard Art-Net 4 OpDmx (`0x5000`)
- **Buffer:** Low-overhead `DmxUniverseBuffer` maintaining real-time snapshots of up to 16 DMX universes (512 channels each).
- **Thread Safety:** Packet parsing runs in a high-priority background daemon thread. Snapshot copies are delivered to the UI render thread without lock contention.

## 3. JavaFX 3D Graphics Pipeline

- **Scene Graph:** Utilizes `javafx.scene.SubScene` with antialiasing enabled (`SceneAntialiasing.BALANCED`).
- **Hardware Acceleration:**
  - Windows: Direct3D via Prism.
  - Linux: OpenGL via Mesa / Hardware driver.
- **Lighting & Beams:**
  - Dynamic `MeshView` cones with semi-transparent diffuse colors and additive blending.
  - Moving Head fixtures rotate around two independent axes: Pan (Y-axis of base) and Tilt (X-axis of yoke).

## 4. Persistence Architecture

- **Embedded Database:** H2 Engine in file mode (`~/.artnet2view/rigs.mv.db`).
- **Connection Pooling:** HikariCP with connection validation and optimized pool size.
- **Interchange:** JSON serialization for project files (`*.rig.json`).
