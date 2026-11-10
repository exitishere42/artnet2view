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

        // Position Inputs (X, Y, Z)
        VBox posBox = new VBox(6);
        posBox.getChildren().add(new Label("Position (X, Y, Z in Metern):"));
        GridPane posGrid = new GridPane();
        posGrid.setHgap(6);
        posGrid.setVgap(6);

        Spinner<Double> spinX = new Spinner<>(-20.0, 20.0, fixture.transform().x(), 0.1);
        Spinner<Double> spinY = new Spinner<>(-10.0, 10.0, fixture.transform().y(), 0.1);
        Spinner<Double> spinZ = new Spinner<>(-20.0, 20.0, fixture.transform().z(), 0.1);
        spinX.setPrefWidth(80);
        spinY.setPrefWidth(80);
        spinZ.setPrefWidth(80);

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

        posGrid.add(new Label("X:"), 0, 0);
        posGrid.add(spinX, 1, 0);
        posGrid.add(new Label("Y:"), 0, 1);
        posGrid.add(spinY, 1, 1);
        posGrid.add(new Label("Z:"), 0, 2);
        posGrid.add(spinZ, 1, 2);
        posBox.getChildren().add(posGrid);

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
