package de.exit.artnet2view.service;

import de.exit.artnet2view.core.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for .a2v project file serialization and deserialization.
 */
class A2vProjectServiceTest {

    @Test
    @DisplayName("Should save and load .a2v project file preserving fixtures and trusses")
    void testSaveAndLoadA2vFile(@TempDir Path tempDir) throws IOException {
        A2vProjectService service = new A2vProjectService();

        RigProject original = new RigProject("Festival Main Stage");
        original.setDescription("Outdoor stage with 4 ZQ02040 moving heads and F34 trusses");

        TrussElement truss = new TrussElement(
                1L,
                TrussType.STRAIGHT_2M,
                new Transform3D(0.0, -3.5, 0.0, 0.0, 0.0, 0.0)
        );
        original.addTruss(truss);

        Fixture movingHead = new Fixture(
                10L,
                "UKing ZQ02040 Spot 1",
                FixtureType.MOVING_HEAD_ZQ02040,
                0,
                1,
                11,
                new Transform3D(-2.5, -3.5, 0.0, 0.0, 0.0, 0.0),
                false,
                true,
                6.0
        );
        original.addFixture(movingHead);

        File file = tempDir.resolve("test_stage.a2v").toFile();
        service.saveToFile(original, file);

        assertThat(file).exists();
        assertThat(file.length()).isGreaterThan(50);

        RigProject loaded = service.loadFromFile(file);

        assertThat(loaded.getName()).isEqualTo("Festival Main Stage");
        assertThat(loaded.getDescription()).isEqualTo("Outdoor stage with 4 ZQ02040 moving heads and F34 trusses");
        assertThat(loaded.getTrusses()).hasSize(1);
        assertThat(loaded.getTrusses().get(0).type()).isEqualTo(TrussType.STRAIGHT_2M);
        assertThat(loaded.getFixtures()).hasSize(1);

        Fixture loadedFixture = loaded.getFixtures().get(0);
        assertThat(loadedFixture.name()).isEqualTo("UKing ZQ02040 Spot 1");
        assertThat(loadedFixture.type()).isEqualTo(FixtureType.MOVING_HEAD_ZQ02040);
        assertThat(loadedFixture.startChannel()).isEqualTo(1);
        assertThat(loadedFixture.channelCount()).isEqualTo(11);
        assertThat(loadedFixture.tiltInvert()).isTrue();
    }
}
