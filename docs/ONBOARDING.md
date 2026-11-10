# Developer Onboarding Guide: artnet2rig

Welcome to `artnet2rig`! This guide helps you get up to speed with the codebase, development workflow, and conventions.

## 1. Prerequisites
- **JDK 21 LTS** (e.g. Eclipse Temurin 21)
- **Apache Maven 3.9+**
- Git

## 2. Quick Setup & Build
Clone the repository and build:
```bash
git clone https://github.com/exitishere42/artnet2rig.git
cd artnet2rig
mvn clean test
mvn package
```

Run in development mode:
```bash
mvn exec:java
```

## 3. Strict Development Rules
Before committing any changes, you must strictly follow:
1. **Tests First (TDD):** Every feature or bugfix must have JUnit 5 tests written first in `src/test/java/...`.
2. **All Tests Green:** Run `mvn test` before submitting changes.
3. **MVC Separation:** Never put network parsing, DMX decoding, or math calculations inside JavaFX UI controllers. Use `de.exit.artnet2rig.core` and `de.exit.artnet2rig.service`.
4. **Icons:** Use Lucide vector icons (`LucideIcon.java`). Never use emojis in the UI.

## 4. How to add a new 3D Fixture Type
1. Define the fixture enum in `de.exit.artnet2rig.core.model.FixtureType`.
2. Implement the DMX channel profile in `de.exit.artnet2rig.core.dmx.FixtureProfile`.
3. Create the 3D mesh representation in `de.exit.artnet2rig.ui.canvas.MeshFactory`.
4. Add comprehensive unit tests in `src/test/java/de/exit/artnet2rig/core/model/`.
