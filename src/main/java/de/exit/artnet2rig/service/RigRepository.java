package de.exit.artnet2rig.service;

import de.exit.artnet2rig.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Repository for persisting {@link RigProject}, {@link Fixture}, and {@link TrussElement}.
 */
public class RigRepository {

    private static final Logger log = LoggerFactory.getLogger(RigRepository.class);
    private final DatabaseService databaseService;

    public RigRepository(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Saves or updates a project including all its fixtures and truss elements.
     *
     * @param project the project to persist
     * @return persisted project with generated IDs
     */
    public RigProject save(RigProject project) {
        if (project.getId() == null) {
            return insertProject(project);
        } else {
            return updateProject(project);
        }
    }

    private RigProject insertProject(RigProject project) {
        String sql = "INSERT INTO projects (name, description, created_at, updated_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            Timestamp now = Timestamp.from(Instant.now());
            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setTimestamp(3, now);
            ps.setTimestamp(4, now);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    project.setId(rs.getLong(1));
                }
            }

            saveChildren(conn, project);
            return project;
        } catch (SQLException e) {
            log.error("Failed to insert project", e);
            throw new RuntimeException("Failed to insert project: " + project.getName(), e);
        }
    }

    private RigProject updateProject(RigProject project) {
        String sql = "UPDATE projects SET name = ?, description = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, project.getName());
            ps.setString(2, project.getDescription());
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setLong(4, project.getId());
            ps.executeUpdate();

            // Clear existing children and re-insert
            try (PreparedStatement delTrusses = conn.prepareStatement("DELETE FROM truss_elements WHERE project_id = ?");
                 PreparedStatement delFixtures = conn.prepareStatement("DELETE FROM fixtures WHERE project_id = ?")) {
                delTrusses.setLong(1, project.getId());
                delTrusses.executeUpdate();

                delFixtures.setLong(1, project.getId());
                delFixtures.executeUpdate();
            }

            saveChildren(conn, project);
            return project;
        } catch (SQLException e) {
            log.error("Failed to update project", e);
            throw new RuntimeException("Failed to update project id: " + project.getId(), e);
        }
    }

    private void saveChildren(Connection conn, RigProject project) throws SQLException {
        // Insert Trusses
        String trussSql = "INSERT INTO truss_elements (project_id, type, pos_x, pos_y, pos_z, rot_pitch, rot_yaw, rot_roll) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(trussSql)) {
            for (TrussElement truss : project.getTrusses()) {
                ps.setLong(1, project.getId());
                ps.setString(2, truss.type().name());
                ps.setDouble(3, truss.transform().x());
                ps.setDouble(4, truss.transform().y());
                ps.setDouble(5, truss.transform().z());
                ps.setDouble(6, truss.transform().pitch());
                ps.setDouble(7, truss.transform().yaw());
                ps.setDouble(8, truss.transform().roll());
                ps.addBatch();
            }
            ps.executeBatch();
        }

        // Insert Fixtures
        String fixSql = """
                INSERT INTO fixtures (project_id, name, type, universe, start_channel, channel_count,
                                      pos_x, pos_y, pos_z, rot_pitch, rot_yaw, rot_roll,
                                      pan_invert, tilt_invert, beam_angle)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(fixSql)) {
            for (Fixture fix : project.getFixtures()) {
                ps.setLong(1, project.getId());
                ps.setString(2, fix.name());
                ps.setString(3, fix.type().name());
                ps.setInt(4, fix.universe());
                ps.setInt(5, fix.startChannel());
                ps.setInt(6, fix.channelCount());
                ps.setDouble(7, fix.transform().x());
                ps.setDouble(8, fix.transform().y());
                ps.setDouble(9, fix.transform().z());
                ps.setDouble(10, fix.transform().pitch());
                ps.setDouble(11, fix.transform().yaw());
                ps.setDouble(12, fix.transform().roll());
                ps.setBoolean(13, fix.panInvert());
                ps.setBoolean(14, fix.tiltInvert());
                ps.setDouble(15, fix.beamAngle());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Loads a project by ID with all its fixtures and truss elements.
     */
    public Optional<RigProject> findById(Long id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }

                RigProject project = new RigProject(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                );

                loadChildren(conn, project);
                return Optional.of(project);
            }
        } catch (SQLException e) {
            log.error("Failed to load project by ID", e);
            throw new RuntimeException("Failed to load project ID: " + id, e);
        }
    }

    private void loadChildren(Connection conn, RigProject project) throws SQLException {
        // Load Trusses
        String trussSql = "SELECT * FROM truss_elements WHERE project_id = ? ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(trussSql)) {
            ps.setLong(1, project.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    TrussType type = TrussType.valueOf(rs.getString("type"));
                    Transform3D t = new Transform3D(
                            rs.getDouble("pos_x"),
                            rs.getDouble("pos_y"),
                            rs.getDouble("pos_z"),
                            rs.getDouble("rot_pitch"),
                            rs.getDouble("rot_yaw"),
                            rs.getDouble("rot_roll")
                    );
                    project.addTruss(new TrussElement(rs.getLong("id"), type, t));
                }
            }
        }

        // Load Fixtures
        String fixSql = "SELECT * FROM fixtures WHERE project_id = ? ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(fixSql)) {
            ps.setLong(1, project.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FixtureType type = FixtureType.valueOf(rs.getString("type"));
                    Transform3D t = new Transform3D(
                            rs.getDouble("pos_x"),
                            rs.getDouble("pos_y"),
                            rs.getDouble("pos_z"),
                            rs.getDouble("rot_pitch"),
                            rs.getDouble("rot_yaw"),
                            rs.getDouble("rot_roll")
                    );
                    Fixture fix = new Fixture(
                            rs.getLong("id"),
                            rs.getString("name"),
                            type,
                            rs.getInt("universe"),
                            rs.getInt("start_channel"),
                            rs.getInt("channel_count"),
                            t,
                            rs.getBoolean("pan_invert"),
                            rs.getBoolean("tilt_invert"),
                            rs.getDouble("beam_angle")
                    );
                    project.addFixture(fix);
                }
            }
        }
    }

    /**
     * Finds all projects without full child entities for overview lists.
     */
    public List<RigProject> findAll() {
        String sql = "SELECT * FROM projects ORDER BY updated_at DESC";
        List<RigProject> list = new ArrayList<>();
        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(new RigProject(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at").toInstant(),
                        rs.getTimestamp("updated_at").toInstant()
                ));
            }
            return list;
        } catch (SQLException e) {
            log.error("Failed to list projects", e);
            throw new RuntimeException("Failed to list projects", e);
        }
    }

    /**
     * Deletes a project by ID and cascades deletion to child fixtures and trusses.
     */
    public void delete(Long id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = databaseService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Failed to delete project", e);
            throw new RuntimeException("Failed to delete project ID: " + id, e);
        }
    }
}
