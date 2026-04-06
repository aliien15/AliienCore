package com.aliiensmp.core;

import com.aliiensmp.core.menu.MenuListener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The shaded initializer for AliienCore.
 * This class handles the setup of global systems (like the GUI engine)
 * using the host plugin's instance.
 */
public class AliienCore {

    /**
     * Initializes the AliienCore framework for a specific plugin.
     * Run this in your plugin's onEnable() method.
     * * @param plugin The instance of the plugin using the library.
     */
    public static void init(JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(new MenuListener(plugin), plugin);
    }
}