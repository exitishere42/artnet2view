<div align="center">

<img src="artnet2view.png" alt="artnet2view Logo" width="128" height="128" />

```text
            _              _   ____            _                 
  __ _ _ __| |_ _ __   ___| |_|___ \__   _____(_) _____      __  
 / _` | '__| __| '_ \ / _ \ __| __) \ \ / / _ \ |/ _ \ \ /\ / /  
| (_| | |  | |_| | | |  __/ |_ / __/ \ V /  __/ |  __/\ V  V /   
 \__,_|_|   \__|_| |_|\___|\__|_____| \_/ \___|_|\___| \_/\_/    
```

**Interactive 3D Lighting Rig & Stage Visualizer for Art-Net**  
*Built with Java 21 LTS & JavaFX 3D*

[Features](#features) • [Quick Start](#quick-start) • [Installation](#installation) • [Build from Source](#build-from-source) • [Architecture](#architecture)

</div>

---

## Features

- **Interactive 3D Stage Visualizer**: Real-time 3D viewport rendering stage rigging, F34 trusses, and dynamic volumetric light cones.
- **Art-Net 4 Ingestion**: Native UDP receiver on port `6454` for up to 16 universes with jitter-free buffering and real-time telemetry.
- **Intuitive 3D Orbit Camera**: Smooth mouse-driven camera navigation (orbit rotation, panning, zooming, and instant perspective presets).
- **Pro Fixture Library**:
  - **Moving Head Spots**: Realistic 16-bit Pan/Tilt dual-axis rotation, dimmer, shutter/strobe, RGBW color mixing, and focused beam cones.
  - **LED Pars & Fluters**: RGBW color blending with adjustable beam angle.
  - **LED Bars**: Linear multizone wash fixtures.
- **Integrated 3D Rig Editor**:
  - Live 3D placement, translation (X, Y, Z), and rotation of trusses and lighting fixtures.
  - Fixture inspector for editing universe patches, DMX start addresses, and reverse pan/tilt toggles.
- **Embedded Persistence**: Robust project and profile storage powered by an embedded H2 database with HikariCP connection pooling, plus portable `.rig.json` export/import.
- **Cross-Platform**: Seamless native performance on Windows (Direct3D) and Linux (OpenGL / AppImage).
- **Lighting Suite Ecosystem**: Designed to work in perfect harmony with [**sound2artnet**](https://github.com/exitishere42/sound2artnet), [**artnet2dmx**](https://github.com/exitishere42/artnet2dmx), and [**jdmx**](https://github.com/exitishere42/JDMX).

---

## Quick Start

### Windows
Run `run.bat` or use the one-line installer:
```cmd
install.bat
```

### Linux
Run `run.sh` or install the AppImage:
```bash
chmod +x install.sh
./install.sh
```

---

## Build from Source

Requirements:
- JDK 21 LTS
- Apache Maven 3.9+

```bash
# Run unit tests
mvn clean test

# Build all-in-one executable fat JAR
mvn clean package

# Run directly
mvn exec:java
```

---

## Architecture

Please review [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for technical deep dives into the Art-Net buffer, 3D scene graph, and threading model.

---

## License

GNU General Public License v3.0 (GPL-3.0)
