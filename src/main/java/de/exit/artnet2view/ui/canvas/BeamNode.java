package de.exit.artnet2view.ui.canvas;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * 3D Volumetric light cone rendering stage haze and beam projection.
 * Uses back-face culling and strict dirty-checking to avoid unnecessary GPU material updates.
 */
public class BeamNode extends Group {

    private final MeshView coneMeshView;
    private final PhongMaterial material;

    private boolean strobePhase = true;
    private long lastStrobeToggleTime = 0;

    private boolean lastVisibleState = false;
    private double lastDimmer = -1.0;
    private double lastRed = -1.0;
    private double lastGreen = -1.0;
    private double lastBlue = -1.0;
    private double lastWhite = -1.0;

    public BeamNode(double lengthMeters, double apexRadius, double baseRadius) {
        this.material = new PhongMaterial();
        this.material.setDiffuseColor(Color.color(1.0, 1.0, 1.0, 0.0));
        this.material.setSpecularColor(Color.TRANSPARENT);

        TriangleMesh mesh = createConeMesh(14, lengthMeters, apexRadius, baseRadius);
        this.coneMeshView = new MeshView(mesh);
        this.coneMeshView.setMaterial(material);
        this.coneMeshView.setCullFace(CullFace.BACK);

        setVisible(false);
        getChildren().add(coneMeshView);
    }

    private TriangleMesh createConeMesh(int segments, double length, double r0, double r1) {
        TriangleMesh mesh = new TriangleMesh();

        float[] points = new float[segments * 2 * 3];
        float[] texCoords = new float[]{0, 0, 1, 0, 0, 1, 1, 1};

        for (int i = 0; i < segments; i++) {
            double angle = 2.0 * Math.PI * i / segments;
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);

            points[i * 3] = cos * (float) r0;
            points[i * 3 + 1] = 0.0f;
            points[i * 3 + 2] = sin * (float) r0;

            points[(segments + i) * 3] = cos * (float) r1;
            points[(segments + i) * 3 + 1] = (float) length;
            points[(segments + i) * 3 + 2] = sin * (float) r1;
        }

        int[] faces = new int[segments * 2 * 6];
        int faceIdx = 0;
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            int p0 = i;
            int p1 = next;
            int p2 = segments + i;
            int p3 = segments + next;

            // Outward CCW winding matching CullFace.BACK in Y-down coordinates
            faces[faceIdx++] = p0;
            faces[faceIdx++] = 0;
            faces[faceIdx++] = p2;
            faces[faceIdx++] = 2;
            faces[faceIdx++] = p1;
            faces[faceIdx++] = 1;

            faces[faceIdx++] = p1;
            faces[faceIdx++] = 1;
            faces[faceIdx++] = p2;
            faces[faceIdx++] = 2;
            faces[faceIdx++] = p3;
            faces[faceIdx++] = 3;
        }

        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(texCoords);
        mesh.getFaces().setAll(faces);
        return mesh;
    }

    /**
     * Updates the volumetric beam appearance if dimmer, color, or strobe visibility changed.
     *
     * @return {@code true} if a visual property was modified, {@code false} if skipped via dirty check
     */
    public boolean update(double dimmerNorm, double redNorm, double greenNorm, double blueNorm, double whiteNorm,
                          boolean strobeActive, double strobeHz) {
        if (dimmerNorm <= 0.001) {
            if (!lastVisibleState) {
                return false;
            }
            lastVisibleState = false;
            setVisible(false);
            return true;
        }

        if (strobeActive && strobeHz > 0) {
            long now = System.currentTimeMillis();
            long periodMs = (long) (1000.0 / strobeHz);
            if (now - lastStrobeToggleTime > (periodMs / 2)) {
                strobePhase = !strobePhase;
                lastStrobeToggleTime = now;
            }
            if (!strobePhase) {
                if (!lastVisibleState) {
                    return false;
                }
                lastVisibleState = false;
                setVisible(false);
                return true;
            }
        }

        boolean changed = !lastVisibleState;
        if (!lastVisibleState) {
            lastVisibleState = true;
            setVisible(true);
        }

        if (Math.abs(dimmerNorm - lastDimmer) > 1e-4
                || Math.abs(redNorm - lastRed) > 1e-4
                || Math.abs(greenNorm - lastGreen) > 1e-4
                || Math.abs(blueNorm - lastBlue) > 1e-4
                || Math.abs(whiteNorm - lastWhite) > 1e-4) {
            lastDimmer = dimmerNorm;
            lastRed = redNorm;
            lastGreen = greenNorm;
            lastBlue = blueNorm;
            lastWhite = whiteNorm;

            double r = Math.min(1.0, redNorm + (whiteNorm * 0.95));
            double g = Math.min(1.0, greenNorm + (whiteNorm * 0.95));
            double b = Math.min(1.0, blueNorm + (whiteNorm * 0.85));

            if (r < 0.01 && g < 0.01 && b < 0.01) {
                r = 1.0;
                g = 0.95;
                b = 0.90;
            }

            double alpha = Math.min(0.80, dimmerNorm * 0.65);
            material.setDiffuseColor(Color.color(r, g, b, alpha));
            changed = true;
        }

        return changed;
    }
}
