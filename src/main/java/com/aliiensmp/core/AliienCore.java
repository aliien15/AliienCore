package com.aliiensmp.core;

import com.aliiensmp.core.database.DatabaseManager;
import com.aliiensmp.core.menu.MenuListener;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

/**
 * The shaded initializer for AliienCore.
 * This class handles the setup of global systems (like the GUI engine)
 * using the host plugin's instance.
 */
public final class AliienCore {

    private static DatabaseManager databaseManager;
    private static boolean initialized;

    private AliienCore() {
    }

    /**
     * Initializes the AliienCore framework for a specific plugin.
     * Run this in your plugin's onEnable() method.
     * @param plugin The instance of the plugin using the library.
     */
    public static synchronized void init(JavaPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin");

        if (!initialized) {
            plugin.getServer().getPluginManager().registerEvents(new MenuListener(plugin), plugin);
            initialized = true;
        }

        if (databaseManager == null) databaseManager = new DatabaseManager();
    }

    /**
     * Enables bStats metrics for the plugin.
     * @param plugin The JavaPlugin instance.
     * @param serviceId The plugin ID provided by bStats.org.
     */
    public static Metrics enableMetrics(JavaPlugin plugin, int serviceId) {
        return new Metrics(plugin, serviceId);
    }

    /**
     * @return the global DatabaseManager instance.
     */
    public static DatabaseManager getDatabase() {
        if (databaseManager == null) {
            databaseManager = new DatabaseManager();
        }
        return databaseManager;
    }
}
