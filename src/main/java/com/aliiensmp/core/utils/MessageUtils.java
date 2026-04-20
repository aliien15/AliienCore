package com.aliiensmp.core.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;

/**
 * Universal dispatcher for sending formatted messages, action bars, and titles.
 * Automatically handles prefix injection, local placeholders, ColorUtils parsing,
 * and PlaceholderAPI integration (if installed).
 */
public final class MessageUtils {

    private static final Method PLACEHOLDER_METHOD = resolvePlaceholderMethod();
    private static final Title.Times DEFAULT_TITLE_TIMES = Title.Times.times(
            Duration.ofMillis(500),
            Duration.ofMillis(3500),
            Duration.ofMillis(1000)
    );

    private MessageUtils() {
    }

    /**
     * Sends a fully formatted message to a CommandSender.
     *
     * @param sender       The recipient (Player or Console).
     * @param prefix       The plugin's prefix (can be empty or null).
     * @param message      The raw message string.
     * @param placeholders Local placeholder pairs (e.g., "%player%", "Aliien").
     */
    public static void send(CommandSender sender, String prefix, String message, String... placeholders) {
        if (message == null || message.isEmpty()) return;

        String finalMessage = applyPlaceholders(message, placeholders);
        String fullMessage = (prefix != null ? prefix : "") + finalMessage;

        // Apply PlaceholderAPI if the sender is a Player and PAPI is installed
        if (sender instanceof Player player) {
            fullMessage = applyPAPI(player, fullMessage);
        }

        Component component = ColorUtils.color(fullMessage);
        sender.sendMessage(component);
    }

    /**
     * Broadcasts a fully formatted message to all online players and the console.
     * Utilizes the existing send method to ensure PAPI placeholders are parsed per-player.
     *
     * @param prefix       The plugin's prefix (can be empty or null).
     * @param message      The raw message string.
     * @param placeholders Local placeholder pairs (e.g., "%event%", "boss_fight").
     */
    public static void broadcast(String prefix, String message, String... placeholders) {
        if (message == null || message.isEmpty()) return;

        // Send to all online players
        Bukkit.getOnlinePlayers().forEach(player ->
                send(player, prefix, message, placeholders)
        );

        // Send to console
        send(Bukkit.getConsoleSender(), prefix, message, placeholders);
    }

    /**
     * Sends a fully formatted Action Bar message to a Player.
     *
     * @param player       The recipient.
     * @param message      The raw action bar string.
     * @param placeholders Local placeholder pairs.
     */
    public static void sendActionBar(Player player, String message, String... placeholders) {
        if (message == null || message.isEmpty()) return;

        String finalMessage = applyPlaceholders(message, placeholders);
        finalMessage = applyPAPI(player, finalMessage);

        player.sendActionBar(ColorUtils.color(finalMessage));
    }

    /**
     * Sends a fully formatted Title and Subtitle to a Player.
     *
     * @param player       The recipient.
     * @param title        The raw main title string.
     * @param subtitle     The raw subtitle string.
     * @param placeholders Local placeholder pairs.
     */
    public static void sendTitle(Player player, String title, String subtitle, String... placeholders) {
        String finalTitle = title != null ? applyPAPI(player, applyPlaceholders(title, placeholders)) : "";
        String finalSubtitle = subtitle != null ? applyPAPI(player, applyPlaceholders(subtitle, placeholders)) : "";

        player.showTitle(Title.title(
                ColorUtils.color(finalTitle),
                ColorUtils.color(finalSubtitle),
                DEFAULT_TITLE_TIMES
        ));
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

    /**
     * Internal helper to safely parse PlaceholderAPI placeholders.
     * Fails silently and returns the original text if PAPI is not installed.
     */
    private static String applyPAPI(Player player, String text) {
        if (PLACEHOLDER_METHOD == null || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return text;
        }

        try {
            return (String) PLACEHOLDER_METHOD.invoke(null, player, text);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
            return text;
        }
    }

    private static Method resolvePlaceholderMethod() {
        try {
            return Class.forName("me.clip.placeholderapi.PlaceholderAPI")
                    .getMethod("setPlaceholders", Player.class, String.class);
        } catch (ClassNotFoundException | NoSuchMethodException exception) {
            return null;
        }
    }
}
