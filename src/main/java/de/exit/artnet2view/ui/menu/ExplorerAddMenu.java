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
 * Action dropdown menu for adding new elements in the Project Explorer dock.
 * Styled identically to JDMX's ExplorerAddMenu.
 */
public class ExplorerAddMenu extends Popup {

    private final Map<String, Button> items = new HashMap<>();
    private final Map<String, Runnable> actions = new HashMap<>();

    public ExplorerAddMenu() {
        setAutoHide(true);
        setHideOnEscape(true);

        VBox root = new VBox(2);
        root.getStyleClass().add("action-menu-popup");
        var cssUrl = ExplorerAddMenu.class.getResource("/de/exit/artnet2view/styles/dark-theme.css");
        if (cssUrl != null) {
            root.getStylesheets().add(cssUrl.toExternalForm());
        }
        root.setMinWidth(170);
        root.setPrefWidth(178);

        Button btnSpot = createMenuItem("moving_head", "Moving Head", LucideIcon.BOX, Color.web("#38BDF8"));
        Button btnPar = createMenuItem("par", "PAR-Scheinwerfer", LucideIcon.BOX, Color.web("#F59E0B"));
        Button btnTruss = createMenuItem("truss", "Traverse (2m)", LucideIcon.LAYERS, Color.web("#A78BFA"));

        root.getChildren().addAll(
                btnSpot,
                btnPar,
                createMenuSeparator(),
                btnTruss
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

        btn.setGraphic(LucideIcon.create(iconName, 15, toHex(iconColor)));

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
}
