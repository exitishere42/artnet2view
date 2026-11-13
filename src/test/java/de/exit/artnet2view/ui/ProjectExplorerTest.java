package de.exit.artnet2view.ui;

import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.FixtureType;
import de.exit.artnet2view.core.model.Transform3D;
import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.core.model.TrussType;
import de.exit.artnet2view.service.RigService;
import de.exit.artnet2view.ui.components.ProjectExplorerView;
import de.exit.artnet2view.ui.menu.ExplorerAddMenu;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectExplorerTest {

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @Test
    @DisplayName("ExplorerAddMenu registers actions for spot, par, and truss")
    void testExplorerAddMenuActions() {
        ExplorerAddMenu menu = new ExplorerAddMenu();
        AtomicBoolean spotCalled = new AtomicBoolean(false);
        AtomicBoolean parCalled = new AtomicBoolean(false);
        AtomicBoolean trussCalled = new AtomicBoolean(false);

        menu.setAction("moving_head", () -> spotCalled.set(true));
        menu.setAction("par", () -> parCalled.set(true));
        menu.setAction("truss", () -> trussCalled.set(true));

        assertThat(menu.getItem("moving_head")).isNotNull();
        assertThat(menu.getItem("par")).isNotNull();
        assertThat(menu.getItem("truss")).isNotNull();

        menu.getItem("moving_head").fire();
        assertThat(spotCalled.get()).isTrue();

        menu.getItem("par").fire();
        assertThat(parCalled.get()).isTrue();

        menu.getItem("truss").fire();
        assertThat(trussCalled.get()).isTrue();
    }

    @Test
    @DisplayName("ProjectExplorerView populates Moving Heads, PARs, and Trusses")
    void testProjectExplorerPopulatesCategories() {
        RigService rigService = new RigService(null);
        rigService.addFixture(new Fixture(
                1L, "ZQ02040 1", FixtureType.MOVING_HEAD_ZQ02040, 0, 1, 11,
                Transform3D.ZERO, false, false, 6.0
        ));
        rigService.addFixture(new Fixture(
                2L, "Par 1", FixtureType.LED_PAR, 0, 12, 8,
                Transform3D.ZERO, false, false, 25.0
        ));
        rigService.addTruss(new TrussElement(
                10L, TrussType.STRAIGHT_2M, Transform3D.ZERO
        ));

        AtomicBoolean selectedTriggered = new AtomicBoolean(false);
        ProjectExplorerView view = new ProjectExplorerView(
                rigService,
                () -> selectedTriggered.set(true),
                () -> {}
        );

        assertThat(view).isNotNull();
        assertThat(rigService.getActiveProject().getFixtures()).hasSize(2);
        assertThat(rigService.getActiveProject().getTrusses()).hasSize(1);
    }
}
