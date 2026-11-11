package de.exit.artnet2view.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.RigProject;
import de.exit.artnet2view.core.model.TrussElement;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for saving and loading ArtNet2View (.a2v) project files.
 */
public class A2vProjectService {

    public static final String FILE_EXTENSION = ".a2v";
    public static final String FILE_DESCRIPTION = "ArtNet2View Projekt (*.a2v)";

    private final ObjectMapper objectMapper;

    public A2vProjectService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Data Transfer Object for JSON serialization into .a2v file.
     */
    public record A2vProjectDto(
            String format,
            String version,
            String name,
            String description,
            Instant createdAt,
            Instant updatedAt,
            List<TrussElement> trusses,
            List<Fixture> fixtures
    ) {}

    /**
     * Saves a RigProject to a .a2v file.
     *
     * @param project the project to save
     * @param file    target .a2v file
     * @throws IOException if writing fails
     */
    public void saveToFile(RigProject project, File file) throws IOException {
        if (project == null) {
            throw new IllegalArgumentException("Project must not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }

        A2vProjectDto dto = new A2vProjectDto(
                "artnet2view",
                "1.0.2",
                project.getName(),
                project.getDescription(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                new ArrayList<>(project.getTrusses()),
                new ArrayList<>(project.getFixtures())
        );

        objectMapper.writeValue(file, dto);
    }

    /**
     * Loads a RigProject from a .a2v file.
     *
     * @param file source .a2v file
     * @return loaded RigProject
     * @throws IOException if reading or parsing fails
     */
    public RigProject loadFromFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File not found: " + file);
        }

        A2vProjectDto dto = objectMapper.readValue(file, A2vProjectDto.class);

        RigProject project = new RigProject(
                null,
                dto.name(),
                dto.description(),
                dto.createdAt() != null ? dto.createdAt() : Instant.now(),
                dto.updatedAt() != null ? dto.updatedAt() : Instant.now()
        );

        if (dto.trusses() != null) {
            for (TrussElement truss : dto.trusses()) {
                project.addTruss(truss);
            }
        }

        if (dto.fixtures() != null) {
            for (Fixture fix : dto.fixtures()) {
                project.addFixture(fix);
            }
        }

        return project;
    }
}
