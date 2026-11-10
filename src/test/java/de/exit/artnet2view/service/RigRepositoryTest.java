package de.exit.artnet2view.service;

import de.exit.artnet2view.core.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration and unit tests for {@link DatabaseService} and {@link RigRepository}.
 */
class RigRepositoryTest {

    private DatabaseService databaseService;
    private RigRepository repository;

    @BeforeEach
    void setUp() {
        // Use an isolated unique in-memory H2 database per test execution
        String dbName = "artnet2view_test_" + java.util.UUID.randomUUID().toString().replace("-", "");
        databaseService = new DatabaseService("jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
        databaseService.initSchema();
        repository = new RigRepository(databaseService);
    }

    @AfterEach
    void tearDown() {
        databaseService.close();
    }

    @Test
    @DisplayName("Create, save and load RigProject with Fixtures and Trusses")
    void testSaveAndLoadProject() {
        RigProject project = new RigProject("Main Stage Rig");
        project.setDescription("Arena Festival Setup");

        // Add a Moving Head
        Fixture spot1 = new Fixture(
                null,
                "Spot Left",
                FixtureType.MOVING_HEAD_SPOT,
                0,
                1,
                16,
                new Transform3D(-2.0, 3.5, 1.0, 0, 0, 0),
                false,
                false,
                15.0
        );
        project.addFixture(spot1);

        // Add a Truss segment
        TrussElement truss1 = new TrussElement(
                null,
                TrussType.STRAIGHT_2M,
                new Transform3D(0, 4.0, 0, 0, 0, 0)
        );
        project.addTruss(truss1);

        RigProject saved = repository.save(project);
        assertThat(saved.getId()).isNotNull();

        Optional<RigProject> loadedOpt = repository.findById(saved.getId());
        assertThat(loadedOpt).isPresent();

        RigProject loaded = loadedOpt.get();
        assertThat(loaded.getName()).isEqualTo("Main Stage Rig");
        assertThat(loaded.getDescription()).isEqualTo("Arena Festival Setup");
        assertThat(loaded.getFixtures()).hasSize(1);
        assertThat(loaded.getFixtures().get(0).name()).isEqualTo("Spot Left");
        assertThat(loaded.getFixtures().get(0).type()).isEqualTo(FixtureType.MOVING_HEAD_SPOT);
        assertThat(loaded.getFixtures().get(0).universe()).isEqualTo(0);
        assertThat(loaded.getFixtures().get(0).startChannel()).isEqualTo(1);
        assertThat(loaded.getFixtures().get(0).transform().x()).isEqualTo(-2.0);

        assertThat(loaded.getTrusses()).hasSize(1);
        assertThat(loaded.getTrusses().get(0).type()).isEqualTo(TrussType.STRAIGHT_2M);
        assertThat(loaded.getTrusses().get(0).transform().y()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("List all projects and delete project")
    void testListAndDelete() {
        RigProject p1 = repository.save(new RigProject("Stage A"));
        RigProject p2 = repository.save(new RigProject("Stage B"));

        List<RigProject> all = repository.findAll();
        assertThat(all).hasSize(2);

        repository.delete(p1.getId());
        assertThat(repository.findById(p1.getId())).isEmpty();
        assertThat(repository.findAll()).hasSize(1);
    }
}
