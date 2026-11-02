package com.aliiensmp.core.config;

import static org.junit.jupiter.api.Assertions.*;

import dev.dejvokep.boostedyaml.YamlDocument;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

class ConfigManagerTest {
    static class Settings {
        @Key("server.name")
        private String name = "default";

        @Key("limit")
        private int limit = 5;

        private String untouched = "unchanged";
    }

    static class StaticSettings {
        @Key("enabled")
        private static boolean enabled = false;
    }

    @Test
    void bindsPrivateFieldsAndLeavesUnannotatedFieldsAlone() throws Exception {
        var yaml =
                YamlDocument.create(
                        new ByteArrayInputStream(
                                "server:\n  name: custom\nlimit: 42\n"
                                        .getBytes(StandardCharsets.UTF_8)));
        var settings = new Settings();
        ConfigManager.bindConfig(yaml, settings);
        assertEquals("custom", settings.name);
        assertEquals(42, settings.limit);
        assertEquals("unchanged", settings.untouched);
        var other = new Settings();
        ConfigManager.bindConfig(yaml, other);
        assertEquals(42, other.limit);
    }

    @Test
    void bindsStaticConfiguration() throws Exception {
        StaticSettings.enabled = false;
        var yaml =
                YamlDocument.create(
                        new ByteArrayInputStream(
                                "enabled: true\n".getBytes(StandardCharsets.UTF_8)));
        try {
            ConfigManager.bindConfig(yaml, StaticSettings.class);
            assertTrue(StaticSettings.enabled);
        } finally {
            StaticSettings.enabled = false;
        }
    }

    @org.junit.jupiter.api.Test
    void missingDefaultsAreSavedAndSurviveReload(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path directory) throws Exception {
        var file = directory.resolve("settings.yml").toFile();
        var yaml = YamlDocument.create(file);
        ConfigManager.bindConfig(yaml, new Settings());
        assertEquals("default", yaml.getString("server.name"));
        assertEquals(5, yaml.getInt("limit"));
        org.awaitility.Awaitility.await()
                .atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(
                        () -> {
                            var reloaded = YamlDocument.create(file);
                            assertEquals("default", reloaded.getString("server.name"));
                            assertEquals(5, reloaded.getInt("limit"));
                        });
    }
}
