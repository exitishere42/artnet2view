package de.exit.artnet2view.ui.canvas;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * 3D Volumetric light cone rendering realistic stage haze and beam projection.
 * Optimized for 60+ FPS zero-lag rendering via pure diffuse material shading.
 */
public class BeamNode extends Group {

    private final MeshView coneMeshView;
    private final PhongMaterial material;

    private boolean strobePhase = true;
    private long lastStrobeToggleTime = 0;

    public BeamNode(double lengthMeters, double apexRadius, double baseRadius) {
        this.material = new PhongMaterial();
        this.material.setDiffuseColor(Color.color(1.0, 1.0, 1.0, 0.0));
        this.material.setSpecularColor(Color.TRANSPARENT);

        TriangleMesh mesh = createConeMesh(20, lengthMeters, apexRadius, baseRadius);
        this.coneMeshView = new MeshView(mesh);
        this.coneMeshView.setMaterial(material);
        this.coneMeshView.setCullFace(CullFace.NONE);

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

            // Ring 0 (Apex / Lens) at Y=0
            points[i * 3] = cos * (float) r0;
            points[i * 3 + 1] = 0.0f;
            points[i * 3 + 2] = sin * (float) r0;

            // Ring 1 (Base) pointing downwards at Y = length
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

            // Triangle 1: p0 -> p2 -> p1
            faces[faceIdx++] = p0;
            faces[faceIdx++] = 0;
            faces[faceIdx++] = p2;
            faces[faceIdx++] = 2;
            faces[faceIdx++] = p1;
            faces[faceIdx++] = 1;

            // Triangle 2: p1 -> p2 -> p3
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
     * Updates the volumetric beam appearance with instant diffuse color shading.
     */
    public void update(double dimmerNorm, double redNorm, double greenNorm, double blueNorm, double whiteNorm, boolean strobeActive, double strobeHz) {
        if (dimmerNorm <= 0.001) {
            setVisible(false);
            return;
        }

        if (strobeActive && strobeHz > 0) {
            long now = System.currentTimeMillis();
            long periodMs = (long) (1000.0 / strobeHz);
            if (now - lastStrobeToggleTime > (periodMs / 2)) {
                strobePhase = !strobePhase;
                lastStrobeToggleTime = now;
            }
            if (!strobePhase) {
                setVisible(false);
                return;
            }
        }

        setVisible(true);

        double r = Math.min(1.0, redNorm + (whiteNorm * 0.95));
        double g = Math.min(1.0, greenNorm + (whiteNorm * 0.95));
        double b = Math.min(1.0, blueNorm + (whiteNorm * 0.85));

        // Fallback to bright warm-white if all colors are zero but dimmer is active
        if (r < 0.01 && g < 0.01 && b < 0.01) {
            r = 1.0;
            g = 0.95;
            b = 0.90;
        }

        // Volumetric alpha: rich visible beam cone
        double alpha = Math.min(0.80, dimmerNorm * 0.65);
        Color beamColor = Color.color(r, g, b, alpha);

        material.setDiffuseColor(beamColor);
    }
}
