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
 * In-App Modal Dialog for checking and applying software updates.
 * Styled identically to JDMX's AboutUpdateDialog.
 */
public class AboutUpdateDialog extends VBox {

    private final Runnable closeCallback;

    private Label lblTitle;
    private Label lblCurTitle;
    private Label lblLatTitle;
    private Label lblCurrentVersion;
    private Label lblLatestVersion;
    private Label lblStatus;
    private ProgressIndicator spinner;
    private MarkdownView markdownChangelog;
    private ProgressBar progressBar;
    private Label lblProgressDetail;
    private HBox actionButtonBox;
    private Button btnClose;
    private Button btnCloseFooter;
    private Button btnUpdate;
    private Button btnRetry;
    private UpdateService.ReleaseInfo lastRelease;
    private boolean isChecking = true;
    private boolean isError = false;

    /**
     * Constructs the update dialog card and immediately initiates an async GitHub release check.
     *
     * @param closeCallback callback invoked when the user closes the dialog
     */
    public AboutUpdateDialog(Runnable closeCallback) {
        this.closeCallback = closeCallback != null ? closeCallback : () -> {};

        getStyleClass().add("settings-dialog-card");
        setMinWidth(520);
        setPrefWidth(550);
        setMaxWidth(580);
        setMaxHeight(Region.USE_PREF_SIZE);
        setSpacing(14);
        setOnMouseClicked(Event::consume);

        buildUi();
        checkForUpdates();
    }

    /**
     * Helper displaying the dialog inside an overlay host.
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

    private void buildUi() {
        // 1. Header (Icon, Titel, roter Schließen-Button)
        HBox header = buildHeader();

        // 2. Versions-Kachel (Installiert vs. Neueste Version)
        HBox versionCard = buildVersionCard();

        // 3. Status & Update-Bereich
        VBox statusCard = buildStatusCard();

        // 4. Footer (GitHub Link & Schließen)
        HBox footer = buildFooter();

        getChildren().addAll(header, versionCard, statusCard, footer);
    }

    private HBox buildHeader() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("settings-dialog-header");

        Node iconLogo = LucideIcon.create(LucideIcon.ARROW_UP_CIRCLE, 20, "#38BDF8");
        this.lblTitle = new Label("Software-Updates");
        lblTitle.getStyleClass().add("settings-dialog-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnClose = new Button();
        btnClose.getStyleClass().add("modal-close-btn");
        Node closeIcon = LucideIcon.create(LucideIcon.X, 14, "#EF4444");
        btnClose.setGraphic(closeIcon);
        btnClose.setOnAction(e -> close());

        header.getChildren().addAll(iconLogo, lblTitle, spacer, btnClose);
        return header;
    }

    private HBox buildVersionCard() {
        HBox versionCard = new HBox(16);
        versionCard.setAlignment(Pos.CENTER_LEFT);
        versionCard.setPadding(new Insets(12, 16, 12, 16));
        versionCard.setStyle("-fx-background-color: #18181B; -fx-border-color: #27272A; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        // Installierte Version
        VBox curBox = new VBox(2);
        this.lblCurTitle = new Label("INSTALLIERTE VERSION");
        lblCurTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-font-weight: bold;");
        this.lblCurrentVersion = new Label("v" + UpdateService.CURRENT_VERSION);
        lblCurrentVersion.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 14px; -fx-font-weight: bold;");
        curBox.getChildren().addAll(lblCurTitle, lblCurrentVersion);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Neueste Version auf GitHub
        VBox latBox = new VBox(2);
        latBox.setAlignment(Pos.CENTER_RIGHT);
        this.lblLatTitle = new Label("NEUESTE VERSION");
        lblLatTitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 10px; -fx-font-weight: bold;");
        this.lblLatestVersion = new Label("Prüfe...");
        lblLatestVersion.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 14px; -fx-font-weight: bold;");
        latBox.getChildren().addAll(lblLatTitle, lblLatestVersion);

        versionCard.getChildren().addAll(curBox, spacer, latBox);
        return versionCard;
    }

    private VBox buildStatusCard() {
        VBox statusCard = new VBox(10);
        statusCard.setPadding(new Insets(12, 16, 12, 16));
        statusCard.setStyle("-fx-background-color: #18181B; -fx-border-color: #27272A; -fx-border-width: 1px; -fx-background-radius: 4px; -fx-border-radius: 4px;");

        // Status-Zeile mit Spinner
        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        this.spinner = new ProgressIndicator();
        spinner.setMaxSize(16, 16);
        spinner.setMinSize(16, 16);
        spinner.setPrefSize(16, 16);
        spinner.setStyle("-fx-progress-color: #38BDF8;");

        this.lblStatus = new Label("Suche nach Updates auf GitHub...");
        lblStatus.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px;");
        statusRow.getChildren().addAll(spinner, lblStatus);

        // Changelog / Release Notes als native formatierte MarkdownView
        this.markdownChangelog = new MarkdownView();
        markdownChangelog.setMaxHeight(200);
        markdownChangelog.setPrefHeight(180);
        markdownChangelog.setVisible(false);
        markdownChangelog.setManaged(false);

        // Fortschrittsbalken für Download
        this.progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #38BDF8;");
        progressBar.setVisible(false);
        progressBar.setManaged(false);

        this.lblProgressDetail = new Label();
        lblProgressDetail.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11px;");
        lblProgressDetail.setVisible(false);
        lblProgressDetail.setManaged(false);

        // Aktionsbuttons (innenliegend rechtsbündig)
        this.actionButtonBox = new HBox(8);
        actionButtonBox.setAlignment(Pos.CENTER_RIGHT);

        statusCard.getChildren().addAll(statusRow, markdownChangelog, progressBar, lblProgressDetail, actionButtonBox);
        return statusCard;
    }

    private HBox buildFooter() {
        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);

        Button btnGithub = new Button("GitHub");
        btnGithub.getStyleClass().add("patch-cancel-btn");
        btnGithub.setGraphic(LucideIcon.create(LucideIcon.EXTERNAL_LINK, 13, "#9CA3AF"));
        btnGithub.setOnAction(e -> {
            String targetUrl = lastRelease != null && lastRelease.htmlUrl() != null
                    ? lastRelease.htmlUrl()
                    : UpdateService.REPO_URL + "/releases";
            UpdateService.openWebpage(targetUrl);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.btnCloseFooter = new Button("Schließen");
        btnCloseFooter.getStyleClass().add("patch-cancel-btn");
        btnCloseFooter.setOnAction(e -> close());

        footer.getChildren().addAll(btnGithub, spacer, btnCloseFooter);
        return footer;
    }

    /**
     * Startet die asynchrone Prüfung gegen die GitHub Releases API.
     */
    public void checkForUpdates() {
        this.isChecking = true;
        this.isError = false;
        spinner.setVisible(true);
        spinner.setManaged(true);
        lblStatus.setText("Suche nach Updates auf GitHub...");
        lblStatus.setStyle("-fx-text-fill: #F3F4F6; -fx-font-size: 12px;");
        lblLatestVersion.setText("Prüfe...");
        lblLatestVersion.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 14px; -fx-font-weight: bold;");
        markdownChangelog.setVisible(false);
        markdownChangelog.setManaged(false);
        progressBar.setVisible(false);
        progressBar.setManaged(false);
        lblProgressDetail.setVisible(false);
        lblProgressDetail.setManaged(false);
        actionButtonBox.getChildren().clear();

        UpdateService.checkLatestRelease()
                .thenAccept(release -> Platform.runLater(() -> applyReleaseResult(release)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> applyErrorResult(ex.getMessage()));
                    return null;
                });
    }

    private void applyReleaseResult(UpdateService.ReleaseInfo release) {
        this.isChecking = false;
        this.isError = false;
        this.lastRelease = release;
        spinner.setVisible(false);
        spinner.setManaged(false);

        String displayTag = release.tagName().startsWith("v") ? release.tagName() : "v" + release.tagName();
        lblLatestVersion.setText(displayTag);

        if (release.isNewer()) {
            lblStatus.setText("Neue Version verfügbar: " + displayTag);
            lblStatus.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 12px; -fx-font-weight: bold;");

            if (release.body() != null && !release.body().isBlank()) {
                markdownChangelog.setMarkdown(release.body());
                markdownChangelog.setVisible(true);
                markdownChangelog.setManaged(true);
            }

            this.btnUpdate = new Button("Jetzt aktualisieren");
            btnUpdate.setStyle("-fx-background-color: #38BDF8; -fx-text-fill: #0F172A; -fx-font-weight: bold; -fx-background-radius: 4px; -fx-padding: 6 12; -fx-cursor: hand;");
            btnUpdate.setGraphic(LucideIcon.create(LucideIcon.DOWNLOAD, 14, "#0F172A"));

            btnUpdate.setOnAction(e -> {
                btnUpdate.setDisable(true);
                progressBar.setVisible(true);
                progressBar.setManaged(true);
                lblProgressDetail.setVisible(true);
                lblProgressDetail.setManaged(true);

                UpdateService.performUpdate(
                        release,
                        progress -> Platform.runLater(() -> {
                            if (progress >= 0) {
                                progressBar.setProgress(progress);
                            }
                        }),
                        status -> Platform.runLater(() -> {
                            lblProgressDetail.setText(status);
                        })
                ).exceptionally(ex -> {
                    Platform.runLater(() -> {
                        btnUpdate.setDisable(false);
                        lblProgressDetail.setText("Update fehlgeschlagen: " + ex.getMessage());
                        lblProgressDetail.setStyle("-fx-text-fill: #EF4444;");
                    });
                    return null;
                });
            });

            actionButtonBox.getChildren().setAll(btnUpdate);
        } else {
            lblStatus.setText("artnet2view ist auf dem neuesten Stand.");
            lblStatus.setStyle("-fx-text-fill: #10B981; -fx-font-size: 12px; -fx-font-weight: bold;");

            this.btnRetry = new Button("Erneut prüfen");
            btnRetry.getStyleClass().add("patch-cancel-btn");
            btnRetry.setGraphic(LucideIcon.create(LucideIcon.REFRESH_CW, 13, "#9CA3AF"));
            btnRetry.setOnAction(e -> checkForUpdates());

            actionButtonBox.getChildren().setAll(btnRetry);
        }
    }

    private void applyErrorResult(String errorMessage) {
        this.isChecking = false;
        this.isError = true;
        spinner.setVisible(false);
        spinner.setManaged(false);
        lblLatestVersion.setText("Fehler");
        lblLatestVersion.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 14px; -fx-font-weight: bold;");
        lblStatus.setText("Update-Prüfung fehlgeschlagen: " + errorMessage);
        lblStatus.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 12px;");

        this.btnRetry = new Button("Erneut prüfen");
        btnRetry.getStyleClass().add("patch-cancel-btn");
        btnRetry.setGraphic(LucideIcon.create(LucideIcon.REFRESH_CW, 13, "#9CA3AF"));
        btnRetry.setOnAction(e -> checkForUpdates());

        actionButtonBox.getChildren().setAll(btnRetry);
    }

    private void close() {
        if (closeCallback != null) {
            closeCallback.run();
        }
    }

    public Node getView() {
        return this;
    }

    public Label getLblCurrentVersion() {
        return lblCurrentVersion;
    }

    public Label getLblCurTitle() {
        return lblCurTitle;
    }

    public Label getLblLatestVersion() {
        return lblLatestVersion;
    }

    public Label getLblLatTitle() {
        return lblLatTitle;
    }

    public Label getLblStatus() {
        return lblStatus;
    }

    public Button getBtnClose() {
        return btnClose;
    }

    public MarkdownView getMarkdownChangelog() {
        return markdownChangelog;
    }
}
