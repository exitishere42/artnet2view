package de.exit.artnet2view.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.exit.artnet2view.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Automatic GitHub Release Update Service for artnet2view.
 * Asynchronously fetches latest release metadata from GitHub and performs seamless
 * cross-platform self-updates (Windows Fat-JAR via update-runner.bat & Linux AppImage/JAR).
 */
public class UpdateService {

    private static final Logger log = LoggerFactory.getLogger(UpdateService.class);

    public static final String CURRENT_VERSION = "1.3.0";
    public static final String GITHUB_REPO = "exitishere42/artnet2view";
    public static final String API_URL = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
    public static final String REPO_URL = "https://github.com/" + GITHUB_REPO;

    public record ReleaseInfo(
            String tagName,
            String name,
            String body,
            String htmlUrl,
            boolean isNewer,
            String jarDownloadUrl,
            String jarFileName,
            long jarSize,
            String appImageDownloadUrl,
            String appImageFileName,
            long appImageSize
    ) {}

    public static CompletableFuture<ReleaseInfo> checkLatestRelease() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(6))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .timeout(Duration.ofSeconds(10))
                        .header("User-Agent", "artnet2view-updater/" + CURRENT_VERSION)
                        .header("Accept", "application/vnd.github.v3+json")
                        .GET()
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    throw new IllegalStateException("GitHub API Fehler: HTTP " + response.statusCode());
                }

                return parseReleaseJson(response.body(), CURRENT_VERSION);
            } catch (Exception e) {
                log.warn("Could not fetch GitHub releases: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public static ReleaseInfo parseReleaseJson(String jsonBody, String currentVersion) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonBody);

        String tagName = root.path("tag_name").asText("");
        String name = root.path("name").asText(tagName);
        String body = root.path("body").asText("");
        String htmlUrl = root.path("html_url").asText(REPO_URL);

        String jarUrl = null;
        String jarName = null;
        long jarSize = 0;

        String appImageUrl = null;
        String appImageName = null;
        long appImageSize = 0;

        JsonNode assets = root.path("assets");
        if (assets.isArray()) {
            for (JsonNode asset : assets) {
                String aName = asset.path("name").asText("");
                String aUrl = asset.path("browser_download_url").asText("");
                long aSize = asset.path("size").asLong(0);

                if (aName.endsWith("-all.jar") || (aName.endsWith(".jar") && !aName.contains("javadoc") && !aName.contains("sources"))) {
                    jarUrl = aUrl;
                    jarName = aName;
                    jarSize = aSize;
                } else if (aName.endsWith(".AppImage")) {
                    appImageUrl = aUrl;
                    appImageName = aName;
                    appImageSize = aSize;
                }
            }
        }

        boolean isNewer = isVersionNewer(tagName, currentVersion);
        return new ReleaseInfo(tagName, name, body, htmlUrl, isNewer, jarUrl, jarName, jarSize, appImageUrl, appImageName, appImageSize);
    }

    public static boolean isVersionNewer(String latestTag, String currentVersion) {
        if (latestTag == null || currentVersion == null) {
            return false;
        }
        String cleanLatest = latestTag.trim().replaceAll("^[^0-9]+", "");
        String cleanCurrent = currentVersion.trim().replaceAll("^[^0-9]+", "");
        if (cleanLatest.isEmpty() || cleanCurrent.isEmpty()) {
            return false;
        }

        String[] latParts = cleanLatest.split("[.\\-_]");
        String[] curParts = cleanCurrent.split("[.\\-_]");
        int len = Math.max(latParts.length, curParts.length);
        for (int i = 0; i < len; i++) {
            int l = i < latParts.length ? parsePart(latParts[i]) : 0;
            int c = i < curParts.length ? parsePart(curParts[i]) : 0;
            if (l > c) {
                return true;
            }
            if (l < c) {
                return false;
            }
        }
        return false;
    }

    private static int parsePart(String s) {
        try {
            String digits = s.replaceAll("[^0-9].*$", "");
            return digits.isEmpty() ? 0 : Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    public static CompletableFuture<Void> performUpdate(
            ReleaseInfo release,
            Consumer<Double> progressCallback,
            Consumer<String> statusCallback
    ) {
        return CompletableFuture.runAsync(() -> {
            try {
                File codeSource;
                try {
                    codeSource = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                } catch (Exception e) {
                    codeSource = new File(".");
                }

                File installDir = codeSource.isDirectory() ? codeSource : codeSource.getParentFile();
                if (installDir == null) {
                    installDir = new File(".");
                }

                String appImagePath = System.getenv("APPIMAGE");
                boolean isAppImage = appImagePath != null && new File(appImagePath).exists() && release.appImageDownloadUrl() != null;
                boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");

                if (isAppImage) {
                    statusCallback.accept("Lade neues AppImage herunter...");
                    File targetAppImage = new File(appImagePath);
                    File tempFile = new File(targetAppImage.getParentFile(), targetAppImage.getName() + ".download");

                    downloadFile(release.appImageDownloadUrl(), tempFile, release.appImageSize(), progressCallback, statusCallback);

                    tempFile.setExecutable(true, false);
                    statusCallback.accept("Ersetze AppImage...");
                    Files.move(tempFile.toPath(), targetAppImage.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    statusCallback.accept("Starte neue Version...");
                    new ProcessBuilder(targetAppImage.getAbsolutePath()).start();
                    System.exit(0);
                    return;
                }

                if (release.jarDownloadUrl() == null) {
                    throw new IllegalStateException("Kein passendes Release-JAR in den GitHub Assets gefunden.");
                }

                statusCallback.accept("Lade Programmdatei herunter...");
                File tempJar = new File(installDir, "artnet2view-update.jar");
                downloadFile(release.jarDownloadUrl(), tempJar, release.jarSize(), progressCallback, statusCallback);

                String newJarName = release.jarFileName() != null ? release.jarFileName() : "artnet2view-" + release.tagName() + "-all.jar";
                File targetJar = new File(installDir, newJarName);

                if (!isWindows) {
                    statusCallback.accept("Wende Update an...");
                    Files.move(tempJar.toPath(), targetJar.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    File runSh = new File(installDir, "run.sh");
                    if (runSh.exists()) {
                        String runScriptContent = "#!/usr/bin/env bash\n" +
                                "SCRIPT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")\" && pwd)\"\n" +
                                "cd \"$SCRIPT_DIR\"\n" +
                                "exec java -jar \"$SCRIPT_DIR/" + targetJar.getName() + "\" \"$@\"\n";
                        Files.writeString(runSh.toPath(), runScriptContent);
                        runSh.setExecutable(true);
                        statusCallback.accept("Starte artnet2view neu...");
                        new ProcessBuilder("bash", runSh.getAbsolutePath()).directory(installDir).start();
                    } else {
                        statusCallback.accept("Starte artnet2view neu...");
                        new ProcessBuilder("java", "-jar", targetJar.getAbsolutePath()).directory(installDir).start();
                    }
                    System.exit(0);
                } else {
                    statusCallback.accept("Bereite Neustart vor...");
                    String runningJar = (codeSource != null && codeSource.isFile()) ? codeSource.getName() : "artnet2view-*.jar";
                    File updateBat = new File(installDir, "update-runner.bat");
                    String batContent = "@echo off\r\n" +
                            "chcp 65001 >nul 2>&1\r\n" +
                            "timeout /t 1 /nobreak >nul\r\n" +
                            ":wait\r\n" +
                            "del \"%~dp0artnet2view-*-all.jar\" >nul 2>&1\r\n" +
                            "if exist \"%~dp0" + runningJar + "\" (\r\n" +
                            "    timeout /t 1 /nobreak >nul\r\n" +
                            "    goto wait\r\n" +
                            ")\r\n" +
                            "move /y \"%~dp0artnet2view-update.jar\" \"%~dp0" + targetJar.getName() + "\" >nul 2>&1\r\n" +
                            "if exist \"%~dp0run.bat\" (\r\n" +
                            "    start \"\" \"%~dp0run.bat\"\r\n" +
                            ") else (\r\n" +
                            "    start \"\" javaw -jar \"%~dp0" + targetJar.getName() + "\"\r\n" +
                            ")\r\n" +
                            "del \"%~f0\"\r\n";

                    Files.writeString(updateBat.toPath(), batContent);
                    statusCallback.accept("Starte artnet2view neu...");
                    new ProcessBuilder("cmd.exe", "/c", "start", "/b", updateBat.getAbsolutePath())
                            .directory(installDir)
                            .start();
                    System.exit(0);
                }
            } catch (Exception e) {
                log.error("Failed to execute update: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    private static void downloadFile(
            String downloadUrl,
            File destination,
            long expectedSize,
            Consumer<Double> progressCallback,
            Consumer<String> statusCallback
    ) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .header("User-Agent", "artnet2view-updater/" + CURRENT_VERSION)
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Download fehlgeschlagen: HTTP " + response.statusCode());
        }

        long totalBytes = expectedSize;
        if (totalBytes <= 0) {
            totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(0);
        }

        try (InputStream in = response.body();
             FileOutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            long bytesReadTotal = 0;
            int bytesRead;

            long lastProgressUpdate = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                bytesReadTotal += bytesRead;

                long now = System.currentTimeMillis();
                if (now - lastProgressUpdate > 100 || bytesReadTotal == totalBytes) {
                    lastProgressUpdate = now;
                    if (totalBytes > 0) {
                        double progress = (double) bytesReadTotal / totalBytes;
                        progressCallback.accept(progress);
                        double mbRead = bytesReadTotal / (1024.0 * 1024.0);
                        double mbTotal = totalBytes / (1024.0 * 1024.0);
                        statusCallback.accept(String.format("Lade Update herunter... (%.1f / %.1f MB - %.0f%%)", mbRead, mbTotal, progress * 100));
                    } else {
                        progressCallback.accept(-1.0);
                        double mbRead = bytesReadTotal / (1024.0 * 1024.0);
                        statusCallback.accept(String.format("Lade Update herunter... (%.1f MB)", mbRead));
                    }
                }
            }
        }
    }

    /**
     * Cross-platform safe browser launcher without java.awt.Desktop.
     */
    public static void openWebpage(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception ex) {
            log.warn("Could not open browser for URL {}: {}", url, ex.getMessage());
        }
    }
}
