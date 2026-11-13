package de.exit.artnet2view.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SemVer comparison and GitHub API release JSON parsing in UpdateService.
 */
class UpdateServiceTest {

    @Test
    @DisplayName("Should compare SemVer release tags accurately")
    void testIsVersionNewer() {
        assertThat(UpdateService.isVersionNewer("v1.2.0", "1.1.0")).isTrue();
        assertThat(UpdateService.isVersionNewer("v1.4.0", UpdateService.CURRENT_VERSION)).isTrue();
        assertThat(UpdateService.isVersionNewer("v1.1.1", "1.1.0")).isTrue();
        assertThat(UpdateService.isVersionNewer("v2.0.0", "1.1.0")).isTrue();

        assertThat(UpdateService.isVersionNewer("v1.3.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v1.2.0", UpdateService.CURRENT_VERSION)).isFalse();
        assertThat(UpdateService.isVersionNewer("v1.1.0", "1.1.0")).isFalse();
        assertThat(UpdateService.isVersionNewer("1.1.0", "1.1.0")).isFalse();
        assertThat(UpdateService.isVersionNewer("v1.0.9", "1.1.0")).isFalse();
        assertThat(UpdateService.isVersionNewer(null, "1.1.0")).isFalse();
    }

    @Test
    @DisplayName("Should parse GitHub Release JSON response cleanly")
    void testParseReleaseJson() throws Exception {
        String sampleJson = """
                {
                  "tag_name": "v1.2.0",
                  "name": "artnet2view v1.2.0",
                  "body": "## Features\\n- 3D Gizmo\\n- ZQ02040",
                  "html_url": "https://github.com/exitishere42/artnet2view/releases/tag/v1.2.0",
                  "assets": [
                    {
                      "name": "artnet2view-1.2.0-all.jar",
                      "browser_download_url": "https://github.com/exitishere42/artnet2view/releases/download/v1.2.0/artnet2view-1.2.0-all.jar",
                      "size": 18271116
                    },
                    {
                      "name": "artnet2view-x86_64.AppImage",
                      "browser_download_url": "https://github.com/exitishere42/artnet2view/releases/download/v1.2.0/artnet2view-x86_64.AppImage",
                      "size": 16645120
                    }
                  ]
                }
                """;

        UpdateService.ReleaseInfo info = UpdateService.parseReleaseJson(sampleJson, "1.1.0");
        assertThat(info.tagName()).isEqualTo("v1.2.0");
        assertThat(info.isNewer()).isTrue();
        assertThat(info.jarFileName()).isEqualTo("artnet2view-1.2.0-all.jar");
        assertThat(info.appImageFileName()).isEqualTo("artnet2view-x86_64.AppImage");
        assertThat(info.body()).contains("3D Gizmo");
    }
}
