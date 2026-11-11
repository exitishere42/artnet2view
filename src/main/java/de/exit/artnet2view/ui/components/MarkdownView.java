package de.exit.artnet2view.ui.components;

import de.exit.artnet2view.service.UpdateService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight native JavaFX Markdown renderer for GitHub release notes.
 */
public class MarkdownView extends ScrollPane {

    private static final Pattern INLINE_PATTERN = Pattern.compile(
            "(\\*\\*[^*]+\\*\\*|`[^`]+`|\\[[^\\]]+\\]\\([^)]+\\))"
    );
    private static final Pattern LINK_PATTERN = Pattern.compile("^\\[([^\\]]+)\\]\\(([^)]+)\\)$");

    private final VBox contentBox;

    /**
     * Creates an empty scrollable Markdown viewport with dark styling.
     */
    public MarkdownView() {
        this.contentBox = new VBox(4);
        this.contentBox.setPadding(new Insets(10, 12, 10, 12));
        this.contentBox.setFillWidth(true);

        setContent(contentBox);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setStyle("-fx-background-color: #141417; -fx-background: #141417; "
                + "-fx-border-color: #27272A; -fx-border-width: 1px; "
                + "-fx-border-radius: 6px; -fx-background-radius: 6px;");
    }

    /**
     * Parses and renders the given Markdown string into JavaFX nodes.
     *
     * @param markdown Markdown source text (e.g. GitHub release notes)
     */
    public void setMarkdown(String markdown) {
        contentBox.getChildren().clear();
        if (markdown == null || markdown.isBlank()) {
            contentBox.getChildren().add(createParagraph("Keine Versionshinweise vorhanden."));
            return;
        }

        String normalized = markdown.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);

        boolean inCodeBlock = false;
        StringBuilder codeBuffer = new StringBuilder();

        for (String rawLine : lines) {
            String line = rawLine;

            if (line.trim().startsWith("```")) {
                if (inCodeBlock) {
                    contentBox.getChildren().add(createCodeBlock(codeBuffer.toString().stripTrailing()));
                    codeBuffer.setLength(0);
                    inCodeBlock = false;
                } else {
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                codeBuffer.append(rawLine).append("\n");
                continue;
            }

            if (line.trim().isEmpty()) {
                Region spacer = new Region();
                spacer.setMinHeight(4);
                spacer.setPrefHeight(4);
                contentBox.getChildren().add(spacer);
                continue;
            }

            if (line.trim().equals("---") || line.trim().equals("***")) {
                contentBox.getChildren().add(createDivider());
                continue;
            }

            if (line.startsWith("# ")) {
                contentBox.getChildren().add(createHeading(line.substring(2).trim(), 1));
                continue;
            }
            if (line.startsWith("## ")) {
                contentBox.getChildren().add(createHeading(line.substring(3).trim(), 2));
                continue;
            }
            if (line.startsWith("### ")) {
                contentBox.getChildren().add(createHeading(line.substring(4).trim(), 3));
                continue;
            }
            if (line.startsWith("#### ")) {
                contentBox.getChildren().add(createHeading(line.substring(5).trim(), 4));
                continue;
            }

            if (line.startsWith("> ")) {
                contentBox.getChildren().add(createBlockquote(line.substring(2).trim()));
                continue;
            }

            if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
                String itemText = line.trim().substring(2).trim();
                contentBox.getChildren().add(createListItem(itemText));
                continue;
            }

            if (line.trim().matches("^\\d+\\.\\s+.*")) {
                int dotIndex = line.indexOf('.');
                String num = line.substring(0, dotIndex + 1).trim();
                String itemText = line.substring(dotIndex + 1).trim();
                contentBox.getChildren().add(createNumberedListItem(num, itemText));
                continue;
            }

            contentBox.getChildren().add(createParagraph(line));
        }

        if (inCodeBlock && !codeBuffer.isEmpty()) {
            contentBox.getChildren().add(createCodeBlock(codeBuffer.toString().stripTrailing()));
        }
    }

    private Node createHeading(String text, int level) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        switch (level) {
            case 1 -> lbl.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #F3F4F6; -fx-padding: 6 0 2 0;");
            case 2 -> lbl.setStyle("-fx-font-size: 13.5px; -fx-font-weight: 700; -fx-text-fill: #38BDF8; -fx-padding: 5 0 2 0;");
            case 3 -> lbl.setStyle("-fx-font-size: 12.5px; -fx-font-weight: 600; -fx-text-fill: #E5E7EB; -fx-padding: 4 0 1 0;");
            default -> lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #9CA3AF; -fx-padding: 2 0 1 0;");
        }
        return lbl;
    }

    private Node createDivider() {
        Region line = new Region();
        line.setMinHeight(1);
        line.setPrefHeight(1);
        line.setMaxHeight(1);
        line.setStyle("-fx-background-color: #27272A;");
        VBox.setMargin(line, new Insets(4, 0, 4, 0));
        return line;
    }

    private Node createCodeBlock(String code) {
        VBox box = new VBox();
        box.setPadding(new Insets(8, 12, 8, 12));
        box.setStyle("-fx-background-color: #0A0A0C; -fx-border-color: #27272A; "
                + "-fx-border-width: 1px; -fx-border-radius: 4px; -fx-background-radius: 4px;");

        Label lbl = new Label(code);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-family: 'Consolas', 'JetBrains Mono', 'Courier New', monospace; "
                + "-fx-font-size: 11px; -fx-text-fill: #38BDF8;");
        box.getChildren().add(lbl);
        return box;
    }

    private Node createBlockquote(String quote) {
        HBox box = new HBox(8);
        box.setPadding(new Insets(6, 10, 6, 10));
        box.setStyle("-fx-background-color: #161922; -fx-border-color: #38BDF8; -fx-border-width: 0 0 0 3px; "
                + "-fx-background-radius: 2px;");

        TextFlow flow = new TextFlow();
        parseInlineMarkdown(quote, flow);
        box.getChildren().add(flow);
        return box;
    }

    private Node createListItem(String text) {
        HBox itemBox = new HBox(8);
        itemBox.setAlignment(Pos.TOP_LEFT);
        itemBox.setPadding(new Insets(1, 0, 1, 8));

        Label bullet = new Label("•");
        bullet.setStyle("-fx-text-fill: #38BDF8; -fx-font-size: 13px; -fx-font-weight: bold;");

        TextFlow flow = new TextFlow();
        HBox.setHgrow(flow, Priority.ALWAYS);
        parseInlineMarkdown(text, flow);

        itemBox.getChildren().addAll(bullet, flow);
        return itemBox;
    }

    private Node createNumberedListItem(String number, String text) {
        HBox itemBox = new HBox(6);
        itemBox.setAlignment(Pos.TOP_LEFT);
        itemBox.setPadding(new Insets(1, 0, 1, 8));

        Label numLabel = new Label(number);
        numLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 11.5px; -fx-font-weight: 600; -fx-min-width: 18px;");

        TextFlow flow = new TextFlow();
        HBox.setHgrow(flow, Priority.ALWAYS);
        parseInlineMarkdown(text, flow);

        itemBox.getChildren().addAll(numLabel, flow);
        return itemBox;
    }

    private Node createParagraph(String text) {
        TextFlow flow = new TextFlow();
        parseInlineMarkdown(text, flow);
        return flow;
    }

    private void parseInlineMarkdown(String input, TextFlow flow) {
        if (input == null || input.isEmpty()) {
            return;
        }

        Matcher matcher = INLINE_PATTERN.matcher(input);
        int lastIndex = 0;

        while (matcher.find()) {
            if (matcher.start() > lastIndex) {
                flow.getChildren().add(createPlainText(input.substring(lastIndex, matcher.start())));
            }

            String match = matcher.group();
            if (match.startsWith("**") && match.endsWith("**") && match.length() >= 4) {
                flow.getChildren().add(createBoldText(match.substring(2, match.length() - 2)));
            } else if (match.startsWith("`") && match.endsWith("`") && match.length() >= 2) {
                flow.getChildren().add(createInlineCodeText(match.substring(1, match.length() - 1)));
            } else if (match.startsWith("[") && match.contains("](") && match.endsWith(")")) {
                Matcher linkMatch = LINK_PATTERN.matcher(match);
                if (linkMatch.matches()) {
                    flow.getChildren().add(createLinkText(linkMatch.group(1), linkMatch.group(2)));
                } else {
                    flow.getChildren().add(createPlainText(match));
                }
            } else {
                flow.getChildren().add(createPlainText(match));
            }

            lastIndex = matcher.end();
        }

        if (lastIndex < input.length()) {
            flow.getChildren().add(createPlainText(input.substring(lastIndex)));
        }
    }

    private Text createPlainText(String text) {
        Text t = new Text(text);
        t.setStyle("-fx-fill: #D1D5DB; -fx-font-size: 11.5px;");
        return t;
    }

    private Text createBoldText(String text) {
        Text t = new Text(text);
        t.setStyle("-fx-fill: #FFFFFF; -fx-font-size: 11.5px; -fx-font-weight: bold;");
        return t;
    }

    private Text createInlineCodeText(String text) {
        Text t = new Text(" " + text + " ");
        t.setStyle("-fx-fill: #38BDF8; -fx-font-family: 'Consolas', 'JetBrains Mono', 'Courier New', monospace; "
                + "-fx-font-size: 11px; -fx-font-weight: bold;");
        return t;
    }

    private Text createLinkText(String title, String url) {
        Text t = new Text(title);
        t.setStyle("-fx-fill: #38BDF8; -fx-underline: true; -fx-font-size: 11.5px;");
        t.setCursor(Cursor.HAND);
        t.setOnMouseClicked(e -> UpdateService.openWebpage(url));
        return t;
    }

    /**
     * Returns the inner container holding rendered Markdown nodes.
     *
     * @return the content VBox
     */
    public VBox getContentBox() {
        return contentBox;
    }
}
