package de.exit.artnet2rig.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lighting Rig Project containing all fixtures, truss structures, and metadata.
 */
public class RigProject {

    private Long id;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private final List<Fixture> fixtures = new ArrayList<>();
    private final List<TrussElement> trusses = new ArrayList<>();

    public RigProject(String name) {
        this(null, name, "", Instant.now(), Instant.now());
    }

    public RigProject(Long id, String name, String description, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = (name == null || name.isBlank()) ? "Untitled Rig" : name;
        this.description = description != null ? description : "";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Fixture> getFixtures() {
        return Collections.unmodifiableList(fixtures);
    }

    public void addFixture(Fixture fixture) {
        if (fixture != null) {
            fixtures.add(fixture);
            this.updatedAt = Instant.now();
        }
    }

    public void removeFixture(Long fixtureId) {
        fixtures.removeIf(f -> f.id() != null && f.id().equals(fixtureId));
        this.updatedAt = Instant.now();
    }

    public List<TrussElement> getTrusses() {
        return Collections.unmodifiableList(trusses);
    }

    public void addTruss(TrussElement truss) {
        if (truss != null) {
            trusses.add(truss);
            this.updatedAt = Instant.now();
        }
    }

    public void removeTruss(Long trussId) {
        trusses.removeIf(t -> t.id() != null && t.id().equals(trussId));
        this.updatedAt = Instant.now();
    }

    public void clearAll() {
        fixtures.clear();
        trusses.clear();
        this.updatedAt = Instant.now();
    }
}
