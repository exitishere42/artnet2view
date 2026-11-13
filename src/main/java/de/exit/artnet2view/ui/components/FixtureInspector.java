package de.exit.artnet2view.ui.components;

import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.Transform3D;
import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.service.RigService;
import de.exit.artnet2view.ui.theme.LucideIcon;
import de.exit.artnet2view.ui.theme.ThemeConstants;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Inspector dock presenting properties of the currently selected fixture or truss element.
 * Supports fast in-place value synchronization during 3D gizmo drags without rebuilding UI nodes.
 */
public class FixtureInspector extends VBox {

    private final RigService rigService;
    private final Consumer<Fixture> onFixtureUpdated;
    private final Runnable onStageModified;

    private final VBox contentBox;
    private final Label emptyLabel;

    private Fixture activeFixture;
    private TrussElement activeTruss;
    private boolean suppressSync = false;

    private Spinner<Double> spinX;
    private Spinner<Double> spinY;
    private Spinner<Double> spinZ;
    private Spinner<Double> spinYaw;
    private Spinner<Double> spinPitch;
    private Spinner<Double> spinRoll;

    public FixtureInspector(RigService rigService, Runnable onStageModified) {
        this(rigService, null, onStageModified);
    }

    public FixtureInspector(RigService rigService, Consumer<Fixture> onFixtureUpdated, Runnable onStageModified) {
        this.rigService = Objects.requireNonNull(rigService, "rigService must not be null");
        this.onFixtureUpdated = onFixtureUpdated;
        this.onStageModified = onStageModified;

        setPadding(new Insets(12));
        setSpacing(10);
        VBox.setVgrow(this, Priority.ALWAYS);

        this.emptyLabel = new Label("Kein Element ausgewählt.\nWähle ein Element im Project Explorer oder im 3D-Raum aus.");
        emptyLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-alignment: center; -fx-text-alignment: center; -fx-padding: 30 10;");
        emptyLabel.setWrapText(true);

        this.contentBox = new VBox(12);
        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-padding: 0;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        getChildren().addAll(emptyLabel, scrollPane);

        rigService.selectedFixtureProperty().addListener((obs, oldFix, newFix) -> {
            if (newFix != null) {
                if (activeFixture != null && Objects.equals(newFix.id(), activeFixture.id())) {
                    syncFromModel(newFix);
                } else {
                    updateFixtureUI(newFix);
                }
            } else if (rigService.getSelectedTruss() == null) {
                updateEmptyUI();
            }
        });

        rigService.selectedTrussProperty().addListener((obs, oldTruss, newTruss) -> {
            if (newTruss != null) {
                updateTrussUI(newTruss);
            } else if (rigService.getSelectedFixture() == null) {
                updateEmptyUI();
            }
        });

        if (rigService.getSelectedFixture() != null) {
            updateFixtureUI(rigService.getSelectedFixture());
        } else if (rigService.getSelectedTruss() != null) {
            updateTrussUI(rigService.getSelectedTruss());
        } else {
            updateEmptyUI();
        }
    }

    /**
     * Updates spinner values in-place when the active fixture transform changes (e.g. via 3D gizmo drag).
     *
     * @param updated updated fixture snapshot
     */
    public void syncFromModel(Fixture updated) {
        if (updated == null || activeFixture == null || !Objects.equals(updated.id(), activeFixture.id())) {
            return;
        }
        this.activeFixture = updated;
        suppressSync = true;
        try {
            Transform3D t = updated.transform();
            if (spinX != null && spinX.getValueFactory() != null) {
                spinX.getValueFactory().setValue(t.x());
            }
            if (spinY != null && spinY.getValueFactory() != null) {
                spinY.getValueFactory().setValue(t.y());
            }
            if (spinZ != null && spinZ.getValueFactory() != null) {
                spinZ.getValueFactory().setValue(t.z());
            }
            if (spinYaw != null && spinYaw.getValueFactory() != null) {
                spinYaw.getValueFactory().setValue(t.yaw());
            }
            if (spinPitch != null && spinPitch.getValueFactory() != null) {
                spinPitch.getValueFactory().setValue(t.pitch());
            }
        } finally {
            suppressSync = false;
        }
    }

    private void updateEmptyUI() {
        this.activeFixture = null;
        this.activeTruss = null;
        this.spinX = null;
        this.spinY = null;
        this.spinZ = null;
        this.spinYaw = null;
        this.spinPitch = null;
        this.spinRoll = null;
        contentBox.getChildren().clear();
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
    }

    private void notifyFixtureUpdated(Fixture updated) {
        this.activeFixture = updated;
        rigService.updateFixture(updated, updated);
        if (onFixtureUpdated != null) {
            onFixtureUpdated.accept(updated);
        } else if (onStageModified != null) {
            onStageModified.run();
        }
    }

    private void updateFixtureUI(Fixture fixture) {
        this.activeFixture = fixture;
        this.activeTruss = null;
        this.spinX = null;
        this.spinY = null;
        this.spinZ = null;
        this.spinYaw = null;
        this.spinPitch = null;
        this.spinRoll = null;
        contentBox.getChildren().clear();

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        // Header Title / Type
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(fixture.name());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F3F4F6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label typeBadge = new Label(fixture.type().getDisplayName());
        String badgeColor = fixture.type().isMovingHead() ? "#38BDF8" : "#F59E0B";
        typeBadge.setStyle("-fx-background-color: " + ThemeConstants.BG_ELEVATED + "; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: " + badgeColor + ";");
        headerBox.getChildren().addAll(LucideIcon.create(LucideIcon.BOX, 16, badgeColor), titleLabel, spacer, typeBadge);

        // Name Field
        VBox nameBox = new VBox(4);
        nameBox.getChildren().add(new Label("Name:"));
        TextField nameField = new TextField(fixture.name());
        nameField.textProperty().addListener((obs, o, n) -> {
            if (!suppressSync && !n.isBlank() && activeFixture != null) {
                Fixture updated = new Fixture(
                        activeFixture.id(), n, activeFixture.type(), activeFixture.universe(),
                        activeFixture.startChannel(), activeFixture.channelCount(), activeFixture.transform(),
                        activeFixture.panInvert(), activeFixture.tiltInvert(), activeFixture.beamAngle()
                );
                notifyFixtureUpdated(updated);
            }
        });
        nameBox.getChildren().add(nameField);

        // DMX Patch Grid
        GridPane patchGrid = new GridPane();
        patchGrid.setHgap(8);
        patchGrid.setVgap(8);

        patchGrid.add(new Label("Universum (0-15):"), 0, 0);
        Spinner<Integer> uniSpinner = new Spinner<>(0, 15, fixture.universe());
        uniSpinner.setPrefWidth(90);
        uniSpinner.valueProperty().addListener((obs, o, n) -> {
            if (!suppressSync && activeFixture != null) {
                Fixture updated = new Fixture(
                        activeFixture.id(), activeFixture.name(), activeFixture.type(), n,
                        activeFixture.startChannel(), activeFixture.channelCount(), activeFixture.transform(),
                        activeFixture.panInvert(), activeFixture.tiltInvert(), activeFixture.beamAngle()
                );
                notifyFixtureUpdated(updated);
            }
        });
        patchGrid.add(uniSpinner, 1, 0);

        patchGrid.add(new Label("Startadresse (1-512):"), 0, 1);
        int maxStart = 512 - fixture.channelCount() + 1;
        Spinner<Integer> chSpinner = new Spinner<>(1, Math.max(1, maxStart), fixture.startChannel());
        chSpinner.setPrefWidth(90);
        chSpinner.valueProperty().addListener((obs, o, n) -> {
            if (!suppressSync && activeFixture != null) {
                Fixture updated = new Fixture(
                        activeFixture.id(), activeFixture.name(), activeFixture.type(), activeFixture.universe(),
                        n, activeFixture.channelCount(), activeFixture.transform(),
                        activeFixture.panInvert(), activeFixture.tiltInvert(), activeFixture.beamAngle()
                );
                notifyFixtureUpdated(updated);
            }
        });
        patchGrid.add(chSpinner, 1, 1);

        // Position Box
        VBox posBox = new VBox(6);
        posBox.getChildren().add(new Label("Position (in Metern):"));
        GridPane posGrid = new GridPane();
        posGrid.setHgap(4);
        posGrid.setVgap(6);

        spinX = new Spinner<>(-50.0, 50.0, fixture.transform().x(), 0.1);
        spinY = new Spinner<>(-20.0, 20.0, fixture.transform().y(), 0.1);
        spinZ = new Spinner<>(-50.0, 50.0, fixture.transform().z(), 0.1);
        spinX.setPrefWidth(72);
        spinY.setPrefWidth(72);
        spinZ.setPrefWidth(72);

        javafx.beans.value.ChangeListener<Double> posListener = (obs, o, n) -> {
            if (suppressSync || activeFixture == null) {
                return;
            }
            Transform3D t = activeFixture.transform().withPosition(spinX.getValue(), spinY.getValue(), spinZ.getValue());
            Fixture updated = new Fixture(
                    activeFixture.id(), activeFixture.name(), activeFixture.type(), activeFixture.universe(),
                    activeFixture.startChannel(), activeFixture.channelCount(), t,
                    activeFixture.panInvert(), activeFixture.tiltInvert(), activeFixture.beamAngle()
            );
            notifyFixtureUpdated(updated);
        };
        spinX.valueProperty().addListener(posListener);
        spinY.valueProperty().addListener(posListener);
        spinZ.valueProperty().addListener(posListener);

        Button btnLeftX = new Button("◀");
        btnLeftX.setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-padding: 3px 6px;");
        btnLeftX.setOnAction(e -> spinX.decrement(5));

        Button btnRightX = new Button("▶");
        btnRightX.setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-padding: 3px 6px;");
        btnRightX.setOnAction(e -> spinX.increment(5));

        Button btnDownY = new Button("▼");
        btnDownY.setStyle("-fx-font-size: 10px; -fx-text-fill: #22C55E; -fx-padding: 3px 6px;");
        btnDownY.setOnAction(e -> spinY.decrement(5));

        Button btnUpY = new Button("▲");
        btnUpY.setStyle("-fx-font-size: 10px; -fx-text-fill: #22C55E; -fx-padding: 3px 6px;");
        btnUpY.setOnAction(e -> spinY.increment(5));

        Button btnBackZ = new Button("◀");
        btnBackZ.setStyle("-fx-font-size: 10px; -fx-text-fill: #3B82F6; -fx-padding: 3px 6px;");
        btnBackZ.setOnAction(e -> spinZ.decrement(5));

        Button btnForwardZ = new Button("▶");
        btnForwardZ.setStyle("-fx-font-size: 10px; -fx-text-fill: #3B82F6; -fx-padding: 3px 6px;");
        btnForwardZ.setOnAction(e -> spinZ.increment(5));

        Label lblX = new Label("X:");
        lblX.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        Label lblY = new Label("Y:");
        lblY.setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;");
        Label lblZ = new Label("Z:");
        lblZ.setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold;");

        posGrid.add(lblX, 0, 0);
        posGrid.add(btnLeftX, 1, 0);
        posGrid.add(spinX, 2, 0);
        posGrid.add(btnRightX, 3, 0);

        posGrid.add(lblY, 0, 1);
        posGrid.add(btnDownY, 1, 1);
        posGrid.add(spinY, 2, 1);
        posGrid.add(btnUpY, 3, 1);

        posGrid.add(lblZ, 0, 2);
        posGrid.add(btnBackZ, 1, 2);
        posGrid.add(spinZ, 2, 2);
        posGrid.add(btnForwardZ, 3, 2);

        posBox.getChildren().add(posGrid);

        // Rotation (Moving Head)
        if (fixture.type().isMovingHead()) {
            VBox rotBox = new VBox(4);
            rotBox.getChildren().add(new Label("Pan / Tilt Ausrichtung:"));
            GridPane rotGrid = new GridPane();
            rotGrid.setHgap(4);
            rotGrid.setVgap(4);

            spinYaw = new Spinner<>(-180.0, 180.0, fixture.transform().yaw(), 5.0);
            spinPitch = new Spinner<>(-90.0, 90.0, fixture.transform().pitch(), 5.0);
            spinYaw.setPrefWidth(72);
            spinPitch.setPrefWidth(72);

            javafx.beans.value.ChangeListener<Double> rotListener = (obs, o, n) -> {
                if (suppressSync || activeFixture == null) {
                    return;
                }
                Transform3D t = activeFixture.transform().withRotation(spinPitch.getValue(), spinYaw.getValue(), activeFixture.transform().roll());
                Fixture updated = new Fixture(
                        activeFixture.id(), activeFixture.name(), activeFixture.type(), activeFixture.universe(),
                        activeFixture.startChannel(), activeFixture.channelCount(), t,
                        activeFixture.panInvert(), activeFixture.tiltInvert(), activeFixture.beamAngle()
                );
                notifyFixtureUpdated(updated);
            };
            spinYaw.valueProperty().addListener(rotListener);
            spinPitch.valueProperty().addListener(rotListener);

            Button btnYawLeft = new Button("◀");
            btnYawLeft.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px;");
            btnYawLeft.setOnAction(e -> spinYaw.decrement(1));

            Button btnYawRight = new Button("▶");
            btnYawRight.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px;");
            btnYawRight.setOnAction(e -> spinYaw.increment(1));

            Button btnPitchDown = new Button("▼");
            btnPitchDown.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px;");
            btnPitchDown.setOnAction(e -> spinPitch.decrement(1));

            Button btnPitchUp = new Button("▲");
            btnPitchUp.setStyle("-fx-font-size: 10px; -fx-padding: 3px 6px;");
            btnPitchUp.setOnAction(e -> spinPitch.increment(1));

            rotGrid.add(new Label("Pan:"), 0, 0);
            rotGrid.add(btnYawLeft, 1, 0);
            rotGrid.add(spinYaw, 2, 0);
            rotGrid.add(btnYawRight, 3, 0);

            rotGrid.add(new Label("Tilt:"), 0, 1);
            rotGrid.add(btnPitchDown, 1, 1);
            rotGrid.add(spinPitch, 2, 1);
            rotGrid.add(btnPitchUp, 3, 1);

            rotBox.getChildren().add(rotGrid);
            posBox.getChildren().add(rotBox);
        }

        // Invert Checkboxes
        VBox invertBox = new VBox(6);
        CheckBox chkPanInvert = new CheckBox("Pan invertieren");
        chkPanInvert.setSelected(fixture.panInvert());
        chkPanInvert.setOnAction(e -> {
            if (activeFixture != null) {
                Fixture updated = new Fixture(
                        activeFixture.id(), activeFixture.name(), activeFixture.type(), activeFixture.universe(),
                        activeFixture.startChannel(), activeFixture.channelCount(), activeFixture.transform(),
                        chkPanInvert.isSelected(), activeFixture.tiltInvert(), activeFixture.beamAngle()
                );
                notifyFixtureUpdated(updated);
            }
        });

        CheckBox chkTiltInvert = new CheckBox("Tilt invertieren");
        chkTiltInvert.setSelected(fixture.tiltInvert());
        chkTiltInvert.setOnAction(e -> {
            if (activeFixture != null) {
                Fixture updated = new Fixture(
                        activeFixture.id(), activeFixture.name(), activeFixture.type(), activeFixture.universe(),
                        activeFixture.startChannel(), activeFixture.channelCount(), activeFixture.transform(),
                        activeFixture.panInvert(), chkTiltInvert.isSelected(), activeFixture.beamAngle()
                );
                notifyFixtureUpdated(updated);
            }
        });
        invertBox.getChildren().addAll(chkPanInvert, chkTiltInvert);

        // Delete Button
        Button delBtn = new Button("Fixture löschen");
        delBtn.setGraphic(LucideIcon.create(LucideIcon.TRASH_2, 14, "#FF5252"));
        delBtn.setStyle("-fx-border-color: #FF5252; -fx-text-fill: #FF5252;");
        delBtn.setMaxWidth(Double.MAX_VALUE);
        delBtn.setOnAction(e -> {
            rigService.removeFixture(fixture.id());
            if (onStageModified != null) {
                onStageModified.run();
            }
        });

        contentBox.getChildren().addAll(
                headerBox,
                nameBox,
                new Separator(),
                patchGrid,
                new Separator(),
                posBox,
                invertBox,
                new Separator(),
                delBtn
        );
    }

    private void updateTrussUI(TrussElement truss) {
        this.activeTruss = truss;
        this.activeFixture = null;
        this.spinX = null;
        this.spinY = null;
        this.spinZ = null;
        this.spinYaw = null;
        this.spinPitch = null;
        this.spinRoll = null;
        contentBox.getChildren().clear();

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        // Header Title / Type
        HBox headerBox = new HBox(8);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Traverse #" + truss.id());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F3F4F6;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label typeBadge = new Label(truss.type().getDisplayName());
        typeBadge.setStyle("-fx-background-color: " + ThemeConstants.BG_ELEVATED + "; -fx-padding: 2px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #A78BFA;");
        headerBox.getChildren().addAll(LucideIcon.create(LucideIcon.LAYERS, 16, "#A78BFA"), titleLabel, spacer, typeBadge);

        // Position Box
        VBox posBox = new VBox(6);
        posBox.getChildren().add(new Label("Position (in Metern):"));
        GridPane posGrid = new GridPane();
        posGrid.setHgap(4);
        posGrid.setVgap(6);

        spinX = new Spinner<>(-50.0, 50.0, truss.transform().x(), 0.1);
        spinY = new Spinner<>(-20.0, 20.0, truss.transform().y(), 0.1);
        spinZ = new Spinner<>(-50.0, 50.0, truss.transform().z(), 0.1);
        spinX.setPrefWidth(72);
        spinY.setPrefWidth(72);
        spinZ.setPrefWidth(72);

        javafx.beans.value.ChangeListener<Double> posListener = (obs, o, n) -> {
            if (suppressSync || activeTruss == null) {
                return;
            }
            Transform3D t = activeTruss.transform().withPosition(spinX.getValue(), spinY.getValue(), spinZ.getValue());
            TrussElement updated = new TrussElement(activeTruss.id(), activeTruss.type(), t);
            rigService.updateTruss(activeTruss, updated);
            this.activeTruss = updated;
            if (onStageModified != null) {
                onStageModified.run();
            }
        };
        spinX.valueProperty().addListener(posListener);
        spinY.valueProperty().addListener(posListener);
        spinZ.valueProperty().addListener(posListener);

        Button btnLeftX = new Button("◀");
        btnLeftX.setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-padding: 3px 6px;");
        btnLeftX.setOnAction(e -> spinX.decrement(5));

        Button btnRightX = new Button("▶");
        btnRightX.setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-padding: 3px 6px;");
        btnRightX.setOnAction(e -> spinX.increment(5));

        Button btnDownY = new Button("▼");
        btnDownY.setStyle("-fx-font-size: 10px; -fx-text-fill: #22C55E; -fx-padding: 3px 6px;");
        btnDownY.setOnAction(e -> spinY.decrement(5));

        Button btnUpY = new Button("▲");
        btnUpY.setStyle("-fx-font-size: 10px; -fx-text-fill: #22C55E; -fx-padding: 3px 6px;");
        btnUpY.setOnAction(e -> spinY.increment(5));

        Button btnBackZ = new Button("◀");
        btnBackZ.setStyle("-fx-font-size: 10px; -fx-text-fill: #3B82F6; -fx-padding: 3px 6px;");
        btnBackZ.setOnAction(e -> spinZ.decrement(5));

        Button btnForwardZ = new Button("▶");
        btnForwardZ.setStyle("-fx-font-size: 10px; -fx-text-fill: #3B82F6; -fx-padding: 3px 6px;");
        btnForwardZ.setOnAction(e -> spinZ.increment(5));

        Label lblX = new Label("X:");
        lblX.setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;");
        Label lblY = new Label("Y:");
        lblY.setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;");
        Label lblZ = new Label("Z:");
        lblZ.setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold;");

        posGrid.add(lblX, 0, 0);
        posGrid.add(btnLeftX, 1, 0);
        posGrid.add(spinX, 2, 0);
        posGrid.add(btnRightX, 3, 0);

        posGrid.add(lblY, 0, 1);
        posGrid.add(btnDownY, 1, 1);
        posGrid.add(spinY, 2, 1);
        posGrid.add(btnUpY, 3, 1);

        posGrid.add(lblZ, 0, 2);
        posGrid.add(btnBackZ, 1, 2);
        posGrid.add(spinZ, 2, 2);
        posGrid.add(btnForwardZ, 3, 2);

        posBox.getChildren().add(posGrid);

        // Rotation Box
        VBox rotBox = new VBox(4);
        rotBox.getChildren().add(new Label("Rotation (Yaw / Pitch / Roll in Grad):"));
        GridPane rotGrid = new GridPane();
        rotGrid.setHgap(4);
        rotGrid.setVgap(4);

        spinYaw = new Spinner<>(-180.0, 180.0, truss.transform().yaw(), 5.0);
        spinPitch = new Spinner<>(-180.0, 180.0, truss.transform().pitch(), 5.0);
        spinRoll = new Spinner<>(-180.0, 180.0, truss.transform().roll(), 5.0);
        spinYaw.setPrefWidth(72);
        spinPitch.setPrefWidth(72);
        spinRoll.setPrefWidth(72);

        javafx.beans.value.ChangeListener<Double> rotListener = (obs, o, n) -> {
            if (suppressSync || activeTruss == null) {
                return;
            }
            Transform3D t = new Transform3D(
                    activeTruss.transform().x(),
                    activeTruss.transform().y(),
                    activeTruss.transform().z(),
                    spinPitch.getValue(),
                    spinYaw.getValue(),
                    spinRoll.getValue()
            );
            TrussElement updated = new TrussElement(activeTruss.id(), activeTruss.type(), t);
            rigService.updateTruss(activeTruss, updated);
            this.activeTruss = updated;
            if (onStageModified != null) {
                onStageModified.run();
            }
        };
        spinYaw.valueProperty().addListener(rotListener);
        spinPitch.valueProperty().addListener(rotListener);
        spinRoll.valueProperty().addListener(rotListener);

        rotGrid.add(new Label("Yaw:"), 0, 0);
        rotGrid.add(spinYaw, 1, 0);

        rotGrid.add(new Label("Pitch:"), 0, 1);
        rotGrid.add(spinPitch, 1, 1);

        rotGrid.add(new Label("Roll:"), 0, 2);
        rotGrid.add(spinRoll, 1, 2);

        rotBox.getChildren().add(rotGrid);

        // Delete Truss Button
        Button delBtn = new Button("Traverse löschen");
        delBtn.setGraphic(LucideIcon.create(LucideIcon.TRASH_2, 14, "#FF5252"));
        delBtn.setStyle("-fx-border-color: #FF5252; -fx-text-fill: #FF5252;");
        delBtn.setMaxWidth(Double.MAX_VALUE);
        delBtn.setOnAction(e -> {
            rigService.removeTruss(truss.id());
            if (onStageModified != null) {
                onStageModified.run();
            }
        });

        contentBox.getChildren().addAll(
                headerBox,
                new Separator(),
                posBox,
                new Separator(),
                rotBox,
                new Separator(),
                delBtn
        );
    }
}
