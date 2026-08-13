package com.aliiensmp.core;

import com.aliiensmp.core.database.DatabaseManager;
import com.aliiensmp.core.input.chat.ChatPrompt;
import com.aliiensmp.core.input.chat.ChatPromptListeners;
import com.aliiensmp.core.menu.MenuListener;
import com.aliiensmp.core.utils.DebugUtils;
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

    public static final boolean IS_FOLIA;

    static {
        boolean folia = false;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException ignored) {
            // Empty, which means that we aren't on folia
        }
        IS_FOLIA = folia;
    }

    /**
     * Initializes the AliienCore framework with all features enabled.
     * Run this in your plugin's onEnable() method.
     *
     * @param plugin The instance of the plugin using the library.
     */
    public static synchronized void init(JavaPlugin plugin) {
        init(plugin, true, true, true);
    }

    /**
     * Initializes the AliienCore framework with specific feature toggles.
     *
     * @param plugin The instance of the plugin using the library.
     * @param enableMenus Whether to register the MenuListener for AliienGUI.
     * @param enableDatabase Whether to initialize the DatabaseManager pool.
     * @param enableChatPrompts Whether to register the ChatPrompt system listeners.
     */
    public static synchronized void init(JavaPlugin plugin, boolean enableMenus, boolean enableDatabase, boolean enableChatPrompts) {
        Objects.requireNonNull(plugin, "plugin");

        if (!initialized) {

            if (enableMenus) {
                plugin.getServer().getPluginManager().registerEvents(new MenuListener(plugin), plugin);
            }

            if (enableChatPrompts) {
                ChatPrompt.init(plugin);
                plugin.getServer().getPluginManager().registerEvents(new ChatPromptListeners(), plugin);
            }

            initialized = true;
        }

        if (enableDatabase && databaseManager == null) {
            databaseManager = new DatabaseManager();
        }

        DebugUtils.setPlugin(plugin);
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