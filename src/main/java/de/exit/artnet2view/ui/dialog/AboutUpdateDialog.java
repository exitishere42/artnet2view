package de.exit.artnet2view.ui.dialog;

import de.exit.artnet2view.service.UpdateService;
import de.exit.artnet2view.ui.components.MarkdownView;
import de.exit.artnet2view.ui.theme.LucideIcon;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Modal overlay dialog showing installed vs. latest GitHub release version,
 * rendered Markdown release notes, and one-click self-update installation.
 */
public class AboutUpdateDialog extends VBox {

    private final Runnable closeCallback;

    private final Label availableVersionValue;
    private final ProgressIndicator checkSpinner;
    private final Label statusLabel;
    private final MarkdownView releaseNotesView;

    private final VBox progressContainer;
    private final ProgressBar downloadProgressBar;
    private final Label downloadStatusLabel;

    private final Button githubBtn;
    private final Button retryBtn;
    private final Button updateBtn;

    private UpdateService.ReleaseInfo latestRelease;

    /**
     * Constructs the modal update dialog and immediately initiates an async GitHub release check.
     *
     * @param closeCallback callback invoked when the user closes the dialog
     */
    public AboutUpdateDialog(Runnable closeCallback) {
        this.closeCallback = closeCallback != null ? closeCallback : () -> {};

        getStyleClass().add("settings-dialog-card");
        setPrefWidth(540);
        setMaxWidth(540);
        setMaxHeight(600);
        setSpacing(14);
        setPadding(new Insets(18, 22, 18, 22));
        setOnMouseClicked(Event::consume);

        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("settings-dialog-header");

        Node titleIcon = LucideIcon.create(LucideIcon.ARROW_UP_CIRCLE, 18, "#38BDF8");
        Label titleLabel = new Label("Software-Updates");
        titleLabel.getStyleClass().add("settings-dialog-title");

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        Button closeBtn = new Button();
        closeBtn.setGraphic(LucideIcon.create(LucideIcon.X, 14, "#A1A1AA"));
        closeBtn.getStyleClass().add("modal-close-btn");
        closeBtn.setOnAction(e -> this.closeCallback.run());

        header.getChildren().addAll(titleIcon, titleLabel, headerSpacer, closeBtn);

        // Version Overview Card
        HBox versionCard = new HBox(24);
        versionCard.setAlignment(Pos.CENTER_LEFT);
        versionCard.setPadding(new Insets(12, 16, 12, 16));
        versionCard.setStyle("-fx-background-color: #141417; -fx-border-color: #27272A; "
                + "-fx-border-width: 1px; -fx-border-radius: 6px; -fx-background-radius: 6px;");

        VBox installedBox = new VBox(3);
        Label installedTitle = new Label("INSTALLIERTE VERSION");
        installedTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #71717A;");
        Label installedValue = new Label("v" + UpdateService.CURRENT_VERSION);
        installedValue.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; "
                + "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #F4F4F5;");
        installedBox.getChildren().addAll(installedTitle, installedValue);

        Region vSep = new Region();
        vSep.setMinWidth(1);
        vSep.setPrefWidth(1);
        vSep.setMaxWidth(1);
        vSep.setStyle("-fx-background-color: #27272A;");

        VBox availableBox = new VBox(3);
        Label availableTitle = new Label("NEUESTE VERSION");
        availableTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #71717A;");
        availableVersionValue = new Label("Prüfe...");
        availableVersionValue.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; "
                + "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #38BDF8;");
        availableBox.getChildren().addAll(availableTitle, availableVersionValue);

        versionCard.getChildren().addAll(installedBox, vSep, availableBox);

        // Status Line
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        checkSpinner = new ProgressIndicator();
        checkSpinner.setPrefSize(16, 16);
        checkSpinner.setMaxSize(16, 16);

        statusLabel = new Label("Suche nach Updates auf GitHub...");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #D4D4D8;");
        statusLabel.setWrapText(true);

        statusRow.getChildren().addAll(checkSpinner, statusLabel);

        // Release Notes
        Label notesHeader = new Label("VERSIONS-HINWEISE");
        notesHeader.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #71717A;");

        releaseNotesView = new MarkdownView();
        releaseNotesView.setPrefHeight(220);
        releaseNotesView.setMinHeight(160);
        VBox.setVgrow(releaseNotesView, Priority.ALWAYS);
        releaseNotesView.setMarkdown("Verbinde mit GitHub Releases...");

        // Download Progress
        progressContainer = new VBox(6);
        progressContainer.setVisible(false);
        progressContainer.setManaged(false);

        downloadProgressBar = new ProgressBar(0);
        downloadProgressBar.setMaxWidth(Double.MAX_VALUE);
        downloadProgressBar.setPrefHeight(8);
        downloadProgressBar.setStyle("-fx-accent: #38BDF8;");

        downloadStatusLabel = new Label("Bereite Download vor...");
        downloadStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9CA3AF;");

        progressContainer.getChildren().addAll(downloadProgressBar, downloadStatusLabel);

        // Footer Buttons
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(4, 0, 0, 0));

        githubBtn = new Button("GitHub öffnen");
        githubBtn.setGraphic(LucideIcon.create(LucideIcon.EXTERNAL_LINK, 13, "#E4E4E7"));
        githubBtn.getStyleClass().add("patch-cancel-btn");
        githubBtn.setOnAction(e -> {
            String targetUrl = latestRelease != null && latestRelease.htmlUrl() != null
                    ? latestRelease.htmlUrl()
                    : UpdateService.REPO_URL + "/releases";
            UpdateService.openWebpage(targetUrl);
        });

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);

        retryBtn = new Button("Erneut prüfen");
        retryBtn.getStyleClass().add("patch-cancel-btn");
        retryBtn.setOnAction(e -> checkUpdates());

        updateBtn = new Button("Jetzt aktualisieren");
        updateBtn.setGraphic(LucideIcon.create(LucideIcon.DOWNLOAD, 14, "#FFFFFF"));
        updateBtn.setStyle("-fx-background-color: #0284C7; -fx-text-fill: #FFFFFF; -fx-font-weight: 700; "
                + "-fx-font-size: 12px; -fx-padding: 7 16; -fx-background-radius: 6px; -fx-cursor: hand;");
        updateBtn.setVisible(false);
        updateBtn.setManaged(false);
        updateBtn.setOnAction(e -> triggerUpdate());

        Button closeFooterBtn = new Button("Schließen");
        closeFooterBtn.getStyleClass().add("patch-cancel-btn");
        closeFooterBtn.setOnAction(e -> this.closeCallback.run());

        footer.getChildren().addAll(githubBtn, footerSpacer, retryBtn, updateBtn, closeFooterBtn);

        getChildren().addAll(header, versionCard, statusRow, notesHeader, releaseNotesView, progressContainer, footer);

        checkUpdates();
    }

    /**
     * Convenience helper displaying the dialog inside an overlay host.
     *
     * @param showOverlay callback to mount and fade in a dialog node
     * @param hideOverlay callback to hide and unmount the active dialog node
     */
    public static void show(Consumer<Node> showOverlay, Runnable hideOverlay) {
        if (showOverlay != null) {
            AboutUpdateDialog dialog = new AboutUpdateDialog(hideOverlay);
            showOverlay.accept(dialog);
        }
    }

    private void checkUpdates() {
        checkSpinner.setVisible(true);
        checkSpinner.setManaged(true);
        availableVersionValue.setText("Prüfe...");
        availableVersionValue.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; "
                + "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #38BDF8;");
        statusLabel.setText("Suche nach Updates auf GitHub...");
        statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #D4D4D8;");
        updateBtn.setVisible(false);
        updateBtn.setManaged(false);
        retryBtn.setDisable(true);

        UpdateService.checkLatestRelease().whenComplete((release, ex) -> Platform.runLater(() -> {
            checkSpinner.setVisible(false);
            checkSpinner.setManaged(false);
            retryBtn.setDisable(false);

            if (ex != null || release == null) {
                Throwable cause = ex != null && ex.getCause() != null ? ex.getCause() : ex;
                String msg = cause != null && cause.getMessage() != null ? cause.getMessage() : "Unbekannter Fehler";
                availableVersionValue.setText("Fehler");
                availableVersionValue.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; "
                        + "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #EF4444;");
                statusLabel.setText("Update-Prüfung fehlgeschlagen: " + msg);
                statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #EF4444;");
                releaseNotesView.setMarkdown("Konnte GitHub Releases nicht abrufen.\n\n**Fehler:** `" + msg + "`");
                return;
            }

            this.latestRelease = release;
            String displayTag = release.tagName().startsWith("v") ? release.tagName() : "v" + release.tagName();
            availableVersionValue.setText(displayTag);

            if (release.isNewer()) {
                availableVersionValue.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; "
                        + "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #10B981;");
                statusLabel.setText("Neue Version verfügbar!");
                statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #10B981;");
                updateBtn.setVisible(true);
                updateBtn.setManaged(true);
                updateBtn.setDisable(false);
            } else {
                availableVersionValue.setStyle("-fx-font-family: 'Consolas', 'Courier New', monospace; "
                        + "-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: #A1A1AA;");
                statusLabel.setText("artnet2view ist auf dem neuesten Stand.");
                statusLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #D4D4D8;");
            }

            releaseNotesView.setMarkdown(release.body());
        }));
    }

    private void triggerUpdate() {
        if (latestRelease == null) {
            return;
        }
        updateBtn.setDisable(true);
        retryBtn.setDisable(true);
        progressContainer.setVisible(true);
        progressContainer.setManaged(true);
        downloadProgressBar.setProgress(-1);
        downloadStatusLabel.setText("Starte Download...");

        UpdateService.performUpdate(
                latestRelease,
                progress -> Platform.runLater(() -> downloadProgressBar.setProgress(progress)),
                status -> Platform.runLater(() -> downloadStatusLabel.setText(status))
        ).whenComplete((unused, ex) -> {
            if (ex != null) {
                Platform.runLater(() -> {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String msg = cause != null && cause.getMessage() != null ? cause.getMessage() : "Download fehlgeschlagen";
                    downloadStatusLabel.setText("Fehler: " + msg);
                    downloadStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #EF4444;");
                    downloadProgressBar.setProgress(0);
                    updateBtn.setDisable(false);
                    retryBtn.setDisable(false);
                });
            }
        });
    }
}
