package de.exit.artnet2view.ui.components;

import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.FixtureType;
import de.exit.artnet2view.core.model.Transform3D;
import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.core.model.TrussType;
import de.exit.artnet2view.service.RigService;
import de.exit.artnet2view.ui.menu.ExplorerAddMenu;
import de.exit.artnet2view.ui.theme.LucideIcon;
import de.exit.artnet2view.ui.theme.ThemeConstants;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Right-side Dock Container featuring a two-tab navigation system:
 * - Tab 1: Project Explorer (hierarchical tree of Moving Heads, PAR lights, and Trusses)
 * - Tab 2: Inspector (detailed property inspector for selected Fixture or Truss)
 * Includes top-right JDMX-style '+' Add Button opening ExplorerAddMenu.
 */
public class RightDockView extends VBox {

    private final RigService rigService;
    private final Supplier<Long> idGenerator;
    private final Runnable onStageModified;

    private final ToggleButton btnExplorer;
    private final ToggleButton btnInspector;
    private final Button btnAdd;
    private final ExplorerAddMenu addMenu;

    private final ProjectExplorerView projectExplorerView;
    private final FixtureInspector fixtureInspector;

    public RightDockView(RigService rigService,
                         Consumer<Fixture> onFixtureUpdated,
                         Runnable onStageModified,
                         Supplier<Long> idGenerator) {
        this.rigService = Objects.requireNonNull(rigService, "rigService must not be null");
        this.onStageModified = onStageModified;
        this.idGenerator = idGenerator != null ? idGenerator : () -> System.currentTimeMillis();

        setPrefWidth(320);
        setMinWidth(300);
        setMaxWidth(360);
        setStyle("-fx-background-color: " + ThemeConstants.BG_CARD + "; -fx-border-color: " + ThemeConstants.BORDER_COLOR + "; -fx-border-width: 0 0 0 1;");
        VBox.setVgrow(this, Priority.ALWAYS);

        // 1. Top Header with Tabs & Add-Button
        this.btnExplorer = new ToggleButton("Project Explorer");
        btnExplorer.getStyleClass().add("dock-tab-btn");
        btnExplorer.setGraphic(LucideIcon.create(LucideIcon.BOX, 14, "#9CA3AF"));

        this.btnInspector = new ToggleButton("Inspector");
        btnInspector.getStyleClass().add("dock-tab-btn");
        btnInspector.setGraphic(LucideIcon.create(LucideIcon.SLIDERS, 14, "#9CA3AF"));

        ToggleGroup tabGroup = new ToggleGroup();
        btnExplorer.setToggleGroup(tabGroup);
        btnInspector.setToggleGroup(tabGroup);
        btnExplorer.setSelected(true);

        HBox tabButtonGroup = new HBox(4);
        tabButtonGroup.setAlignment(Pos.CENTER_LEFT);
        tabButtonGroup.getChildren().addAll(btnExplorer, btnInspector);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.addMenu = new ExplorerAddMenu();
        initAddActions();

        this.btnAdd = new Button();
        btnAdd.getStyleClass().add("project-explorer-add-btn");
        btnAdd.setGraphic(LucideIcon.create(LucideIcon.PLUS, 13, "#10B981"));
        btnAdd.setOnAction(e -> {
            if (addMenu.isShowing()) {
                addMenu.hide();
            } else {
                Bounds bounds = btnAdd.localToScreen(btnAdd.getBoundsInLocal());
                if (bounds != null && btnAdd.getScene() != null && btnAdd.getScene().getWindow() != null) {
                    addMenu.show(btnAdd.getScene().getWindow(), bounds.getMinX(), bounds.getMaxY() + 4);
                }
            }
        });

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-border-color: " + ThemeConstants.BORDER_COLOR + "; -fx-border-width: 0 0 1 0;");
        header.getChildren().addAll(tabButtonGroup, spacer, btnAdd);

        // 2. Views
        this.projectExplorerView = new ProjectExplorerView(rigService, this::showInspectorTab, onStageModified);
        this.fixtureInspector = new FixtureInspector(rigService, onFixtureUpdated, onStageModified);

        StackPane contentPane = new StackPane(projectExplorerView, fixtureInspector);
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        getChildren().addAll(header, contentPane);

        // Tab Switching Logic
        btnExplorer.setOnAction(e -> {
            if (!btnExplorer.isSelected()) {
                btnExplorer.setSelected(true);
            }
            showExplorerTab();
        });

        btnInspector.setOnAction(e -> {
            if (!btnInspector.isSelected()) {
                btnInspector.setSelected(true);
            }
            showInspectorTab();
        });

        // Auto-switch to Inspector on selection
        rigService.selectedFixtureProperty().addListener((obs, oldF, newF) -> {
            if (newF != null) {
                showInspectorTab();
            }
        });
        rigService.selectedTrussProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                showInspectorTab();
            }
        });

        showExplorerTab();
    }

    private void initAddActions() {
        addMenu.setAction("moving_head", () -> {
            long id = idGenerator.get();
            Fixture spot = new Fixture(
                    id,
                    "ZQ02040 " + id,
                    FixtureType.MOVING_HEAD_ZQ02040,
                    0,
                    1,
                    11,
                    new Transform3D(0, -3.5, 0, 0, 0, 0),
                    false,
                    false,
                    6.0
            );
            rigService.addFixture(spot);
            if (onStageModified != null) {
                onStageModified.run();
            }
            rebuildExplorer();
            showInspectorTab();
        });

        addMenu.setAction("par", () -> {
            long id = idGenerator.get();
            Fixture par = new Fixture(
                    id,
                    "Par " + id,
                    FixtureType.LED_PAR,
                    0,
                    1,
                    8,
                    new Transform3D(0, -0.2, 0, 0, 0, 0),
                    false,
                    false,
                    25.0
            );
            rigService.addFixture(par);
            if (onStageModified != null) {
                onStageModified.run();
            }
            rebuildExplorer();
            showInspectorTab();
        });

        addMenu.setAction("truss", () -> {
            long id = idGenerator.get();
            TrussElement truss = new TrussElement(
                    id,
                    TrussType.STRAIGHT_2M,
                    new Transform3D(0, -3.5, 0, 0, 0, 0)
            );
            rigService.addTruss(truss);
            if (onStageModified != null) {
                onStageModified.run();
            }
            rebuildExplorer();
            showInspectorTab();
        });
    }

    public void showExplorerTab() {
        btnExplorer.setSelected(true);
        projectExplorerView.setVisible(true);
        projectExplorerView.setManaged(true);
        fixtureInspector.setVisible(false);
        fixtureInspector.setManaged(false);
    }

    public void showInspectorTab() {
        btnInspector.setSelected(true);
        fixtureInspector.setVisible(true);
        fixtureInspector.setManaged(true);
        projectExplorerView.setVisible(false);
        projectExplorerView.setManaged(false);
    }

    public void rebuildExplorer() {
        projectExplorerView.rebuildTree();
    }

    public FixtureInspector getInspector() {
        return fixtureInspector;
    }

    public ProjectExplorerView getProjectExplorerView() {
        return projectExplorerView;
    }
}
