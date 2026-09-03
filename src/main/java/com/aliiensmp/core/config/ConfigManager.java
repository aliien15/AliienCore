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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
    private static final Map<Class<?>, List<BoundField>> BOUND_FIELDS_CACHE = new ConcurrentHashMap<>();

    private ConfigManager() {
    }

    /**
     * Loads or creates a .yml config file
     *
     * @param plugin The plugin instance
     * @param fileName the name of the file
     * @return The .yml file objects
     * @requires {@code fileName != null} and has to include the {@code .yml} extension at the end
     * @throws IOException If it fails to create a directory to the file if it didn't exist already
     */
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

        for (BoundField boundField : getBoundFields(targetClass)) {
            Field field = boundField.field();
            String path = boundField.path();

            try {
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

    private static List<BoundField> getBoundFields(Class<?> targetClass) {
        return BOUND_FIELDS_CACHE.computeIfAbsent(targetClass, ConfigManager::scanBoundFields);
    }

    private static List<BoundField> scanBoundFields(Class<?> targetClass) {
        List<BoundField> boundFields = new ArrayList<>();

        for (Field field : targetClass.getDeclaredFields()) {
            Key key = field.getAnnotation(Key.class);
            if (key == null) continue;

            field.setAccessible(true);
            boundFields.add(new BoundField(field, key.value()));
        }

        return List.copyOf(boundFields);
    }

    private record BoundField(Field field, String path) {
    }
}