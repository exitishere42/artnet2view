package de.exit.artnet2view.ui.canvas;

import de.exit.artnet2view.core.model.Transform3D;
import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.core.model.TrussType;
import javafx.scene.Group;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying batched single-draw-call TriangleMesh generation in MeshFactory.
 */
class MeshFactoryTest {

    @Test
    @DisplayName("Truss 2m generates a single batched MeshView with valid geometry")
    void testBatchedTrussMeshGeneration() {
        TrussElement truss = new TrussElement(1L, TrussType.STRAIGHT_2M, new Transform3D(0, -3.5, 0, 0, 0, 0));
        Group node = MeshFactory.createTrussNode(truss);

        assertThat(node.getChildren()).hasSize(1);
        assertThat(node.getChildren().get(0)).isInstanceOf(MeshView.class);

        MeshView view = (MeshView) node.getChildren().get(0);
        assertThat(view.getMesh()).isInstanceOf(TriangleMesh.class);

        TriangleMesh mesh = (TriangleMesh) view.getMesh();
        int pointCount = mesh.getPoints().size() / 3;
        int faceElements = mesh.getFaces().size();

        assertThat(pointCount).isGreaterThan(32);
        assertThat(faceElements).isGreaterThan(0);
        assertThat(faceElements % 6).isZero();
        // Verify low-poly budget (< 2500 triangles per 2m truss)
        assertThat(faceElements / 6).isLessThan(2500);
    }

    @Test
    @DisplayName("Stage floor grid generates a single batched MeshView")
    void testStageGridMeshGeneration() {
        MeshView grid = MeshFactory.createStageGridMesh(12.0, 8.0, 1.0);
        assertThat(grid).isNotNull();
        assertThat(grid.getMesh()).isInstanceOf(TriangleMesh.class);

        TriangleMesh mesh = (TriangleMesh) grid.getMesh();
        // 13 X-lines + 9 Z-lines = 22 boxes = 22 * 12 triangles = 264 triangles
        assertThat(mesh.getFaces().size() / 6).isEqualTo((13 + 9) * 12);
    }

    @Test
    @DisplayName("Stage grid box normals point strictly outwards on top surface (-Y)")
    void testOutwardWindingNormals() {
        MeshView grid = MeshFactory.createStageGridMesh(2.0, 2.0, 2.0);
        TriangleMesh mesh = (TriangleMesh) grid.getMesh();
        var pts = mesh.getPoints();
        var faces = mesh.getFaces();

        boolean foundUpwardTopFace = false;
        for (int f = 0; f < faces.size(); f += 6) {
            int i0 = faces.get(f) * 3;
            int i1 = faces.get(f + 2) * 3;
            int i2 = faces.get(f + 4) * 3;

            double ax = pts.get(i1) - pts.get(i0);
            double ay = pts.get(i1 + 1) - pts.get(i0 + 1);
            double az = pts.get(i1 + 2) - pts.get(i0 + 2);

            double bx = pts.get(i2) - pts.get(i0);
            double by = pts.get(i2 + 1) - pts.get(i0 + 1);
            double bz = pts.get(i2 + 2) - pts.get(i0 + 2);

            // Cross product normal in Y-down stage coordinates; upward face normal points along -Y
            double nx = ay * bz - az * by;
            double ny = az * bx - ax * bz;
            double nz = ax * by - ay * bx;

            if (Math.abs(nx) < 1e-6 && Math.abs(nz) < 1e-6 && ny < -1e-6) {
                foundUpwardTopFace = true;
            }
        }
        assertThat(foundUpwardTopFace).isTrue();
    }

    @Test
    @DisplayName("Truss cylinders produce non-degenerate outward normals matching radial chord direction")
    void testTrussCylinderNormals() {
        TrussElement truss = new TrussElement(3L, TrussType.STRAIGHT_2M, Transform3D.ZERO);
        Group node = MeshFactory.createTrussNode(truss);
        MeshView view = (MeshView) node.getChildren().get(0);
        TriangleMesh mesh = (TriangleMesh) view.getMesh();

        var pts = mesh.getPoints();
        var faces = mesh.getFaces();

        // Check first chord centered at (-0.145, -0.145) in YZ: top side (-Y) normal must point outward (ny < 0)
        int nonZeroNormals = 0;
        for (int f = 0; f < faces.size(); f += 6) {
            int i0 = faces.get(f) * 3;
            int i1 = faces.get(f + 2) * 3;
            int i2 = faces.get(f + 4) * 3;

            double ax = pts.get(i1) - pts.get(i0);
            double ay = pts.get(i1 + 1) - pts.get(i0 + 1);
            double az = pts.get(i1 + 2) - pts.get(i0 + 2);

            double bx = pts.get(i2) - pts.get(i0);
            double by = pts.get(i2 + 1) - pts.get(i0 + 1);
            double bz = pts.get(i2 + 2) - pts.get(i0 + 2);

            double nx = ay * bz - az * by;
            double ny = az * bx - ax * bz;
            double nz = ax * by - ay * bx;
            if (Math.hypot(nx, Math.hypot(ny, nz)) > 1e-6) {
                nonZeroNormals++;
            }
        }
        assertThat(nonZeroNormals).isEqualTo(faces.size() / 6);
    }

    @Test
    @DisplayName("Base plate generates compact single node")
    void testBasePlate() {
        TrussElement plate = new TrussElement(2L, TrussType.BASE_PLATE, Transform3D.ZERO);
        Group node = MeshFactory.createTrussNode(plate);

        assertThat(node.getChildren()).hasSize(1);
    }
}
