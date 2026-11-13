package de.exit.artnet2view.ui.menu;

import de.exit.artnet2view.ui.theme.LucideIcon;
import javafx.application.Platform;
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
 * Brand Action Dropdown Menu styled identically to JDMX.
 * Contains strictly Save, Save As, Updates, Fullscreen, and Exit actions.
 */
public class BrandActionMenu extends Popup {

    private final Map<String, Button> items = new HashMap<>();
    private final Map<String, Runnable> actions = new HashMap<>();

    public BrandActionMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(2);
        root.getStyleClass().add("action-menu-popup");
        var cssUrl = BrandActionMenu.class.getResource("/de/exit/artnet2view/styles/dark-theme.css");
        if (cssUrl != null) {
            root.getStylesheets().add(cssUrl.toExternalForm());
        }
        root.setMinWidth(175);
        root.setPrefWidth(178);

        // 1. Project Saving (.a2v)
        Button btnSave = createMenuItem("save", "Speichern", LucideIcon.SAVE, Color.web("#10B981"));
        Button btnSaveAs = createMenuItem("save_as", "Speichern unter...", LucideIcon.FILE_PEN, Color.web("#2DD4BF"));

        // 2. Updates & View
        Button btnUpdates = createMenuItem("check_updates", "Updates", LucideIcon.ARROW_UP_CIRCLE, Color.web("#38BDF8"));
        Button btnFullscreen = createMenuItem("fullscreen", "Vollbild", LucideIcon.MAXIMIZE, Color.web("#D1D5DB"));

        // 3. Application Exit
        Button btnExit = createMenuItem("exit", "Beenden", LucideIcon.LOG_OUT, Color.web("#EF4444"));

        root.getChildren().addAll(
                btnSave,
                btnSaveAs,
                createMenuSeparator(),
                btnUpdates,
                btnFullscreen,
                createMenuSeparator(),
                btnExit
        );

        getContent().add(root);
    }

    private Button createMenuItem(String key, String title, String iconName, Color iconColor) {
        Button btn = new Button(title);
        btn.getStyleClass().add("action-menu-item");
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setCursor(Cursor.HAND);
        btn.setContentDisplay(ContentDisplay.LEFT);
        btn.setGraphicTextGap(10);

        btn.setGraphic(LucideIcon.create(iconName, 16, toHex(iconColor)));

        btn.setOnAction(e -> {
            safeHide();
            Runnable action = actions.get(key);
            if (action != null) {
                action.run();
            }
        });

        items.put(key, btn);
        return btn;
    }

    /**
     * Closes the dropdown menu safely and thread-safe if showing.
     */
    public void safeHide() {
        if (isShowing()) {
            if (Platform.isFxApplicationThread()) {
                hide();
            } else {
                Platform.runLater(this::hide);
            }
        }
    }

    private Separator createMenuSeparator() {
        Separator sep = new Separator();
        sep.getStyleClass().add("action-menu-separator");
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

    public Button getItem(String key) {
        return items.get(key);
    }

    public Map<String, Button> getItems() {
        return items;
    }
}
