package de.exit.artnet2view.service;

import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.RigProject;
import de.exit.artnet2view.core.model.TrussElement;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service orchestrating the active lighting project, entity selection, and persistence.
 */
public class RigService {

    private static final Logger log = LoggerFactory.getLogger(RigService.class);

    private final RigRepository repository;
    private final ObjectProperty<RigProject> activeProjectProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Fixture> selectedFixtureProperty = new SimpleObjectProperty<>(null);
    private final ObjectProperty<TrussElement> selectedTrussProperty = new SimpleObjectProperty<>(null);

    public RigService(RigRepository repository) {
        this.repository = repository;
        this.activeProjectProperty.set(new RigProject("Default Stage"));
    }

    public RigProject getActiveProject() {
        return activeProjectProperty.get();
    }

    public ObjectProperty<RigProject> activeProjectProperty() {
        return activeProjectProperty;
    }

    public void createNewProject(String name) {
        clearSelection();
        activeProjectProperty.set(new RigProject(name));
    }

    public void saveActiveProject() {
        RigProject project = activeProjectProperty.get();
        if (project != null && repository != null) {
            RigProject saved = repository.save(project);
            activeProjectProperty.set(saved);
            log.info("Saved project '{}' with ID {}", saved.getName(), saved.getId());
        }
    }

    public boolean loadProject(Long id) {
        if (repository == null) {
            return false;
        }
        Optional<RigProject> loaded = repository.findById(id);
        if (loaded.isPresent()) {
            clearSelection();
            activeProjectProperty.set(loaded.get());
            log.info("Loaded project '{}' (ID {})", loaded.get().getName(), id);
            return true;
        }
        return false;
    }

    public List<RigProject> listProjects() {
        return repository != null ? repository.findAll() : List.of();
    }

    public void addFixture(Fixture fixture) {
        if (fixture != null) {
            getActiveProject().addFixture(fixture);
            setSelectedFixture(fixture);
        }
    }

    public void updateFixture(Fixture oldFixture, Fixture updatedFixture) {
        if (oldFixture == null || updatedFixture == null) {
            return;
        }
        getActiveProject().removeFixture(oldFixture.id());
        getActiveProject().addFixture(updatedFixture);
        if (selectedFixtureProperty.get() != null && selectedFixtureProperty.get().equals(oldFixture)) {
            selectedFixtureProperty.set(updatedFixture);
        }
    }

    public void removeFixture(Long id) {
        if (id != null) {
            getActiveProject().removeFixture(id);
            if (selectedFixtureProperty.get() != null && id.equals(selectedFixtureProperty.get().id())) {
                selectedFixtureProperty.set(null);
            }
        }
    }

    public void addTruss(TrussElement truss) {
        if (truss != null) {
            getActiveProject().addTruss(truss);
            setSelectedTruss(truss);
        }
    }

    public void removeTruss(Long id) {
        if (id != null) {
            getActiveProject().removeTruss(id);
            if (selectedTrussProperty.get() != null && id.equals(selectedTrussProperty.get().id())) {
                selectedTrussProperty.set(null);
            }
        }
    }

    public void clearSelection() {
        selectedFixtureProperty.set(null);
        selectedTrussProperty.set(null);
    }

    public Fixture getSelectedFixture() {
        return selectedFixtureProperty.get();
    }

    public void setSelectedFixture(Fixture fixture) {
        selectedTrussProperty.set(null);
        selectedFixtureProperty.set(fixture);
    }

    public ObjectProperty<Fixture> selectedFixtureProperty() {
        return selectedFixtureProperty;
    }

    public TrussElement getSelectedTruss() {
        return selectedTrussProperty.get();
    }

    public void setSelectedTruss(TrussElement truss) {
        selectedFixtureProperty.set(null);
        selectedTrussProperty.set(truss);
    }

    public ObjectProperty<TrussElement> selectedTrussProperty() {
        return selectedTrussProperty;
    }
}
