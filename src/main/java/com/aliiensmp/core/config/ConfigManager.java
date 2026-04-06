package com.aliiensmp.core.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * A universal utility for generating, loading, and auto-updating
 * BoostedYAML configuration files across the Aliien plugin suite.
 */
public class ConfigManager {

    /**
     * Creates or loads a BoostedYAML configuration file.
     * Automatically handles plugin data folder creation, default file extraction,
     * and safe automatic version updating based on the "config-version" key.
     *
     * @param plugin   The plugin instance requesting the config.
     * @param fileName The name of the file (e.g., "config.yml" or "gui.yml").
     * @return The loaded YamlDocument.
     * @throws IOException If the file cannot be read, created, or saved.
     */
    public static YamlDocument loadConfig(JavaPlugin plugin, String fileName) throws IOException {
        return YamlDocument.create(
                new File(plugin.getDataFolder(), fileName),
                plugin.getResource(fileName),
                GeneralSettings.builder().setUseDefaults(false).build(),
                LoaderSettings.builder().setAutoUpdate(true).build(),
                DumperSettings.DEFAULT,
                UpdaterSettings.builder().setKeepAll(true).setVersioning(new BasicVersioning("config-version")).build()
        );
    }
}