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
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A universal utility for generating, loading, and auto-updating
 * BoostedYAML configuration files across the Aliien plugin suite.
 */
public class ConfigManager {

    private static final GeneralSettings GENERAL_SETTINGS = GeneralSettings.builder()
            .setUseDefaults(false)
            .build();
    private static final LoaderSettings LOADER_SETTINGS = LoaderSettings.builder()
            .setAutoUpdate(true)
            .build();
    private static final UpdaterSettings UPDATER_SETTINGS = UpdaterSettings.builder()
            .setKeepAll(true)
            .setVersioning(new BasicVersioning("config-version"))
            .build();

    private ConfigManager() {
    }

    public static YamlDocument loadConfig(JavaPlugin plugin, String fileName) throws IOException {
        File configFile = new File(plugin.getDataFolder(), fileName);
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Unable to create config directory: " + parent.getAbsolutePath());
        }

        return YamlDocument.create(
                configFile,
                Objects.requireNonNull(plugin.getResource(fileName)),
                GENERAL_SETTINGS,
                LOADER_SETTINGS,
                DumperSettings.DEFAULT,
                UPDATER_SETTINGS
        );
    }

    /**
     * Binds a YamlDocument to an object's annotated fields.
     * Safely handles both Class definitions (for static fields) and instantiated objects.
     *
     * @param config the config file
     * @param configInstance the config instance (or Class object)
     */
    public static void bindConfig(YamlDocument config, Object configInstance) {
        boolean needsSave = false;

        // Determine the target class, even if a static Class object was passed.
        Class<?> targetClass = (configInstance instanceof Class<?>)
                ? (Class<?>) configInstance
                : configInstance.getClass();

        // If target is a static class, pass 'null' as the instance to the reflection methods.
        Object targetInstance = (configInstance instanceof Class<?>) ? null : configInstance;

        for (Field field : targetClass.getDeclaredFields()) {
            if (!field.isAnnotationPresent(Key.class)) continue;

            Key key = field.getAnnotation(Key.class);
            String path = key.value();

            try {
                field.setAccessible(true);

                if (config.contains(path)) {
                    // Inject from file into Java
                    field.set(targetInstance, config.get(path));
                } else {
                    // Missing in file: extract Java default and save to file
                    config.set(path, field.get(targetInstance));
                    needsSave = true;
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        if (needsSave) {
            CompletableFuture.runAsync(() -> {
                try {
                    config.save();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}