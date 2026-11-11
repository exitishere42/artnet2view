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

/**
 * Inspector dock presenting the properties of the currently selected fixture.
 */
public class FixtureInspector extends VBox {

    private final RigService rigService;
    private final Runnable onStageModified;

    private final VBox contentBox;
    private final Label emptyLabel;

    public FixtureInspector(RigService rigService, Runnable onStageModified) {
        this.rigService = rigService;
        this.onStageModified = onStageModified;

        setPrefWidth(300);
        setMinWidth(280);
        setMaxWidth(340);
        setPadding(new Insets(16));
        setSpacing(12);
        setStyle("-fx-background-color: " + ThemeConstants.BG_CARD + "; -fx-border-color: " + ThemeConstants.BORDER_COLOR + "; -fx-border-width: 0 0 0 1;");

        // Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(
                LucideIcon.create(LucideIcon.SLIDERS, 16, ThemeConstants.PRIMARY_COLOR),
                new Label("Inspektor") {{
                    getStyleClass().add("label-title");
                }}
        );

        this.emptyLabel = new Label("Kein Element ausgewählt.\nKlicke ein Fixture im 3D-Raum an.");
        emptyLabel.setStyle("-fx-text-fill: #9E9E9E; -fx-alignment: center; -fx-text-alignment: center;");
        emptyLabel.setWrapText(true);

        this.contentBox = new VBox(12);

        getChildren().addAll(header, new Separator(), emptyLabel, contentBox);

        // Listen to selection changes
        rigService.selectedFixtureProperty().addListener((obs, oldFix, newFix) -> updateUI(newFix));
        updateUI(rigService.getSelectedFixture());
    }

    private void updateUI(Fixture fixture) {
        contentBox.getChildren().clear();

        if (fixture == null) {
            emptyLabel.setVisible(true);
            emptyLabel.setManaged(true);
            return;
        }

        emptyLabel.setVisible(false);
        emptyLabel.setManaged(false);

        // Fixture Name
        VBox nameBox = new VBox(4);
        nameBox.getChildren().add(new Label("Name"));
        TextField nameField = new TextField(fixture.name());
        nameField.textProperty().addListener((obs, o, n) -> {
            if (!n.isBlank()) {
                Fixture updated = new Fixture(
                        fixture.id(), n, fixture.type(), fixture.universe(),
                        fixture.startChannel(), fixture.channelCount(), fixture.transform(),
                        fixture.panInvert(), fixture.tiltInvert(), fixture.beamAngle()
                );
                rigService.updateFixture(fixture, updated);
            }
        });
        nameBox.getChildren().add(nameField);

        // Type Badge
        HBox typeBox = new HBox(6);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label(fixture.type().getDisplayName());
        typeLabel.setStyle("-fx-background-color: " + ThemeConstants.BG_ELEVATED + "; -fx-padding: 3px 8px; -fx-background-radius: 4px; -fx-font-weight: bold; -fx-text-fill: " + ThemeConstants.PRIMARY_COLOR + ";");
        typeBox.getChildren().addAll(new Label("Typ:"), typeLabel);

        // DMX Patching Grid
        GridPane patchGrid = new GridPane();
        patchGrid.setHgap(8);
        patchGrid.setVgap(8);

        patchGrid.add(new Label("Universum (0-15):"), 0, 0);
        Spinner<Integer> uniSpinner = new Spinner<>(0, 15, fixture.universe());
        uniSpinner.setPrefWidth(90);
        uniSpinner.valueProperty().addListener((obs, o, n) -> {
            Fixture updated = new Fixture(
                    fixture.id(), fixture.name(), fixture.type(), n,
                    fixture.startChannel(), fixture.channelCount(), fixture.transform(),
                    fixture.panInvert(), fixture.tiltInvert(), fixture.beamAngle()
            );
            rigService.updateFixture(fixture, updated);
        });
        patchGrid.add(uniSpinner, 1, 0);

        patchGrid.add(new Label("Startadresse (1-512):"), 0, 1);
        int maxStart = 512 - fixture.channelCount() + 1;
        Spinner<Integer> chSpinner = new Spinner<>(1, Math.max(1, maxStart), fixture.startChannel());
        chSpinner.setPrefWidth(90);
        chSpinner.valueProperty().addListener((obs, o, n) -> {
            Fixture updated = new Fixture(
                    fixture.id(), fixture.name(), fixture.type(), fixture.universe(),
                    n, fixture.channelCount(), fixture.transform(),
                    fixture.panInvert(), fixture.tiltInvert(), fixture.beamAngle()
            );
            rigService.updateFixture(fixture, updated);
        });
        patchGrid.add(chSpinner, 1, 1);

        // Position Inputs (X, Y, Z) with Axis Arrow Buttons (like JDMX)
        VBox posBox = new VBox(6);
        posBox.getChildren().add(new Label("Position & Achsen-Pfeile (in Metern):"));
        GridPane posGrid = new GridPane();
        posGrid.setHgap(4);
        posGrid.setVgap(6);

        Spinner<Double> spinX = new Spinner<>(-20.0, 20.0, fixture.transform().x(), 0.1);
        Spinner<Double> spinY = new Spinner<>(-10.0, 10.0, fixture.transform().y(), 0.1);
        Spinner<Double> spinZ = new Spinner<>(-20.0, 20.0, fixture.transform().z(), 0.1);
        spinX.setPrefWidth(72);
        spinY.setPrefWidth(72);
        spinZ.setPrefWidth(72);

        javafx.beans.value.ChangeListener<Double> posListener = (obs, o, n) -> {
            Transform3D t = fixture.transform().withPosition(spinX.getValue(), spinY.getValue(), spinZ.getValue());
            Fixture updated = new Fixture(
                    fixture.id(), fixture.name(), fixture.type(), fixture.universe(),
                    fixture.startChannel(), fixture.channelCount(), t,
                    fixture.panInvert(), fixture.tiltInvert(), fixture.beamAngle()
            );
            rigService.updateFixture(fixture, updated);
            if (onStageModified != null) {
                onStageModified.run();
            }
        };
        spinX.valueProperty().addListener(posListener);
        spinY.valueProperty().addListener(posListener);
        spinZ.valueProperty().addListener(posListener);

        // Axis Arrow Nudge Buttons (Red=X, Green=Y, Blue=Z)
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

        posGrid.add(new Label("X:") {{ setStyle("-fx-text-fill: #EF4444; -fx-font-weight: bold;"); }}, 0, 0);
        posGrid.add(btnLeftX, 1, 0);
        posGrid.add(spinX, 2, 0);
        posGrid.add(btnRightX, 3, 0);

        posGrid.add(new Label("Y:") {{ setStyle("-fx-text-fill: #22C55E; -fx-font-weight: bold;"); }}, 0, 1);
        posGrid.add(btnDownY, 1, 1);
        posGrid.add(spinY, 2, 1);
        posGrid.add(btnUpY, 3, 1);

        posGrid.add(new Label("Z:") {{ setStyle("-fx-text-fill: #3B82F6; -fx-font-weight: bold;"); }}, 0, 2);
        posGrid.add(btnBackZ, 1, 2);
        posGrid.add(spinZ, 2, 2);
        posGrid.add(btnForwardZ, 3, 2);

        posBox.getChildren().add(posGrid);

        // Rotation & Orientation for Moving Heads
        if (fixture.type().isMovingHead()) {
            VBox rotBox = new VBox(4);
            rotBox.getChildren().add(new Label("Pan / Tilt Ausrichtung:"));
            GridPane rotGrid = new GridPane();
            rotGrid.setHgap(4);
            rotGrid.setVgap(4);

            Spinner<Double> spinYaw = new Spinner<>(-180.0, 180.0, fixture.transform().yaw(), 5.0);
            Spinner<Double> spinPitch = new Spinner<>(-90.0, 90.0, fixture.transform().pitch(), 5.0);
            spinYaw.setPrefWidth(72);
            spinPitch.setPrefWidth(72);

            javafx.beans.value.ChangeListener<Double> rotListener = (obs, o, n) -> {
                Transform3D t = fixture.transform().withRotation(spinPitch.getValue(), spinYaw.getValue(), fixture.transform().roll());
                Fixture updated = new Fixture(
                        fixture.id(), fixture.name(), fixture.type(), fixture.universe(),
                        fixture.startChannel(), fixture.channelCount(), t,
                        fixture.panInvert(), fixture.tiltInvert(), fixture.beamAngle()
                );
                rigService.updateFixture(fixture, updated);
                if (onStageModified != null) {
                    onStageModified.run();
                }
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

        // Invert Toggles
        VBox invertBox = new VBox(6);
        CheckBox chkPanInvert = new CheckBox("Pan invertieren");
        chkPanInvert.setSelected(fixture.panInvert());
        chkPanInvert.setOnAction(e -> {
            Fixture updated = new Fixture(
                    fixture.id(), fixture.name(), fixture.type(), fixture.universe(),
                    fixture.startChannel(), fixture.channelCount(), fixture.transform(),
                    chkPanInvert.isSelected(), fixture.tiltInvert(), fixture.beamAngle()
            );
            rigService.updateFixture(fixture, updated);
        });

        CheckBox chkTiltInvert = new CheckBox("Tilt invertieren");
        chkTiltInvert.setSelected(fixture.tiltInvert());
        chkTiltInvert.setOnAction(e -> {
            Fixture updated = new Fixture(
                    fixture.id(), fixture.name(), fixture.type(), fixture.universe(),
                    fixture.startChannel(), fixture.channelCount(), fixture.transform(),
                    fixture.panInvert(), chkTiltInvert.isSelected(), fixture.beamAngle()
            );
            rigService.updateFixture(fixture, updated);
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
