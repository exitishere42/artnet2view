package de.exit.artnet2view.ui.components;

import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.Transform3D;
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
 * Inspector dock presenting the properties of the currently selected fixture.
 * Supports fast in-place value synchronization during 3D gizmo drags without rebuilding UI nodes.
 */
public class FixtureInspector extends VBox {

    private final RigService rigService;
    private final Consumer<Fixture> onFixtureUpdated;
    private final Runnable onStageModified;

    private final VBox contentBox;
    private final Label emptyLabel;

    private Fixture activeFixture;
    private boolean suppressSync = false;
    private Spinner<Double> spinX;
    private Spinner<Double> spinY;
    private Spinner<Double> spinZ;
    private Spinner<Double> spinYaw;
    private Spinner<Double> spinPitch;

    public FixtureInspector(RigService rigService, Runnable onStageModified) {
        this(rigService, null, onStageModified);
    }

    public FixtureInspector(RigService rigService, Consumer<Fixture> onFixtureUpdated, Runnable onStageModified) {
        this.rigService = rigService;
        this.onFixtureUpdated = onFixtureUpdated;
        this.onStageModified = onStageModified;

        setPrefWidth(300);
        setMinWidth(280);
        setMaxWidth(340);
        setPadding(new Insets(16));
        setSpacing(12);
        setStyle("-fx-background-color: " + ThemeConstants.BG_CARD + "; -fx-border-color: " + ThemeConstants.BORDER_COLOR + "; -fx-border-width: 0 0 0 1;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label("Inspektor");
        titleLabel.getStyleClass().add("label-title");
        header.getChildren().addAll(
                LucideIcon.create(LucideIcon.SLIDERS, 16, ThemeConstants.PRIMARY_COLOR),
                titleLabel
        );

        this.emptyLabel = new Label("Kein Element ausgewählt.\nKlicke ein Fixture im 3D-Raum an.");
        emptyLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-alignment: center; -fx-text-alignment: center;");
        emptyLabel.setWrapText(true);

        this.contentBox = new VBox(12);

        getChildren().addAll(header, new Separator(), emptyLabel, contentBox);

        rigService.selectedFixtureProperty().addListener((obs, oldFix, newFix) -> {
            if (newFix != null && activeFixture != null && Objects.equals(newFix.id(), activeFixture.id())) {
                syncFromModel(newFix);
            } else {
                updateUI(newFix);
            }
        });
        updateUI(rigService.getSelectedFixture());
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

    private void notifyFixtureUpdated(Fixture updated) {
        this.activeFixture = updated;
        rigService.updateFixture(updated, updated);
        if (onFixtureUpdated != null) {
            onFixtureUpdated.accept(updated);
        } else if (onStageModified != null) {
            onStageModified.run();
        }
    }

    private void updateUI(Fixture fixture) {
        this.activeFixture = fixture;
        this.spinX = null;
        this.spinY = null;
        this.spinZ = null;
        this.spinYaw = null;
        this.spinPitch = null;
        contentBox.getChildren().clear();

        if (fixture == null) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        VBox nameBox = new VBox(4);
        nameBox.getChildren().add(new Label("Name"));
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

        HBox typeBox = new HBox(6);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label(fixture.type().getDisplayName());
        typeLabel.setStyle("-fx-background-color: " + ThemeConstants.BG_ELEVATED + "; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-text-fill: " + ThemeConstants.PRIMARY_COLOR + ";");
        typeBox.getChildren().addAll(new Label("Typ:"), typeLabel);

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

        VBox posBox = new VBox(6);
        posBox.getChildren().add(new Label("Position & Achsen-Pfeile (in Metern):"));
        GridPane posGrid = new GridPane();
        posGrid.setHgap(4);
        posGrid.setVgap(6);

        spinX = new Spinner<>(-20.0, 20.0, fixture.transform().x(), 0.1);
        spinY = new Spinner<>(-10.0, 10.0, fixture.transform().y(), 0.1);
        spinZ = new Spinner<>(-20.0, 20.0, fixture.transform().z(), 0.1);
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
                nameBox,
                typeBox,
                new Separator(),
                patchGrid,
                new Separator(),
                posBox,
                invertBox,
                new Separator(),
                delBtn
        );
    }
}
