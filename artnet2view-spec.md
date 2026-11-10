# Technische Masterspezifikation: artnet2view (v1.0.0)

## 1. Systemarchitektur & Datenfluss

```text
[ Art-Net Sender ] (jdmx / sound2artnet / GrandMA / QLC+)
        │  (UDP Port 6454 - OpDmx)
        ▼
[ ArtNetReceiver (UDP Socket) ]
        │  Thread-sicherer Transfer
        ▼
[ DmxUniverseBuffer (16 Universen à 512 Bytes) ]
        │
        ├────────────────────────────┐
        ▼                            ▼
[ 3D Render Loop (JavaFX) ]   [ Inspector / Telemetry ]
  - 16-Bit Pan / Tilt           - Paketrate (Hz)
  - RGBW Lichtkegel             - Kanal-Werte
  - Dimmer & Shutter Strobe     - Patch-Status
        ▼
[ JavaFX 3D SubScene ]
  - Orbit Camera
  - 3D-Traversen (F34)
  - 3D-Moving Heads & Pars
  - Bühnenboden & Lichtstrahlen
```

---

## 2. Art-Net Spezifikationsdetails (Art-Net 4)

- **Protokoll-Kennung:** 8 Bytes ASCII: `Art-Net\0`.
- **OpCode:** `0x5000` (OpOutput / OpDmx).
- **Protokoll-Version:** `14` (High Byte: `0x00`, Low Byte: `0x0E`).
- **Sequence:** `0x00` bis `0xFF`.
- **Physical:** Physischer Eingangsport (`0x00`).
- **SubUni:** Low 8-Bits der Universe-Adresse (Net/Subnet/Universe).
- **Net:** High 7-Bits der Universe-Adresse.
- **Length:** 2 bis 512 (High Byte vor Low Byte).
- **Data:** 512 Byte DMX-Array (`0x00` - `0xFF`).

---

## 3. Datenmodell (H2 & JSON)

### 3.1 Tabelle `projects`
- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `name` VARCHAR(128) NOT NULL
- `description` VARCHAR(512)
- `created_at` TIMESTAMP NOT NULL
- `updated_at` TIMESTAMP NOT NULL

### 3.2 Tabelle `truss_elements`
- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `project_id` BIGINT NOT NULL (FK)
- `type` VARCHAR(64) NOT NULL (STRAIGHT_1M, STRAIGHT_2M, STRAIGHT_3M, CORNER_90, T_JOINT, BASE_PLATE)
- `pos_x` DOUBLE NOT NULL
- `pos_y` DOUBLE NOT NULL
- `pos_z` DOUBLE NOT NULL
- `rot_pitch` DOUBLE NOT NULL
- `rot_yaw` DOUBLE NOT NULL
- `rot_roll` DOUBLE NOT NULL

### 3.3 Tabelle `fixtures`
- `id` BIGINT AUTO_INCREMENT PRIMARY KEY
- `project_id` BIGINT NOT NULL (FK)
- `name` VARCHAR(128) NOT NULL
- `type` VARCHAR(64) NOT NULL (MOVING_HEAD_SPOT, LED_PAR, LED_BAR)
- `universe` INT NOT NULL DEFAULT 0
- `start_channel` INT NOT NULL DEFAULT 1
- `channel_count` INT NOT NULL DEFAULT 16
- `pos_x` DOUBLE NOT NULL
- `pos_y` DOUBLE NOT NULL
- `pos_z` DOUBLE NOT NULL
- `rot_pitch` DOUBLE NOT NULL
- `rot_yaw` DOUBLE NOT NULL
- `rot_roll` DOUBLE NOT NULL
- `pan_invert` BOOLEAN DEFAULT FALSE
- `tilt_invert` BOOLEAN DEFAULT FALSE
- `beam_angle` DOUBLE DEFAULT 15.0

---

## 4. Standard DMX-Fixture-Profile

### 4.1 Moving Head Spot (16 Kanäle)
1. Pan Coarse (0-255)
2. Pan Fine (0-255)
3. Tilt Coarse (0-255)
4. Tilt Fine (0-255)
5. Pan/Tilt Speed (0-255)
6. Dimmer Master (0-255)
7. Shutter / Strobe (0: Offen, 1-255: Strobe-Frequenz)
8. Red (0-255)
9. Green (0-255)
10. Blue (0-255)
11. White (0-255)
12. Color Wheel Preset
13. Gobo Wheel Preset
14. Prism / Effect
15. Focus
16. Special / Reset

### 4.2 LED Par 64 (8 Kanäle)
1. Dimmer Master (0-255)
2. Shutter / Strobe (0-255)
3. Red (0-255)
4. Green (0-255)
5. Blue (0-255)
6. White (0-255)
7. Macro Color
8. Speed

---

## 5. UI-Spezifikation (Material Design 2 Dark Theme)

- **Primärfarbe:** `#CFD8DC` (Studio Gray)
- **Hintergründe:**
  - Base: `#121212`
  - Cards & Sidebars: `#1E1E1E`
  - Hover / Elevation: `#2A2A2A`
  - Divider / Borders: `#333333`
- **Signal-Farben:**
  - Status Online / Receiving: `#00E676` (Cyber Mint)
  - Warning / Frame Drops: `#FFB74D` (Amber)
  - Offline / Stop: `#FF5252` (Red)
- **Komponenten:**
  - 3D Viewport im Zentrum (flexibel skalierend).
  - Header-Bar oben mit Logo, Verbindungs-Status, Frame-Rate, Projekt-Name, Preset-Views.
  - Rechter Inspector (ausklappbar): Eigenschaften des selektierten Objekts.
  - Untere Statusleiste mit Universum-Monitor und Koordinaten.
