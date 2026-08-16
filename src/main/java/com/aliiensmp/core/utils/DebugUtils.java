package com.aliiensmp.core.utils;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;

import java.util.logging.Level;

public class DebugUtils {

    private static JavaPlugin plugin;
    private static boolean isEnabled;

    /**
     * Sets the plugin instance to send out the debug messages to the console
     * <p>
     * <b>INTERNAL USE ONLY:</b> This is automatically called by AliienCore.init().
     * Do not call this manually in your plugins.
     *
     * @param javaPlugin the plugin instance
     * @requires {@code javaPlugin != null}
     */
    @ApiStatus.Internal
    public static void setPlugin(JavaPlugin javaPlugin) {
        plugin = javaPlugin;
    }

    /**
     * Sets the debug mode, which tells the class whether to send debug messages or not
     *
     * @param debugModeEnabled what to set the debug mode to
     */
    public static void setDebug(boolean debugModeEnabled) {
        isEnabled = debugModeEnabled;
    }

    /**
     * Toggles the debug mode without knowing the currrent state of the toggle (if it is already on or not)
     *
     * @return true if it gets toggled on, false if it gets disabled
     */
    public static boolean toggleDebug() {
        isEnabled = !isEnabled;
        return isEnabled;
    }

    /**
     * Send a debug message if the debug mode is enabled with a "Level.INFO" loggging Level
     *
     * @param message the message that will be sent
     * @param placeholders a list of placeholders to replace in the message
     * @requires .setPlugin() must be used correctly with a non-null parameter, and all parameters here should not be null
     */
    public static void send(String message, String... placeholders) {
        send(Level.INFO, message, placeholders);
    }

    /**
     * Send a debug message if the debug mode is enabled
     *
     * @param loggingLevel the logging level that will be displayed in the console
     * @param message the message that will be sent
     * @param placeholders a list of placeholders to replace in the message
     * @requires .setPlugin() must be used correctly with a non-null parameter, and all parameters here should not be null
     */
    public static void send(Level loggingLevel, String message, String... placeholders) {
        if (!isEnabled) {
            return;
        }

        String finalMessage = applyPlaceholders(message, placeholders);
        plugin.getLogger().log(loggingLevel, finalMessage);
    }

    /**
     * Internal helper to process local key-value vararg placeholders.
     */
    private static String applyPlaceholders(String text, String... placeholders) {
        if (placeholders == null || placeholders.length == 0) return text;

        String result = text;
        for (int i = 0; i < placeholders.length; i += 2) {
            if (i + 1 < placeholders.length) {
                result = result.replace(placeholders[i], placeholders[i + 1]);
            }
        }
        return result;
    }
}
