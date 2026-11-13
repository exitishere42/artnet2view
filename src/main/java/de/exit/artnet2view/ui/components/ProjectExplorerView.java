package de.exit.artnet2view.ui.components;

import de.exit.artnet2view.core.model.Fixture;
import de.exit.artnet2view.core.model.FixtureType;
import de.exit.artnet2view.core.model.RigProject;
import de.exit.artnet2view.core.model.TrussElement;
import de.exit.artnet2view.service.RigService;
import de.exit.artnet2view.ui.theme.LucideIcon;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Hierarchical Project Explorer tree view for ArtNet2View.
 * Groups fixtures into Moving Heads, PAR Lights, and Truss elements.
 * Clicking any element selects it in RigService and switches the dock to the Inspector.
 */
public class ProjectExplorerView extends VBox {

    public enum NodeType {
        FOLDER_MOVING_HEADS,
        FOLDER_PARS,
        FOLDER_TRUSSES,
        ITEM_MOVING_HEAD,
        ITEM_PAR,
        ITEM_TRUSS
    }

    public static class ExplorerNode {
        private final String id;
        private final String name;
        private final String subtitle;
        private final NodeType type;
        private final boolean folder;
        private final String iconName;
        private final String iconColor;
        private final Fixture fixture;
        private final TrussElement truss;

        public ExplorerNode(String id, String name, String subtitle, NodeType type, boolean folder,
                            String iconName, String iconColor, Fixture fixture, TrussElement truss) {
            this.id = id;
            this.name = name;
            this.subtitle = subtitle;
            this.type = type;
            this.folder = folder;
            this.iconName = iconName;
            this.iconColor = iconColor;
            this.fixture = fixture;
            this.truss = truss;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getSubtitle() { return subtitle; }
        public NodeType getType() { return type; }
        public boolean isFolder() { return folder; }
        public String getIconName() { return iconName; }
        public String getIconColor() { return iconColor; }
        public Fixture getFixture() { return fixture; }
        public TrussElement getTruss() { return truss; }

        @Override
        public String toString() {
            return name;
        }
    }

    private final RigService rigService;
    private final Runnable onSelectionChanged;
    private final Runnable onStageModified;

    private final TreeView<ExplorerNode> treeView;
    private final TreeItem<ExplorerNode> rootItem;
    private boolean suppressSelectionEvent = false;

    public ProjectExplorerView(RigService rigService, Runnable onSelectionChanged, Runnable onStageModified) {
        this.rigService = Objects.requireNonNull(rigService, "rigService must not be null");
        this.onSelectionChanged = onSelectionChanged;
        this.onStageModified = onStageModified;

        getStyleClass().add("project-explorer-view");
        VBox.setVgrow(this, Priority.ALWAYS);

        ExplorerNode rootObj = new ExplorerNode("root", "Project", "", null, true, "box", "#FFFFFF", null, null);
        this.rootItem = new TreeItem<>(rootObj);
        rootItem.setExpanded(true);

        this.treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("project-explorer-tree");
        VBox.setVgrow(treeView, Priority.ALWAYS);

        treeView.setCellFactory(tv -> new ExplorerTreeCell());

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (suppressSelectionEvent || newVal == null || newVal.getValue() == null) {
                return;
            }
            ExplorerNode node = newVal.getValue();
            if (node.isFolder()) {
                return;
            }
            if (node.getFixture() != null) {
                rigService.setSelectedFixture(node.getFixture());
                if (this.onSelectionChanged != null) {
                    this.onSelectionChanged.run();
                }
            } else if (node.getTruss() != null) {
                rigService.setSelectedTruss(node.getTruss());
                if (this.onSelectionChanged != null) {
                    this.onSelectionChanged.run();
                }
            }
        });

        getChildren().add(treeView);

        rebuildTree();
    }

    /**
     * Rebuilds the TreeView to reflect current fixtures and trusses in the active RigProject.
     */
    public void rebuildTree() {
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::rebuildTree);
            return;
        }

        Map<String, Boolean> expandedState = new HashMap<>();
        for (TreeItem<ExplorerNode> child : rootItem.getChildren()) {
            if (child.getValue() != null && child.getValue().isFolder()) {
                expandedState.put(child.getValue().getId(), child.isExpanded());
            }
        }

        suppressSelectionEvent = true;
        try {
            rootItem.getChildren().clear();

            RigProject project = rigService.getActiveProject();
            if (project == null) {
                return;
            }

            // 1. Moving Heads Folder
            var movingHeads = project.getFixtures().stream()
                    .filter(f -> f.type() == FixtureType.MOVING_HEAD_ZQ02040)
                    .toList();
            String mhTitle = "Moving Heads (" + movingHeads.size() + ")";
            ExplorerNode mhFolderNode = new ExplorerNode("folder_mh", mhTitle, "",
                    NodeType.FOLDER_MOVING_HEADS, true, LucideIcon.BOX, "#38BDF8", null, null);
            TreeItem<ExplorerNode> mhFolderItem = new TreeItem<>(mhFolderNode);
            mhFolderItem.setExpanded(expandedState.getOrDefault("folder_mh", true));

            for (Fixture fix : movingHeads) {
                String sub = "Ch. " + fix.startChannel() + "-" + (fix.startChannel() + fix.channelCount() - 1) + " • U" + fix.universe();
                ExplorerNode fixNode = new ExplorerNode("fix_" + fix.id(), fix.name(), sub,
                        NodeType.ITEM_MOVING_HEAD, false, LucideIcon.BOX, "#38BDF8", fix, null);
                mhFolderItem.getChildren().add(new TreeItem<>(fixNode));
            }

            // 2. PAR Lights Folder
            var pars = project.getFixtures().stream()
                    .filter(f -> f.type() == FixtureType.LED_PAR)
                    .toList();
            String parTitle = "PAR-Scheinwerfer (" + pars.size() + ")";
            ExplorerNode parFolderNode = new ExplorerNode("folder_par", parTitle, "",
                    NodeType.FOLDER_PARS, true, LucideIcon.BOX, "#F59E0B", null, null);
            TreeItem<ExplorerNode> parFolderItem = new TreeItem<>(parFolderNode);
            parFolderItem.setExpanded(expandedState.getOrDefault("folder_par", true));

            for (Fixture fix : pars) {
                String sub = "Ch. " + fix.startChannel() + "-" + (fix.startChannel() + fix.channelCount() - 1) + " • U" + fix.universe();
                ExplorerNode fixNode = new ExplorerNode("fix_" + fix.id(), fix.name(), sub,
                        NodeType.ITEM_PAR, false, LucideIcon.BOX, "#F59E0B", fix, null);
                parFolderItem.getChildren().add(new TreeItem<>(fixNode));
            }

            // 3. Trusses Folder
            var trusses = project.getTrusses();
            String trussTitle = "Traversen (" + trusses.size() + ")";
            ExplorerNode trussFolderNode = new ExplorerNode("folder_truss", trussTitle, "",
                    NodeType.FOLDER_TRUSSES, true, LucideIcon.LAYERS, "#A78BFA", null, null);
            TreeItem<ExplorerNode> trussFolderItem = new TreeItem<>(trussFolderNode);
            trussFolderItem.setExpanded(expandedState.getOrDefault("folder_truss", true));

            for (TrussElement truss : trusses) {
                String name = "Traverse #" + truss.id();
                String sub = "F34 (2.0m)";
                ExplorerNode tNode = new ExplorerNode("truss_" + truss.id(), name, sub,
                        NodeType.ITEM_TRUSS, false, LucideIcon.LAYERS, "#A78BFA", null, truss);
                trussFolderItem.getChildren().add(new TreeItem<>(tNode));
            }

            rootItem.getChildren().addAll(mhFolderItem, parFolderItem, trussFolderItem);

            syncActiveSelection();
        } finally {
            suppressSelectionEvent = false;
        }
    }

    private void syncActiveSelection() {
        Fixture selFix = rigService.getSelectedFixture();
        TrussElement selTruss = rigService.getSelectedTruss();

        if (selFix == null && selTruss == null) {
            return;
        }

        for (TreeItem<ExplorerNode> folder : rootItem.getChildren()) {
            for (TreeItem<ExplorerNode> item : folder.getChildren()) {
                ExplorerNode val = item.getValue();
                if (val != null) {
                    if (selFix != null && val.getFixture() != null && Objects.equals(val.getFixture().id(), selFix.id())) {
                        treeView.getSelectionModel().select(item);
                        return;
                    }
                    if (selTruss != null && val.getTruss() != null && Objects.equals(val.getTruss().id(), selTruss.id())) {
                        treeView.getSelectionModel().select(item);
                        return;
                    }
                }
            }
        }
    }

    private class ExplorerTreeCell extends TreeCell<ExplorerNode> {

        private final HBox layout = new HBox(8);
        private final Label lblTitle = new Label();
        private final Label lblSubtitle = new Label();
        private final Region spacer = new Region();
        private final HBox graphicContainer = new HBox();

        ExplorerTreeCell() {
            layout.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(spacer, Priority.ALWAYS);

            lblTitle.setStyle("-fx-text-fill: #E5E7EB; -fx-font-size: 12px;");
            lblSubtitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");

            layout.getChildren().addAll(graphicContainer, lblTitle, spacer, lblSubtitle);
        }

        @Override
        protected void updateItem(ExplorerNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            graphicContainer.getChildren().clear();
            Node icon = LucideIcon.create(item.getIconName(), item.isFolder() ? 15 : 14, item.getIconColor());
            graphicContainer.getChildren().add(icon);

            lblTitle.setText(item.getName());
            if (item.isFolder()) {
                lblTitle.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px; -fx-font-weight: bold;");
                lblSubtitle.setText("");
                setContextMenu(null);
            } else {
                lblTitle.setStyle("-fx-text-fill: #E5E7EB; -fx-font-size: 12px; -fx-font-weight: 500;");
                lblSubtitle.setText(item.getSubtitle());

                // Context Menu for Items (Delete / Duplicate)
                ContextMenu cm = new ContextMenu();
                MenuItem miDelete = new MenuItem("Löschen");
                miDelete.setOnAction(e -> {
                    if (item.getFixture() != null) {
                        rigService.removeFixture(item.getFixture().id());
                    } else if (item.getTruss() != null) {
                        rigService.removeTruss(item.getTruss().id());
                    }
                    if (onStageModified != null) {
                        onStageModified.run();
                    }
                    rebuildTree();
                });
                cm.getItems().add(miDelete);
                setContextMenu(cm);
            }

            setGraphic(layout);
        }
    }
}
