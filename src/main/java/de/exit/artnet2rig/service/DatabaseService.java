package de.exit.artnet2rig.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Embedded H2 database service backed by a HikariCP connection pool.
 */
public class DatabaseService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final HikariDataSource dataSource;

    public DatabaseService() {
        this(getDefaultJdbcUrl());
    }

    public DatabaseService(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setPoolName("ArtNet2Rig-H2Pool");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        log.info("Initialized DatabaseService with URL: {}", jdbcUrl);
    }

    private static String getDefaultJdbcUrl() {
        String userHome = System.getProperty("user.home", ".");
        File dir = new File(userHome, ".artnet2rig");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return "jdbc:h2:" + new File(dir, "artnet2rig").getAbsolutePath() + ";DB_CLOSE_DELAY=-1";
    }

    /**
     * Initializes the relational database schema if not already present.
     */
    public void initSchema() {
        String sql = """
                CREATE TABLE IF NOT EXISTS projects (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(128) NOT NULL,
                    description VARCHAR(512),
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                );

                CREATE TABLE IF NOT EXISTS truss_elements (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    project_id BIGINT NOT NULL,
                    type VARCHAR(64) NOT NULL,
                    pos_x DOUBLE NOT NULL,
                    pos_y DOUBLE NOT NULL,
                    pos_z DOUBLE NOT NULL,
                    rot_pitch DOUBLE NOT NULL,
                    rot_yaw DOUBLE NOT NULL,
                    rot_roll DOUBLE NOT NULL,
                    CONSTRAINT fk_truss_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                );

                CREATE TABLE IF NOT EXISTS fixtures (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    project_id BIGINT NOT NULL,
                    name VARCHAR(128) NOT NULL,
                    type VARCHAR(64) NOT NULL,
                    universe INT NOT NULL,
                    start_channel INT NOT NULL,
                    channel_count INT NOT NULL,
                    pos_x DOUBLE NOT NULL,
                    pos_y DOUBLE NOT NULL,
                    pos_z DOUBLE NOT NULL,
                    rot_pitch DOUBLE NOT NULL,
                    rot_yaw DOUBLE NOT NULL,
                    rot_roll DOUBLE NOT NULL,
                    pan_invert BOOLEAN DEFAULT FALSE,
                    tilt_invert BOOLEAN DEFAULT FALSE,
                    beam_angle DOUBLE DEFAULT 15.0,
                    CONSTRAINT fk_fixture_project FOREIGN KEY (project_id) REFERENCES projects(id) ON DELETE CASCADE
                );
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Database schema initialized successfully.");
        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("DatabaseService connection pool closed.");
        }
    }
}
