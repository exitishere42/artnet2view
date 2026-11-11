package de.exit.artnet2view.ui.menu;

import de.exit.artnet2view.ui.theme.LucideIcon;
import de.exit.artnet2view.ui.theme.ThemeConstants;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;

import java.util.HashMap;
import java.util.Map;

/**
 * Modern Action Dropdown Menu for the brand/logo button, matching JDMX.
 * Provides quick access to project management (.a2v), fullscreen, and app info.
 */
public class BrandActionMenu extends Popup {

    private final Map<String, Button> items = new HashMap<>();
    private final Map<String, Runnable> actions = new HashMap<>();

    public BrandActionMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(2);
        root.setStyle("-fx-background-color: #1E1E1E; -fx-background-radius: 4px; -fx-border-color: #333333; -fx-border-radius: 4px; -fx-padding: 4px; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 10, 0, 0, 4);");
        root.setMinWidth(195);
        root.setPrefWidth(200);

        // Project operations (.a2v)
        Button btnNew = createMenuItem("new_project", "Neues Projekt", LucideIcon.PLUS, Color.web("#22C55E"));
        Button btnOpen = createMenuItem("open_file", "Projekt öffnen...", LucideIcon.FOLDER, Color.web("#38BDF8"));
        Button btnSave = createMenuItem("save", "Projekt speichern", LucideIcon.SAVE, Color.web("#A855F7"));
        Button btnSaveAs = createMenuItem("save_as", "Speichern unter...", LucideIcon.SAVE, Color.web("#F59E0B"));

        // View & App
        Button btnFullscreen = createMenuItem("fullscreen", "Vollbild umschalten", LucideIcon.MAXIMIZE, Color.web("#F43F5E"));
        Button btnAbout = createMenuItem("about", "Über artnet2view", LucideIcon.INFO, Color.web("#94A3B8"));

        root.getChildren().addAll(
                btnNew,
                btnOpen,
                btnSave,
                btnSaveAs,
                createSeparator(),
                btnFullscreen,
                btnAbout
        );

        getContent().add(root);
    }

    private Button createMenuItem(String key, String title, String iconName, Color iconColor) {
        Button btn = new Button(title);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setCursor(Cursor.HAND);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E0E0E0; -fx-font-size: 12px; -fx-padding: 6px 10px; -fx-background-radius: 3px;");

        btn.setGraphic(LucideIcon.create(iconName, 15, toHex(iconColor)));

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #2E2E2E; -fx-text-fill: #FFFFFF; -fx-font-size: 12px; -fx-padding: 6px 10px; -fx-background-radius: 3px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #E0E0E0; -fx-font-size: 12px; -fx-padding: 6px 10px; -fx-background-radius: 3px;"));

        btn.setOnAction(e -> {
            hide();
            Runnable action = actions.get(key);
            if (action != null) {
                action.run();
            }
        });

        items.put(key, btn);
        return btn;
    }

    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #333333; -fx-padding: 2px 0 2px 0;");
        return sep;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    public void setAction(String key, Runnable action) {
        if (action != null) {
            actions.put(key, action);
        } else {
            actions.remove(key);
        }
    }
}
