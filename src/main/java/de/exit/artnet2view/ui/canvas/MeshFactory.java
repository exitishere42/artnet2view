package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.core.model.TrussType;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

/**
 * Procedural batched 3D Mesh generator for F34 aluminum trusses and the stage floor grid.
 * Combines truss chords, diagonal bracings, and coupler plates into a single {@link TriangleMesh}
 * draw call per truss to keep GPU vertex and state overhead minimal on integrated graphics.
 */
public final class MeshFactory {

    private static final PhongMaterial ALUMINUM_MAT = new PhongMaterial(Color.web("#B0BEC5"));
    private static final PhongMaterial GRID_MAT = new PhongMaterial(Color.web("#2A2A2A"));

    private static final int CHORD_SIDES = 8;
    private static final int BRACE_SIDES = 6;

    static {
        ALUMINUM_MAT.setSpecularColor(Color.web("#ECEFF1"));
        GRID_MAT.setSpecularColor(Color.TRANSPARENT);
    }

    private MeshFactory() {
    }

    /**
     * Creates a single-draw-call 3D Node representing an F34 Truss element.
     *
     * @param truss truss entity with type and coordinates
     * @return ready-to-render JavaFX 3D Group
     */
    public static Group createTrussNode(TrussElement truss) {
        Group node = new Group();

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
        double halfLen = length / 2.0;
        double chordOffset = 0.145;
        double mainChordRadius = 0.025;
        double braceRadius = 0.010;

        MeshBuilder mb = new MeshBuilder();

        // 4 Main Chords along X axis
        for (double dy : new double[]{-chordOffset, chordOffset}) {
            for (double dz : new double[]{-chordOffset, chordOffset}) {
                mb.addCylinder(-halfLen, dy, dz, halfLen, dy, dz, mainChordRadius, CHORD_SIDES);
            }
        }

        // Diagonals (Bracings) along the 4 outer faces
        double bracingStep = 0.29;
        int steps = Math.max(1, (int) (length / bracingStep));
        for (int i = 0; i < steps; i++) {
            double x0 = -halfLen + (i * bracingStep);
            double x1 = x0 + bracingStep;
            boolean even = (i % 2 == 0);

            // Top & Bottom faces
            for (double dy : new double[]{-chordOffset, chordOffset}) {
                double zA = even ? -chordOffset : chordOffset;
                double zB = even ? chordOffset : -chordOffset;
                mb.addCylinder(x0, dy, zA, x1, dy, zB, braceRadius, BRACE_SIDES);
            }

            // Left & Right faces
            for (double dz : new double[]{-chordOffset, chordOffset}) {
                double yA = even ? -chordOffset : chordOffset;
                double yB = even ? chordOffset : -chordOffset;
                mb.addCylinder(x0, yA, dz, x1, yB, dz, braceRadius, BRACE_SIDES);
            }
        }

        // End Coupler Plates
        double couplerSize = chordOffset * 2 + 0.05;
        for (double endX : new double[]{-halfLen, halfLen}) {
            mb.addBox(endX, 0, 0, 0.015, couplerSize, couplerSize);
        }

        MeshView trussView = new MeshView(mb.build());
        trussView.setMaterial(ALUMINUM_MAT);
        trussView.setCullFace(CullFace.BACK);
        node.getChildren().add(trussView);
        return node;
    }

    /**
     * Generates a single batched MeshView for the entire stage floor grid (1 draw call).
     *
     * @param widthMeters floor width along X axis
     * @param depthMeters floor depth along Z axis
     * @param stepMeters  grid cell spacing in meters
     * @return batched MeshView ready for placement in the stage root
     */
    public static MeshView createStageGridMesh(double widthMeters, double depthMeters, double stepMeters) {
        MeshBuilder mb = new MeshBuilder();
        double halfW = widthMeters / 2.0;
        double halfD = depthMeters / 2.0;

        for (double x = -halfW; x <= halfW + 1e-6; x += stepMeters) {
            mb.addBox(x, -0.01, 0, 0.02, 0.01, depthMeters);
        }
        for (double z = -halfD; z <= halfD + 1e-6; z += stepMeters) {
            mb.addBox(0, -0.01, z, widthMeters, 0.01, 0.02);
        }

        MeshView gridView = new MeshView(mb.build());
        gridView.setMaterial(GRID_MAT);
        gridView.setCullFace(CullFace.BACK);
        return gridView;
    }

    /**
     * Compact builder accumulating multiple cylinders and boxes into a single {@link TriangleMesh}.
     */
    private static final class MeshBuilder {
        private float[] points = new float[1024 * 3];
        private int pointFloatLen = 0;
        private int[] faces = new int[2048 * 6];
        private int faceIntLen = 0;

        private int addPoint(double x, double y, double z) {
            if (pointFloatLen + 3 > points.length) {
                float[] next = new float[points.length * 2];
                System.arraycopy(points, 0, next, 0, pointFloatLen);
                points = next;
            }
            int idx = pointFloatLen / 3;
            points[pointFloatLen++] = (float) x;
            points[pointFloatLen++] = (float) y;
            points[pointFloatLen++] = (float) z;
            return idx;
        }

        private void addTri(int p0, int p1, int p2) {
            if (faceIntLen + 6 > faces.length) {
                int[] next = new int[faces.length * 2];
                System.arraycopy(faces, 0, next, 0, faceIntLen);
                faces = next;
            }
            faces[faceIntLen++] = p0;
            faces[faceIntLen++] = 0;
            faces[faceIntLen++] = p1;
            faces[faceIntLen++] = 0;
            faces[faceIntLen++] = p2;
            faces[faceIntLen++] = 0;
        }

        void addCylinder(double x1, double y1, double z1, double x2, double y2, double z2, double r, int sides) {
            double dx = x2 - x1;
            double dy = y2 - y1;
            double dz = z2 - z1;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-6) {
                return;
            }
            double tx = dx / len;
            double ty = dy / len;
            double tz = dz / len;

            // Pick a non-collinear helper axis to form orthonormal basis (u, v, t) with u x v = t
            double hx = Math.abs(tx) < 0.8 ? 1.0 : 0.0;
            double hy = Math.abs(tx) < 0.8 ? 0.0 : 1.0;
            double hz = 0.0;

            double ux = ty * hz - tz * hy;
            double uy = tz * hx - tx * hz;
            double uz = tx * hy - ty * hx;
            double uLen = Math.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= uLen;
            uy /= uLen;
            uz /= uLen;

            // v = t x u so (u x v = t)
            double vx = ty * uz - tz * uy;
            double vy = tz * ux - tx * uz;
            double vz = tx * uy - ty * ux;

            int baseRing0 = pointFloatLen / 3;
            for (int i = 0; i < sides; i++) {
                double a = (2.0 * Math.PI * i) / sides;
                double ca = Math.cos(a) * r;
                double sa = Math.sin(a) * r;
                double ox = ux * ca + vx * sa;
                double oy = uy * ca + vy * sa;
                double oz = uz * ca + vz * sa;
                addPoint(x1 + ox, y1 + oy, z1 + oz);
            }
            int baseRing1 = pointFloatLen / 3;
            for (int i = 0; i < sides; i++) {
                double a = (2.0 * Math.PI * i) / sides;
                double ca = Math.cos(a) * r;
                double sa = Math.sin(a) * r;
                double ox = ux * ca + vx * sa;
                double oy = uy * ca + vy * sa;
                double oz = uz * ca + vz * sa;
                addPoint(x2 + ox, y2 + oy, z2 + oz);
            }

            // Outward CCW winding matching CullFace.BACK where (u x v = t), so (v x t = +u)
            for (int i = 0; i < sides; i++) {
                int next = (i + 1) % sides;
                int p0 = baseRing0 + i;
                int p1 = baseRing0 + next;
                int p2 = baseRing1 + i;
                int p3 = baseRing1 + next;
                addTri(p0, p1, p2);
                addTri(p1, p3, p2);
            }
        }

        void addBox(double cx, double cy, double cz, double w, double h, double d) {
            double hx = w / 2.0;
            double hy = h / 2.0;
            double hz = d / 2.0;

            int v0 = addPoint(cx - hx, cy - hy, cz - hz);
            int v1 = addPoint(cx + hx, cy - hy, cz - hz);
            int v2 = addPoint(cx + hx, cy + hy, cz - hz);
            int v3 = addPoint(cx - hx, cy + hy, cz - hz);
            int v4 = addPoint(cx - hx, cy - hy, cz + hz);
            int v5 = addPoint(cx + hx, cy - hy, cz + hz);
            int v6 = addPoint(cx + hx, cy + hy, cz + hz);
            int v7 = addPoint(cx - hx, cy + hy, cz + hz);

            // -Z front face (facing -Z)
            addTri(v0, v3, v1);
            addTri(v1, v3, v2);
            // +Z back face (facing +Z)
            addTri(v5, v6, v4);
            addTri(v4, v6, v7);
            // -X left face
            addTri(v4, v7, v0);
            addTri(v0, v7, v3);
            // +X right face
            addTri(v1, v2, v5);
            addTri(v5, v2, v6);
            // -Y top face (up in stage Y-down coords)
            addTri(v4, v0, v5);
            addTri(v5, v0, v1);
            // +Y bottom face
            addTri(v3, v7, v2);
            addTri(v2, v7, v6);
        }

        TriangleMesh build() {
            TriangleMesh mesh = new TriangleMesh();
            float[] finalPoints = new float[pointFloatLen];
            System.arraycopy(points, 0, finalPoints, 0, pointFloatLen);
            int[] finalFaces = new int[faceIntLen];
            System.arraycopy(faces, 0, finalFaces, 0, faceIntLen);

            mesh.getPoints().setAll(finalPoints);
            mesh.getTexCoords().setAll(0f, 0f);
            mesh.getFaces().setAll(finalFaces);
            return mesh;
        }
    }
}
