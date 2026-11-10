package de.exit.artnet2view.ui.canvas;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * 3D Volumetric light cone rendering realistic stage haze and beam projection.
 */
public class BeamNode extends Group {

    private final MeshView coneMeshView;
    private final PhongMaterial material;
    private final double lengthMeters;
    private final double apexRadius;
    private final double baseRadius;

    private boolean strobePhase = true;
    private long lastStrobeToggleTime = 0;

    public BeamNode(double lengthMeters, double apexRadius, double baseRadius) {
        this.lengthMeters = lengthMeters;
        this.apexRadius = apexRadius;
        this.baseRadius = baseRadius;

        this.material = new PhongMaterial();
        this.material.setDiffuseColor(Color.color(1.0, 1.0, 1.0, 0.0));
        this.material.setSpecularColor(Color.color(1.0, 1.0, 1.0, 0.2));

        TriangleMesh mesh = createConeMesh(16, lengthMeters, apexRadius, baseRadius);
        this.coneMeshView = new MeshView(mesh);
        this.coneMeshView.setMaterial(material);
        this.coneMeshView.setCullFace(javafx.scene.shape.CullFace.NONE);

        getChildren().add(coneMeshView);
    }

    private TriangleMesh createConeMesh(int segments, double length, double r0, double r1) {
        TriangleMesh mesh = new TriangleMesh();

        // Vertices: Top circle (0..segments-1), Bottom circle (segments..2*segments-1)
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

        // Build side faces (quads made of 2 triangles per segment)
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
     * Updates the volumetric beam appearance from normalized DMX parameters.
     *
     * @param dimmerNorm   normalized intensity (0.0 to 1.0)
     * @param redNorm      red component (0.0 to 1.0)
     * @param greenNorm    green component (0.0 to 1.0)
     * @param blueNorm     blue component (0.0 to 1.0)
     * @param whiteNorm    white component (0.0 to 1.0)
     * @param strobeActive whether strobe is triggered
     * @param strobeHz     strobe rate in Hertz
     */
    public void update(double dimmerNorm, double redNorm, double greenNorm, double blueNorm, double whiteNorm, boolean strobeActive, double strobeHz) {
        if (dimmerNorm <= 0.001) {
            setVisible(false);
            return;
        }

        // Strobe timing logic
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

        // Mix RGB with White
        double r = Math.min(1.0, redNorm + whiteNorm);
        double g = Math.min(1.0, greenNorm + whiteNorm);
        double b = Math.min(1.0, blueNorm + whiteNorm);

        // Volumetric alpha: subtle translucent haze
        double alpha = dimmerNorm * 0.40;
        Color beamColor = Color.color(r, g, b, alpha);
        material.setDiffuseColor(beamColor);
        material.setSpecularColor(Color.color(r, g, b, alpha * 0.5));
    }
}
