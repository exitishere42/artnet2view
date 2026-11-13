package de.exit.artnet2view.service;

import de.exit.artnet2view.core.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RigService}.
 */
class RigServiceTest {

    private DatabaseService databaseService;
    private RigRepository repository;
    private RigService rigService;

    @BeforeEach
    void setUp() {
        String dbName = "artnet2view_rigservice_test_" + UUID.randomUUID().toString().replace("-", "");
        databaseService = new DatabaseService("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        databaseService.initSchema();
        repository = new RigRepository(databaseService);
        rigService = new RigService(repository);
    }

    @AfterEach
    void tearDown() {
        databaseService.close();
    }

    @Test
    @DisplayName("RigService creates and selects new fixtures")
    void testAddAndSelectFixture() {
        Fixture spot = new Fixture(
                null,
                "Spot 1",
                FixtureType.MOVING_HEAD_SPOT,
                0,
                1,
                16,
                Transform3D.ZERO,
                false,
                false,
                15.0
        );

        rigService.addFixture(spot);
        assertThat(rigService.getActiveProject().getFixtures()).hasSize(1);

        rigService.setSelectedFixture(spot);
        assertThat(rigService.getSelectedFixture()).isEqualTo(spot);
    }

    @Test
    @DisplayName("RigService removes fixture and clears selection")
    void testRemoveFixture() {
        Fixture spot = new Fixture(
                100L,
                "Spot 1",
                FixtureType.MOVING_HEAD_SPOT,
                0,
                1,
                16,
                Transform3D.ZERO,
                false,
                false,
                15.0
        );

        rigService.addFixture(spot);
        rigService.setSelectedFixture(spot);

        rigService.removeFixture(100L);
        assertThat(rigService.getActiveProject().getFixtures()).isEmpty();
        assertThat(rigService.getSelectedFixture()).isNull();
    }

    @Test
    @DisplayName("RigService persists and reloads active project")
    void testSaveAndLoadProject() {
        rigService.getActiveProject().setName("Festival Stage");
        rigService.addFixture(new Fixture(
                null,
                "Main Spot",
                FixtureType.MOVING_HEAD_SPOT,
                0,
                1,
                16,
                Transform3D.ZERO,
                false,
                false,
                15.0
        ));

        rigService.saveActiveProject();
        Long savedId = rigService.getActiveProject().getId();
        assertThat(savedId).isNotNull();

        rigService.createNewProject("Empty Project");
        assertThat(rigService.getActiveProject().getFixtures()).isEmpty();

        rigService.loadProject(savedId);
        assertThat(rigService.getActiveProject().getName()).isEqualTo("Festival Stage");
        assertThat(rigService.getActiveProject().getFixtures()).hasSize(1);
    }

    @Test
    @DisplayName("RigService adds, updates, selects, and removes trusses")
    void testTrussOperations() {
        TrussElement truss = new TrussElement(1L, TrussType.STRAIGHT_2M, new Transform3D(0, -3.5, 0, 0, 0, 0));
        rigService.addTruss(truss);

        assertThat(rigService.getActiveProject().getTrusses()).hasSize(1);
        assertThat(rigService.getSelectedTruss()).isEqualTo(truss);
        assertThat(rigService.getSelectedFixture()).isNull();

        TrussElement updated = new TrussElement(1L, TrussType.STRAIGHT_2M, new Transform3D(1.0, -3.5, 0, 0, 45, 0));
        rigService.updateTruss(truss, updated);

        assertThat(rigService.getActiveProject().getTrusses()).hasSize(1);
        assertThat(rigService.getSelectedTruss()).isEqualTo(updated);

        rigService.removeTruss(1L);
        assertThat(rigService.getActiveProject().getTrusses()).isEmpty();
        assertThat(rigService.getSelectedTruss()).isNull();
    }
}
