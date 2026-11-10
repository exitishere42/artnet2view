package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.core.model.TrussType;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;

/**
 * Procedural 3D Mesh generator for F34 professional aluminum lighting trusses and rigging hardware.
 */
public final class MeshFactory {

    private static final PhongMaterial ALUMINUM_MAT = new PhongMaterial(Color.web("#B0BEC5"));

    static {
        ALUMINUM_MAT.setSpecularColor(Color.web("#ECEFF1"));
    }

    private MeshFactory() {
    }

    /**
     * Creates a textured 3D Node representing an F34 Truss element.
     *
     * @param truss truss entity with type and coordinates
     * @return ready-to-render JavaFX 3D Group
     */
    public static Group createTrussNode(TrussElement truss) {
        Group node = new Group();

        // Position and rotations
        node.setTranslateX(truss.transform().x());
        node.setTranslateY(truss.transform().y());
        node.setTranslateZ(truss.transform().z());

        node.getTransforms().addAll(
                new Rotate(truss.transform().yaw(), Rotate.Y_AXIS),
                new Rotate(truss.transform().pitch(), Rotate.X_AXIS),
                new Rotate(truss.transform().roll(), Rotate.Z_AXIS)
        );

        if (truss.type() == TrussType.BASE_PLATE) {
            Box plate = new Box(0.8, 0.02, 0.8);
            plate.setMaterial(ALUMINUM_MAT);
            node.getChildren().add(plate);
            return node;
        }

        double length = truss.type().getLengthMeters();
        double chordOffset = 0.145; // 290mm square F34 center-to-center distance = 0.29m / 2
        double mainChordRadius = 0.025; // 50mm tube diameter

        // 4 Main Chords along X axis
        for (double dy : new double[]{-chordOffset, chordOffset}) {
            for (double dz : new double[]{-chordOffset, chordOffset}) {
                Cylinder chord = new Cylinder(mainChordRadius, length);
                chord.setMaterial(ALUMINUM_MAT);
                // Rotate cylinder from Y-axis to X-axis
                chord.setRotationAxis(Rotate.Z_AXIS);
                chord.setRotate(90);
                chord.setTranslateY(dy);
                chord.setTranslateZ(dz);
                node.getChildren().add(chord);
            }
        }

        // Diagonals (Bracings) along the 4 outer faces
        double bracingStep = 0.29; // Step length
        int steps = (int) (length / bracingStep);
        double braceRadius = 0.010; // 20mm tube diameter

        for (int i = 0; i < steps; i++) {
            double startX = -length / 2.0 + (i * bracingStep);
            double endX = startX + bracingStep;

            // Top & Bottom braces
            for (double dy : new double[]{-chordOffset, chordOffset}) {
                Cylinder brace = new Cylinder(braceRadius, Math.hypot(bracingStep, chordOffset * 2));
                brace.setMaterial(ALUMINUM_MAT);
                brace.setRotationAxis(Rotate.Y_AXIS);
                brace.setRotate((i % 2 == 0) ? 45 : -45);
                brace.setTranslateX(startX + (bracingStep / 2.0));
                brace.setTranslateY(dy);
                node.getChildren().add(brace);
            }

            // Left & Right braces
            for (double dz : new double[]{-chordOffset, chordOffset}) {
                Cylinder brace = new Cylinder(braceRadius, Math.hypot(bracingStep, chordOffset * 2));
                brace.setMaterial(ALUMINUM_MAT);
                brace.setRotationAxis(Rotate.Z_AXIS);
                brace.setRotate((i % 2 == 0) ? 45 : -45);
                brace.setTranslateX(startX + (bracingStep / 2.0));
                brace.setTranslateZ(dz);
                node.getChildren().add(brace);
            }
        }

        // End Coupler Plates
        for (double endX : new double[]{-length / 2.0, length / 2.0}) {
            Box coupler = new Box(0.015, chordOffset * 2 + 0.05, chordOffset * 2 + 0.05);
            coupler.setMaterial(ALUMINUM_MAT);
            coupler.setTranslateX(endX);
            node.getChildren().add(coupler);
        }

        return node;
    }
}
